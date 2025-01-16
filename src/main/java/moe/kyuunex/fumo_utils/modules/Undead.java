package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;


public class Undead extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> resetDeathTime = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-death-time")
        .description("Reset death time")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> resetHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-health")
        .description("Reset heath")
        .defaultValue(true)
        .build()
    );

    public Undead() {
        super(FumoUtils.CATEGORY, "undead", "OMAE WA MOU SHINDEIRU JA NAI");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen instanceof DeathScreen) {
            mc.setScreen(null);
            assert mc.player != null;

            if(resetDeathTime.get()){
                mc.player.deathTime = 0;
            }
            if(resetHealth.get()){
                mc.player.setHealth(20.0f);
            }
        }
    }
}
