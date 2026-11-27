package com.andrey.chessduel.client;

import com.andrey.chessduel.chess.Board;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Простое окно настроек: тема доски и стиль фигур, с предпросмотром. Открывается клавишей J. */
public class SettingsScreen extends Screen {
    private final Screen parent;
    private final Board preview = Board.start();
    private Button themeBtn, piecesBtn;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Шахматная дуэль — настройки"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2, y = this.height / 2 - 60;
        themeBtn = addRenderableWidget(Button.builder(themeLabel(), b -> {
            ClientSettings.THEME.set((ClientSettings.THEME.get() + 1) % ChessRenderer.THEME_NAMES.length);
            ClientSettings.THEME.save();
            b.setMessage(themeLabel());
        }).bounds(cx - 100, y, 200, 20).build());
        piecesBtn = addRenderableWidget(Button.builder(piecesLabel(), b -> {
            ClientSettings.PIECES.set(1 - ClientSettings.PIECES.get());
            ClientSettings.PIECES.save();
            b.setMessage(piecesLabel());
        }).bounds(cx - 100, y + 24, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(cx - 100, this.height - 30, 200, 20).build());
    }

    private Component themeLabel() {
        return Component.literal("Доска: " + ChessRenderer.THEME_NAMES[ClientSettings.THEME.get()]);
    }

    private Component piecesLabel() {
        return Component.literal("Фигуры: " + (ClientSettings.PIECES.get() == 0 ? "символы" : "буквы"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        int cell = Math.max(10, Math.min(22, (this.height - this.height / 2 - 20 - 50) / 8));
        ChessRenderer.drawBoard(g, preview, this.width / 2 - cell * 4, this.height / 2 - 10, cell, false, -1, null, -1, -1);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }
}
