package moe.kyuunex.fumo_utils.modules.highwaytools;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.level.block.state.BlockState;

public class HighwayWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSafeWalkSettings = settings.createGroup("Safe Walk");
    private final SettingGroup sgExperimentalSettings = settings.createGroup("Experimental");

    private final Setting<Boolean> waitForChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-unloaded-chunks")
        .description("Do not allow movement into unloaded chunks")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> howFarAheadEnsure = sgGeneral.add(new IntSetting.Builder()
        .name("how-far-ahead-ensure")
        .description("Don't move until this far blocks are mined")
        .sliderRange(0, 6)
        .defaultValue(3)
        .build()
    );

    private final Setting<Integer> howFarBehindEnsure = sgGeneral.add(new IntSetting.Builder()
        .name("how-far-behined-ensure")
        .description("Also ensure blocks are mined behind the player. 0 to disable.")
        .sliderRange(-6, 0)
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> yLevel = sgGeneral.add(new IntSetting.Builder()
        .name("y-level")
        .description("")
        .defaultValue(119)
        .range(-500, 20000)
        .sliderRange(50, 130)
        .build()
    );

    private final Setting<Boolean> pauseWhenEating = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-when-eating")
        .description("As it says.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> eatingStuckFixEnable = sgGeneral.add(new BoolSetting.Builder()
        .name("fix-eating-getting-stuck")
        .description("As it says.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> eatingStuckFixTime = sgGeneral.add(new IntSetting.Builder()
        .name("fix-after-eating-for-x-ticks")
        .description("How long to wait before attempting to unstuck eating.")
        .sliderRange(0, 2000)
        .defaultValue(500)
        .visible(eatingStuckFixEnable::get)
        .build()
    );

    private final Setting<Integer> eatingStuckFixSlot = sgGeneral.add(new IntSetting.Builder()
        .name("eating-resync-slot")
        .description("Which slot to try to resync eating too.")
        .sliderRange(0, 8)
        .defaultValue(8)
        .visible(eatingStuckFixEnable::get)
        .build()
    );

    private final Setting<Boolean> pauseWhenHighway = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-when-unmined")
        .description("Stop when not fully mined in front.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> fumoSafeWalk = sgSafeWalkSettings.add(new BoolSetting.Builder()
        .name("fumo-safe-walk")
        .description("Make sure fumo doesnt fly off the edge!")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> howFarAheadSafe = sgSafeWalkSettings.add(new IntSetting.Builder()
        .name("how-far-ahead-safe")
        .description("How far ahead to check for safe to walk blocks?")
        .sliderRange(0, 6)
        .defaultValue(3)
        .visible(fumoSafeWalk::get)
        .build()
    );

    private final Setting<Boolean> sneakWhenSlowingDown = sgSafeWalkSettings.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Try sneaking while slowing down. Decreases chances of falling off.")
        .defaultValue(true)
        .visible(fumoSafeWalk::get)
        .build()
    );

    private final Setting<Boolean> noFwdWhenSlowingDown = sgSafeWalkSettings.add(new BoolSetting.Builder()
        .name("unpress-forward")
        .description("Just stop pressing forward to slow down.")
        .defaultValue(false)
        .visible(fumoSafeWalk::get)
        .build()
    );

    private final Setting<Boolean> timerWhenSlowingDown = sgSafeWalkSettings.add(new BoolSetting.Builder()
        .name("timer-adjust")
        .description("Slow down the game when slowing down.")
        .defaultValue(true)
        .visible(fumoSafeWalk::get)
        .build()
    );

    private final Setting<Double> safeTimer = sgSafeWalkSettings.add(new DoubleSetting.Builder()
        .name("safe-timer")
        .description("Speed to slow down to.")
        .range(0, 10)
        .sliderRange(0, 4.4)
        .defaultValue(0.5)
        .visible(() -> fumoSafeWalk.get() && timerWhenSlowingDown.get())
        .build()
    );

    private final Setting<Double> regularTimer = sgSafeWalkSettings.add(new DoubleSetting.Builder()
        .name("regular-timer")
        .description("Normal walk timer speed.")
        .range(0, 10)
        .sliderRange(0, 4.4)
        .defaultValue(1)
        .visible(() -> fumoSafeWalk.get() && timerWhenSlowingDown.get())
        .build()
    );

    private final Setting<Boolean> lagbackPauseEnable = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("lagback-pause-enable")
        .description("Pause moving temporarily when getting lagback.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> lagbackPauseDuration = sgExperimentalSettings.add(new IntSetting.Builder()
        .name("lagback-pause-duration")
        .description("Amount of ticks to pause when lagback.")
        .sliderRange(0, 4000)
        .defaultValue(80)
        .visible(lagbackPauseEnable::get)
        .build()
    );

    public HighwayWalk() {
        super(FumoUtils.HIGHWAY, "highway-walk", "Meteor's Auto Walk forked for highway building.");
    }

    private int eatingTimePassed = 0;
    private boolean keepMoving = true;
    private int walkingCooldown = 0;


    @Override
    public void onDeactivate() {
        mc.options.keyUp.setDown(false);
        mc.options.keyShift.setDown(false);
        if (timerWhenSlowingDown.get()) {
            Timer timerMod = Modules.get().get(Timer.class);
            if (timerMod == null)
                return;
            timerMod.setOverride(1);
        }
        eatingTimePassed = 0;
        walkingCooldown = 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null) return;
        if (mc.player == null) return;
        if (mc.getConnection() == null) return;

        if (mc.player.getBlockY() - yLevel.get() > 2) {
            toggle();
            return;
        }

        if (HighwayAligner.misaligned) {
            info("misaligned, stopping...");
            mc.options.keyUp.setDown(false);
            return;
        }

        if (HighwayPaver.stopMovement) {
            info("can't replenish, stopping movement...");
            mc.options.keyUp.setDown(false);
            return;
        }

        if (walkingCooldown > 0) {
            walkingCooldown--;
            mc.options.keyUp.setDown(false);
            return;
        }

        if (fumoSafeWalk.get()) {
            BlockPos currentBlockPos = mc.player.blockPosition();
            boolean allClear = true;

            for (int i = 0; i <= howFarAheadSafe.get(); i++) {

                BlockPos forwardPos =
                    currentBlockPos.atY(HighwayPaver.yLevel).relative(HighwayPaver.diggingDirection, i);

                boolean fwClear = canWalkOn(forwardPos);

                boolean sideClear = true;
                boolean side2Clear = true;

                boolean sideClearRail = true;
                boolean side2ClearRail = true;

                if (HighwayPaver.sidePavingEnabled) {
                    sideClear = canWalkOn(
                        forwardPos.relative(HighwayPaver.sideDirection)
                    );
                    if (HighwayPaver.cornerPavingEnabled) {
                        side2Clear = canWalkOn(
                            forwardPos.relative(HighwayPaver.sideDirection, 2)
                        );
                    } else {
                        side2Clear = canWalkOn(
                            forwardPos.relative(HighwayPaver.sideDirection.getOpposite())
                        );
                    }
                }

                if (HighwayPaver.guardRailsEnabled) {
                    BlockPos railPos = currentBlockPos.atY(HighwayPaver.yLevel + 1).relative(HighwayPaver.diggingDirection, i);
                    if (HighwayPaver.cornerPavingEnabled) {
                        sideClearRail = canWalkOn(
                            railPos.relative(HighwayPaver.sideDirection, 3)
                        );
                        side2ClearRail = canWalkOn(
                            railPos.relative(HighwayPaver.sideDirection.getOpposite(), 1)
                        );
                    } else {
                        sideClearRail = canWalkOn(
                            railPos.relative(HighwayPaver.sideDirection, 2)
                        );
                        side2ClearRail = canWalkOn(
                            railPos.relative(HighwayPaver.sideDirection.getOpposite(), 2)
                        );
                    }
                }

                // if ANY check fails, stop immediately
                if (!(fwClear && sideClear && side2Clear && sideClearRail && side2ClearRail)) {
                    allClear = false;
                    break;
                }
            }

            if (allClear) {
                if (timerWhenSlowingDown.get()) {
                    Timer timerMod = Modules.get().get(Timer.class);
                    timerMod.setOverride(regularTimer.get());
                }
                if (sneakWhenSlowingDown.get()) {
                    mc.options.keyShift.setDown(false);
                }
                if (noFwdWhenSlowingDown.get()) {
                    mc.options.keyUp.setDown(true);
                }
                keepMoving = true;
            } else {
                if (timerWhenSlowingDown.get()) {
                    Timer timerMod = Modules.get().get(Timer.class);
                    timerMod.setOverride(safeTimer.get());
                }
                if (sneakWhenSlowingDown.get()) {
                    mc.options.keyShift.setDown(true);
                }
                if (noFwdWhenSlowingDown.get()) {
                    mc.options.keyUp.setDown(false);
                    keepMoving = false;
                }
            }
        }

        if (pauseWhenEating.get()) {
            if (eatingStuckFixEnable.get() && eatingTimePassed > eatingStuckFixTime.get()) {
                info("eating for too long. attempting to reset eating.");
                mc.player.getInventory().setSelectedSlot(eatingStuckFixSlot.get());
                mc.getConnection().getConnection().send(
                    new ServerboundSetCarriedItemPacket(eatingStuckFixSlot.get()),
                    null,
                    true
                );
                eatingTimePassed = 0;
            }
            if (Modules.get().get(AutoGap.class).isEating() || Modules.get().get(AutoEat.class).eating) {
                mc.options.keyUp.setDown(false);
                eatingTimePassed += 1;
                return;
            } else {
                eatingTimePassed = 0;
            }
        }

        if (pauseWhenHighway.get()){
            net.minecraft.core.Direction fwDir = mc.player.getDirection();
            BlockPos basePos = mc.player.blockPosition().atY(yLevel.get());

            boolean blocked = false;
            for (int forward = howFarBehindEnsure.get(); forward <= howFarAheadEnsure.get() && !blocked; forward++) {
                int minY = (forward == 0) ? 2 : 0; // don't check where player already is
                for (int up = minY; up <= 3; up++) {
                    BlockPos checkPos = basePos.relative(fwDir, forward).above(up);
                    if (!mc.level.getBlockState(checkPos).isAir()) {
                        blocked = true;
                        break;
                    }
                    if (HighwayPaver.sidePavingEnabled) {
                        BlockPos checkPosSide = basePos.relative(fwDir, forward).above(up)
                            .relative(HighwayPaver.sideDirection);
                        if (!mc.level.getBlockState(checkPosSide).isAir()) {
                            blocked = true;
                            break;
                        }
                        if (HighwayPaver.cornerPavingEnabled) {
                            BlockPos checkPosSide2 = basePos.relative(fwDir, forward).above(up)
                                .relative(HighwayPaver.sideDirection, 2);
                            if (!mc.level.getBlockState(checkPosSide2).isAir()) {
                                blocked = true;
                                break;
                            }
                        } else {
                            BlockPos checkPosSide2 = basePos.relative(fwDir, forward).above(up)
                                .relative(HighwayPaver.sideDirection.getOpposite());
                            if (!mc.level.getBlockState(checkPosSide2).isAir()) {
                                blocked = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (blocked) {
                mc.options.keyUp.setDown(false);
                return;
            }
        }

        if (keepMoving) {
            mc.options.keyUp.setDown(true);
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (waitForChunks.get()) {
            int chunkX = (int) ((mc.player.getX() + event.movement.x * 2) / 16);
            int chunkZ = (int) ((mc.player.getZ() + event.movement.z * 2) / 16);
            if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                event.movement.x = 0;
                event.movement.z = 0;
            }
        }
    }

    private boolean canWalkOn(BlockPos pos){
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.liquid()) return false;
        if (state.isSolid()) return true;
        return false;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof ServerboundAcceptTeleportationPacket)) return;

        if (lagbackPauseEnable.get()) {
            info("Rubber banding detected?");
            walkingCooldown = lagbackPauseDuration.get();
        }
    }
}
