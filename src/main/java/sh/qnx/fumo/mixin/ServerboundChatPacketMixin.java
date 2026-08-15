package sh.qnx.fumo.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import sh.qnx.fumo.modules.Unreportable;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerboundChatPacket.class)
public class ServerboundChatPacketMixin {

    @Shadow
    @Final
    @Mutable
    private MessageSignature signature;

    @Shadow
    @Final
    @Mutable
    private long salt;

    @Inject(
        method = "<init>(Ljava/lang/String;Ljava/time/Instant;JLnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/network/chat/LastSeenMessages$Update;)V",
        at = @At("RETURN")
    )
    private void onInit(
        String message,
        java.time.Instant timeStamp,
        long salt,
        MessageSignature signature,
        net.minecraft.network.chat.LastSeenMessages.Update lastSeenMessages,
        CallbackInfo ci
    ) {
        Unreportable unreportable = Modules.get().get(Unreportable.class);
        if (unreportable != null && unreportable.isActive()) {
            this.signature = null;
            this.salt = 0;
        }
    }
}
