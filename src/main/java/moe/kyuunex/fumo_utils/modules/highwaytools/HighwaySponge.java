package moe.kyuunex.fumo_utils.modules.highwaytools;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import moe.kyuunex.fumo_utils.FumoUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public class HighwaySponge extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
        .name("shape")
        .description("The shape of placing algorithm.")
        .defaultValue(Shape.Sphere)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range at which blocks can be placed.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between actions in ticks.")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to place per tick.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("The blocks you want to place first.")
        .defaultValue(SortMode.Furthest)
        .build()
    );

    // Whitelist and blacklist

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The allowed blocks that it will use to fill up the liquid.")
        .defaultValue(
            Blocks.NETHERRACK
        )
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("The denied blocks that it not will use to fill up the liquid.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final List<BlockPos.MutableBlockPos> blocks = new ArrayList<>();

    private int timer;

    public HighwaySponge() {
        super(
            FumoUtils.HIGHWAY,
            "highway-sponge",
            "Plug lava source with netherrack. Must be with offhand!"
        );
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Update timer according to delay
        if (timer < delay.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        // Find slot with a block
        FindItemResult item;
        if (listMode.get() == ListMode.Whitelist) {
            item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && whitelist.get().contains(Block.byItem(itemStack.getItem())));
        } else {
            item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && !blacklist.get().contains(Block.byItem(itemStack.getItem())));
        }
        if (!item.found()) return;

        // Loop blocks around the player
        BlockIterator.register((int) Math.ceil(placeRange.get() + 1), (int) Math.ceil(placeRange.get()), (blockPos, blockState) -> {

            // Check raycast and range
            if (isOutOfRange(blockPos)) return;

            // Check if the block is a source block and set to be filled
            Fluid fluid = blockState.getFluidState().getType();
            if (fluid != Fluids.LAVA)
                return;

            // Check if the player can place at pos
            if (!BlockUtils.canPlace(blockPos)) return;

            // Add block
            blocks.add(blockPos.mutable());
        });

        BlockIterator.after(() -> {
            // Sort blocks
            if (sortMode.get() == SortMode.TopDown || sortMode.get() == SortMode.BottomUp)
                blocks.sort(Comparator.comparingDouble(value -> value.getY() * (sortMode.get() == SortMode.BottomUp ? 1 : -1)));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            // Place and clear place positions
            int count = 0;
            for (BlockPos pos : blocks) {
                if (count >= maxBlocksPerTick.get()) break;
                placeBlock(pos);
                count++;
            }
            blocks.clear();
        });
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum SortMode {
        None,
        Closest,
        Furthest,
        TopDown,
        BottomUp
    }

    public enum Shape {
        Sphere,
        UniformCube
    }

    private boolean isOutOfRange(BlockPos blockPos) {
        if (mc.player == null) return true;
        if (!isWithinShape(blockPos, placeRange.get())) return true;

        ClipContext clipContext = new ClipContext(mc.player.getEyePosition(), blockPos.getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult result = mc.level.clip(clipContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !isWithinShape(blockPos, placeRange.get());

        return false;
    }

    private boolean isWithinShape(BlockPos blockPos, double range) {
        if (mc.player == null) return false;
        // Cube shape
        if (shape.get() == Shape.UniformCube) {
            BlockPos playerBlockPos = mc.player.blockPosition();
            double dX = Math.abs(blockPos.getX() - playerBlockPos.getX());
            double dY = Math.abs(blockPos.getY() - playerBlockPos.getY());
            double dZ = Math.abs(blockPos.getZ() - playerBlockPos.getZ());
            double maxDist = Math.max(Math.max(dX, dY), dZ);
            return maxDist <= Math.floor(range);
        }

        // Spherical shape
        return PlayerUtils.isWithin(blockPos.getCenter(), range);
    }

    private void placeBlock(BlockPos pos) {
        if (mc.player == null) return;
        if (mc.gameMode == null) return;

        mc.gameMode.useItemOn(
            mc.player,
            InteractionHand.OFF_HAND,
            new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
            )
        ).consumesAction();
    }
}
