package moe.kyuunex.fumo_utils.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import moe.kyuunex.fumo_utils.modules.IgnoreUsers;
import net.minecraft.command.CommandSource;

public class Ignore extends Command {
    public Ignore() {
        super("ignore", "Ignores the user");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(this::ignore));
    }

    private int ignore(CommandContext<CommandSource> context) {
        GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();

        IgnoreUsers ignoreModule = Modules.get().get(IgnoreUsers.class);
        ignoreModule.ignoredUsers.get().add(profile.getName());

        info("%s ignored client side.", profile.getName());
        return SINGLE_SUCCESS;
    }
}
