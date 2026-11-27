package com.andrey.chessduel.chess;

import java.util.ArrayList;
import java.util.List;

/** Полные правила шахмат: рокировка, взятие на проходе, превращение, мат/пат. Клетка 0 = a1, 63 = h8. */
public final class Board {
    public static final int PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;
    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public enum Result { ONGOING, WHITE_WINS, BLACK_WINS, DRAW }

    public final int[] sq = new int[64]; // >0 белые, <0 чёрные
    public boolean whiteToMove = true;
    public final boolean[] castle = new boolean[4]; // WK, WQ, BK, BQ
    public int ep = -1;
    public int halfmove = 0, fullmove = 1;

    private static final int[][] KNIGHT_D = {{1,2},{2,1},{-1,2},{-2,1},{1,-2},{2,-1},{-1,-2},{-2,-1}};
    private static final int[][] KING_D = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    private static final int[][] BISHOP_D = {{1,1},{1,-1},{-1,1},{-1,-1}};
    private static final int[][] ROOK_D = {{1,0},{-1,0},{0,1},{0,-1}};

    public static Board start() { return fromFen(START_FEN); }

    public Board copy() {
        Board b = new Board();
        System.arraycopy(sq, 0, b.sq, 0, 64);
        System.arraycopy(castle, 0, b.castle, 0, 4);
        b.whiteToMove = whiteToMove; b.ep = ep; b.halfmove = halfmove; b.fullmove = fullmove;
        return b;
    }

    // ---------- FEN ----------
    public static Board fromFen(String fen) {
        Board b = new Board();
        String[] parts = fen.trim().split("\\s+");
        String[] ranks = parts[0].split("/");
        for (int i = 0; i < 8; i++) {
            int rank = 7 - i, file = 0;
            for (char c : ranks[i].toCharArray()) {
                if (Character.isDigit(c)) file += c - '0';
                else {
                    int t = "pnbrqk".indexOf(Character.toLowerCase(c)) + 1;
                    b.sq[rank * 8 + file] = Character.isUpperCase(c) ? t : -t;
                    file++;
                }
            }
        }
        b.whiteToMove = parts.length < 2 || parts[1].equals("w");
        String c = parts.length > 2 ? parts[2] : "-";
        b.castle[0] = c.contains("K"); b.castle[1] = c.contains("Q");
        b.castle[2] = c.contains("k"); b.castle[3] = c.contains("q");
        b.ep = (parts.length > 3 && !parts[3].equals("-")) ? (parts[3].charAt(1) - '1') * 8 + (parts[3].charAt(0) - 'a') : -1;
        b.halfmove = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        b.fullmove = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
        return b;
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int f = 0; f < 8; f++) {
                int p = sq[rank * 8 + f];
                if (p == 0) { empty++; continue; }
                if (empty > 0) { sb.append(empty); empty = 0; }
                char ch = "pnbrqk".charAt(Math.abs(p) - 1);
                sb.append(p > 0 ? Character.toUpperCase(ch) : ch);
            }
            if (empty > 0) sb.append(empty);
            if (rank > 0) sb.append('/');
        }
        sb.append(whiteToMove ? " w " : " b ");
        String c = (castle[0] ? "K" : "") + (castle[1] ? "Q" : "") + (castle[2] ? "k" : "") + (castle[3] ? "q" : "");
        sb.append(c.isEmpty() ? "-" : c).append(' ');
        sb.append(ep < 0 ? "-" : "" + (char) ('a' + (ep & 7)) + (char) ('1' + (ep >> 3)));
        sb.append(' ').append(halfmove).append(' ').append(fullmove);
        return sb.toString();
    }

    // ---------- атаки ----------
    private static boolean in(int f, int r) { return f >= 0 && f < 8 && r >= 0 && r < 8; }

    public boolean isAttacked(int s, boolean byWhite) {
        int f = s & 7, r = s >> 3;
        int pr = byWhite ? r - 1 : r + 1;
        for (int df = -1; df <= 1; df += 2)
            if (in(f + df, pr) && sq[pr * 8 + f + df] == (byWhite ? PAWN : -PAWN)) return true;
        for (int[] d : KNIGHT_D)
            if (in(f + d[0], r + d[1]) && sq[(r + d[1]) * 8 + f + d[0]] == (byWhite ? KNIGHT : -KNIGHT)) return true;
        for (int[] d : KING_D)
            if (in(f + d[0], r + d[1]) && sq[(r + d[1]) * 8 + f + d[0]] == (byWhite ? KING : -KING)) return true;
        for (int[] d : BISHOP_D) {
            int nf = f + d[0], nr = r + d[1];
            while (in(nf, nr)) {
                int p = sq[nr * 8 + nf];
                if (p != 0) { if (p == (byWhite ? BISHOP : -BISHOP) || p == (byWhite ? QUEEN : -QUEEN)) return true; break; }
                nf += d[0]; nr += d[1];
            }
        }
        for (int[] d : ROOK_D) {
            int nf = f + d[0], nr = r + d[1];
            while (in(nf, nr)) {
                int p = sq[nr * 8 + nf];
                if (p != 0) { if (p == (byWhite ? ROOK : -ROOK) || p == (byWhite ? QUEEN : -QUEEN)) return true; break; }
                nf += d[0]; nr += d[1];
            }
        }
        return false;
    }

    public boolean inCheck(boolean white) {
        for (int s = 0; s < 64; s++) if (sq[s] == (white ? KING : -KING)) return isAttacked(s, !white);
        return false;
    }

    // ---------- генерация ходов ----------
    private void addPawn(List<Move> out, int from, int to, boolean promo) {
        if (promo) for (int t = QUEEN; t >= KNIGHT; t--) out.add(new Move(from, to, t, false, false, false));
        else out.add(new Move(from, to, 0, false, false, false));
    }

    private void pseudo(List<Move> out, boolean white) {
        for (int s = 0; s < 64; s++) {
            int p = sq[s];
            if (p == 0 || (p > 0) != white) continue;
            int t = Math.abs(p), f = s & 7, r = s >> 3;
            switch (t) {
                case PAWN -> {
                    int dir = white ? 1 : -1, nr = r + dir;
                    if (nr < 0 || nr > 7) break;
                    boolean promo = nr == (white ? 7 : 0);
                    int one = nr * 8 + f;
                    if (sq[one] == 0) {
                        addPawn(out, s, one, promo);
                        if (r == (white ? 1 : 6) && sq[one + dir * 8] == 0)
                            out.add(new Move(s, one + dir * 8, 0, false, false, true));
                    }
                    for (int df = -1; df <= 1; df += 2) {
                        int nf = f + df;
                        if (nf < 0 || nf > 7) continue;
                        int to = nr * 8 + nf;
                        if (sq[to] != 0 && (sq[to] > 0) != white) addPawn(out, s, to, promo);
                        else if (to == ep && sq[to] == 0) out.add(new Move(s, to, 0, true, false, false));
                    }
                }
                case KNIGHT -> step(out, s, white, KNIGHT_D);
                case KING -> {
                    step(out, s, white, KING_D);
                    if (white && s == 4 && !isAttacked(4, false)) {
                        if (castle[0] && sq[5] == 0 && sq[6] == 0 && sq[7] == ROOK && !isAttacked(5, false))
                            out.add(new Move(4, 6, 0, false, true, false));
                        if (castle[1] && sq[1] == 0 && sq[2] == 0 && sq[3] == 0 && sq[0] == ROOK && !isAttacked(3, false))
                            out.add(new Move(4, 2, 0, false, true, false));
                    } else if (!white && s == 60 && !isAttacked(60, true)) {
                        if (castle[2] && sq[61] == 0 && sq[62] == 0 && sq[63] == -ROOK && !isAttacked(61, true))
                            out.add(new Move(60, 62, 0, false, true, false));
                        if (castle[3] && sq[57] == 0 && sq[58] == 0 && sq[59] == 0 && sq[56] == -ROOK && !isAttacked(59, true))
                            out.add(new Move(60, 58, 0, false, true, false));
                    }
                }
                case BISHOP -> slide(out, s, white, BISHOP_D);
                case ROOK -> slide(out, s, white, ROOK_D);
                case QUEEN -> { slide(out, s, white, BISHOP_D); slide(out, s, white, ROOK_D); }
            }
        }
    }

    private void step(List<Move> out, int s, boolean white, int[][] ds) {
        int f = s & 7, r = s >> 3;
        for (int[] d : ds) {
            int nf = f + d[0], nr = r + d[1];
            if (!in(nf, nr)) continue;
            int to = nr * 8 + nf;
            if (sq[to] == 0 || (sq[to] > 0) != white) out.add(new Move(s, to, 0, false, false, false));
        }
    }

    private void slide(List<Move> out, int s, boolean white, int[][] ds) {
        int f = s & 7, r = s >> 3;
        for (int[] d : ds) {
            int nf = f + d[0], nr = r + d[1];
            while (in(nf, nr)) {
                int to = nr * 8 + nf;
                if (sq[to] == 0) out.add(new Move(s, to, 0, false, false, false));
                else { if ((sq[to] > 0) != white) out.add(new Move(s, to, 0, false, false, false)); break; }
                nf += d[0]; nr += d[1];
            }
        }
    }

    public List<Move> legalMoves() {
        List<Move> ps = new ArrayList<>(48), out = new ArrayList<>(40);
        pseudo(ps, whiteToMove);
        boolean mover = whiteToMove;
        for (Move m : ps) {
            Board c = copy();
            c.make(m);
            if (!c.inCheck(mover)) out.add(m);
        }
        return out;
    }

    /** Ищет легальный ход; при превращении без выбора ставит ферзя. */
    public Move find(int from, int to, int promo) {
        for (Move m : legalMoves())
            if (m.from == from && m.to == to && (m.promo == 0 || m.promo == (promo == 0 ? QUEEN : promo))) return m;
        return null;
    }

    public void make(Move m) {
        int p = sq[m.from];
        boolean white = p > 0;
        int captured = sq[m.to];
        halfmove = (Math.abs(p) == PAWN || captured != 0 || m.ep) ? 0 : halfmove + 1;
        sq[m.to] = p;
        sq[m.from] = 0;
        if (m.ep) sq[m.to + (white ? -8 : 8)] = 0;
        if (m.promo != 0) sq[m.to] = white ? m.promo : -m.promo;
        if (m.castle) {
            if (m.to > m.from) { sq[m.from + 1] = sq[m.from + 3]; sq[m.from + 3] = 0; }
            else { sq[m.from - 1] = sq[m.from - 4]; sq[m.from - 4] = 0; }
        }
        if (Math.abs(p) == KING) { if (white) { castle[0] = false; castle[1] = false; } else { castle[2] = false; castle[3] = false; } }
        rights(m.from); rights(m.to);
        ep = m.dbl ? (m.from + m.to) / 2 : -1;
        if (!white) fullmove++;
        whiteToMove = !whiteToMove;
    }

    private void rights(int s) {
        if (s == 0) castle[1] = false; else if (s == 7) castle[0] = false;
        else if (s == 56) castle[3] = false; else if (s == 63) castle[2] = false;
    }

    public Result result() {
        if (legalMoves().isEmpty())
            return inCheck(whiteToMove) ? (whiteToMove ? Result.BLACK_WINS : Result.WHITE_WINS) : Result.DRAW;
        if (halfmove >= 100 || insufficient()) return Result.DRAW;
        return Result.ONGOING;
    }

    private boolean insufficient() {
        int minors = 0;
        for (int p : sq) {
            int t = Math.abs(p);
            if (t == 0 || t == KING) continue;
            if (t == KNIGHT || t == BISHOP) minors++; else return false;
        }
        return minors <= 1;
    }
}
