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
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

public class UnSilkToucher extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<List<Block>> sourceBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("source-blocks")
        .description("Blocks to re-break")
        .defaultValue(Blocks.DEEPSLATE)
        .build()
    );
    public final Setting<List<Item>> targetItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("target-items")
        .description("Items to aim for")
        .defaultValue(Items.COBBLED_DEEPSLATE)
        .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Disables when you reach the desired amount of target block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreExisting = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-existing")
        .description("Ignores existing target blocks in your inventory and mines the total target amount.")
        .defaultValue(true)
        .visible(selfToggle::get)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of target blocks to farm.")
        .defaultValue(64)
        .sliderMax(128)
        .range(8, 512)
        .sliderRange(8, 128)
        .visible(selfToggle::get)
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
        .description("Renders a block overlay where the target blocks will be placed.")
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

    private final VoxelShape SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    private BlockPos target;
    private int startCount;

    public UnSilkToucher() {
        super(FumoUtils.CATEGORY, "un-silk-toucher", "Places and breaks blocks to get other blocks.");
    }

    @Override
    public void onActivate() {
        target = null;
        startCount = InvUtils.find(targetItems.get().getFirst()).count();
    }

    @Override
    public void onDeactivate() {
        InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Finding target pos
        if (target == null) {
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            BlockState state = mc.world.getBlockState(pos);

            if (state.isReplaceable() || state.getBlock() == sourceBlocks.get().getFirst()) {
                target = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            } else return;
        }

        // Disable if the block is too far away
        if (!PlayerUtils.isWithinReach(target)) {
            error("Target block pos out of reach.");
            target = null;
            return;
        }

        // Toggle if obby amount reached
        if (selfToggle.get() && InvUtils.find(targetItems.get().getFirst()).count() - (ignoreExisting.get() ? startCount : 0) >= amount.get()) {
            InvUtils.swapBack();
            toggle();
            return;
        }

        // Break existing source block at target pos
        if (mc.world.getBlockState(target).getBlock() == sourceBlocks.get().getFirst()) {
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (Utils.hasEnchantment(itemStack, Enchantments.SILK_TOUCH)) continue;

                double score = itemStack.getMiningSpeedMultiplier(sourceBlocks.get().getFirst().getDefaultState());

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) return;

            InvUtils.swap(bestSlot, true);
            BlockUtils.breakBlock(target, swingHand.get());
        }

        // Place source block if the target pos is empty
        if (mc.world.getBlockState(target).isReplaceable()) {
            FindItemResult sourceBlock1 = InvUtils.findInHotbar(sourceBlocks.get().getFirst().asItem());

            if (!sourceBlock1.found()) {
                error("No Source blocks in hotbar, disabling");
                toggle();
                return;
            }

            BlockUtils.place(target, sourceBlock1, true, 0, true);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null || !render.get() || Modules.get().get(PacketMine.class).isMiningBlock(target)) return;

        Box box = SHAPE.getBoundingBoxes().getFirst();
        event.renderer.box(target.getX() + box.minX, target.getY() + box.minY, target.getZ() + box.minZ, target.getX() + box.maxX, target.getY() + box.maxY, target.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
