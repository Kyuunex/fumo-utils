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
    private final SettingGroup sgStrings = settings.createGroup("Strings");

    public final Setting<List<String>> ignoredUsers = sgDefault.add(new StringListSetting.Builder()
        .name("ignored-users")
        .description("List of users that are ignored")
        .build()
    );

    private final Setting<String> messageFormat = sgStrings.add(new StringSetting.Builder()
        .name("message-format")
        .description("Message format specific to the server")
        .defaultValue("<%s>")
        .build()
    );

    private final Setting<String> whisperFormat = sgStrings.add(new StringSetting.Builder()
        .name("whisper-message-format")
        .description("Whisper message format specific to the server")
        .defaultValue("From %s:")
        .build()
    );

    private final Setting<String> joinFormat = sgStrings.add(new StringSetting.Builder()
        .name("join-message-format")
        .description("Join message format specific to the server")
        .defaultValue("%s joined the game")
        .build()
    );

    private final Setting<String> leaveFormat = sgStrings.add(new StringSetting.Builder()
        .name("leave-message-format")
        .description("Leave message format specific to the server")
        .defaultValue("%s left the game")
        .build()
    );

    public IgnoreUsers() {
        super(FumoUtils.CATEGORY, "ignore", "Ignore users in chat, client side. Use '.ignore' to add users");
    }

    @EventHandler(priority = 1)
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();

        String messageString = message.getString().toLowerCase();
        for (String ignoredUser : ignoredUsers.get()) {
            if (messageString.startsWith((messageFormat.get().formatted(ignoredUser)).toLowerCase())) event.cancel();

            if (messageString.startsWith((whisperFormat.get().formatted(ignoredUser)).toLowerCase())) event.cancel();

            if (messageString.startsWith((joinFormat.get().formatted(ignoredUser)).toLowerCase())) event.cancel();

            if (messageString.startsWith((leaveFormat.get().formatted(ignoredUser)).toLowerCase())) event.cancel();
        }

    }
}
