package sh.qnx.fumo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;

import meteordevelopment.meteorclient.systems.modules.Modules;
import sh.qnx.fumo.modules.HostOverride;

@Mixin(ClientIntentionPacket.class)
public class ClientIntentionPacketMixin {

    @Mutable
    @Shadow
    private String hostName;

    @Inject(method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V", at = @At("RETURN"))
    private void onInit(int protocolVersion, String hostName, int port, ClientIntent intention, CallbackInfo ci) {
        HostOverride hostOverrideModule = Modules.get().get(HostOverride.class);
        if (hostOverrideModule != null && hostOverrideModule.isActive())
            this.hostName = hostOverrideModule.newHostname.get();
    }
}
