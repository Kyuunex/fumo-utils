package sh.qnx.fumo.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PearlLoader extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgFlipBack = settings.createGroup("Flip back");

    private final Setting<Mode> operatingMode = sgDefault.add(new EnumSetting.Builder<Mode>()
        .name("operating-mode")
        .description("How to locate the trapdoor")
        .defaultValue(Mode.BLOCK_POS)
        .build()
    );

    public final Setting<BlockPos> trapdoorPosition = sgDefault.add(new BlockPosSetting.Builder()
        .name("trapdoor-position")
        .description("The coords to the stasis chamber trapdoor")
        .defaultValue(BlockPos.ZERO)
        .build());

    private final Setting<String> triggerString = sgDefault.add(new StringSetting.Builder()
        .name("trigger-string")
        .description("The string that will trigger this module")
        .defaultValue("From USERNAME: !load")
        .build()
    );

    private final Setting<Boolean> flipBackTrapdoor = sgFlipBack.add(new BoolSetting.Builder()
        .name("flip-back-trapdoor")
        .description("Whether to flip back the trapdoor")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> flipBackDelay = sgFlipBack.add(new IntSetting.Builder()
        .name("flip-back-delay")
        .description("How long to wait before flipping the trapdoor back")
        .defaultValue(20)
        .sliderRange(1, 100)
        .range(0, 1000)
        .build()
    );

    private int flipBackTimer = -1;

    public PearlLoader() {
        super(
            FumoUtils.CATEGORY,
            "pearl-loader",
            "Remotely load pearls by looking for a specific command in chat"
        );
    }

    @EventHandler(priority = 1)
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();
        String messageString = message.getString();

        if (messageString.startsWith(triggerString.get())) {
            flipTrapdoor(pickTrapdoor());
            if (flipBackTrapdoor.get()) flipBackTimer = 0;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;

        BlockHitResult hitResult = pickTrapdoor();

        if (hitResult == null) return;

        if (flipBackTimer >= flipBackDelay.get()){
            // Trapdoor is open, do nothing and stop it.
            if (mc.level.getBlockState(hitResult.getBlockPos()).getValueOrElse(BlockStateProperties.OPEN, true)) {
                flipBackTimer = -1;
                return;
            }

            flipTrapdoor(hitResult);

            flipBackTimer = 0;
            return;
        }

        if (flipBackTimer == -1) return;

        flipBackTimer++;
    }

    private BlockHitResult pickTrapdoor() {
        if (mc.player == null) return null;

        return switch (operatingMode.get()) {
            case CROSSHAIR -> (BlockHitResult) mc.hitResult;
            case BLOCK_POS -> new BlockHitResult(
                Vec3.atCenterOf(trapdoorPosition.get()),
                Direction.DOWN,
                trapdoorPosition.get(),
                false
            );
        };
    }

    private void flipTrapdoor(BlockHitResult hitResult) {
        if (mc.player == null) return;
        if (hitResult == null) return;

        ClientPacketListener network = mc.getConnection();
        if (network == null) return;
        network.getConnection().send(
            new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, 0),
            null,
            true
        );
        if (mc.level == null) return;
        mc.level.playSound(
            mc.player,
            mc.player,
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.VOICE,
            3.0F,
            1.0F
        );
    }

    private enum Mode {
        BLOCK_POS,
        CROSSHAIR
    }
}
