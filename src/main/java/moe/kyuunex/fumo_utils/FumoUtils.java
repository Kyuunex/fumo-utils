package moe.kyuunex.fumo_utils;

import moe.kyuunex.fumo_utils.commands.*;
import moe.kyuunex.fumo_utils.modules.*;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class FumoUtils extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FumoUtils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing FumoUtils");

        // Modules
        Modules.get().add(new AltitudeStabilizer());
        Modules.get().add(new AutoDump());
        Modules.get().add(new ChatNotifier());
        Modules.get().add(new DoujinDupe());
        Modules.get().add(new ElytraWatch());
        Modules.get().add(new FumoVoidESP());
        Modules.get().add(new HighwayHighlighter());
        Modules.get().add(new IgnoreUsers());
        Modules.get().add(new ItemESP());
        Modules.get().add(new Karaoke());
        Modules.get().add(new MapHighlighter());
        Modules.get().add(new QuartzFarmer());
        Modules.get().add(new RegionFileHighlighter());
        Modules.get().add(new TPSLogger());
        Modules.get().add(new TridentDupe());
        Modules.get().add(new Undead());
        Modules.get().add(new UnSilkToucher());
        Modules.get().add(new WebhookBridge());
        Modules.get().add(new WireGuardIntegration());

        // Commands
        Commands.add(new ChatCooker());
        Commands.add(new Ignore());
        Commands.add(new OfflineUUIDGen());
        Commands.add(new PrintRemoteIP());
        Commands.add(new PrintTPS());
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
