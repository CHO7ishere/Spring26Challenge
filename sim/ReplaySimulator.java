package sim;

import java.util.*;

/**
 * Loads a dump file into the GameEngine and simulates the recorded commands.
 * Useful for verifying the simulator matches Codingame behavior.
 */
public class ReplaySimulator {

    public static void runReplay(String dumpFile, boolean verbose) throws Exception {
        ReplayParser.ReplayData replay = ReplayParser.parse(dumpFile);

        // Initialize engine with the map
        GameEngine engine = new GameEngine();
        engine.initMap(replay.mapLines);

        // Add trees from turn 1
        ReplayParser.TurnData t1 = replay.turns.get(0);
        for (String treeLine : t1.treeLines) {
            String[] parts = treeLine.split(" ");
            GameEngine.TreeType type = GameEngine.TreeType.valueOf(parts[0]);
            int x = Integer.parseInt(parts[1]), y = Integer.parseInt(parts[2]);
            int size = Integer.parseInt(parts[3]), health = Integer.parseInt(parts[4]);
            int fruits = Integer.parseInt(parts[5]), cd = Integer.parseInt(parts[6]);
            engine.addTree(type, x, y, size, health, fruits, cd);
        }

        // Add trolls from turn 1
        for (String trollLine : t1.trollLines) {
            String[] parts = trollLine.split(" ");
            int id = Integer.parseInt(parts[0]), player = Integer.parseInt(parts[1]);
            int x = Integer.parseInt(parts[2]), y = Integer.parseInt(parts[3]);
            int ms = Integer.parseInt(parts[4]), cc = Integer.parseInt(parts[5]);
            int hp = Integer.parseInt(parts[6]), cp = Integer.parseInt(parts[7]);
            int cP = Integer.parseInt(parts[8]), cL = Integer.parseInt(parts[9]);
            int cA = Integer.parseInt(parts[10]), cB = Integer.parseInt(parts[11]);
            // Create troll at its initial position
            engine.spawnTroll(player, ms, cc, hp, cp);
            // Manually set position and carry
            // Note: spawnTroll creates at shack, so we need to override
            // This is a limitation - for now we'll just spawn at shack
        }

        engine.setVerbose(verbose);
        engine.setMaxTurns(replay.turns.size());

        // Run each turn with the recorded commands
        for (ReplayParser.TurnData td : replay.turns) {
            List<String> p0cmds = td.command != null ? Arrays.asList(td.command) : Collections.emptyList();
            List<String> p1cmds = Collections.emptyList(); // Only recorded player 0 commands
            engine.executeTurn(p0cmds, p1cmds);
        }

        System.out.println("Replay simulation complete.");
        System.out.println("P0 score: " + engine.getScore(0));
        System.out.println("P1 score: " + engine.getScore(1));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java sim.ReplaySimulator <dump_file> [--verbose]");
            return;
        }
        boolean verbose = args.length > 1 && args[1].equals("--verbose");
        runReplay(args[0], verbose);
    }
}
