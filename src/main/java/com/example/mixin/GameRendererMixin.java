package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    private static double smoothFov = 70.0;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                          CallbackInfoReturnable<Double> cir) {

        double baseFov   = cir.getReturnValue();

        double zoomFov   = ModFeatures.isEnabled(ModFeatures.Feature.ZOOM)
            ? ModFeatures.getSlider(ModFeatures.Feature.ZOOM, "FOV Level")
            : baseFov;
        double lerpSpeed = ModFeatures.getSlider(ModFeatures.Feature.ZOOM, "Lerp Speed");

        smoothFov += (zoomFov - smoothFov) * lerpSpeed;

        if (ModFeatures.isEnabled(ModFeatures.Feature.ZOOM)) {
            cir.setReturnValue(smoothFov);
        } else if (Math.abs(smoothFov - baseFov) > 0.1) {
            cir.setReturnValue(smoothFov);
        } else {
            smoothFov = baseFov;
        }
    }

    // ── Hide Hand ─────────────────────────────────────────────
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void onRenderHand(Camera camera, float tickDelta, org.joml.Matrix4f matrix4f, CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.HIDE_HAND)) {
            ci.cancel();
        }
    }
}
