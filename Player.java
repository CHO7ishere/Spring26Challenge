import java.util.*;
import java.io.*;

public class Player {

    public static void main(String[] args) {
        new Player().run();
    }

    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    // Game state
    private int width;
    private int height;
    private String[] map;

    private int myPlums, myLemons, myApples, myBananas, myIron, myWood;
    private int oppPlums, oppLemons, oppApples, oppBananas, oppIron, oppWood;

    private List<Tree> trees = new ArrayList<>();
    private List<Troll> myTrolls = new ArrayList<>();
    private List<Troll> oppTrolls = new ArrayList<>();

    private int turn = 0;
    private boolean dumpScenarios = false;
    private String scenarioDir = "scenarii";

    public void run() {
        // Check for dump mode
        String dumpMode = System.getenv("DUMP_SCENARIOS");
        if (dumpMode != null && dumpMode.equals("1")) {
            dumpScenarios = true;
            new File(scenarioDir).mkdirs();
        }

        // Read initial input
        readInitialInput();

        // Dump initial state if enabled
        if (dumpScenarios) dumpScenario("initial");

        // Game loop
        while (true) {
            // Read turn input
            readTurnInput();

            // Dump turn state if enabled
            if (dumpScenarios) dumpScenario("turn_" + turn);

            // Compute output
            String output = computeOutput();

            // Write output
            System.out.println(output);

            turn++;
        }
    }

    private void dumpScenario(String prefix) {
        try {
            String filename = scenarioDir + "/" + prefix + "_" + System.currentTimeMillis() + ".txt";
            PrintWriter writer = new PrintWriter(new FileWriter(filename));

            // Write map
            writer.println(width + " " + height);
            for (String row : map) {
                writer.println(row);
            }

            // Write inventories
            writer.println(myPlums + " " + myLemons + " " + myApples + " " + myBananas + " " + myIron + " " + myWood);
            writer.println(oppPlums + " " + oppLemons + " " + oppApples + " " + oppBananas + " " + oppIron + " " + oppWood);

            // Write trees
            writer.println(trees.size());
            for (Tree t : trees) {
                writer.println(t.type + " " + t.x + " " + t.y + " " + t.size + " " + t.health + " " + t.fruits + " " + t.cooldown);
            }

            // Write trolls
            int totalTrolls = myTrolls.size() + oppTrolls.size();
            writer.println(totalTrolls);
            for (Troll t : myTrolls) {
                writer.println(t.id + " 0 " + t.x + " " + t.y + " " + t.movementSpeed + " " + t.carryCapacity + " " + t.harvestPower + " " + t.chopPower + " " + t.carryPlum + " " + t.carryLemon + " " + t.carryApple + " " + t.carryBanana + " " + t.carryIron + " " + t.carryWood);
            }
            for (Troll t : oppTrolls) {
                writer.println(t.id + " 1 " + t.x + " " + t.y + " " + t.movementSpeed + " " + t.carryCapacity + " " + t.harvestPower + " " + t.chopPower + " " + t.carryPlum + " " + t.carryLemon + " " + t.carryApple + " " + t.carryBanana + " " + t.carryIron + " " + t.carryWood);
            }

            writer.close();
            System.err.println("Dumped scenario to " + filename);
        } catch (IOException e) {
            System.err.println("Failed to dump scenario: " + e.getMessage());
        }
    }

    private void readInitialInput() {
        try {
            String line = reader.readLine();
            StringTokenizer st = new StringTokenizer(line);
            width = Integer.parseInt(st.nextToken());
            height = Integer.parseInt(st.nextToken());

            map = new String[height];
            for (int y = 0; y < height; y++) {
                map[y] = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readTurnInput() {
        try {
            // Read my inventory
            String line = reader.readLine();
            StringTokenizer st = new StringTokenizer(line);
            myPlums = Integer.parseInt(st.nextToken());
            myLemons = Integer.parseInt(st.nextToken());
            myApples = Integer.parseInt(st.nextToken());
            myBananas = Integer.parseInt(st.nextToken());
            myIron = Integer.parseInt(st.nextToken());
            myWood = Integer.parseInt(st.nextToken());

            // Read opponent inventory
            line = reader.readLine();
            st = new StringTokenizer(line);
            oppPlums = Integer.parseInt(st.nextToken());
            oppLemons = Integer.parseInt(st.nextToken());
            oppApples = Integer.parseInt(st.nextToken());
            oppBananas = Integer.parseInt(st.nextToken());
            oppIron = Integer.parseInt(st.nextToken());
            oppWood = Integer.parseInt(st.nextToken());

            // Read trees
            trees.clear();
            int treeCount = Integer.parseInt(reader.readLine());
            for (int i = 0; i < treeCount; i++) {
                line = reader.readLine();
                st = new StringTokenizer(line);
                String type = st.nextToken();
                int x = Integer.parseInt(st.nextToken());
                int y = Integer.parseInt(st.nextToken());
                int size = Integer.parseInt(st.nextToken());
                int health = Integer.parseInt(st.nextToken());
                int fruits = Integer.parseInt(st.nextToken());
                int cooldown = Integer.parseInt(st.nextToken());
                trees.add(new Tree(type, x, y, size, health, fruits, cooldown));
            }

            // Read trolls
            myTrolls.clear();
            oppTrolls.clear();
            int trollCount = Integer.parseInt(reader.readLine());
            for (int i = 0; i < trollCount; i++) {
                line = reader.readLine();
                st = new StringTokenizer(line);
                int id = Integer.parseInt(st.nextToken());
                int player = Integer.parseInt(st.nextToken());
                int x = Integer.parseInt(st.nextToken());
                int y = Integer.parseInt(st.nextToken());
                int movementSpeed = Integer.parseInt(st.nextToken());
                int carryCapacity = Integer.parseInt(st.nextToken());
                int harvestPower = Integer.parseInt(st.nextToken());
                int chopPower = Integer.parseInt(st.nextToken());
                int carryPlum = Integer.parseInt(st.nextToken());
                int carryLemon = Integer.parseInt(st.nextToken());
                int carryApple = Integer.parseInt(st.nextToken());
                int carryBanana = Integer.parseInt(st.nextToken());
                int carryIron = Integer.parseInt(st.nextToken());
                int carryWood = Integer.parseInt(st.nextToken());

                Troll troll = new Troll(id, x, y, movementSpeed, carryCapacity, harvestPower, chopPower,
                        carryPlum, carryLemon, carryApple, carryBanana, carryIron, carryWood);

                if (player == 0) {
                    myTrolls.add(troll);
                } else {
                    oppTrolls.add(troll);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String computeOutput() {
        StringBuilder sb = new StringBuilder();

        for (Troll troll : myTrolls) {
            // If carrying something and near shack, drop
            if (troll.getTotalCarry() > 0 && isNearShack(troll)) {
                if (sb.length() > 0) sb.append(";");
                sb.append("DROP ").append(troll.id);
                continue;
            }

            // If full, return to shack
            if (troll.getFreeCapacity() <= 0) {
                // Find nearest path to shack
                int[] shackPos = findMyShack();
                if (sb.length() > 0) sb.append(";");
                sb.append("MOVE ").append(troll.id).append(" ").append(shackPos[0]).append(" ").append(shackPos[1]);
                continue;
            }

            // Find nearest tree with fruits
            Tree nearestTree = findNearestTreeWithFruits(troll);
            if (nearestTree != null) {
                // If already at tree, harvest
                if (troll.x == nearestTree.x && troll.y == nearestTree.y) {
                    if (sb.length() > 0) sb.append(";");
                    sb.append("HARVEST ").append(troll.id);
                } else {
                    // Move towards tree
                    if (sb.length() > 0) sb.append(";");
                    sb.append("MOVE ").append(troll.id).append(" ").append(nearestTree.x).append(" ").append(nearestTree.y);
                }
            } else {
                // No trees with fruits, wait
                if (sb.length() > 0) sb.append(";");
                sb.append("WAIT");
            }
        }

        if (sb.length() == 0) {
            sb.append("WAIT");
        }

        return sb.toString();
    }

    private int[] findMyShack() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y].charAt(x) == '0') {
                    return new int[]{x, y};
                }
            }
        }
        return new int[]{0, 0};
    }

    private boolean isNearShack(Troll troll) {
        int[] shack = findMyShack();
        int dx = Math.abs(troll.x - shack[0]);
        int dy = Math.abs(troll.y - shack[1]);
        return (dx + dy) <= 1;
    }

    private Tree findNearestTreeWithFruits(Troll troll) {
        Tree best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Tree tree : trees) {
            if (tree.fruits > 0) {
                int dist = Math.abs(troll.x - tree.x) + Math.abs(troll.y - tree.y);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = tree;
                }
            }
        }
        return best;
    }

    // Helper classes
    static class Tree {
        String type;
        int x, y, size, health, fruits, cooldown;

        Tree(String type, int x, int y, int size, int health, int fruits, int cooldown) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.size = size;
            this.health = health;
            this.fruits = fruits;
            this.cooldown = cooldown;
        }
    }

    static class Troll {
        int id, x, y;
        int movementSpeed, carryCapacity, harvestPower, chopPower;
        int carryPlum, carryLemon, carryApple, carryBanana, carryIron, carryWood;

        Troll(int id, int x, int y, int movementSpeed, int carryCapacity, int harvestPower, int chopPower,
              int carryPlum, int carryLemon, int carryApple, int carryBanana, int carryIron, int carryWood) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.movementSpeed = movementSpeed;
            this.carryCapacity = carryCapacity;
            this.harvestPower = harvestPower;
            this.chopPower = chopPower;
            this.carryPlum = carryPlum;
            this.carryLemon = carryLemon;
            this.carryApple = carryApple;
            this.carryBanana = carryBanana;
            this.carryIron = carryIron;
            this.carryWood = carryWood;
        }

        int getTotalCarry() {
            return carryPlum + carryLemon + carryApple + carryBanana + carryIron + carryWood;
        }

        int getFreeCapacity() {
            return carryCapacity - getTotalCarry();
        }
    }
}
