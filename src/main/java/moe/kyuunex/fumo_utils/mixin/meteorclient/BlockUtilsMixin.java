package moe.kyuunex.fumo_utils.mixin.meteorclient;

import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockUtils.class)
public class BlockUtilsMixin {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * @author Kyuunex
     * @reason Replace BlockUtils.getDirection(BlockPos)
     */
    @Overwrite
    public static Direction getDirection(BlockPos pos) {
        double eyePos = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape outline = state.getCollisionShape(mc.level, pos);

        if (eyePos > pos.getY() + outline.max(Direction.Axis.Y) && mc.level.getBlockState(pos.above()).canBeReplaced()) {
            return Direction.UP;
        } else if (eyePos < pos.getY() + outline.min(Direction.Axis.Y) && mc.level.getBlockState(pos.below()).canBeReplaced()) {
            return Direction.DOWN;
        } else {
            BlockPos difference = pos.subtract(mc.player.blockPosition());

            if (Math.abs(difference.getX()) > Math.abs(difference.getZ())) {
                return difference.getX() > 0 ? Direction.WEST : Direction.EAST;
            } else {
                return difference.getZ() > 0 ? Direction.NORTH : Direction.SOUTH;
            }
        }
    }
}
