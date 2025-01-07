package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.utils.DisconnectUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import java.util.List;

public class ElytraWatch extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");
    private final SoundEvent defaultNotificationSound = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
    private int tickTrack = 0;
    private boolean isNotified = false;

    private final Setting<Integer> durabilityThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("durability-threshold")
        .description("Alert at this durability")
        .defaultValue(30)
        .sliderRange(1, 432)
        .range(1, 432)
        .build()
    );

    private final Setting<Boolean> disconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect")
        .description("Disconnect when elytra durability is bellow threshold.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enableSound = sgSound.add(new BoolSetting.Builder()
        .name("enable-sound-notification")
        .description("Enable sound notifications")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<SoundEvent>> soundSetting = sgSound.add(new SoundEventListSetting.Builder()
        .name("notification-sound")
        .description("Notification sound. PICK ONLY ONE!")
        .defaultValue(defaultNotificationSound)
        .build()
    );

    public ElytraWatch() {
        super(FumoUtils.CATEGORY, "elytra-watch", "Watch Elytra durability");
    }

    @Override
    public void onActivate() {
        isNotified = false;
    }

    @Override
    public void onDeactivate() {
        isNotified = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        assert mc.player != null;
        ItemStack chestStack = mc.player.getInventory().getArmorStack(2);
        boolean isWearingElytra = chestStack.getItem() == Items.ELYTRA;
        if (isWearingElytra && chestStack.getMaxDamage() - chestStack.getDamage() <= durabilityThreshold.get()){
            String alertMsg = "Elytra durability is bellow threshold; ";

            if(!isNotified) {
                info(Text.literal(alertMsg));
                isNotified = true;
            }

            if(disconnect.get()){
                ClientPlayNetworkHandler network = mc.getNetworkHandler();
                DisconnectUtils.disconnect(network, Text.literal("[ElytraWatch] " + alertMsg + "Disconnecting;"));
            }

            if(enableSound.get()) {
                assert mc.world != null;
                SoundEvent notificationSound;

                if(soundSetting.get().isEmpty()) {
                    notificationSound = defaultNotificationSound;
                } else {
                    notificationSound = soundSetting.get().getFirst();
                }

                if(tickTrack == 0 || tickTrack == 3 || tickTrack == 6 || tickTrack == 9){
                    mc.world.playSoundFromEntity(
                        mc.player, mc.player, notificationSound, SoundCategory.VOICE, 3.0F, 1.0F
                    );
                }

                tickTrack++;
                if(tickTrack == 20) tickTrack = 0;
            }
        } else {
            isNotified = false;
        }
    }

}
