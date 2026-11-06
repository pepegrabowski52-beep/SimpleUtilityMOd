package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void onTickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        // ---- 1. FreeCam Movement Block ----
        if (ModFeatures.isEnabled(ModFeatures.Feature.FREE_CAM)) {
            player.setVelocity(0, 0, 0);
            if (player.input != null) {
                player.input.movementForward = 0f;
                player.input.movementSideways = 0f;
                player.input.jumping = false;
                player.input.sneaking = false;
            }
            ci.cancel();
            return;
        }

        // ---- 2. Jesus (Water Walk) ----
        if (ModFeatures.isEnabled(ModFeatures.Feature.JESUS)) {
            if ((player.isTouchingWater() || player.isInLava()) && (player.input != null && !player.input.sneaking)) {
                Vec3d vel = player.getVelocity();
                if (player.isSubmergedInWater()) {
                    // Submerged -> Aufsteigen zur Oberfläche
                    player.setVelocity(vel.x, 0.15, vel.z);
                } else {
                    // Auf der Oberfläche -> stabil stehen
                    player.setVelocity(vel.x, 0.0, vel.z);
                    player.setOnGround(true);
                }
            }
        }

        // ---- 3. Spider (Wall Climb) ----
        if (ModFeatures.isEnabled(ModFeatures.Feature.SPIDER) && player.horizontalCollision) {
            Vec3d vel = player.getVelocity();
            double speed = ModFeatures.getSlider(ModFeatures.Feature.SPIDER, "Climb Speed");
            player.setVelocity(vel.x, speed, vel.z);
        }
    }

    // ---- 4. No Slowdown (Verlangsamung beim Essen/Bogen spannen blockieren) ----
    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean redirectIsUsingItem(ClientPlayerEntity player) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.NO_SLOWDOWN)) {
            return false; // Tut so, als würden wir kein Item benutzen, um die Verlangsamung zu umgehen
        }
        return player.isUsingItem();
    }
}
