package org.example;

import static org.example.Definitions.*;
import static org.example.ChessBoard.*;

public class Generator {
    public static void gen() {
        new_substack();
        for (byte i = 0; i < 64; i++) {
            byte piece = board[i];
            byte piece_type = (byte) (piece & TYPE_MASK);
            byte piece_color = (byte) (piece & COLOR_MASK);
            byte piece_col = (byte) (i % 8);
            byte piece_row = (byte) (i / 8);

            if (piece_type == EMPTY) {
                continue;
            }
            if (piece_color != side) {
                continue;
            }
            if (piece_type == PAWN) {
                if (piece_color == BLACK) {
                    if (piece_col != 0
                            && (board[i - 9] & TYPE_MASK) != EMPTY
                            && (board[i - 9] & COLOR_MASK) == WHITE) {
                        if (!promotion(i, (byte) (i - 9))) {
                            push_move(i, (byte) (i - 9));
                        }
                    }
                    if (piece_col != 7
                            && (board[i - 7] & TYPE_MASK) != EMPTY
                            && (board[i - 7] & COLOR_MASK) == WHITE) {
                        if (!promotion(i, (byte) (i - 7))) {
                            push_move(i, (byte) (i - 7));
                        }
                    }
                    // En passant move generation
                    if (piece_col != 7
                            && ep == i + 1
                            && (board[i - 7] & TYPE_MASK) == EMPTY) { // Ep, no capture
                        push_move(i, (byte) (i - 7), PMove.EP);
                    } else if (piece_col != 7
                            && ep == i + 1
                            && (board[i - 7] & TYPE_MASK) != EMPTY
                            && (board[i - 7] & COLOR_MASK) == WHITE) { // Ep, with capture, super rare :)
                        push_move(i, (byte) (i - 7), PMove.EP | PMove.CAPTURE);
                    }
                    if (piece_col != 0
                            && ep == i - 1
                            && (board[i - 9] & TYPE_MASK) == EMPTY) { // Ep, no capture
                        push_move(i, (byte) (i - 9), PMove.EP);
                    } else if (piece_col != 0
                            && ep == i - 1
                            && (board[i - 9] & TYPE_MASK) != EMPTY
                            && (board[i - 9] & COLOR_MASK) == WHITE) { // Ep, with capture, super rare :)
                        push_move(i, (byte) (i - 9), PMove.EP | PMove.CAPTURE);
                    }

                    // Push pawn
                    if ((board[i - 8] & TYPE_MASK) == EMPTY) {
                        if (!promotion(i, (byte) (i - 8))) {
                            push_move(i, (byte) (i - 8));
                        }
                        // Double push move generation
                        if (piece_row == 6 && (board[i - 16] & TYPE_MASK) == EMPTY) {
                            push_move(i, (byte) (i - 16));
                        }
                    }
                } else {
                    // White pawn
                    if (piece_col != 7
                            && (board[i + 9] & TYPE_MASK) != EMPTY
                            && (board[i + 9] & COLOR_MASK) == BLACK) {
                        if (!promotion(i, (byte) (i + 9))) {
                            push_move(i, (byte) (i + 9));
                        }
                    }
                    if (piece_col != 0
                            && (board[i + 7] & TYPE_MASK) != EMPTY
                            && (board[i + 7] & COLOR_MASK) == BLACK) {
                        if (!promotion(i, (byte) (i + 7))) {
                            push_move(i, (byte) (i + 7));
                        }
                    }

                    // En passant move generation
                    if (piece_col != 0
                            && ep == i - 1
                            && (board[i + 7] & TYPE_MASK) == EMPTY) { // Ep, no capture
                        push_move(i, (byte) (i + 7), PMove.EP);
                    } else if (piece_col != 0
                            && ep == i - 1
                            && (board[i + 7] & TYPE_MASK) != EMPTY
                            && (board[i + 7] & COLOR_MASK) == BLACK) { // Ep, with capture, super rare :)
                        push_move(i, (byte) (i - 7), PMove.EP | PMove.CAPTURE);
                    }
                    if (piece_col != 7
                            && ep == i + 1
                            && (board[i + 9] & TYPE_MASK) == EMPTY) { // Ep, no capture
                        push_move(i, (byte) (i + 9), PMove.EP);
                    } else if (piece_col != 7
                            && ep == i + 1
                            && (board[i + 9] & TYPE_MASK) != EMPTY
                            && (board[i + 9] & COLOR_MASK) == BLACK) { // Ep, with capture, super rare :)
                        push_move(i, (byte) (i + 9), PMove.EP | PMove.CAPTURE);
                    }

                    if ((board[i + 8] & TYPE_MASK) == EMPTY) {
                        if (!promotion(i, (byte) (i + 8))) {
                            push_move(i, (byte) (i + 8));
                        }
                        if (piece_row == 1 && (board[i + 16] & TYPE_MASK) == EMPTY) {
                            push_move(i, (byte) (i + 16));
                        }
                    }
                }
            } else {
                byte idx120 = MAILBOX64[i];
                byte[] offsets = OFFSETS[piece_type];
                for (byte offset : offsets) {
                    byte to;
                    byte idx120_curr = idx120;
                    while (true) {
                        idx120_curr += offset;
                        to = MAILBOX[idx120_curr];
                        if (to == -1) {
                            break;
                        }

                        byte to_piece = board[to];
                        byte to_type = (byte) (to_piece & TYPE_MASK);
                        byte to_color = (byte) (to_piece & COLOR_MASK);

                        if (to_type == EMPTY) {
                            push_move(i, to);
                        } else if (to_color == piece_color) {
                            break;
                        } else {
                            push_move(i, to); // capture
                            break;
                        }
                        if (piece_type == PAWN || piece_type == KNIGHT || piece_type == KING) { // they dont slide
                            break;
                        }
                    }
                }
            }
            if (piece_type == KING) {
                side ^= BLACK;
                if (piece_color == WHITE) {
                    if ((castle & CASTLE_WK) != 0) {
                        if (!attack(F1) && !attack(G1)
                                && (board[F1] & TYPE_MASK) == EMPTY
                                && (board[G1] & TYPE_MASK) == EMPTY) {
                            push_move(E1, G1, PMove.CASTLE_KING);
                        }
                    }
                    if ((castle & CASTLE_WQ) != 0) {
                        if (!attack(D1) && !attack(C1)
                                && (board[D1] & TYPE_MASK) == EMPTY
                                && (board[C1] & TYPE_MASK) == EMPTY
                                && (board[B1] & TYPE_MASK) == EMPTY) {
                            push_move(E1, C1, PMove.CASTLE_QUEEN);
                        }
                    }
                } else {
                    if ((castle & CASTLE_BK) != 0) {
                        if (!attack(F8) && !attack(G8)
                                && (board[F8] & TYPE_MASK) == EMPTY
                                && (board[G8] & TYPE_MASK) == EMPTY) {
                            push_move(E8, G8, PMove.CASTLE_KING);
                        }
                    }
                    if ((castle & CASTLE_BQ) != 0) {
                        if (!attack(D8) && !attack(C8)
                                && (board[D8] & TYPE_MASK) == EMPTY
                                && (board[C8] & TYPE_MASK) == EMPTY
                                && (board[B8] & TYPE_MASK) == EMPTY) {
                            push_move(E8, C8, PMove.CASTLE_QUEEN);
                        }
                    }
                }
                side ^= BLACK;
            }
        }
    }
}
