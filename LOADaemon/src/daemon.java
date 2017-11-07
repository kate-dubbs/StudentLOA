import java.io.*;
import java.net.*;

/**
 * Title:        LOADaemon
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version alpha
 */

public class daemon {
    public static final String name = "LOAdaemon";
    public static final String version = "alpha";
    public static final int server_base = 4000;
    public static final int max_observers = 10;
    static int max_servers = 10;

    static gamed games[];
    static Status stats[];

    public static boolean time_controls = false;
    public static long white_msecs = 0;
    public static long black_msecs = 0;

    private static boolean debug_server = false;

    /**
     * secs converts the millisecond time into seconds
     *
     * @param long msecs
     */
    public static int secs(long msecs) {
	return (int)(msecs / 1000);
    }

    /**
     * main() interprets the string arguments and initializes the sockets and
     * the gamedaemons (gamed). Then calls the main loop.
     */
    public static void main(String args[]) {
	// Parse the command line arguments
	if ( args.length > 3 )
	    throw new IllegalArgumentException(
	      "\nusage: LOAdemon server_number [secs (white)[secs black]]\n");
        if (args.length > 0 ) {
          int snum = Integer.parseInt(args[0]);
          if (snum <= 0 )
              throw new
                 IllegalArgumentException( "CL error: Number of servers must be larger than 0\n" );
          max_servers = snum;
          if (args.length > 1) {
              white_msecs = Integer.parseInt(args[1]) * 1000;
              black_msecs = white_msecs;
              time_controls = true;
          }
          if (args.length > 2)
              black_msecs = Integer.parseInt(args[2]) * 1000;
        }

        // Initialize the server socket.
        ServerSocket ss = null;
        try {
           ss = new ServerSocket(server_base);
        } catch (IOException e ) {
          e.printStackTrace();
          System.exit(1);
        }

        try {
          InetAddress addr = InetAddress.getLocalHost();
          String t = "Server socket generated at " + server_base + " Address " + addr;
          System.out.println( t );
        } catch (IOException e ) {
          e.printStackTrace();
          System.exit(1);
        }

        // Initalize the external objects
        games = new gamed[max_servers];
        stats = new Status[max_servers];

        // Initialize the status blocks.
        for( int i = 0; i < max_servers; i++ ) {
          stats[i] = new Status();
          stats[i].index = i;
        }
        listenSocket( ss ); // listen to the socket and maintain data structures
      }

      /**
       * Listens to the socket when a connection is initialized, calls Initialize
       * Connection
       */
      static private void listenSocket( ServerSocket ss ) {
        while ( true ) {
          try {
            InitializeConnection( ss );
          } catch (IOException e ) {
            System.out.println("\nFailed connection attempt\n" );
            e.printStackTrace();
            continue;
          }
        }
      }

      /**
       * Initializes client connection. This is where all the work is done.
       */
      static private void InitializeConnection( ServerSocket ss )
      throws IOException {
        Socket socket;
        Messages msg;
        int who, i = 0;

      	socket = ss.accept();
      	if (debug_server == true)
          System.out.println(socket);
	    msg = new Messages(socket.getInputStream(), socket.getOutputStream());
	    msg.send_greeting();
	    who = msg.req_side();
	    if (who == -9) {
	      msg.response("199 Request not understood (botched connection)");
	      socket.close();
	      throw new IOException("Comm Error: botched initial handshake\n");
	    }
        if (who >= 10 ) { // OBSERVER
	      if (stats[who-10].num_observers >= daemon.max_observers) {
		    msg.response("193 Cannot observe");
		    socket.close();
		    throw new IOException("too many observers");
	      }
	      if (debug_server == true)
            System.out.println("Assigning new observer to server " + (who-10) );
          stats[who-10].num_observers++;
          stats[who-10].observer[who-10] = socket;
	      if (daemon.time_controls) {
		    msg.response("101 " + daemon.secs(daemon.white_msecs) + " " +
			   daemon.secs(daemon.black_msecs) + " Request accepted with time controls");
		    return;
	      }
	      msg.response("100 Request accepted");
          msg.response("153 You will observe");
	      return;
        }
	    if (who == Board.PLAYER_WHITE) {  // PLAYER_WHITE
          // Find a free gamed
          if ( msg.server == -1 )
            for ( i = 0; i < max_servers; i++ ){
              try {
            	stats[i].lock.lock();
            	if ( stats[i].white == null)
            	  break;
              } finally {
            	stats[i].lock.unlock();
              }
//               if ( stats[i].white == null )
//                 break;
            }
          else
        	try {
              stats[msg.server].lock.lock();
              if( stats[msg.server].white != null)
                  i = max_servers;
        	} finally {
        	  stats[msg.server].lock.unlock();
        	}
//            if( stats[msg.server].white != null)
//              i = max_servers;
             
          // No free gamed
          if ( i >= max_servers ) {
		    msg.response("191 No server with requested side free");
		    socket.close();
		    return;
		    //throw new IOException("second try for white player");
	      }
          // Assign the player to a gamed
          if (debug_server == true)
            System.out.println("Assigning new white player to server " + i );
          try {
        	stats[i].lock.lock();
        	stats[i].white = socket;
          } finally {
        	stats[i].lock.unlock();
          }
          //stats[i].white = socket;
	      if (daemon.time_controls) {
		    msg.response("101 " + daemon.secs(daemon.white_msecs) +
			       " " + daemon.secs(daemon.black_msecs) +
			       " Request accepted with time controls (you / opp)");
		   return;
	      }
	      msg.response("100 Request accepted");
	      try {
	    	stats[i].lock.lock();
	        if( stats[i].white != null && stats[i].black != null ) {
	          // START THE GAME
	          games[i] = new gamed( stats[i] );
	          Thread t = new Thread(games[i] );
	          t.start();
	        }
	      } finally {
	    	stats[i].lock.unlock();
	      }
	      return;
	    }
	    if (who == Board.PLAYER_BLACK) {  // PLAYER_BLACK
          // Find a free gamed
          if ( msg.server == -1 )
            for ( i = 0; i < max_servers; i++ ){
              try {
            	stats[i].lock.lock();
            	if (stats[i].black == null)
            	  break;
              } finally {
            	stats[i].lock.unlock();
              }
//              if ( stats[i].black == null )
  //              break;
            }
          else
        	try {
        	  stats[msg.server].lock.lock();
        	  if (stats[msg.server].black != null)
        		i = max_servers;
        	} finally {
        	  stats[msg.server].lock.unlock();
        	}
//          	if ( stats[msg.server].black != null)
//          	    i = max_servers;

            // No free gamed for black.
          if ( i >= max_servers ) {
		    msg.response("191 No server with requested side free");
  		    socket.close();
  		    return;
		    //throw new IOException("second try for black player");
	      }
          // Assign the player
          if (debug_server == true)
            System.out.println("Assigning new black player to server " + i );
          try {
        	  stats[i].lock.lock();
        	  stats[i].black = socket;
          } finally {
        	  stats[i].lock.unlock();
          }
          //stats[i].black = socket;
	      if (daemon.time_controls) {
		    msg.response("101 " + daemon.secs(daemon.black_msecs) +	" " +
			    daemon.secs(daemon.white_msecs) + " Request accepted with time controls (you / opp)");
		    return;
	      }
	      msg.response("100 Request accepted");
	      try {
	    	stats[i].lock.lock();
	        if( stats[i].white != null && stats[i].black != null ) {
	          // START THE GAME
	          games[i] = new gamed( stats[i] );
	          Thread t = new Thread(games[i] );
	          t.start();
	        }
	      } finally {
	    	stats[i].lock.unlock();
	      }
	      return;
	    }
	    msg.response("999 Internal error");
	    socket.close();
	    throw new Error("internal error: Incorrect value for who");
      }
}
