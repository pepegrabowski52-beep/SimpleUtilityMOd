package com.example.gui;

import com.example.ModFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AuthScreen extends Screen {

    private enum Tab { LOGIN, REGISTER }

    private final Screen parent;
    private Tab activeTab = Tab.LOGIN;

    // Input fields (manual – no TextFieldWidget to avoid blur issues)
    private String userInput  = "";
    private String passInput  = "";
    private boolean userFocus = true;  // true = username focused, false = password focused

    private String statusMessage = "";
    private int    statusColor   = 0xFFFFFFFF;
    private long   statusUntil   = 0; // show for 3s then fade

    private static final File  AUTH_FILE = new File("simplemod_auth.json");
    private static final Gson  GSON      = new GsonBuilder().setPrettyPrinting().create();

    public AuthScreen(Screen parent) {
        super(Text.literal("CS2 Client - Auth"));
        this.parent = parent;
    }

    @Override
    protected void applyBlur(float delta) {}

    // =========================================================================
    // RENDER
    // =========================================================================
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModFeatures.Theme t = ModFeatures.getTheme();
        int W = this.width, H = this.height;
        int cx = W / 2, cy = H / 2;

        // Background
        ctx.fill(0, 0, W, H, 0xF00B0C12);

        // Animated background gradient lines (decorative)
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            int lineY = (int)((now / 40.0 + i * 60) % H);
            ctx.fill(0, lineY, W, lineY + 1, (t.color & 0x00FFFFFF) | 0x18000000);
        }

        // Card
        int cardW = 240, cardH = 230;
        int cardX = cx - cardW / 2, cardY = cy - cardH / 2;

        // Card shadow
        ctx.fill(cardX + 4, cardY + 4, cardX + cardW + 4, cardY + cardH + 4, 0x60000000);
        // Card border
        ctx.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, t.color);
        // Card body
        ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xFF0E0F16);

        // Top accent bar
        ctx.fill(cardX, cardY, cardX + cardW, cardY + 3, t.color);

        // Logo
        String logo = "✦ CS2 Client";
        ctx.drawText(textRenderer, logo, cx - textRenderer.getWidth(logo) / 2, cardY + 10, t.color, true);

        // Tab buttons
        int tabY    = cardY + 26;
        int tabH    = 18;
        int tabW    = (cardW - 6) / 2;
        int loginTabX = cardX + 3;
        int regTabX   = cardX + 3 + tabW + 2;

        boolean hovLogin = inBounds(mouseX, mouseY, loginTabX, tabY, tabW, tabH);
        boolean hovReg   = inBounds(mouseX, mouseY, regTabX,   tabY, tabW, tabH);

        // Login tab
        ctx.fill(loginTabX, tabY, loginTabX + tabW, tabY + tabH,
                activeTab == Tab.LOGIN ? t.color : (hovLogin ? 0xFF1D202C : 0xFF14151F));
        String loginLabel = "Login";
        ctx.drawText(textRenderer, loginLabel, loginTabX + (tabW - textRenderer.getWidth(loginLabel)) / 2,
                tabY + 5, activeTab == Tab.LOGIN ? 0xFFFFFFFF : 0xFF888888, false);

        // Register tab
        ctx.fill(regTabX, tabY, regTabX + tabW, tabY + tabH,
                activeTab == Tab.REGISTER ? t.color : (hovReg ? 0xFF1D202C : 0xFF14151F));
        String regLabel = "Register";
        ctx.drawText(textRenderer, regLabel, regTabX + (tabW - textRenderer.getWidth(regLabel)) / 2,
                tabY + 5, activeTab == Tab.REGISTER ? 0xFFFFFFFF : 0xFF888888, false);

        // Divider
        int formY = tabY + tabH + 12;

        // Username field
        ctx.drawText(textRenderer, "Username", cardX + 12, formY, 0xFF888888, false);
        formY += 11;
        int fieldW = cardW - 24;
        renderInputField(ctx, mouseX, mouseY, cardX + 12, formY, fieldW, userInput, userFocus, t);
        formY += 22;

        // Password field
        ctx.drawText(textRenderer, "Password", cardX + 12, formY, 0xFF888888, false);
        formY += 11;
        String maskedPass = "*".repeat(passInput.length());
        renderInputField(ctx, mouseX, mouseY, cardX + 12, formY, fieldW, maskedPass, !userFocus, t);
        formY += 22;

        // Note for login mode
        if (activeTab == Tab.LOGIN) {
            String hint = "Master: Pepe838 / Test";
            ctx.drawText(textRenderer, hint, cx - textRenderer.getWidth(hint) / 2, formY, 0xFF444455, false);
            formY += 14;
        } else {
            formY += 4;
        }

        // Submit button
        int submitW = fieldW, submitH = 18;
        int submitX = cardX + 12;
        boolean hovSubmit = inBounds(mouseX, mouseY, submitX, formY, submitW, submitH);
        ctx.fill(submitX, formY, submitX + submitW, formY + submitH,
                hovSubmit ? t.color : 0xFF1E2030);
        ctx.fill(submitX, formY, submitX + 2, formY + submitH, t.color);
        String submitLabel = activeTab == Tab.LOGIN ? "→ Login" : "→ Register";
        ctx.drawText(textRenderer, submitLabel,
                submitX + (submitW - textRenderer.getWidth(submitLabel)) / 2, formY + 5,
                0xFFFFFFFF, false);
        formY += 24;

        // Status message
        if (!statusMessage.isEmpty() && System.currentTimeMillis() < statusUntil) {
            ctx.drawText(textRenderer, statusMessage,
                    cx - textRenderer.getWidth(statusMessage) / 2, formY, statusColor, true);
        }

        // Logged-in state badge (top-left of card)
        String badge;
        int badgeColor;
        if (ModFeatures.isClientUnlocked()) {
            badge = "✓ UNLOCKED – " + ModFeatures.getLoggedInUser();
            badgeColor = 0xFF2ED573;
        } else if (ModFeatures.isLoggedIn()) {
            badge = "⚠ LOCKED – " + ModFeatures.getLoggedInUser();
            badgeColor = 0xFFFF4757;
        } else {
            badge = "Not logged in";
            badgeColor = 0xFF555566;
        }
        ctx.drawText(textRenderer, badge, cardX + 6, cardY - 14, badgeColor, false);

        // Back hint
        ctx.drawText(textRenderer, "ESC to go back", cx - textRenderer.getWidth("ESC to go back") / 2,
                cardY + cardH + 6, 0xFF333344, false);
    }

    private void renderInputField(DrawContext ctx, int mouseX, int mouseY,
                                  int x, int y, int w, String text, boolean focused,
                                  ModFeatures.Theme t) {
        boolean hov = inBounds(mouseX, mouseY, x, y, w, 16);
        ctx.fill(x, y, x + w, y + 16, focused ? 0xFF1A1C28 : (hov ? 0xFF14161F : 0xFF0F1018));
        ctx.fill(x, y + 15, x + w, y + 16, focused ? t.color : 0xFF2F3342); // underline
        String display = text + (focused ? "▌" : "");
        // Trim if too long to fit
        String shown = textRenderer.getWidth(display) > w - 8
                ? "…" + textRenderer.trimToWidth(display, w - 14, true)
                : display;
        ctx.drawText(textRenderer, shown.isEmpty() && !focused ? "" : shown, x + 4, y + 4, 0xFFFFFFFF, false);
    }

    private boolean inBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // =========================================================================
    // INPUT
    // =========================================================================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        int W = this.width, H = this.height;
        int cx = W / 2, cy = H / 2;
        int cardW = 240, cardH = 230;
        int cardX = cx - cardW / 2, cardY = cy - cardH / 2;

        // Tab buttons
        int tabY = cardY + 26, tabH = 18, tabW = (cardW - 6) / 2;
        if (inBounds(mx, my, cardX + 3, tabY, tabW, tabH)) {
            activeTab = Tab.LOGIN;
            clearStatus();
            return true;
        }
        if (inBounds(mx, my, cardX + 3 + tabW + 2, tabY, tabW, tabH)) {
            activeTab = Tab.REGISTER;
            clearStatus();
            return true;
        }

        // Field focus
        int formY = tabY + tabH + 12 + 11; // after "Username" label
        int fieldW = cardW - 24;
        if (inBounds(mx, my, cardX + 12, formY, fieldW, 16)) { userFocus = true;  return true; }
        formY += 22 + 11; // after username field + "Password" label
        if (inBounds(mx, my, cardX + 12, formY, fieldW, 16)) { userFocus = false; return true; }
        formY += 22 + (activeTab == Tab.LOGIN ? 14 : 4); // skip hint

        // Submit button
        if (inBounds(mx, my, cardX + 12, formY, fieldW, 18)) {
            if (activeTab == Tab.LOGIN)   handleLogin();
            else                          handleRegister();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            userFocus = !userFocus;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (activeTab == Tab.LOGIN) handleLogin();
            else                        handleRegister();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.client != null) this.client.setScreen(parent);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (userFocus && !userInput.isEmpty())
                userInput = userInput.substring(0, userInput.length() - 1);
            else if (!userFocus && !passInput.isEmpty())
                passInput = passInput.substring(0, passInput.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32) { // printable characters only
            if (userFocus && userInput.length() < 32)  userInput += chr;
            else if (!userFocus && passInput.length() < 32) passInput += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    // =========================================================================
    // AUTH LOGIC
    // =========================================================================
    private void handleLogin() {
        String user = userInput.trim();
        String pass = passInput.trim();

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Please fill in all fields!", 0xFFFF4757);
            return;
        }

        Map<String, String> accounts = loadAccounts();
        if (accounts.containsKey(user) && accounts.get(user).equals(pass)) {
            ModFeatures.setLoggedInUser(user);
            ModFeatures.setLoggedInPassword(pass);
            if (ModFeatures.isClientUnlocked()) {
                setStatus("✓ Client UNLOCKED! Welcome, " + user, 0xFF2ED573);
            } else {
                setStatus("Logged in as " + user + " (client locked)", 0xFFFFAA00);
            }
        } else {
            setStatus("Invalid username or password!", 0xFFFF4757);
        }
    }

    private void handleRegister() {
        String user = userInput.trim();
        String pass = passInput.trim();

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Please fill in all fields!", 0xFFFF4757);
            return;
        }
        if (user.length() < 3) {
            setStatus("Username must be at least 3 characters!", 0xFFFF4757);
            return;
        }
        if (pass.length() < 4) {
            setStatus("Password must be at least 4 characters!", 0xFFFF4757);
            return;
        }

        Map<String, String> accounts = loadAccounts();
        if (accounts.containsKey(user)) {
            setStatus("Username already taken!", 0xFFFF4757);
            return;
        }

        accounts.put(user, pass);
        saveAccounts(accounts);
        setStatus("✓ Registered! Switch to Login tab.", 0xFF2ED573);
        activeTab = Tab.LOGIN; // auto-switch to login after register
    }

    private void setStatus(String msg, int color) {
        statusMessage = msg;
        statusColor   = color;
        statusUntil   = System.currentTimeMillis() + 4000;
    }

    private void clearStatus() {
        statusMessage = "";
        statusUntil   = 0;
    }

    // =========================================================================
    // ACCOUNT FILE I/O
    // =========================================================================
    private Map<String, String> loadAccounts() {
        // Always ensure Pepe838/Test exists as master account
        Map<String, String> defaults = new HashMap<>();
        defaults.put("Pepe838", "Test");

        if (!AUTH_FILE.exists()) {
            saveAccounts(defaults);
            return defaults;
        }
        try (FileReader reader = new FileReader(AUTH_FILE)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> data = GSON.fromJson(reader, type);
            if (data == null) data = new HashMap<>();
            // Always keep master account
            if (!data.containsKey("Pepe838")) {
                data.put("Pepe838", "Test");
                saveAccounts(data);
            }
            return data;
        } catch (Exception e) {
            return defaults;
        }
    }

    private void saveAccounts(Map<String, String> accounts) {
        try (FileWriter writer = new FileWriter(AUTH_FILE)) {
            GSON.toJson(accounts, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
