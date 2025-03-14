package moe.kyuunex.fumo_utils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class OfflineUUIDGen extends Command {
    public OfflineUUIDGen() {
        super("offline-uuid-gen", "Generates offline UUID from a string");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.player == null) return -1;
            String playerName = mc.player.getName().getString();

            info(Component.literal(genUUID(playerName)));
            return SINGLE_SUCCESS;
        });

        builder.then(argument("playerName", StringArgumentType.word()).executes(context -> {
            String playerName = StringArgumentType.getString(context, "playerName");
            info(Component.literal(genUUID(playerName)));
            return SINGLE_SUCCESS;
        }));
    }

    private String genUUID(String playerName){
        String stringToHash = "OfflinePlayer:" + playerName;
        UUID uuid = UUID.nameUUIDFromBytes(stringToHash.getBytes(StandardCharsets.UTF_8));
        return "UUID for " + playerName + " is: " + String.valueOf(uuid);
    }
}
