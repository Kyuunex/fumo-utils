package moe.kyuunex.fumo_utils.modules.highwaytools;

import java.util.ArrayList;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import moe.kyuunex.fumo_utils.utils.DisconnectUtils;
import moe.kyuunex.fumo_utils.utils.ported.InventoryUtils;
import moe.kyuunex.fumo_utils.utils.ported.BlockUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;


public class HighwayPaver extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgInventorySettings = settings.createGroup("Inventory");
    private final SettingGroup sgExperimentalSettings = settings.createGroup("Experimental");

    private final Setting<Integer> interval = sgDefault.add(new IntSetting.Builder()
        .name("interval")
        .description("How long to wait between placing bursts")
        .defaultValue(1)
        .sliderRange(0, 100)
        .range(-1, 1000)
        .build()
    );

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

    private final Setting<Boolean> sideBlocksEnableSetting = sgDefault.add(new BoolSetting.Builder()
        .name("place-blocks-on-side")
        .description("3 blocks wide instead of 1.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Direction> sideDirectionSetting = sgDefault.add(new EnumSetting.Builder<Direction>()
        .name("side-direction")
        .description("Which direction is your right or left. "
            + "If you are walking North, you select East or West, "
            + "and East and West in front of you is paved. "
            + "In Corner Paving mode, only the selected direction is paved, 2 blocks.")
        .defaultValue(Direction.WEST)
        .visible(sideBlocksEnableSetting::get)
        .build()
    );

    private final Setting<Integer> howFarAhead = sgDefault.add(new IntSetting.Builder()
        .name("how-far-ahead")
        .description("How far ahead to place the blocks?")
        .sliderRange(0, 6)
        .defaultValue(3)
        .build()
    );

    private final Setting<Boolean> packet = sgDefault.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Packet place instead of normal place. Recommended so you don't fall off.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgDefault.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Only places blocks in this list.")
        .build()
    );

    public final Setting<Boolean> debugPrint = sgDefault.add(new BoolSetting.Builder()
        .name("debug-print")
        .description("Print debug messages")
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
        .description("")
        .sliderRange(0, 64)
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

    private final Setting<Boolean> noGrimDesyncFixWhenNoMove = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("no-desync-fix-when-stopping")
        .description("You can tell just how jank is needed to deal with grim")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cornerPaveEnableSetting = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("corner-pave")
        .description("Instead of paving 1 block in each direction, you are walking at the corner and paving 2 blocks in only one direction.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> guardRailsEnableSetting = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("guard-rails")
        .description("Add guardrails.")
        .defaultValue(false)
        .build()
    );

    private int timer = -1;
    private int inventoryCooldown = 0;
    public static int yLevel = 118;
    public static boolean stopMovement = false;
    public static Direction diggingDirection = Direction.EAST;
    public static Direction sideDirection = Direction.SOUTH;
    public static boolean sidePavingEnabled = false;
    public static boolean cornerPavingEnabled = false;
    public static boolean guardRailsEnabled = false;

    public HighwayPaver() {
        super(
            FumoUtils.HIGHWAY,
            "highway-paver",
            "Specialized scaffold module to pave a tunnel."
        );
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

        stopMovement = false;
        sideDirection = sideDirectionSetting.get();
        sidePavingEnabled = sideBlocksEnableSetting.get();
        cornerPavingEnabled = cornerPaveEnableSetting.get();
        guardRailsEnabled = guardRailsEnableSetting.get();
    }

    @Override
    public void onDeactivate() {
        stopMovement = false;
        inventoryCooldown = 0;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof ServerboundAcceptTeleportationPacket)) return;

        if (grimDesyncFix.get()) {
            info("Rubber banding detected?");
            if (stopMovement && noGrimDesyncFixWhenNoMove.get()) return;
            BlockPos currentBlockPos = mc.player.blockPosition();
            placeBlock(currentBlockPos.relative(diggingDirection), false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (Modules.get().get(AutoGap.class).isEating()) return;
        if (Modules.get().get(AutoEat.class).eating) return;

        if (timer < interval.get()) {
            timer++;
            return;
        }

        if (offhandReplenish.get() && mc.player.getOffhandItem().getCount() < replenishWhenBelow.get() && inventoryCooldown == 0)
        {
//            FindItemResult results = InvUtils.find(stack -> whitelist.get().stream().anyMatch((block -> block.asItem() == stack.getItem() && stack.getCount() >= replenishWhenBelow.get())));
            int result = findBlockInInv();
            if (result != -1) {
                // InvUtils.move().from(results.slot()).to(40);
                InventoryUtils.swapToHotbar(slotAdjust(result), 40);
                inventoryCooldown = inventoryCooldownDef.get();
                stopMovement = false;
                if (debugPrint.get()) info("replenished from %s to %s, unadjusted %s".formatted(slotAdjust(result), 40, result));
            } else {
                if (disconnectWhenCantReplenish.get()) {
                    ClientPacketListener network = mc.getConnection();
                    DisconnectUtils.disconnect(network, "cannot replenish blocks, none found in inventory!");
                } else {
                    info("cannot replenish blocks, none found in inventory!");
                    inventoryCooldown = inventoryCooldownDef.get();
                    stopMovement = true;
                }
            }
        }

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
                        placeBlock(basePos.above().relative(sideDirection.getOpposite(), 1), packet.get());
                    } else {
                        placeBlock(basePos.above().relative(sideDirection, 2), packet.get());
                        placeBlock(basePos.above().relative(sideDirection.getOpposite(), 2), packet.get());
                    }
                }
            }
        }


        timer = 0;

        if (inventoryCooldown > 0) {
            inventoryCooldown--;
        }
    }

    private void placeBlock(BlockPos pos, boolean packetPlace) {
        if (mc.gameMode == null || mc.player == null) return;

        if (!isPlacable(pos)) return;

        if (packetPlace) {
            sendPacket(new ServerboundUseItemOnPacket(InteractionHand.OFF_HAND, BlockUtils.getSafeHitResult(pos),0));
        } else {
            mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, BlockUtils.getSafeHitResult(pos)).consumesAction();
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

    private int slotAdjust(int slot) {
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

        for (Block block : whitelist.get()) {
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

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (stopMovement) {
            event.movement.x = 0;
            event.movement.y = 0;
            event.movement.z = 0;
        }
    }

    public void sendPacket(Packet<?> packet) {
        ClientPacketListener network = mc.getConnection();
        if (network == null) return;
        network.getConnection().send(packet, null, true);
    }
}
