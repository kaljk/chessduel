package com.andrey.chessduel.chess;

import java.util.List;
import java.util.Random;

/** Лёгкий бот: поиск на несколько ходов + шум оценки + шанс "зевка". Сила настраивается по уровню 1..4. */
public final class Bot {
    public static final int[] ELO = {0, 300, 500, 800, 1000};
    private static final int INF = 1_000_000, MATE = 100_000;
    private static final int[] VAL = {0, 100, 320, 330, 500, 900, 0};

    private final int depth, noise;
    private final double blunder;
    private final Random rnd;

    public Bot(int depth, int noise, double blunder, Random rnd) {
        this.depth = depth; this.noise = noise; this.blunder = blunder; this.rnd = rnd;
    }

    public static Bot forLevel(int level, Random rnd) {
        return switch (level) {
            case 1 -> new Bot(1, 250, 0.40, rnd);
            case 2 -> new Bot(2, 200, 0.22, rnd);
            case 3 -> new Bot(2, 60, 0.06, rnd);
            default -> new Bot(3, 20, 0.015, rnd);
        };
    }

    public Move choose(Board b) {
        List<Move> moves = b.legalMoves();
        if (moves.isEmpty()) return null;
        if (rnd.nextDouble() < blunder) return moves.get(rnd.nextInt(moves.size()));
        Move best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Move m : moves) {
            Board c = b.copy();
            c.make(m);
            int score = -negamax(c, depth - 1, -INF, INF, 1) + (noise > 0 ? rnd.nextInt(2 * noise + 1) - noise : 0);
            if (score > bestScore) { bestScore = score; best = m; }
        }
        return best;
    }

    private int negamax(Board b, int depth, int alpha, int beta, int ply) {
        if (depth == 0) return eval(b);
        List<Move> ms = b.legalMoves();
        if (ms.isEmpty()) return b.inCheck(b.whiteToMove) ? -MATE + ply : 0;
        if (b.halfmove >= 100) return 0;
        ms.sort((x, y) -> Integer.compare(Math.abs(b.sq[y.to]) * 10 - Math.abs(b.sq[y.from]),
                                          Math.abs(b.sq[x.to]) * 10 - Math.abs(b.sq[x.from])));
        for (Move m : ms) {
            Board c = b.copy();
            c.make(m);
            int s = -negamax(c, depth - 1, -beta, -alpha, ply + 1);
            if (s >= beta) return beta;
            if (s > alpha) alpha = s;
        }
        return alpha;
    }

    /** Оценка с точки зрения того, чей ход. */
    static int eval(Board b) {
        int e = 0;
        for (int s = 0; s < 64; s++) {
            int p = b.sq[s];
            if (p == 0) continue;
            int t = Math.abs(p), f = s & 7, r = s >> 3, rr = p > 0 ? r : 7 - r;
            int v = VAL[t];
            int centre = 7 - Math.max(Math.abs(2 * f - 7), Math.abs(2 * r - 7));
            if (t == Board.PAWN) v += rr * 6 + ((f == 3 || f == 4) ? rr * 2 : 0);
            else if (t == Board.KNIGHT || t == Board.BISHOP) v += centre * 3;
            e += p > 0 ? v : -v;
        }
        return b.whiteToMove ? e : -e;
    }
}
