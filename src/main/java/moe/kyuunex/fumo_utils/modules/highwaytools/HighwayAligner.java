package moe.kyuunex.fumo_utils.modules.highwaytools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.systems.modules.world.Nuker;
import moe.kyuunex.fumo_utils.utils.DisconnectUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;


public class HighwayAligner extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> disconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect")
        .description("Disconnect when an admin is on")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> leniency = sgGeneral.add(new IntSetting.Builder()
        .name("leniency")
        .description("")
        .defaultValue(5)
        .sliderRange(0, 10)
        .range(-1, 1000)
        .build()
    );

    private final Setting<Integer> yLevel = sgGeneral.add(new IntSetting.Builder()
        .name("y-level")
        .description("")
        .defaultValue(119)
        .range(-500, 20000)
        .sliderRange(50, 130)
        .build()
    );

    private final Setting<Direction.Axis> currentAxis = sgGeneral.add(new EnumSetting.Builder<Direction.Axis>()
        .name("current-axis")
        .description("Axis that stays the same")
        .defaultValue(Direction.Axis.X)
        .build()
    );

    private final Setting<Integer> horizontalCoord = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-coordinate")
        .description("")
        .defaultValue(30000000)
        .range(-30000000, 30000000)
        .sliderRange(30000000, 30000000)
        .build()
    );

    private final Setting<Integer> blockOffset = sgGeneral.add(new IntSetting.Builder()
        .name("block-offset")
        .description("Offset the coordinate in each direction if you are digging from the corner.")
        .defaultValue(0)
        .range(-20, 20)
        .sliderRange(-2, 2)
        .build()
    );

    public final Setting<Boolean> debugPrint = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-print")
        .description("Print debug messages")
        .defaultValue(false)
        .build());

    public HighwayAligner() {
        super(
            FumoUtils.HIGHWAY,
            "highway-aligner",
            "Disconnects you when you get misaligned on the highway."
        );
    }

    Timer timerMod = Modules.get().get(Timer.class);
    Nuker nukerMod = Modules.get().get(Nuker.class);
    HighwayWalk highwayWalkMod = Modules.get().get(HighwayWalk.class);
    HighwayTunneler tunnelMinerMod = Modules.get().get(HighwayTunneler.class);
    private int timer = 0;
    public static boolean misaligned = false;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (debugPrint.get()) info("%s".formatted(mc.player.blockPosition().get(currentAxis.get())));
        if ((int)mc.player.position().y() != yLevel.get() || mc.player.blockPosition().get(currentAxis.get()) != (horizontalCoord.get() + blockOffset.get())) {

            if (timer < leniency.get()) {
                timer++;
                return;
            }

            if (mc.level.getBlockState(mc.player.blockPosition()).getBlock() == Blocks.SOUL_SAND) {
                info("soul sand");
                return;
            }

            if (timerMod.isActive()) timerMod.toggle();
            if (nukerMod.isActive()) nukerMod.toggle();
            if (highwayWalkMod != null && highwayWalkMod.isActive()) highwayWalkMod.toggle();
            if (tunnelMinerMod != null && tunnelMinerMod.isActive()) tunnelMinerMod.toggle();
            misaligned = true;

            ClientPacketListener network = mc.getConnection();
            timer = 0;
            if (disconnect.get()){
                DisconnectUtils.disconnect(network, "[HighwayAligner] You are misaligned");
            }
        } else {
            timer = 0;
            misaligned = false;
        }
    }
}
