package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;

public class ForceRaining extends Module {

    public ForceRaining() {
        super(FumoUtils.CATEGORY, "force-raining", "Force raining client-side. OW only.");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundGameEventPacket packet) {
            if (packet.getEvent() == ClientboundGameEventPacket.STOP_RAINING)
                event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;
        mc.level.setRainLevel(1.0f);
    }
}
