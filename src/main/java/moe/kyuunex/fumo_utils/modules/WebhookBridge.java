package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.SettingGroup;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import moe.kyuunex.fumo_utils.utils.Webhook;

import java.util.List;

public class WebhookBridge extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<String> webhookUrl = sgGeneral.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Discord webhook URL")
        .defaultValue("")
        .build()
    );

    private final Setting<List<String>> blacklist = sgGeneral.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("Strings to blacklist")
        .defaultValue(List.of("https://", "http://", "[Meteor]", "[Baritone]"))
        .build()
    );

    public WebhookBridge() {
        super(FumoUtils.CATEGORY, "webhook-bridge", "Bridge the server chat to Discord (one way)");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();
        String messageString = message.getString();

        for (String string : blacklist.get()){
            if (messageString.contains(string)) {
                return;
            }
        }

        Webhook.sendAsync(webhookUrl.get(), getServerName(), messageString);
    }
    private String getServerName(){
        ServerData server = mc.getCurrentServer();
        if (server == null) return "Meteor Client";

        return server.ip;
    }
}
