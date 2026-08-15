package sh.qnx.fumo.utils.ported;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

// Forked from https://github.com/CunnyCorp/lu-public/blob/1.21.8/src/main/java/pictures/cunny/loli_utils/utility/BlockUtils.java

public class BlockUtils {
    private static final double MAGIC_PLACE_OFFSET = 0.0154;

    public static Direction[] getDirections() {
        return Direction.values();
    }

    public static boolean isReplaceable(BlockPos pos) {
        return mc.player != null && (mc.player.level().getBlockState(pos).isAir()
                || mc.player.level().getBlockState(pos).canBeReplaced()
                || isLiquid(pos));
    }

    public static boolean isLiquid(BlockPos pos) {
        return mc.player != null
                && mc.player.level().getBlockState(pos).getBlock() instanceof LiquidBlock;
    }

    public static boolean isNotAir(BlockPos pos) {
        return mc.player == null || !mc.player.level().getBlockState(pos).isAir();
    }

    public static boolean canPlace(BlockPos pos) {
       return canPlace(pos, 3.75, false);
    }

    public static boolean canPlace(BlockPos pos, double dist) {
        return canPlace(pos, dist, false);
    }

    public static boolean canPlace(BlockPos pos, double dist, boolean liquidPlace) {
        assert mc.player != null;

        List<Entity> entities =
                mc.player
                        .level()
                        .getEntities(null, new AABB(Vec3.atCenterOf(pos).add(3, 3, 3), Vec3.atCenterOf(pos).add(-3, -3, -3))).stream().filter(
                                entity -> {
                                    if (EntityUtils.canPlaceIn(entity)) {
                                        return false;
                                    }

                                    return entity.isColliding(pos, Blocks.BEDROCK.defaultBlockState());
                                }).toList();

        return isReplaceable(pos) && entities.isEmpty() && mc.player.getEyePosition().closerThan(getSafeHitResult(pos).getLocation(), dist) && (liquidPlace ? shouldLiquidPlace(pos) : !shouldAirPlace(pos));
    }

    public static boolean hasEntitiesInside(BlockPos pos) {
        assert mc.player != null;
        List<Entity> entities =
                mc.player
                        .level()
                        .getEntities(null, new AABB(Vec3.atCenterOf(pos).add(3, 3, 3), Vec3.atCenterOf(pos).add(-3, -3, -3))).stream().filter(
                                entity -> {
                                    if (EntityUtils.canPlaceIn(entity)) {
                                        return false;
                                    }

                                    return entity.isColliding(pos, Blocks.BEDROCK.defaultBlockState());
                                }).toList();
        return !entities.isEmpty();
    }

    public static boolean isPlayerInside(BlockPos pos) {
        assert mc.player != null;
        List<Entity> entities =
                mc.player
                        .level()
                        .getEntities(null, new AABB(Vec3.atCenterOf(pos).add(3, 3, 3), Vec3.atCenterOf(pos).add(-3, -3, -3))).stream().filter(
                                entity -> {
                                    if (entity == mc.player) {
                                        return entity.isColliding(pos, Blocks.BEDROCK.defaultBlockState());
                                    }

                                    return false;
                                }).toList();
        return !entities.isEmpty();
    }

    public static Direction getPlaceDirection(BlockPos pos) {
        for (Direction direction : getDirections()) {
            if (isReplaceable(pos.relative(direction))) return direction;
        }
        return Direction.UP;
    }

    public static boolean shouldAirPlace(BlockPos pos) {
        for (Direction direction : getDirections()) {
            if (!BlockUtils.isReplaceable(pos.relative(direction))) return false;
        }
        return true;
    }

    public static boolean shouldLiquidPlace(BlockPos pos) {
        return BlockUtils.isLiquid(pos.relative(Direction.DOWN));
    }


    public static Vec3 clickOffset(BlockPos pos) {
        return clickOffset(pos, getPlaceDirection(pos));
    }

    public static Vec3 clickOffset(BlockPos pos, Direction direction) {
        return Vec3.atCenterOf(pos).add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5);
    }

    public static BlockHitResult getSafeHitResult(BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        Direction direction = Direction.UP;

        Vec3 offset;
        double yHeight = 0;

        for (Direction dir : getDirections()) {
            // Performance!
            mutable.set(pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ());
            if (!isReplaceable(mutable)) {
                yHeight = getHeight(mutable);

                direction = dir;

                if (dir == Direction.DOWN) {
                    break;
                }
            }
        }

        offset = clickOffset(pos, direction);

        if (yHeight <= 0.2) {
            offset = new Vec3(offset.x, Math.floor(offset.y) + MAGIC_PLACE_OFFSET, offset.z);
        }

        return new BlockHitResult(offset, direction.getOpposite(), mutable.set(pos.getX() + direction.getStepX(), pos.getY() + direction.getStepY(), pos.getZ() + direction.getStepZ()), false);
    }

    public static double getHeight(BlockPos pos) {
        return mc.level.getBlockState(pos).getShape(mc.level, pos).max(Direction.Axis.Y);
    }

    public static BlockHitResult getBlockHitResult(boolean raytrace, BlockPos pos, Direction direction) {
        return new BlockHitResult(
                clickOffset(pos),
                direction == null ? getPlaceDirection(pos).getOpposite() : direction,
                pos.relative(direction == null ? getPlaceDirection(pos) : direction.getOpposite()),
                false);
    }

}
