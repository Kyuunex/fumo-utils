package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;


public class AltitudeStabilizer extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Double> yLevelNether = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-level-in-nether")
        .description("Y level to target in nether")
        .defaultValue(121.2)
        .range(-64, 256)
        .sliderRange(100, 160)
        .build()
    );
    private final Setting<Double> yLevelOwEnd = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-level-in-ow-and-end")
        .description("Y level to target in ow and end")
        .defaultValue(325)
        .range(-100, 400)
        .sliderRange(100, 350)
        .build()
    );
    private final Setting<Double> correctionPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("correction-pitch")
        .description("Pitch to use to stabilize the Y level")
        .defaultValue(-5)
        .range(-10, 0)
        .sliderRange(-10, 0)
        .build()
    );
    private final Setting<Double> normalPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("normal-pitch")
        .description("Pitch to use normally while flying")
        .defaultValue(-3.5)
        .range(-10, 0)
        .sliderRange(-10, 0)
        .build()
    );

    public AltitudeStabilizer() {
        super(FumoUtils.CATEGORY, "altitude-stabilizer", "Stabilizes the Y level during elytra flight");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        assert mc.player != null;
        if (!mc.player.isFallFlying()) {
            return;
        }

        Setting<Double> yLevel;

        if (mc.player.clientLevel.dimensionType().respawnAnchorWorks()){
            yLevel = yLevelNether;
        } else {
            yLevel = yLevelOwEnd;
        }

        if (mc.player.getY() < yLevel.get()){
            mc.player.setXRot(correctionPitch.get().floatValue());
        } else {
            mc.player.setXRot(normalPitch.get().floatValue());
        }
    }
}
