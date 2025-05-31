package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.settings.*;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

public class AreaHighlighter extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<AreaType> areaType = sgGeneral.add(new EnumSetting.Builder<AreaType>()
        .name("area-type")
        .description("MapArt or Region file?")
        .defaultValue(AreaType.MapArt)
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

    private final Setting<Boolean> feetYLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("render-at-feet-y-level-instead")
        .description("Render the shape at feet y-level instead of the set y-level")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines.")
        .defaultValue(new Color(255, 255, 255, 192))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides.")
        .defaultValue(new Color(255, 255, 255, 32))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Lines)
        .build()
    );

    public AreaHighlighter() {
        super(FumoUtils.CATEGORY, "area-highlighter", "Highlights the area depending on the setting");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null) return;
        double yLevelToUse;
        Vec3 pos = mc.player.position();

        if (feetYLevel.get()) {
            yLevelToUse = pos.y - 1;
        } else {
            yLevelToUse = yLevel.get();
        }

        switch (areaType.get()) {
            case RegionFile:
                event.renderer.box(
                    getEdge(pos.x), yLevelToUse, getEdge(pos.z),
                    getEdge(pos.x) - 512, yLevelToUse + 1, getEdge(pos.z) - 512,
                    sideColor.get(), lineColor.get(), shapeMode.get(), 0
                );
                break;
            case MapArt:
                event.renderer.box(
                    getMapCenter(pos.x) - 64, yLevelToUse, getMapCenter(pos.z) - 64,
                    getMapCenter(pos.x) + 64, yLevelToUse + 1, getMapCenter(pos.z) + 64,
                    sideColor.get(), lineColor.get(), shapeMode.get(), 0
                );
                break;
        }
    }

    private int getEdge(double i){
        return (int)Math.ceil(i / 512) * 512;
    }

    private int getMapCenter(double i){
        return (int)Math.ceil((i - 64) / 128) * 128;
    }

    private enum AreaType {
        RegionFile,
        MapArt
    }
}
