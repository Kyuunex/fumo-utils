package moe.kyuunex.fumo_utils.modules;

import java.util.List;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoSmith extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Integer> cooldownAmount = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Cooldown between the cycles.")
        .range(0, 2147483647)
        .sliderRange(0, 20)
        .defaultValue(3)
        .build()
    );

    private final Setting<Boolean> dropInstead = sgGeneral.add(new BoolSetting.Builder()
        .name("drop-upgraded-item")
        .description("Instead of shift-clicking, drop the upgraded item.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Item>> itemsToUpgrade = sgGeneral.add(new ItemListSetting.Builder()
        .name("items-to-upgrade")
        .description("A list of items to upgrade.")
        .defaultValue(
            Items.DIAMOND_AXE,
            Items.DIAMOND_BOOTS,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_HOE,
            Items.DIAMOND_HORSE_ARMOR,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_NAUTILUS_ARMOR,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_SHOVEL,
            Items.DIAMOND_SPEAR,
            Items.DIAMOND_SWORD
        )
        .build());

    public AutoSmith() {
        super(
            FumoUtils.CATEGORY,
            "auto-smith",
            "Automatically operate a smithing table to upgrade all upgradable items."
        );
    }

    int flushTimer = 0;

    @Override
    public void onActivate() {
        flushTimer = 0;
    }

    @Override
    public void onDeactivate() {
        flushTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (!(mc.screen instanceof SmithingScreen smither)) return;

        if (flushTimer < cooldownAmount.get()) {
            flushTimer++;
            return;
        }

        if (ensureTemplate()) return;
        if (ensureIngot()) return;
        if (ensureItem()) return;
        if (upgradeItem()) return;

        flushTimer = 0;
    }

    private boolean ensureTemplate() {
        if (mc.player == null) return false;
        ItemStack templateSlot = mc.player.containerMenu.getSlot(0).getItem();
        if (templateSlot.isEmpty()) {
            int slotNumber = findItemInInv(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            if (slotNumber == -1) {
                info("Ran out of smithing templates");
                mc.player.closeContainer();
                return false;
            }
            InvUtils.shiftClick().slotId(slotNumber);
            return true;
        }
        return false;
    }

    private boolean ensureIngot() {
        if (mc.player == null) return false;
        ItemStack templateSlot = mc.player.containerMenu.getSlot(2).getItem();
        if (templateSlot.isEmpty()) {
            int slotNumber = findItemInInv(Items.NETHERITE_INGOT);
            if (slotNumber == -1) {
                info("Ran out of netherite ingots");
                mc.player.closeContainer();
                return false;
            }
            InvUtils.shiftClick().slotId(slotNumber);
            return true;
        }
        return false;
    }

    private boolean ensureItem() {
        if (mc.player == null) return false;
        ItemStack templateSlot = mc.player.containerMenu.getSlot(1).getItem();
        if (templateSlot.isEmpty()) {
            int slotNumber = findUpgradableInInv();
            if (slotNumber == -1) {
                info("Ran out of upgradable items");
                mc.player.closeContainer();
                return false;
            }
            InvUtils.shiftClick().slotId(slotNumber);
            return true;
        }
        return false;
    }

    private boolean upgradeItem() {
        if (mc.player == null) return false;
        ItemStack templateSlot = mc.player.containerMenu.getSlot(3).getItem();
        if (!templateSlot.isEmpty()) {
            if (dropInstead.get()) {
                InvUtils.drop().slotId(3);
            } else {
                InvUtils.shiftClick().slotId(3);
            }
            return true;
        }
        return false;
    }

    private int findItemInInv(Item item) {
        if (mc.player == null) return -1;

        for (int i = 4; i < 40; i++) {
            ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();

            if (stack.is(item)) {
                return i;
            }
        }

        return -1;
    }

    private int findUpgradableInInv() {
        if (mc.player == null) return -1;

        for (int i = 4; i < 40; i++) {
            ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
            if (itemsToUpgrade.get().contains(stack.getItem())) {
                return i;
            }
        }

        return -1;
    }
}
