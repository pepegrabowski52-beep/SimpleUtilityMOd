package com.example.mixin;

import com.example.ModFeatures;
import com.example.SimpleModClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (ModFeatures.isEnabled(ModFeatures.Feature.AUTO_GG)) {
            String text = message.getString().toLowerCase();
            if (text.contains("winner") || text.contains("won the game") || text.contains("game over") || 
                text.contains("spiel beendet") || text.contains("gewonnen") || text.contains("sieg") || 
                text.contains("1st place") || text.contains("1. platz")) {
                
                double delaySec = ModFeatures.getSlider(ModFeatures.Feature.AUTO_GG, "Chat Delay");
                SimpleModClient.triggerAutoGg((int) (delaySec * 20)); // In Ticks
            }
        }
    }
}
