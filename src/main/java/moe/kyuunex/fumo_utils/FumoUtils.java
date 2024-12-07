package moe.kyuunex.fumo_utils;

import moe.kyuunex.fumo_utils.commands.*;
import moe.kyuunex.fumo_utils.modules.*;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class FumoUtils extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FumoUtils");
    public static final HudGroup HUD_GROUP = new HudGroup("FumoUtils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing FumoUtils");

        // Modules
        Modules.get().add(new AutoDump());
        Modules.get().add(new ChatNotifier());
        Modules.get().add(new DoujinDupe());
        Modules.get().add(new MapHighlighter());
        Modules.get().add(new QuartzFarmer());
        Modules.get().add(new RegionFileHighlighter());
        Modules.get().add(new UnSilkToucher());
        Modules.get().add(new WebhookBridge());

        // Commands
        Commands.add(new ChatCooker());
        Commands.add(new PrintTPS());
        Commands.add(new OfflineUUIDGen());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "moe.kyuunex.fumo_utils";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Kyuunex", "fumo-utils");
    }
}
