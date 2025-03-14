package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.world.TickRate;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class TPSLogger extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    int tickTrack = 0;

    private final Setting<Integer> interval = sgDefault.add(new IntSetting.Builder()
        .name("interval")
        .description("Interval in client side ticks")
        .defaultValue(20)
        .range(0, 2000)
        .sliderRange(10, 200)
        .build()
    );

    public TPSLogger() {
        super(FumoUtils.CATEGORY, "TPS-logger", "Logs TPS in chat.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(tickTrack >= interval.get()){
            info(String.valueOf(TickRate.INSTANCE.getTickRate()));
            tickTrack = 0;
            return;
        }
        tickTrack++;
    }
}
