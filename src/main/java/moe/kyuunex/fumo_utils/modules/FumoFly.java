package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;


public class FumoFly extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgPCSettings = settings.createGroup("Pitch Check");

    private final Setting<Integer> speedDelay = sgGeneral.add(new IntSetting.Builder()
        .name("speed-delay")
        .description("How often to accelerate (in ticks)")
        .range(0, 200)
        .sliderRange(0, 8)
        .defaultValue(2)
        .build()
    );

    private final Setting<Double> speedStep = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-step")
        .description("How much to accelerate by")
        .range(0, 40)
        .sliderRange(0, 8)
        .defaultValue(1)
        .build()
    );

    private final Setting<Double> startSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("start-speed")
        .description("Start speed in BPS")
        .defaultValue(15)
        .range(1, 1000)
        .sliderRange(1, 50)
        .build()
    );

    private final Setting<Double> maxSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-speed")
        .description("Max speed in BPS")
        .defaultValue(40)
        .range(1, 100000)
        .sliderRange(15, 130)
        .build()
    );

    private final Setting<Boolean> pitchCheck = sgPCSettings.add(new BoolSetting.Builder()
        .name("pitch-check")
        .description("Don't accelerate when aiming up")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> pitchRecovery = sgPCSettings.add(new IntSetting.Builder()
        .name("pitch-recovery")
        .description("How long to wait until we start accelerating after aiming downwards")
        .defaultValue(1)
        .range(0, 2000)
        .sliderRange(0, 100)
        .visible(pitchCheck::get)
        .build()
    );

    private final Setting<Boolean> pitchRecoveryResetSpeed = sgPCSettings.add(new BoolSetting.Builder()
        .name("pitch-recovery-reset-speed")
        .description("Reset the speed when aiming downwards")
        .defaultValue(true)
        .visible(pitchCheck::get)
        .build()
    );

    private final Setting<Double> pitchRecoveryStartSpeed = sgPCSettings.add(new DoubleSetting.Builder()
        .name("pitch-recovery-start-speed")
        .description("Start speed in BPS after aiming downwards")
        .defaultValue(1)
        .range(1, 1000)
        .sliderRange(1, 50)
        .visible(pitchCheck::get)
        .build()
    );

    private double speed;
    private int accelTimer;
    Vec3 velocity;

    public FumoFly() {
        super(FumoUtils.CATEGORY, "fumo-fly", "Boost type Elytra Fly");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        if (mc.player.isFallFlying()) {
            velocity = mc.player.getDeltaMovement();
            speed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z) * 20;
        } else {
            speed = startSpeed.get();
        }

        accelTimer = 0;
    }

    @Override
    public void onDeactivate() {
        speed = startSpeed.get();
        accelTimer = 0;
    }

    @Override
    public String getInfoString() {
        return String.format("%.2f", speed);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        accelTimer++;
    }

    @EventHandler(priority = 6969)
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null) return;

        if (!(mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER))) return;

        if (!mc.player.isFallFlying() || mc.player.isInLava() || mc.player.isInWater()){
            speed = startSpeed.get();
            accelTimer = 0;
            return;
        }

        if (pitchCheck.get() && mc.player.getXRot() <= 0){
            if (pitchRecoveryResetSpeed.get()) {
                speed = pitchRecoveryStartSpeed.get();
            } else {
                velocity = mc.player.getDeltaMovement();
                speed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z) * 20;
            }
            accelTimer = -1 * pitchRecovery.get();
            return;
        }

        if (accelTimer >= speedDelay.get()) {
            if (speed >= maxSpeed.get())
                speed = maxSpeed.get();
            else
                speed += speedStep.get();

            accelTimer = 0;
        }

        double yaw = mc.player.getYRot();

        event.movement.x = (speed / 20d) * Math.cos(Math.toRadians(yaw + 90d));
        event.movement.z = (speed / 20d) * Math.sin(Math.toRadians(yaw + 90d));

        mc.player.setDeltaMovement(event.movement);
    }
}
