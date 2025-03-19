package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class SleepAura extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    public final Setting<Boolean> toggleAfteruse = sgDefault.add(new BoolSetting.Builder()
        .name("toggle-after-use")
        .description("Toggle after sleeping once")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> enableSchedule = sgDefault.add(new BoolSetting.Builder()
        .name("enable-schedule")
        .description("Enable schedule")
        .defaultValue(false)
        .build());

    private final Setting<Integer> scheduleTime = sgDefault.add(new IntSetting.Builder()
        .name("schedule-time")
        .description("The specified time to sleep at.")
        .defaultValue(22000)
        .sliderRange(0, 24000)
        .build()
    );
    private final Setting<Integer> scheduleTimeRain = sgDefault.add(new IntSetting.Builder()
        .name("schedule-time-rain")
        .description("The specified time to sleep at.")
        .defaultValue(23000)
        .sliderRange(0, 24000)
        .build()
    );

    private BlockPos bedPos;
    private long serverTime;
    private int attemptTime;

    public SleepAura() {
        super(FumoUtils.CATEGORY, "sleep-aura", "Bed aura, but meant for sleeping");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;

        if (!mc.level.dimensionType().bedWorks()) {
            error("Works only in The Overworld! Disabling");
            toggle();
            return;
        }

        if (mc.player.isSleeping()) return;

        if (enableSchedule.get()) {
            if (mc.level.isRaining()){
                attemptTime = scheduleTimeRain.get();
            } else {
                attemptTime = scheduleTime.get();
            }
            if (mc.level.dayTime() % 24000 != attemptTime) return;
        } else {
            if (!mc.level.isNight()) return;
        }

        if (bedPos == null) bedPos = findBed();
        sleepBed(bedPos);
    }

    private BlockPos findBed() {
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof BedBlockEntity)) continue;

            BlockPos bedPos = blockEntity.getBlockPos();
            Vec3 bedVec = Utils.vec3d(bedPos);

            if (PlayerUtils.isWithinReach(bedVec)) {
                info("reachable bed found");
                return bedPos;
            }
        }

        return null;
    }


    private void sleepBed(BlockPos pos) {
        if (pos == null) return;
        bedPos = null;

        if (!(mc.level.getBlockState(pos).getBlock() instanceof BedBlock)) return;

        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atCenterOf(pos),
            Direction.UP,
            pos,
            false
        );
        ClientPacketListener network = mc.getConnection();
        if (network == null) return;
        network.getConnection().send(
            new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, 0),
            null,
            true
        );

        info("packet send!");
        if (toggleAfteruse.get()) toggle();
    }

    @Override
    public String getInfoString() {
        if (mc.level == null) return "";
        return String.format(
            "packet: %s, client: %s, target %s, rain %s",
            serverTime % 24000,
            mc.level.dayTime() % 24000,
            scheduleTime.get(),
            scheduleTimeRain.get()
        );
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundSetTimePacket) {
            serverTime = ((ClientboundSetTimePacket) event.packet).dayTime();
        }
    }
}
