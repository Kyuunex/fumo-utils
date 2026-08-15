package sh.qnx.fumo.modules.highwaytools;

import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import sh.qnx.fumo.enums.HighwayType;
import sh.qnx.fumo.enums.XDirection;
import sh.qnx.fumo.enums.YDirection;

public class HighwayHighlighter extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgStraight = settings.createGroup("Straight/Diagonal");
    private final SettingGroup sgRing = settings.createGroup("Ring");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final int WB = 30000000;

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
        .name("y-direction")
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

    private final Setting<Integer> radius = sgRing.add(new IntSetting.Builder()
        .name("radius")
        .description("Radius of the ring")
        .defaultValue(5000)
        .range(0, WB)
        .sliderRange(0, WB)
        .build()
    );

    private final Setting<Double> blockOffset = sgRing.add(new DoubleSetting.Builder()
        .name("block-offset")
        .description("Offset the line to center it to a block.")
        .defaultValue(0.5)
        .range(-0.5, 0.5)
        .sliderRange(-0.5, 0.5)
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
        super(FumoUtils.HIGHWAY, "highway-highlighter", "Highlights a selected highway");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null) return;
        double yLevelToUse;
        Vec3 pos = mc.player.position();

        if (feetYLevel.get()) {
            yLevelToUse = pos.y;
        } else {
            yLevelToUse = yLevel.get();
        }

        if (highwayType.get() == HighwayType.STRAIGHT) {
            event.renderer.line(
                0 + blockOffset.get(), yLevelToUse, 0 + blockOffset.get(),
                WB * xMultiplier.get().getRaw() + blockOffset.get(), yLevelToUse, WB * yMultiplier.get().getRaw() + blockOffset.get(),
                lineColor.get()
            );
        } else {
            event.renderer.box(
                getBlockOffset(-radius.get()), yLevelToUse, getBlockOffset(-radius.get()),   // min
                getBlockOffset(radius.get()), yLevelToUse,  getBlockOffset(radius.get()),   // max
                sideColor.get(), lineColor.get(), shapeMode.get(), 0
            );
        }
    }

    private double getBlockOffset(int n) {
        return n + 0.5;
    }
}
