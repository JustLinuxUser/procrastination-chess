package org.example;

import static org.example.ChessBoard.make_move;
import static org.example.ChessBoard.setup_board;
import static org.example.Search.get_best_move;
import static org.example.Search.search;

import java.util.HashMap;
import java.util.Scanner;

public class UCI {
    Scanner stdin = new Scanner(System.in);

    public void process_command() {
        if (!stdin.hasNextLine())
            return;
        String command = stdin.nextLine().trim();
        String subcommands[] = command.split(" ");
        if (subcommands[0].equals("uci")) {
            System.out.println("id name the Procrastination");
            System.out.println("id author Andrii Dokhniak");
            System.out.println("uciok");
        } else if (subcommands[0].equals("ucinewgame")) {
        } else if (subcommands[0].equals("explorer")) {
            ChessBoard.explorer();
        } else if (subcommands[0].equals("isready")) {
            System.out.println("readyok");
        } else if (subcommands[0].equals("position")) {
            if (subcommands[1].equals("fen")) {
                String fen = command.substring("position fen ".length() - 1);
                setup_board(fen);
            } else if (subcommands[1].equals("startpos")) {
                setup_board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
                for (int i = 3; i < subcommands.length; i++) {
                    if (!make_move(subcommands[i])) {
                        System.out.println("Could not find the move!!! " + subcommands[i]);
                        System.exit(-1);
                        return;
                    }
                }
            }
        } else if (subcommands[0].equals("quit")) {
            System.exit(0);
        } else if (subcommands[0].equals("go")) {
            HashMap<String, String> attributes = new HashMap<>();
            for (int i = 1; i < subcommands.length - 1; i += 2) {
                if (subcommands[i].equals("infinite")) {
                    attributes.put(subcommands[i], "true");
                    i--;
                    continue;
                }
                attributes.put(subcommands[i], subcommands[i + 1]);
            }
            // go wtime 60000 btime 60000 winc 1000 binc 1000
            int time_left = 0;
            int time_inc = 0;
            if (ChessBoard.side == 0) { // white
                time_left = Integer.parseInt(attributes.get("wtime"));
                time_inc = Integer.parseInt(attributes.get("winc"));
            } else {
                time_left = Integer.parseInt(attributes.get("btime"));
                time_inc = Integer.parseInt(attributes.get("binc"));
            }
            int time_to_think = time_left / 20 + time_inc / 2;
            search(6, time_to_think);
            System.out.println("bestmove " + get_best_move(6));
        }
        System.out.flush();
    }
}
