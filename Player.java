import java.util.*;

class Player {

    static final int MAX_TURNS = 300;
    static int turn = 0;
    static int width, height;
    static int myShackX, myShackY, oppShackX, oppShackY;
    static int[][] grid; // 0=grass, 1=myShack, 2=oppShack, 3=water, 4=rock, 5=iron

    static int[] myInv = new int[6]; // plum,lemon,apple,banana,iron,wood
    static int[] oppInv = new int[6];

    static List<int[]> trees = new ArrayList<>();
    // tree: [type, x, y, size, health, fruits, cooldown]
    static List<int[]> myTrolls = new ArrayList<>();
    static List<int[]> oppTrolls = new ArrayList<>();
    // troll: [id, x, y, moveSpeed, carryCap, harvestPow, chopPow, cPlum, cLemon, cApple, cBanana, cIron, cWood]


    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        width = in.nextInt(); height = in.nextInt(); in.nextLine();
        grid = new int[height][width];
        for (int y = 0; y < height; y++) {
            String line = in.nextLine();
            for (int x = 0; x < width; x++) {
                char c = line.charAt(x);
                if (c == '0') { grid[y][x] = 1; myShackX = x; myShackY = y; }
                else if (c == '1') { grid[y][x] = 2; oppShackX = x; oppShackY = y; }
                else if (c == '~') { grid[y][x] = 3; }
                else if (c == '#') { grid[y][x] = 4; }
                else if (c == '+') { grid[y][x] = 5; }
            }
        }

        while (true) {
            turn++;
            trees.clear(); myTrolls.clear(); oppTrolls.clear();

            for (int i = 0; i < 6; i++) myInv[i] = in.nextInt();
            for (int i = 0; i < 6; i++) oppInv[i] = in.nextInt();

            int tc = in.nextInt();
            for (int i = 0; i < tc; i++) {
                String t = in.next();
                int type = t.equals("PLUM") ? 0 : t.equals("LEMON") ? 1 : t.equals("APPLE") ? 2 : 3;
                trees.add(new int[]{type, in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt()});
            }

            int trc = in.nextInt();
            for (int i = 0; i < trc; i++) {
                int id = in.nextInt(); int pl = in.nextInt();
                int[] tr = new int[]{id, in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(),
                        in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt()};
                if (pl == 0) myTrolls.add(tr); else oppTrolls.add(tr);
            }

            List<String> cmds = new ArrayList<>();
            int remaining = MAX_TURNS - turn;

            // --- TRAIN decision ---
            tryTrain(cmds, remaining);

            // --- Per-troll assignment ---
            // Sort: carrying trolls first so they drop/return before we assign idle ones
            myTrolls.sort((a, b) -> totalCarried(b) - totalCarried(a));

            // Track cells occupied/claimed by our trolls to avoid collisions
            Set<String> occupiedCells = new HashSet<>();
            for (int[] tr : myTrolls) occupiedCells.add(tr[1] + "," + tr[2]);
            int plantersThisTurn = 0;

            for (int[] tr : myTrolls) {
                String cmd = decideTrollAction(tr, remaining, occupiedCells, plantersThisTurn);
                if (cmd != null) {
                    cmds.add(cmd);
                    if (cmd.startsWith("PICK") || cmd.startsWith("PLANT")) plantersThisTurn++;
                    // Update occupied: troll moves from current pos to target
                    if (cmd.startsWith("MOVE")) {
                        String[] parts = cmd.split(" ");
                        occupiedCells.remove(tr[1] + "," + tr[2]);
                        occupiedCells.add(parts[2] + "," + parts[3]);
                    }
                }
            }

            System.out.println(cmds.isEmpty() ? "WAIT" : String.join(";", cmds));
        }
    }

    static int totalCarried(int[] tr) { return tr[7] + tr[8] + tr[9] + tr[10] + tr[11] + tr[12]; }
    static int remainCap(int[] tr) { return tr[4] - totalCarried(tr); }
    static int dist(int x1, int y1, int x2, int y2) { return Math.abs(x1-x2) + Math.abs(y1-y2); }
    static int distToShack(int x, int y) { return dist(x, y, myShackX, myShackY); }
    static boolean adjToShack(int x, int y) { return dist(x, y, myShackX, myShackY) == 1; }

    static int[] treeAt(int x, int y) {
        for (int[] t : trees) if (t[1] == x && t[2] == y) return t;
        return null;
    }

    static boolean hasTreeAt(int x, int y) { return treeAt(x, y) != null; }

    static int trainCost(int numTrolls, int attr) { return numTrolls + attr * attr; }

    static void tryTrain(List<String> cmds, int remaining) {
        // Loop to train multiple trolls per turn if affordable
        while (true) {
            int n = myTrolls.size() + cmds.size(); // count pending trains
            if (remaining < 30) return;
            if (n >= 4) return;

            int[][] configs = {{2,1,1,0}, {2,2,1,0}, {1,2,1,0}, {1,1,1,0}};
            int[] bestCfg = null;
            double bestScore = -1;

            for (int[] cfg : configs) {
                int pCost = trainCost(n, cfg[0]);
                int lCost = trainCost(n, cfg[1]);
                int aCost = trainCost(n, cfg[2]);
                int iCost = trainCost(n, cfg[3]);
                if (myInv[0] < pCost || myInv[1] < lCost || myInv[2] < aCost || myInv[4] < iCost) continue;

                int totalCost = pCost + lCost + aCost + iCost;

                if (turn <= 15) {
                    double score = cfg[0] * 3.0 + cfg[1] + cfg[2];
                    if (score > bestScore) { bestScore = score; bestCfg = cfg; }
                    continue;
                }

                double avgDist = avgTreeDistToShack();
                if (avgDist < 1) avgDist = 4;
                double cycleTime = 2.0 * avgDist / cfg[0] + 2;
                double cyclesLeft = remaining / cycleTime;
                double expectedFruits = cyclesLeft * Math.min(cfg[1], cfg[2]);
                if (expectedFruits > totalCost * 1.2) {
                    double score = expectedFruits - totalCost;
                    if (score > bestScore) { bestScore = score; bestCfg = cfg; }
                }
            }

            if (bestCfg == null) return;
            int pCost = trainCost(n, bestCfg[0]);
            int lCost = trainCost(n, bestCfg[1]);
            int aCost = trainCost(n, bestCfg[2]);
            int iCost = trainCost(n, bestCfg[3]);
            cmds.add("TRAIN " + bestCfg[0] + " " + bestCfg[1] + " " + bestCfg[2] + " " + bestCfg[3]);
            myInv[0] -= pCost; myInv[1] -= lCost; myInv[2] -= aCost; myInv[4] -= iCost;
            System.err.println("T" + turn + " TRAIN " + bestCfg[0] + "," + bestCfg[1] + "," + bestCfg[2] + "," + bestCfg[3]);
        }
    }

    static double avgTreeDistToShack() {
        if (trees.isEmpty()) return 5;
        double sum = 0;
        for (int[] t : trees) sum += distToShack(t[1], t[2]);
        return sum / trees.size();
    }

    static String decideTrollAction(int[] tr, int remaining, Set<String> occupied, int planters) {
        int id = tr[0], tx = tr[1], ty = tr[2];
        int speed = tr[3];
        int carried = totalCarried(tr);
        boolean earlyPlant = turn <= 25 && remaining > 50 && countTreesNearShack(4) < 6;

        // 1. On tree with fruits and have capacity → HARVEST
        int[] tree = treeAt(tx, ty);
        if (tree != null && tree[5] > 0 && remainCap(tr) > 0) {
            return "HARVEST " + id;
        }

        // 2. Carrying + on good empty cell near shack → PLANT
        if (carried > 0 && earlyPlant) {
            int dShack = distToShack(tx, ty);
            if (dShack >= 1 && dShack <= 3 && !hasTreeAt(tx, ty) && grid[ty][tx] == 0) {
                String ptype = bestPlantType(tr);
                if (ptype != null) {
                    System.err.println("T" + turn + " troll " + id + " PLANT " + ptype + " at " + tx + "," + ty);
                    return "PLANT " + id + " " + ptype;
                }
            }
        }

        // 3. Adjacent to shack and carrying
        if (adjToShack(tx, ty) && carried > 0) {
            // Early game: if carrying exactly 1, move to plant spot instead of dropping
            if (earlyPlant && carried == 1) {
                int[] plantCell = findPlantableCell(occupied);
                if (plantCell != null) {
                    return "MOVE " + id + " " + plantCell[0] + " " + plantCell[1];
                }
            }
            return "DROP " + id;
        }

        // 4. Carrying fruits → go home, optionally detour through plantable cell
        if (carried > 0) {
            if (earlyPlant && carried <= 2) {
                int[] plantCell = findPlantableOnPath(tx, ty, speed, occupied);
                if (plantCell != null) {
                    return "MOVE " + id + " " + plantCell[0] + " " + plantCell[1];
                }
            }
            int[] adj = bestAdjacentToShack(tx, ty, occupied);
            if (adj != null) return "MOVE " + id + " " + adj[0] + " " + adj[1];
            return "MOVE " + id + " " + myShackX + " " + myShackY;
        }

        // 5. Adjacent to shack, not carrying, early game → PICK seed to plant (max 1 planter)
        if (adjToShack(tx, ty) && earlyPlant && planters == 0) {
            int[] plantCell = findPlantableCell(occupied);
            if (plantCell != null) {
                String pickType = bestPickType();
                if (pickType != null) {
                    System.err.println("T" + turn + " troll " + id + " PICK " + pickType + " to plant");
                    return "PICK " + id + " " + pickType;
                }
            }
        }

        // 6. Find best tree to go to
        return findBestTree(tr, remaining, occupied);
    }

    static int[] findPlantableCell(Set<String> occupied) {
        for (int d = 1; d <= 2; d++) {
            for (int dx = -d; dx <= d; dx++) {
                for (int dy = -d; dy <= d; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != d) continue;
                    int x = myShackX + dx, y = myShackY + dy;
                    if (x >= 0 && x < width && y >= 0 && y < height
                            && grid[y][x] == 0 && !hasTreeAt(x, y)
                            && !occupied.contains(x + "," + y)) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        return null;
    }

    static String bestPickType() {
        String[] names = {"PLUM", "LEMON", "APPLE", "BANANA"};
        int bestType = -1; int minCount = Integer.MAX_VALUE;
        for (int type = 0; type < 4; type++) {
            if (myInv[type] >= 3) { // only pick if we have plenty
                int count = 0;
                for (int[] t : trees) if (t[0] == type && distToShack(t[1], t[2]) <= 4) count++;
                if (count < minCount) { minCount = count; bestType = type; }
            }
        }
        return bestType >= 0 ? names[bestType] : null;
    }

    static String bestPlantType(int[] tr) {
        String[] names = {"PLUM", "LEMON", "APPLE", "BANANA"};
        int bestType = -1; int minCount = Integer.MAX_VALUE;
        for (int type = 0; type < 4; type++) {
            if (tr[7 + type] > 0) {
                int count = 0;
                for (int[] t : trees) if (t[0] == type && distToShack(t[1], t[2]) <= 4) count++;
                if (count < minCount) { minCount = count; bestType = type; }
            }
        }
        return bestType >= 0 ? names[bestType] : null;
    }

    static int[] findPlantableOnPath(int fromX, int fromY, int speed, Set<String> occupied) {
        int directDist = distToShack(fromX, fromY);
        int[] best = null; int bestDetour = Integer.MAX_VALUE;
        for (int d = 1; d <= 2; d++) {
            for (int dx = -d; dx <= d; dx++) {
                for (int dy = -d; dy <= d; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != d) continue;
                    int x = myShackX + dx, y = myShackY + dy;
                    if (x < 0 || x >= width || y < 0 || y >= height) continue;
                    if (grid[y][x] != 0 || hasTreeAt(x, y)) continue;
                    if (occupied.contains(x + "," + y)) continue;
                    int totalPath = dist(fromX, fromY, x, y) + d;
                    int detour = totalPath - directDist;
                    if (detour <= 2 && detour < bestDetour) {
                        bestDetour = detour; best = new int[]{x, y};
                    }
                }
            }
        }
        return best;
    }

    static int countTreesNearShack(int maxDist) {
        int count = 0;
        for (int[] t : trees) if (distToShack(t[1], t[2]) <= maxDist) count++;
        return count;
    }

    static String findBestTree(int[] tr, int remaining, Set<String> occupied) {
        int id = tr[0], tx = tr[1], ty = tr[2], speed = tr[3];
        int cap = tr[4], hp = tr[5];

        double bestValue = -1;
        int[] bestTree = null;

        for (int[] tree : trees) {
            String key = tree[1] + "," + tree[2];
            // Skip if another troll is already on/heading to this cell
            if (occupied.contains(key)) continue;

            int fruits = tree[5];
            int cooldown = tree[6];
            int size = tree[3];
            
            int dToTree = dist(tx, ty, tree[1], tree[2]);
            int turnsToArrive = (int) Math.ceil((double) dToTree / speed);
            
            int availFruits = fruits;
            if (size == 4 && cooldown > 0 && cooldown <= turnsToArrive) {
                availFruits = Math.min(3, fruits + 1);
            }
            if (availFruits == 0 && size == 4 && cooldown <= turnsToArrive + 2) {
                availFruits = 1;
            }
            
            if (availFruits <= 0) continue;

            int harvestable = Math.min(availFruits, Math.min(cap, hp));
            int dBack = distToShack(tree[1], tree[2]);
            double cycleTime = (double) dToTree / speed + 1 + (double) dBack / speed + 1;
            if (cycleTime < 1) cycleTime = 1;

            if (turnsToArrive + 1 + Math.ceil((double) dBack / speed) + 1 > remaining) continue;

            double value = harvestable / cycleTime;
            
            int oppDist = dist(tree[1], tree[2], oppShackX, oppShackY);
            if (dBack < oppDist) value *= 1.15;

            if (value > bestValue) {
                bestValue = value;
                bestTree = tree;
            }
        }

        if (bestTree != null) {
            return "MOVE " + id + " " + bestTree[1] + " " + bestTree[2];
        }

        // No good tree found - consider going to a tree that will produce soon
        for (int[] tree : trees) {
            if (tree[3] == 4 && tree[6] <= 5) {
                String key = tree[1] + "," + tree[2];
                if (!occupied.contains(key)) {
                    return "MOVE " + id + " " + tree[1] + " " + tree[2];
                }
            }
        }

        // Nothing to do, move toward map center
        return "MOVE " + id + " " + (width / 2) + " " + (height / 2);
    }

    static int[] bestAdjacentToShack(int fromX, int fromY, Set<String> occupied) {
        int[] best = null; int bestDist = Integer.MAX_VALUE;
        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
        for (int[] d : dirs) {
            int ax = myShackX + d[0], ay = myShackY + d[1];
            if (ax >= 0 && ax < width && ay >= 0 && ay < height && grid[ay][ax] == 0
                    && !occupied.contains(ax + "," + ay)) {
                int dd = dist(fromX, fromY, ax, ay);
                if (dd < bestDist) { bestDist = dd; best = new int[]{ax, ay}; }
            }
        }
        return best;
    }
}
