/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.example;

import static org.example.ChessBoard.setup_board;
import static org.example.Search.*;

import java.util.List;

public class App {

    public static void perftree_client(String[] args, ChessBoard board) {
        if (args.length != 2 && args.length != 3) {
            for (String arg : args) {
                System.out.println("    " + arg);
            }
            System.out.println("You provided " + args.length + " args");
            System.out.println("Error, invalid arguments, the format is: ");
            System.out.println("<app_name> \"$depth\" \"$fen\" \"$moves\"");
            return;
        }
        int depth = Integer.parseInt(args[0]);
        String fen = args[1];
        ChessBoard.setup_board(fen);
        if (args.length == 3) {
            ChessBoard.perft(depth, args[2]);
        } else {
            ChessBoard.perft(depth, "");
        }
    }

    public static void main(String[] args) {
        // perftree_client(args, board);
        setup_board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        Eval.init_tables();
        UCI uci = new UCI();
        while (true) {
            uci.process_command();
        }
    }
}
