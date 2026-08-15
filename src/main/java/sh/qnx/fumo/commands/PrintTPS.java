package sh.qnx.fumo.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.network.chat.Component;

public class PrintTPS extends Command {
    public PrintTPS() {
        super("print-tps", "Prints the tps in chat");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            info(Component.literal(String.valueOf(TickRate.INSTANCE.getTickRate())));
            return SINGLE_SUCCESS;
        });
    }
}
