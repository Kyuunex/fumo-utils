package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import moe.kyuunex.fumo_utils.utils.ported.InventoryUtils;


import java.util.List;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class FumoReplenish extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgPickaxeSettings = settings.createGroup("Pickaxes");
    private final SettingGroup sgFoodSettings = settings.createGroup("Food");

    private final Setting<Boolean> replenishPickaxes = sgPickaxeSettings.add(new BoolSetting.Builder()
        .name("replenish-pickaxes")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fortuneOnlyEnable = sgPickaxeSettings.add(new BoolSetting.Builder()
        .name("fortune-only")
        .description("")
        .defaultValue(false)
        .visible(replenishPickaxes::get)
        .build()
    );

    private final Setting<Integer> targetPickaxeDurability = sgPickaxeSettings.add(new IntSetting.Builder()
        .name("durability")
        .description("Swap below this durability")
        .defaultValue(80)
        .sliderRange(1, Items.NETHERITE_PICKAXE.components().get(DataComponents.MAX_DAMAGE))
        .range(0, Items.NETHERITE_PICKAXE.components().get(DataComponents.MAX_DAMAGE))
        .visible(replenishPickaxes::get)
        .build()
    );

    private final Setting<Integer> preferredPickaxeHotbarSlot = sgPickaxeSettings.add(new IntSetting.Builder()
        .name("preferred-pickaxe-slot")
        .description("")
        .defaultValue(1)
        .range(0, 8)
        .sliderRange(0, 8)
        .visible(replenishPickaxes::get)
        .build()
    );

    private final Setting<Integer> preferredFoodHotbarSlot = sgFoodSettings.add(new IntSetting.Builder()
        .name("preferred-food-slot")
        .description("")
        .defaultValue(2)
        .range(0, 8)
        .sliderRange(0, 8)
        .build()
    );

    public final Setting<List<Item>> itemsToTake = sgFoodSettings.add(new ItemListSetting.Builder()
        .name("items-to-take")
        .description("A list of items to take")
        .defaultValue(
            Items.ENCHANTED_GOLDEN_APPLE
        )
        .visible(() -> Boolean.FALSE)
        .build()
    );

    public final Setting<List<Item>> blacklist = sgFoodSettings.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Which items to not eat.")
        .defaultValue(
            Items.GOLDEN_APPLE,
            Items.CHORUS_FRUIT,
            Items.POISONOUS_POTATO,
            Items.PUFFERFISH,
            Items.CHICKEN,
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.SUSPICIOUS_STEW
        )
        .filter(item -> item.components().get(DataComponents.FOOD) != null)
        .build()
    );

    private final Setting<Integer> cooldown = sgDefault.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Cooldown after restocking")
        .range(0, 2147483647)
        .sliderRange(0, 100)
        .defaultValue(20)
        .build()
    );

    private final Setting<Integer> inventoryCooldownDef = sgDefault.add(new IntSetting.Builder()
        .name("inventory-cooldown")
        .description("Cooldown after restocking")
        .range(0, 2147483647)
        .sliderRange(0, 100)
        .defaultValue(20)
        .build()
    );

    public final Setting<Boolean> debugPrint = sgDefault.add(new BoolSetting.Builder()
        .name("debug-print")
        .description("Print debug messages")
        .defaultValue(false)
        .build()
    );

    public FumoReplenish() {
        super(FumoUtils.CATEGORY, "fumo-replenish", "A very specialized replenish module");
    }

    int timer = 0;
    private int inventoryCooldown = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;
        if (mc.player == null) return;

        if (timer > cooldown.get()) {
            timer = 0;
            return;
        }

        if (replenishPickaxes.get()){
            if (!usablePickaxeInHotbar() && inventoryCooldown == 0) {
                replenishPickaxe();
            }
        }

        if (!foodExistsInHotbar() && inventoryCooldown == 0)
        {
            replenishFood();
        }

        timer++;

        if (inventoryCooldown > 0) {
            inventoryCooldown--;
        }
    }

    private boolean usablePickaxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (stack.is(Items.NETHERITE_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)) {
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                if (remaining > targetPickaxeDurability.get()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void replenishPickaxe(){
        if (mc.player == null) return;

        int pickaxeFoundInSlot = findPickaxeInInv();

        if (pickaxeFoundInSlot != -1) {
            // InvUtils.move().from(pickaxeFoundInSlot).to(preferredPickaxeHotbarSlot.get());
            InventoryUtils.swapToHotbar(pickaxeFoundInSlot, preferredPickaxeHotbarSlot.get());
            inventoryCooldown = inventoryCooldownDef.get();
            if (debugPrint.get()) info("replenished from %s to %s".formatted(pickaxeFoundInSlot, preferredPickaxeHotbarSlot.get()));
            info("replenished pickaxe!");
        }
    }

    private int findPickaxeInInv() {
        if (mc.player == null) return -1;

        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (stack.is(Items.NETHERITE_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)) {

                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                if (remaining <= targetPickaxeDurability.get()) continue;

                if (!fortuneOnlyEnable.get()) {
                    return i;
                }
                if (EnchantmentHelper.getItemEnchantmentLevel(
                    mc.player.level().registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                    stack
                ) > 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean foodExistsInHotbar() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            if (item.components().get(DataComponents.FOOD) == null) continue;
            if (blacklist.get().contains(item)) continue;

            return true;
        }

        return false;
    }

    private void replenishFood(){
        if (mc.player == null) return;

        int foodFoundInSlot = findFoodInInv();

        if (foodFoundInSlot != -1) {
            // InvUtils.move().from(foodFoundInSlot).to(preferredFoodHotbarSlot.get());
            InventoryUtils.swapToHotbar(foodFoundInSlot, preferredFoodHotbarSlot.get());
            inventoryCooldown = 4;
            info("replenished food!");
            if (debugPrint.get()) info("replenished from %s to %s".formatted(foodFoundInSlot, preferredFoodHotbarSlot.get()));
        }
    }

    private int findFoodInInv() {
        if (mc.player == null) return -1;

        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            if (item.components().get(DataComponents.FOOD) != null && !blacklist.get().contains(item)) {
                return i;
            }
        }

        return -1;
    }
}
