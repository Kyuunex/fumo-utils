package moe.kyuunex.fumo_utils.utils;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;


public class DisconnectUtils {
    public static void disconnect(ClientPlayNetworkHandler network, Text reason) {
        MutableText text = Text.empty();
        text.append(reason);

        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect.isActive()) {
            text.append(Text.literal("\n\nINFO - AutoReconnect was disabled").withColor(Colors.GRAY));
            autoReconnect.toggle();
        }

        network.onDisconnect(new DisconnectS2CPacket(text));
    }
}
