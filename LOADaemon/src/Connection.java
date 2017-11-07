import java.net.Socket;
import java.io.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 *
 * A connection is the actual player or observer socket connection.
 */

public class Connection {
    static final int STATE_INITIAL = 0;
    static final int STATE_SEATED = 1;
    static final int STATE_PLAYING = 2;
    static final int STATE_DONE = 3;
//    private int state = STATE_INITIAL;
    private int who = 0;

    private Socket socket;
    private Messages msg;
    
    private static boolean connection_debug = false;

    /**
     * Constructor
     */
    public Connection( Socket ss, int w )
    throws IOException {
      socket = ss;
      who = w;
      msg = new Messages(ss.getInputStream(),ss.getOutputStream());
    }

    /**
     * Notify the socket the play that we are starting.
     *	                15x	                Designation
     * role_white	151	                You will play white
     * role_black	152	                You will play black
     * role_observer	153	                You will observe
     * error            999                     Internal error
     */
    public void start()
      throws IOException {
    	if (connection_debug)
          System.out.println("In connection start.");
	    switch (who) {
	    case Board.PLAYER_WHITE:
	      msg.response("151 You will play white");
	      return;
	    case Board.PLAYER_BLACK:
	      msg.response("152 You will play black");
	      return;
	    case Board.OBSERVER:
	      msg.response("153 You will observe");
	      return;
	    }
	    msg.response("999 internal error");
	    //socket.close();
	    throw new Error("internal error: funny who");
    }

    /**
     * Wait for a move request on this connection
     */
    public Move get_move(int serial, int to_move)
      throws IOException {
	while (true) {
	   Move m = msg.req_move(serial, to_move);
	   if (m != null)
	       return m;
	}
    }

    /**
     * Move is accepted, keep playing.
     * result_continue	200	        Continue playing
     */
    public void legal_move(Move m) {
	msg.response("200 Move " + m.name() + " accepted, continue playing");
    }

    /**
     * Time controlled move is accepted, keep playing.
     * result_continue	207 <secs>	Continue with time left
     */
    public void legal_move_tc(Move m, int time) {
	msg.response("207 " + time + " secs remaining after move " +
		     m.name() + " accepted, continue playing");
    }

    /**
     * The last move was a winning move. Notify the players.
     * result_win	201	          You win
     * result_lost	202	          You lose
     * result_drawn	203               You draw
     */
    public void final_move(int to_move, int winner, Move m) {
	String result_code, result_desc;
	if (to_move == winner) {
	    result_code = "201";
	    result_desc = "You win";
	} else if(winner == Board.OBSERVER) {
	    result_code = "203";
	    result_desc = "You draw";
	} else {
	    result_code = "202";
	    result_desc = "You lose";
	}
	msg.response(result_code + " Move " + m.name() + " accepted, " + result_desc);
    }

    /**
     * Out of time notification
     * flag_fell	202	          Out of time notification
     */
    public void flag_fell() {
	msg.response("202 Your time expires, and you lose");
    }

    /**
     * The move the player attempted was illegal.
     * result_illegal	291	          Illegal request
     */
    public void illegal_move(Move m) {
	  msg.response("291 Illegal move " + m.name());
    }

    /**
     * The other player moved, notify this player what happened.
     * status_moves_black	311 <move-number> <move>	Black move
     * status_moves_white	312 <move-number> ... <move>	White move
     */
    public void move(int serial, int to_move, Move m) {
	String result_code, ellipses, result_desc;
	if (to_move == Board.PLAYER_BLACK) {
	    result_code = "311";
	    ellipses = "";
	    result_desc = "black";
	} else {
	    result_code = "312";
	    ellipses = " ...";
	    result_desc = "white";
	}
	msg.response(result_code + " " + serial + ellipses + " " + m.name() +
		     " is " + result_desc + " move, game continues");
    }

    /**
     * This is a timed game, and the other player moved, notify this player what
     * happened.
     * status_moves_black_tc	313 <move-number> <move> <secs>	      Black move and time
     * status_moves_white_tx	314 <move-number> ... <move> <secs>   White move and time
     */
    public void move_tc(int serial, int to_move, Move m, int time) {
	String result_code, ellipses, result_desc;
	if (to_move == Board.PLAYER_BLACK) {
	    result_code = "313";
	    ellipses = "";
	    result_desc = "black";
	} else {
	    result_code = "314";
	    ellipses = " ...";
	    result_desc = "white";
	}
	msg.response(result_code + " " + serial + ellipses + " " +  m.name() +
		     " " + time + " (secs) is " + result_desc + " move, game continues");
    }

    /**
     * The other player moved, notify this observer what happened.
     * status_board_black	381 <move-number> <board> <move>      Board after black move
     * status_board_white	382 <move-number> <board> <move>      Board after white move
     */
    public void board(int serial, int to_move, Board b, Move m) {
	String result_code, result_b = "";
	if (to_move == Board.PLAYER_BLACK) {
	    result_code = "381";
	} else {
	    result_code = "382";
	}
        for (int i = 0; i < 8; i++ )
          for (int j = 0; j < 8; j++) {
            if (b.checker_of(Board.PLAYER_BLACK, i, j ))
              result_b = result_b + "b";
            if (b.checker_of(Board.PLAYER_WHITE, i, j ))
              result_b = result_b + "w";
            if ( !b.checker_of(Board.PLAYER_BLACK, i, j) &&
                 !b.checker_of(Board.PLAYER_WHITE,i,j))
              result_b = result_b + "X";
          }
	msg.response( result_code + " " + serial + " " + result_b + " " +
                      m.name() + " game continues");
    }

    /**
     * This is a timed game, the other player moved, notify this observer what happened.
     * status_board_black	383 <move-number> <board> <move> <secs>   Board after black move
     * status_board_white	384 <move-number> <board> <move> <secs>   Board after white move
     */
    public void board_tc(int serial, int to_move, Board b, Move m, int time) {
	String result_code, result_b = "" ;
	if (to_move == Board.PLAYER_BLACK) {
	    result_code = "383";
	} else {
	    result_code = "384";
	}
        for (int i = 0; i < 8; i++ )
          for (int j = 0; j < 8; j++) {
            if (b.checker_of(Board.PLAYER_BLACK, i, j ))
              result_b = result_b + "b";
            if (b.checker_of(Board.PLAYER_WHITE, i, j ))
              result_b = result_b + "w";
            if ( !b.checker_of(Board.PLAYER_BLACK, i, j) &&
                 !b.checker_of(Board.PLAYER_WHITE,i,j))
              result_b = result_b + "X";
          }
	msg.response( result_code + " " + serial + " " + result_b + " " +
                      m.name() + " " + time + " game continues");
    }

    /**
     * Shutting down this gamed. Notify this player, that the game is over, and
     * the server is disconnecting them.
     * This is also called when one of the players abandons the game. In which
     * case the person still here automatically wins.
     *
     * status_winsmove_black	321 <move-number> <move>	Black wins by move
     * status_losesmove_black	322 <move-number> <move>	Black loses by move
     * status_winsmove_white	323 <move-number> ... <move>	White wins by move
     * status_losesmove_white	324 <move_number> ... <move>	White losts by move
     * status_drawsmove_black	325 <move-number> <move>	Drawn by Black move
     * status_drawsmove_white	326 <move_number> ... <move>	Drawn by White move
     */
    public void stop(int serial, int to_move, int winner, Move m) {
	  String result, result_code, ellipses, result_desc;
	  if (to_move == Board.PLAYER_BLACK) {
	    ellipses = "";
	    result_desc = "black ";
	    switch (winner) {
	      case Board.PLAYER_BLACK:
		    result_code = "321";
		    result_desc = result_desc + "wins";
		    break;
	      case Board.PLAYER_WHITE:
		    result_code = "322";
		    result_desc = result_desc + "loses";
		    break;
	      case Board.OBSERVER:
		    result_code = "325";
		    result_desc = result_desc + "draws";
		    break;
	      default:
		    throw new Error("internal error: funny winner");
	    }
	  } else {
	    ellipses = " ...";
	    result_desc = "white ";
	    switch (winner) {
	      case Board.PLAYER_WHITE:
		    result_code = "323";
		    result_desc = result_desc + "wins";
		    break;
	      case Board.PLAYER_BLACK:
		    result_code = "324";
		    result_desc = result_desc + "loses";
		    break;
	      case Board.OBSERVER:
		    result_code = "326";
		    result_desc = result_desc + "draws";
		    break;
	      default:
		    throw new Error("internal error: funny winner");
	    }
	  }
        result = result_code + " " + serial + ellipses + " ";
        if ( m != null)
          result = result + m.name();
        else
          result = result + "NONE";
        result = result + " and " + result_desc + "\r\n";
        try {
          msg.response(result);
        } catch( Exception e ){
          System.out.println("Stop message not sent");
        }
	    try {
          msg.close_streams();
	      socket.close();
	    } catch(IOException e) {
	    // do nothing
	  }
    }

    /**
     * Default win conditions based upon time. The player that still has time
     * wins.
     * status_flagfell_white	361	      Black wins by White time expiring
     * status_flagfell_black	362	      White wins by Black time expiring
     */
    public void stop_flag(int serial, int winner) {
	if (winner == Board.PLAYER_BLACK) {
	    msg.response("361 Black wins by White time expiring");
	} else {
	    msg.response("362 White wins by Black time expiring");
	}
	try {
	    socket.close();
	} catch(IOException e) {
	    // do nothing
	}
    }
}
