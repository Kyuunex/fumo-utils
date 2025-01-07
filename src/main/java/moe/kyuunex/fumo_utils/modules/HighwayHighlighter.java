package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import moe.kyuunex.fumo_utils.enums.HighwayType;
import moe.kyuunex.fumo_utils.enums.XDirection;
import moe.kyuunex.fumo_utils.enums.YDirection;

public class HighwayHighlighter extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgStraight = settings.createGroup("Straight/Diagonal");
    private final SettingGroup sgRing = settings.createGroup("Ring");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final int worldBorder = 30000000;

    // General
    private final Setting<HighwayType> highwayType = sgGeneral.add(new EnumSetting.Builder<HighwayType>()
        .name("highway-type")
        .description("Choose highway type, Straight/Diag or Ring")
        .defaultValue(HighwayType.STRAIGHT)
        .build()
    );

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

    // Straight/Diag
    private final Setting<XDirection> xMultiplier = sgStraight.add(new EnumSetting.Builder<XDirection>()
        .name("x-direction")
        .description("X direction")
        .defaultValue(XDirection.CENTER)
        .build()
    );

    private final Setting<YDirection> yMultiplier = sgStraight.add(new EnumSetting.Builder<YDirection>()
        .name("x-direction")
        .description("Y direction")
        .defaultValue(YDirection.CENTER)
        .build()
    );


    // Ring
    private final Setting<ShapeMode> shapeMode = sgRing.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Lines)
        .build()
    );

    private final Setting<Double> radius = sgRing.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Radius of the ring")
        .defaultValue(5000)
        .range(0, worldBorder)
        .sliderRange(0, worldBorder)
        .build()
    );

    // Render
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines.")
        .defaultValue(new Color(255, 128, 0, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides.")
        .defaultValue(new Color(255, 128, 0, 16))
        .build()
    );

    public HighwayHighlighter() {
        super(FumoUtils.CATEGORY, "highway-highlighter", "Highlights a selected highway");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        assert mc.player != null;
        double yLevelToUse;
        Vec3d pos = mc.player.getPos();

        if (feetYLevel.get()) {
            yLevelToUse = pos.y;
        } else {
            yLevelToUse = yLevel.get();
        }

        if(highwayType.get() == HighwayType.STRAIGHT) {
            event.renderer.line(
                0, yLevelToUse, 0,
                worldBorder*xMultiplier.get().getRaw(), yLevelToUse, worldBorder*yMultiplier.get().getRaw(),
                lineColor.get()
            );
        } else {
            event.renderer.box(
                radius.get(), yLevelToUse, radius.get(),
                -radius.get(), yLevelToUse, -radius.get(),
                sideColor.get(), lineColor.get(), shapeMode.get(), 0
            );
        }
    }
}
