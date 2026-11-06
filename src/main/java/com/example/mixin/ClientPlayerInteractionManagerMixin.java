package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Shadow
    private int blockBreakingCooldown;

    @Shadow
    private float currentBreakingProgress;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.FAST_BREAK)) {
            // blockBreakingCooldown auf 0 setzen erlaubt es, Blöcke ohne Verzögerung nacheinander anzuwählen
            this.blockBreakingCooldown = 0;
            
            // Wenn der Spieler bereits einen Block abbaut (Fortschritt > 0.0)
            // Erhöhen wir den Abbau-Fortschritt pro Tick zusätzlich, um ihn viel schneller zu brechen
            if (this.currentBreakingProgress > 0.0f) {
                this.currentBreakingProgress += 0.18f; // Bricht Blöcke ca. 4-5x so schnell!
            }
        }
    }
}
