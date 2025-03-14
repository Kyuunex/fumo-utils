package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.PacketMine;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import moe.kyuunex.fumo_utils.FumoUtils;

public class QuartzFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Disables when the Elytra is fully repaired.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the Quartz Ore will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    private BlockPos target;

    public QuartzFarmer() {
        super(FumoUtils.CATEGORY, "quartz-farmer", "Places and breaks Quartz Ores to farm EXP.");
    }

    @Override
    public void onActivate() {
        target = null;
    }

    @Override
    public void onDeactivate() {
        InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Finding target pos
        if (target == null) {
            if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos().above();
            BlockState state = mc.level.getBlockState(pos);

            if (state.canBeReplaced() || state.getBlock() == Blocks.NETHER_QUARTZ_ORE) {
                target = ((BlockHitResult) mc.hitResult).getBlockPos().above();
            } else return;
        }

        // Disable if the block is too far away
        if (!PlayerUtils.isWithinReach(target)) {
            error("Target block pos out of reach.");
            target = null;
            return;
        }

        // Toggle if quartz amount reached
        if (selfToggle.get()) {
            if (mc.player == null) return;
            ItemStack itemStack = mc.player.getInventory().armor.get(2);

            if (itemStack.getDamageValue() == 0) {
                InvUtils.swapBack();
                toggle();
                info(Component.literal(itemStack.getHoverName().getString() + " is fully repaired, disabling."));
                return;
            }
        }

        // Break existing Quartz Ore at target pos
        if (mc.level.getBlockState(target).getBlock() == Blocks.NETHER_QUARTZ_ORE) {
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = mc.player.getInventory().getItem(i);
                if (Utils.hasEnchantment(itemStack, Enchantments.SILK_TOUCH)) continue;

                double score = itemStack.getDestroySpeed(Blocks.NETHER_QUARTZ_ORE.defaultBlockState());

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) return;

            InvUtils.swap(bestSlot, true);
            BlockUtils.breakBlock(target, swingHand.get());
        }

        // Place Quartz Ore if the target pos is empty
        if (mc.level.getBlockState(target).canBeReplaced()) {
            FindItemResult quartz_ore = InvUtils.findInHotbar(Items.NETHER_QUARTZ_ORE);

            if (!quartz_ore.found()) {
                error("No Quartz Ore in hotbar, disabling");
                toggle();
                return;
            }

            BlockUtils.place(target, quartz_ore, true, 0, true);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null || !render.get() || Modules.get().get(PacketMine.class).isMiningBlock(target)) return;

        AABB box = SHAPE.toAabbs().getFirst();
        event.renderer.box(target.getX() + box.minX, target.getY() + box.minY, target.getZ() + box.minZ, target.getX() + box.maxX, target.getY() + box.maxY, target.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
