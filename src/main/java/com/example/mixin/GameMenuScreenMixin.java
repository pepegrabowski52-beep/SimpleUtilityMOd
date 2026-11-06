package com.example.mixin;

import com.example.gui.ClickGuiScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // Add "CS2 Client" button at the top-right corner of the pause menu
        this.addDrawableChild(ButtonWidget.builder(Text.literal("✦ CS2 Client"), button -> {
            if (this.client != null) {
                this.client.setScreen(new ClickGuiScreen());
            }
        }).dimensions(this.width - 110, 10, 100, 20).build());
    }
}
