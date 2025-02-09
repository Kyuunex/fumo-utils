package moe.kyuunex.fumo_utils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class OfflineUUIDGen extends Command {
    public OfflineUUIDGen() {
        super("offline_uuid_gen", "Generates offline UUID from a string");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            PlayerEntity player = MinecraftClient.getInstance().player;
            assert player != null;
            String playerName = player.getName().getString();

            info(Text.literal(genUUID(playerName)));
            return SINGLE_SUCCESS;
        });

        builder.then(argument("playerName", StringArgumentType.word()).executes(context -> {
            String playerName = StringArgumentType.getString(context, "playerName");
            info(Text.literal(genUUID(playerName)));
            return SINGLE_SUCCESS;
        }));
    }

    private String genUUID(String playerName){
        String stringToHash = "OfflinePlayer:" + playerName;
        UUID uuid = UUID.nameUUIDFromBytes(stringToHash.getBytes(StandardCharsets.UTF_8));
        return "UUID for " + playerName + " is: " + String.valueOf(uuid);
    }
}
