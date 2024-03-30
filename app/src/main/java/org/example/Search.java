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
    public static long time_start = System.currentTimeMillis();
    public static long timeout = 0;

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

    public static void pick_move(int[] moves, int i) {
        int max = -99999;
        int max_idx = i;
        for (int j = i; j < moves.length; j++) {
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

    public static void score_moves(int[] moves) {
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
        if (nodes % 500 == 0) {
            if (System.currentTimeMillis() - time_start > timeout)
                throw new Exception();
        }

        if (depthleft == 0) {
            // return Eval.eval();
            return qsearch(alpha, beta);
        }

        gen();
        int moves[] = get_stack();
        pop_stack();

        int best_score = -999999999; // failsoft approach

        score_moves(moves);
        // sort_moves(moves, ply);
        if (ply == 0 && best_move != 0) {
            swap_best_move(moves, best_move);
        }
        int legal_moves = 0;
        for (int i = 0; i < moves.length; i++) {
            pick_move(moves, i);
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
                if (score > best_score) {
                    best_score = score;
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
        return best_score;
    }

    static int qsearch(int alpha, int beta) throws Exception {
        if (nodes % 500 == 0) {
            if (System.currentTimeMillis() - time_start > timeout)
                throw new Exception();
        }
        int standing_pat = Eval.eval();
        if (standing_pat > beta) {
            return standing_pat;
        }
        if (standing_pat > alpha) {
            alpha = standing_pat;
        }
        gen_caps();
        int moves[] = get_stack();
        pop_stack();
        score_moves(moves);

        int best_score = standing_pat;

        int legal_moves = 0;
        for (int i = 0; i < moves.length; i++) {
            pick_move(moves, i);
            int move = moves[i];
            if (make_move(move)) {
                legal_moves++;
                nodes++;
                int score;
                score = -qsearch(-alpha, -beta);
                unmake_move();
                if (score >= beta) {
                    return score;
                }
                if (score > best_score) {
                    best_score = score;
                    if (score > alpha) {
                        alpha = score;
                    }
                }
            }
        }
        return best_score;
    }

    /*
     * int Quiesce( int alpha, int beta ) {
     * int stand_pat = Evaluate();
     * if( stand_pat >= beta )
     * return beta;
     * if( alpha < stand_pat )
     * alpha = stand_pat;
     * 
     * until( every_capture_has_been_examined ) {
     * MakeCapture();
     * score = -Quiesce( -beta, -alpha );
     * TakeBackMove();
     * 
     * if( score >= beta )
     * return beta;
     * if( score > alpha )
     * alpha = score;
     * }
     * return alpha;
     * }
     */

    public static int search(int max_depth, long timeout_millis) {
        int score = 0;
        nodes = 0;
        time_start = System.currentTimeMillis();
        timeout = timeout_millis;
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
        long time_end = System.currentTimeMillis();
        double time_diff_secs = (time_end - time_start) / 1000;
        System.out.println("info nps " + (int) (nodes / time_diff_secs));
        return score;
    }
}
