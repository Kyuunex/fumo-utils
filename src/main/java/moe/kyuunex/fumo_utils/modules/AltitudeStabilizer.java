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
    private final SettingGroup sgAdaptivePitches = settings.createGroup("Adaptive pitch adjustment");

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

    // Adaptive
    private final Setting<Boolean> adaptivePitchAdjustment = sgAdaptivePitches.add(new BoolSetting.Builder()
        .name("adaptive-pitch-adjustment")
        .description("Should the pitch adjustments be smooth or instant?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> dramaticPitchDelta = sgAdaptivePitches.add(new DoubleSetting.Builder()
        .name("dramatic-pitch-delta")
        .description("By how much to dramatically change the pitch")
        .defaultValue(0.5)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );
    private final Setting<Double> subtlePitchDelta = sgAdaptivePitches.add(new DoubleSetting.Builder()
        .name("subtle-pitch-delta")
        .description("By how much to subtly change the pitch")
        .defaultValue(0.15)
        .range(0, 1)
        .sliderRange(0, 1)
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

        if (!adaptivePitchAdjustment.get()){
            if (mc.player.getY() < yLevel.get()){
                mc.player.setXRot(correctionPitch.get().floatValue());
            } else {
                mc.player.setXRot(normalPitch.get().floatValue());
            }
            return;
        }

        if (mc.player.getY() < yLevel.get()){
            if(yLevel.get() - mc.player.getY() <= 1)
                mc.player.setXRot(getPerfectPitch(speed) - subtlePitchDelta.get().floatValue());
            else if(yLevel.get() - mc.player.getY() > 1)
                mc.player.setXRot(getPerfectPitch(speed) - dramaticPitchDelta.get().floatValue());
        } else {
            mc.player.setXRot(getPerfectPitch(speed));
        }
    }

    private float getPerfectPitch(double speed){
        if (speed <= 33)
            return -5.009f;
        else if (speed <= 34)
            return -4.855f;
        else if (speed <= 35)
            return -4.707f;
        else if (speed <= 36)
            return -4.570f;
        else if (speed <= 37)
            return -4.443f;
        else if (speed <= 38)
            return -4.323f;
        else if (speed <= 39)
            return -4.207f;
        else if (speed <= 40)
            return -4.097f;
        else if (speed <= 41)
            return -3.993f;
        else if (speed <= 42)
            return -3.900f;
        else if (speed <= 43)
            return -3.806f;
        else if (speed <= 44)
            return -3.713f;
        else if (speed <= 45)
            return -3.630f;
        else if (speed <= 46)
            return -3.548f;
        else if (speed <= 47)
            return -3.471f;
        else if (speed <= 48)
            return -3.400f;
        else if (speed <= 49)
            return -3.328f;
        else if (speed <= 50)
            return -3.257f;
        else if (speed <= 51)
            return -3.197f;
        else if (speed <= 52)
            return -3.131f;
        else if (speed <= 53)
            return -3.070f;
        else if (speed <= 54)
            return -3.015f;
        else if (speed <= 55)
            return -2.960f;
        else if (speed <= 60)
            return -2.708f;
        else if (speed <= 65)
            return -2.499f;
        else if (speed <= 70)
            return -2.318f;
        else if (speed <= 75)
            return -2.158f;
        else if (speed <= 80)
            return -2.026f;
        else if (speed <= 85)
            return -1.906f;
        else if (speed <= 90)
            return -1.801f;
        else if (speed <= 95)
            return -1.702f;
        else if (speed <= 100)
            return -1.620f;
        else if (speed <= 105)
            return -1.543f;
        else if (speed <= 110)
            return -1.472f;
        else if (speed <= 115)
            return -1.406f;
        else if (speed <= 120)
            return -1.345f;
        else if (speed <= 125)
            return -1.296f;
        else if (speed <= 130)
            return -1.246f;
        else if (speed <= 135)
            return -1.197f;
        else if (speed <= 140)
            return -1.153f;
        else if (speed <= 145)
            return -1.115f;
        else if (speed <= 150)
            return -1.076f;
        else if (speed <= 155)
            return -1.043f;
        else if (speed <= 200)
            return -0.807f;
        else if (speed <= 250)
            return -0.648f;
        else if (speed <= 300)
            return -0.538f;
        else if (speed <= 345)
            return -0.472f;
        return 0;
    }
}
