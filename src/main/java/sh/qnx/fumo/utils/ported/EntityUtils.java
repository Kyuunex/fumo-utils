package sh.qnx.fumo.utils.ported;


import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

// Forked from https://github.com/CunnyCorp/lu-public/blob/1.21.8/src/main/java/pictures/cunny/loli_utils/utility/EntityUtils.java

public class EntityUtils {
    private static final List<EntityType<?>> collidable =
            List.of(EntityTypes.ITEM, EntityTypes.TRIDENT, EntityTypes.ARROW, EntityTypes.AREA_EFFECT_CLOUD);

    public static boolean canPlaceIn(Entity entity) {
        return collidable.contains(entity.getType()) || entity.isRemoved() || entity.isSpectator();
    }

    // For use in multi-ticking behavior, lmao?
    public static boolean isTouchingGround() {
        // Account for slabs.
        assert mc.player != null;
        if (BlockUtils.isNotAir(mc.player.blockPosition())) {
            double yS = mc.player.getBlockStateOn().getInteractionShape(mc.level, mc.player.getOnPos()).max(Direction.Axis.Y);

            // Phased ?
            if (yS == 1) {
                return true;
            }

            return mc.player.getY() - (Math.round(mc.player.getY()) + yS) < 0.02;
        }

        return mc.player.getY() - Math.round(mc.player.getY()) < 0.02;
    }
}
