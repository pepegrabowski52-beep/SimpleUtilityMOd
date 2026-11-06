package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fast Leaves: Blendet benachbarte Laubblatt-Flächen aus, die ohnehin verdeckt sind.
 * Dies spart viele Draw Calls im Wald und erhöht die FPS spürbar.
 *
 * Wir targeten AbstractBlock.isSideInvisible statt LeavesBlock direkt,
 * da LeavesBlock die Methode von AbstractBlock erbt und dort injiziert werden kann.
 */
@Mixin(AbstractBlock.class)
public class LeavesBlockMixin {

    @Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
    private void onIsSideInvisible(BlockState state, BlockState neighbor, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.FAST_LEAVES)) {
            // Wenn beide Blöcke Laub-Typ sind, Seite ausblenden
            if (state.getBlock() instanceof net.minecraft.block.LeavesBlock
                    && neighbor.getBlock() instanceof net.minecraft.block.LeavesBlock) {
                cir.setReturnValue(true);
            }
        }
    }
}
