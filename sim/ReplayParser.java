package sim;

import java.io.*;
import java.util.*;

/**
 * Parses DUMP| lines from DumpPlayer stderr output and reconstructs
 * the game state turn by turn. This allows replaying Codingame matches locally.
 *
 * Usage:
 *   java sim.ReplayParser <dump_file>
 *
 * The dump file should contain lines starting with "DUMP|" (other lines are ignored).
 */
public class ReplayParser {

    public static class TurnData {
        public int turn;
        public int[] myInv = new int[6];
        public int[] oppInv = new int[6];
        public List<String> treeLines = new ArrayList<>();
        public List<String> trollLines = new ArrayList<>();
        public String command;
    }

    public static class ReplayData {
        public int width, height;
        public String[] mapLines;
        public List<TurnData> turns = new ArrayList<>();
    }

    public static ReplayData parse(String filename) throws IOException {
        return parse(new BufferedReader(new FileReader(filename)));
    }

    public static ReplayData parse(BufferedReader reader) throws IOException {
        ReplayData replay = new ReplayData();
        TurnData current = null;
        String line;

        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("DUMP|")) continue;
            String payload = line.substring(5);

            if (payload.startsWith("INIT|")) {
                String[] parts = payload.substring(5).split(" ");
                replay.width = Integer.parseInt(parts[0]);
                replay.height = Integer.parseInt(parts[1]);
                replay.mapLines = new String[replay.height];
            } else if (payload.startsWith("MAP|")) {
                String mapLine = payload.substring(4);
                for (int i = 0; i < replay.mapLines.length; i++) {
                    if (replay.mapLines[i] == null) {
                        replay.mapLines[i] = mapLine;
                        break;
                    }
                }
            } else if (payload.startsWith("TURN|")) {
                current = new TurnData();
                current.turn = Integer.parseInt(payload.substring(5));
                replay.turns.add(current);
            } else if (current != null) {
                if (payload.startsWith("INV|MY|")) {
                    String[] parts = payload.substring(7).split(" ");
                    for (int i = 0; i < parts.length && i < 6; i++)
                        current.myInv[i] = Integer.parseInt(parts[i]);
                } else if (payload.startsWith("INV|OPP|")) {
                    String[] parts = payload.substring(8).split(" ");
                    for (int i = 0; i < parts.length && i < 6; i++)
                        current.oppInv[i] = Integer.parseInt(parts[i]);
                } else if (payload.startsWith("TREE|")) {
                    current.treeLines.add(payload.substring(5));
                } else if (payload.startsWith("TROLL|")) {
                    current.trollLines.add(payload.substring(6));
                } else if (payload.startsWith("CMD|")) {
                    current.command = payload.substring(4);
                }
            }
        }
        reader.close();
        return replay;
    }

    /**
     * Prints a human-readable summary of the replay.
     */
    public static void printSummary(ReplayData replay) {
        System.out.println("Map: " + replay.width + "x" + replay.height);
        for (String m : replay.mapLines) System.out.println("  " + m);
        System.out.println("Turns: " + replay.turns.size());
        for (TurnData td : replay.turns) {
            System.out.println("\n--- Turn " + td.turn + " ---");
            System.out.println("My inv: " + Arrays.toString(td.myInv));
            System.out.println("Opp inv: " + Arrays.toString(td.oppInv));
            System.out.println("Trees: " + td.treeLines.size());
            for (String t : td.treeLines) System.out.println("  " + t);
            System.out.println("Trolls: " + td.trollLines.size());
            for (String t : td.trollLines) System.out.println("  " + t);
            System.out.println("Cmd: " + td.command);
        }
        if (!replay.turns.isEmpty()) {
            TurnData last = replay.turns.get(replay.turns.size() - 1);
            int myScore = last.myInv[0] + last.myInv[1] + last.myInv[2] + last.myInv[3] + last.myInv[5] * 4;
            int oppScore = last.oppInv[0] + last.oppInv[1] + last.oppInv[2] + last.oppInv[3] + last.oppInv[5] * 4;
            System.out.println("\nFinal scores: My=" + myScore + " Opp=" + oppScore);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java sim.ReplayParser <dump_file>");
            return;
        }
        ReplayData replay = parse(args[0]);
        printSummary(replay);
    }
}
