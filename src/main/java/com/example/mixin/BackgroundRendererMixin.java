package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("RETURN"))
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.NO_FOG)) {
            // Nebel auf extreme Distanz setzen
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(viewDistance * 4.0F);
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(viewDistance * 5.0F);
        }
    }
}
