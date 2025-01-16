package moe.kyuunex.fumo_utils.mixin.meteorclient;

import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
import meteordevelopment.meteorclient.systems.modules.Modules;
import moe.kyuunex.fumo_utils.modules.WireproxyIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MicrosoftAccount.class)
public class MicrosoftAccountMixin {
    @Inject(method = "login", at = @At("TAIL"), remap = false)
    public void onLogin(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get() != null && Modules.get().isActive(WireproxyIntegration.class)) {
            WireproxyIntegration module = Modules.get().get(WireproxyIntegration.class);

            module.destroyWireProxy();
            module.startWireProxy();
        }
    }
}
