package org.example;

import static org.example.ChessBoard.*;
import static org.example.Definitions.BLACK;
import static org.example.Definitions.TYPE_MASK;
import static org.example.Generator.*;

import static org.example.EvalDefinitions.*;

public class Search {

    public static int[][] pv_arr = new int[20][200];
    public static int best_move = 0;
    public static int nodes = 0;
    public static long time = System.currentTimeMillis();
    public static long time_out = 0;

    public static void set_move(int move, int ply) {
        pv_arr[ply][0] = move; // -1, because the depth 0 will never do anything
        for (int i = 0; i < 10; i++) {
            pv_arr[ply][i + 1] = pv_arr[ply + 1][i];
        }
        pv_arr[ply][10] = 0;
    }

    public static void print_pv() {
        System.out.print(get_pv());
    }

    public static String get_pv() {
        String ret = "";
        for (int i = 0; pv_arr[0][i] != 0; i++) {
            ret += PMove.toString(pv_arr[0][i], BLACK) + "  ";
        }
        ret += "\n";
        return ret;
    }

    public static String get_best_move(int depth) {
        return PMove.toString(pv_arr[0][0], ChessBoard.side);
    }

    public static void sort_moves(int[] moves, int ply) {
        for (int i = 0; i < moves.length - 1; i++) {
            int m1_score = PMove.get_score(moves[i]);
            int max = -99999;
            int max_idx = i;
            for (int j = i + 1; j < moves.length; j++) {
                int m2_score = PMove.get_score(moves[j]);
                if (m2_score > max) {
                    max = m2_score;
                    max_idx = j;
                }
            }
            int temp = moves[i];
            moves[i] = moves[max_idx];
            moves[max_idx] = temp;
        }
    }

    public static void score_moves(int[] moves, int ply) {
        for (int i = 0; i < moves.length; i++) {
            int move = moves[i];
            int score = 0;
            int move_type = PMove.get_flags(move);
            if (move_type == PMove.EP) {
                score += 5;
            }
            if ((move & PMove.CAPTURE) != 0) {
                score += 10;
                int from = PMove.get_from(move);
                int to = PMove.get_to(move);
                int from_piece_type = board[from] & TYPE_MASK;
                int to_piece_type = board[to] & TYPE_MASK;
                score += mg_value[to_piece_type];
                score -= mg_value[from_piece_type];
            }
            if (ply == 0 && move == best_move) {
                score += 999999; // pv moves first
            }
            move = PMove.set_score(move, score);
            moves[i] = move;
        }
    }

    public static void swap_best_move(int[] moves, int best_move) {
        for (int i = 0; i < moves.length; i++) {
            if (moves[i] == best_move) {
                int temp = moves[0];
                moves[0] = moves[i];
                moves[i] = temp;
                break;
            }
        }
    }

    static int alphaBeta(int alpha, int beta, int depthleft, int ply, int best_move) throws Exception {
        if (depthleft == 0) {
            return Eval.eval();
        }
        if (nodes % 50 == 0) {
            if (System.currentTimeMillis() - time > time_out)
                throw new Exception();
        }

        gen();
        int moves[] = get_stack();
        pop_stack();

        int bestscore = -999999999; // failsoft approach

        // score_moves(moves, ply);
        // sort_moves(moves, ply);
        if (ply == 0 && time_out != 0) {
            swap_best_move(moves, best_move);
        }
        int legal_moves = 0;
        for (int i = 0; i < moves.length; i++) {
            int move = moves[i];
            if (make_move(move)) {
                nodes++;
                legal_moves++;
                int score;
                score = -alphaBeta(-beta, -alpha, depthleft - 1, ply + 1, 0);
                unmake_move();
                if (score >= beta) {
                    return score; // Soft fail
                }
                if (score > bestscore) {
                    bestscore = score;
                    if (score > alpha) { // alpha acts like max in MiniMax
                        alpha = score;
                        set_move(move, ply);
                    }
                }
            }
        }
        if (legal_moves == 0) {
            boolean checked = is_checked();
            if (checked) {
                return -100000;
            } else {
                return 0;
            }
        }
        return bestscore;
    }

    public static int search(int max_depth, long timeout_millis) {
        int score = 0;
        nodes = 0;
        time = System.currentTimeMillis();
        time_out = timeout_millis;
        best_move = 0;
        int prev_score = 0;
        for (int depth = 1; depth <= max_depth; depth++) {
            try {
                prev_score = score;
                score = alphaBeta(-9999999, 9999999, depth, 0, best_move);
            } catch (Exception e) {
                score = prev_score;
                break;
            }
            best_move = pv_arr[0][0];
        }
        long timeEnd = System.currentTimeMillis();
        double time_diff_secs = (timeEnd - time) / 1000;
        System.out.println("info nps " + (int) (nodes / time_diff_secs));
        return score;
    }
}