package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.GLOW_ESP)) {
            Entity self = (Entity) (Object) this;
            MinecraftClient client = MinecraftClient.getInstance();
            
            // Bringt andere lebende Wesen zum Leuchten, außer den eigenen Spieler
            if (self instanceof LivingEntity && self != client.player) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.HITBOX)) {
            Entity self = (Entity) (Object) this;
            MinecraftClient client = MinecraftClient.getInstance();
            if (self instanceof LivingEntity && self != client.player) {
                float expand = (float) ModFeatures.getSlider(ModFeatures.Feature.HITBOX, "Expand Size");
                cir.setReturnValue(cir.getReturnValue() + expand);
            }
        }
    }
}
