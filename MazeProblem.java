package pathfinder.informed;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Specifies the Maze Grid pathfinding problem including the actions, transitions,
 * goal test, and solution test. Can be fed as an input to a Search algorithm to
 * find and then test a solution.
 */
public class MazeProblem {

    // Fields
    // -----------------------------------------------------------------------------
    private String[] maze;
    private int rows, cols;
    private boolean foundKey;
    public final MazeState INITIAL_STATE, KEY_STATE;
    private Map<String, MazeState> goals = new HashMap<>();
    private Map<String, MazeState> mudTiles = new HashMap<>();
    private Set<MazeState> graveyard = new HashSet<>();
    private static final Map<String, MazeState> TRANS_MAP = createTransitions();

    /**
     * @return Creates the transition map that maps String actions to
     * MazeState offsets, of the format:
     * { "U": (0, -1), "D": (0, +1), "L": (-1, 0), "R": (+1, 0) }
     */
    private static final Map<String, MazeState> createTransitions () {
        Map<String, MazeState> result = new HashMap<>();
        result.put("U", new MazeState(0, -1));
        result.put("D", new MazeState(0,  1));
        result.put("L", new MazeState(-1, 0));
        result.put("R", new MazeState( 1, 0));
        return result;
    }


    // Constructor
    // -----------------------------------------------------------------------------

    /**
     * Constructs a new MazeProblem from the given maze; responsible for finding
     * the initial and goal states in the maze, and storing in the MazeProblem state.
     *
     * @param maze An array of Strings in which characters represent the legal maze
     * entities, including:<br>
     * 'X': A wall, 'G': A goal, 'I': The initial state, '.': an open spot
     * For example, a valid maze might look like:
     * <pre>
     * String[] maze = {
     *     "XXXXXXX",
     *     "X.....X",
     *     "XIX.X.X",
     *     "XX.X..X",
     *     "XG....X",
     *     "XXXXXXX"
     * };
     * </pre>
     */
    MazeProblem (String[] maze) {
        this.maze = maze;
        this.rows = maze.length;
        this.cols = (rows == 0) ? 0 : maze[0].length();
        MazeState foundInitial = null, foundGoal = null, foundKey = null, foundMud = null;;

        // Find the initial and goal state in the given maze, and then
        // store in fields once found
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                switch (maze[row].charAt(col)) {
                case 'I':
                    foundInitial = new MazeState(col, row); break;
                case 'G':
                    foundGoal = new MazeState(col, row);
                    goals.put(foundGoal.toString(), foundGoal);
                    break;
                case 'K':
                    foundKey = new MazeState(col, row); break;
                case '.':
                case 'M':
                  foundMud = new MazeState(col, row);
                  mudTiles.put(foundMud.toString(), foundMud);
                  break;
                case 'X':
                    break;
                default:
                    throw new IllegalArgumentException("Maze formatted invalidly");
                }
            }
        }
        INITIAL_STATE = foundInitial;
        KEY_STATE = foundKey;
        this.foundKey = (KEY_STATE == null) ? true : false;
    }


    // Methods
    // -----------------------------------------------------------------------------

    /**
     * Returns whether or not the given state is a Goal state.
     *
     * @param state A MazeState (col, row) to test
     * @return Boolean of whether or not the given state is a Goal.
     */
    public boolean isGoal (MazeState state) {
        return goals.containsValue(state);
    }

    /**
     * Returns a map of the states that can be reached from the given input
     * state using any of the available actions.
     *
     * @param state A MazeState (col, row) representing the current state
     * from which actions can be taken
     * @return Map A map of actions to the states that they lead to, of the
     * format, for current MazeState (c, r):<br>
     * { "U": (c, r-1), "D": (c, r+1), "L": (c-1, r), "R": (c+1, r) }
     */
    public Map<String, MazeState> getTransitions (MazeState state) {
        // Store transitions as a Map between actions ("U", "D", ...) and
        // the MazeStates that they result in from state
        Map<String, MazeState> result = new HashMap<>();

        // For each of the possible directions (stored in TRANS_MAP), test
        // to see if it is a valid transition
        for (Map.Entry<String, MazeState> action : TRANS_MAP.entrySet()) {
            MazeState actionMod = action.getValue(),
                      newState  = new MazeState(state.col, state.row);
            newState.add(actionMod);

            // If the given state *is* a valid transition (i.e., within
            // map bounds and no wall at the position)...
            if (newState.row >= 0 && newState.row < rows &&
                newState.col >= 0 && newState.col < cols &&
                maze[newState.row].charAt(newState.col) != 'X' &&
                !graveyard.contains(newState)) {
                // ...then add it to the result!
                result.put(action.getKey(), newState);
            }
        }
        return result;
    }

    /**
    * Given a possibleSoln, tests to ensure that it is indeed a solution to this MazeProblem,
    * as well as returning the cost.
    *
    * @param possibleSoln A possible solution to test, which is a list of actions of the format:
    * ["U", "D", "D", "L", ...]
    * @return A 2-element array of ints of the format [isSoln, cost] where:
    * isSoln will be 0 if it is not a solution, and 1 if it is
    * cost will be an integer denoting the cost of the given solution to test optimality
    */
   public int[] testSolution (ArrayList<String> possibleSoln) {
       // Update the "moving state" that begins at the start and is modified by the transitions
       MazeState movingState = new MazeState(INITIAL_STATE.col, INITIAL_STATE.row);
       int cost = 0;
       boolean hasKey = false;
       int[] result = {0, -1};

       // For each action, modify the movingState, and then check that we have landed in
       // a legal position in this maze
       for (String action : possibleSoln) {
           MazeState actionMod = TRANS_MAP.get(action);
           movingState.add(actionMod);
           switch (maze[movingState.row].charAt(movingState.col)) {
           case 'X':
               return result;
           case 'K':
               hasKey = true; break;
           }
           cost += getCost(movingState);
       }
       result[0] = isGoal(movingState) && hasKey ? 1 : 0;
       result[1] = cost;
       return result;
   }

    /**
     * Calculates the cost of a single move depending on if there is mud or not.
     *
     * @param state A MazeState (col, row) representing the current state
     * from which actions can be taken
     * @return An integer which represents the cost of a single move depending
     * on what the tile being landed on is.
     */
    public int getCost(MazeState state){
      if (maze[state.row].charAt(state.col) == 'M') {
        return 3;
      }
      else {
        return 1;
      }
    }
    
    /**
     * Calculates the total cost of from a given node.
     *
     * @param node The current MazeState, the action that *led to* this state / node,
     * and the reference to parent SearchTreeNode in the Search Tree.
     * @return An integer which represents the total cost of going from the
     * intial state to the state of the node or from the key state to the state of the node if the 
     * key has been found.
     */
    public int getTotalCost(SearchTreeNode node) {
    	SearchTreeNode current = node;
    	int cost = getCost(current.state);
    	while(current.parent != null) {
    		cost += getCost(current.parent.state);
    		current = current.parent;
    	}
    	cost += estimateDistance(node.state);
    	return cost;
    }   
    
    /**
     * Calculates the estimated distance from the state to the goal state or key state
     * if the key has not yet been found without taking into account mud tile costs.
     * @param state A MazeState (col, row) representing the current state
     * from which actions can be taken.
     * @return An integer which represents the estimated distance from the initial 
     * state to the goal state or key state if the key has not yet been found.
     */
    private int estimateDistance(MazeState state) {
    	int distance = 0;
    	if(foundKey) {
    		int minDistance = 2147483647;
    		for (Entry<String, MazeState> x : goals.entrySet()) {
    			MazeState xMod = x.getValue();
    			int tempDistance = Math.abs(state.row - xMod.row);
    			tempDistance += Math.abs(state.col - xMod.col);
    			if(tempDistance < minDistance) {
    				minDistance = tempDistance;
    			}
    		}
    		distance = minDistance;
    	} else {
    		distance += Math.abs(state.row - KEY_STATE.row);
        	distance += Math.abs(state.col - KEY_STATE.col);
    	}
    	
    	return distance;
    }
    
    /**
     * Determines if the current state is a key or not a key.
     *
     * @param state A MazeState (col, row) representing the current state
     * from which actions can be taken.
     * @return Returns true if the current state is a key, false if it is not.
     */
    public boolean isKey (MazeState state) {
        return state.equals(KEY_STATE);
    }
    
    /**
     * Getter to see the goal states.
     * @return A set that has the location of the goal states.
     */
    public Map getGoals() {
    	return goals;
    }
    
    /**
     * Signifies that the key has been found.
     */
    public void findKey() {
    	foundKey = true;
    }
    
    /**
     * Determines if whether or not the key as been found.
     *
     * @return true if the key is found and false if it is not.
     */
    public boolean foundKey() {
    	return foundKey;
    }
    
    /**
     * Adds the given state to a set which contains the traversed states.
     *
     * @param state A MazeState (col, row) representing the current state
     * from which actions can be taken.
     */
    public void addToGraveyard(MazeState state) {
    	graveyard.add(state);
    }
    
    /**
     * Empties the graveyard set.
     */
    public void clearGraveyard() {
    	graveyard.clear();
    }

}