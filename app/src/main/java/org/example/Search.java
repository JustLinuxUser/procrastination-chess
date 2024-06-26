package org.example;

import static org.example.ChessBoard.*;
import static org.example.Definitions.BLACK;
import static org.example.Definitions.TYPE_MASK;
import static org.example.Generator.*;

import static org.example.EvalDefinitions.*;

public class Search {

    public static long[][] pv_arr = new long[20][200];
    public static long best_move = 0;
    public static int nodes = 0;
    public static long time_start = System.currentTimeMillis();
    public static long timeout = 0;
    public static long timer = 0;
    public static int is_timing_q = 0;
    public static long timeInAB = 0;
    public static long timeInQ = 0;

    public static void set_move(long move, int ply) {
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

    public static void pick_move(long[] moves, int i) {
        int max = PMove.get_score(moves[i]);
        int max_idx = i;
        for (int j = i; j < moves.length; j++) {
            int m2_score = PMove.get_score(moves[j]);
            if (m2_score > max) {
                max = m2_score;
                max_idx = j;
            }
        }
        long temp = moves[i];
        moves[i] = moves[max_idx];
        moves[max_idx] = temp;
    }

    public static void score_moves(long[] moves, int ply) {
        int best_move_matches = 0;
        for (int i = 0; i < moves.length; i++) {
            long move = moves[i];
            int score = 0;
            int to = PMove.get_to(move);
            int from = PMove.get_from(move);
            int move_flags = PMove.get_flags(move);
            if ((move_flags & PMove.TYPE_MASK) == PMove.EP) {
                score += 100;
            }
            if ((move_flags & PMove.CAPTURE) != 0) {
                int to_piece_type = board[to] & TYPE_MASK;
                int from_piece_type = board[from] & TYPE_MASK;
                score += mg_value[to_piece_type] * 100;
                score -= mg_value[from_piece_type];
            }
            if (ply == 0 && best_move == move) {
                score += 100000000;
                best_move_matches++;
            }
            move = PMove.set_score(move, score);
            moves[i] = move;
        }
        if (best_move_matches > 1) {
            System.exit(1);
        }
    }

    public static void swap_best_move(long[] moves) {
        for (int i = 0; i < moves.length; i++) {
            if (moves[i] == best_move) {
                long temp = moves[0];
                moves[0] = moves[i];
                moves[i] = temp;
                break;
            }
        }
    }

    static int alphaBeta(int alpha, int beta, int depthleft, int ply) throws Exception {
        nodes++;
        if (nodes % 500 == 0) {
            if (System.currentTimeMillis() - time_start > timeout)
                throw new Exception();
        }

        if (depthleft == 0) {
            // return Eval.eval();
            timeInAB += System.currentTimeMillis() - timer;
            timer = System.currentTimeMillis();
            is_timing_q = 1;
            return qsearch(alpha, beta, 0);
            //return Eval.eval();
        }

        gen();
        long moves[] = get_stack();
        pop_stack();

        int best_score = -999999999; // failsoft approach
        if (ply == 0 && best_move != 0) {
            swap_best_move(moves);
        }

        //score_moves(moves, ply);
        int legal_moves = 0;
        for (int i = 0; i < moves.length; i++) {
            //pick_move(moves, i);
            long move = moves[i];
            if (make_move(move)) {
                legal_moves++;
                int score;
                score = -alphaBeta(-beta, -alpha, depthleft - 1, ply + 1);
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
                return -100000 + ply;
            } else {
                return 0;
            }
        }
        return best_score;
    }

    static int qsearch(int alpha, int beta, int depth) throws Exception {
        nodes++;
        if (nodes % 500 == 0) {
            if (System.currentTimeMillis() - time_start > timeout)
                throw new Exception();
        }
        int standing_pat = Eval.eval();
        if (standing_pat >= beta || depth >= 2) {
            return standing_pat;
        }
        if (standing_pat > alpha) {
            alpha = standing_pat;
        }
        gen_caps();
        long moves[] = get_stack();
        pop_stack();
        score_moves(moves, 2);

        int best_score = standing_pat;

        for (int i = 0; i < moves.length; i++) {
            pick_move(moves, i);
            long move = moves[i];
            if (make_move(move)) {
                int score = -qsearch(-beta, -alpha, depth + 1);
                unmake_move();
                if (score >= beta) {
                    return score;
                }
                if (score > best_score) {
                    best_score = score;
                }
                if (score > alpha) {
                    alpha = score;
                }
            }
        }
        return best_score;
    } 


    public static int search(int max_depth, long timeout_millis) {
        int score = 0;
        nodes = 0;
        time_start = System.currentTimeMillis();
        timeout = timeout_millis;
        best_move = 0;
        timer = System.currentTimeMillis();
        int prev_score = 0;
        for (int depth = 1; depth <= max_depth; depth++) {
            try {
                prev_score = score;
                if (is_timing_q == 0) {
                    timeInQ += System.currentTimeMillis() - timer;
                } else {
                    timeInAB += System.currentTimeMillis() - timer;
                }
                is_timing_q = 0;
                timer = System.currentTimeMillis();
                score = alphaBeta(-9999999, 9999999, depth, 0);
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
