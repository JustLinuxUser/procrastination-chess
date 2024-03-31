package org.example;

public class Hist {
    long move;
    byte ep;
    byte captured;
    byte from_piece;

    byte castle;
    // Castle possibility bitfield
    // @formatter:off
    // 0000 1 1 1 1
    //      | | | |- White, king
    //      | | |- White, queen
    //      | |- Black, king
    //      |- Black, queen
    //  @formatter:on
    public static final byte W_CASTLE_KING = 1;
    public static final byte W_CASTLE_QUEEN = 0b10;
    public static final byte B_CASTLE_KING = 0b100;
    public static final byte B_CASTLE_QUEEN = 0b1000;
}
