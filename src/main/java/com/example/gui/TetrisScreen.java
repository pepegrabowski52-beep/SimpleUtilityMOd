package com.example.gui;

import com.example.ModFeatures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TetrisScreen extends Screen {

    public enum GameMode { TETRIS, BLOCK_BLAST }
    private GameMode mode = GameMode.TETRIS;

    // â”€â”€ CLASSIC TETRIS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static final int T_COLS = 10;
    private static final int T_ROWS = 20;

    private static int tetrisHighScore = 0;
    private static int blockBlastHighScore = 0;

    private final int[][] tetrisGrid = new int[T_ROWS][T_COLS];
    private final Random random = new Random();

    // 7 Classic Tetromino shapes
    private static final int[][][][] TETRIS_PIECES = {
        // 1: I (Cyan)
        {
            {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
            {{0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0}},
            {{0,0,0,0},{0,0,0,0},{1,1,1,1},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{0,1,0,0},{0,1,0,0}}
        },
        // 2: J (Blue)
        {
            {{1,0,0},{1,1,1},{0,0,0}},
            {{0,1,1},{0,1,0},{0,1,0}},
            {{0,0,0},{1,1,1},{0,0,1}},
            {{0,1,0},{0,1,0},{1,1,0}}
        },
        // 3: L (Orange)
        {
            {{0,0,1},{1,1,1},{0,0,0}},
            {{0,1,0},{0,1,0},{0,1,1}},
            {{0,0,0},{1,1,1},{1,0,0}},
            {{1,1,0},{0,1,0},{0,1,0}}
        },
        // 4: O (Yellow)
        {
            {{1,1},{1,1}}
        },
        // 5: S (Green)
        {
            {{0,1,1},{1,1,0},{0,0,0}},
            {{0,1,0},{0,1,1},{0,0,1}},
            {{0,0,0},{0,1,1},{1,1,0}},
            {{1,0,0},{1,1,0},{0,1,0}}
        },
        // 6: T (Purple)
        {
            {{0,1,0},{1,1,1},{0,0,0}},
            {{0,1,0},{0,1,1},{0,1,0}},
            {{0,0,0},{1,1,1},{0,1,0}},
            {{0,1,0},{1,1,0},{0,1,0}}
        },
        // 7: Z (Red)
        {
            {{1,1,0},{0,1,1},{0,0,0}},
            {{0,0,1},{0,1,1},{0,1,0}},
            {{0,0,0},{1,1,0},{0,1,1}},
            {{0,1,0},{1,1,0},{1,0,0}}
        }
    };

    private static final int[] PIECE_COLORS = {
        0x000000,   // 0: Empty
        0xFF00E5FF, // 1: Cyan (I)
        0xFF3B82F6, // 2: Blue (J)
        0xFFF97316, // 3: Orange (L)
        0xFFFACC15, // 4: Yellow (O)
        0xFF22C55E, // 5: Green (S)
        0xFFA855F7, // 6: Purple (T)
        0xFFEF4444, // 7: Red (Z)
        0xFFEC4899, // 8: Pink
        0xFF14B8A6  // 9: Teal
    };

    // Tetris state
    private int currentPiece = 0;
    private int currentRot = 0;
    private int pieceX = 0, pieceY = 0;

    private int nextPiece = 0;
    private int holdPiece = -1;
    private boolean canHold = true;

    private int tetrisScore = 0;
    private int linesCleared = 0;
    private int level = 1;
    private boolean tetrisGameOver = false;
    private boolean tetrisPaused = false;
    private long lastFallTime = 0;

    // â”€â”€ BLOCK BLAST (8x8 MOUSE MODE) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static final int BB_GRID_SIZE = 8;
    private final int[][] bbGrid = new int[BB_GRID_SIZE][BB_GRID_SIZE];

    public static class BBShape {
        public final int[][] matrix;
        public final int color;
        public boolean used;

        public BBShape(int[][] matrix, int color) {
            this.matrix = matrix;
            this.color = color;
            this.used = false;
        }
    }

    // Variety of Block Blast Shapes
    private static final int[][][] BB_PRESETS = {
        {{1}},
        {{1,1}},
        {{1},{1}},
        {{1,1,1}},
        {{1},{1},{1}},
        {{1,1},{1,1}},
        {{1,1,1},{1,1,1},{1,1,1}},
        {{1,1,1,1}},
        {{1},{1},{1},{1}},
        {{1,1,1,1,1}},
        {{1},{1},{1},{1},{1}},
        {{1,0},{1,1}},
        {{0,1},{1,1}},
        {{1,1},{1,0}},
        {{1,1},{0,1}},
        {{1,0,0},{1,0,0},{1,1,1}},
        {{0,0,1},{0,0,1},{1,1,1}},
        {{1,1,1},{1,0,0},{1,0,0}},
        {{1,1,1},{0,0,1},{0,0,1}},
        {{1,1,1},{0,1,0}},
        {{0,1,0},{1,1,1}},
        {{1,0},{1,1},{1,0}},
        {{0,1},{1,1},{0,1}},
        {{0,1,0},{1,1,1},{0,1,0}}
    };

    private final List<BBShape> bbTray = new ArrayList<>();
    private int bbSelectedShape = -1;
    private int bbScore = 0;
    private int bbCombo = 0;
    private boolean bbGameOver = false;
    private long bbLastClearTime = 0;
    private String bbComboText = "";

    private final Screen parent;

    public TetrisScreen(Screen parent) {
        super(Text.literal("CS2 Arcade Games"));
        this.parent = parent;
        initTetris();
        initBlockBlast();
    }

    public TetrisScreen() {
        this(null);
    }

    @Override
    protected void applyBlur(float delta) {}

    // =========================================================================
    // TETRIS LOGIC
    // =========================================================================
    private void initTetris() {
        for (int r = 0; r < T_ROWS; r++) {
            for (int c = 0; c < T_COLS; c++) {
                tetrisGrid[r][c] = 0;
            }
        }
        tetrisScore = 0;
        linesCleared = 0;
        level = 1;
        tetrisGameOver = false;
        tetrisPaused = false;
        holdPiece = -1;
        canHold = true;

        nextPiece = random.nextInt(7);
        spawnTetrisPiece();
        lastFallTime = System.currentTimeMillis();
    }

    private void spawnTetrisPiece() {
        currentPiece = nextPiece;
        nextPiece = random.nextInt(7);
        currentRot = 0;
        int[][] shape = getTetrisShape(currentPiece, currentRot);
        pieceX = T_COLS / 2 - shape[0].length / 2;
        pieceY = 0;
        canHold = true;

        if (collidesTetris(pieceX, pieceY, currentPiece, currentRot)) {
            tetrisGameOver = true;
            if (tetrisScore > tetrisHighScore) tetrisHighScore = tetrisScore;
        }
    }

    private int[][] getTetrisShape(int type, int rot) {
        int[][][] orientations = TETRIS_PIECES[type];
        return orientations[rot % orientations.length];
    }

    private boolean collidesTetris(int px, int py, int type, int rot) {
        int[][] shape = getTetrisShape(type, rot);
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int gx = px + c;
                    int gy = py + r;
                    if (gx < 0 || gx >= T_COLS || gy >= T_ROWS) return true;
                    if (gy >= 0 && tetrisGrid[gy][gx] != 0) return true;
                }
            }
        }
        return false;
    }

    private void lockTetrisPiece() {
        int[][] shape = getTetrisShape(currentPiece, currentRot);
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int gx = pieceX + c;
                    int gy = pieceY + r;
                    if (gy >= 0 && gy < T_ROWS && gx >= 0 && gx < T_COLS) {
                        tetrisGrid[gy][gx] = currentPiece + 1;
                    }
                }
            }
        }

        int cleared = 0;
        for (int r = T_ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < T_COLS; c++) {
                if (tetrisGrid[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                for (int rowAbove = r; rowAbove > 0; rowAbove--) {
                    System.arraycopy(tetrisGrid[rowAbove - 1], 0, tetrisGrid[rowAbove], 0, T_COLS);
                }
                for (int c = 0; c < T_COLS; c++) tetrisGrid[0][c] = 0;
                r++;
            }
        }

        if (cleared > 0) {
            linesCleared += cleared;
            level = 1 + (linesCleared / 10);
            int base = switch (cleared) {
                case 1 -> 100;
                case 2 -> 300;
                case 3 -> 500;
                case 4 -> 800;
                default -> cleared * 200;
            };
            tetrisScore += base * level;
            if (tetrisScore > tetrisHighScore) tetrisHighScore = tetrisScore;
        }

        spawnTetrisPiece();
    }

    private void hardDropTetris() {
        if (tetrisGameOver || tetrisPaused) return;
        while (!collidesTetris(pieceX, pieceY + 1, currentPiece, currentRot)) {
            pieceY++;
            tetrisScore += 2;
        }
        lockTetrisPiece();
    }

    private void holdTetris() {
        if (!canHold || tetrisGameOver || tetrisPaused) return;
        if (holdPiece == -1) {
            holdPiece = currentPiece;
            spawnTetrisPiece();
        } else {
            int temp = holdPiece;
            holdPiece = currentPiece;
            currentPiece = temp;
            currentRot = 0;
            int[][] shape = getTetrisShape(currentPiece, currentRot);
            pieceX = T_COLS / 2 - shape[0].length / 2;
            pieceY = 0;
        }
        canHold = false;
    }

    private int getGhostTetrisY() {
        int gy = pieceY;
        while (!collidesTetris(pieceX, gy + 1, currentPiece, currentRot)) {
            gy++;
        }
        return gy;
    }

    // =========================================================================
    // BLOCK BLAST LOGIC (MOUSE)
    // =========================================================================
    private void initBlockBlast() {
        for (int r = 0; r < BB_GRID_SIZE; r++) {
            for (int c = 0; c < BB_GRID_SIZE; c++) {
                bbGrid[r][c] = 0;
            }
        }
        bbScore = 0;
        bbCombo = 0;
        bbGameOver = false;
        bbSelectedShape = -1;
        bbComboText = "";
        spawnBBTray();
    }

    private void spawnBBTray() {
        bbTray.clear();
        for (int i = 0; i < 3; i++) {
            int[][] preset = BB_PRESETS[random.nextInt(BB_PRESETS.length)];
            int color = 1 + random.nextInt(PIECE_COLORS.length - 1);
            bbTray.add(new BBShape(preset, color));
        }
        bbSelectedShape = -1;
        checkBBGameOver();
    }

    private boolean canPlaceBBShape(BBShape shape, int gridR, int gridC) {
        if (shape == null || shape.used) return false;
        int[][] m = shape.matrix;
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int gr = gridR + r;
                    int gc = gridC + c;
                    if (gr < 0 || gr >= BB_GRID_SIZE || gc < 0 || gc >= BB_GRID_SIZE) return false;
                    if (bbGrid[gr][gc] != 0) return false;
                }
            }
        }
        return true;
    }

    private boolean canPlaceShapeAnywhere(BBShape shape) {
        if (shape == null || shape.used) return false;
        for (int r = 0; r < BB_GRID_SIZE; r++) {
            for (int c = 0; c < BB_GRID_SIZE; c++) {
                if (canPlaceBBShape(shape, r, c)) return true;
            }
        }
        return false;
    }

    private void checkBBGameOver() {
        boolean anyPlayable = false;
        for (BBShape s : bbTray) {
            if (!s.used && canPlaceShapeAnywhere(s)) {
                anyPlayable = true;
                break;
            }
        }
        if (!anyPlayable && !bbTray.isEmpty()) {
            boolean allUsed = true;
            for (BBShape s : bbTray) if (!s.used) { allUsed = false; break; }
            if (allUsed) {
                spawnBBTray();
            } else {
                bbGameOver = true;
                if (bbScore > blockBlastHighScore) blockBlastHighScore = bbScore;
            }
        }
    }

    private void placeBBShape(int shapeIndex, int gridR, int gridC) {
        if (shapeIndex < 0 || shapeIndex >= bbTray.size()) return;
        BBShape shape = bbTray.get(shapeIndex);
        if (shape.used || !canPlaceBBShape(shape, gridR, gridC)) return;

        int blockCount = 0;
        int[][] m = shape.matrix;
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    bbGrid[gridR + r][gridC + c] = shape.color;
                    blockCount++;
                }
            }
        }
        shape.used = true;
        bbSelectedShape = -1;
        bbScore += blockCount * 10;

        List<Integer> fullRows = new ArrayList<>();
        for (int r = 0; r < BB_GRID_SIZE; r++) {
            boolean full = true;
            for (int c = 0; c < BB_GRID_SIZE; c++) {
                if (bbGrid[r][c] == 0) { full = false; break; }
            }
            if (full) fullRows.add(r);
        }

        List<Integer> fullCols = new ArrayList<>();
        for (int c = 0; c < BB_GRID_SIZE; c++) {
            boolean full = true;
            for (int r = 0; r < BB_GRID_SIZE; r++) {
                if (bbGrid[r][c] == 0) { full = false; break; }
            }
            if (full) fullCols.add(c);
        }

        int totalCleared = fullRows.size() + fullCols.size();
        if (totalCleared > 0) {
            bbCombo++;
            int comboMultiplier = Math.max(1, bbCombo);
            bbScore += (totalCleared * 100) * comboMultiplier;
            bbComboText = "COMBO x" + bbCombo + "! +" + ((totalCleared * 100) * comboMultiplier);
            bbLastClearTime = System.currentTimeMillis();

            for (int r : fullRows) {
                for (int c = 0; c < BB_GRID_SIZE; c++) bbGrid[r][c] = 0;
            }
            for (int c : fullCols) {
                for (int r = 0; r < BB_GRID_SIZE; r++) bbGrid[r][c] = 0;
            }
        } else {
            bbCombo = 0;
        }

        if (bbScore > blockBlastHighScore) blockBlastHighScore = bbScore;

        boolean allUsed = true;
        for (BBShape s : bbTray) if (!s.used) { allUsed = false; break; }
        if (allUsed) spawnBBTray();
        else checkBBGameOver();
    }

    // =========================================================================
    // MAIN RENDER
    // =========================================================================
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModFeatures.Theme theme = ModFeatures.getTheme();

        // 1. Dark background
        ctx.fill(0, 0, this.width, this.height, 0xFA0B0C12);

        // 2. Top Mode Switcher Tabs
        int topBarY = 6;
        int tabW = 110, tabH = 18;
        int t1X = this.width / 2 - tabW - 4;
        int t2X = this.width / 2 + 4;

        boolean hovT1 = mouseX >= t1X && mouseX <= t1X + tabW && mouseY >= topBarY && mouseY <= topBarY + tabH;
        boolean hovT2 = mouseX >= t2X && mouseX <= t2X + tabW && mouseY >= topBarY && mouseY <= topBarY + tabH;

        ctx.fill(t1X, topBarY, t1X + tabW, topBarY + tabH, mode == GameMode.TETRIS ? theme.color : (hovT1 ? 0xFF222638 : 0xFF141620));
        ctx.drawText(textRenderer, "ðŸ•¹ï¸ Classic Tetris", t1X + 10, topBarY + 5, mode == GameMode.TETRIS ? 0xFFFFFFFF : 0xFFAAAAAA, false);

        ctx.fill(t2X, topBarY, t2X + tabW, topBarY + tabH, mode == GameMode.BLOCK_BLAST ? theme.color : (hovT2 ? 0xFF222638 : 0xFF141620));
        ctx.drawText(textRenderer, "ðŸ’¥ Block Blast", t2X + 16, topBarY + 5, mode == GameMode.BLOCK_BLAST ? 0xFFFFFFFF : 0xFFAAAAAA, false);

        int exitX = this.width - 24, exitY = 6;
        boolean hovExit = mouseX >= exitX && mouseX <= exitX + 18 && mouseY >= exitY && mouseY <= exitY + 18;
        ctx.fill(exitX, exitY, exitX + 18, exitY + 18, hovExit ? 0xFFE11D48 : 0xFF1E212E);
        ctx.drawText(textRenderer, "âœ•", exitX + 5, exitY + 5, 0xFFFFFFFF, false);

        if (mode == GameMode.TETRIS) {
            renderTetris(ctx, mouseX, mouseY, theme);
        } else {
            renderBlockBlast(ctx, mouseX, mouseY, theme);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // =========================================================================
    // RENDER: TETRIS (DYNAMICALLY SCALED)
    // =========================================================================
    private void renderTetris(DrawContext ctx, int mouseX, int mouseY, ModFeatures.Theme theme) {
        int blockSize = Math.max(7, Math.min(12, (this.height - 70) / T_ROWS));

        int boardW = T_COLS * blockSize;
        int boardH = T_ROWS * blockSize;
        int boardX = this.width / 2 - boardW / 2;
        int boardY = this.height / 2 - boardH / 2 + 12;

        ctx.fill(boardX - 2, boardY - 2, boardX + boardW + 2, boardY + boardH + 2, 0xFF222636);
        ctx.fill(boardX - 1, boardY - 1, boardX + boardW + 1, boardY + boardH + 1, 0xFF0D0E16);
        ctx.fill(boardX - 1, boardY - 1, boardX + boardW + 1, boardY + 1, theme.color);

        for (int r = 0; r < T_ROWS; r++) {
            for (int c = 0; c < T_COLS; c++) {
                int bx = boardX + c * blockSize;
                int by = boardY + r * blockSize;
                ctx.fill(bx, by, bx + blockSize, by + blockSize, (r + c) % 2 == 0 ? 0xFF12141F : 0xFF0F111B);

                int cell = tetrisGrid[r][c];
                if (cell > 0) {
                    renderBlock(ctx, bx, by, blockSize, PIECE_COLORS[cell]);
                }
            }
        }

        if (!tetrisGameOver && !tetrisPaused) {
            int ghostY = getGhostTetrisY();
            int[][] shape = getTetrisShape(currentPiece, currentRot);
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int bx = boardX + (pieceX + c) * blockSize;
                        int by = boardY + (ghostY + r) * blockSize;
                        ctx.fill(bx + 1, by + 1, bx + blockSize - 1, by + blockSize - 1, 0x2AFFFFFF);
                    }
                }
            }
        }

        if (!tetrisGameOver && !tetrisPaused) {
            int[][] shape = getTetrisShape(currentPiece, currentRot);
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int bx = boardX + (pieceX + c) * blockSize;
                        int by = boardY + (pieceY + r) * blockSize;
                        renderBlock(ctx, bx, by, blockSize, PIECE_COLORS[currentPiece + 1]);
                    }
                }
            }
        }

        int sideW = 68;
        int leftX = boardX - sideW - 8;
        int leftY = boardY;

        ctx.fill(leftX - 1, leftY - 1, leftX + sideW + 1, leftY + 50, 0xFF222636);
        ctx.fill(leftX, leftY, leftX + sideW, leftY + 49, 0xFF12141F);
        ctx.drawText(textRenderer, "HOLD", leftX + 22, leftY + 4, 0xFFAAAAAA, false);
        if (holdPiece >= 0) renderMiniPiece(ctx, leftX + 18, leftY + 16, holdPiece, 7);

        int ctrlY = leftY + 56;
        ctx.fill(leftX - 1, ctrlY - 1, leftX + sideW + 1, ctrlY + 96, 0xFF222636);
        ctx.fill(leftX, ctrlY, leftX + sideW, ctrlY + 95, 0xFF12141F);
        ctx.drawText(textRenderer, "KEYS", leftX + 20, ctrlY + 4, theme.color, true);
        ctx.drawText(textRenderer, "â† â†’ : Move", leftX + 4, ctrlY + 16, 0xFF777788, false);
        ctx.drawText(textRenderer, "â†‘ W : Turn", leftX + 4, ctrlY + 28, 0xFF777788, false);
        ctx.drawText(textRenderer, "â†“ S : Soft", leftX + 4, ctrlY + 40, 0xFF777788, false);
        ctx.drawText(textRenderer, "Space: Drop", leftX + 4, ctrlY + 52, 0xFF777788, false);
        ctx.drawText(textRenderer, "Shift: Hold", leftX + 4, ctrlY + 64, 0xFF777788, false);
        ctx.drawText(textRenderer, "R : Reset", leftX + 4, ctrlY + 76, 0xFF777788, false);

        int rightX = boardX + boardW + 8;
        int rightY = boardY;

        ctx.fill(rightX - 1, rightY - 1, rightX + sideW + 1, rightY + 50, 0xFF222636);
        ctx.fill(rightX, rightY, rightX + sideW, rightY + 49, 0xFF12141F);
        ctx.drawText(textRenderer, "NEXT", rightX + 22, rightY + 4, 0xFFAAAAAA, false);
        renderMiniPiece(ctx, rightX + 18, rightY + 16, nextPiece, 7);

        int statY = rightY + 56;
        ctx.fill(rightX - 1, statY - 1, rightX + sideW + 1, statY + 96, 0xFF222636);
        ctx.fill(rightX, statY, rightX + sideW, statY + 95, 0xFF12141F);
        ctx.drawText(textRenderer, "STATS", rightX + 18, statY + 4, theme.color, true);
        ctx.drawText(textRenderer, "Score:", rightX + 4, statY + 16, 0xFF777788, false);
        ctx.drawText(textRenderer, String.valueOf(tetrisScore), rightX + 4, statY + 26, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, "Best:", rightX + 4, statY + 40, 0xFF777788, false);
        ctx.drawText(textRenderer, String.valueOf(tetrisHighScore), rightX + 4, statY + 50, 0xFFFACC15, true);
        ctx.drawText(textRenderer, "Lines: " + linesCleared, rightX + 4, statY + 64, 0xFF00E5FF, false);
        ctx.drawText(textRenderer, "Level: " + level, rightX + 4, statY + 76, theme.color, false);

        if (tetrisGameOver) {
            ctx.fill(boardX, boardY + boardH / 2 - 20, boardX + boardW, boardY + boardH / 2 + 20, 0xE08B0000);
            ctx.drawText(textRenderer, "GAME OVER", boardX + (boardW - textRenderer.getWidth("GAME OVER")) / 2, boardY + boardH / 2 - 12, 0xFFFFFFFF, true);
            ctx.drawText(textRenderer, "Press 'R'", boardX + (boardW - textRenderer.getWidth("Press 'R'")) / 2, boardY + boardH / 2 + 2, 0xFFFFAA00, false);
        }

        long now = System.currentTimeMillis();
        long interval = Math.max(70, 600 - (level * 45));
        if (!tetrisGameOver && !tetrisPaused && now - lastFallTime > interval) {
            if (!collidesTetris(pieceX, pieceY + 1, currentPiece, currentRot)) {
                pieceY++;
            } else {
                lockTetrisPiece();
            }
            lastFallTime = now;
        }
    }

    // =========================================================================
    // RENDER: BLOCK BLAST (MOUSE MODE)
    // =========================================================================
    private void renderBlockBlast(DrawContext ctx, int mouseX, int mouseY, ModFeatures.Theme theme) {
        int cellSize = Math.max(14, Math.min(22, (this.height - 110) / BB_GRID_SIZE));
        int boardW = BB_GRID_SIZE * cellSize;
        int boardH = BB_GRID_SIZE * cellSize;

        int boardX = this.width / 2 - boardW / 2;
        int boardY = 36;

        ctx.fill(boardX - 3, boardY - 3, boardX + boardW + 3, boardY + boardH + 3, 0xFF242838);
        ctx.fill(boardX - 1, boardY - 1, boardX + boardW + 1, boardY + boardH + 1, 0xFF0E0F18);
        ctx.fill(boardX - 1, boardY - 1, boardX + boardW + 1, boardY + 1, theme.color);

        for (int r = 0; r < BB_GRID_SIZE; r++) {
            for (int c = 0; c < BB_GRID_SIZE; c++) {
                int bx = boardX + c * cellSize;
                int by = boardY + r * cellSize;
                ctx.fill(bx, by, bx + cellSize, by + cellSize, (r + c) % 2 == 0 ? 0xFF141624 : 0xFF10121C);

                int cell = bbGrid[r][c];
                if (cell > 0) {
                    renderBlock(ctx, bx, by, cellSize, PIECE_COLORS[cell]);
                }
            }
        }

        int hoverCol = (mouseX - boardX) / cellSize;
        int hoverRow = (mouseY - boardY) / cellSize;
        if (bbSelectedShape >= 0 && bbSelectedShape < bbTray.size() && !bbGameOver) {
            BBShape shape = bbTray.get(bbSelectedShape);
            if (!shape.used) {
                if (hoverCol >= 0 && hoverCol < BB_GRID_SIZE && hoverRow >= 0 && hoverRow < BB_GRID_SIZE) {
                    boolean valid = canPlaceBBShape(shape, hoverRow, hoverCol);
                    int[][] m = shape.matrix;
                    for (int r = 0; r < m.length; r++) {
                        for (int c = 0; c < m[r].length; c++) {
                            if (m[r][c] != 0) {
                                int gx = hoverCol + c;
                                int gy = hoverRow + r;
                                if (gx >= 0 && gx < BB_GRID_SIZE && gy >= 0 && gy < BB_GRID_SIZE) {
                                    int bx = boardX + gx * cellSize;
                                    int by = boardY + gy * cellSize;
                                    ctx.fill(bx + 1, by + 1, bx + cellSize - 1, by + cellSize - 1, valid ? 0x6022C55E : 0x60EF4444);
                                }
                            }
                        }
                    }
                }
            }
        }

        String scoreStr = "Score: " + bbScore;
        String bestStr = "Best: " + blockBlastHighScore;
        ctx.drawText(textRenderer, scoreStr, boardX, boardY - 12, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, bestStr, boardX + boardW - textRenderer.getWidth(bestStr), boardY - 12, 0xFFFACC15, true);

        long elapsed = System.currentTimeMillis() - bbLastClearTime;
        if (elapsed < 1800 && !bbComboText.isEmpty()) {
            ctx.drawText(textRenderer, bbComboText, boardX + (boardW - textRenderer.getWidth(bbComboText)) / 2, boardY + boardH / 2 - 6, 0xFFFF0055, true);
        }

        int trayY = boardY + boardH + 10;
        int traySlotW = 60;
        int totalTrayW = 3 * traySlotW + 16;
        int trayStartX = this.width / 2 - totalTrayW / 2;

        for (int i = 0; i < bbTray.size(); i++) {
            BBShape shape = bbTray.get(i);
            int slotX = trayStartX + i * (traySlotW + 8);
            int slotH = 46;

            boolean isSelected = bbSelectedShape == i;
            boolean hovSlot = mouseX >= slotX && mouseX <= slotX + traySlotW && mouseY >= trayY && mouseY <= trayY + slotH;

            int slotBg = shape.used ? 0xFF0D0E16 : (isSelected ? 0xFF2A2E44 : (hovSlot ? 0xFF1C2030 : 0xFF121420));
            ctx.fill(slotX - 1, trayY - 1, slotX + traySlotW + 1, trayY + slotH + 1, isSelected ? theme.color : 0xFF222638);
            ctx.fill(slotX, trayY, slotX + traySlotW, trayY + slotH, slotBg);

            if (!shape.used) {
                int[][] m = shape.matrix;
                int mini = 8;
                int mw = m[0].length * mini;
                int mh = m.length * mini;
                int sx = slotX + (traySlotW - mw) / 2;
                int sy = trayY + (slotH - mh) / 2;

                for (int r = 0; r < m.length; r++) {
                    for (int c = 0; c < m[r].length; c++) {
                        if (m[r][c] != 0) {
                            renderBlock(ctx, sx + c * mini, sy + r * mini, mini, PIECE_COLORS[shape.color]);
                        }
                    }
                }
            } else {
                ctx.drawText(textRenderer, "USED", slotX + 18, trayY + 18, 0xFF444455, false);
            }
        }

        if (bbGameOver) {
            ctx.fill(boardX, boardY + boardH / 2 - 24, boardX + boardW, boardY + boardH / 2 + 24, 0xEE8B0000);
            String go = "NO MOVES LEFT!";
            ctx.drawText(textRenderer, go, boardX + (boardW - textRenderer.getWidth(go)) / 2, boardY + boardH / 2 - 14, 0xFFFFFFFF, true);
            String rText = "Click to Play Again";
            ctx.drawText(textRenderer, rText, boardX + (boardW - textRenderer.getWidth(rText)) / 2, boardY + boardH / 2 + 2, 0xFFFFAA00, false);
        }
    }

    private void renderBlock(DrawContext ctx, int x, int y, int size, int color) {
        ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, color);
        ctx.fill(x + 1, y + 1, x + size - 1, y + 2, 0x50FFFFFF);
        ctx.fill(x + 1, y + 1, x + 2, y + size - 1, 0x50FFFFFF);
        ctx.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, 0x50000000);
        ctx.fill(x + size - 2, y + 1, x + size - 1, y + size - 1, 0x50000000);
    }

    private void renderMiniPiece(DrawContext ctx, int startX, int startY, int type, int miniSize) {
        int[][] shape = getTetrisShape(type, 0);
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int bx = startX + c * miniSize;
                    int by = startY + r * miniSize;
                    renderBlock(ctx, bx, by, miniSize, PIECE_COLORS[type + 1]);
                }
            }
        }
    }

    // =========================================================================
    // MOUSE HANDLING
    // =========================================================================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int topBarY = 6, tabW = 110, tabH = 18;
        int t1X = this.width / 2 - tabW - 4;
        int t2X = this.width / 2 + 4;
        if (mouseY >= topBarY && mouseY <= topBarY + tabH) {
            if (mouseX >= t1X && mouseX <= t1X + tabW) {
                mode = GameMode.TETRIS;
                return true;
            }
            if (mouseX >= t2X && mouseX <= t2X + tabW) {
                mode = GameMode.BLOCK_BLAST;
                return true;
            }
        }

        int exitX = this.width - 24, exitY = 6;
        if (mouseX >= exitX && mouseX <= exitX + 18 && mouseY >= exitY && mouseY <= exitY + 18) {
            if (this.client != null) this.client.setScreen(parent);
            return true;
        }

        if (mode == GameMode.BLOCK_BLAST) {
            if (bbGameOver) {
                initBlockBlast();
                return true;
            }

            int cellSize = Math.max(14, Math.min(22, (this.height - 110) / BB_GRID_SIZE));
            int boardW = BB_GRID_SIZE * cellSize;
            int boardH = BB_GRID_SIZE * cellSize;
            int boardX = this.width / 2 - boardW / 2;
            int boardY = 36;

            int trayY = boardY + boardH + 10;
            int traySlotW = 60;
            int totalTrayW = 3 * traySlotW + 16;
            int trayStartX = this.width / 2 - totalTrayW / 2;

            for (int i = 0; i < bbTray.size(); i++) {
                int slotX = trayStartX + i * (traySlotW + 8);
                int slotH = 46;
                if (mouseX >= slotX && mouseX <= slotX + traySlotW && mouseY >= trayY && mouseY <= trayY + slotH) {
                    if (!bbTray.get(i).used) {
                        bbSelectedShape = (bbSelectedShape == i) ? -1 : i;
                        return true;
                    }
                }
            }

            int col = (int)((mouseX - boardX) / cellSize);
            int row = (int)((mouseY - boardY) / cellSize);
            if (col >= 0 && col < BB_GRID_SIZE && row >= 0 && row < BB_GRID_SIZE) {
                if (bbSelectedShape >= 0 && bbSelectedShape < bbTray.size()) {
                    placeBBShape(bbSelectedShape, row, col);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // =========================================================================
    // KEYBOARD INPUT
    // =========================================================================
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.client != null) this.client.setScreen(parent);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            if (mode == GameMode.TETRIS) initTetris();
            else initBlockBlast();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_P && mode == GameMode.TETRIS) {
            tetrisPaused = !tetrisPaused;
            return true;
        }

        if (mode == GameMode.TETRIS) {
            if (tetrisGameOver || tetrisPaused) return super.keyPressed(keyCode, scanCode, modifiers);

            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
                if (!collidesTetris(pieceX - 1, pieceY, currentPiece, currentRot)) pieceX--;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
                if (!collidesTetris(pieceX + 1, pieceY, currentPiece, currentRot)) pieceX++;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
                int nextRot = (currentRot + 1) % TETRIS_PIECES[currentPiece].length;
                if (!collidesTetris(pieceX, pieceY, currentPiece, nextRot)) {
                    currentRot = nextRot;
                } else if (!collidesTetris(pieceX - 1, pieceY, currentPiece, nextRot)) {
                    pieceX--;
                    currentRot = nextRot;
                } else if (!collidesTetris(pieceX + 1, pieceY, currentPiece, nextRot)) {
                    pieceX++;
                    currentRot = nextRot;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
                if (!collidesTetris(pieceX, pieceY + 1, currentPiece, currentRot)) {
                    pieceY++;
                    tetrisScore += 1;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_SPACE) {
                hardDropTetris();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_C) {
                holdTetris();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}