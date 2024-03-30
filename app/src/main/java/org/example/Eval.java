package org.example;

import static org.example.ChessBoard.*;
import static org.example.Definitions.*;
import static org.example.EvalDefinitions.*;

public class Eval {
    static int mg_table[][] = new int[12][64];
    static int eg_table[][] = new int[12][64];
    static int gamephase_inc_table[] = { 0, 1, 1, 2, 4, 0 };

    static int flip(int sq) {
        return sq ^ 56;
    }

    public static void init_tables() {
        int piece, piece_type, sq;
        for (piece_type = PAWN, piece = PAWN; piece_type <= QUEEN; piece++, piece_type++) {
            for (sq = 0; sq < 64; sq++) {
                mg_table[piece][sq] = mg_value[piece_type] + mg_pesto_table[piece_type][sq];
                eg_table[piece][sq] = eg_value[piece_type] + eg_pesto_table[piece_type][sq];
                mg_table[piece + 6][sq] = mg_value[piece_type] + mg_pesto_table[piece_type][flip(sq)];
                eg_table[piece + 6][sq] = eg_value[piece_type] + eg_pesto_table[piece_type][flip(sq)];
            }
        }
    }

    static int eval() {
        int mg[] = { 0, 0 };
        int eg[] = { 0, 0 };
        int game_phase = 0;

        /* evaluate each piece */
        for (int sq = 0; sq < 64; ++sq) {
            int piece = board[sq];
            int piece_color = piece & COLOR_MASK;
            int piece_type = piece & TYPE_MASK;
            int color_idx = piece_color >> 3;
            int type_idx = piece_type;
            if (piece_color == WHITE) {
                type_idx += 6;
            }
            if (piece_type != EMPTY) {
                mg[color_idx] += mg_table[type_idx][sq];
                eg[color_idx] += eg_table[type_idx][sq];
                game_phase += gamephase_inc_table[piece_type];
            }
        }

        /* tapered eval */
        int side_color_idx = side >> 3;
        int mg_score = mg[side_color_idx] - mg[side_color_idx ^ 1];
        int eg_score = eg[side_color_idx] - eg[side_color_idx ^ 1];
        int mg_phase = game_phase;
        if (mg_phase > 24)
            mg_phase = 24; /* in case of early promotion */
        int eg_phase = 24 - mg_phase;
        return (mg_score * mg_phase + eg_score * eg_phase) / 24;
    }
}
