package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels fall-damage calculation for the local player when No Fall is active.
 *
 * Works in singleplayer (integrated server) because the mixin runs inside the same
 * JVM.  On dedicated multiplayer servers the server calculates damage server-side
 * so only the fallDistance reset in the tick event helps there.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void cancelFallDamage(float fallDistance, float damageMultiplier,
                                  DamageSource source,
                                  CallbackInfoReturnable<Boolean> cir) {

        if (!ModFeatures.isEnabled(ModFeatures.Feature.NO_FALL)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();

        // Match by UUID so we only cancel damage for the local player
        if (mc != null && mc.player != null
                && self.getUuid().equals(mc.player.getUuid())) {
            cir.setReturnValue(false);  // no fall damage
        }
    }
}
