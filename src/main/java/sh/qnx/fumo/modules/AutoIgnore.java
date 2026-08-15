package sh.qnx.fumo.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

public class AutoIgnore extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<IgnoreMethod> ignoreMethod = sgDefault.add(new EnumSetting.Builder<IgnoreMethod>()
        .name("ignore-method")
        .description("Client side ignore requires 'Ignore' module enabled.")
        .defaultValue(IgnoreMethod.CLIENT_SIDE)
        .build()
    );

    private final Setting<String> serverIgnoreCommand = sgDefault.add(new StringSetting.Builder()
        .name("ignore-command")
        .description("Server ignore command. Put <USERNAME> as username placeholder.")
        .defaultValue("ignore player <USERNAME>")
        .visible(() -> ignoreMethod.get() == IgnoreMethod.SERVER_SIDE)
        .build()
    );

    private final Setting<Integer> threshold = sgDefault.add(new IntSetting.Builder()
        .name("threshold")
        .description("How many times per window")
        .range(1, 200)
        .sliderRange(1, 20)
        .defaultValue(4)
        .build()
    );

    private final Setting<Integer> timeWindow = sgDefault.add(new IntSetting.Builder()
        .name("time-window")
        .description("Time window")
        .range(1, 2000)
        .sliderRange(1, 400)
        .defaultValue(20)
        .build()
    );

    private final Setting<List<String>> blacklist = sgDefault.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("List of words that imply spam.")
        .defaultValue(
            Arrays.asList(
                "crypto",
                "rubles",
                "discord.com/invite",
                "discord.gg",
                "dsc.gg",
                "kit",
                "paypal",
                "shop",
                "giveaway",
                "кит",
                "join",
                "t.com",
                ".com",
                ".org",
                ".net",
                "buy",
                "anti-spam",
                "dcs.gg",
                "anarchy",
                "vanilla",
                "imps-join",
                "market",
                "check out"
            )
        )
        .build()
    );

    private final Map<String, List<Long>> userSpamTimestamps = new ConcurrentHashMap<>();

    public AutoIgnore() {
        super(FumoUtils.CATEGORY, "auto-ignore", "Uses some math to auto ignore a user");
    }

    @EventHandler(priority = 1)
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();
        String messageString = message.getString();

        String username = extractUsername(messageString);
        if (username == null) {
            return;
        }

        for (String word : blacklist.get()) {
            if (messageString.toLowerCase().contains(word.toLowerCase())) {
                registerSpam(username);
                // break;
            }
        }
    }

    private String extractUsername(String message) {
        if (message.startsWith("<")) {
            int closing = message.indexOf(">");
            if (closing > 1) {
                return message.substring(1, closing);
            }
        }
        return null;
    }

    private void registerSpam(String username) {
        long now = Instant.now().toEpochMilli();

        userSpamTimestamps.putIfAbsent(username, new ArrayList<>());
        List<Long> timestamps = userSpamTimestamps.get(username);

        // Add current timestamp
        timestamps.add(now);

        // Remove timestamps older than 20s
        timestamps.removeIf(ts -> now - ts > timeWindow.get() * 1000);

        // If threshold exceeded, ignore user
        if (timestamps.size() >= threshold.get()) {
            ignore(username);
            userSpamTimestamps.remove(username); // Reset after ignoring
        }
    }

    private void ignore(String username) {
        if (ignoreMethod.get() == IgnoreMethod.CLIENT_SIDE) {
            IgnoreUsers ignoreModule = Modules.get().get(IgnoreUsers.class);
            List<String> ignoredUsers = ignoreModule.ignoredUsers.get();

            if (!ignoredUsers.contains(username)) {
                ignoredUsers.add(username);
                info("%s ignored client side.", username);
            }
        } else if (ignoreMethod.get() == IgnoreMethod.SERVER_SIDE) {
            ClientPacketListener network = mc.getConnection();
            if (network == null) return;
            network.getConnection().send(
                new ServerboundChatCommandPacket(serverIgnoreCommand.get().replace("<USERNAME>", username)),
                null,
                true
            );
        }
    }

    public enum IgnoreMethod {
        CLIENT_SIDE,
        SERVER_SIDE
    }
}
