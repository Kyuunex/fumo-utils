package moe.kyuunex.fumo_utils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;

public class PrintRemoteIP extends Command {
    public PrintRemoteIP() {
        super("print_remote_ip", "Prints the remote IP address");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;

            ClientConnection connection = mc.getNetworkHandler().getConnection();
            String ip = connection.channel.remoteAddress().toString();

            info(Text.literal(String.valueOf(ip)));
            return SINGLE_SUCCESS;
        });
    }
}
