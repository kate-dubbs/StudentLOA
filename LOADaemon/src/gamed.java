import java.io.*;

/**
 * Title:        LOADaemon
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version alpha
 */

public class gamed implements Runnable {
  Status stat;
  static Connection white_conn = null;
  static Connection black_conn = null;
  static Connection active_conn = null;
  static Connection observer_conn[] = new Connection[daemon.max_observers];

  public static Board board;

  private static long timestamp = 0;

  private static boolean gamed_debug = false;

    /**
     * secs converts the millisecond time into seconds
     *
     * @param long msecs
     */
    public static int secs(long msecs) {
	  return (int)(msecs / 1000);
    }

    /**
     * Constuctor
     */
    public gamed( Status s)
    throws IOException {
      if (gamed_debug)
        System.out.println("New game "+ s.index +" initializing.");
      stat = s;
      white_conn = new Connection( stat.white, Board.PLAYER_WHITE );
      black_conn = new Connection( stat.black, Board.PLAYER_BLACK );
      if (gamed_debug) {
    	System.out.println(stat.white);
    	System.out.println(stat.black);
      }
      for (int i = 0; i < stat.num_observers; i++ )
        observer_conn[i] = new Connection( stat.observer[i], Board.OBSERVER );
      if (gamed_debug)
        System.out.println("New game "+ stat.index +" initializing completed.");
    }

    /**
     *
     */
    public void run() {
      // tell the clients we're set
      try {
        for (int i = 0; i < stat.num_observers; i++)
            observer_conn[i].start();
        white_conn.start();
        black_conn.start();
      } catch ( IOException e) {
          // Somebody left without telling us.
          e.printStackTrace();
          CloseDown(1, 0, 0, null);
          return;
      }
      if (gamed_debug)
        System.out.println("Starting to play the game");
      // play the game
      board = new Board();
//      board.setUser();
      while (true) {
          System.out.println();
          if ( gamed_debug )
            board.print(System.out);

          if (board.to_move == Board.PLAYER_WHITE)
              active_conn = white_conn;
          else
              active_conn = black_conn;
          int to_move = board.to_move;
          int serial = board.serial;
          timestamp = System.currentTimeMillis();

          Move m = null;
          try {
            m = active_conn.get_move(serial, to_move);
          } catch (IOException e ) {
            // Someone left that shouldn't have.
            e.printStackTrace();
            int w = to_move;
            if (to_move == Board.PLAYER_BLACK ) {
              w = Board.PLAYER_WHITE;
              System.out.println( "White connection Failed!");
            } else {
              w = Board.PLAYER_BLACK;
              System.out.println( "Black connection Failed!");
            }
            CloseDown(serial, to_move, w, null);
            return;
          }
          int winner = 0;
          long move_time = 0;
          if (daemon.time_controls) {
              boolean flag_fell = false;
              if (to_move == Board.PLAYER_WHITE) {
                  stat.white_msecs -= System.currentTimeMillis() - timestamp;
                  move_time = stat.white_msecs;
                  if (stat.white_msecs < 0) {
                      flag_fell = true;
                      winner = Board.PLAYER_BLACK;
                  }
              } else {
                  stat.black_msecs -= System.currentTimeMillis() - timestamp;
                  move_time = stat.black_msecs;
                  if (stat.black_msecs < 0) {
                      flag_fell = true;
                      winner = Board.PLAYER_WHITE;
                  }
              }
              if (flag_fell) {
                  if (winner == Board.PLAYER_WHITE)
                      System.out.print("White");
                  else if (winner == Board.PLAYER_BLACK)
                      System.out.print("Black");
                  else
                      throw new Error("bogus winner");
                  System.out.println(" player wins on time.");
                  active_conn.flag_fell();
                  board.game_state = Board.GAME_OVER;
                  white_conn.stop_flag(serial, winner);
                  black_conn.stop_flag(serial, winner);
                  for (int i = 0; i < stat.num_observers; i++)
                      observer_conn[i].stop_flag(serial, winner);
                  return;
              }
          }
          int status = board.try_move(m);
          // Call the game a draw if we hit the maximum number of moves that can be
          // stored.
          if ( serial >= Board.MAX_DEPTH/2)
          	status = Board.GAME_OVER;
          switch(status) {
          case Board.ILLEGAL_MOVE:
              active_conn.illegal_move(m);
              continue;
          case Board.GAME_OVER:
              if ( gamed_debug )
                board.print(System.out);
              winner = board.referee();
              System.out.println("Player " + winner + " wins.");
              CloseDown(serial, to_move, winner, m);
              return;
          case Board.CONTINUE:
              if ( daemon.time_controls ) {
                  int ms = secs(move_time);
                  active_conn.legal_move_tc(m, ms);
                  white_conn.move_tc(serial, to_move, m, ms);
                  black_conn.move_tc(serial, to_move, m, ms);
                  for (int i = 0; i < stat.num_observers; i++)
                    observer_conn[i].board_tc(serial, to_move, board, m, ms);
              } else {
                  active_conn.legal_move(m);
                  white_conn.move(serial, to_move, m);
                  black_conn.move(serial, to_move, m);
                  for (int i = 0; i < stat.num_observers; i++)
                    observer_conn[i].board(serial, to_move, board, m);
              }
              continue;
          }
          throw new Error("Internal error: Player doesn't match with who it should be.");
      }
    }

    /**
     * This method shuts down this game daemon. For those clients still connected,
     * it notifies them of the final moves, and then releasing the socket connections.
     */
    private void CloseDown( int serial, int to_move, int winner, Move m) {
      if ( gamed_debug)
          System.out.println("Starting CloseDown procedure "+ stat.index +".");
      try {
        white_conn.stop(serial, to_move, winner, m);
      } catch ( Exception e) {
        System.out.println("Exception on white stop.");e.printStackTrace();}
      try {
        black_conn.stop(serial, to_move, winner, m);
      } catch ( Exception e) {
        System.out.println("Exception on black stop.");}
      for (int i = 0; i < stat.num_observers; i++) {
      	observer_conn[i].board(serial, to_move, board, m);
        observer_conn[i].stop(serial, to_move, winner, m);
      }
      stat.num_observers = 0;
      for (int i = 0; i < daemon.max_observers; i++ )
        stat.observer[i] = null;
      try {
    	stat.lock.tryLock(); //blocks until locked.
        stat.black = null;
        stat.white = null;
      } finally {
        stat.lock.unlock();
      }
      if ( gamed_debug)
        System.out.println("Ending CloseDown procedure "+ stat.index +".");
  }
}
