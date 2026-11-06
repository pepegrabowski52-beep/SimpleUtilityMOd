package com.example.mixin;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.NO_KNOCKBACK)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && packet.getId() == client.player.getId()) {
                ci.cancel(); // Blockiert Knockback vom Server für den Spieler
            }
        }
    }

    @Inject(method = "onPlayerList", at = @At("HEAD"))
    private void onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                    if (entry.profile() != null && entry.profile().getName() != null) {
                        String name = entry.profile().getName();
                        if (ModFeatures.isEnabled(ModFeatures.Feature.FRIEND_MANAGER) && ModFeatures.isFriend(name)) {
                            // Essential Mod Style Join notification
                            client.player.sendMessage(Text.literal("§8[§bSocial§8] §aYour friend §e" + name + " §ahas joined the game!"), false);
                        }
                    }
                }
            }
        }
    }
}
