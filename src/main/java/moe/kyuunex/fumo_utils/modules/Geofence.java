package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import moe.kyuunex.fumo_utils.utils.DisconnectUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class Geofence extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<BlockPos> fence = sgDefault.add(new BlockPosSetting.Builder()
        .name("position")
        .description("The coords")
        .defaultValue(BlockPos.ZERO)
        .build());

    public final Setting<Boolean> disconnect = sgDefault.add(new BoolSetting.Builder()
        .name("disconnect-after")
        .description("Disconnect after")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> debugPrint = sgDefault.add(new BoolSetting.Builder()
        .name("debug-print")
        .description("Print debug messages")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> checkY = sgDefault.add(new BoolSetting.Builder()
        .name("check-y-level-too")
        .description("Check Y value as well, otherwise, only X and Z are checked")
        .defaultValue(false)
        .build());

    private final Setting<Integer> proximity = sgDefault.add(new IntSetting.Builder()
        .name("proximity")
        .description("Proximity in blocks")
        .defaultValue(16)
        .range(0, 20000)
        .sliderRange(1, 32)
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

    private Vec3 currentPos;
    private boolean inXRange;
    private boolean inYRange;
    private boolean inZRange;
    private int yProx;

    public Geofence() {
        super(
            FumoUtils.CATEGORY,
            "geofence",
            "Disconnect when specific coords are reached"
        );
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        currentPos = mc.player.position();
        inXRange = Math.abs(currentPos.x() - fence.get().getX()) <= proximity.get();
        inZRange = Math.abs(currentPos.z() - fence.get().getZ()) <= proximity.get();

        if (checkY.get())
            inYRange = Math.abs(currentPos.y() - fence.get().getY()) <= proximity.get();
        else
            inYRange = true;

        if (debugPrint.get()){
            info(
                "X %s, Y %s, Z %s",
                Math.abs(currentPos.x() - fence.get().getX()),
                Math.abs(currentPos.y() - fence.get().getY()),
                Math.abs(currentPos.z() - fence.get().getZ())
            );
        }

        if (inXRange && inYRange && inZRange) dc();

    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null) return;
        currentPos = mc.player.position();

        if (checkY.get())
            yProx = proximity.get();
        else
            yProx = 0;

        event.renderer.box(
            fence.get().getX() - proximity.get(), fence.get().getY() - yProx, fence.get().getZ() - proximity.get(),
            fence.get().getX() + 1 + proximity.get(), fence.get().getY() + 1 + yProx, fence.get().getZ() + 1 + proximity.get(),
            sideColor.get(), lineColor.get(), shapeMode.get(), 0
        );
    }

    private void dc() {
        if (disconnect.get()) {
            ClientPacketListener network = mc.getConnection();
            DisconnectUtils.disconnect(network, Component.literal("[Geofence] Specific coords are reached"));
        }
    }
}
