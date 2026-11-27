package com.andrey.chessduel.chess;

public final class Move {
    public final int from, to, promo; // promo: 0 или тип фигуры (2..5)
    public final boolean ep, castle, dbl;

    public Move(int from, int to, int promo, boolean ep, boolean castle, boolean dbl) {
        this.from = from; this.to = to; this.promo = promo;
        this.ep = ep; this.castle = castle; this.dbl = dbl;
    }

    @Override
    public String toString() {
        String s = "" + (char) ('a' + (from & 7)) + (char) ('1' + (from >> 3))
                + (char) ('a' + (to & 7)) + (char) ('1' + (to >> 3));
        return promo != 0 ? s + "?nbrq".charAt(promo - 1) : s;
    }
}
