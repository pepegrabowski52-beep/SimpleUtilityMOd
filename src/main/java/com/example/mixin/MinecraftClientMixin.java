package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow private int itemUseCooldown;
    @Shadow private int attackCooldown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.FAST_PLACE)) {
            // Holt sich den Cooldown direkt aus den Einstellungen (0 = kein Cooldown)
            int delay = (int) ModFeatures.getSlider(ModFeatures.Feature.FAST_PLACE, "Place Cooldown");
            if (this.itemUseCooldown > delay) {
                this.itemUseCooldown = delay;
            }
        }
        if (ModFeatures.isEnabled(ModFeatures.Feature.FAST_BREAK)) {
            this.attackCooldown = 0;
        }
    }
}
