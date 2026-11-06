package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.ENTITY_CULLING)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && entity != mc.player) {
                double distSq = entity.squaredDistanceTo(mc.player);
                
                // Get settings
                double maxDist = ModFeatures.getSlider(ModFeatures.Feature.ENTITY_CULLING, "Max Distance");
                boolean cullAnimals = ModFeatures.getBool(ModFeatures.Feature.ENTITY_CULLING, "Cull Animals");
                boolean cullItems = ModFeatures.getBool(ModFeatures.Feature.ENTITY_CULLING, "Cull Items");

                // Cull Items at 16 blocks if option enabled
                if (cullItems && entity instanceof ItemEntity) {
                    if (distSq > 16 * 16) {
                        cir.setReturnValue(false);
                        return;
                    }
                }

                // Cull Animals at a slightly shorter distance if option enabled
                if (cullAnimals && entity instanceof AnimalEntity) {
                    if (distSq > (maxDist * 0.7) * (maxDist * 0.7)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }

                // General Entity Culling based on Max Distance slider
                if (distSq > maxDist * maxDist) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
