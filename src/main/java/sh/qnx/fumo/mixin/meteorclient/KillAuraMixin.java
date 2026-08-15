package sh.qnx.fumo.mixin.meteorclient;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KillAura.class)
public abstract class KillAuraMixin {
    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void onTickHead(CallbackInfo ci) {
        if (MeteorClient.mc.player == null) {
            ci.cancel();
            return;
        }
        if (Modules.get().get(AutoGap.class).isEating()) {
            ci.cancel();
            return;
        }

        if (Modules.get().get(AutoEat.class).eating) {
            ci.cancel();
        }
    }
}
