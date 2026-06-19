package moe.kyuunex.fumo_utils.mixin.meteorclient;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AutoTool.class)
public abstract class AutoToolMixin {

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void onTickHead(CallbackInfo ci) {
        if (Modules.get().get(AutoGap.class).isEating()) {
            ci.cancel();
            return;
        }

        if (Modules.get().get(AutoEat.class).eating) {
            ci.cancel();
        }
    }

    @Inject(method = "onStartBreakingBlock", at = @At("HEAD"), cancellable = true)
    private void onStartBreakingBlockHead(CallbackInfo ci) {
        if (Modules.get().get(AutoGap.class).isEating()) {
            ci.cancel();
            return;
        }

        if (Modules.get().get(AutoEat.class).eating) {
            ci.cancel();
        }
    }
}

