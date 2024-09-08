package moe.kyuunex.fumo_utils.modules;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

public class DoujinDupe extends Module {
    private int idleTimer = 0;
    private int cycleCount = 0;
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    public final Setting<Integer> idleDelay =
        sgDefault.add(
            new IntSetting.Builder()
                .name("idle-delay")
                .description("The time to idle when joining in ticks.")
                .defaultValue(40)
                .sliderRange(10, 60)
                .build());
    public final Setting<Boolean> autoDrop =
        sgDefault.add(
            new BoolSetting.Builder()
                .name("auto-drop")
                .description("Automatically drop items.")
                .defaultValue(true)
                .build());
    public final Setting<Integer> drops =
        sgDefault.add(
            new IntSetting.Builder()
                .name("drops")
                .description("How many items to drop per tick.")
                .defaultValue(6)
                .sliderRange(1, 20)
                .build());
    public final Setting<List<Item>> dupedItems =
        sgDefault.add(
            new ItemListSetting.Builder()
                .name("duped-items")
                .description("A list of items to wait to be out of inventory before duping.")
                .defaultValue(
                    Items.SHULKER_BOX,
                    Items.WHITE_SHULKER_BOX,
                    Items.ORANGE_SHULKER_BOX,
                    Items.MAGENTA_SHULKER_BOX,
                    Items.LIGHT_BLUE_SHULKER_BOX,
                    Items.YELLOW_SHULKER_BOX,
                    Items.LIME_SHULKER_BOX,
                    Items.PINK_SHULKER_BOX,
                    Items.GRAY_SHULKER_BOX,
                    Items.LIGHT_GRAY_SHULKER_BOX,
                    Items.CYAN_SHULKER_BOX,
                    Items.PURPLE_SHULKER_BOX,
                    Items.BLUE_SHULKER_BOX,
                    Items.BROWN_SHULKER_BOX,
                    Items.GREEN_SHULKER_BOX,
                    Items.RED_SHULKER_BOX,
                    Items.BLACK_SHULKER_BOX
                    )
                .build());
    public final Setting<Boolean> bookCheck =
        sgDefault.add(
            new BoolSetting.Builder()
                .name("book-check")
                .description("Safety check for a book in offhand.")
                .defaultValue(true)
                .build());
    public final Setting<Boolean> cycle =
        sgDefault.add(
            new BoolSetting.Builder()
                .name("cycle")
                .description("Whether or not to dupe via cycle.")
                .defaultValue(false)
                .onChanged(c -> cycleCount = 0)
                .build());
    public final Setting<Integer> cycles =
        sgDefault.add(
            new IntSetting.Builder()
                .name("cycles")
                .description("How many dupe cycles to run.")
                .defaultValue(40)
                .sliderRange(1, 200)
                .onChanged(c -> cycleCount = 0)
                .build());

    public DoujinDupe() {
        super(FumoUtils.CATEGORY, "doujin-dupe", "An automation to 1.20.5+ book dupe.");
    }

    @Override
    public void onActivate() {
        idleTimer = idleDelay.get();
        if (cycle.get() && cycleCount == cycles.get()) {
            if (this.isActive()) {
                cycleCount = 0;
                info("Finished " + cycles.get() + " cycles, disabling.");
                this.toggle();
            }
        }

        if (!isActive()) {
            cycleCount = 0;
        }
    }

    @Override
    public void onDeactivate() {
        onActivate();
    }

    @EventHandler
    public void onTick(TickEvent.Post tickEvent) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }

        idleTimer--;

        if (idleTimer > 0) return;

        if (mc.player.getOffHandStack().getItem() != Items.WRITABLE_BOOK && bookCheck.get()) {
            warning("No writable book in offhand, disabling.");
            toggle();
            return;
        }

        FindItemResult itemResult = InvUtils.find(itemStack -> dupedItems.get().contains(itemStack.getItem()));

        if (itemResult.found()) {
            if (autoDrop.get()) {
                for (int i = 0; i < drops.get(); i++) {
                    FindItemResult nItemResult = InvUtils.find(itemStack -> dupedItems.get().contains(itemStack.getItem()));
                    if (!nItemResult.found()) break;
                    InvUtils.drop().slot(nItemResult.slot());
                }
            }
        } else {
            writeDoujin();
            if (cycle.get()) {
                cycleCount++;
            }
            idleTimer = idleDelay.get();
        }
    }

    public void writeDoujin() {
        send_packet(new BookUpdateC2SPacket(
            40,
            List.of("A"),
            Optional.of(randomText(33))));
    }

    public static String randomText(int amount) {
        StringBuilder str = new StringBuilder();
        Random random = new Random(System.currentTimeMillis());
        int leftLimit = 48;
        int rightLimit = 122;

        for (int i = 0; i < amount; i++) {
            str.append((char) (leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1))));
        }
        return str.toString();
    }

    public void send_packet(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        ClientConnection connection = mc.getNetworkHandler().getConnection();
        if (connection == null) {
            FumoUtils.LOG.error("Connection is null");
            return;
        }

        connection.channel.writeAndFlush(packet);
    }
}
