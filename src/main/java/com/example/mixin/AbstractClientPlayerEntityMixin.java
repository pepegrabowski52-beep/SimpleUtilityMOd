package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    private static final Identifier CAPE_PURPLE = Identifier.of("simplemod", "textures/capes/purple_wave.png");
    private static final Identifier CAPE_CYAN   = Identifier.of("simplemod", "textures/capes/cyan_neon.png");
    private static final Identifier CAPE_RED    = Identifier.of("simplemod", "textures/capes/ruby_dragon.png");
    private static final Identifier CAPE_GOLD   = Identifier.of("simplemod", "textures/capes/gold_crown.png");

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (!ModFeatures.isEnabled(ModFeatures.Feature.CUSTOM_CAPE)) return;

        SkinTextures original = cir.getReturnValue();
        if (original == null) return;

        int design = (int) ModFeatures.getSlider(ModFeatures.Feature.CUSTOM_CAPE, "Cape Design");
        Identifier selectedCape = switch (design) {
            case 2 -> CAPE_CYAN;
            case 3 -> CAPE_RED;
            case 4 -> CAPE_GOLD;
            default -> CAPE_PURPLE;
        };

        SkinTextures modified = new SkinTextures(
            original.texture(),
            original.textureUrl(),
            selectedCape,
            selectedCape, // Also use for elytra if desired
            original.model(),
            original.secure()
        );

        cir.setReturnValue(modified);
    }
}
