package sim;

import java.util.*;

public class GameEngine {
    public enum CellType { GRASS, SHACK_0, SHACK_1, WATER, ROCK, IRON_CELL }
    public enum TreeType { PLUM, LEMON, APPLE, BANANA }

    public static class TreeInfo {
        public TreeType type;
        public int x, y, size, health, fruits, cooldown;
        public TreeInfo(TreeType t, int x, int y, int s, int h, int f, int c) {
            type = t; this.x = x; this.y = y; size = s; health = h; fruits = f; cooldown = c;
        }
    }

    public static class TrollInfo {
        public int id, player, x, y, movementSpeed, carryCapacity, harvestPower, chopPower;
        public int carryPlum, carryLemon, carryApple, carryBanana, carryIron, carryWood;
        public TrollInfo(int id, int p, int x, int y, int ms, int cc, int hp, int cp,
                         int cP, int cL, int cA, int cB, int cI, int cW) {
            this.id = id; player = p; this.x = x; this.y = y;
            movementSpeed = ms; carryCapacity = cc; harvestPower = hp; chopPower = cp;
            carryPlum = cP; carryLemon = cL; carryApple = cA; carryBanana = cB;
            carryIron = cI; carryWood = cW;
        }
    }

    // --- Internal data ---
    static class TD {
        TreeType type; int x, y, size, health, fruits, cooldown;
        TD(TreeType t, int x, int y, int s, int h, int f, int c) {
            type = t; this.x = x; this.y = y; size = s; health = h; fruits = f; cooldown = c;
        }
        TD copy() { return new TD(type, x, y, size, health, fruits, cooldown); }
    }

    static class TrD {
        int id, player, x, y, ms, cc, hp, cp;
        int[] carry = new int[6]; // 0=plum,1=lemon,2=apple,3=banana,4=iron,5=wood
        boolean leftSpawn;
        TrD(int id, int p, int x, int y, int ms, int cc, int hp, int cp) {
            this.id = id; player = p; this.x = x; this.y = y;
            this.ms = ms; this.cc = cc; this.hp = hp; this.cp = cp;
        }
        int totalCarry() { return carry[0] + carry[1] + carry[2] + carry[3] + carry[4] + carry[5]; }
        int remCap() { return cc - totalCarry(); }
    }

    // --- State ---
    private int width, height;
    private CellType[][] grid;
    private int[] shackX = new int[2], shackY = new int[2];
    private int[][] inv = new int[2][6]; // [player][PLUM=0,LEMON=1,APPLE=2,BANANA=3,IRON=4,WOOD=5]
    private List<TD> trees = new ArrayList<>();
    private List<TrD> trolls = new ArrayList<>();
    private int turn = 0, maxTurns = 300, nextId = 0;
    private boolean verbose = false;

    // Tree type constants indexed by TreeType ordinal: PLUM=0, LEMON=1, APPLE=2, BANANA=3
    private static final int[] TREE_COOLDOWN =       {8, 8, 9, 6};
    private static final int[] TREE_COOLDOWN_WATER = {3, 3, 2, 4};
    private static final int[][] TREE_HEALTH = {
        {6, 8, 10, 12},   // PLUM: size 1-4
        {6, 8, 10, 12},   // LEMON
        {11, 14, 17, 20},  // APPLE
        {3, 4, 5, 6}       // BANANA
    };

    private static final int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    private boolean isNearWater(int x, int y) {
        for (int[] d : DIRS) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx] == CellType.WATER)
                return true;
        }
        return false;
    }

    // --- Accessors ---
    public void setVerbose(boolean v) { verbose = v; }
    public void setMaxTurns(int t) { maxTurns = t; }
    public int getTurn() { return turn; }
    public boolean isOver() { return turn >= maxTurns; }
    public int getScore(int p) { return inv[p][0] + inv[p][1] + inv[p][2] + inv[p][3] + inv[p][5] * 4; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getShackX(int p) { return shackX[p]; }
    public int getShackY(int p) { return shackY[p]; }

    // --- Initialization ---
    public void initMap(String[] lines) {
        height = lines.length;
        width = lines[0].length();
        grid = new CellType[height][width];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                char c = lines[y].charAt(x);
                if (c == '0')      { grid[y][x] = CellType.SHACK_0; shackX[0] = x; shackY[0] = y; }
                else if (c == '1') { grid[y][x] = CellType.SHACK_1; shackX[1] = x; shackY[1] = y; }
                else if (c == '~') { grid[y][x] = CellType.WATER; }
                else if (c == '#') { grid[y][x] = CellType.ROCK; }
                else if (c == '+') { grid[y][x] = CellType.IRON_CELL; }
                else               { grid[y][x] = CellType.GRASS; }
            }
    }

    public void addTree(TreeType t, int x, int y, int size, int health, int fruits, int cd) {
        trees.add(new TD(t, x, y, size, health, fruits, cd));
    }

    public void spawnTroll(int player, int ms, int cc, int hp, int cp) {
        TrD t = new TrD(nextId++, player, shackX[player], shackY[player], ms, cc, hp, cp);
        trolls.add(t);
    }

    // --- View methods (player perspective) ---
    public CellType[][] getGridFor(int forPlayer) {
        CellType[][] g = new CellType[height][width];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                CellType c = grid[y][x];
                if (forPlayer == 0) g[y][x] = c;
                else if (c == CellType.SHACK_0) g[y][x] = CellType.SHACK_1;
                else if (c == CellType.SHACK_1) g[y][x] = CellType.SHACK_0;
                else g[y][x] = c;
            }
        return g;
    }

    public String getGridString(int forPlayer) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                CellType c = grid[y][x];
                if (c == CellType.GRASS) sb.append('.');
                else if (c == CellType.WATER) sb.append('~');
                else if (c == CellType.ROCK) sb.append('#');
                else if (c == CellType.IRON_CELL) sb.append('+');
                else if ((c == CellType.SHACK_0 && forPlayer == 0) || (c == CellType.SHACK_1 && forPlayer == 1))
                    sb.append('0');
                else sb.append('1');
            }
            if (y < height - 1) sb.append('\n');
        }
        return sb.toString();
    }

    public List<TreeInfo> getTreeInfos() {
        List<TreeInfo> r = new ArrayList<>();
        for (TD t : trees) r.add(new TreeInfo(t.type, t.x, t.y, t.size, t.health, t.fruits, t.cooldown));
        return r;
    }

    public List<TrollInfo> getTrollInfos(int forPlayer) {
        List<TrollInfo> r = new ArrayList<>();
        for (TrD t : trolls) {
            int vp = (t.player == forPlayer) ? 0 : 1;
            r.add(new TrollInfo(t.id, vp, t.x, t.y, t.ms, t.cc, t.hp, t.cp,
                    t.carry[0], t.carry[1], t.carry[2], t.carry[3], t.carry[4], t.carry[5]));
        }
        return r;
    }

    public int[] getInv(int player) { return inv[player].clone(); }

    // --- Turn execution ---
    public void executeTurn(List<String> p0cmds, List<String> p1cmds) {
        turn++;
        List<String[]> c0 = parseCmds(p0cmds);
        List<String[]> c1 = parseCmds(p1cmds);

        processMoves(c0, 0);
        processMoves(c1, 1);
        processHarvest(c0, c1);
        processPlant(c0, c1);
        processChop(c0, c1);
        processPick(c0, 0);
        processPick(c1, 1);
        processTrain(c0, 0);
        processTrain(c1, 1);
        processDrop(c0, 0);
        processDrop(c1, 1);
        processMine(c0, 0);
        processMine(c1, 1);
        tickTrees();
        trees.removeIf(t -> t.health <= 0);
    }

    private List<String[]> parseCmds(List<String> cmds) {
        List<String[]> r = new ArrayList<>();
        for (String cmd : cmds)
            for (String c : cmd.split(";")) {
                String[] parts = c.trim().split("\\s+");
                if (parts.length > 0 && !parts[0].isEmpty()) r.add(parts);
            }
        return r;
    }

    private TrD findTroll(int id, int player) {
        for (TrD t : trolls) if (t.id == id && t.player == player) return t;
        return null;
    }

    private TD findTreeAt(int x, int y) {
        for (TD t : trees) if (t.x == x && t.y == y) return t;
        return null;
    }

    private boolean hasSameTeamTrollAt(int x, int y, int player, TrD exclude) {
        for (TrD t : trolls)
            if (t != exclude && t.player == player && t.x == x && t.y == y) return true;
        return false;
    }

    // --- TRAIN ---
    private void processTrain(List<String[]> cmds, int player) {
        for (String[] c : cmds) {
            if (!c[0].equals("TRAIN") || c.length < 5) continue;
            int ms = Integer.parseInt(c[1]), cc = Integer.parseInt(c[2]);
            int hp = Integer.parseInt(c[3]), cp = Integer.parseInt(c[4]);
            int tc = 0;
            for (TrD t : trolls) if (t.player == player) tc++;
            int pCost = tc + ms * ms, lCost = tc + cc * cc, aCost = tc + hp * hp, iCost = tc + cp * cp;
            if (inv[player][0] >= pCost && inv[player][1] >= lCost && inv[player][2] >= aCost && inv[player][4] >= iCost) {
                inv[player][0] -= pCost; inv[player][1] -= lCost; inv[player][2] -= aCost; inv[player][4] -= iCost;
                TrD t = new TrD(nextId++, player, shackX[player], shackY[player], ms, cc, hp, cp);
                trolls.add(t);
                if (verbose) log("P" + player + " trained troll " + t.id);
            }
        }
    }

    // --- MOVE ---
    private void processMoves(List<String[]> cmds, int player) {
        for (String[] c : cmds) {
            if (!c[0].equals("MOVE") || c.length < 4) continue;
            int id = Integer.parseInt(c[1]);
            int tx = Integer.parseInt(c[2]), ty = Integer.parseInt(c[3]);
            TrD troll = findTroll(id, player);
            if (troll == null) continue;
            int[] dest = computeMove(troll, tx, ty);
            if (dest[0] != troll.x || dest[1] != troll.y) troll.leftSpawn = true;
            troll.x = dest[0]; troll.y = dest[1];
        }
    }

    private int[] computeMove(TrD troll, int tx, int ty) {
        int fx = troll.x, fy = troll.y;
        if (fx == tx && fy == ty) return new int[]{fx, fy};

        // BFS from troll position
        int[][] dist = new int[height][width];
        int[][][] prev = new int[height][width][];
        for (int[] r : dist) Arrays.fill(r, Integer.MAX_VALUE);
        dist[fy][fx] = 0;
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{fx, fy});

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int cx = cur[0], cy = cur[1];
            for (int[] d : DIRS) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                if (grid[ny][nx] != CellType.GRASS) continue;
                if (dist[ny][nx] <= dist[cy][cx] + 1) continue;
                dist[ny][nx] = dist[cy][cx] + 1;
                prev[ny][nx] = new int[]{cx, cy};
                q.add(new int[]{nx, ny});
            }
        }

        // Find best destination: target if reachable, else closest reachable cell to target
        int destX = fx, destY = fy;
        if (dist[ty][tx] != Integer.MAX_VALUE) {
            destX = tx; destY = ty;
        } else {
            int bestManhattan = Integer.MAX_VALUE;
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    if (dist[y][x] != Integer.MAX_VALUE) {
                        int md = Math.abs(x - tx) + Math.abs(y - ty);
                        if (md < bestManhattan || (md == bestManhattan && dist[y][x] < dist[destY][destX])) {
                            bestManhattan = md; destX = x; destY = y;
                        }
                    }
        }

        // Trace path back from dest
        if (dist[destY][destX] == Integer.MAX_VALUE || dist[destY][destX] == 0)
            return new int[]{fx, fy};

        List<int[]> path = new ArrayList<>();
        int cx = destX, cy = destY;
        while (cx != fx || cy != fy) {
            path.add(new int[]{cx, cy});
            int[] p = prev[cy][cx];
            cx = p[0]; cy = p[1];
        }
        Collections.reverse(path);

        // Move up to movementSpeed steps, checking collision
        int steps = Math.min(troll.ms, path.size());
        for (int i = steps - 1; i >= 0; i--) {
            int[] pos = path.get(i);
            if (!hasSameTeamTrollAt(pos[0], pos[1], troll.player, troll))
                return pos;
        }
        return new int[]{fx, fy};
    }

    // --- HARVEST (simultaneous) ---
    private void processHarvest(List<String[]> c0, List<String[]> c1) {
        Map<String, List<TrD>> harvesters = new LinkedHashMap<>();
        collectHarvesters(c0, 0, harvesters);
        collectHarvesters(c1, 1, harvesters);

        for (Map.Entry<String, List<TrD>> e : harvesters.entrySet()) {
            String[] xy = e.getKey().split(",");
            TD tree = findTreeAt(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
            if (tree == null || tree.fruits == 0) continue;

            List<TrD> hList = e.getValue();
            int[] maxTake = new int[hList.size()];
            int[] taken = new int[hList.size()];
            for (int i = 0; i < hList.size(); i++) {
                TrD t = hList.get(i);
                maxTake[i] = Math.min(t.hp, t.remCap());
            }

            while (tree.fruits > 0) {
                List<Integer> active = new ArrayList<>();
                for (int i = 0; i < hList.size(); i++)
                    if (taken[i] < maxTake[i]) active.add(i);
                if (active.isEmpty()) break;

                if (tree.fruits == 1 && active.size() > 1) {
                    // Duplicate last fruit
                    int idx = tree.type.ordinal();
                    for (int i : active) { hList.get(i).carry[idx]++; taken[i]++; }
                    tree.fruits = 0;
                } else {
                    for (int i : active) {
                        if (tree.fruits <= 0) break;
                        hList.get(i).carry[tree.type.ordinal()]++;
                        taken[i]++;
                        tree.fruits--;
                    }
                }
            }
        }
    }

    private void collectHarvesters(List<String[]> cmds, int player, Map<String, List<TrD>> map) {
        for (String[] c : cmds) {
            if (!c[0].equals("HARVEST") || c.length < 2) continue;
            TrD t = findTroll(Integer.parseInt(c[1]), player);
            if (t == null) continue;
            String key = t.x + "," + t.y;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
    }

    // --- DROP ---
    private void processDrop(List<String[]> cmds, int player) {
        for (String[] c : cmds) {
            if (!c[0].equals("DROP") || c.length < 2) continue;
            TrD t = findTroll(Integer.parseInt(c[1]), player);
            if (t == null) continue;
            if (Math.abs(t.x - shackX[player]) + Math.abs(t.y - shackY[player]) != 1) continue;
            for (int i = 0; i < 6; i++) { inv[player][i] += t.carry[i]; t.carry[i] = 0; }
            if (verbose) log("P" + player + " troll " + t.id + " dropped items");
        }
    }

    // --- PLANT (simultaneous) ---
    private void processPlant(List<String[]> c0, List<String[]> c1) {
        // Group by cell
        Map<String, List<Object[]>> planters = new LinkedHashMap<>(); // key -> list of [TrD, TreeType]
        collectPlanters(c0, 0, planters);
        collectPlanters(c1, 1, planters);

        for (Map.Entry<String, List<Object[]>> e : planters.entrySet()) {
            String[] xy = e.getKey().split(",");
            int px = Integer.parseInt(xy[0]), py = Integer.parseInt(xy[1]);
            if (findTreeAt(px, py) != null) continue; // already a tree
            if (grid[py][px] != CellType.GRASS) continue;

            List<Object[]> pList = e.getValue();
            // Check all same type
            TreeType plantType = (TreeType) pList.get(0)[1];
            boolean allSame = true;
            for (Object[] o : pList)
                if (o[1] != plantType) { allSame = false; break; }
            if (!allSame) continue; // mixed types -> nothing

            // All lose 1 seed, tree planted
            int idx = plantType.ordinal();
            boolean valid = true;
            for (Object[] o : pList) {
                TrD t = (TrD) o[0];
                if (t.carry[idx] <= 0) { valid = false; break; }
            }
            if (!valid) continue;

            for (Object[] o : pList) ((TrD) o[0]).carry[idx]--;
            int ti = plantType.ordinal();
            boolean water = isNearWater(px, py);
            int plantHp = TREE_HEALTH[ti][0];
            int plantCd = water ? TREE_COOLDOWN_WATER[ti] : TREE_COOLDOWN[ti];
            trees.add(new TD(plantType, px, py, 1, plantHp, 0, plantCd));
            if (verbose) log("Planted " + plantType + " at " + px + "," + py);
        }
    }

    private void collectPlanters(List<String[]> cmds, int player, Map<String, List<Object[]>> map) {
        for (String[] c : cmds) {
            if (!c[0].equals("PLANT") || c.length < 3) continue;
            TrD t = findTroll(Integer.parseInt(c[1]), player);
            if (t == null) continue;
            TreeType tt = TreeType.valueOf(c[2]);
            String key = t.x + "," + t.y;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(new Object[]{t, tt});
        }
    }

    // --- CHOP (simultaneous) ---
    private void processChop(List<String[]> c0, List<String[]> c1) {
        Map<String, List<TrD>> choppers = new LinkedHashMap<>();
        collectChoppers(c0, 0, choppers);
        collectChoppers(c1, 1, choppers);

        for (Map.Entry<String, List<TrD>> e : choppers.entrySet()) {
            String[] xy = e.getKey().split(",");
            TD tree = findTreeAt(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
            if (tree == null) continue;

            List<TrD> cList = e.getValue();
            for (TrD t : cList) tree.health -= t.cp;

            if (tree.health <= 0) {
                int wood = tree.size;
                int[] maxTake = new int[cList.size()];
                int[] taken = new int[cList.size()];
                for (int i = 0; i < cList.size(); i++)
                    maxTake[i] = cList.get(i).remCap();

                while (wood > 0) {
                    List<Integer> active = new ArrayList<>();
                    for (int i = 0; i < cList.size(); i++)
                        if (taken[i] < maxTake[i]) active.add(i);
                    if (active.isEmpty()) break;

                    if (wood == 1 && active.size() > 1) {
                        for (int i : active) { cList.get(i).carry[5]++; taken[i]++; }
                        wood = 0;
                    } else {
                        for (int i : active) {
                            if (wood <= 0) break;
                            cList.get(i).carry[5]++;
                            taken[i]++;
                            wood--;
                        }
                    }
                }
                if (verbose) log("Chopped " + tree.type + " at " + tree.x + "," + tree.y);
            }
        }
    }

    private void collectChoppers(List<String[]> cmds, int player, Map<String, List<TrD>> map) {
        for (String[] c : cmds) {
            if (!c[0].equals("CHOP") || c.length < 2) continue;
            TrD t = findTroll(Integer.parseInt(c[1]), player);
            if (t == null) continue;
            String key = t.x + "," + t.y;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
    }

    // --- PICK ---
    private void processPick(List<String[]> cmds, int player) {
        for (String[] c : cmds) {
            if (!c[0].equals("PICK") || c.length < 3) continue;
            TrD t = findTroll(Integer.parseInt(c[1]), player);
            if (t == null) continue;
            if (Math.abs(t.x - shackX[player]) + Math.abs(t.y - shackY[player]) > 1) continue;
            TreeType tt = TreeType.valueOf(c[2]);
            int idx = tt.ordinal();
            if (inv[player][idx] > 0 && t.remCap() > 0) {
                inv[player][idx]--;
                t.carry[idx]++;
            }
        }
    }

    // --- MINE ---
    private void processMine(List<String[]> cmds, int player) {
        for (String[] c : cmds) {
            if (!c[0].equals("MINE") || c.length < 2) continue;
            TrD t = findTroll(Integer.parseInt(c[1]), player);
            if (t == null) continue;
            boolean adjIron = false;
            for (int[] d : DIRS) {
                int nx = t.x + d[0], ny = t.y + d[1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx] == CellType.IRON_CELL) {
                    adjIron = true; break;
                }
            }
            if (!adjIron) continue;
            int amount = Math.min(t.cp, t.remCap());
            t.carry[4] += amount;
            if (verbose) log("P" + player + " troll " + t.id + " mined " + amount + " iron");
        }
    }

    // --- Tree tick ---
    private void tickTrees() {
        for (TD t : trees) {
            if (t.cooldown > 0) {
                t.cooldown--;
                if (t.cooldown == 0) {
                    int ti = t.type.ordinal();
                    boolean water = isNearWater(t.x, t.y);
                    int cd = water ? TREE_COOLDOWN_WATER[ti] : TREE_COOLDOWN[ti];
                    if (t.size < 4) {
                        int oldHp = TREE_HEALTH[ti][t.size - 1];
                        t.size++;
                        int newHp = TREE_HEALTH[ti][t.size - 1];
                        t.health += (newHp - oldHp);
                        t.cooldown = cd;
                    } else {
                        if (t.fruits < 3) t.fruits++;
                        t.cooldown = cd;
                    }
                }
            }
        }
    }

    private void log(String msg) { System.err.println("[Engine T" + turn + "] " + msg); }

    // --- Default map generation ---
    public static GameEngine createDefault() {
        return createDefault(16, 8, new Random());
    }

    public static GameEngine createDefault(int w, int h, Random rng) {
        GameEngine e = new GameEngine();
        // Build symmetric map
        String[] map = new String[h];
        char[][] chars = new char[h][w];
        for (int y = 0; y < h; y++) Arrays.fill(chars[y], '.');
        // Place shacks symmetrically
        int sx0 = 1, sy0 = h / 2;
        int sx1 = w - 2, sy1 = h / 2;
        chars[sy0][sx0] = '0';
        chars[sy1][sx1] = '1';
        // Add some water cells symmetrically
        int waterPairs = 1 + rng.nextInt(3);
        for (int i = 0; i < waterPairs; i++) {
            int wx, wy;
            do { wx = 1 + rng.nextInt(w / 2 - 1); wy = rng.nextInt(h); }
            while (chars[wy][wx] != '.');
            chars[wy][wx] = '~';
            chars[wy][w - 1 - wx] = '~';
        }
        // Add iron cells symmetrically
        int ironPairs = 1 + rng.nextInt(2);
        for (int i = 0; i < ironPairs; i++) {
            int ix, iy;
            do { ix = 1 + rng.nextInt(w / 2 - 1); iy = rng.nextInt(h); }
            while (chars[iy][ix] != '.');
            chars[iy][ix] = '+';
            chars[iy][w - 1 - ix] = '+';
        }
        for (int y = 0; y < h; y++) map[y] = new String(chars[y]);
        e.initMap(map);

        // Place symmetric trees
        TreeType[] types = TreeType.values();
        int treePairs = 3 + rng.nextInt(4); // 3-6 pairs
        Set<String> used = new HashSet<>();
        used.add(sx0 + "," + sy0);
        used.add(sx1 + "," + sy1);
        for (int i = 0; i < treePairs; i++) {
            int tx, ty;
            do {
                tx = 2 + rng.nextInt(w / 2 - 3);
                ty = rng.nextInt(h);
            } while (used.contains(tx + "," + ty) || chars[ty][tx] != '.' || chars[ty][w - 1 - tx] != '.');
            int mx = w - 1 - tx;
            used.add(tx + "," + ty);
            used.add(mx + "," + ty);
            TreeType tt = types[rng.nextInt(types.length)];
            int size = 2 + rng.nextInt(3); // 2-4
            int fruits = (size == 4) ? 1 + rng.nextInt(3) : 0;
            int cd = 2 + rng.nextInt(5);
            int hp = size * 3;
            e.addTree(tt, tx, ty, size, hp, fruits, cd);
            e.addTree(tt, mx, ty, size, hp, fruits, cd);
        }

        // Spawn initial trolls (1 per player: ms=1, cc=1, hp=1, cp=0)
        e.spawnTroll(0, 1, 1, 1, 0);
        e.spawnTroll(1, 1, 1, 1, 0);
        return e;
    }

    // --- Formatted state dump (Codingame protocol) ---
    public String formatInitFor(int player) {
        StringBuilder sb = new StringBuilder();
        sb.append(width).append(' ').append(height).append('\n');
        sb.append(getGridString(player));
        return sb.toString();
    }

    public String formatTurnFor(int player) {
        StringBuilder sb = new StringBuilder();
        int me = player, opp = 1 - player;
        // My inventory
        sb.append(inv[me][0]).append(' ').append(inv[me][1]).append(' ')
          .append(inv[me][2]).append(' ').append(inv[me][3]).append(' ')
          .append(inv[me][4]).append(' ').append(inv[me][5]).append('\n');
        // Opp inventory
        sb.append(inv[opp][0]).append(' ').append(inv[opp][1]).append(' ')
          .append(inv[opp][2]).append(' ').append(inv[opp][3]).append(' ')
          .append(inv[opp][4]).append(' ').append(inv[opp][5]).append('\n');
        // Trees
        sb.append(trees.size()).append('\n');
        for (TD t : trees)
            sb.append(t.type).append(' ').append(t.x).append(' ').append(t.y).append(' ')
              .append(t.size).append(' ').append(t.health).append(' ')
              .append(t.fruits).append(' ').append(t.cooldown).append('\n');
        // Trolls
        sb.append(trolls.size()).append('\n');
        for (TrD t : trolls) {
            int vp = (t.player == player) ? 0 : 1;
            sb.append(t.id).append(' ').append(vp).append(' ')
              .append(t.x).append(' ').append(t.y).append(' ')
              .append(t.ms).append(' ').append(t.cc).append(' ')
              .append(t.hp).append(' ').append(t.cp).append(' ')
              .append(t.carry[0]).append(' ').append(t.carry[1]).append(' ')
              .append(t.carry[2]).append(' ').append(t.carry[3]).append(' ')
              .append(t.carry[4]).append(' ').append(t.carry[5]).append('\n');
        }
        return sb.toString();
    }
}
