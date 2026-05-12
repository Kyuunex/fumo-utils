package moe.kyuunex.fumo_utils.modules.highwaytools;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import moe.kyuunex.fumo_utils.utils.DisconnectUtils;
import moe.kyuunex.fumo_utils.utils.InventoryUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class HighwayPaver extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgInventorySettings = settings.createGroup("Inventory");
    private final SettingGroup sgSafeWalkSettings = settings.createGroup("Safe Walk");
    private final SettingGroup sgExperimentalSettings = settings.createGroup("Experimental");

    private final Setting<Integer> interval = sgDefault.add(new IntSetting.Builder()
        .name("interval")
        .description("How long to wait between placing bursts")
        .defaultValue(1)
        .sliderRange(0, 100)
        .range(-1, 1000)
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
        .description("Y Level to place on.")
        .sliderRange(62, 320)
        .range(-64, 320)
        .defaultValue(118)
        .visible(forceYLevelEnable::get)
        .build()
    );

    private final Setting<Boolean> forcedDirectionEnable = sgDefault.add(new BoolSetting.Builder()
        .name("forced-direction")
        .description("Force digging direction instead of guessing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Direction> forcedDirection = sgDefault.add(new EnumSetting.Builder<Direction>()
        .name("forced-direction")
        .description("In which direction are you digging?")
        .defaultValue(Direction.WEST)
        .visible(forcedDirectionEnable::get)
        .build()
    );

    private final Setting<Boolean> sideBlocksEnable = sgDefault.add(new BoolSetting.Builder()
        .name("place-blocks-on-side")
        .description("3 blocks wide instead of 1.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Direction> sideDirection = sgDefault.add(new EnumSetting.Builder<Direction>()
        .name("side-direction")
        .description("Which direction is your right or left. "
            + "If you are walking North, you select East or West, "
            + "and East and West in front of you is paved. "
            + "In Corner Paving mode, only the selected direction is paved, 2 blocks.")
        .defaultValue(Direction.WEST)
        .visible(sideBlocksEnable::get)
        .build()
    );

    private final Setting<Integer> howFarAhead = sgDefault.add(new IntSetting.Builder()
        .name("how-far-ahead")
        .description("How far ahead to place the blocks?")
        .sliderRange(0, 6)
        .defaultValue(2)
        .build()
    );

    private final Setting<Boolean> packet = sgDefault.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Packet place instead of normal place. Recommended so you don't fall off.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airPlace = sgDefault.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Literally air place. Doesn't work on 9b9t.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgDefault.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Only places blocks in this list.")
        .build()
    );

    private final Setting<Boolean> offhand = sgInventorySettings.add(new BoolSetting.Builder()
        .name("offhand")
        .description("Use the offhand for holding blocks. Highly recommended to avoid inventory desync.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> offhandReplenish = sgInventorySettings.add(new BoolSetting.Builder()
        .name("offhand-replenish")
        .description("Replenish the offhand slot with blocks.")
        .defaultValue(true)
        .visible(offhand::get)
        .build()
    );

    private final Setting<Integer> replenishWhenBelow = sgInventorySettings.add(new IntSetting.Builder()
        .name("replenish-when-below")
        .description("")
        .sliderRange(0, 64)
        .defaultValue(32)
        .visible(offhand::get)
        .build()
    );

    private final Setting<Boolean> disconnectWhenCantReplenish = sgInventorySettings.add(new BoolSetting.Builder()
        .name("disconnect-when-cant-replenish")
        .description("")
        .defaultValue(true)
        .visible(offhand::get)
        .build()
    );

    private final Setting<Integer> dedicatedSlot = sgInventorySettings.add(new IntSetting.Builder()
        .name("dedicated-slot")
        .description("The hotbar slot to use for blocks.")
        .sliderRange(0, 8)
        .defaultValue(7)
        .visible(() -> !offhand.get())
        .build()
    );

    private final Setting<Boolean> fumoSafeWalk = sgSafeWalkSettings.add(new BoolSetting.Builder()
        .name("fumo-safe-walk")
        .description("Make sure fumo doesnt fly off the edge!")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> safeTimer = sgSafeWalkSettings.add(new DoubleSetting.Builder()
        .name("safe-timer")
        .description("Speed to slow down to.")
        .range(0, 10)
        .sliderRange(0, 4.4)
        .defaultValue(0.6)
        .visible(fumoSafeWalk::get)
        .build()
    );

    private final Setting<Double> regularTimer = sgSafeWalkSettings.add(new DoubleSetting.Builder()
        .name("regular-timer")
        .description("Normal walk timer speed.")
        .range(0, 10)
        .sliderRange(0, 4.4)
        .defaultValue(4.4)
        .visible(fumoSafeWalk::get)
        .build()
    );

    private final Setting<Boolean> grimDesyncFix = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("grim-desync-fix")
        .description("Place a block in front of you, to force grim resync")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> cornerPaveEnable = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("corner-pave")
        .description("Instead of paving 1 block in each direction, you are walking at the corner and paving 2 blocks in only one direction.")
        .defaultValue(false)
        .build()
    );

    private int timer = -1;
    private int sequence = 0;
    private final Map<BlockPos, Long> placedBlocks = new ConcurrentHashMap<>();
    private int delayTimer = 0;
    private int yLevel = 118;
    private Direction diggingDirection = Direction.EAST;

    private static final double MAGIC_PLACE_OFFSET = 0.0154;

    public HighwayPaver() {
        super(
            FumoUtils.CATEGORY,
            "highway-paver",
            "Specialized scaffold module to pave a tunnel."
        );
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        if (forcedDirectionEnable.get()) {
            diggingDirection = forcedDirection.get();
        } else {
            diggingDirection = mc.player.getDirection();
        }

        if (forceYLevelEnable.get()) {
            yLevel = forcedYLevel.get();
        } else {
            yLevel = mc.player.getBlockY() - 1;
        }
    }

    @Override
    public void onDeactivate() {
        Timer timerMod = Modules.get().get(Timer.class);
        if (timerMod == null) return;
        timerMod.setOverride(1);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof ServerboundAcceptTeleportationPacket)) return;
        info("Rubber banding detected?");

        if (grimDesyncFix.get()) {
            BlockPos currentBlockPos = mc.player.blockPosition();
            placeBlock(currentBlockPos.relative(diggingDirection), false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (Modules.get().get(AutoGap.class).isEating()) return;
        if (Modules.get().get(AutoEat.class).eating) return;

        if (fumoSafeWalk.get()) {
            BlockPos currentBlockPos = mc.player.blockPosition();
            boolean fwClear = canWalkOn(currentBlockPos.atY(yLevel).relative(diggingDirection));
            boolean sideClear;
            boolean side2Clear;

            if (sideBlocksEnable.get()){
                sideClear = canWalkOn(currentBlockPos.atY(yLevel).relative(diggingDirection).relative(sideDirection.get()));

                if (cornerPaveEnable.get()) {
                    side2Clear = canWalkOn(currentBlockPos.atY(yLevel).relative(diggingDirection).relative(sideDirection.get()).relative(sideDirection.get()));
                } else {
                    side2Clear = canWalkOn(currentBlockPos.atY(yLevel).relative(diggingDirection).relative(sideDirection.get().getOpposite()));
                }
            } else {
                sideClear = true;
                side2Clear = true;
            }

            if (fwClear && sideClear && side2Clear) {
                Timer timerMod = Modules.get().get(Timer.class);
                timerMod.setOverride(regularTimer.get());
            } else {
                Timer timerMod = Modules.get().get(Timer.class);
                timerMod.setOverride(safeTimer.get());
            }
        }

        if (timer < interval.get()) {
            timer++;
            return;
        }

        if (offhand.get() && offhandReplenish.get() && mc.player.getOffhandItem().getCount() < replenishWhenBelow.get())
        {
            FindItemResult results = InvUtils.find(stack ->
                whitelist.get().stream().anyMatch(block -> block.asItem() == stack.getItem()));
            if (results.found()) {
                // InvUtils.move().from(results.slot()).to(40);
                InventoryUtils.swapToHotbar(results.slot(), 40);
            } else {
                if (disconnectWhenCantReplenish.get()) {
                    ClientPacketListener network = mc.getConnection();
                    DisconnectUtils.disconnect(network, "cannot replenish blocks, none found in inventory!");
                } else {
                    info("cannot replenish blocks, none found in inventory!");
                }
            }
        }

        BlockPos currentBlockPos = mc.player.blockPosition();

        for (int i = 0; i <= howFarAhead.get(); i++) {
            placeBlock(currentBlockPos.atY(yLevel).relative(diggingDirection, i), packet.get());
            if (sideBlocksEnable.get()) {
                placeBlock(currentBlockPos.relative(sideDirection.get())
                    .atY(yLevel)
                    .relative(diggingDirection, i), packet.get());
                if (cornerPaveEnable.get()) {
                    placeBlock(currentBlockPos.relative(sideDirection.get()).relative(sideDirection.get())
                        .atY(yLevel)
                        .relative(diggingDirection, i), packet.get());
                } else {
                    placeBlock(currentBlockPos.relative(sideDirection.get().getOpposite())
                        .atY(yLevel)
                        .relative(diggingDirection, i), packet.get());
                }
            }
        }


        timer = 0;
    }

    private boolean placeBlock(BlockPos pos, boolean packetPlace) {
        if (mc.gameMode == null || mc.player == null) return false;

        if (!isPlacable(pos)) return false;

        InteractionHand handToUse = null;
        if (!offhand.get()) {
            // Find suitable block
            FindItemResult item = InvUtils.find(stack ->
                whitelist.get().stream().anyMatch(block -> block.asItem() == stack.getItem()));

            if (!item.found()) return false;
            // Handle inventory switching
            if (!handleInventory(item)) return false;
            handToUse = item.getHand();
        } else {
            handToUse = InteractionHand.OFF_HAND;
        }

        boolean placed = false;

        // Place the block
        if (packetPlace) {
            ClientPacketListener network = mc.getConnection();
            if (network == null) return false;
            network.getConnection().send(
                new ServerboundUseItemOnPacket(handToUse, getSafeHitResult(pos), 0),
                null,
                true
            );
            placed = true;
        } else {
            placed = mc.gameMode.useItemOn(mc.player, handToUse,
                getSafeHitResult(pos)).consumesAction();
        }
        if (placed) {
            // Track placed block
            placedBlocks.put(pos, System.currentTimeMillis());
        }

        return placed;
    }

    private boolean isPlacable(BlockPos pos){
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.liquid()) return true;
        if (state.getBlock() == Blocks.FIRE) return true;
        if (state.isSolid()) return false;
        return false;
    }

    private boolean canWalkOn(BlockPos pos){
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.liquid()) return false;
        if (state.isSolid()) return true;
        return false;
    }

    private static BlockHitResult getPerfectHitRes(BlockPos pos) {
        BlockPos neighbour = new BlockPos(pos.getX(), pos.getY()-1, pos.getZ());
        return new BlockHitResult(
            Vec3.atCenterOf(neighbour),
            Direction.UP,
            neighbour,
            false
        );
    }

    private boolean handleInventory(FindItemResult item) {
        if (item.isOffhand()) return true;

        if (!item.isHotbar()) {
            InventoryUtils.swapToHotbar(item.slot(), dedicatedSlot.get());
            delayTimer = 2; // Small delay after inventory operation
            return false;
        }

        if (mc.player.getInventory().getSelectedSlot() != item.slot()) {
            InventoryUtils.swapSlot(item.slot());
            delayTimer = 1;
            return false;
        }

        return true;
    }

    public BlockHitResult getSafeHitResult(BlockPos pos) {
        if (airPlace.get()){
            return new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
            );
        }

        BlockPos.MutableBlockPos mutable = pos.mutable();
        Direction direction = Direction.UP;

        Vec3 offset;
        double yHeight = 0;

        for (Direction dir : getDirections()) {
            // Performance!
            mutable.set(pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ());
            if (!isReplaceable(mutable)) {
                yHeight = getHeight(mutable);

                direction = dir;

                if (dir == Direction.DOWN) {
                    break;
                }
            }
        }

        offset = clickOffset(pos, direction);

        if (yHeight <= 0.2) {
            offset = new Vec3(offset.x, Math.floor(offset.y) + MAGIC_PLACE_OFFSET, offset.z);
        }

        return new BlockHitResult(offset, direction.getOpposite(), mutable.set(pos.getX() + direction.getStepX(), pos.getY() + direction.getStepY(), pos.getZ() + direction.getStepZ()), false);
    }

    public boolean isReplaceable(BlockPos pos) {
        return mc.player != null && (mc.player.level().getBlockState(pos).isAir()
            || mc.player.level().getBlockState(pos).canBeReplaced()
            || isLiquid(pos));
    }

    public boolean isLiquid(BlockPos pos) {
        return mc.player != null
            && mc.player.level().getBlockState(pos).getBlock() instanceof LiquidBlock;
    }

    public Vec3 clickOffset(BlockPos pos) {
        return clickOffset(pos, getPlaceDirection(pos));
    }

    public Vec3 clickOffset(BlockPos pos, Direction direction) {
        return Vec3.atCenterOf(pos).add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5);
    }

    public Direction getPlaceDirection(BlockPos pos) {
        for (Direction direction : getDirections()) {
            if (isReplaceable(pos.relative(direction))) return direction;
        }
        return Direction.UP;
    }

    public Direction[] getDirections() {
        return Direction.values();
    }

    public double getHeight(BlockPos pos) {
        return mc.level.getBlockState(pos).getShape(mc.level, pos).max(Direction.Axis.Y);
    }
}
