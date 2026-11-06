package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class SafeWalkMixin {

    @Inject(method = "clipAtLedge", at = @At("RETURN"), cancellable = true)
    private void onClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.SAFE_WALK)) {
            // Verhindert das Herunterfallen an Kanten (wie beim Sneaken, aber mit normalem Tempo)
            cir.setReturnValue(true);
        }
    }
}
