package moe.kyuunex.fumo_utils.modules;

import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

public class MapHighlighter extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Double> yLevel = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-level")
        .description("Y level to render the line at")
        .defaultValue(63)
        .range(-100, 400)
        .sliderRange(62, 325)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines.")
        .defaultValue(new Color(255, 255, 255, 192))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides.")
        .defaultValue(new Color(255, 255, 255, 32))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Lines)
        .build()
    );

    public MapHighlighter() {
        super(FumoUtils.CATEGORY, "map-highlighter", "Mapart area highlighter");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null) return;
        Vec3 pos = mc.player.position();

        event.renderer.box(
            getCenter(pos.x) - 64, yLevel.get(), getCenter(pos.z) - 64,
            getCenter(pos.x) + 64, yLevel.get() + 1, getCenter(pos.z) + 64,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0
        );
    }

    private int getCenter(double i){
        return (int)Math.ceil((i - 64) / 128) * 128;
    }
}
