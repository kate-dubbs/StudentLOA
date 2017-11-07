/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2004
 * Company:
 * @author: Put in your name
 * @version 1.0
 */

public class WorkBoard extends Board {
	static final int INF = 10000;
	Move best_move = null;  // Put your best move in here!
    int start_depth = 0;
    int totalNodesSearched = 0;
    int numLeafNodes = 0;
    boolean stoptime = true;
    public long searchtime = 0L;

    public WorkBoard() {
    }

    public WorkBoard(WorkBoard w) {
	super(w);
    }

    /**
     * This is where your board evaluator will go. This function will be called
     * from min_max
     *
     * @return int calculated heuristic value of the board
     */
    int h_value() {

  
      return 0;
    }


    /**
     * This is where you will write min-max alpha-beta search. Note that the
     * Board class maintains a predecessor, so you don't have to deal with
     * keeping up with dynamic memory allocation.
     * The function takes the search depth, and returns the maximum value from
     * the search tree given the board and depth.
     *
     * @parama depth int the depth of the search to conduct
     * @return maximum heuristic board found value
     */
    Move min_max_AB(int depth, int alpha, int beta) {
      totalNodesSearched++;
      
      return null;
    }
       
    /**
     * This function is called to perform search. All it does is call min_max.
     *
     * @param depth int the depth to conduct search
     */
    void bestMove(int depth) {
      best_move = null;
      int runningNodeTotal = 0;
      totalNodesSearched = numLeafNodes = moveCount = 0;
      start_depth = 1;
      int i = 1;
      long startTime = System.currentTimeMillis();
      long elapsedTime = 0;
      long currentPeriod = 0;
      long previousPeriod = 0;
      stoptime = false;

      while ( i <= depth && !stoptime) {
        totalNodesSearched = numLeafNodes = moveCount = 0;
        start_depth = i;

        best_move = min_max_AB(i, -INF, INF); // Min-Max alpha beta

        elapsedTime = System.currentTimeMillis()-startTime;
        currentPeriod = elapsedTime-previousPeriod;
        double rate = 0;
        if ( i > 3 && previousPeriod > 50 )
          rate = (currentPeriod - previousPeriod)/previousPeriod;

        runningNodeTotal += totalNodesSearched;
        System.out.println("Depth: " + i +" Time: " + elapsedTime/1000.0 + " Nodes Searched: " + totalNodesSearched + " Leaf Nodes: " + numLeafNodes);

        // increment indexes;
        i++;
        if ( (elapsedTime+(rate+1.0)*currentPeriod) > searchtime )
          stoptime = true;
      }

      System.out.println("Nodes per Second = " + runningNodeTotal/(elapsedTime/1000.0));
      if (best_move == null  || best_move.piece == null) {
        throw new Error ("No Move Available - Search Error!");
      }
    }
}
