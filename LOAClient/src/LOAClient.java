import java.io.*;

/**
 * Title:        LOADaemon
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 */
class TerminationException extends Exception {
	private static final long serialVersionUID = 1L;
}

public class LOAClient {
    ClientConnection client;
    int depth;
    private static int side;
    WorkBoard board = new WorkBoard();
    private static int wins;
    private static int draws;

    public LOAClient(ClientConnection client, int depth) {
	this.client = client;
	this.depth = depth;
	this.board.initialize();
    }

    private void make_my_move()
      throws TerminationException {
        board.bestMove(depth);
	    int result = board.try_move(board.best_move);
	    if (result == Board.ILLEGAL_MOVE)
	      throw new Error("attempted illegal move");
	    int state;
	    try {
	      state = client.make_move(board.best_move);
	    } catch(IOException e) {
	      e.printStackTrace(System.out);
	      throw new Error("move refused by referee");
	    }
	    if (state == ClientConnection.STATE_CONTINUE && result != Board.CONTINUE)
	      throw new Error("Client erroneously expected game over");
	    if (state == ClientConnection.STATE_DONE) {
	      if (result != Board.GAME_OVER)
		    System.out.println("Game over unexpectedly");
	      throw new TerminationException();
	    }
    }

    private void get_opp_move()
      throws TerminationException {
	int state;
	try {
	    state = client.get_move();
	} catch(IOException e) {
	    e.printStackTrace(System.out);
	    throw new Error("couldn't get move from referee");
	}
	if (state == ClientConnection.STATE_DONE)
	    throw new TerminationException();
	int result = board.try_move(client.move);
	if (result == Board.ILLEGAL_MOVE)
	    throw new Error("received apparently illegal move");
    }

    public void play() {
      try {
        while (true) {
          if (client.who == Board.PLAYER_BLACK) {
              make_my_move();
//              board.print(System.out);
          }
          else {
              get_opp_move();
//              board.print(System.out);
          }
          if (client.who == Board.PLAYER_WHITE) {
              make_my_move();
//              board.print(System.out);
          }
          else{
              get_opp_move();
//              board.print(System.out);
          }
        }
      } catch(TerminationException e) {
        if (client.winner == side)
          wins++;
        System.out.print("Game ends with ");
        switch (client.winner) {
        case Board.PLAYER_WHITE:
            System.out.println("white win");
            break;
        case Board.PLAYER_BLACK:
            System.out.println("black win");
            break;
        case Board.NONE:
            System.out.println("draw");
            draws++;
            break;
        }
      } catch(Error e) {
      	System.out.println(e);
      	if (client.winner == side)
      	  wins++;
        System.out.print("Game ends with ");
        switch (client.winner) {
        case Board.PLAYER_WHITE:
            System.out.println("white win");
            break;
        case Board.PLAYER_BLACK:
            System.out.println("black win");
            break;
        case Board.NONE:
            System.out.println("draw");
            draws++;
            break;
        }
      }
    }

    public static void main(String args[])
      throws IOException {
		if (args.length != 5)
		    throw new IllegalArgumentException(
		       "usage: black|white hostname server-number depth number_of_games");
		if (args[0].equals("black"))
		    side = Board.PLAYER_BLACK;
		else if (args[0].equals("white"))
		    side = Board.PLAYER_WHITE;
		else
		    throw new IllegalArgumentException("unknown side");
		String host = args[1];
		int server = Integer.parseInt(args[2]);
		int threshold = Integer.parseInt(args[4]);
	
		for (int i = 0; i < threshold; i++) {
			System.out.println("<<init>>  Game Number: " + i + " / " + threshold);
			System.out.print( "Connecting to server: " + server + " requesting ");
		    if( side == Board.PLAYER_BLACK)
		      System.out.println("black.");
		    else
		      System.out.println("white.");
		    ClientConnection client;
		    try {
		      client = null;
		      boolean result = false;
		      while (result == false) {
			    client = new ClientConnection(side, host, server);
			    result = client.NegotiateConnection();
		      }
			  int depth = Integer.parseInt(args[3]);
			  LOAClient game = new LOAClient(client, depth);
			  System.out.println(">> start <<");
			  game.play();
		    } catch (IOException e) {
			      System.out.println(e);
			      i--;
			      continue;
		    }
			System.out.println(">> stop <<");
			if(side == Board.PLAYER_BLACK)
			  side = Board.PLAYER_WHITE;
			else
			  side = Board.PLAYER_BLACK;
			for( int j = 0; j < 100000; j++)
				;
		}
		System.out.println("Percentage wins (" + wins + "/" +threshold+"): "+((double)wins/(double)threshold));
		System.out.println("Percentage draws (" + draws + "/" +threshold+"): "+((double)draws/(double)threshold));
    }
}
