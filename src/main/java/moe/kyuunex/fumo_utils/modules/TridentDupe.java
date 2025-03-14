package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class TridentDupe extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Integer> interval = sgDefault.add(new IntSetting.Builder()
        .name("interval")
        .description("How many ticks to wait before releasing")
        .defaultValue(11)
        .range(0, 200)
        .sliderRange(10, 40)
        .build()
    );

    public final Setting<Boolean> drop = sgDefault.add(new BoolSetting.Builder()
        .name("drop-after")
        .description("Drop after")
        .defaultValue(false)
        .build());

    private static int currentTick;
    private static boolean holdPacket = false;
    private final List<Packet<?>> packetQueue = new ArrayList<>();

    public TridentDupe() {
        super(FumoUtils.CATEGORY, "trident-dupe", "A better trident dupe module");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (mc.gameMode == null) return;

        if (currentTick > interval.get()) currentTick = 0;

        if (currentTick == 1) {
            if (!mc.player.getInventory().getSelected().is(Items.TRIDENT)) return;
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            holdPacket = true;
        } else if (currentTick == interval.get()) {
            mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                3,
                mc.player.getInventory().selected,
                ClickType.SWAP,
                mc.player
            );

            holdPacket = false;
            ClientPacketListener network = mc.getConnection();
            if (network == null) return;

            for (Packet<?> packet : packetQueue) {
                network.getConnection().send(packet, null, true);
            }
            packetQueue.clear();

            if (drop.get())
                mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    3,
                    0,
                    ClickType.THROW,
                    mc.player
                );
            else
                mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    3,
                    mc.player.getInventory().selected,
                    ClickType.SWAP,
                    mc.player
                );
        }
        currentTick++;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundPlayerActionPacket && holdPacket)) return;

        packetQueue.add(event.packet);
        event.cancel();
    }
}
