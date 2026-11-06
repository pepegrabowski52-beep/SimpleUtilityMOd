package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.NO_WEATHER)) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.NO_WEATHER)) {
            cir.setReturnValue(0.0f);
        }
    }
}
