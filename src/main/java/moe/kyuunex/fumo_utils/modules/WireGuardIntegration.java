package moe.kyuunex.fumo_utils.modules;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.settings.*;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WireGuardIntegration extends Module {
    private Process wpProcess;
    private File tmpConfigfile;
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgInfo = settings.createGroup("Info");

    private final Setting<Integer> port = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("Port number of meteor configured proxy")
        .defaultValue(50125)
        .range(0, 65535)
        .sliderRange(1025, 65535)
        .build()
    );

    private final Setting<String> wireproxyExecutable = sgGeneral.add(new StringSetting.Builder()
        .name("wireproxy-executable")
        .description("Full path to the wireproxy executable")
        .defaultValue("/usr/bin/wireproxy")
        .build()
    );

    private final Setting<String> wireguardConfigDirectory = sgGeneral.add(new StringSetting.Builder()
        .name("wireguard-config-directory")
        .description("Full path to your wireguard config directory. Must contain files named UUID followed by .conf")
        .defaultValue("/home/user/Documents/mc-wg/")
        .build()
    );

    private final Setting<String> currentlyActiveAccount = sgInfo.add(new StringSetting.Builder()
        .name("currently-active-account")
        .description("Shows what account wireproxy is running for. If empty, it's not active.")
        .defaultValue("")
        .build()
    );

    private final Setting<String> accountWireguardConfigFile = sgInfo.add(new StringSetting.Builder()
        .name("account-wireguard-config-file")
        .description("Shows what config file is the temporary config pointed to. If empty, it's not active.")
        .defaultValue("")
        .build()
    );

    private final Setting<String> temporaryConfigFile = sgInfo.add(new StringSetting.Builder()
        .name("temporary-config-file")
        .description("Shows what config file wireproxy is running. If empty, it's not active.")
        .defaultValue("")
        .build()
    );

    public WireGuardIntegration() {
        super(
            FumoUtils.CATEGORY,
            "WireGuard-integration",
            "WireGuard integration via wireproxy. Loads up a wireproxy instance depending on the current account."
        );
        this.runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        startInstance();
    }

    @Override
    public void onDeactivate() {
        destroyInstance();
    }
    
    public void startInstance() {
        if (!Files.exists(Path.of(wireproxyExecutable.get()))) {
            FumoUtils.LOG.info("The wireproxy executable at: {} does not exist !!!! Baka Baka", wireproxyExecutable.get());
            return;
        }

        FumoUtils.LOG.info("Starting WireGuard integration.");

        String tmpConfigFileTemplate = "WGConfig = %s\n[Socks5]\nBindAddress = 127.0.0.1:%s";
        String wgConfigDirectory = wireguardConfigDirectory.get();

        GameProfile gameProfile = mc.getGameProfile();
        String wgConfigPath = wgConfigDirectory.concat(gameProfile.getId().toString()).concat(".conf");

        if (!Files.exists(Path.of(wgConfigPath))) {
            FumoUtils.LOG.info("Account config file at: {} does not exist.", wgConfigPath);
            return;
        }

        accountWireguardConfigFile.set(wgConfigPath);
        FumoUtils.LOG.info("Account config file is at: {}", wgConfigPath);

        try {
            if (tmpConfigfile != null) {
                Files.deleteIfExists(tmpConfigfile.toPath());
            }

            tmpConfigfile = File.createTempFile("mc-wg-", ".conf");
            temporaryConfigFile.set(tmpConfigfile.getPath());

            FumoUtils.LOG.info("Temporary config file is at: {}", tmpConfigfile.toString());

            FileWriter fileWriter = new FileWriter(tmpConfigfile);
            fileWriter.write(String.format(tmpConfigFileTemplate, wgConfigPath, port.get()));
            fileWriter.close();

            wpProcess = new ProcessBuilder(wireproxyExecutable.get(), "-c", tmpConfigfile.toString()).start();

            currentlyActiveAccount.set(gameProfile.getName());

            FumoUtils.LOG.info("WireGuard integration finished loading.");
        } catch (IOException ignored) {
        }

    }

    public void destroyInstance() {
        if (wpProcess != null) {
            wpProcess.descendants().forEach(ProcessHandle::destroy);
            wpProcess.destroy();
        }

        if (tmpConfigfile != null) {
            try {
                Files.deleteIfExists(tmpConfigfile.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            tmpConfigfile = null;
        }

        currentlyActiveAccount.reset();
        temporaryConfigFile.reset();
        accountWireguardConfigFile.reset();
        FumoUtils.LOG.info("Deactivated WireGuard integration.");
    }
}
