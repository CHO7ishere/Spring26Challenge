package sim;

import java.util.*;

/**
 * A basic bot that harvests the nearest tree and returns to drop.
 * Used as a default opponent for testing.
 */
public class SimpleBot implements PlayerInterface {
    private int width, height;
    private int myShackX, myShackY;

    @Override
    public void init(int width, int height, GameEngine.CellType[][] grid,
                     int myShackX, int myShackY, int oppShackX, int oppShackY) {
        this.width = width;
        this.height = height;
        this.myShackX = myShackX;
        this.myShackY = myShackY;
    }

    @Override
    public List<String> play(int turn,
                             int[] myInventory, int[] oppInventory,
                             List<GameEngine.TreeInfo> trees,
                             List<GameEngine.TrollInfo> trolls) {
        List<String> cmds = new ArrayList<>();
        Set<String> assignedTrees = new HashSet<>();

        List<GameEngine.TrollInfo> myTrolls = new ArrayList<>();
        for (GameEngine.TrollInfo t : trolls) if (t.player == 0) myTrolls.add(t);

        for (GameEngine.TrollInfo troll : myTrolls) {
            int carried = troll.carryPlum + troll.carryLemon + troll.carryApple + troll.carryBanana + troll.carryIron + troll.carryWood;
            int remaining = troll.carryCapacity - carried;

            // On a tree with fruits and can carry -> harvest
            boolean onFruitTree = false;
            for (GameEngine.TreeInfo tree : trees) {
                if (tree.x == troll.x && tree.y == troll.y && tree.fruits > 0 && remaining > 0) {
                    cmds.add("HARVEST " + troll.id);
                    onFruitTree = true;
                    break;
                }
            }
            if (onFruitTree) continue;

            // Adjacent to shack and carrying -> drop
            if (Math.abs(troll.x - myShackX) + Math.abs(troll.y - myShackY) == 1 && carried > 0) {
                cmds.add("DROP " + troll.id);
                continue;
            }

            // If carrying and full (or no reachable trees) -> go home
            if (remaining <= 0 && carried > 0) {
                cmds.add("MOVE " + troll.id + " " + myShackX + " " + myShackY);
                continue;
            }

            // Find closest tree with fruits
            GameEngine.TreeInfo best = null;
            int bestDist = Integer.MAX_VALUE;
            for (GameEngine.TreeInfo tree : trees) {
                if (tree.fruits <= 0) continue;
                String key = tree.x + "," + tree.y;
                if (assignedTrees.contains(key)) continue;
                int d = Math.abs(tree.x - troll.x) + Math.abs(tree.y - troll.y);
                if (d < bestDist) { bestDist = d; best = tree; }
            }

            if (best != null) {
                assignedTrees.add(best.x + "," + best.y);
                cmds.add("MOVE " + troll.id + " " + best.x + " " + best.y);
            } else if (carried > 0) {
                cmds.add("MOVE " + troll.id + " " + myShackX + " " + myShackY);
            } else {
                cmds.add("WAIT");
            }
        }

        if (cmds.isEmpty()) cmds.add("WAIT");
        return cmds;
    }
}
