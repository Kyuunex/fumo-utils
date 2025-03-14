package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.chat.Component;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;

import java.util.List;

public class IgnoreUsers extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    public final Setting<List<String>> ignoredUsers = sgDefault.add(new StringListSetting.Builder()
        .name("ignored-users")
        .description("List of users that are ignored")
        .build()
    );

    private final Setting<String> messageFormat = sgDefault.add(new StringSetting.Builder()
        .name("message-format")
        .description("Message format specific to the server")
        .defaultValue("<%s>")
        .build()
    );

    private final Setting<String> whisperFormat = sgDefault.add(new StringSetting.Builder()
        .name("whisper-message-format")
        .description("Whisper message format specific to the server")
        .defaultValue("From %s:")
        .build()
    );

    public IgnoreUsers() {
        super(FumoUtils.CATEGORY, "ignore", "Ignore users in chat, client side. Use '.ignore' to add users");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();

        String messageString = message.getString().toLowerCase();
        for (String ignoredUser : ignoredUsers.get()) {
            if (
                messageString.startsWith((messageFormat.get().formatted(ignoredUser)).toLowerCase()) ||
                messageString.startsWith((whisperFormat.get().formatted(ignoredUser)).toLowerCase())
            ) {
                event.cancel();
                return;
            }
        }

        // event.setMessage(message);
    }
}
