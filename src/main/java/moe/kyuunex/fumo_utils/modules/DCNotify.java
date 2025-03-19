package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.utils.Webhook;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import moe.kyuunex.fumo_utils.FumoUtils;

public class DCNotify extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<String> webhookUrl = sgGeneral.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Discord webhook URL")
        .defaultValue("")
        .build()
    );

    public DCNotify() {
        super(FumoUtils.CATEGORY, "DC-notify", "Notifies you when disconnected from the server.");
        this.runInMainMenu = true;
    }

    @EventHandler
    public void onScreenChange(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            Webhook.sendAsync(
                webhookUrl.get(),
                "Disconnect notifier",
                "disconnected"
            );
        }
    }

}
