package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin {

    @Inject(method = "getValue()Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void onGetValue(CallbackInfoReturnable<Object> cir) {
        SimpleOption<?> instance = (SimpleOption<?>) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            try {
                if (instance == client.options.getGamma()
                        && ModFeatures.isEnabled(ModFeatures.Feature.FULLBRIGHT)) {
                    // Override gamma to maximum brightness (1200%)
                    cir.setReturnValue(12.0);
                }
            } catch (NullPointerException ignored) {
                // Prevent bootstrap errors before options are fully initialised
            }
        }
    }
}
