package com.example;

import org.lwjgl.glfw.GLFW;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModFeatures {

    // ── Global Themes (Synchronisiert mit ClickGuiScreen) ────
    public enum Theme {
        PURPLE("Purple Accent", 0xFFA573FF, 0xFF1E212B, 0xF0151720),
        CYAN("Neon Cyan",     0xFF00E5FF, 0xFF16252B, 0xF0121A20),
        RED("Ruby Red",       0xFFFF4757, 0xFF2B1A1E, 0xF0201316),
        GREEN("Emerald",      0xFF2ED573, 0xFF1A2B21, 0xF0132017);

        public final String name;
        public final int color; // Accent
        public final int headerBg;
        public final int cardBg;

        Theme(String name, int color, int headerBg, int cardBg) {
            this.name = name;
            this.color = color;
            this.headerBg = headerBg;
            this.cardBg = cardBg;
        }
    }

    private static Theme currentTheme = Theme.PURPLE;

    public static Theme getTheme() { return currentTheme; }
    public static void setTheme(Theme theme) { currentTheme = theme; }
    public static void cycleTheme() {
        int next = (currentTheme.ordinal() + 1) % Theme.values().length;
        currentTheme = Theme.values()[next];
    }

    // ── Categories ───────────────────────────────────────────
    public enum Category {
        COMBAT(     "Combat"),
        MOVEMENT(   "Movement"),
        VISUAL(     "Visual"),
        WORLD(      "World"),
        PERFORMANCE("FPS/Perf"),
        MINIGAME(   "Minigame"),
        SOCIAL(     "Social");

        public final String displayName;
        public final boolean isHack;

        Category(String displayName) {
            this.displayName = displayName;
            // FPS/Perf, Social und Minigame gelten standardmäßig nicht als klassische "Hacks"
            this.isHack = !displayName.equals("FPS/Perf") && !displayName.equals("Minigame") && !displayName.equals("Social");
        }
    }

    // ── Features ─────────────────────────────────────────────
    public enum FeatureTag { HACK, UTILITY, PERFORMANCE, MINIGAME }

    public enum Feature {
        // COMBAT
        KILL_AURA(  "KillAura",    Category.COMBAT,   FeatureTag.HACK),
        TRIGGER_BOT("TriggerBot",  Category.COMBAT,   FeatureTag.HACK),
        CLICK_ASSIST("Click Assist", Category.COMBAT, FeatureTag.HACK),
        AIM_ASSIST( "Aim Assist",  Category.COMBAT,   FeatureTag.HACK),
        AUTO_CRIT(  "Auto Crit",   Category.COMBAT,   FeatureTag.HACK),
        HITBOX(     "Hitbox Expand", Category.COMBAT, FeatureTag.HACK),
        REACH(      "Reach",       Category.COMBAT,   FeatureTag.HACK),

        // MOVEMENT
        AUTO_SPRINT("Auto Sprint", Category.MOVEMENT, FeatureTag.HACK),
        FREE_CAM(   "FreeCam",     Category.MOVEMENT, FeatureTag.HACK),
        NO_FALL(    "No Fall",     Category.MOVEMENT, FeatureTag.HACK),
        FLIGHT(     "Flight",      Category.MOVEMENT, FeatureTag.HACK),
        SAFE_WALK(  "Safe Walk",   Category.MOVEMENT, FeatureTag.HACK),
        JESUS(      "Jesus",       Category.MOVEMENT, FeatureTag.HACK),
        SPIDER(     "Spider",      Category.MOVEMENT, FeatureTag.HACK),
        STEP(       "Step",        Category.MOVEMENT, FeatureTag.HACK),
        HIGH_JUMP(  "High Jump",   Category.MOVEMENT, FeatureTag.HACK),
        NO_SLOWDOWN("No Slowdown", Category.MOVEMENT, FeatureTag.HACK),
        SPEED(      "Speedhack",   Category.MOVEMENT, FeatureTag.HACK),
        ANTI_FIRE(  "Anti Fire",   Category.MOVEMENT, FeatureTag.HACK),
        SCAFFOLD(   "Scaffold",    Category.MOVEMENT, FeatureTag.HACK),

        // VISUAL
        FULLBRIGHT( "Fullbright",  Category.VISUAL, FeatureTag.UTILITY),
        ZOOM(       "Zoom",        Category.VISUAL, FeatureTag.UTILITY),
        GLOW_ESP(   "Glow ESP",    Category.VISUAL, FeatureTag.HACK),
        NO_WEATHER( "No Weather",  Category.VISUAL, FeatureTag.UTILITY),
        ANTI_BLIND( "Anti-Blind",  Category.VISUAL, FeatureTag.UTILITY),
        GUI_SCALE(  "GUI Scale",   Category.VISUAL, FeatureTag.UTILITY),
        TIME_CHANGER("Time Changer", Category.VISUAL, FeatureTag.UTILITY),
        XRAY(       "XRay",        Category.VISUAL, FeatureTag.HACK),
        CUSTOM_CAPE("Custom Cape", Category.VISUAL, FeatureTag.UTILITY),

        // WORLD
        FAST_PLACE( "Fast Place",  Category.WORLD, FeatureTag.HACK),
        FAST_BREAK( "Fast Break",  Category.WORLD, FeatureTag.HACK),

        // PERFORMANCE (Echte FPS Booster)
        NO_PARTICLES(      "No Particles",       Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        NO_ENTITY_SHADOWS( "No Entity Shadows",  Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        ENTITY_CULLING(    "Entity Culling",      Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        FAST_RENDER(       "Fast Render",         Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        HIDE_HAND(         "Hide Hand",           Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        NO_FOG(            "No Fog",              Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        FAST_LEAVES(       "Fast Leaves",         Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        NO_CLOUDS(         "No Clouds",           Category.PERFORMANCE, FeatureTag.PERFORMANCE),
        NO_SKY(            "No Sky",              Category.PERFORMANCE, FeatureTag.PERFORMANCE),

        // MINIGAME
        TETRIS(            "Tetris Minigame",     Category.MINIGAME, FeatureTag.MINIGAME),
        ANTI_AFK(          "Anti AFK",            Category.MINIGAME, FeatureTag.MINIGAME),
        NO_KNOCKBACK(      "No Knockback",        Category.MINIGAME, FeatureTag.HACK),
        BED_ALARM(         "Bed Alarm",           Category.MINIGAME, FeatureTag.MINIGAME),
        AUTO_GG(           "Auto GG",             Category.MINIGAME, FeatureTag.MINIGAME),
        COMPASS_HUD(       "Compass HUD",         Category.MINIGAME, FeatureTag.MINIGAME),

        // SOCIAL
        HOST_LAN(          "Host LAN World",      Category.SOCIAL, FeatureTag.UTILITY),
        FRIEND_MANAGER(    "Friend Manager",      Category.SOCIAL, FeatureTag.UTILITY);

        public final String displayName;
        public final Category category;
        public final FeatureTag tag;

        Feature(String displayName, Category category, FeatureTag tag) {
            this.displayName = displayName;
            this.category    = category;
            this.tag         = tag;
        }

        public boolean isHack() { return tag == FeatureTag.HACK; }
    }

    // ── Settings System ──────────────────────────────────────
    public static abstract class Setting {
        public final String name;
        public final Feature feature;

        public Setting(String name, Feature feature) {
            this.name = name;
            this.feature = feature;
        }
    }

    public static class SliderSetting extends Setting {
        public double value;
        public final double min;
        public final double max;
        public final double defaultValue;
        public final String suffix;

        public SliderSetting(String name, Feature feature, double defaultValue, double min, double max, String suffix) {
            super(name, feature);
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
        }
    }

    public static class BooleanSetting extends Setting {
        public boolean value;
        public final boolean defaultValue;

        public BooleanSetting(String name, Feature feature, boolean defaultValue) {
            super(name, feature);
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }
    }

    private static final Map<Feature, Boolean> states = new HashMap<>();
    private static final Map<Feature, Integer> keycodes = new HashMap<>();
    private static final Map<Feature, List<Setting>> featureSettings = new HashMap<>();

    private static boolean hideHacks = false;

    public static boolean isHideHacks() { return hideHacks; }
    public static void setHideHacks(boolean v) { hideHacks = v; }
    public static void toggleHideHacks() { hideHacks = !hideHacks; }

    // --- Authentication State (must be before static block) ---
    private static String loggedInUser     = "";
    private static String loggedInPassword = "";

    // --- Friends List (must be before static block that calls loadFriends) ---
    private static final List<String> friends    = new ArrayList<>();
    private static final File FRIENDS_FILE        = new File("simplemod_friends.json");
    private static final Gson GSON                = new Gson();

    static {
        // ---- 1. Standard-Keybinds ----
        keycodes.put(Feature.AUTO_SPRINT, GLFW.GLFW_KEY_H);
        keycodes.put(Feature.FREE_CAM,     GLFW.GLFW_KEY_F8);
        keycodes.put(Feature.NO_FALL,      GLFW.GLFW_KEY_N);
        keycodes.put(Feature.FLIGHT,       GLFW.GLFW_KEY_R);
        keycodes.put(Feature.SAFE_WALK,    GLFW.GLFW_KEY_J);
        keycodes.put(Feature.JESUS,        GLFW.GLFW_KEY_K);
        keycodes.put(Feature.SPIDER,       GLFW.GLFW_KEY_V);
        keycodes.put(Feature.STEP,         GLFW.GLFW_KEY_UP);
        keycodes.put(Feature.HIGH_JUMP,    GLFW.GLFW_KEY_O);
        keycodes.put(Feature.NO_SLOWDOWN,  GLFW.GLFW_KEY_M);

        keycodes.put(Feature.FULLBRIGHT,   GLFW.GLFW_KEY_G);
        keycodes.put(Feature.ZOOM,         GLFW.GLFW_KEY_C);
        keycodes.put(Feature.GLOW_ESP,     GLFW.GLFW_KEY_B);
        keycodes.put(Feature.NO_WEATHER,   GLFW.GLFW_KEY_L);
        keycodes.put(Feature.ANTI_BLIND,   0);

        keycodes.put(Feature.FAST_PLACE,   GLFW.GLFW_KEY_U);
        keycodes.put(Feature.FAST_BREAK,   GLFW.GLFW_KEY_I);
        keycodes.put(Feature.REACH,        GLFW.GLFW_KEY_P);
        keycodes.put(Feature.TRIGGER_BOT,  GLFW.GLFW_KEY_RIGHT_ALT);

        keycodes.put(Feature.NO_PARTICLES,      0);
        keycodes.put(Feature.NO_ENTITY_SHADOWS, 0);
        keycodes.put(Feature.ENTITY_CULLING,    0);
        keycodes.put(Feature.FAST_RENDER,       0);
        keycodes.put(Feature.HIDE_HAND,         0);
        keycodes.put(Feature.NO_FOG,            0);
        keycodes.put(Feature.FAST_LEAVES,       0);
        keycodes.put(Feature.NO_CLOUDS,         0);
        keycodes.put(Feature.NO_SKY,            0);
        keycodes.put(Feature.GUI_SCALE,         0);
        keycodes.put(Feature.HOST_LAN,          0);
        keycodes.put(Feature.FRIEND_MANAGER,    0);

        keycodes.put(Feature.ANTI_AFK,     0);
        keycodes.put(Feature.NO_KNOCKBACK, 0);
        keycodes.put(Feature.BED_ALARM,    0);
        keycodes.put(Feature.AUTO_GG,      0);
        keycodes.put(Feature.COMPASS_HUD,  0);

        // Combat & Visual Feature keybinds
        keycodes.put(Feature.KILL_AURA,     GLFW.GLFW_KEY_R);
        keycodes.put(Feature.AIM_ASSIST,    0);
        keycodes.put(Feature.AUTO_CRIT,     0);
        keycodes.put(Feature.HITBOX,        0);
        keycodes.put(Feature.CUSTOM_CAPE,   0);
        keycodes.put(Feature.SPEED,         GLFW.GLFW_KEY_X);
        keycodes.put(Feature.ANTI_FIRE,     0);
        keycodes.put(Feature.SCAFFOLD,      GLFW.GLFW_KEY_Z);
        keycodes.put(Feature.TIME_CHANGER,  0);
        keycodes.put(Feature.XRAY,          0);
        keycodes.put(Feature.CLICK_ASSIST,  0);
        keycodes.put(Feature.TETRIS,        0);

        // ---- 2. Settings ----
        // KillAura Settings
        addSetting(new SliderSetting("Range",         Feature.KILL_AURA,   4.2,  2.0,  6.0, " blocks"));
        addSetting(new SliderSetting("APS",           Feature.KILL_AURA,   10.0, 1.0,  20.0, " /s"));
        addSetting(new BooleanSetting("Silent Aim",   Feature.KILL_AURA,   true));
        addSetting(new BooleanSetting("Players Only", Feature.KILL_AURA,   true));
        addSetting(new BooleanSetting("Mobs",         Feature.KILL_AURA,   false));
        addSetting(new BooleanSetting("Auto Rotate",  Feature.KILL_AURA,   false));

        // AimAssist Settings
        addSetting(new SliderSetting("Aim Range",     Feature.AIM_ASSIST,  4.5,  2.0,  7.0, " blocks"));
        addSetting(new SliderSetting("Smoothness",    Feature.AIM_ASSIST,  4.0,  1.0,  10.0, ""));
        addSetting(new SliderSetting("FOV",           Feature.AIM_ASSIST,  90.0, 20.0, 180.0, "°"));

        // AutoCrit Settings
        addSetting(new BooleanSetting("Only on Fall", Feature.AUTO_CRIT,   false));

        // Hitbox Extender Settings
        addSetting(new SliderSetting("Expand Size",   Feature.HITBOX,      0.4,  0.1,  2.0, " blocks"));

        // Custom Cape Settings
        addSetting(new SliderSetting("Cape Design",   Feature.CUSTOM_CAPE, 1.0,  1.0,  4.0, ""));
        addSetting(new BooleanSetting("Animated Wave",Feature.CUSTOM_CAPE, true));

        addSetting(new SliderSetting("Speed",         Feature.FREE_CAM,    0.5,  0.1,  2.5, " blocks"));
        addSetting(new BooleanSetting("Show Body",    Feature.FREE_CAM,    true));

        addSetting(new SliderSetting("FOV Level",     Feature.ZOOM,       15.0,  5.0, 50.0, "°"));
        addSetting(new SliderSetting("Lerp Speed",    Feature.ZOOM,        0.15, 0.05, 0.50, ""));

        addSetting(new SliderSetting("Scale",     Feature.GUI_SCALE,  1.0,  0.5,  1.5, "x"));
        addSetting(new SliderSetting("Card Size", Feature.GUI_SCALE, 36.0, 26.0, 60.0, "px"));

        addSetting(new SliderSetting("Fly Speed",     Feature.FLIGHT,      0.6,  0.1,  3.0, " blocks"));

        addSetting(new SliderSetting("Climb Speed",   Feature.SPIDER,      0.2,  0.05, 0.8, ""));

        addSetting(new SliderSetting("Jump Power",    Feature.HIGH_JUMP,   1.5,  1.0,  3.0, "x"));

        addSetting(new SliderSetting("Place Cooldown",Feature.FAST_PLACE,  0.0,  0.0,  4.0, " ticks"));

        addSetting(new SliderSetting("Range",         Feature.REACH,       5.0,  3.0,  7.5, " blocks"));

        addSetting(new SliderSetting("Delay Ticks",   Feature.TRIGGER_BOT, 1.0,  0.0, 10.0, " ticks"));

        addSetting(new SliderSetting("AFK Interval",  Feature.ANTI_AFK,    30.0, 5.0, 120.0, "s"));

        addSetting(new SliderSetting("Chat Delay",    Feature.AUTO_GG,     1.0,  0.5,  5.0, "s"));

        // Entity Culling Settings
        addSetting(new SliderSetting("Max Distance",  Feature.ENTITY_CULLING, 32.0, 8.0, 64.0, " blocks"));
        addSetting(new BooleanSetting("Cull Animals", Feature.ENTITY_CULLING, true));
        addSetting(new BooleanSetting("Cull Items",   Feature.ENTITY_CULLING, true));

        // Fast Render (Block Entity Culling) Settings
        addSetting(new SliderSetting("Max Distance",  Feature.FAST_RENDER, 24.0, 8.0, 48.0, " blocks"));
        addSetting(new BooleanSetting("Cull Signs",   Feature.FAST_RENDER, true));
        addSetting(new BooleanSetting("Cull Chests",  Feature.FAST_RENDER, true));

        // Host LAN Settings
        addSetting(new SliderSetting("LAN Port",      Feature.HOST_LAN,    25565.0, 1024.0, 65535.0, ""));
        addSetting(new BooleanSetting("Allow Cheats", Feature.HOST_LAN,    true));

        // New feature settings
        addSetting(new SliderSetting("Speed Mult",    Feature.SPEED,        2.0,  1.1,  8.0, "x"));
        addSetting(new SliderSetting("CPS",           Feature.CLICK_ASSIST, 12.0, 1.0, 20.0, " cps"));
        addSetting(new BooleanSetting("Jitter",       Feature.CLICK_ASSIST, true));
        addSetting(new SliderSetting("Time",          Feature.TIME_CHANGER, 6000.0, 0.0, 24000.0, ""));
        addSetting(new BooleanSetting("Lock Time",    Feature.TIME_CHANGER, true));

        // Load friends
        loadFriends();
    }

    // --- Authentication State accessors ---
    public static String getLoggedInUser()    { return loggedInUser; }
    public static String getLoggedInPassword() { return loggedInPassword; }
    public static void setLoggedInUser(String user)    { loggedInUser = user; }
    public static void setLoggedInPassword(String pass) { loggedInPassword = pass; }
    public static boolean isLoggedIn()        { return !loggedInUser.isEmpty(); }
    public static boolean isClientUnlocked()  {
        return isLoggedIn() && loggedInUser.equals("Pepe838") && loggedInPassword.equals("Test");
    }

    // --- Friends List Manager accessors ---

    public static void loadFriends() {
        friends.clear();
        if (FRIENDS_FILE.exists()) {
            try (FileReader reader = new FileReader(FRIENDS_FILE)) {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) friends.addAll(loaded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveFriends() {
        try (FileWriter writer = new FileWriter(FRIENDS_FILE)) {
            GSON.toJson(friends, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getFriends() { return friends; }
    public static void addFriend(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
            saveFriends();
        }
    }
    public static void removeFriend(String name) {
        friends.remove(name);
        saveFriends();
    }
    public static boolean isFriend(String name) {
        return friends.contains(name);
    }

    private static void addSetting(Setting setting) {
        featureSettings.computeIfAbsent(setting.feature, k -> new ArrayList<>()).add(setting);
    }

    public static List<Setting> getSettings(Feature feature) {
        return featureSettings.getOrDefault(feature, new ArrayList<>());
    }

    public static int getKeycode(Feature f) {
        return keycodes.getOrDefault(f, 0);
    }

    public static void setKeycode(Feature f, int key) {
        keycodes.put(f, key);
    }

    public static String getKeyName(Feature f) {
        int code = getKeycode(f);
        if (code <= 0) return "NONE";
        
        String name = GLFW.glfwGetKeyName(code, 0);
        if (name != null) return name.toUpperCase();
        
        if (code == GLFW.GLFW_KEY_F1)  return "F1";
        if (code == GLFW.GLFW_KEY_F2)  return "F2";
        if (code == GLFW.GLFW_KEY_F3)  return "F3";
        if (code == GLFW.GLFW_KEY_F4)  return "F4";
        if (code == GLFW.GLFW_KEY_F5)  return "F5";
        if (code == GLFW.GLFW_KEY_F6)  return "F6";
        if (code == GLFW.GLFW_KEY_F7)  return "F7";
        if (code == GLFW.GLFW_KEY_F8)  return "F8";
        if (code == GLFW.GLFW_KEY_F9)  return "F9";
        if (code == GLFW.GLFW_KEY_F10) return "F10";
        if (code == GLFW.GLFW_KEY_F11) return "F11";
        if (code == GLFW.GLFW_KEY_F12) return "F12";
        if (code == GLFW.GLFW_KEY_SPACE) return "SPACE";
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT)   return "LSHIFT";
        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT)  return "RSHIFT";
        if (code == GLFW.GLFW_KEY_LEFT_CONTROL) return "LCTRL";
        if (code == GLFW.GLFW_KEY_UP)    return "UP";
        if (code == GLFW.GLFW_KEY_DOWN)  return "DOWN";
        if (code == GLFW.GLFW_KEY_LEFT)  return "LEFT";
        if (code == GLFW.GLFW_KEY_RIGHT) return "RIGHT";
        
        return "KEY " + code;
    }

    public static boolean isEnabled(Feature f)             { return states.getOrDefault(f, false); }
    public static void    setEnabled(Feature f, boolean b) { states.put(f, b); }
    public static void    toggle(Feature f)                { states.put(f, !isEnabled(f)); }

    public static double getSlider(Feature f, String name) {
        List<Setting> list = featureSettings.get(f);
        if (list != null) {
            for (Setting s : list) {
                if (s instanceof SliderSetting && s.name.equals(name)) {
                    return ((SliderSetting) s).value;
                }
            }
        }
        return 0;
    }

    public static boolean getBool(Feature f, String name) {
        List<Setting> list = featureSettings.get(f);
        if (list != null) {
            for (Setting s : list) {
                if (s instanceof BooleanSetting && s.name.equals(name)) {
                    return ((BooleanSetting) s).value;
                }
            }
        }
        return false;
    }
}
