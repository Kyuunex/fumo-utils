package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.settings.*;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class RegionFileHighlighter extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> feetYLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("render-at-feet-y-level-instead")
        .description("Render the shape at feet y-level instead of the set y-level")
        .defaultValue(false)
        .build()
    );

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

    public RegionFileHighlighter() {
        super(FumoUtils.CATEGORY, "region-file-highlighter", "Highlights the area for the current region file");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        assert mc.player != null;
        double yLevelToUse;
        Vec3d pos = mc.player.getPos();

        if (feetYLevel.get()) {
            yLevelToUse = pos.y - 1;
        } else {
            yLevelToUse = yLevel.get();
        }

        event.renderer.box(
            getEdge(pos.x), yLevelToUse, getEdge(pos.z),
            getEdge(pos.x) - 512, yLevelToUse + 1, getEdge(pos.z) - 512,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0
        );
    }

    private int getEdge(double i){
        return (int)Math.ceil(i / 512) * 512;
    }
}
