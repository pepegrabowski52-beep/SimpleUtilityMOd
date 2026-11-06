package com.example.mixin;

import com.example.ModFeatures;
import com.example.gui.AuthScreen;
import com.example.gui.ClickGuiScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // 1. CS2 Client GUI Button in Main Menu
        this.addDrawableChild(ButtonWidget.builder(Text.literal("✦ CS2 Client"), button -> {
            if (this.client != null) {
                this.client.setScreen(new ClickGuiScreen());
            }
        }).dimensions(10, 10, 100, 20).build());

        // 2. CS2 Account Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("👤 Account"), button -> {
            if (this.client != null) {
                this.client.setScreen(new AuthScreen(this));
            }
        }).dimensions(115, 10, 85, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        ModFeatures.Theme theme = ModFeatures.getTheme();

        // Pulsing theme color
        double pulse = 0.85 + 0.15 * Math.sin(now / 180.0);
        int r = (int) (((theme.color >> 16) & 0xFF) * pulse);
        int g = (int) (((theme.color >>  8) & 0xFF) * pulse);
        int b = (int) ((theme.color & 0xFF) * pulse);
        int pulsedColor = 0xFF000000 | (r << 16) | (g << 8) | b;

        // Top Status Badge
        int statusX = 208;
        int statusY = 15;
        if (ModFeatures.isClientUnlocked()) {
            ctx.drawText(this.textRenderer, "✓ Unlocked: " + ModFeatures.getLoggedInUser(), statusX, statusY, 0xFF2ED573, true);
        } else if (ModFeatures.isLoggedIn()) {
            ctx.drawText(this.textRenderer, "⚠ Locked: " + ModFeatures.getLoggedInUser(), statusX, statusY, 0xFFFF4757, true);
        } else {
            ctx.drawText(this.textRenderer, "● Not logged in (Click Account)", statusX, statusY, 0xFFAAAAAA, true);
        }

        // Bottom-Right CS2 Brand Watermark
        int bwX = this.width - 150;
        int bwY = this.height - 24;
        ctx.fill(bwX - 6, bwY - 4, this.width - 4, this.height - 4, 0x880C0D16);
        ctx.fill(bwX - 6, bwY - 4, bwX - 4, this.height - 4, pulsedColor);
        ctx.drawText(this.textRenderer, "CS2 Utility Client", bwX + 2, bwY, pulsedColor, true);
        ctx.drawText(this.textRenderer, "v1.7 Fabric 1.21", bwX + 2, bwY + 10, 0xFF888899, false);
    }
}
