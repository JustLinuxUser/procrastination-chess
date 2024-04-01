package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.example.Generator.*;
import static org.example.Search.pick_move;
import static org.example.Search.print_pv;
import static org.example.Search.score_moves;
import static org.example.Search.search;
import static org.example.Definitions.*;

public class ChessBoard {

    static short move_idx = 1;
    static byte ep = -1;
    static byte side = 0;

    static byte castle = 0b0000_1111;
    static byte[] board = {
            3, 1, 2, 4, 5, 2, 1, 3,
            0, 0, 0, 0, 0, 0, 0, 0,
            6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6,
            0, 0, 0, 0, 0, 0, 0, 0,
            3, 1, 2, 4, 5, 2, 1, 3
    }; // 2, 3, 4 rth bit are for piece type, 1st bit is for piece color

    static long[] move_stacks = new long[1000];
    static List<Integer> move_stack_offsets = new ArrayList<>();
    static List<Hist> history = new ArrayList<>();

    static void new_substack() {
        int curr_offset = move_stack_offsets.getLast();
        move_stack_offsets.addLast(curr_offset);
    }

    static int get_stack_offset() {
        int size = move_stack_offsets.size();
        return move_stack_offsets.get(size - 2);
    }

    static int get_stack_end() {
        return move_stack_offsets.getLast();
    }

    static long[] get_stack() {
        int offset = get_stack_offset();
        int offset_end = get_stack_end();
        long[] moves = Arrays.copyOfRange(move_stacks, offset, offset_end);
        return moves;
    }

    static int stack_len() {
        int offset = get_stack_offset();
        int offset_end = get_stack_end();
        return offset_end - offset;
    }

    static void stack_push_move(long h) {
        int curr_offset = move_stack_offsets.removeLast();
        move_stacks[curr_offset] = h;
        move_stack_offsets.addLast(curr_offset + 1);
    }

    static void pop_stack() {
        move_stack_offsets.removeLast();
    }

    static void setup_board(String fen) {
        move_stack_offsets.addLast(0);
        move_stack_offsets.addLast(0);
        String[] subcmds = fen.split(" ");
        String piece_placement = subcmds[0];
        String pieces = "pnbrkqPNBRKQ";
        for (int i = 0; i < 64; i++) {
            board[i] = EMPTY;
        }
        int i = 0;
        for (char c : piece_placement.toCharArray()) {
            int my_idx = (63 - (i / 8) * 8) - 7 + i % 8;
            if (c >= '1' && c <= '8') {
                int skip = Integer.parseInt("" + c);
                i += skip;
            } else if (pieces.contains("" + c)) {
                int piece = pieces.indexOf(c);
                if (piece >= 6) {
                    board[my_idx] = (byte) (piece - 6);
                } else {
                    board[my_idx] = (byte) (piece | BLACK);
                }
                i++;
            } else {
                continue;
            }
        }
        String turn = subcmds[1];
        if (turn.equals("w")) {
            side = 0;
        } else {
            side = BLACK;
        }

        String castling = subcmds[2];
        castle = 0;
        if (castling.contains("k")) {
            castle |= CASTLE_BK;
        }
        if (castling.contains("q")) {
            castle |= CASTLE_BQ;
        }
        if (castling.contains("K")) {
            castle |= CASTLE_WK;
        }
        if (castling.contains("Q")) {
            castle |= CASTLE_WQ;
        }

        String ep = subcmds[3];
        if (ep.equals("-")) {
            ChessBoard.ep = -1;
        } else {
            ChessBoard.ep = Notation.to_idx(ep);
        }
    }

    static String gen_fen() {
        String ret = "";
        // rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
        // White are uppercase!!!
        String pieces = "pnbrkqPNBRKQ";
        for (int y = 7; y >= 0; y--) {
            int empty = 0;
            for (int x = 0; x < 8; x++) {
                byte piece = board[y * 8 + x];
                byte piece_type = (byte) (piece & TYPE_MASK);
                byte piece_color = (byte) (piece & COLOR_MASK);
                if (piece_type == EMPTY) {
                    empty++;
                    continue;
                }
                if (empty != 0) {
                    ret += "" + empty;
                    empty = 0;
                }
                if (piece_color == BLACK) {
                    ret += pieces.charAt(piece_type);
                } else {
                    ret += pieces.charAt(piece_type + 6);
                }
            }
            if (empty != 0) {
                ret += "" + empty;
                empty = 0;
            }
            if (y != 0) {
                ret += "/";
            }
        }
        if (side == BLACK) {
            ret += " b ";
        } else {
            ret += " w ";
        }
        if ((castle & CASTLE_WK) != 0) {
            ret += "K";
        }
        if ((castle & CASTLE_WQ) != 0) {
            ret += "Q";
        }
        if ((castle & CASTLE_BK) != 0) {
            ret += "k";
        }
        if ((castle & CASTLE_BQ) != 0) {
            ret += "q";
        }
        ret += " ";
        if (ep != -1) {
            ret += Notation.from_idx(ep);
        } else {
            ret += "-";
        }
        return ret;
    }

    static void push_move(byte from, byte to, int flags) {
        stack_push_move(PMove.create_move(from, to, (byte) flags));
    }

    static boolean promotion(byte from, byte to) {
        if (side == WHITE) { // White
            if (to < 56) {
                return false;
            }
            if (to - from != 8) { // diagonal capture
                // we need to generate all, because there is a chance that the queen will make a
                // draw
                push_move(from, to, PMove.CAPTURE | PMove.PROMO | PMove.PROMO_QUEEN);
                push_move(from, to, PMove.CAPTURE | PMove.PROMO | PMove.PROMO_KNIGHT);
                push_move(from, to, PMove.CAPTURE | PMove.PROMO | PMove.PROMO_BSHOP);
                push_move(from, to, PMove.CAPTURE | PMove.PROMO | PMove.PROMO_ROOK);
            } else {
                push_move(from, to, PMove.PROMO | PMove.PROMO_QUEEN);
                push_move(from, to, PMove.PROMO | PMove.PROMO_KNIGHT);
                push_move(from, to, PMove.PROMO | PMove.PROMO_BSHOP);
                push_move(from, to, PMove.PROMO | PMove.PROMO_ROOK);
            }
        } else {
            if (to > 7) {
                return false;
            }
            if (to - from != -8) { // diagonal capture
                // we need to generate all, because there is a chance that the queen will make a
                // draw
                push_move(from, to, PMove.PROMO | PMove.CAPTURE | PMove.PROMO_QUEEN);
                push_move(from, to, PMove.PROMO | PMove.CAPTURE | PMove.PROMO_KNIGHT);
                push_move(from, to, PMove.PROMO | PMove.CAPTURE | PMove.PROMO_BSHOP);
                push_move(from, to, PMove.PROMO | PMove.CAPTURE | PMove.PROMO_ROOK);
            } else {
                push_move(from, to, PMove.PROMO | PMove.PROMO_QUEEN);
                push_move(from, to, PMove.PROMO | PMove.PROMO_KNIGHT);
                push_move(from, to, PMove.PROMO | PMove.PROMO_BSHOP);
                push_move(from, to, PMove.PROMO | PMove.PROMO_ROOK);
            }
        }
        return true;
    }

    static boolean promotion_queen(byte from, byte to) {
        if (side == WHITE) { // White
            if (to < 56) {
                return false;
            }
            if (to - from != 8) { // diagonal capture
                // we need to generate all, because there is a chance that the queen will make a
                // draw
                push_move(from, to, PMove.CAPTURE | PMove.PROMO | PMove.PROMO_QUEEN);
            } else {
                push_move(from, to, PMove.PROMO | PMove.PROMO_QUEEN);
            }
        } else {
            if (to > 7) {
                return false;
            }
            if (to - from != -8) { // diagonal capture
                push_move(from, to, PMove.PROMO | PMove.CAPTURE | PMove.PROMO_QUEEN);
            } else {
                push_move(from, to, PMove.PROMO | PMove.PROMO_QUEEN);
            }
        }
        return true;
    }

    static boolean attack(byte attacked_idx) {
        for (byte i = 0; i < 64; i++) {
            byte piece = board[i];
            byte piece_type = (byte) (piece & TYPE_MASK);
            byte piece_color = (byte) (piece & COLOR_MASK);
            byte piece_col = (byte) (i & 0b111);
            if (piece_type == EMPTY) {
                continue;
            }
            if (piece_color != side) {
                continue;
            }
            if (piece_type == PAWN) {
                if (piece_color == WHITE) {
                    if (piece_col != 0 && i + 7 == attacked_idx) {
                        return true;
                    }
                    if (piece_col != 7 && i + 9 == attacked_idx) {
                        return true;
                    }
                } else {
                    if (piece_col != 0 && i - 9 == attacked_idx) {
                        return true;
                    }
                    if (piece_col != 7 && i - 7 == attacked_idx) {
                        return true;
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

                        if (to == attacked_idx) {
                            return true;
                        }
                        if (to_type != EMPTY) {
                            break;
                        }
                        if (piece_type == PAWN || piece_type == KNIGHT || piece_type == KING) { // they shouldn't
                            break;
                        }
                    }
                }
            }
        }
        return false;

    }

    static boolean is_checked() {
        byte king = 0;
        for (byte i = 0; i < 64; i++) {
            if ((board[i] & TYPE_MASK) == KING
                    && (board[i] & COLOR_MASK) == side) {
                king = i;
                break;
            }
        }
        side ^= BLACK;
        boolean is_checked = attack(king);
        side ^= BLACK;
        return is_checked;
    }

    static boolean make_move(long m) {
        boolean checked = is_checked();
        byte from = PMove.get_from(m);
        byte to = PMove.get_to(m);
        byte flags = PMove.get_flags(m);

        byte piece = board[from];
        byte piece_type = (byte) (piece & TYPE_MASK);
        byte move_type = (byte) (flags & PMove.TYPE_MASK);
        if (move_type == PMove.CASTLE_KING) {
            if (checked) {
                return false;
            }
            board[to - 1] = board[to + 1];
            board[to + 1] = EMPTY;
        } else if (move_type == PMove.CASTLE_QUEEN) {
            if (checked) {
                return false;
            }
            board[to + 1] = board[to - 2];
            board[to - 2] = EMPTY;
        }
        Hist hist = new Hist();
        hist.captured = board[to];
        hist.ep = ep;
        hist.move = m;
        hist.castle = castle;
        // EP handling
        if ((flags & PMove.TYPE_MASK) == PMove.EP) {
            if ((board[from] & COLOR_MASK) == WHITE) {
                board[to - 8] = EMPTY;
            } else {
                board[to + 8] = EMPTY;
            }
        }

        if (piece_type == KING) {
            if (side == BLACK) {
                castle &= ~CASTLE_BK;
                castle &= ~CASTLE_BQ;
            } else {
                castle &= ~CASTLE_WK;
                castle &= ~CASTLE_WQ;
            }
        }
        // Check 4 corners, if ANYTHING is going on there, we reset the castle flag
        if (from == A1 || to == A1) {
            castle &= ~CASTLE_WQ;
        }
        if (from == H1 || to == H1) {
            castle &= ~CASTLE_WK;
        }
        if (from == A8 || to == A8) {
            castle &= ~CASTLE_BQ;
        }
        if (from == H8 || to == H8) {
            castle &= ~CASTLE_BK;
        }

        if (piece_type == PAWN
                && Math.abs(from - to) == 16) {
            ep = to;
        } else {
            ep = -1;
        }
        board[to] = board[from];
        board[from] = EMPTY;

        if ((flags & PMove.TYPE_MASK) == PMove.PROMO_QUEEN) {
            board[to] = (byte) (QUEEN | side);
        } else if ((flags & PMove.TYPE_MASK) == PMove.PROMO_BSHOP) {
            board[to] = (byte) (BISHOP | side);
        } else if ((flags & PMove.TYPE_MASK) == PMove.PROMO_ROOK) {
            board[to] = (byte) (ROOK | side);
        } else if ((flags & PMove.TYPE_MASK) == PMove.PROMO_KNIGHT) {
            board[to] = (byte) (KNIGHT | side);
        }

        move_idx++;
        history.addLast(hist);
        if (is_checked()) {
            side ^= BLACK;
            unmake_move();
            return false;
        }
        side ^= BLACK;
        return true;
    }

    static void unmake_move() {
        Hist hist = history.removeLast();
        long m = hist.move;
        byte from = PMove.get_from(m);
        byte to = PMove.get_to(m);
        byte flags = PMove.get_flags(m);

        board[from] = board[to];
        board[to] = hist.captured;
        ep = hist.ep;
        castle = hist.castle;

        if ((flags & PMove.TYPE_MASK) == PMove.EP) {
            // EP handling
            if ((board[from] & COLOR_MASK) == WHITE) {
                board[to - 8] = PAWN | BLACK;
            } else {
                board[to + 8] = PAWN;
            }
        }

        if ((flags & PMove.TYPE_MASK) == PMove.CASTLE_KING) {
            board[to + 1] = board[to - 1];
            board[to - 1] = EMPTY;
        }
        if ((flags & PMove.TYPE_MASK) == PMove.CASTLE_QUEEN) {
            board[to - 2] = board[to + 1];
            board[to + 1] = EMPTY;
        }

        move_idx--;
        side ^= BLACK;
        if ((flags & PMove.PROMO) != 0) {
            board[from] = (byte) (PAWN | side);
        }

    }

    static long perft(int depth) {
        if (depth == 0) {
            return 1;
        }
        gen();
        var moves = get_stack();
        long count = 0;
        for (var m : moves) {
            if (make_move(m)) {
                count += perft(depth - 1);
                unmake_move();
            }
        }
        pop_stack();
        return count;
    }

    static void perft(int depth, String s_req_moves) {
        long count = 0;
        String result = "";
        String[] s_arr_req_moves = s_req_moves.split(" ");
        List<Long> req_moves = new ArrayList<>();
        gen();
        var generated_moves = get_stack();
        if (s_arr_req_moves.length != 1) {
            for (String wanted_move : s_arr_req_moves) {
                for (var m : generated_moves) {
                    if (PMove.toString(m, side).equals(wanted_move)) {
                        req_moves.addLast(m);
                        break;
                    }
                }
            }
        } else {
            req_moves = Arrays.stream(generated_moves).boxed().collect(Collectors.toList());
        }

        for (var m : req_moves) {
            if (make_move(m)) {
                long curr_count = perft(depth - 1);
                result += PMove.toString(m, side) + " " + curr_count + "\n";
                count += curr_count;
                unmake_move();
            }
        }
        result += "\n";
        result += count;
        result += "\n";
        System.out.print(result);
        pop_stack();
    }

    static boolean make_move(String requested_move) {
        gen();
        long[] generated_moves = get_stack();
        pop_stack();
        for (var m : generated_moves) {
            if (PMove.toString(m, side).equals(requested_move)) {
                if (make_move(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    static void explorer() {
        Scanner s = new Scanner(System.in);
        while (true) {
            print();
            gen_caps();
            long[] moves = get_stack();
            pop_stack();
            score_moves(moves, 1);
            for (int i = 0; i < moves.length; i++) {
                pick_move(moves, i);
                long m = moves[i];
                byte flags = PMove.get_flags(m);
                int score = PMove.get_score(m);
                System.out
                        .println(i + " " + PMove.toString(m, side) + " " + Integer.toBinaryString(flags) + " " + score);
            }
            System.out.println("Count: " + moves.length);
            System.out.print("> ");
            String command = s.nextLine().trim();
            if (command.equals("quit")) {
                break;
            } else if (command.equals("unmove")) {
                unmake_move();
            } else if (command.equals("fen")) {
                System.out.print("FEN> ");
                String fen = s.nextLine().trim();
                setup_board(fen);
            } else if (command.equals("perft")) {
                System.out.print("PERFT> ");
                int depth = s.nextInt();
                s.nextLine();
                perft(depth, "");
            } else if (command.equals("genfen")) {
                System.out.println(gen_fen());
            } else if (command.equals("eval")) {
                System.out.println(Eval.eval());
            } else if (command.equals("go")) {
                System.out.print("GO> ");
                int depth = s.nextInt();
                int score = search(depth, 0);
                System.out.println("Score: " + score);
                print_pv();
            } else {
                try {
                    int idx = Integer.parseInt(command);
                    if (idx < 0 || idx >= moves.length) {
                        System.out.println("OUT OF RANGE");
                    }
                    if (!make_move(moves[idx])) {
                        System.out.println("FAILED TO MOVE");
                    }
                } catch (Exception e) {
                    System.out.println("Invalid command, here is a list: ");
                    System.out.println("perft");
                    System.out.println("fen");
                    System.out.println("genfen");
                    System.out.println("unmove");
                    System.out.println("quit");
                    System.out.println("go");
                    System.out.println("eval");
                }
            }
        }
        s.close();
    }

    static void print() {
        String ANSI_BG1 = "\u001B[48;5;58m";
        String ANSI_BG2 = "\u001B[48;5;102m";
        String ANSI_RESET = "\u001B[0m";

        String[] pieces = { "♟︎", "♞", "♝", "♜", "♚", "♛", " ", "♙", "♘", "♗", "♖", "♔", "♕", " " };

        for (int y = 7; y >= 0; y--) {
            System.out.print((y + 1) + " ");
            for (int x = 0; x < 8; x++) {
                if ((x + y) % 2 == 0) {
                    System.out.print(ANSI_BG1);
                } else {
                    System.out.print(ANSI_BG2);
                }
                byte piece = board[y * 8 + x];
                byte piece_color = (byte) (piece & COLOR_MASK);
                byte piece_type = (byte) (piece & TYPE_MASK);
                if (piece_color == BLACK) {
                    System.out.print(pieces[piece_type + 7]);
                } else {
                    System.out.print(pieces[piece_type]);
                }
                System.out.print(" ");
            }
            System.out.println(ANSI_RESET);
        }
        System.out.print("  ");
        for (int i = 0; i < 8; i++) {
            System.out.print((char) (i + 'a') + " ");
        }
        System.out.println();
    }
}
