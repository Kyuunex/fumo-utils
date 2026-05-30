package moe.kyuunex.fumo_utils.modules.highwaytools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK;

public class HighwayTunneler extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Integer> interval = sgDefault.add(new IntSetting.Builder()
        .name("interval")
        .description("Cooldown")
        .defaultValue(0)
        .sliderRange(0, 10)
        .range(-1, 1000)
        .build()
    );

    private final Setting<Boolean> mushroomsOnly = sgDefault.add(new BoolSetting.Builder()
        .name("mushrooms-only")
        .description("Only dig out the mushrooms")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> forceYLevelEnable = sgDefault.add(new BoolSetting.Builder()
        .name("forced-y-level")
        .description("Force Y level instead of guessing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> forcedYLevel = sgDefault.add(new IntSetting.Builder()
        .name("y-level")
        .description("Y level to level the ground at.")
        .sliderRange(-64, 320)
        .defaultValue(119)
        .visible(forceYLevelEnable::get)
        .build()
    );

    private final Setting<Integer> farAhead = sgDefault.add(new IntSetting.Builder()
        .name("how-far-ahead")
        .description("")
        .sliderRange(0, 8)
        .defaultValue(1)
        .build()
    );

    private final Setting<Boolean> packetMine = sgDefault.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Packet mine instead. IT'S VERY SLOW!")
        .defaultValue(false)
        .build()
    );

    private int timer = -1;
    private int sequence = 0;
    private int yLevel = 118;

    public HighwayTunneler() {
        super(
            FumoUtils.HIGHWAY,
            "highway-tunneler",
            "Highway tunnel miner."
        );
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        if (forceYLevelEnable.get()) {
            yLevel = forcedYLevel.get();
        } else {
            yLevel = mc.player.getBlockY();
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (Modules.get().get(AutoGap.class).isEating()) return;
        if (Modules.get().get(AutoEat.class).eating) return;

        if (timer < interval.get()) {
            timer++;
            return;
        }

        BlockPos playerBlockPos = mc.player.blockPosition();

        for (int i = 1; i <= farAhead.get(); i++) {
            BlockPos basePos = playerBlockPos.atY(yLevel).relative(HighwayPaver.diggingDirection, i);

            if (!mushroomsOnly.get()) {
                if (!glowstoneExistsAt(basePos))
                    mine(basePos, false);

                if (!glowstoneExistsAt(basePos.above()))
                    mine(basePos.above(), false);
            }

            BlockPos pavePos = basePos.below();

            mineMushroomIfExists(pavePos);
            if (HighwayPaver.sidePavingEnabled) {
                mineMushroomIfExists(pavePos.relative(HighwayPaver.sideDirection));
                if (HighwayPaver.cornerPavingEnabled) {
                    mineMushroomIfExists(pavePos.relative(HighwayPaver.sideDirection, 2));
                } else {
                    mineMushroomIfExists(pavePos.relative(HighwayPaver.sideDirection.getOpposite()));
                }

                if (HighwayPaver.guardRailsEnabled) {
                    if (HighwayPaver.cornerPavingEnabled) {
                        mineMushroomIfExists(pavePos.above().relative(HighwayPaver.sideDirection, 3));
                        mineMushroomIfExists(pavePos.above().relative(HighwayPaver.sideDirection.getOpposite(), 1));
                    } else {
                        mineMushroomIfExists(pavePos.above().relative(HighwayPaver.sideDirection, 2));
                        mineMushroomIfExists(pavePos.above().relative(HighwayPaver.sideDirection.getOpposite(), 2));
                    }
                }
            }

        }

        timer = 0;
    }

    private void mineMushroomIfExists(BlockPos pos) {
        if (mushroomExistsAt(pos)) {
            mine(pos, false);
        }
    }

    private boolean mushroomExistsAt(BlockPos pos){
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() == Blocks.BROWN_MUSHROOM) return true;
        if (state.getBlock() == Blocks.RED_MUSHROOM) return true;
        if (state.getBlock() == Blocks.TWISTING_VINES) return true;
        if (state.getBlock() == Blocks.TWISTING_VINES_PLANT) return true;
        if (state.getBlock() == Blocks.WEEPING_VINES) return true;
        if (state.getBlock() == Blocks.WEEPING_VINES_PLANT) return true;
        return false;
    }

    private boolean glowstoneExistsAt(BlockPos pos){
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() == Blocks.GLOWSTONE) return true;
        if (state.getBlock() == Blocks.WARPED_WART_BLOCK) return true;
        if (state.getBlock() == Blocks.NETHER_WART_BLOCK) return true;
        return false;
    }

    private void mine(BlockPos blockPos, boolean swing) {
        if (mc.player == null) return;
        if (blockPos == null) return;

        if (packetMine.get()) {
            ClientPacketListener network = mc.getConnection();
            if (network == null) return;
            network.getConnection().send(
                new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, blockPos, HighwayPaver.diggingDirection, sequence),
                null,
                true
            );
        } else {
            BlockUtils.breakBlock(blockPos, swing);
        }
        sequence += 1;
    }
}
