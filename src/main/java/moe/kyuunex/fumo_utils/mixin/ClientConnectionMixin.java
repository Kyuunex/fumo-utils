package moe.kyuunex.fumo_utils.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.listener.ClientPacketListener;
import net.minecraft.network.listener.ServerPacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import moe.kyuunex.fumo_utils.FumoUtils;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(
        method =
            "connect(Ljava/lang/String;ILnet/minecraft/network/NetworkState;Lnet/minecraft/network/NetworkState;Lnet/minecraft/network/listener/ClientPacketListener;Lnet/minecraft/network/packet/c2s/handshake/ConnectionIntent;)V",
        at = @At("HEAD"))
    public void connect0(
        String address,
        int port,
        NetworkState<ServerPacketListener> outboundState,
        NetworkState<ClientPacketListener> inboundState,
        ClientPacketListener prePlayStateListener,
        ConnectionIntent intent,
        CallbackInfo ci) {
        FumoUtils.LOG.info("Server Ip: {}", address);
    }
}
