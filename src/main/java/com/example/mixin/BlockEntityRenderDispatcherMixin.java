package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void onRender(E blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.FAST_RENDER)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && blockEntity.getPos() != null) {
                double distSq = blockEntity.getPos().getSquaredDistance(mc.player.getPos());
                
                // Get settings
                double maxDist = ModFeatures.getSlider(ModFeatures.Feature.FAST_RENDER, "Max Distance");
                boolean cullSigns = ModFeatures.getBool(ModFeatures.Feature.FAST_RENDER, "Cull Signs");
                boolean cullChests = ModFeatures.getBool(ModFeatures.Feature.FAST_RENDER, "Cull Chests");

                // Aggressively cull signs at 12 blocks distance
                if (cullSigns && blockEntity instanceof SignBlockEntity) {
                    if (distSq > 12 * 12) {
                        ci.cancel();
                        return;
                    }
                }

                // Cull chests using a slightly adjusted distance
                if (cullChests && blockEntity instanceof ChestBlockEntity) {
                    if (distSq > (maxDist * 0.8) * (maxDist * 0.8)) {
                        ci.cancel();
                        return;
                    }
                }

                // General block entity culling based on Max Distance
                if (distSq > maxDist * maxDist) {
                    ci.cancel();
                }
            }
        }
    }
}
