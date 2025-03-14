package moe.kyuunex.fumo_utils.modules;

import java.util.List;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.SettingGroup;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import moe.kyuunex.fumo_utils.utils.Webhook;

public class ChatNotifier extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgWhispers = settings.createGroup("Whispers");
    private final SettingGroup sgSound = settings.createGroup("Sound");
    private final SettingGroup sgWebhook = settings.createGroup("Webhook");

    private final Setting<Boolean> notifyOnMention = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-on-mention")
        .description("Enable notifications when the currently logged in player is mentioned")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notifyOnOwnMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-on-own-messages")
        .description("Enable notifications even if you are the message sender")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> strings = sgGeneral.add(new StringListSetting.Builder()
        .name("strings")
        .description("Notify when the message contains these strings")
        .defaultValue(List.of("[server]"))
        .build()
    );

    private final Setting<Boolean> whisperNotifications = sgWhispers.add(new BoolSetting.Builder()
        .name("whisper-notifications")
        .description("Get notified when someone whispers to you")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> whisperDetectionString = sgWhispers.add(new StringSetting.Builder()
        .name("whisper-detection-string")
        .description("String that lets this module distinguish the message as a whisper")
        .defaultValue(" whispers: ")
        .build()
    );

    private final Setting<Boolean> soundNotification = sgSound.add(new BoolSetting.Builder()
        .name("sound-notification")
        .description("Enable sound notifications")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<SoundEvent>> notificationSound = sgSound.add(new SoundEventListSetting.Builder()
        .name("notification-sound")
        .description("Notification sound. PICK ONLY ONE!")
        .defaultValue(SoundEvents.EXPERIENCE_ORB_PICKUP)
        .build()
    );

    private final Setting<Boolean> webhookNotification = sgWebhook.add(new BoolSetting.Builder()
        .name("webhook-notification")
        .description("Enable Webhook notifications")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> webhookUrl = sgWebhook.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Discord webhook URL")
        .defaultValue("")
        .build()
    );

    public ChatNotifier() {
        super(FumoUtils.CATEGORY, "chat-notifier", "Notifies you when certain things are mentioned in the chat");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();
        String messageString = message.getString();

        if (mc.player == null) return;
        if (!notifyOnOwnMessages.get()){
            if (messageString.startsWith("<" + mc.player.getName().getString() + ">")) {
                return;
            }
        }

        if (notifyOnMention.get()) {
            if (messageString.contains(mc.player.getName().getString())) {
                sendSoundNotification();
                sendWebhookNotification(webhookUrl.get(), messageString);
            }
        }

        if (whisperNotifications.get()) {
            if (messageString.contains(whisperDetectionString.get())) {
                sendSoundNotification();
                sendWebhookNotification(webhookUrl.get(), messageString);
            }
        }

        for (String string : strings.get()){
            if (messageString.contains(string)) {
                sendSoundNotification();
                sendWebhookNotification(webhookUrl.get(), messageString);
            }
        }
    }

    private void sendSoundNotification() {
        if (!soundNotification.get()){
            return;
        }

        if (mc.level == null) return;
        if (mc.player == null) return;
        mc.level.playSound(mc.player, mc.player, notificationSound.get().getFirst(), SoundSource.VOICE, 3.0F, 1.0F);
    }

    private void sendWebhookNotification(String webhookUrl, String message) {
        if (!webhookNotification.get()) {
            return;
        }

        Webhook.sendAsync(webhookUrl, getServerName(), message);
    }

    private String getServerName(){
        ServerData server = mc.getCurrentServer();
        if (server == null) return "Meteor Client";

        return server.ip;
    }
}
