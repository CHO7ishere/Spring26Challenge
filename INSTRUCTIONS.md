# Codingame Spring Challenge 2026 - Troll Farm

## Overview
This is a template and guide for building a player for the CodinGame Spring Challenge 2026 (Troll Farm).

## Quick Start

### Files Created
- **Player.java** - Main player code (ready for Codingame submission)
- **launcher.ps1** - Local game launcher using the referee
- **rules.md** - Game rules with clarifications from referee code
- **scenarii/** - Directory for saved game scenarios

### Running Locally

```powershell
# Run two players against each other
.\launcher.ps1 -Player1 "java Player" -Player2 "java Player" -Seed 1

# Run with visualizer
.\launcher.ps1 -Player1 "java Player" -Player2 "java Player" -Seed 1 -Watch
```

### Environment Variables
- `DUMP_SCENARIOS=1` - Enable scenario dumping to `scenarii/` folder

```powershell
$env:DUMP_SCENARIOS="1"; java Player
```

## Player Structure

### Class Requirements
- Class must be named exactly `Player`
- Must have a `main` method that reads from stdin and writes to stdout
- Single line output with commands separated by `;`

### Input Format
1. First turn: `width height` then `height` lines of map
2. Each turn:
   - Your inventory: `plums lemons apples bananas iron wood`
   - Opponent inventory: same format
   - Tree count
   - Tree lines: `type x y size health fruits cooldown`
   - Troll count
   - Troll lines: `id player x y movementSpeed carryCapacity harvestPower chopPower carryPlum carryLemon carryApple carryBanana carryIron carryWood`

### Available Commands
- `MOVE id x y` - Move troll to cell
- `HARVEST id` - Harvest at current cell
- `PLANT id type` - Plant tree (PLUM, LEMON, APPLE, BANANA)
- `CHOP id` - Chop tree at current cell (League 3+)
- `PICK id type` - Pick item from shack
- `DROP id` - Drop all items at shack
- `TRAIN moveSpeed carryCapacity harvestPower chopPower` - Train new troll
- `MINE id` - Mine adjacent iron (League 3+)
- `WAIT` - Do nothing
- `MSG text` - Display message in replay

### League System
- **League 1 (Bronze)**: 100 turns, no iron/rock/water, MOVE/HARVEST/DROP/WAIT only
- **League 2 (Silver)**: 300 turns, no iron/water, +TRAIN/PLANT/PICK
- **League 3 (Gold)**: 300 turns, full map, +CHOP/MINE
- **League 4 (Legend)**: Same as Gold but harder opponents

### Training Costs
```
Cost = (existing trolls) + (attribute)^2
```
- movementSpeed → PLUM
- carryCapacity → LEMON
- harvestPower → APPLE
- chopPower → IRON (League 3+)

## Scenario Dumping

The player can dump game states for debugging:

```java
// Enable via environment variable
String dumpMode = System.getenv("DUMP_SCENARIOS");
if (dumpMode != null && dumpMode.equals("1")) {
    dumpScenarios = true;
}
```

Dumped scenarios can be used to replay games locally.

## Common Patterns

### Finding Your Shack
```java
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
```

### Checking if Near Shack
```java
private boolean isNearShack(Troll troll) {
    int[] shack = findMyShack();
    int dx = Math.abs(troll.x - shack[0]);
    int dy = Math.abs(troll.y - shack[1]);
    return (dx + dy) <= 1;
}
```

### Finding Nearest Tree with Fruits
```java
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
```

## Referee
- Source: https://github.com/eulerscheZahl/Troll-Farm
- Download: https://github.com/eulerscheZahl/Troll-Farm/releases

## Tips
1. Each troll can only perform ONE action per turn
2. Actions execute in order: MOVE → HARVEST → PLANT → CHOP → PICK → TRAIN → DROP → MINE → Grow
3. When multiple trolls harvest/chop same tree, resources are shared one-at-a-time
4. Trees grow faster near water
5. Wood scores 4 points, fruits score 1 point each
