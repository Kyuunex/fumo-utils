package sh.qnx.fumo.modules.highwaytools;

import java.util.ArrayList;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.orbit.EventHandler;
import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import sh.qnx.fumo.utils.DisconnectUtils;
import sh.qnx.fumo.utils.ported.InventoryUtils;
import sh.qnx.fumo.utils.ported.BlockUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;


public class HighwayPaver extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgPlacementSettings = settings.createGroup("Placement");
    private final SettingGroup sgInventorySettings = settings.createGroup("Inventory");
    private final SettingGroup sgExperimentalSettings = settings.createGroup("Experimental");

    private final Setting<Boolean> forceYLevelEnable = sgDefault.add(new BoolSetting.Builder()
        .name("forced-y-level")
        .description("Force Y level instead of guessing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> forcedYLevel = sgDefault.add(new IntSetting.Builder()
        .name("y-level")
        .description("Y Level to place on.")
        .sliderRange(62, 320)
        .range(-64, 320)
        .defaultValue(118)
        .visible(forceYLevelEnable::get)
        .build()
    );

    private final Setting<Boolean> forcedDirectionEnable = sgDefault.add(new BoolSetting.Builder()
        .name("forced-direction")
        .description("Force digging direction instead of guessing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Direction> forcedDirection = sgDefault.add(new EnumSetting.Builder<Direction>()
        .name("forced-direction")
        .description("In which direction are you digging?")
        .defaultValue(Direction.WEST)
        .visible(forcedDirectionEnable::get)
        .build()
    );

    public final Setting<Boolean> debugPrint = sgDefault.add(new BoolSetting.Builder()
        .name("debug-print")
        .description("Print debug messages")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> placeCooldownDef = sgPlacementSettings.add(new IntSetting.Builder()
        .name("placement-burst-cooldown")
        .description("How long to wait between placing bursts.")
        .defaultValue(10)
        .sliderRange(0, 100)
        .range(-1, 1000)
        .build()
    );

    private final Setting<Boolean> sideBlocksEnableSetting = sgPlacementSettings.add(new BoolSetting.Builder()
        .name("place-blocks-on-side")
        .description("3 blocks wide instead of 1.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Direction> sideDirectionSetting = sgPlacementSettings.add(new EnumSetting.Builder<Direction>()
        .name("side-direction")
        .description("Which direction is your right or left. "
            + "If you are walking North, you select East or West, "
            + "and East and West in front of you is paved. "
            + "In Corner Paving mode, only the selected direction is paved, 2 blocks.")
        .defaultValue(Direction.WEST)
        .visible(sideBlocksEnableSetting::get)
        .build()
    );

    private final Setting<Integer> howFarAhead = sgPlacementSettings.add(new IntSetting.Builder()
        .name("how-far-ahead")
        .description("How far ahead to place the blocks?")
        .sliderRange(0, 6)
        .defaultValue(3)
        .build()
    );

    private final Setting<Boolean> packet = sgPlacementSettings.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Packet place instead of normal place. Recommended so you don't fall off.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgPlacementSettings.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Only places blocks in this list.")
        .defaultValue(
            Blocks.NETHERRACK,
            Blocks.BLACKSTONE,
            Blocks.BASALT
        )
        .build()
    );

    private final Setting<Boolean> cornerPaveEnableSetting = sgPlacementSettings.add(new BoolSetting.Builder()
        .name("corner-pave")
        .description("Instead of paving 1 block in each direction, you are walking at the corner and paving 2 blocks in only one direction.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> guardRailsEnableSetting = sgPlacementSettings.add(new BoolSetting.Builder()
        .name("guard-rails")
        .description("Add guardrails.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> offhandReplenish = sgInventorySettings.add(new BoolSetting.Builder()
        .name("replenish")
        .description("Replenish the offhand slot with blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> inventoryCooldownDef = sgInventorySettings.add(new IntSetting.Builder()
        .name("inventory-cooldown")
        .description("Cooldown after restocking")
        .range(0, 2147483647)
        .sliderRange(0, 100)
        .defaultValue(20)
        .build()
    );

    private final Setting<Integer> replenishWhenBelow = sgInventorySettings.add(new IntSetting.Builder()
        .name("replenish-when-below")
        .description("Replenish when item stack count is less than this.")
        .sliderRange(1, 64)
        .defaultValue(16)
        .build()
    );

    private final Setting<Boolean> disconnectWhenCantReplenish = sgInventorySettings.add(new BoolSetting.Builder()
        .name("disconnect-when-cant-replenish")
        .description("")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> grimDesyncFix = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("grim-desync-fix")
        .description("Place a block in front of you, to force grim resync")
        .defaultValue(true)
        .build()
    );

    private int inventoryCooldown = 0;
    private int placingCycleCooldown = 0;
    public static int yLevel = 118;
    public static Direction diggingDirection = Direction.EAST;
    public static Direction sideDirection = Direction.SOUTH;
    public static boolean sidePavingEnabled = false;
    public static boolean cornerPavingEnabled = false;
    public static boolean guardRailsEnabled = false;
    public static List<Block> pavingBlocks = new ArrayList<>();

    private void notice(String notice) {
        if (debugPrint.get()) {
            info(notice);
        }
    }

    public HighwayPaver() {
        super(
            FumoUtils.HIGHWAY,
            "highway-paver",
            "Specialized scaffold module to pave a tunnel."
        );
        pavingBlocks = List.copyOf(whitelist.get());
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        if (forcedDirectionEnable.get()) {
            diggingDirection = forcedDirection.get();
        } else {
            diggingDirection = mc.player.getDirection();
        }

        if (forceYLevelEnable.get()) {
            yLevel = forcedYLevel.get();
        } else {
            yLevel = mc.player.getBlockY() - 1;
        }

        sideDirection = sideDirectionSetting.get();
        sidePavingEnabled = sideBlocksEnableSetting.get();
        cornerPavingEnabled = cornerPaveEnableSetting.get();
        guardRailsEnabled = guardRailsEnableSetting.get();
        pavingBlocks = List.copyOf(whitelist.get());
    }

    @Override
    public void onDeactivate() {
        inventoryCooldown = 0;
        placingCycleCooldown = 0;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof ServerboundAcceptTeleportationPacket)) return;

        if (Modules.get().get(AutoGap.class).isEating()) return;
        if (Modules.get().get(AutoEat.class).eating) return;

        if (grimDesyncFix.get()) {
            notice("Rubber banding detected?");
            BlockPos currentBlockPos = mc.player.blockPosition();
            placeBlock(currentBlockPos.relative(diggingDirection), false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (Modules.get().get(AutoGap.class).isEating()) return;
        if (Modules.get().get(AutoEat.class).eating) return;

        if (HighwayAligner.misaligned) {
            return;
        }

        if (offhandReplenish.get()
            && mc.player.getOffhandItem().getCount() < replenishWhenBelow.get()
            && inventoryCooldown == 0
            && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
            && mc.player.getOffhandItem().getItem().components().get(DataComponents.FOOD) == null
        )
        {
            int result = findBlockInInv();
            if (result != -1) {
                InventoryUtils.swapToHotbar(toContainerId(result), 40);
                inventoryCooldown = inventoryCooldownDef.get();
                notice("replenished from %s to %s, unadjusted %s".formatted(toContainerId(result), 40, result));
            } else {
                if (disconnectWhenCantReplenish.get()) {
                    DisconnectUtils.disconnect(mc.getConnection(), "cannot replenish blocks, none found in inventory!");
                } else {
                    info("cannot replenish blocks, none found in inventory!");
                    inventoryCooldown = inventoryCooldownDef.get();
                }
            }
        }

        if (placingCycleCooldown == 0) {
            BlockPos playerBlockPos = mc.player.blockPosition();

            for (int i = 0; i <= howFarAhead.get(); i++) {
                BlockPos basePos = playerBlockPos.atY(yLevel).relative(diggingDirection, i);
                placeBlock(basePos, packet.get());
                if (sidePavingEnabled) {
                    placeBlock(basePos.relative(sideDirection), packet.get());
                    if (cornerPavingEnabled) {
                        placeBlock(basePos.relative(sideDirection, 2), packet.get());
                    } else {
                        placeBlock(basePos.relative(sideDirection.getOpposite()), packet.get());
                    }

                    if (guardRailsEnabled) {
                        if (cornerPavingEnabled) {
                            placeBlock(basePos.above().relative(sideDirection, 3), packet.get());
                            placeBlock(basePos.above().relative(sideDirection.getOpposite(), 1),
                                packet.get());
                        } else {
                            placeBlock(basePos.above().relative(sideDirection, 2), packet.get());
                            placeBlock(basePos.above().relative(sideDirection.getOpposite(), 2),
                                packet.get());
                        }
                    }
                }
            }
            placingCycleCooldown = placeCooldownDef.get();
        }

        if (placingCycleCooldown > 0) {
            placingCycleCooldown--;
        }

        if (inventoryCooldown > 0) {
            inventoryCooldown--;
        }
    }

    private void placeBlock(BlockPos pos, boolean packetPlace) {
        if (mc.player == null) return;
        if (mc.gameMode == null) return;

        if (!isPlacable(pos)) return;

        if (packetPlace) {
            sendPacket(
                new ServerboundUseItemOnPacket(
                    InteractionHand.OFF_HAND,
                    BlockUtils.getSafeHitResult(pos),0
                )
            );
        } else {
            mc.gameMode.useItemOn(
                mc.player,
                InteractionHand.OFF_HAND,
                BlockUtils.getSafeHitResult(pos)
            ).consumesAction();
        }
    }

    private boolean isPlacable(BlockPos pos){
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.liquid()) return true;
        if (state.getBlock() == Blocks.FIRE) return true;
        if (state.isSolid()) return false;
        return false;
    }

    private int toContainerId(int slot) {
        // I have no idea what Mojang was smoking to make this necessary

        if (slot == -1) return -1;
        if (slot < 9) {
            return slot + 36;
        }
        return slot;
    }

    private int findBlockInInv() {
        if (mc.player == null) return -1;

        List<Item> items = new ArrayList<>();

        for (Block block : pavingBlocks) {
            items.add(block.asItem());
        }

        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);

            if (items.contains(itemStack.getItem()) && itemStack.getCount() >= replenishWhenBelow.get() ) {
                return i;
            }
        }

        return -1;
    }

    public static boolean slotHasPavingBlock(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        if (itemStack.isEmpty()) {
            return false;
        }
        for (Block block : pavingBlocks) {
            if (itemStack.is(block.asItem())) {
                return true;
            }
        }
        return false;
    }

    public void sendPacket(Packet<?> packet) {
        ClientPacketListener network = mc.getConnection();
        if (network == null) return;
        network.getConnection().send(packet, null, true);
    }
}
