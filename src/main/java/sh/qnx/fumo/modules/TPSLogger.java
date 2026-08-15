package sh.qnx.fumo.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.world.TickRate;
import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class TPSLogger extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    int tickTrack = 0;

    float longestInterval = 0;
    float lastPrintTime = 0;
    float smoothTPS = 0;
    float rawTPS = 0;
    float timeSinceLastTick;

    private final Setting<Integer> interval = sgDefault.add(new IntSetting.Builder()
        .name("interval")
        .description("Interval in client side ticks (does not work with raw)")
        .defaultValue(20)
        .range(0, 2000)
        .sliderRange(10, 200)
        .build()
    );

    private final Setting<Boolean> logRaw = sgDefault.add(new BoolSetting.Builder()
        .name("raw")
        .description("Print raw TPS instead of average over time")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> logRawSmoothed = sgDefault.add(new BoolSetting.Builder()
        .name("raw-smoothed")
        .description("Slightly smooth the raw TPS")
        .defaultValue(false)
        .build()
    );

    public TPSLogger() {
        super(FumoUtils.CATEGORY, "TPS-logger", "Logs TPS in chat.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (logRaw.get()) {
            timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();
            if (timeSinceLastTick < longestInterval) { // did the time reset
                if (timeSinceLastTick != lastPrintTime) { // deduplicaton
                    rawTPS = 20 / longestInterval;
                    if (logRawSmoothed.get()) {
                        if (smoothTPS == 0)
                            smoothTPS = rawTPS;
                        else
                            smoothTPS = (smoothTPS + rawTPS) / 2;
                        info(String.valueOf(smoothTPS));
                    } else {
                        info(String.valueOf(rawTPS));
                    }
                    lastPrintTime = timeSinceLastTick;
                }
            }
            longestInterval = timeSinceLastTick;
        } else {
            if (tickTrack >= interval.get()) {
                info(String.valueOf(TickRate.INSTANCE.getTickRate()));

                tickTrack = 0;
                return;
            }
            tickTrack++;
        }
    }
}
