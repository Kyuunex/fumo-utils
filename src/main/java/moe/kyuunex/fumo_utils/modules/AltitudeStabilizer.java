package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.world.phys.Vec3;


public class AltitudeStabilizer extends Module {
    private final SettingGroup sgYLevel = settings.createGroup("Y Level");
    private final SettingGroup sgTakeOff = settings.createGroup("Taking off");
    private final SettingGroup sgSimplePitches = settings.createGroup("Simple pitch adjustment");

    // Y Level
    private final Setting<Double> yNether = sgYLevel.add(new DoubleSetting.Builder()
        .name("the-nether")
        .description("Y level to target in The Nether")
        .defaultValue(121.2)
        .range(-128, 256)
        .sliderRange(96, 160)
        .build()
    );
    private final Setting<Double> yOverworld = sgYLevel.add(new DoubleSetting.Builder()
        .name("the-overworld")
        .description("Y level to target in The Overworld")
        .defaultValue(325)
        .range(-128, 512)
        .sliderRange(96, 386)
        .build()
    );
    private final Setting<Double> yEnd = sgYLevel.add(new DoubleSetting.Builder()
        .name("the-end")
        .description("Y level to target in The End")
        .defaultValue(325)
        .range(-128, 512)
        .sliderRange(96, 386)
        .build()
    );

    // Take off
    private final Setting<Integer> takeOffSpeedThreshold = sgTakeOff.add(new IntSetting.Builder()
        .name("take-off-speed-threshold")
        .description("Bellow what speed is considered still taking off")
        .defaultValue(34)
        .range(0, 400)
        .sliderRange(0, 40)
        .build()
    );

    private final Setting<Double> takeOffPitch = sgTakeOff.add(new DoubleSetting.Builder()
        .name("take-off-pitch")
        .description("Pitch to set when taking off")
        .defaultValue(-6.5)
        .range(-10, 0)
        .sliderRange(-10, 0)
        .build()
    );

    // Simple pitches
    private final Setting<Double> normalPitch = sgSimplePitches.add(new DoubleSetting.Builder()
        .name("normal-pitch")
        .description("Pitch to use normally while flying")
        .defaultValue(-3.5)
        .range(-10, 0)
        .sliderRange(-10, 0)
        .build()
    );

    private final Setting<Double> correctionPitch = sgSimplePitches.add(new DoubleSetting.Builder()
        .name("correction-pitch")
        .description("Pitch to use to stabilize the Y level")
        .defaultValue(-4.6)
        .range(-10, 0)
        .sliderRange(-10, 0)
        .build()
    );

    public AltitudeStabilizer() {
        super(FumoUtils.CATEGORY, "altitude-stabilizer", "Stabilizes the Y level during elytra flight");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (!mc.player.isFallFlying()) {
            return;
        }

        Vec3 velocity = mc.player.getDeltaMovement();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z) * 20;

        if (speed == 0)
            return;

        if (speed < takeOffSpeedThreshold.get()) {
            mc.player.setXRot(takeOffPitch.get().floatValue());
            return;
        }

        Setting<Double> yLevel;

        if (mc.player.clientLevel.dimensionType().respawnAnchorWorks()){
            yLevel = yNether;
        } else if (mc.player.clientLevel.dimensionType().bedWorks()){
            yLevel = yOverworld;
        } else {
            yLevel = yEnd;
        }

        if (mc.player.getY() < yLevel.get()){
            mc.player.setXRot(correctionPitch.get().floatValue());
        } else {
            mc.player.setXRot(normalPitch.get().floatValue());
        }
    }
}
