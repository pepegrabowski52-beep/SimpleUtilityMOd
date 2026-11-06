package com.example.mixin;

import com.example.ModFeatures;
import com.example.SimpleModClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides the camera's world position after the vanilla update so that
 * when FreeCam is active the camera floats freely while the player stays put.
 */
@Mixin(Camera.class)
public abstract class FreeCamMixin {

    /**
     * Shadow of Camera#setPos so we can call it from within the mixin.
     * Camera.setPos is protected final – @Shadow lets us invoke it without overriding.
     */
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("RETURN"))
    private void onCameraUpdate(BlockView area, Entity focusedEntity,
                                boolean thirdPerson, boolean inverseView,
                                float tickDelta, CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.FREE_CAM)) {
            Vec3d pos = SimpleModClient.getFreeCamPos();
            this.setPos(pos.x, pos.y, pos.z);
        }
    }
}
