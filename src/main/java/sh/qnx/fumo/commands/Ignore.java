package sh.qnx.fumo.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import sh.qnx.fumo.modules.IgnoreUsers;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.List;

public class Ignore extends Command {
    public Ignore() {
        super("ignore", "Ignores the user");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(this::ignoreProfile));
        builder.then(argument("username", StringArgumentType.word()).executes(this::ignoreString));
    }

    private int ignoreProfile(CommandContext<ClientSuggestionProvider> context) {
        GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
        String username = profile.name();
        return ignore(username);
    }

    private int ignoreString(CommandContext<ClientSuggestionProvider> context) {
        String username = StringArgumentType.getString(context, "username");
        return ignore(username);
    }

    private int ignore(String username) {
        IgnoreUsers ignoreModule = Modules.get().get(IgnoreUsers.class);
        List<String> ignoredUsers = ignoreModule.ignoredUsers.get();

        if (ignoredUsers.contains(username)) {
            ignoredUsers.remove(username);
            info("%s unignored client side.", username);
        } else {
            ignoredUsers.add(username);
            info("%s ignored client side.", username);
        }

        return SINGLE_SUCCESS;
    }
}
