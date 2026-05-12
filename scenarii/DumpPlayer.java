import java.util.*;

/**
 * A Codingame-compatible player that:
 * 1. Dumps ALL input to stderr in a replayable format (prefixed with "DUMP|")
 * 2. Plays basic greedy strategy (harvest nearest fruit tree, drop at shack)
 *
 * To extract game data: submit this on Codingame, then parse stderr lines starting with "DUMP|"
 * to reconstruct the full game state for local replay in the simulator.
 *
 * Dump format:
 *   DUMP|INIT|width height
 *   DUMP|MAP|<grid_line>          (one per row)
 *   DUMP|TURN|<turn_number>
 *   DUMP|INV|MY|plums lemons apples bananas 0 0
 *   DUMP|INV|OPP|plums lemons apples bananas 0 0
 *   DUMP|TREES|count
 *   DUMP|TREE|type x y size health fruits cooldown
 *   DUMP|TROLLS|count
 *   DUMP|TROLL|id player x y ms cc hp cp cPlum cLemon cApple cBanana 0 0
 *   DUMP|CMD|<commands_sent>
 */
class DumpPlayer {

    static int width, height;
    static int myShackX, myShackY;
    static int turn = 0;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // --- Initial input ---
        width = in.nextInt();
        height = in.nextInt();
        if (in.hasNextLine()) in.nextLine();
        System.err.println("DUMP|INIT|" + width + " " + height);

        char[][] grid = new char[height][width];
        for (int y = 0; y < height; y++) {
            String line = in.nextLine();
            System.err.println("DUMP|MAP|" + line);
            for (int x = 0; x < line.length(); x++) {
                grid[y][x] = line.charAt(x);
                if (line.charAt(x) == '0') { myShackX = x; myShackY = y; }
            }
        }

        // --- Game loop ---
        while (true) {
            turn++;
            System.err.println("DUMP|TURN|" + turn);

            // My inventory
            int myPlum = in.nextInt(), myLemon = in.nextInt();
            int myApple = in.nextInt(), myBanana = in.nextInt();
            int r1 = in.nextInt(), r2 = in.nextInt();
            System.err.println("DUMP|INV|MY|" + myPlum + " " + myLemon + " " + myApple + " " + myBanana + " " + r1 + " " + r2);

            // Opponent inventory
            int oppPlum = in.nextInt(), oppLemon = in.nextInt();
            int oppApple = in.nextInt(), oppBanana = in.nextInt();
            int or1 = in.nextInt(), or2 = in.nextInt();
            System.err.println("DUMP|INV|OPP|" + oppPlum + " " + oppLemon + " " + oppApple + " " + oppBanana + " " + or1 + " " + or2);

            // Trees
            int treeCount = in.nextInt();
            System.err.println("DUMP|TREES|" + treeCount);
            int[][] treeData = new int[treeCount][6]; // x,y,size,health,fruits,cooldown
            String[] treeTypes = new String[treeCount];
            for (int i = 0; i < treeCount; i++) {
                String type = in.next();
                int x = in.nextInt(), y = in.nextInt();
                int size = in.nextInt(), health = in.nextInt();
                int fruits = in.nextInt(), cooldown = in.nextInt();
                treeTypes[i] = type;
                treeData[i] = new int[]{x, y, size, health, fruits, cooldown};
                System.err.println("DUMP|TREE|" + type + " " + x + " " + y + " " + size + " " + health + " " + fruits + " " + cooldown);
            }

            // Trolls
            int trollCount = in.nextInt();
            System.err.println("DUMP|TROLLS|" + trollCount);
            int[][] myTrolls = new int[trollCount][14]; // only mine
            int myTrollCount = 0;
            for (int i = 0; i < trollCount; i++) {
                int id = in.nextInt(), player = in.nextInt();
                int x = in.nextInt(), y = in.nextInt();
                int ms = in.nextInt(), cc = in.nextInt();
                int hp = in.nextInt(), cp = in.nextInt();
                int cP = in.nextInt(), cL = in.nextInt();
                int cA = in.nextInt(), cB = in.nextInt();
                int tr1 = in.nextInt(), tr2 = in.nextInt();
                System.err.println("DUMP|TROLL|" + id + " " + player + " " + x + " " + y +
                        " " + ms + " " + cc + " " + hp + " " + cp +
                        " " + cP + " " + cL + " " + cA + " " + cB + " " + tr1 + " " + tr2);
                if (player == 0) {
                    myTrolls[myTrollCount++] = new int[]{id, x, y, ms, cc, hp, cp, cP, cL, cA, cB, tr1, tr2, cc - cP - cL - cA - cB - tr1 - tr2};
                }
            }

            // --- Basic greedy strategy ---
            List<String> commands = new ArrayList<>();
            Set<String> usedTrees = new HashSet<>();

            for (int t = 0; t < myTrollCount; t++) {
                int id = myTrolls[t][0], tx = myTrolls[t][1], ty = myTrolls[t][2];
                int speed = myTrolls[t][3], cap = myTrolls[t][4];
                int carried = myTrolls[t][7] + myTrolls[t][8] + myTrolls[t][9] + myTrolls[t][10] + myTrolls[t][11] + myTrolls[t][12];
                int remaining = myTrolls[t][13];

                // On a fruit tree and can carry -> harvest
                boolean harvested = false;
                for (int i = 0; i < treeCount; i++) {
                    if (treeData[i][0] == tx && treeData[i][1] == ty && treeData[i][4] > 0 && remaining > 0) {
                        commands.add("HARVEST " + id);
                        harvested = true;
                        break;
                    }
                }
                if (harvested) continue;

                // Adjacent to shack and carrying -> drop
                if (Math.abs(tx - myShackX) + Math.abs(ty - myShackY) == 1 && carried > 0) {
                    commands.add("DROP " + id);
                    continue;
                }

                // Full -> go home
                if (remaining <= 0 && carried > 0) {
                    commands.add("MOVE " + id + " " + myShackX + " " + myShackY);
                    continue;
                }

                // Find closest fruit tree
                int bestDist = Integer.MAX_VALUE;
                int bestIdx = -1;
                for (int i = 0; i < treeCount; i++) {
                    if (treeData[i][4] <= 0) continue;
                    String key = treeData[i][0] + "," + treeData[i][1];
                    if (usedTrees.contains(key)) continue;
                    int d = Math.abs(treeData[i][0] - tx) + Math.abs(treeData[i][1] - ty);
                    if (d < bestDist) { bestDist = d; bestIdx = i; }
                }

                if (bestIdx >= 0) {
                    usedTrees.add(treeData[bestIdx][0] + "," + treeData[bestIdx][1]);
                    commands.add("MOVE " + id + " " + treeData[bestIdx][0] + " " + treeData[bestIdx][1]);
                } else if (carried > 0) {
                    commands.add("MOVE " + id + " " + myShackX + " " + myShackY);
                } else {
                    commands.add("WAIT");
                }
            }

            String output = commands.isEmpty() ? "WAIT" : String.join(";", commands);
            System.err.println("DUMP|CMD|" + output);
            System.out.println(output);
        }
    }
}
