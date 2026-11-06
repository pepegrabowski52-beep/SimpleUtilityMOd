package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import com.example.gui.ClickGuiScreen;
import com.example.ModFeatures.Feature;

public class SimpleModClient implements ClientModInitializer {

    // ── FreeCam state ────────────────────────────────────────
    public static Vec3d freeCamPos = Vec3d.ZERO;
    private static boolean wasFreeCamActive = false;

    // ── Anti AFK state ───────────────────────────────────────
    private static int antiAfkTimer = 0;
    private static int antiAfkDir = 0;

    // ── Bed Alarm state ──────────────────────────────────────
    private static int bedAlarmCooldown = 0;

    // ── Auto GG state ────────────────────────────────────────
    private static int autoGgTimer = -1;

    public static void triggerAutoGg(int ticks) {
        autoGgTimer = ticks;
    }

    // Key state tracking array
    private static final boolean[] prevKeyState = new boolean[512];

    public static Vec3d getFreeCamPos() { return freeCamPos; }

    /**
     * Directly hosts the world on LAN. Can be called from any thread/GUI.
     * Returns a human-readable result string.
     */
    public static String hostLan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return "Not in game!";
        if (client.getServer() == null) return "Must be in a singleplayer world!";
        int     port   = (int) ModFeatures.getSlider(ModFeatures.Feature.HOST_LAN, "LAN Port");
        boolean cheats = ModFeatures.getBool(ModFeatures.Feature.HOST_LAN, "Allow Cheats");
        boolean ok     = client.getServer().openToLan(null, cheats, port);
        if (ok) {
            client.keyboard.setClipboard("127.0.0.1:" + port);
            client.player.sendMessage(
                Text.literal("§8[§bCs2§8] §aLAN hosted on port §e" + port + "§a! IP copied."), false);
            return "Hosted on port " + port + "!";
        } else {
            return "Already hosted or failed! Check port.";
        }
    }

    @Override
    public void onInitializeClient() {

        // ── Client tick ──────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            long handle = client.getWindow().getHandle();

            // ---- 1. Dynamisches Tastatur-Handling ----
            for (Feature f : Feature.values()) {
                int code = ModFeatures.getKeycode(f);
                if (code <= 0 || code >= 512) continue;

                boolean isDown = InputUtil.isKeyPressed(handle, code);
                boolean wasDown = prevKeyState[code];
                prevKeyState[code] = isDown;

                if (client.currentScreen == null && client.player != null) {
                    if (isDown && !wasDown) {
                        if (!ModFeatures.isClientUnlocked()) {
                            client.player.sendMessage(Text.literal("§8[§bCs2§8] §cClient is locked! Login with Pepe838 / Test in Main Menu."), true);
                            continue;
                        }
                        ModFeatures.toggle(f);
                        String status = ModFeatures.isEnabled(f) ? "§aAN" : "§cAUS";
                        client.player.sendMessage(
                            Text.literal("§8[§bCs2§8] §f" + f.displayName + " " + status), true);
                    }
                }
            }

            // ---- 2. Öffnen des Click GUIs mit Y ----
            boolean isYDown = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_Y);
            boolean wasYDown = prevKeyState[GLFW.GLFW_KEY_Y];
            prevKeyState[GLFW.GLFW_KEY_Y] = isYDown;

            if (isYDown && !wasYDown && client.currentScreen == null) {
                client.setScreen(new ClickGuiScreen());
            }

            if (client.player == null) return;

            // Block all cheats if client is locked
            if (!ModFeatures.isClientUnlocked()) {
                if (!client.player.isCreative() && !client.player.isSpectator()) {
                    client.player.getAbilities().flying = false;
                }
                return;
            }

            // ---- 3. LAN Hosting Handler (tick-based fallback) ----
            if (ModFeatures.isEnabled(Feature.HOST_LAN)) {
                ModFeatures.setEnabled(Feature.HOST_LAN, false);
                hostLan(); // delegate to static method
            }

            // ---- 4. Auto GG Scheduler Tick ----
            if (autoGgTimer > 0) {
                autoGgTimer--;
                if (autoGgTimer == 0) {
                    client.player.networkHandler.sendChatMessage("GG");
                }
            }

            // ────────────────── MOVEMENT ──────────────────────

            // Auto Sprint
            if (ModFeatures.isEnabled(Feature.AUTO_SPRINT) && client.currentScreen == null) {
                client.player.setSprinting(true);
            }

            // No Fall
            if (ModFeatures.isEnabled(Feature.NO_FALL)) {
                client.player.fallDistance = 0f;
            }

            // Flight
            boolean flightNow = ModFeatures.isEnabled(Feature.FLIGHT);
            if (flightNow) {
                client.player.getAbilities().flying = true;
                float flySpeed = (float) ModFeatures.getSlider(Feature.FLIGHT, "Fly Speed");
                client.player.getAbilities().setFlySpeed(flySpeed * 0.1f);
            } else {
                if (!client.player.isCreative() && !client.player.isSpectator()) {
                    client.player.getAbilities().flying = false;
                }
                client.player.getAbilities().setFlySpeed(0.05f);
            }

            // High Jump: raise jump height attribute
            if (ModFeatures.isEnabled(Feature.HIGH_JUMP)) {
                EntityAttributeInstance jumpAttr = client.player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
                if (jumpAttr != null) {
                    double power = ModFeatures.getSlider(Feature.HIGH_JUMP, "Jump Power");
                    jumpAttr.setBaseValue(0.42 * power);
                }
            } else {
                EntityAttributeInstance jumpAttr = client.player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
                if (jumpAttr != null && jumpAttr.getBaseValue() != 0.42) jumpAttr.setBaseValue(0.42);
            }

            // FreeCam – setup
            boolean freeCamNow = ModFeatures.isEnabled(Feature.FREE_CAM);
            if (freeCamNow && !wasFreeCamActive) {
                freeCamPos = client.player.getPos()
                    .add(0, client.player.getEyeHeight(client.player.getPose()), 0);
            }
            wasFreeCamActive = freeCamNow;

            // FreeCam – movement
            if (freeCamNow && client.currentScreen == null) {
                float speedVal = (float) ModFeatures.getSlider(Feature.FREE_CAM, "Speed");
                float speed = client.options.sprintKey.isPressed() ? speedVal * 2.5f : speedVal;

                double yaw   = Math.toRadians(client.player.getYaw());
                double pitch = Math.toRadians(client.player.getPitch());

                if (client.options.forwardKey.isPressed()) {
                    freeCamPos = freeCamPos.add(
                        -Math.sin(yaw) * Math.cos(pitch) * speed,
                        -Math.sin(pitch) * speed,
                         Math.cos(yaw) * Math.cos(pitch) * speed);
                }
                if (client.options.backKey.isPressed()) {
                    freeCamPos = freeCamPos.add(
                         Math.sin(yaw) * Math.cos(pitch) * speed,
                         Math.sin(pitch) * speed,
                        -Math.cos(yaw) * Math.cos(pitch) * speed);
                }
                if (client.options.leftKey.isPressed()) {
                    freeCamPos = freeCamPos.add(
                        -Math.sin(yaw - Math.PI / 2) * speed, 0,
                         Math.cos(yaw - Math.PI / 2) * speed);
                }
                if (client.options.rightKey.isPressed()) {
                    freeCamPos = freeCamPos.add(
                        -Math.sin(yaw + Math.PI / 2) * speed, 0,
                         Math.cos(yaw + Math.PI / 2) * speed);
                }
                if (client.options.jumpKey.isPressed())  freeCamPos = freeCamPos.add(0,  speed, 0);
                if (client.options.sneakKey.isPressed()) freeCamPos = freeCamPos.add(0, -speed, 0);
            }

            // Step (Auto climb full blocks) - via EntityAttribute STEP_HEIGHT
            EntityAttributeInstance stepAttr = client.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (stepAttr != null) {
                stepAttr.setBaseValue(ModFeatures.isEnabled(Feature.STEP) ? 1.0 : 0.6);
            }

            // ────────────────── VISUAL ────────────────────────

            // Anti-Blind
            if (ModFeatures.isEnabled(Feature.ANTI_BLIND)) {
                client.player.removeStatusEffect(StatusEffects.BLINDNESS);
            }

            // ────────────────── COMBAT ─────────────────────────

            // KillAura: 360 attack nearest target within range (with Silent Aim support)
            if (ModFeatures.isEnabled(Feature.KILL_AURA) && client.interactionManager != null
                    && client.world != null && client.currentScreen == null) {
                double rangeKa = ModFeatures.getSlider(Feature.KILL_AURA, "Range");
                double aps = ModFeatures.getSlider(Feature.KILL_AURA, "APS");
                boolean silentAim = ModFeatures.getBool(Feature.KILL_AURA, "Silent Aim");
                boolean playersOnly = ModFeatures.getBool(Feature.KILL_AURA, "Players Only");
                boolean mobs = ModFeatures.getBool(Feature.KILL_AURA, "Mobs");
                boolean autoRotate = ModFeatures.getBool(Feature.KILL_AURA, "Auto Rotate");

                int interval = Math.max(1, (int)(20.0 / aps));
                if (client.inGameHud.getTicks() % interval == 0) {
                    net.minecraft.entity.Entity target = null;
                    double bestDist = rangeKa * rangeKa;

                    for (net.minecraft.entity.Entity e : client.world.getEntities()) {
                        if (e == client.player || !e.isAlive()) continue;
                        if (e instanceof net.minecraft.entity.player.PlayerEntity && !playersOnly && !mobs) continue;
                        if (e instanceof net.minecraft.entity.player.PlayerEntity && ModFeatures.isFriend(e.getName().getString())) continue;
                        if (e instanceof net.minecraft.entity.mob.MobEntity && !mobs) continue;
                        if (!(e instanceof net.minecraft.entity.LivingEntity)) continue;

                        double distSq = client.player.squaredDistanceTo(e);
                        if (distSq < bestDist) {
                            bestDist = distSq;
                            target = e;
                        }
                    }

                    if (target != null) {
                        // Only rotate camera if Silent Aim is disabled and Auto Rotate is enabled
                        if (!silentAim && autoRotate) {
                            double dx = target.getX() - client.player.getX();
                            double dy = (target.getY() + target.getStandingEyeHeight()) - (client.player.getY() + client.player.getStandingEyeHeight());
                            double dz = target.getZ() - client.player.getZ();
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                            float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
                            client.player.setYaw(yaw);
                            client.player.setPitch(pitch);
                        }

                        // Auto Crit assistance
                        if (ModFeatures.isEnabled(Feature.AUTO_CRIT) && client.player.isOnGround()) {
                            client.player.jump();
                        }

                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }

            // Tetris Minigame trigger
            if (ModFeatures.isEnabled(Feature.TETRIS)) {
                ModFeatures.setEnabled(Feature.TETRIS, false);
                client.setScreen(new com.example.gui.TetrisScreen(client.currentScreen));
            }

            // AimAssist: smoothly pulls crosshair towards nearest target
            if (ModFeatures.isEnabled(Feature.AIM_ASSIST) && client.world != null
                    && client.currentScreen == null && (client.options.attackKey.isPressed() || client.options.useKey.isPressed())) {
                double aimRange = ModFeatures.getSlider(Feature.AIM_ASSIST, "Aim Range");
                double smooth = ModFeatures.getSlider(Feature.AIM_ASSIST, "Smoothness");
                double maxFov = ModFeatures.getSlider(Feature.AIM_ASSIST, "FOV");

                net.minecraft.entity.Entity aimTarget = null;
                double closestFov = maxFov;

                for (net.minecraft.entity.Entity e : client.world.getEntities()) {
                    if (e == client.player || !e.isAlive() || !(e instanceof net.minecraft.entity.LivingEntity)) continue;
                    if (e instanceof net.minecraft.entity.player.PlayerEntity && ModFeatures.isFriend(e.getName().getString())) continue;
                    if (client.player.squaredDistanceTo(e) > aimRange * aimRange) continue;

                    double dx = e.getX() - client.player.getX();
                    double dy = (e.getY() + e.getStandingEyeHeight()) - (client.player.getY() + client.player.getStandingEyeHeight());
                    double dz = e.getZ() - client.player.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                    float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

                    float yawDiff = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - client.player.getYaw()));
                    if (yawDiff < closestFov) {
                        closestFov = yawDiff;
                        aimTarget = e;
                    }
                }

                if (aimTarget != null) {
                    double dx = aimTarget.getX() - client.player.getX();
                    double dy = (aimTarget.getY() + aimTarget.getStandingEyeHeight()) - (client.player.getY() + client.player.getStandingEyeHeight());
                    double dz = aimTarget.getZ() - client.player.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                    float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

                    float deltaYaw = net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
                    float deltaPitch = net.minecraft.util.math.MathHelper.wrapDegrees(targetPitch - client.player.getPitch());

                    client.player.setYaw(client.player.getYaw() + (float)(deltaYaw / smooth));
                    client.player.setPitch(client.player.getPitch() + (float)(deltaPitch / smooth));
                }
            }

            // ────────────────── WORLD ─────────────────────────

            // Reach
            boolean reachEnabled = ModFeatures.isEnabled(Feature.REACH);
            double range = ModFeatures.getSlider(Feature.REACH, "Range");

            EntityAttributeInstance blockAttr = client.player.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE);
            if (blockAttr != null) blockAttr.setBaseValue(reachEnabled ? range : 4.5);

            EntityAttributeInstance entityAttr = client.player.getAttributeInstance(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
            if (entityAttr != null) entityAttr.setBaseValue(reachEnabled ? range : 3.0);

            // TriggerBot: attack entity in crosshair automatically
            if (ModFeatures.isEnabled(Feature.TRIGGER_BOT) && client.targetedEntity != null
                    && client.interactionManager != null) {
                int delayTicks = (int) ModFeatures.getSlider(Feature.TRIGGER_BOT, "Delay Ticks");
                if ((client.inGameHud.getTicks() % Math.max(1, delayTicks + 1)) == 0) {
                    if (ModFeatures.isEnabled(Feature.AUTO_CRIT) && client.player.isOnGround()) {
                        client.player.jump();
                    }
                    client.interactionManager.attackEntity(client.player, client.targetedEntity);
                    client.player.swingHand(Hand.MAIN_HAND);
                }
            }

            // ────────────────── MINIGAME ──────────────────────

            // Anti AFK: wiggle player back and forth
            if (ModFeatures.isEnabled(Feature.ANTI_AFK)) {
                antiAfkTimer++;
                int intervalTicks = (int) (ModFeatures.getSlider(Feature.ANTI_AFK, "AFK Interval") * 20);
                if (antiAfkTimer >= intervalTicks) {
                    antiAfkTimer = 0;
                    float wiggle = (antiAfkDir == 0) ? 5f : -5f;
                    client.player.setYaw(client.player.getYaw() + wiggle);
                    antiAfkDir = 1 - antiAfkDir;
                }
            }

            // Bed Alarm: low health warning
            if (ModFeatures.isEnabled(Feature.BED_ALARM)) {
                if (bedAlarmCooldown > 0) bedAlarmCooldown--;
                if (client.player.getHealth() < 12f && bedAlarmCooldown == 0) {
                    client.player.sendMessage(Text.literal("§c§l⚠ WARNUNG: Niedriges Leben! (" + (int)(client.player.getHealth() / 2) + " Herzen)"), false);
                    bedAlarmCooldown = 100;
                }
            }

            // ── NEW FEATURES ──────────────────────────────────────────────────

            // Speedhack: boost movement speed attribute
            EntityAttributeInstance speedAttr = client.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                if (ModFeatures.isEnabled(Feature.SPEED)) {
                    double mult = ModFeatures.getSlider(Feature.SPEED, "Speed Mult");
                    speedAttr.setBaseValue(0.1 * mult);
                } else {
                    if (speedAttr.getBaseValue() != 0.1) speedAttr.setBaseValue(0.1);
                }
            }

            // Anti Fire: extinguish fire every tick
            if (ModFeatures.isEnabled(Feature.ANTI_FIRE)) {
                client.player.extinguish();
            }

            // Scaffold: place a block directly below the player when in mid-air
            if (ModFeatures.isEnabled(Feature.SCAFFOLD) && client.interactionManager != null
                    && !client.player.isOnGround() && client.currentScreen == null) {
                net.minecraft.util.math.BlockPos below = client.player.getBlockPos().down();
                net.minecraft.world.World world = client.player.getWorld();
                if (world != null && world.getBlockState(below).isAir()) {
                    // Find a solid block in hotbar
                    net.minecraft.item.ItemStack best = net.minecraft.item.ItemStack.EMPTY;
                    int bestSlot = -1;
                    for (int slot = 0; slot < 9; slot++) {
                        net.minecraft.item.ItemStack stack = client.player.getInventory().getStack(slot);
                        if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
                            best = stack; bestSlot = slot; break;
                        }
                    }
                    if (bestSlot >= 0) {
                        int prevSlot = client.player.getInventory().selectedSlot;
                        client.player.getInventory().selectedSlot = bestSlot;
                        net.minecraft.util.math.BlockPos placeOn = below.down();
                        net.minecraft.util.hit.BlockHitResult hit = new net.minecraft.util.hit.BlockHitResult(
                            net.minecraft.util.math.Vec3d.ofCenter(placeOn).add(0, 0.5, 0),
                            net.minecraft.util.math.Direction.UP, placeOn, false);
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
                        client.player.getInventory().selectedSlot = prevSlot;
                    }
                }
            }

            // Click Assist: auto left-click at configured CPS when holding left mouse
            if (ModFeatures.isEnabled(Feature.CLICK_ASSIST) && client.interactionManager != null
                    && client.currentScreen == null && client.targetedEntity != null) {
                double cps = ModFeatures.getSlider(Feature.CLICK_ASSIST, "CPS");
                boolean jitter = ModFeatures.getBool(Feature.CLICK_ASSIST, "Jitter");
                int interval = (int)(20.0 / cps);
                if (jitter) interval += (int)(Math.random() * 2);
                if (interval < 1) interval = 1;
                int ticks = client.inGameHud.getTicks();
                if (ticks % interval == 0 && client.options.attackKey.isPressed()) {
                    client.interactionManager.attackEntity(client.player, client.targetedEntity);
                    client.player.swingHand(Hand.MAIN_HAND);
                }
            }

            // Time Changer: override client-side world time
            if (ModFeatures.isEnabled(Feature.TIME_CHANGER) && client.world != null) {
                boolean lock = ModFeatures.getBool(Feature.TIME_CHANGER, "Lock Time");
                long targetTime = (long) ModFeatures.getSlider(Feature.TIME_CHANGER, "Time");
                if (lock) {
                    ((net.minecraft.client.world.ClientWorld) client.world).setTimeOfDay(targetTime);
                }
            }

            // XRay: extreme gamma boost (clientside visual)
            if (client.options != null) {
                if (ModFeatures.isEnabled(Feature.XRAY)) {
                    client.options.getGamma().setValue(100.0);
                } else if (ModFeatures.isEnabled(Feature.FULLBRIGHT)) {
                    client.options.getGamma().setValue(16.0);
                } else {
                    // Don't reset gamma if the user has custom settings – only reset if we set it
                    double g = client.options.getGamma().getValue();
                    if (g >= 15.9 && g <= 100.0) client.options.getGamma().setValue(1.0);
                }
            }
        });

        // ── HUD Rendering ─────────────────────────────────────
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null) return;

            // Globales, synchronisiertes Theme holen
            ModFeatures.Theme theme = ModFeatures.getTheme();
            int themeColor = theme.color;

            // ---- Left side: Watermark ----
            int y = 5;
            drawContext.drawText(mc.textRenderer, "[Cs2] Utility Client v1.6", 5, y, themeColor, true);
            y += 12;

            String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
            drawContext.drawText(mc.textRenderer, coords, 5, y, 0xCCCCCC, true);
            y += 11;

            String dirName = mc.player.getHorizontalFacing().getName();
            String facing  = "Dir: " + Character.toUpperCase(dirName.charAt(0)) + dirName.substring(1);
            drawContext.drawText(mc.textRenderer, facing, 5, y, 0x999999, true);
            y += 11;

            if (ModFeatures.isHideHacks()) {
                drawContext.drawText(mc.textRenderer, "§a[Hacks Hidden]", 5, y, 0xFFFFFFFF, true);
            }

            // ---- Center Top: Compass HUD ----
            if (ModFeatures.isEnabled(Feature.COMPASS_HUD)) {
                int screenW = drawContext.getScaledWindowWidth();
                int centerX = screenW / 2;
                int compassY = 12;

                // Hintergrund-Leiste
                drawContext.fill(centerX - 60, compassY - 2, centerX + 60, compassY + 10, 0xAA08080C);
                drawContext.fill(centerX - 60, compassY + 9, centerX + 60, compassY + 10, themeColor);

                float yaw = mc.player.getYaw() % 360;
                if (yaw < 0) yaw += 360;

                String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
                for (int i = 0; i < 8; i++) {
                    float angle = i * 45;
                    float diff = angle - yaw;
                    while (diff < -180) diff += 360;
                    while (diff > 180) diff -= 360;

                    if (Math.abs(diff) < 50) {
                        int x = centerX + (int)(diff * 1.2);
                        int col = Math.abs(diff) < 10 ? themeColor : 0xFFFFFFFF;
                        drawContext.drawText(mc.textRenderer, directions[i], x - mc.textRenderer.getWidth(directions[i])/2, compassY, col, true);
                    }
                }
                // Zeiger-Nadel in der Mitte
                drawContext.fill(centerX - 1, compassY - 4, centerX + 1, compassY, themeColor);
            }

            // ---- Right side: Active Modules List ----
            int moduleY = 5;
            int screenW = drawContext.getScaledWindowWidth();
            for (Feature feature : Feature.values()) {
                if (!ModFeatures.isEnabled(feature)) continue;

                String name = getHudName(feature);
                int textW = mc.textRenderer.getWidth(name);
                int bx    = screenW - textW - 8;

                drawContext.fill(bx - 4, moduleY - 1, screenW, moduleY + 10, 0x8A10101C);
                drawContext.fill(bx - 4, moduleY - 1, bx - 2, moduleY + 10, themeColor);
                drawContext.drawText(mc.textRenderer, name, bx, moduleY, 0xFFFFFFFF, true);
                moduleY += 12;
            }
        });
    }

    private static String getHudName(Feature f) {
        String name = f.displayName;
        switch (f) {
            case FREE_CAM   -> name += String.format(" §7(%.1f)", ModFeatures.getSlider(f, "Speed"));
            case ZOOM       -> name += String.format(" §7(%.0f°)", ModFeatures.getSlider(f, "FOV Level"));
            case FLIGHT     -> name += String.format(" §7(%.1f)", ModFeatures.getSlider(f, "Fly Speed"));
            case FAST_PLACE -> name += String.format(" §7(%dt)", (int) ModFeatures.getSlider(f, "Place Cooldown"));
            case REACH      -> name += String.format(" §7(%.1fb)", ModFeatures.getSlider(f, "Range"));
            case SPIDER     -> name += String.format(" §7(%.1f)", ModFeatures.getSlider(f, "Climb Speed"));
            case HIGH_JUMP  -> name += String.format(" §7(%.1fx)", ModFeatures.getSlider(f, "Jump Power"));
            default -> {}
        }
        return name;
    }
}
