package org.example;

public class PMove {
    public static final byte EP = 7;
    public static final byte CAPTURE = 0b1000_000;
    public static final byte CASTLE_QUEEN = 1;
    public static final byte CASTLE_KING = 2;
    public static final byte PROMO_KNIGHT = 3;
    public static final byte PROMO_BSHOP = 4;
    public static final byte PROMO_ROOK = 5;
    public static final byte PROMO_QUEEN = 6;
    public static final byte TYPE_MASK = 0b1111;
    public static final byte PROMO = 0b0001_0000;
    public byte from;
    public byte to;
    // public byte score;
    // @formatter:off
        // 1 1 1 1111
        // |   | |- 1 Castle Queen
        // |   | |- 2 Castle King
        // |   | |- 3 promote knight
        // |   | |- 4 promote bshop
        // |   | |- 5 promote rook
        // |   | |- 6 promote queen
        // |   | |- 7 ep
        // |   |- Promotion 
        // |- Capture
        //@formatter:on
    public byte flags = 0;

    public static long create_move(byte from, byte to) {
        int move = 0;
        move |= to;
        move = move << 8;
        move |= from;
        return move;
    }

    public static long create_move(byte from, byte to, byte flags) {
        long move = flags;
        move = move << 8;
        move |= to;
        move = move << 8;
        move |= from;
        return move;
    }

public static long set_score(long move, int score) {
    move = ((long) score << 32) | (move & 0xffffffffL);
    return move;
}

    public static int get_score(long move) {
        return (int) (move >> 32);
    }

    public static byte get_flags(long move) {
        return (byte) (move >> 16 & 0xffL);
    }

    public static byte get_to(long move) {
        return (byte) (move >> 8 & 0xffL);
    }

    public static byte get_from(long move) {
        return (byte) (move & 0xffL);
    }

    public static String toString(long m, byte side) {
        byte from = PMove.get_from(m);
        byte to = PMove.get_to(m);
        byte flags = PMove.get_flags(m);
        String promo = "";
        if ((flags & PROMO) == PROMO) {
            byte move_type = (byte) (flags & TYPE_MASK);
            if (move_type == PROMO_ROOK) {
                promo += "r";
            } else if (move_type == PROMO_BSHOP) {
                promo += "b";
            } else if (move_type == PROMO_KNIGHT) {
                promo += "n";
            } else if (move_type == PROMO_QUEEN) {
                promo += "q";
            }
        }
        return Notation.from_idx(from) + Notation.from_idx(to) + promo;
    }
}
