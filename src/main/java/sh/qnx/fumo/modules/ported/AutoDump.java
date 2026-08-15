package sh.qnx.fumo.modules.ported;

import java.util.List;

import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;

public class AutoDump extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    public final Setting<Integer> rate =
        sgDefault.add(
            new IntSetting.Builder()
                .name("rate")
                .description("The rate to move items per tick.")
                .defaultValue(6)
                .sliderRange(1, 20)
                .build());
    public final Setting<List<Item>> items =
        sgDefault.add(
            new ItemListSetting.Builder()
                .name("items")
                .description("A list of items to dump.")
                .defaultValue(
                    Items.SHULKER_BOX // TODO: this is missing colored shulkers
                )
                .build());
    public final Setting<List<MenuType<?>>> screens =
        sgDefault.add(
            new ScreenHandlerListSetting.Builder()
                .name("screens")
                .description("The screens to dump items into.")
                .defaultValue(List.of(MenuType.GENERIC_9x3, MenuType.GENERIC_9x6))
                .build());

    public AutoDump() {
        super(FumoUtils.CATEGORY, "auto-dump", "Automatically dump items into chests, skid of meteors but won't time you out.");
    }

    @EventHandler
    public void onTick(TickEvent.Post tickEvent) {
        if (!canUseScreen()) {
            return;
        }

        int r = 0;

        for (int i = SlotUtils.indexToId(SlotUtils.MAIN_START); i < SlotUtils.indexToId(SlotUtils.MAIN_START) + 4 * 9; i++) {
            if (r >= rate.get()) break;
            if (mc.player == null) return;
            if (!mc.player.containerMenu.getSlot(i).hasItem()) continue;
            if (!items.get().contains(mc.player.containerMenu.getSlot(i).getItem().getItem())) continue;

            r++;
            InvUtils.shiftClick().slotId(i);
        }
    }

    public boolean canUseScreen() {
        try {
            return mc.player != null && screens.get().contains(mc.player.containerMenu.getType());
        } catch (Exception e) {
            return false;
        }
    }
}
