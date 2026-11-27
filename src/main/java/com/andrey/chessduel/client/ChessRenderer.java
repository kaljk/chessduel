package com.andrey.chessduel.client;

import com.andrey.chessduel.chess.Board;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Set;

/** Рисует доску и фигуры. Используется и в окне партии, и в окне настроек (предпросмотр). */
public final class ChessRenderer {
    public static final String[] THEME_NAMES = {"Классика", "Дерево", "Лёд", "Незер", "Эндер"};
    private static final int[][] THEMES = { // светлая, тёмная клетка
            {0xFFEEEED2, 0xFF769656}, {0xFFF0D9B5, 0xFFB58863}, {0xFFDCEBF5, 0xFF6A9BC3},
            {0xFF8E5A5A, 0xFF4A1F1F}, {0xFFC9B6E4, 0xFF4B2E83}};
    private static final String[] SYMBOLS = {"", "\u265F", "\u265E", "\u265D", "\u265C", "\u265B", "\u265A"};
    private static final String LETTERS = "PNBRQK";
    private static final int LAST = 0x88F5E663, SELECTED = 0xAA4FC3F7, DOT = 0xAA1B5E20;

    private ChessRenderer() {}

    public static void drawBoard(GuiGraphics g, Board b, int x, int y, int cell, boolean flip,
                                 int selected, Set<Integer> targets, int lastFrom, int lastTo) {
        int[] theme = THEMES[ClientSettings.THEME.get()];
        g.fill(x - 3, y - 3, x + cell * 8 + 3, y + cell * 8 + 3, 0xFF2B1B0E); // рамка
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int file = flip ? 7 - col : col, rank = flip ? row : 7 - row, s = rank * 8 + file;
                int cx = x + col * cell, cy = y + row * cell;
                g.fill(cx, cy, cx + cell, cy + cell, ((file + rank) & 1) == 1 ? theme[0] : theme[1]);
                if (s == lastFrom || s == lastTo) g.fill(cx, cy, cx + cell, cy + cell, LAST);
                if (s == selected) g.fill(cx, cy, cx + cell, cy + cell, SELECTED);
                drawPiece(g, b.sq[s], cx, cy, cell);
                if (targets != null && targets.contains(s)) {
                    int m = cell / 3;
                    if (b.sq[s] == 0) g.fill(cx + m, cy + m, cx + cell - m, cy + cell - m, DOT);
                    else { // взятие: рамка
                        g.fill(cx, cy, cx + cell, cy + 2, DOT); g.fill(cx, cy + cell - 2, cx + cell, cy + cell, DOT);
                        g.fill(cx, cy, cx + 2, cy + cell, DOT); g.fill(cx + cell - 2, cy, cx + cell, cy + cell, DOT);
                    }
                }
            }
        }
    }

    private static void drawPiece(GuiGraphics g, int p, int cx, int cy, int cell) {
        if (p == 0) return;
        int t = Math.abs(p);
        String txt = ClientSettings.PIECES.get() == 0 ? SYMBOLS[t] : String.valueOf(LETTERS.charAt(t - 1));
        int fill = p > 0 ? 0xFFFFFFFF : 0xFF1A1A1A, outline = p > 0 ? 0xFF000000 : 0xFFFFFFFF;
        Font font = Minecraft.getInstance().font;
        float sc = cell * 0.85f / 9f;
        int w = font.width(txt);
        g.pose().pushPose();
        g.pose().translate(cx + cell / 2f, cy + cell / 2f, 0);
        g.pose().scale(sc, sc, 1f);
        float ox = -w / 2f, oy = -4.5f;
        int[][] o = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : o) g.drawString(font, txt, (int) (ox + d[0]), (int) (oy + d[1]), outline, false);
        g.drawString(font, txt, (int) ox, (int) oy, fill, false);
        g.pose().popPose();
    }
}
