package moe.kyuunex.fumo_utils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.text.Text;

public class PrintTPS extends Command {
    public PrintTPS() {
        super("print_tps", "Prints the tps in chat");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info(Text.literal(String.valueOf(TickRate.INSTANCE.getTickRate())));
            return SINGLE_SUCCESS;
        });
    }
}
