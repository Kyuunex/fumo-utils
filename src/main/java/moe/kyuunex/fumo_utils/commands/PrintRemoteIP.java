package moe.kyuunex.fumo_utils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

public class PrintRemoteIP extends Command {
    public PrintRemoteIP() {
        super("print-remote-ip", "Prints the remote IP address");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.getConnection() == null) return SINGLE_SUCCESS;

            Connection connection = mc.getConnection().getConnection();
            String ip = connection.getLoggableAddress(true);

            info(Component.literal(String.valueOf(ip)));
            return SINGLE_SUCCESS;
        });
    }
}
