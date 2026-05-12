package sim;

import java.util.List;

/**
 * Interface that all AI players must implement.
 * The simulator calls these methods each turn.
 */
public interface PlayerInterface {
    /**
     * Called once at the start with initial game state.
     * @param width grid width
     * @param height grid height
     * @param grid the grid (CellType[height][width])
     * @param myShackX x of own shack
     * @param myShackY y of own shack
     * @param oppShackX x of opponent shack
     * @param oppShackY y of opponent shack
     */
    void init(int width, int height, GameEngine.CellType[][] grid,
              int myShackX, int myShackY, int oppShackX, int oppShackY);

    /**
     * Called each turn. Must return a list of command strings.
     * Valid commands: MOVE id x y, HARVEST id, DROP id, PLANT id type,
     *                CHOP id, MINE id, PICK id type, TRAIN ms cc hp cp, WAIT, MSG text
     */
    List<String> play(int turn,
                      int[] myInventory, int[] oppInventory,
                      List<GameEngine.TreeInfo> trees,
                      List<GameEngine.TrollInfo> trolls);
}
