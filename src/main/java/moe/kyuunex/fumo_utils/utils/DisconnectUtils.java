package moe.kyuunex.fumo_utils.utils;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;


public class DisconnectUtils {
    public static void disconnect(ClientPacketListener network, Component reason) {
        MutableComponent text = Component.empty();
        text.append(reason);

        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect.isActive()) {
            text.append(Component.literal("\n\nINFO - AutoReconnect was disabled").withColor(CommonColors.GRAY));
            autoReconnect.toggle();
        }

        network.handleDisconnect(new ClientboundDisconnectPacket(text));
    }
}
