package sh.qnx.fumo.modules;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;


public class HostOverride extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public final Setting<String> newHostname = sgGeneral.add(new StringSetting.Builder()
        .name("new-hostname")
        .description("What hostname to send the server")
        .defaultValue("example.com")
        .build()
    );

    public HostOverride() {
        super(FumoUtils.CATEGORY, "host-override", "Override hostname when connecting to a server");
    }
}
