package com.example.gui;

import com.example.ModFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {

    public enum GuiMode { DROPDOWN, GRID }
    private static GuiMode currentMode = GuiMode.DROPDOWN;

    // --- COLORS ---
    private static final int COLOR_OVERLAY       = 0x80000000;
    private static final int COLOR_BORDER        = 0xFF2F3342;
    private static final int COLOR_HOVER         = 0xFF282B38;
    private static final int COLOR_TEXT_DISABLED = 0xFFCCCCCC;

    // --- DROPDOWN MODE ---
    private static final int CARD_W   = 140;
    private static final int HEADER_H = 22;

    private static class CardPos {
        int x, y;
        CardPos(int x, int y) { this.x = x; this.y = y; }
    }
    private static final Map<ModFeatures.Category, CardPos> dropdownPositions = new HashMap<>();

    private ModFeatures.Category draggingCategory = null;
    private int dragOffsetX = 0, dragOffsetY = 0;

    private ModFeatures.Feature expandedFeature        = null;
    private ModFeatures.Feature selectedSettingsFeature = null;
    private ModFeatures.Feature bindingFeature          = null;

    private ModFeatures.SliderSetting draggingSlider = null;
    private int draggingSliderX = 0, draggingSliderW = 0;

    // --- SEARCH & GRID ---
    private String searchQuery   = "";
    private boolean searchActive = false;
    private ModFeatures.Category activeCategory = ModFeatures.Category.COMBAT;

    private boolean themeMenuOpen = false;

    // --- NAME CHANGER ---
    private String nameChangerInput = "";
    private boolean nameChangerActive = false;
    /** Set true when name was just changed (brief feedback flash) */
    private long nameChangerFeedbackUntil = 0;

    // --- FRIEND MANAGER ---
    private String friendInput = "";
    private boolean friendInputActive = false;

    // --- HOSTING ---
    // Cached public IP (fetched async from api.ipify.org)
    private volatile String publicIp      = null;
    private volatile boolean fetchingIp   = false;
    private volatile boolean hostActive   = false;
    private String hostStatusMsg          = "";
    private long   hostStatusUntil        = 0;

    // --- ANIMATIONS ---
    /** 0.0 → 1.0 fade-in when GUI opens */
    private float  fadeAlpha       = 0f;
    /** Timestamp when this screen was opened (for animations) */
    private long   openTime        = 0;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    public ClickGuiScreen() {
        super(Text.literal("Utility Client"));
    }

    @Override
    protected void applyBlur(float delta) {}

    @Override
    protected void init() {
        openTime  = System.currentTimeMillis();
        fadeAlpha = 0f;

        int padding = 10;
        int startX  = 15;
        int startY  = 40;
        int cols    = Math.max(1, (this.width - 20) / 148);
        int col = 0, row = 0;

        for (ModFeatures.Category cat : ModFeatures.Category.values()) {
            CardPos pos = dropdownPositions.get(cat);
            if (pos == null) {
                pos = new CardPos(startX + (col * 148), startY + (row * 180));
                dropdownPositions.put(cat, pos);
            } else {
                pos.x = Math.max(padding, Math.min(this.width  - CARD_W   - padding, pos.x));
                pos.y = Math.max(30,      Math.min(this.height - HEADER_H - padding, pos.y));
            }
            col++;
            if (col >= cols) { col = 0; row++; }
        }

        // Pre-fill name changer with current Minecraft session name
        if (this.client != null && this.client.getSession() != null) {
            nameChangerInput = this.client.getSession().getUsername();
        }
    }

    // =========================================================================
    // MAIN RENDER
    // =========================================================================
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Animate fade-in (reaches 1.0 in ~200ms)
        long elapsed = System.currentTimeMillis() - openTime;
        fadeAlpha = Math.min(1f, elapsed / 180f);

        // 1. Animated dim background (fades in)
        int overlayAlpha = (int)(0x80 * fadeAlpha);
        ctx.fill(0, 0, this.width, this.height, (overlayAlpha << 24));

        // 2. Content (also slides in slightly from center)
        if (currentMode == GuiMode.DROPDOWN) {
            renderDropdownGui(ctx, mouseX, mouseY);
        } else {
            renderGridGui(ctx, mouseX, mouseY);
        }

        // 3. Top Bar (always on top)
        renderTopBar(ctx, mouseX, mouseY);

        // 4. Theme Picker
        if (themeMenuOpen) {
            renderThemePicker(ctx, mouseX, mouseY);
        }

        // 5. Lock overlay (below settings modal, above content)
        if (!ModFeatures.isClientUnlocked()) {
            renderLockOverlay(ctx, mouseX, mouseY);
        }

        // 6. Settings Modal (topmost)
        if (selectedSettingsFeature != null) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, 0, 150);
            renderSettingsModal(ctx, mouseX, mouseY);
            ctx.getMatrices().pop();
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // =========================================================================
    // LOCK OVERLAY
    // =========================================================================
    private void renderLockOverlay(DrawContext ctx, int mouseX, int mouseY) {
        ModFeatures.Theme t = ModFeatures.getTheme();
        int topBarH = 26;

        // Semi-transparent red over everything below top bar
        ctx.fill(0, topBarH, this.width, this.height, 0xC0180808);

        // Center lock icon + message
        int cx = this.width / 2;
        int cy = this.height / 2;

        String line1 = "🔒  CLIENT LOCKED";
        String line2 = "Login with  Pepe838 / Test  to unlock.";
        String line3 = "Click the  👤 Login  button in the top bar.";

        ctx.drawText(textRenderer, line1, cx - textRenderer.getWidth(line1) / 2, cy - 18, 0xFFFF4757, true);
        ctx.drawText(textRenderer, line2, cx - textRenderer.getWidth(line2) / 2, cy - 4,  0xFFFFAA00, false);
        ctx.drawText(textRenderer, line3, cx - textRenderer.getWidth(line3) / 2, cy + 8,  0xFFAAAAAA, false);

        // Button: open auth screen
        int btnW = 130, btnH = 18;
        int btnX = cx - btnW / 2;
        int btnY = cy + 24;
        boolean hov = inBounds(mouseX, mouseY, btnX, btnY, btnW, btnH);
        ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0xFF8B2626 : 0xFF3A1010);
        ctx.fill(btnX, btnY, btnX + 2, btnY + btnH, 0xFFFF4757);
        String btnLabel = "Open Login Screen";
        ctx.drawText(textRenderer, btnLabel, btnX + (btnW - textRenderer.getWidth(btnLabel)) / 2, btnY + 5, 0xFFFFFFFF, false);
    }

    // =========================================================================
    // TOP BAR  – Responsive (compact < 500px)
    // =========================================================================
    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY) {
        ModFeatures.Theme t = ModFeatures.getTheme();
        int W = this.width;
        boolean compact = W < 500;

        // Background
        ctx.fill(0, 0, W, 26, 0xEE0D0E14);

        // Pulsing accent stripe
        long time = System.currentTimeMillis();
        double pulse = 0.85 + 0.15 * Math.sin(time / 200.0);
        int r = (int) (((t.color >> 16) & 0xFF) * pulse);
        int g = (int) (((t.color >>  8) & 0xFF) * pulse);
        int b = (int) ((t.color & 0xFF) * pulse);
        ctx.fill(0, 25, W, 26, 0xFF000000 | (r << 16) | (g << 8) | b);

        // Left colored border
        ctx.fill(0, 0, 2, 26, t.color);

        // Logo
        int logoEndX = 10;
        if (W >= 350) {
            String logo = compact ? "CS2" : "✦ CS2 Client";
            ctx.drawText(textRenderer, logo, 8, 9, t.color, true);
            logoEndX = textRenderer.getWidth(logo) + 14;
        }

        // Build buttons right → left
        int rightX = W;
        int btnH = 20, btnY = 3;

        // Close
        int closeBtnW = compact ? 18 : 20;
        rightX -= (closeBtnW + 3);
        int closeX = rightX;
        boolean hovClose = inBounds(mouseX, mouseY, closeX, btnY, closeBtnW, btnH);
        ctx.fill(closeX, btnY, closeX + closeBtnW, btnY + btnH, hovClose ? 0xFFD93838 : 0xFF1E1F2A);
        ctx.drawText(textRenderer, "✕", closeX + (closeBtnW / 2) - 2, 8, 0xFFFFFFFF, false);

        // Theme
        int themeBtnW = compact ? 18 : 22;
        rightX -= (themeBtnW + 3);
        int themeBtnX = rightX;
        boolean hovTheme = inBounds(mouseX, mouseY, themeBtnX, btnY, themeBtnW, btnH);
        ctx.fill(themeBtnX, btnY, themeBtnX + themeBtnW, btnY + btnH,
                themeMenuOpen ? t.color : (hovTheme ? COLOR_HOVER : 0xFF1E1F2A));
        ctx.drawText(textRenderer, "🎨", themeBtnX + (themeBtnW / 2) - 3, 7, 0xFFFFFFFF, false);

        // Account
        String accText = ModFeatures.isLoggedIn() ? "👤 " + ModFeatures.getLoggedInUser() : "👤 Login";
        int accBtnW = compact ? 18 : textRenderer.getWidth(accText) + 12;
        rightX -= (accBtnW + 3);
        int accBtnX = rightX;
        boolean hovAcc = inBounds(mouseX, mouseY, accBtnX, btnY, accBtnW, btnH);
        ctx.fill(accBtnX, btnY, accBtnX + accBtnW, btnY + btnH, hovAcc ? COLOR_HOVER : 0xFF1E1F2A);
        if (compact) {
            ctx.drawText(textRenderer, "👤", accBtnX + 5, 8, ModFeatures.isLoggedIn() ? 0xFF2ED573 : 0xFFAAAAAA, false);
        } else {
            ctx.drawText(textRenderer, accText, accBtnX + 6, 8, ModFeatures.isLoggedIn() ? 0xFF2ED573 : 0xFFAAAAAA, false);
        }

        // Hacks toggle
        boolean hacksHidden = ModFeatures.isHideHacks();
        int hackBtnW = compact ? 20 : (hacksHidden ? 78 : 74);
        rightX -= (hackBtnW + 4);
        int hackBtnX = rightX;
        boolean hovHack = inBounds(mouseX, mouseY, hackBtnX, btnY, hackBtnW, btnH);
        ctx.fill(hackBtnX, btnY, hackBtnX + hackBtnW, btnY + btnH,
                hacksHidden ? 0xFF1A5C38 : (hovHack ? 0xFF6B1E1E : 0xFF3A1414));
        ctx.fill(hackBtnX, btnY, hackBtnX + 2, btnY + btnH, hacksHidden ? 0xFF2ED573 : 0xFFFF4757);
        if (compact) {
            ctx.drawText(textRenderer, hacksHidden ? "✓" : "⚠", hackBtnX + 5, 8, 0xFFFFFFFF, false);
        } else {
            ctx.drawText(textRenderer, hacksHidden ? "✓ Hacks OFF" : "⚠ Hacks ON", hackBtnX + 6, 8, 0xFFFFFFFF, false);
        }

        // Mode switch buttons (only if enough space)
        if (W >= 420) {
            int modeBtnW = compact ? 32 : 72;
            rightX -= (modeBtnW + 3);
            int btnGridX = rightX;
            boolean hovGrid = inBounds(mouseX, mouseY, btnGridX, btnY, modeBtnW, btnH);
            ctx.fill(btnGridX, btnY, btnGridX + modeBtnW, btnY + btnH,
                    currentMode == GuiMode.GRID ? t.color : (hovGrid ? COLOR_HOVER : 0xFF1E1F2A));
            ctx.drawText(textRenderer, compact ? "⊞" : "⊞ Grid", btnGridX + 5, 8,
                    currentMode == GuiMode.GRID ? 0xFFFFFFFF : 0xFF888888, false);

            rightX -= (modeBtnW + 3);
            int btnDropX = rightX;
            boolean hovDrop = inBounds(mouseX, mouseY, btnDropX, btnY, modeBtnW, btnH);
            ctx.fill(btnDropX, btnY, btnDropX + modeBtnW, btnY + btnH,
                    currentMode == GuiMode.DROPDOWN ? t.color : (hovDrop ? COLOR_HOVER : 0xFF1E1F2A));
            ctx.drawText(textRenderer, compact ? "☰" : "☰ List", btnDropX + 5, 8,
                    currentMode == GuiMode.DROPDOWN ? 0xFFFFFFFF : 0xFF888888, false);
        }

        // Name changer widget (between logo and mode buttons)
        // Shows when the client is unlocked
        int ncW = 0;
        if (ModFeatures.isClientUnlocked() && W >= 480) {
            ncW = Math.min(100, rightX - logoEndX - 70);
            if (ncW >= 60) {
                int ncX = rightX - ncW - 4;
                rightX = ncX - 2;
                boolean ncHov = inBounds(mouseX, mouseY, ncX, btnY, ncW, btnH);
                ctx.fill(ncX, btnY, ncX + ncW, btnY + btnH, nameChangerActive ? 0xFF252836 : (ncHov ? 0xFF1D202C : 0xFF14151F));
                ctx.fill(ncX, btnY + btnH - 1, ncX + ncW, btnY + btnH, nameChangerActive ? t.color : COLOR_BORDER);
                long now = System.currentTimeMillis();
                String ncDisplay;
                int ncColor;
                if (now < nameChangerFeedbackUntil) {
                    ncDisplay = "✓ Changed!";
                    ncColor = 0xFF2ED573;
                } else {
                    ncDisplay = nameChangerInput.isEmpty() ? (nameChangerActive ? "▌" : "Nick...") : nameChangerInput + (nameChangerActive ? "▌" : "");
                    ncColor = nameChangerInput.isEmpty() ? 0xFF555566 : 0xFFFFFFFF;
                }
                ctx.drawText(textRenderer, ncDisplay, ncX + 4, 8, ncColor, false);
            }
        }

        // Search field (fills remaining space)
        int searchX = logoEndX + 4;
        int searchMaxRight = rightX - 5;
        int searchW = searchMaxRight - searchX;
        if (searchW >= 40) {
            boolean hovSearch = inBounds(mouseX, mouseY, searchX, btnY, searchW, btnH);
            ctx.fill(searchX, btnY, searchX + searchW, btnY + btnH,
                    searchActive ? 0xFF252836 : (hovSearch ? 0xFF1D202C : 0xFF14151F));
            ctx.fill(searchX, btnY + btnH - 1, searchX + searchW, btnY + btnH,
                    searchActive ? t.color : COLOR_BORDER);
            String displayText = searchQuery.isEmpty()
                    ? (searchActive ? "▌" : "🔍 Search...")
                    : searchQuery + (searchActive ? "▌" : "");
            ctx.drawText(textRenderer, displayText, searchX + 4, 8,
                    searchQuery.isEmpty() ? 0xFF555566 : 0xFFFFFFFF, false);
        }
    }

    /** Helper: check if mouse is inside rectangle */
    private boolean inBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // =========================================================================
    // THEME PICKER OVERLAY
    // =========================================================================
    private void renderThemePicker(DrawContext ctx, int mouseX, int mouseY) {
        int popupW = 120;
        int popupH = ModFeatures.Theme.values().length * 22 + 8;
        int px = this.width - 140;
        int py = 30;

        ctx.fill(px - 1, py - 1, px + popupW + 1, py + popupH + 1, COLOR_BORDER);
        ctx.fill(px, py, px + popupW, py + popupH, 0xFF12131A);

        int ty = py + 5;
        for (ModFeatures.Theme t : ModFeatures.Theme.values()) {
            boolean isCurrent = ModFeatures.getTheme() == t;
            boolean hover = inBounds(mouseX, mouseY, px + 5, ty, popupW - 10, 18);
            ctx.fill(px + 5, ty, px + popupW - 5, ty + 18, isCurrent ? t.color : (hover ? COLOR_HOVER : 0x00000000));
            ctx.drawText(textRenderer, t.name, px + 10, ty + 5, isCurrent ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            ty += 22;
        }
    }

    // =========================================================================
    // STYLE 1: DROPDOWN GUI
    // =========================================================================
    private void renderDropdownGui(DrawContext ctx, int mouseX, int mouseY) {
        ModFeatures.Theme activeTheme = ModFeatures.getTheme();

        for (ModFeatures.Category cat : ModFeatures.Category.values()) {
            CardPos pos = dropdownPositions.get(cat);
            if (pos == null) continue;

            List<ModFeatures.Feature> feats = filterFeatures(featsFor(cat));

            int contentH = HEADER_H + 2;
            for (ModFeatures.Feature f : feats) {
                contentH += 15;
                if (expandedFeature == f) contentH += getSettingsHeight(f);
            }

            ctx.fill(pos.x - 1, pos.y - 1, pos.x + CARD_W + 1, pos.y + contentH + 1, COLOR_BORDER);
            ctx.fill(pos.x, pos.y, pos.x + CARD_W, pos.y + contentH, activeTheme.cardBg);
            ctx.fill(pos.x, pos.y, pos.x + CARD_W, pos.y + HEADER_H, activeTheme.headerBg);
            ctx.fill(pos.x, pos.y + HEADER_H - 1, pos.x + CARD_W, pos.y + HEADER_H, activeTheme.color);

            ctx.drawText(textRenderer, getCategoryIcon(cat) + " " + cat.displayName, pos.x + 6, pos.y + 7, 0xFFFFFFFF, true);

            int fy = pos.y + HEADER_H + 2;
            for (ModFeatures.Feature f : feats) {
                boolean on  = ModFeatures.isEnabled(f);
                boolean hov = inBounds(mouseX, mouseY, pos.x, fy, CARD_W, 14);

                if (hov) ctx.fill(pos.x + 1, fy, pos.x + CARD_W - 1, fy + 14, COLOR_HOVER);

                ctx.drawText(textRenderer, f.displayName, pos.x + 6, fy + 3,
                        on ? activeTheme.color : COLOR_TEXT_DISABLED, false);

                if (!ModFeatures.getSettings(f).isEmpty()) {
                    ctx.drawText(textRenderer, "⚙", pos.x + CARD_W - 10, fy + 3,
                            expandedFeature == f ? activeTheme.color : 0xFF555555, false);
                }

                fy += 15;
                if (expandedFeature == f) {
                    fy = renderInlineSettings(ctx, f, pos.x + 4, fy, CARD_W - 8, mouseX, mouseY);
                }
            }
        }
    }

    // =========================================================================
    // STYLE 2: GRID DASHBOARD  (GUI_SCALE applied)
    // =========================================================================
    private void renderGridGui(DrawContext ctx, int mouseX, int mouseY) {
        ModFeatures.Theme activeTheme = ModFeatures.getTheme();

        // GUI scale from slider
        double scale = ModFeatures.getSlider(ModFeatures.Feature.GUI_SCALE, "Scale");
        if (scale <= 0.01) scale = 1.0; // Safety fallback

        // Un-scaled window dimensions, then apply scale around center
        int baseWinW = Math.min(520, this.width  - 40);
        int baseWinH = Math.min(300, this.height - 60);
        int centerX  = this.width  / 2;
        int centerY  = this.height / 2 + 10;

        // Scaled dimensions
        int winW = (int)(baseWinW * scale);
        int winH = (int)(baseWinH * scale);
        int wx   = centerX - winW / 2;
        int wy   = centerY - winH / 2;

        // Window Frame
        ctx.fill(wx - 1, wy - 1, wx + winW + 1, wy + winH + 1, COLOR_BORDER);
        ctx.fill(wx, wy, wx + winW, wy + winH, 0xFF12131A);

        // Sidebar
        int sidebarW = (int)(110 * scale);
        ctx.fill(wx, wy, wx + sidebarW, wy + winH, 0xFF161822);
        ctx.fill(wx + sidebarW - 1, wy, wx + sidebarW, wy + winH, COLOR_BORDER);

        int catItemH = (int)(23 * scale);
        int catPad   = (int)(4 * scale);
        ModFeatures.Category[] cats = ModFeatures.Category.values();
        int catY = wy + catPad + 4;
        int catTextOff = (int)(6 * scale);
        for (ModFeatures.Category cat : cats) {
            boolean active = activeCategory == cat;
            boolean hov = inBounds(mouseX, mouseY, wx + catPad, catY, sidebarW - catPad * 2, (int)(20 * scale));
            ctx.fill(wx + catPad, catY, wx + sidebarW - catPad, catY + (int)(20 * scale),
                    active ? activeTheme.color : (hov ? COLOR_HOVER : 0x00000000));
            ctx.drawText(textRenderer, getCategoryIcon(cat) + " " + cat.displayName,
                    wx + catTextOff + catPad, catY + (int)(6 * scale),
                    active ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            catY += catItemH;
        }

        // Content Area
        int contentX = wx + sidebarW + (int)(10 * scale);
        int contentY = wy + (int)(10 * scale);
        int contentW = winW - sidebarW - (int)(20 * scale);

        List<ModFeatures.Feature> feats = filterFeatures(featsFor(activeCategory));

        // Special Social view
        if (activeCategory == ModFeatures.Category.SOCIAL) {
            renderSocialPanel(ctx, mouseX, mouseY, contentX, contentY, contentW, winH - (int)(20 * scale), activeTheme);
            return;
        }

        int gapX = (int)(6 * scale);
        int gapY = (int)(6 * scale);
        int cols  = Math.max(1, contentW / Math.max(1, (int)(70 * scale) + gapX));
        int cardW = Math.max(40, (contentW - (gapX * (cols - 1))) / cols);
        // Card height from slider — default 36px, minimum 26px so text fits
        int cardH = (int)(ModFeatures.getSlider(ModFeatures.Feature.GUI_SCALE, "Card Size") * scale);
        if (cardH < 26) cardH = 26;

        int col = 0, row = 0;
        for (ModFeatures.Feature f : feats) {
            int cx = contentX + (col * (cardW + gapX));
            int cy = contentY + (row * (cardH + gapY));

            if (cy + cardH > wy + winH - 4) break;

            boolean on  = ModFeatures.isEnabled(f);
            boolean hov = inBounds(mouseX, mouseY, cx, cy, cardW, cardH);

            // Card background
            ctx.fill(cx, cy, cx + cardW, cy + cardH,
                    on ? activeTheme.headerBg : (hov ? COLOR_HOVER : 0xFF1A1C26));
            // Top accent line (2px)
            ctx.fill(cx, cy, cx + cardW, cy + 2, on ? activeTheme.color : COLOR_BORDER);

            // Feature name (always at top)
            String nameText = f.displayName;
            int maxNameW = cardW - 18;
            if (textRenderer.getWidth(nameText) > maxNameW)
                nameText = textRenderer.trimToWidth(nameText, maxNameW - 4) + "…";
            ctx.drawText(textRenderer, nameText, cx + 5, cy + 5,
                    on ? activeTheme.color : 0xFFFFFFFF, true);

            // Only show ON/OFF badge + gear if card is tall enough
            if (cardH >= 32) {
                int badgeY = cy + cardH - 13;
                // ON/OFF badge
                ctx.fill(cx + 4, badgeY, cx + 28, badgeY + 10,
                        on ? activeTheme.color : 0xFF2C2F3E);
                ctx.drawText(textRenderer, on ? "ON" : "OFF",
                        cx + (on ? 7 : 5), badgeY + 2, 0xFFFFFFFF, false);
                // Gear icon (right side)
                boolean hovCog = inBounds(mouseX, mouseY, cx + cardW - 16, badgeY - 2, 14, 14);
                ctx.drawText(textRenderer, "⚙", cx + cardW - 14, badgeY,
                        hovCog ? 0xFFFFFFFF : 0xFF555566, false);
            }

            col++;
            if (col >= cols) { col = 0; row++; }
        }
    }

    // =========================================================================
    // SOCIAL PANEL (inside Grid GUI content area)
    // =========================================================================
    private void renderSocialPanel(DrawContext ctx, int mouseX, int mouseY,
                                   int sx, int sy, int sw, int sh,
                                   ModFeatures.Theme t) {
        int y = sy;

        // ── Section: HOST SERVER ──────────────────────────────────────────────
        ctx.fill(sx, y, sx + sw, y + 22, t.headerBg);
        ctx.fill(sx, y + 21, sx + sw, y + 22, t.color);
        ctx.drawText(textRenderer, "🌐 Host World (für Freunde)", sx + 5, y + 7, 0xFFFFFFFF, true);
        y += 28;

        // Port row
        double port = ModFeatures.getSlider(ModFeatures.Feature.HOST_LAN, "LAN Port");
        ctx.drawText(textRenderer, "Port:", sx + 5, y + 1, 0xFF888888, false);
        ctx.drawText(textRenderer, String.valueOf((int) port), sx + 30, y + 1, 0xFFFFFFFF, false);

        // Port slider
        y += 13;
        int slW = sw - 10;
        ctx.fill(sx + 5, y, sx + 5 + slW, y + 4, 0xFF08090C);
        for (ModFeatures.Setting s : ModFeatures.getSettings(ModFeatures.Feature.HOST_LAN)) {
            if (s instanceof ModFeatures.SliderSetting sl && sl.name.equals("LAN Port")) {
                double pct = (sl.value - sl.min) / (sl.max - sl.min);
                ctx.fill(sx + 5, y, sx + 5 + (int)(slW * pct), y + 4, t.color);
                break;
            }
        }
        y += 10;

        // ▶ Host button
        int hostBtnW = sw - 10;
        boolean hovHost = inBounds(mouseX, mouseY, sx + 5, y, hostBtnW, 16);
        int hostBtnColor = hostActive ? 0xFF1A5C38 : (hovHost ? t.color : 0xFF16301E);
        ctx.fill(sx + 5, y, sx + 5 + hostBtnW, y + 16, hostBtnColor);
        ctx.fill(sx + 5, y, sx + 7, y + 16, t.color);
        String hostLabel = hostActive ? "✓ Welt wird gehostet!" : "▶  Welt hosten (Klicken)";
        ctx.drawText(textRenderer, hostLabel, sx + 12, y + 4, 0xFFFFFFFF, false);
        y += 20;

        // Host status message
        if (!hostStatusMsg.isEmpty() && System.currentTimeMillis() < hostStatusUntil) {
            ctx.drawText(textRenderer, hostStatusMsg, sx + 5, y, 0xFF2ED573, false);
            y += 12;
        }

        // ── Public IP Box ─────────────────────────────────────────────────────
        ctx.fill(sx + 5, y, sx + sw - 5, y + 12, 0xFF0E0F16);
        ctx.fill(sx + 5, y, sx + 7, y + 12, t.color);
        if (fetchingIp) {
            ctx.drawText(textRenderer, "  Lade öffentliche IP…", sx + 5, y + 2, 0xFF888888, false);
        } else if (publicIp != null) {
            String ipLine = "  " + publicIp + ":" + (int)port + "  ← für Freunde";
            ctx.drawText(textRenderer, ipLine, sx + 5, y + 2, 0xFFFFFFFF, false);
        } else {
            boolean hovIp = inBounds(mouseX, mouseY, sx + 5, y, sw - 10, 12);
            ctx.drawText(textRenderer, "  🔍 IP abrufen (klicken)", sx + 5, y + 2,
                    hovIp ? 0xFFFFFFFF : 0xFF888888, false);
        }
        y += 16;

        // Copy IP button (nur wenn IP bekannt)
        if (publicIp != null) {
            boolean hovCopy = inBounds(mouseX, mouseY, sx + 5, y, 80, 13);
            ctx.fill(sx + 5, y, sx + 85, y + 13, hovCopy ? t.color : 0xFF1D202C);
            ctx.drawText(textRenderer, "📋 IP kopieren", sx + 8, y + 3, 0xFFFFFFFF, false);
        }
        y += 18;

        // ── Port-Forwarding Guide ─────────────────────────────────────────────
        ctx.fill(sx, y, sx + sw, y + 20, t.headerBg);
        ctx.fill(sx, y + 19, sx + sw, y + 20, t.color);
        ctx.drawText(textRenderer, "📋 So können Freunde joinen:", sx + 5, y + 6, 0xFFFFFFFF, true);
        y += 24;

        String[] steps = {
            "1. Oben auf ▶ Welt hosten klicken",
            "2. IP oben kopieren und an Freund schicken",
            "3. Freund → Mehrspieler → Direkt verbinden",
            "4. IP:Port eingeben und verbinden!",
            "",
            "⚠ Kein eigenes Netz? Port " + (int)port + " im Router",
            "   freigeben (Port Forwarding)"
        };
        for (String step : steps) {
            if (step.isEmpty()) { y += 4; continue; }
            int color = step.startsWith("⚠") ? 0xFFFFAA00 : 0xFFAAAAAA;
            ctx.drawText(textRenderer, step, sx + 5, y, color, false);
            y += 10;
        }
        y += 6;

        // ── Section: FRIENDS ──────────────────────────────────────────────────
        if (y + 22 > sy + sh) return; // kein Platz mehr
        ctx.fill(sx, y, sx + sw, y + 22, t.headerBg);
        ctx.fill(sx, y + 21, sx + sw, y + 22, t.color);
        ctx.drawText(textRenderer, "👥 Friend Manager", sx + 5, y + 7, 0xFFFFFFFF, true);
        y += 28;

        // Add friend input
        if (y + 14 > sy + sh) return;
        ctx.fill(sx + 5, y, sx + sw - 40, y + 14, friendInputActive ? 0xFF252836 : 0xFF14151F);
        ctx.fill(sx + 5, y + 13, sx + sw - 40, y + 14, friendInputActive ? t.color : COLOR_BORDER);
        String fiDisplay = friendInput.isEmpty()
                ? (friendInputActive ? "▌" : "Spielername…")
                : friendInput + (friendInputActive ? "▌" : "");
        ctx.drawText(textRenderer, fiDisplay, sx + 8, y + 3,
                friendInput.isEmpty() ? 0xFF555566 : 0xFFFFFFFF, false);

        // Add button
        int addBtnX = sx + sw - 36;
        boolean hovAdd = inBounds(mouseX, mouseY, addBtnX, y, 32, 14);
        ctx.fill(addBtnX, y, addBtnX + 32, y + 14, hovAdd ? t.color : 0xFF1A5C38);
        ctx.drawText(textRenderer, "+ Add", addBtnX + 3, y + 3, 0xFFFFFFFF, false);
        y += 20;

        // Friend list
        List<String> friends = ModFeatures.getFriends();
        if (friends.isEmpty()) {
            ctx.drawText(textRenderer, "Noch keine Freunde hinzugefügt.", sx + 5, y, 0xFF555566, false);
        } else {
            for (int i = 0; i < friends.size(); i++) {
                if (y + 12 > sy + sh) break;
                String name = friends.get(i);
                boolean hovFriend = inBounds(mouseX, mouseY, sx + 5, y, sw - 35, 12);
                ctx.fill(sx + 5, y - 1, sx + sw - 10, y + 11,
                        hovFriend ? COLOR_HOVER : 0x00000000);
                ctx.fill(sx + 5, y, sx + 7, y + 10, t.color); // color dot
                ctx.drawText(textRenderer, "  " + name, sx + 8, y + 1, 0xFFFFFFFF, false);

                int rmX = sx + sw - 24;
                boolean hovRm = inBounds(mouseX, mouseY, rmX, y, 18, 10);
                ctx.drawText(textRenderer, "✕", rmX + 2, y + 1,
                        hovRm ? 0xFFFF4757 : 0xFF555555, false);
                y += 14;
            }
        }
    }


    // =========================================================================
    // SETTINGS MODAL
    // =========================================================================
    private void renderSettingsModal(DrawContext ctx, int mouseX, int mouseY) {
        ModFeatures.Theme activeTheme = ModFeatures.getTheme();
        ctx.fill(0, 0, this.width, this.height, 0xD0000000);

        ModFeatures.Feature f = selectedSettingsFeature;
        int modalW = 210;
        int modalH = 45 + getSettingsHeight(f);
        int mx = (this.width - modalW) / 2;
        int my = (this.height - modalH) / 2;

        ctx.fill(mx - 2, my - 2, mx + modalW + 2, my + modalH + 2, COLOR_BORDER);
        ctx.fill(mx, my, mx + modalW, my + modalH, 0xFF12131A);
        ctx.fill(mx, my, mx + modalW, my + 24, activeTheme.headerBg);
        ctx.fill(mx, my + 23, mx + modalW, my + 24, activeTheme.color);

        String headerTitle = "⚙ " + f.displayName;
        if (textRenderer.getWidth(headerTitle) > modalW - 30) {
            headerTitle = textRenderer.trimToWidth(headerTitle, modalW - 35) + "…";
        }
        ctx.drawText(textRenderer, headerTitle, mx + 8, my + 8, 0xFFFFFFFF, true);

        boolean hovClose = inBounds(mouseX, mouseY, mx + modalW - 20, my + 4, 16, 16);
        ctx.drawText(textRenderer, "✕", mx + modalW - 16, my + 7, hovClose ? 0xFFD93838 : 0xFFAAAAAA, false);

        renderInlineSettings(ctx, f, mx + 10, my + 32, modalW - 20, mouseX, mouseY);
    }

    // =========================================================================
    // SETTINGS CONTENT RENDERER
    // =========================================================================
    private int renderInlineSettings(DrawContext ctx, ModFeatures.Feature f, int sx, int sy, int sw, int mouseX, int mouseY) {
        ModFeatures.Theme activeTheme = ModFeatures.getTheme();

        boolean isBinding = bindingFeature == f;
        String kbText = isBinding ? "> Press Key <" : "Keybind: " + ModFeatures.getKeyName(f);
        boolean hoverKb = inBounds(mouseX, mouseY, sx, sy, sw, 16);

        ctx.fill(sx, sy, sx + sw, sy + 16, isBinding ? 0xFF8B2626 : (hoverKb ? COLOR_HOVER : 0xFF1B1C26));
        ctx.drawText(textRenderer, kbText, sx + 6, sy + 4, 0xFFFFFFFF, false);
        sy += 22;

        for (ModFeatures.Setting s : ModFeatures.getSettings(f)) {
            ctx.drawText(textRenderer, s.name, sx + 2, sy, 0xFFAAAAAA, false);
            sy += 12;

            if (s instanceof ModFeatures.SliderSetting slider) {
                ctx.fill(sx, sy, sx + sw, sy + 5, 0xFF08090C);
                double pct = (slider.value - slider.min) / (slider.max - slider.min);
                ctx.fill(sx, sy, sx + (int)(sw * pct), sy + 5, activeTheme.color);
                String valStr = String.format("%.1f%s", slider.value, slider.suffix);
                ctx.drawText(textRenderer, valStr, sx + sw - textRenderer.getWidth(valStr), sy - 12, 0xFFFFFFFF, false);
                sy += 16;
            } else if (s instanceof ModFeatures.BooleanSetting bool) {
                int boxSize = 9;
                ctx.fill(sx, sy, sx + boxSize, sy + boxSize, 0xFF08090C);
                if (bool.value) ctx.fill(sx + 1, sy + 1, sx + boxSize - 1, sy + boxSize - 1, activeTheme.color);
                ctx.drawText(textRenderer, bool.value ? "ENABLED" : "DISABLED", sx + boxSize + 6, sy + 1,
                        bool.value ? activeTheme.color : 0xFF666666, false);
                sy += 16;
            }
        }
        return sy;
    }

    // =========================================================================
    // INPUT HANDLING
    // =========================================================================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;

        // ── 0. Cancel keybinding on any click ───────────────────────────────
        if (bindingFeature != null) {
            bindingFeature = null;
            return true;
        }

        // ── 1. Settings Modal ────────────────────────────────────────────────
        if (selectedSettingsFeature != null) {
            ModFeatures.Feature f = selectedSettingsFeature;
            int modalW = 210;
            int modalH = 45 + getSettingsHeight(f);
            int pmx    = (this.width  - modalW) / 2;
            int pmy    = (this.height - modalH) / 2;

            // Close button
            if (inBounds(mx, my, pmx + modalW - 20, pmy + 4, 16, 16)) {
                selectedSettingsFeature = null;
                return true;
            }

            int sx = pmx + 10, sy = pmy + 32, sw = modalW - 20;

            // Keybind button
            if (inBounds(mx, my, sx, sy, sw, 16)) {
                bindingFeature = f;
                return true;
            }
            sy += 22;

            for (ModFeatures.Setting s : ModFeatures.getSettings(f)) {
                sy += 12;
                if (s instanceof ModFeatures.SliderSetting slider) {
                    if (inBounds(mx, my, sx, sy - 2, sw, 9)) {
                        draggingSlider  = slider;
                        draggingSliderX = sx;
                        draggingSliderW = sw;
                        updateSliderFromMouse(mouseX);
                        return true;
                    }
                    sy += 16;
                } else if (s instanceof ModFeatures.BooleanSetting bool) {
                    if (inBounds(mx, my, sx, sy, sw, 12)) {
                        bool.value = !bool.value;
                        return true;
                    }
                    sy += 16;
                }
            }

            // Click outside → close
            if (!inBounds(mx, my, pmx, pmy, modalW, modalH)) {
                selectedSettingsFeature = null;
            }
            return true;
        }

        // ── 2. Top Bar clicks  (responsive, mirrors renderTopBar) ────────────
        if (my >= 3 && my <= 23) {
            int W = this.width;
            boolean compact = W < 500;
            int btnH = 20, btnY = 3;
            int rightX = W;

            // Close
            int closeBtnW = compact ? 18 : 20;
            rightX -= (closeBtnW + 3);
            if (inBounds(mx, my, rightX, btnY, closeBtnW, btnH)) {
                this.close(); return true;
            }

            // Theme
            int themeBtnW = compact ? 18 : 22;
            rightX -= (themeBtnW + 3);
            if (inBounds(mx, my, rightX, btnY, themeBtnW, btnH)) {
                themeMenuOpen = !themeMenuOpen; return true;
            }

            // Account
            String accText = ModFeatures.isLoggedIn() ? "👤 " + ModFeatures.getLoggedInUser() : "👤 Login";
            int accBtnW = compact ? 18 : textRenderer.getWidth(accText) + 12;
            rightX -= (accBtnW + 3);
            if (inBounds(mx, my, rightX, btnY, accBtnW, btnH)) {
                if (this.client != null) this.client.setScreen(new AuthScreen(this));
                return true;
            }

            // Hacks
            boolean hacksHidden = ModFeatures.isHideHacks();
            int hackBtnW = compact ? 20 : (hacksHidden ? 78 : 74);
            rightX -= (hackBtnW + 4);
            if (inBounds(mx, my, rightX, btnY, hackBtnW, btnH)) {
                ModFeatures.toggleHideHacks(); return true;
            }

            // Mode buttons
            if (W >= 420) {
                int modeBtnW = compact ? 32 : 72;
                rightX -= (modeBtnW + 3);
                if (inBounds(mx, my, rightX, btnY, modeBtnW, btnH)) {
                    currentMode = GuiMode.GRID; return true;
                }
                rightX -= (modeBtnW + 3);
                if (inBounds(mx, my, rightX, btnY, modeBtnW, btnH)) {
                    currentMode = GuiMode.DROPDOWN; return true;
                }
            }

            // Name changer widget
            if (ModFeatures.isClientUnlocked() && W >= 480) {
                int ncW = Math.min(100, rightX - (textRenderer.getWidth(W >= 350 ? (compact ? "CS2" : "✦ CS2 Client") : "") + 14) - 70);
                if (ncW >= 60) {
                    int ncX = rightX - ncW - 4;
                    if (inBounds(mx, my, ncX, btnY, ncW, btnH)) {
                        nameChangerActive = true;
                        searchActive      = false;
                        return true;
                    }
                }
            }

            // Search field
            int logoEndX = W >= 350 ? textRenderer.getWidth(compact ? "CS2" : "✦ CS2 Client") + 14 : 10;
            int searchX  = logoEndX + 4;
            int searchW  = (rightX - 5) - searchX;
            if (searchW >= 40 && inBounds(mx, my, searchX, btnY, searchW, btnH)) {
                searchActive      = true;
                nameChangerActive = false;
                return true;
            }

            searchActive      = false;
            nameChangerActive = false;
        }

        // ── 3. Theme Picker ──────────────────────────────────────────────────
        if (themeMenuOpen) {
            int popupW = 120;
            int px = this.width - 140, py = 30, ty = py + 5;
            for (ModFeatures.Theme t : ModFeatures.Theme.values()) {
                if (inBounds(mx, my, px + 5, ty, popupW - 10, 18)) {
                    ModFeatures.setTheme(t);
                    themeMenuOpen = false;
                    return true;
                }
                ty += 22;
            }
            themeMenuOpen = false;
            return true;
        }

        // ── 4. Lock Overlay intercepts all non-top-bar clicks ────────────────
        if (!ModFeatures.isClientUnlocked()) {
            // But let the "Open Login Screen" button work
            int cx = this.width / 2, cy = this.height / 2;
            int btnW = 130, btnH = 18, btnX = cx - btnW / 2, btnY = cy + 24;
            if (inBounds(mx, my, btnX, btnY, btnW, btnH)) {
                if (this.client != null) this.client.setScreen(new AuthScreen(this));
                return true;
            }
            return true; // Block everything else
        }

        // ── 5. Grid / Dropdown content ───────────────────────────────────────────────
        if (currentMode == GuiMode.DROPDOWN) {
            return handleDropdownClick(mouseX, mouseY, button);
        } else {
            return handleGridClick(mouseX, mouseY, button);
        }
    }

    // ── Social panel click handler ───────────────────────────────────────────
    private void handleSocialClick(int mx, int my, int button) {
        double scale = ModFeatures.getSlider(ModFeatures.Feature.GUI_SCALE, "Scale");
        if (scale <= 0.01) scale = 1.0;

        int baseWinW = Math.min(520, this.width  - 40);
        int baseWinH = Math.min(300, this.height - 60);
        int winW = (int)(baseWinW * scale), winH = (int)(baseWinH * scale);
        int wx = this.width / 2 - winW / 2, wy = this.height / 2 + 10 - winH / 2;
        int sidebarW = (int)(110 * scale);

        int sx = wx + sidebarW + (int)(10 * scale);
        int sy = wy + (int)(10 * scale);
        int sw = winW - sidebarW - (int)(20 * scale);

        // Mirror renderSocialPanel layout exactly
        int y = sy + 28; // skip header

        // Port slider (y+13, then +10)
        int slW = sw - 10;
        int sliderY = y + 13;
        for (ModFeatures.Setting s : ModFeatures.getSettings(ModFeatures.Feature.HOST_LAN)) {
            if (s instanceof ModFeatures.SliderSetting sl && sl.name.equals("LAN Port")) {
                if (inBounds(mx, my, sx + 5, sliderY - 2, slW, 9)) {
                    draggingSlider  = sl;
                    draggingSliderX = sx + 5;
                    draggingSliderW = slW;
                    updateSliderFromMouse(mx);
                }
                break;
            }
        }
        y = sliderY + 10; // now pointing at host button

        // ▶ Host button
        int hostBtnW = sw - 10;
        if (inBounds(mx, my, sx + 5, y, hostBtnW, 16)) {
            String result = com.example.SimpleModClient.hostLan();
            hostActive   = !result.startsWith("Must be") && !result.startsWith("Not in");
            hostStatusMsg   = result;
            hostStatusUntil = System.currentTimeMillis() + 4000;
            return;
        }
        y += 20;

        // Skip hostStatusMsg line (variable height — handled by render only)
        if (!hostStatusMsg.isEmpty() && System.currentTimeMillis() < hostStatusUntil) y += 12;

        // IP box click — fetch IP
        double port = ModFeatures.getSlider(ModFeatures.Feature.HOST_LAN, "LAN Port");
        if (inBounds(mx, my, sx + 5, y, sw - 10, 12) && !fetchingIp && publicIp == null) {
            fetchPublicIp();
            return;
        }
        y += 16;

        // Copy IP button
        if (publicIp != null && inBounds(mx, my, sx + 5, y, 80, 13)) {
            if (this.client != null) {
                this.client.keyboard.setClipboard(publicIp + ":" + (int)port);
            }
            return;
        }
        y += 18;

        // Skip port-forwarding guide (24 header + steps)
        y += 24 + 10 * 7 + 6; // approximate guide height

        // Skip friend manager header (22 + 6 margin)
        y += 28;

        // Friend add input field
        int fiW = sw - 45;
        if (inBounds(mx, my, sx + 5, y, fiW, 14)) {
            friendInputActive = true;
            return;
        }
        // Add friend button
        int addBtnX = sx + sw - 36;
        if (inBounds(mx, my, addBtnX, y, 32, 14)) {
            if (!friendInput.trim().isEmpty()) {
                ModFeatures.addFriend(friendInput.trim());
                friendInput = "";
            }
            return;
        }
        friendInputActive = false;
        y += 20;

        // Remove friend buttons
        List<String> friends = ModFeatures.getFriends();
        for (int i = 0; i < friends.size(); i++) {
            int rmX = sx + sw - 24;
            if (inBounds(mx, my, rmX, y, 18, 10)) {
                ModFeatures.removeFriend(friends.get(i));
                return;
            }
            y += 14;
        }
    }

    /** Fetches public IP asynchronously from api.ipify.org */
    private void fetchPublicIp() {
        if (fetchingIp) return;
        fetchingIp = true;
        publicIp   = null;
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://api.ipify.org");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    String ip = br.readLine();
                    if (ip != null && !ip.isBlank()) publicIp = ip.trim();
                    else                              publicIp = "Fehler";
                }
            } catch (Exception e) {
                publicIp = "Keine Verbindung";
            } finally {
                fetchingIp = false;
            }
        }, "CS2-IPFetch").start();
    }


    // ── Dropdown click handler ───────────────────────────────────────────────
    private boolean handleDropdownClick(double mouseX, double mouseY, int button) {
        for (ModFeatures.Category cat : ModFeatures.Category.values()) {
            CardPos pos = dropdownPositions.get(cat);
            if (pos == null) continue;

            // Drag by header
            if (button == 0 && mouseX >= pos.x && mouseX <= pos.x + CARD_W
                    && mouseY >= pos.y && mouseY <= pos.y + HEADER_H) {
                draggingCategory = cat;
                dragOffsetX = (int) mouseX - pos.x;
                dragOffsetY = (int) mouseY - pos.y;
                return true;
            }

            List<ModFeatures.Feature> feats = filterFeatures(featsFor(cat));
            int fy = pos.y + HEADER_H + 2;

            for (ModFeatures.Feature f : feats) {
                if (mouseX >= pos.x && mouseX <= pos.x + CARD_W && mouseY >= fy && mouseY <= fy + 14) {
                    if (button == 0) {
                        if (f == ModFeatures.Feature.TETRIS) {
                            if (this.client != null) this.client.setScreen(new TetrisScreen(this));
                        } else {
                            ModFeatures.toggle(f);
                        }
                    }
                    else if (button == 1) expandedFeature = (expandedFeature == f) ? null : f;
                    return true;
                }
                fy += 15;

                if (expandedFeature == f) {
                    int sx = pos.x + 4, sw = CARD_W - 8;
                    if (mouseX >= sx && mouseX <= sx + sw && mouseY >= fy && mouseY <= fy + 16) {
                        bindingFeature = f;
                        return true;
                    }
                    fy += 22;

                    for (ModFeatures.Setting s : ModFeatures.getSettings(f)) {
                        fy += 12;
                        if (s instanceof ModFeatures.SliderSetting slider) {
                            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= fy - 2 && mouseY <= fy + 7) {
                                draggingSlider  = slider;
                                draggingSliderX = sx;
                                draggingSliderW = sw;
                                updateSliderFromMouse(mouseX);
                                return true;
                            }
                            fy += 16;
                        } else if (s instanceof ModFeatures.BooleanSetting bool) {
                            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= fy && mouseY <= fy + 12) {
                                bool.value = !bool.value;
                                return true;
                            }
                            fy += 16;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Grid click handler ───────────────────────────────────────────────────
    private boolean handleGridClick(double mouseX, double mouseY, int button) {
        double scale = ModFeatures.getSlider(ModFeatures.Feature.GUI_SCALE, "Scale");
        if (scale <= 0.01) scale = 1.0;

        int baseWinW = Math.min(520, this.width  - 40);
        int baseWinH = Math.min(300, this.height - 60);
        int winW = (int)(baseWinW * scale), winH = (int)(baseWinH * scale);
        int wx   = this.width  / 2 - winW / 2;
        int wy   = this.height / 2 + 10 - winH / 2;
        int sidebarW = (int)(110 * scale);

        // ── SIDEBAR always checked first ───────────────────────────────────
        int catPad   = (int)(4 * scale);
        int catItemH = (int)(23 * scale);
        int catY = wy + catPad + 4;
        for (ModFeatures.Category cat : ModFeatures.Category.values()) {
            if (inBounds((int)mouseX, (int)mouseY, wx + catPad, catY,
                         sidebarW - catPad * 2, (int)(20 * scale))) {
                activeCategory    = cat;
                friendInputActive = false;
                return true;
            }
            catY += catItemH;
        }

        // ── Social content (non-sidebar) ─────────────────────────────────
        if (activeCategory == ModFeatures.Category.SOCIAL) {
            handleSocialClick((int)mouseX, (int)mouseY, button);
            return true;
        }

        // ── Feature cards ─────────────────────────────────────────────────
        int contentX = wx + sidebarW + (int)(10 * scale);
        int contentY = wy + (int)(10 * scale);
        int contentW = winW - sidebarW - (int)(20 * scale);

        List<ModFeatures.Feature> feats = filterFeatures(featsFor(activeCategory));

        int cardH = (int)(ModFeatures.getSlider(ModFeatures.Feature.GUI_SCALE, "Card Size") * scale);
        if (cardH < 26) cardH = 26;
        int gapX  = (int)(6 * scale), gapY = (int)(6 * scale);
        int cols  = Math.max(1, contentW / Math.max(1, (int)(70 * scale) + gapX));
        int cardW = Math.max(40, (contentW - (gapX * (cols - 1))) / cols);

        int col = 0, row = 0;
        for (ModFeatures.Feature f : feats) {
            int cx = contentX + (col * (cardW + gapX));
            int cy = contentY + (row * (cardH + gapY));

            if (cy + cardH > wy + winH - 4) break;

            if (inBounds((int)mouseX, (int)mouseY, cx, cy, cardW, cardH)) {
                // Gear icon click area (right edge of card)
                boolean clickCog = inBounds((int)mouseX, (int)mouseY,
                        cx + cardW - (int)(18 * scale), cy + (int)(cardH / 2),
                        (int)(16 * scale), (int)(cardH / 2));
                if (button == 0) {
                    if (clickCog) {
                        selectedSettingsFeature = f;
                    } else if (f == ModFeatures.Feature.TETRIS) {
                        if (this.client != null) this.client.setScreen(new TetrisScreen(this));
                    } else {
                        ModFeatures.toggle(f);
                    }
                } else if (button == 1) {
                    if (f == ModFeatures.Feature.TETRIS) {
                        if (this.client != null) this.client.setScreen(new TetrisScreen(this));
                    } else {
                        selectedSettingsFeature = f;
                    }
                }
                return true;
            }

            col++;
            if (col >= cols) { col = 0; row++; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider   = null;
        draggingCategory = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSlider != null) {
            updateSliderFromMouse(mouseX);
            return true;
        }
        if (draggingCategory != null) {
            CardPos pos = dropdownPositions.get(draggingCategory);
            if (pos != null) {
                int padding = 10;
                pos.x = Math.max(padding, Math.min(this.width  - CARD_W   - padding, (int) mouseX - dragOffsetX));
                pos.y = Math.max(30,      Math.min(this.height - HEADER_H - padding, (int) mouseY - dragOffsetY));
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateSliderFromMouse(double mouseX) {
        if (draggingSlider == null) return;
        double pct = (mouseX - draggingSliderX) / (double) draggingSliderW;
        pct = Math.max(0.0, Math.min(1.0, pct));
        draggingSlider.value = draggingSlider.min + pct * (draggingSlider.max - draggingSlider.min);
    }

    // =========================================================================
    // KEY / CHAR INPUT
    // =========================================================================
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        // ── Keybind capture ──────────────────────────────────────────────────
        if (bindingFeature != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE)   bindingFeature = null;
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ModFeatures.setKeycode(bindingFeature, 0);
                bindingFeature = null;
            } else {
                ModFeatures.setKeycode(bindingFeature, keyCode);
                bindingFeature = null;
            }
            return true;
        }

        // ── Name changer ─────────────────────────────────────────────────────
        if (nameChangerActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                nameChangerActive = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                applyNameChange();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !nameChangerInput.isEmpty()) {
                nameChangerInput = nameChangerInput.substring(0, nameChangerInput.length() - 1);
                return true;
            }
            return true; // consume all keys while name changer is active
        }

        // ── Friend input ─────────────────────────────────────────────────────
        if (friendInputActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                friendInputActive = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!friendInput.trim().isEmpty()) {
                    ModFeatures.addFriend(friendInput.trim());
                    friendInput = "";
                }
                friendInputActive = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) {
                friendInput = friendInput.substring(0, friendInput.length() - 1);
                return true;
            }
            return true;
        }

        // ── Search field ─────────────────────────────────────────────────────
        if (searchActive) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchActive = false;
                return true;
            }
            return true; // consume keys so E/Z don't close the GUI
        }

        // ── Escape / Close ───────────────────────────────────────────────────
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectedSettingsFeature != null) { selectedSettingsFeature = null; return true; }
            this.close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_Y || keyCode == GLFW.GLFW_KEY_Z) {
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (nameChangerActive) {
            if (chr >= 32 && nameChangerInput.length() < 24) nameChangerInput += chr;
            return true;
        }
        if (friendInputActive) {
            if (chr >= 32 && friendInput.length() < 24) friendInput += chr;
            return true;
        }
        if (searchActive) {
            searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void applyNameChange() {
        String newName = nameChangerInput.trim();
        if (newName.isEmpty() || this.client == null) return;
        // Minecraft Session update (clientside display only)
        try {
            net.minecraft.client.session.Session oldSession = this.client.getSession();
            net.minecraft.client.session.Session newSession = new net.minecraft.client.session.Session(
                newName, oldSession.getUuidOrNull(), oldSession.getAccessToken(),
                oldSession.getXuid(), oldSession.getClientId(), oldSession.getAccountType()
            );
            ((com.example.mixin.MinecraftClientAccessor)(Object)this.client).setSession(newSession);
            nameChangerFeedbackUntil = System.currentTimeMillis() + 2000;
        } catch (Exception e) {
            // ignore if accessor not available
        }
        nameChangerActive = false;
    }

    @Override
    public boolean shouldPause() { return false; }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private List<ModFeatures.Feature> featsFor(ModFeatures.Category cat) {
        List<ModFeatures.Feature> list = new ArrayList<>();
        for (ModFeatures.Feature f : ModFeatures.Feature.values())
            if (f.category == cat) list.add(f);
        return list;
    }

    private List<ModFeatures.Feature> filterFeatures(List<ModFeatures.Feature> list) {
        List<ModFeatures.Feature> filtered = new ArrayList<>();
        for (ModFeatures.Feature f : list) {
            if (ModFeatures.isHideHacks() && f.isHack()) continue;
            if (!searchQuery.trim().isEmpty() &&
                !f.displayName.toLowerCase().contains(searchQuery.toLowerCase())) continue;
            filtered.add(f);
        }
        return filtered;
    }

    private String getCategoryIcon(ModFeatures.Category cat) {
        return switch (cat) {
            case COMBAT      -> "⚔";
            case MOVEMENT    -> "🏃";
            case VISUAL      -> "👁";
            case WORLD       -> "🌐";
            case PERFORMANCE -> "⚡";
            case MINIGAME    -> "🎮";
            case SOCIAL      -> "👥";
        };
    }

    private int getSettingsHeight(ModFeatures.Feature f) {
        int h = 22;
        for (ModFeatures.Setting s : ModFeatures.getSettings(f)) h += 28;
        return h;
    }
}