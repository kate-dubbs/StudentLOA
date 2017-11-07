

/**
 * Title:	Alpha-Beta MinMax Search
 * Description: Implements Computer player for LOA using an Alpha-Beta MinMax Search and Board Evaluator
 * Copyright:    Copyright (c) 2004
 * Company:	CSCE 523
 * @author: Kate Werling
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
     * This function attempts to evaluate the board using center of mass, quads,
     * uniformity, and centralization
     * @return int calculated heuristic value of the board
     */
    int h_value() {  
    	int player = to_move;
    	int board_weight[] = new int[2];
    	int return_value = 0;
    	 	
    	if(connected(to_move)){
    		return INF;
    	} else if(connected(opponent(to_move))){
    		return -INF;
    	} else {
    		
    	for(int zero_sum = 0; zero_sum < 2; zero_sum++){
        	//Weighting Values
        	double w_quad = -1;		//Weighting for Quads
        	double w_uni  = 2;		//Weighting for Uniformity
        	double w_cent = 5;		//Weighting for Centralization
        	
        	int quads = 0;	  //Number of quads with 3 or more pieces of the same color	
        	int uniform = 0;  //Weights area containing the pieces
        	int center = 0;   //Weights moves towards the center of board
        	
    	//Centralization Evaluation: middle is weighted higher
    	for(int x = 0; x<BOARD_SIZE; x++){
    		for(int y = 0; y<BOARD_SIZE; y++){
    			if(checker_of(player, x, y)){
    				if(x == 4 || x == 5){
    	    		  if(y == 4 && y == 5){
    	    			center = center + 3;
    	    		  } else if(y == 3 || y == 6){
    	    			center = center + 2;
    	    		  } else if(y == 2 || y == 7){
    	    			center = center + 1;
    	    		  } 
    	    		} else if(x == 3 || x == 6){
    	    			if(y >= 3 && y <= 6){
        	    			center = center + 2;
        	    		  } else if(y == 2 || y == 7){
        	    			center = center + 1;
        	    		  } 
    	    		} else if(x == 5 || x == 7){
    	    			if(y >= 1 && y <= 7){
        	    			center = center + 1;
        	    		  }
    	    		} 
    			}
    		}
    	}
   		 
    	//Quads Evaluation
    	quads = (quadcount[player][1]-quadcount[player][3]-2*quadcount[player][5])/4;
    	//Smaller Euler number is better, so weight is negated
    	 	
    	//Uniformity
    	int start_x = 0, start_y = 0, end_x = 0, end_y = 0;
    	
    	for(int x = 1; x<BOARD_SIZE+1; x++){
        	for(int y = 1; y<BOARD_SIZE+1; y++){
        		if(checker_of(player, x, y) && start_x == 0 && start_y == 0){
        			start_x = x;
        			start_y = y;
        		}
           }
    	} 
    	for(int x = BOARD_SIZE; x>0; x--){
        	for(int y = 1; y<BOARD_SIZE+1; y++){
        		if(checker_of(player, x, y) && end_y == 0){
        			end_y = y;
        		}
           }
    	}
    	for(int y = 1; y<BOARD_SIZE+1; y++){
        	for(int x = BOARD_SIZE; x>0; x--){
        		if(checker_of(player, x, y) && end_x == 0){
        			end_x = x;
        		}
           }
    	}
    	int area = (end_x-start_x)*(end_y-start_y);
    	uniform = BOARD_SIZE*BOARD_SIZE - area;
    	//Smaller Area is better, so subtract to make uniform bigger when area is small
    	
    	board_weight[zero_sum] = (int) (w_cent*center + w_quad*quads + w_uni*uniform);
    	player = opponent(player);
    		}
    	return_value = board_weight[0] - board_weight[1];
    	
    	return return_value;
    	}
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
    	int best_value = 0; 
        int h_value = 0;
    	Move moveList = genMoves();			
    	Move return_move = null;					
    	
    	while(moveList != null){
        makeMove(moveList);	
    	h_value = max_value(alpha, beta, depth-1);
    		if(h_value > best_value){
    		return_move = moveList;
    		best_value = h_value;
    		}
    	reverseMove(moveList);
    	moveList = moveList.next;
    	totalNodesSearched++;
    	}
    	
    	System.out.println("Best move heuristic " + h_value);
 		return return_move;	
    }
       
int max_value(int alpha, int beta, int depth){
	   	if(connected(to_move) || depth == 0){
	   		numLeafNodes++;
    		return h_value();
    	} 
	   	int state_value = -INF;
	   	

	   	Move m = genMoves();
    	while(m != null){
    		makeMove(m);
    		state_value = Math.max(state_value, min_value(alpha, beta, depth-1));
    		alpha = Math.max(alpha, state_value);
    		reverseMove(m);
    		m = m.next;
    		if (beta <= alpha){
    			return state_value;
    		}
    		totalNodesSearched++;
    	}
    	return state_value; 
    }
   
   int min_value(int alpha, int beta, int depth){
	   if(connected(to_move) || depth == 0){
		numLeafNodes++;
   		return h_value();
   	   } 
	   
	   int state_value = INF;
	   
	   Move m = genMoves();
	   while(m != null){
		   makeMove(m);
		   state_value = Math.min(state_value,  max_value(alpha, beta, depth-1));
		   beta = Math.min(beta, state_value);
		   reverseMove(m);
   		   m = m.next;
   		if (beta <= alpha){
			   return state_value;
		   }
   		   totalNodesSearched++;
	   }
	   return state_value; 
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
