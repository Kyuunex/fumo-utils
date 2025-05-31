package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import moe.kyuunex.fumo_utils.FumoUtils;

public class AutoDropJunk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> autoDropItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("auto-drop-items")
        .description("Items to drop.")
        .build()
    );

    private final Setting<Boolean> autoDropExcludeEquipped = sgGeneral.add(new BoolSetting.Builder()
        .name("exclude-equipped")
        .description("Whether or not to drop items equipped in armor slots.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoDropExcludeHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("exclude-hotbar")
        .description("Whether or not to drop items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDropOnlyFullStacks = sgGeneral.add(new BoolSetting.Builder()
        .name("only-full-stacks")
        .description("Only drops the items if the stack is full.")
        .defaultValue(false)
        .build()
    );

    public AutoDropJunk() {
        super(FumoUtils.CATEGORY, "auto-drop-junk", "Auto drop as a standalone module");
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (mc.screen instanceof AbstractContainerScreen<?> || autoDropItems.get().isEmpty()) return;

        for (int i = autoDropExcludeHotbar.get() ? 9 : 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);

            if (autoDropItems.get().contains(itemStack.getItem())) {
                if ((!autoDropOnlyFullStacks.get() || itemStack.getCount() == itemStack.getMaxStackSize()) &&
                    !(autoDropExcludeEquipped.get() && SlotUtils.isArmor(i))) InvUtils.drop().slot(i);
            }
        }
    }
}
