/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

public class FumoVoidESP extends Module {
    private static final Direction[] SIDES = {Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST};

    private final SoundEvent defaultNotificationSound = SoundEvents.AMETHYST_BLOCK_RESONATE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgSound = settings.createGroup("Sound");

    // General

    private final Setting<Boolean> airOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("air-only")
        .description("Checks bedrock only for air blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-radius")
        .description("Horizontal radius in which to search for holes.")
        .defaultValue(64)
        .min(0)
        .sliderMax(256)
        .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
        .name("hole-height")
        .description("The minimum hole height to be rendered.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Boolean> netherRoof = sgGeneral.add(new BoolSetting.Builder()
        .name("nether-roof")
        .description("Check for holes in nether roof.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("fill-color")
        .description("The color that fills holes in the void.")
        .defaultValue(new SettingColor(225, 25, 25, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color to draw lines of holes to the void.")
        .defaultValue(new SettingColor(225, 25, 255))
        .build()
    );


    // Sound

    private final Setting<Boolean> enableSound = sgSound.add(new BoolSetting.Builder()
        .name("enable-sound-notification")
        .description("Enable sound notifications")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<SoundEvent>> soundSetting = sgSound.add(new SoundEventListSetting.Builder()
        .name("notification-sound")
        .description("Notification sound. PICK ONLY ONE!")
        .defaultValue(defaultNotificationSound)
        .build()
    );

    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    private final Pool<Void> voidHolePool = new Pool<>(Void::new);
    private final List<Void> voidHoles = new ArrayList<>();

    private int tickTrack = 0;
    private boolean isNotified = false;

    public FumoVoidESP() {
        super(FumoUtils.CATEGORY, "fumo-void-esp", "Renders holes in bedrock layers that lead to the void.");
    }

    @Override
    public void onActivate() {
        isNotified = false;
    }

    @Override
    public void onDeactivate() {
        isNotified = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        voidHoles.clear();
        if (PlayerUtils.getDimension() == Dimension.End) return;

        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();
        int radius = horizontalRadius.get();

        for (int x = px - radius; x <= px + radius; x++) {
            for (int z = pz - radius; z <= pz + radius; z++) {
                blockPos.set(x, mc.level.getMinY(), z);
                if (isHole(blockPos, false)) voidHoles.add(voidHolePool.get().set(blockPos.set(x, mc.level.getMinY(), z), false));

                // Check for nether roof
                if (netherRoof.get() && PlayerUtils.getDimension() == Dimension.Nether) {
                    blockPos.set(x, 127, z);
                    if (isHole(blockPos, true)) voidHoles.add(voidHolePool.get().set(blockPos.set(x, 127, z), true));
                }
            }
        }

        if (!voidHoles.isEmpty()){
            if (enableSound.get()){
                if (mc.level == null) return;

                SoundEvent notificationSound;

                if(soundSetting.get().isEmpty()) {
                    notificationSound = defaultNotificationSound;
                } else {
                    notificationSound = soundSetting.get().getFirst();
                }

                if(tickTrack == 0 || tickTrack == 3 || tickTrack == 6 || tickTrack == 9){
                    mc.level.playSound(
                        mc.player, mc.player,
                        notificationSound, SoundSource.VOICE,
                        3.0F, 1.0F
                    );
                }

                tickTrack++;
                if(tickTrack == 20) tickTrack = 0;
            }

            if(!isNotified) {
                info(Component.literal("Void Holes found!"));
                isNotified = true;
            }
        } else {
            isNotified = false;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Void voidHole : voidHoles) voidHole.render(event);
    }

    private boolean isBlockWrong(BlockPos blockPos) {
        ChunkAccess chunk = mc.level.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null) return true;

        Block block = chunk.getBlockState(blockPos).getBlock();

        if (airOnly.get()) return block != Blocks.AIR;
        return block == Blocks.BEDROCK;
    }

    private boolean isHole(BlockPos.MutableBlockPos blockPos, boolean nether) {
        for (int i = 0; i < holeHeight.get(); i++) {
            blockPos.setY(nether ? 127 - i : mc.level.getMinY());
            if (isBlockWrong(blockPos)) return false;
        }

        return true;
    }

    private class Void {
        private int x, y, z;
        private int excludeDir;

        public Void set(BlockPos.MutableBlockPos blockPos, boolean nether) {
            x = blockPos.getX();
            y = blockPos.getY();
            z = blockPos.getZ();

            excludeDir = 0;

            for (Direction side : SIDES) {
                blockPos.set(x + side.getStepX(), y, z + side.getStepZ());
                if (isHole(blockPos, nether)) excludeDir |= Dir.get(side);
            }

            return this;
        }

        public void render(Render3DEvent event) {
            event.renderer.box(x, y, z, x + 1, y + 1, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), excludeDir);
        }
    }
}
