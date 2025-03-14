package moe.kyuunex.fumo_utils.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Random;

public class ChatCooker extends Command {
    private static final Random random = new Random(System.currentTimeMillis());

    public ChatCooker() {
        super("chat-cooker", "Generate and send text in chat.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("unicode").then(argument("length", IntegerArgumentType.integer()).executes(context -> {
            int length = IntegerArgumentType.getInteger(context, "length");

            if (mc.player == null) return -1;
            if (mc.getConnection() == null) return -1;

            mc.getConnection().sendChat(randomText(length, true));
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("ascii").then(argument("length", IntegerArgumentType.integer()).executes(context -> {
            int length = IntegerArgumentType.getInteger(context, "length");

            if (mc.player == null) return -1;
            if (mc.getConnection() == null) return -1;

            mc.getConnection().sendChat(randomText(length, false));
            return SINGLE_SUCCESS;
        })));
    }

    public static String randomText(int length, boolean uni) {
        StringBuilder str = new StringBuilder();
        int leftLimit = 48;
        int rightLimit = 122;

        if (uni) {
            leftLimit = 100000;
            rightLimit = 10000000;
        }

        for (int i = 0; i < length; i++) {
            str.append((char) (leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1))));
        }
        return str.toString();
    }
}
