package com.andrey.chessduel.client;

import com.andrey.chessduel.chess.Board;
import com.andrey.chessduel.chess.Bot;
import com.andrey.chessduel.chess.Move;
import com.andrey.chessduel.net.ActionPacket;
import com.andrey.chessduel.net.DuelStatePacket;
import com.andrey.chessduel.net.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/** Окно партии: доска слева, моб-соперник и статус справа. Состояние целиком приходит с сервера. */
public class ChessScreen extends Screen {
    private DuelStatePacket st;
    private Board board;
    private int selected = -1, cell, boardX, boardY;
    private final Set<Integer> targets = new HashSet<>();

    public ChessScreen(DuelStatePacket st) {
        super(Component.literal("Шахматная дуэль"));
        apply(st);
    }

    public void update(DuelStatePacket st) {
        boolean wasPlaying = this.st != null && this.st.status == 0;
        apply(st);
        selected = -1; targets.clear();
        if (wasPlaying != (st.status == 0)) rebuildWidgets();
    }

    private void apply(DuelStatePacket st) { this.st = st; this.board = Board.fromFen(st.fen); }

    @Override
    protected void init() {
        cell = Mth.clamp(Math.min(this.height - 40, this.width - 190) / 8, 12, 40);
        boardX = 20;
        boardY = (this.height - cell * 8) / 2;
        int px = boardX + cell * 8 + 20;
        int bw = Math.max(80, Math.min(140, this.width - px - 10));
        Component label = (st.status == 0 && !st.spectator) ? Component.literal("Сдаться") : Component.literal("Закрыть");
        addRenderableWidget(Button.builder(label, b -> {
            if (st.status == 0 && !st.spectator) ModNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.RESIGN));
            else onClose();
        }).bounds(px, this.height - 30, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        ChessRenderer.drawBoard(g, board, boardX, boardY, cell, st.flip, selected, targets, st.lastFrom, st.lastTo);

        int px = boardX + cell * 8 + 20, pw = Math.max(80, this.width - px - 10), cx = px + pw / 2;
        g.drawCenteredString(this.font, st.mobName, cx, 14, 0xFFFFFF);
        g.drawCenteredString(this.font, "Ур. " + st.level + "  (≈" + Bot.ELO[st.level] + " Эло)", cx, 26, 0xFFD27F);

        Entity ent = (this.minecraft.level != null && st.mobEntityId >= 0) ? this.minecraft.level.getEntity(st.mobEntityId) : null;
        if (ent instanceof LivingEntity le) {
            int mobBottom = Math.min(this.height - 110, 190);
            int scale = (int) Mth.clamp(70f / Math.max(le.getBbHeight(), le.getBbWidth()), 4f, 70f);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, mobBottom, scale,
                    cx - mx, mobBottom - 50 - my, le);
        } else {
            g.drawCenteredString(this.font, "(соперник вне зоны видимости)", cx, 80, 0x888888);
        }

        g.drawCenteredString(this.font, statusText(), cx, this.height - 60, statusColor());
        super.render(g, mx, my, pt);
    }

    private Component statusText() {
        String s = switch (st.status) {
            case 1 -> st.spectator ? "Победил игрок" : "Победа!";
            case 2 -> st.spectator ? "Победил " + st.mobName : "Поражение";
            case 3 -> "Ничья";
            default -> st.spectator ? (board.whiteToMove ? "Ход игрока" : "Ход соперника")
                                    : (board.whiteToMove ? "Ваш ход" : "Соперник думает...");
        };
        if (st.status == 0 && board.inCheck(board.whiteToMove)) s += "  — ШАХ!";
        return Component.literal(s);
    }

    private int statusColor() {
        return st.status == 1 ? 0x7CFC00 : st.status == 2 ? 0xFF5555 : st.status == 3 ? 0xDDDDDD : 0xFFFFFF;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && !st.spectator && st.status == 0 && board.whiteToMove) {
            int col = (int) Math.floor((mx - boardX) / cell), row = (int) Math.floor((my - boardY) / cell);
            if (col >= 0 && col < 8 && row >= 0 && row < 8) {
                int s = (7 - row) * 8 + col; // игрок всегда за белых, доска не перевёрнута
                if (selected >= 0 && targets.contains(s)) {
                    ModNetwork.CHANNEL.sendToServer(new com.andrey.chessduel.net.MovePacket(selected, s));
                    selected = -1; targets.clear();
                } else if (board.sq[s] > 0) {
                    selected = s; targets.clear();
                    for (Move m : board.legalMoves()) if (m.from == s) targets.add(m.to);
                } else { selected = -1; targets.clear(); }
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean shouldCloseOnEsc() { return st.spectator || st.status != 0; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        if (st.spectator) ModNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.LEAVE_SPECTATE));
    }
}
