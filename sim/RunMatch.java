package sim;

import java.util.*;

/**
 * Entry point to run a simulated match between two PlayerInterface implementations.
 * Usage: java sim.RunMatch [maxTurns] [seed]
 */
public class RunMatch {

    public static int[] runMatch(PlayerInterface p0, PlayerInterface p1,
                                  GameEngine engine, boolean verbose) {
        engine.setVerbose(verbose);

        // Build grid for each player's init
        GameEngine.CellType[][] grid0 = engine.getGridFor(0);
        GameEngine.CellType[][] grid1 = engine.getGridFor(1);
        p0.init(engine.getWidth(), engine.getHeight(), grid0,
                engine.getShackX(0), engine.getShackY(0),
                engine.getShackX(1), engine.getShackY(1));
        p1.init(engine.getWidth(), engine.getHeight(), grid1,
                engine.getShackX(1), engine.getShackY(1),
                engine.getShackX(0), engine.getShackY(0));

        while (!engine.isOver()) {
            int t = engine.getTurn() + 1;

            // Gather state for each player
            List<GameEngine.TreeInfo> treeInfos = engine.getTreeInfos();
            List<GameEngine.TrollInfo> trolls0 = engine.getTrollInfos(0);
            List<GameEngine.TrollInfo> trolls1 = engine.getTrollInfos(1);
            int[] inv0 = engine.getInv(0), inv1 = engine.getInv(1);

            // Get commands
            List<String> cmds0 = p0.play(t, inv0, inv1, treeInfos, trolls0);
            List<String> cmds1 = p1.play(t, inv1, inv0, treeInfos, trolls1);

            if (verbose) {
                System.err.println("=== Turn " + t + " ===");
                System.err.println("P0 cmds: " + cmds0);
                System.err.println("P1 cmds: " + cmds1);
            }

            engine.executeTurn(cmds0, cmds1);

            if (verbose) {
                System.err.println("Score: P0=" + engine.getScore(0) + " P1=" + engine.getScore(1));
            }
        }

        int s0 = engine.getScore(0), s1 = engine.getScore(1);
        return new int[]{s0, s1};
    }

    public static void main(String[] args) {
        int maxTurns = 300;
        long seed = System.currentTimeMillis();
        if (args.length >= 1) maxTurns = Integer.parseInt(args[0]);
        if (args.length >= 2) seed = Long.parseLong(args[1]);

        System.out.println("Creating match with seed=" + seed + " maxTurns=" + maxTurns);
        GameEngine engine = GameEngine.createDefault(16, 8, new Random(seed));
        engine.setMaxTurns(maxTurns);

        PlayerInterface p0 = new SimpleBot();
        PlayerInterface p1 = new SimpleBot();

        int[] scores = runMatch(p0, p1, engine, true);
        System.out.println("\n=== FINAL SCORES ===");
        System.out.println("Player 0: " + scores[0]);
        System.out.println("Player 1: " + scores[1]);
        if (scores[0] > scores[1]) System.out.println("Player 0 WINS!");
        else if (scores[1] > scores[0]) System.out.println("Player 1 WINS!");
        else System.out.println("DRAW!");
    }
}
