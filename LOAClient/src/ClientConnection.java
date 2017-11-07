import java.io.*;
import java.net.*;
import java.util.*;


/**
 * Title:        LOADaemon
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 */

public class ClientConnection {
    /**
     * Game Client constants
     * @param STATE_CONTINUE  Game will continue.
     * @param STATE_DONE  Game over.
     */
    public final static int STATE_CONTINUE = 0;
    public final static int STATE_DONE = 1;

    /**
     * Specific state information
     * @param who       Which side this client is playing.
     * @param winner    If done, player that won.
     * @param move      Move returned by get_move() method as side-effect.
     */
    public int who = Board.NONE;
    public int winner = Board.NONE;
    public Move move;

    /**
     * Time control information
     * @param time_controls         True if playing under time controls.
     * @param white_time_control    Number of seconds white player has at start
     *                              of the game, if time_controls == true.
     * @param black_time_control    Number of seconds black player has at start
     *                              of the game, if time_controls == true.
     * @param my_time               Number of seconds the client has currently
     *                              remaining, if time_controls == true.
     * @param opp_time              Number of seconds the client's opponent has
     *                              currently remaining, if time_controls == true.
     */
    public boolean time_controls = false;
    public int white_time_control;
    public int black_time_control;
    public int my_time;
    public int opp_time;

    private final static String client_version = "alpha";
    private Socket sock = null;
    private BufferedReader fsock_in = null;
    private PrintWriter fsock_out = null;
    private final static int server_base = 4000;

    private String msg_txt;
    private int msg_code;
    private int serial = 0;
    private int side;

    private final boolean DEBUG = true;
    
    /**
     * get_msg
     */
    private void get_msg()
      throws IOException {
		String buf = fsock_in.readLine();
		if (buf == null)
		    throw new IOException("read failed");
		int len = buf.length();
		if (len < 4)
		    throw new IOException("short read");
		if (!Character.isDigit(buf.charAt(0)) ||
		    !Character.isDigit(buf.charAt(1)) ||
		    !Character.isDigit(buf.charAt(2)) ||
		    buf.charAt(3) != ' ')
		    throw new IOException("ill-formatted response code");
		msg_txt = buf.substring(4);
		msg_code = Integer.parseInt(buf.substring(0, 3));
    }

    /**
     * opponent()
     *
     * @param int w
     */
    private int opponent(int w) {
	if (w != Board.PLAYER_WHITE && w != Board.PLAYER_BLACK)
	    throw new Error("internal error: funny who");
	if (w == Board.PLAYER_WHITE)
	    return Board.PLAYER_BLACK;
	return Board.PLAYER_WHITE;
    }

    /**
     * parse_move()
     *
     */
    private Move parse_move()
      throws IOException {
	Move m = null;
	try {
	    StringTokenizer toks = new StringTokenizer(msg_txt);

	    int nserial = Integer.parseInt(toks.nextToken());
	    if (serial != nserial)
		throw new IOException("synchronization lost: expected " +
				  serial + " got " + nserial);
	    switch(msg_code) {
	    case 312:
	    case 314:
	    case 323:
	    case 324:
	    case 326:
		String ellipses = toks.nextToken();
		if(!ellipses.equals("..."))
		    throw new IOException("expected ellipsis, got " + ellipses);
	    }
	    String ms = toks.nextToken();
	    m = new Move(ms);
	    switch (msg_code) {
	    case 313:
	    case 314:
		int tc = Integer.parseInt(toks.nextToken());
		int whose_move = Board.PLAYER_BLACK;
		if (msg_code == 314)
		    whose_move = Board.PLAYER_WHITE;
		if (whose_move == who)
		    my_time = tc;
		else
		    opp_time = tc;
	    }
	} catch(NoSuchElementException e) {
	    throw new IOException("missing argument in opponent move");
	} catch(Exception e) {
	  e.printStackTrace();
	  System.out.println("MESSAGE: " + msg_txt);
	  System.exit(1);
	}
	return m;
    }

    /**
     * get_time_controls()
     */
    private void get_time_controls()
      throws IOException
    {
	int i, j;
	for (i = 0; i < msg_txt.length(); i++)
	    if (Character.isDigit(msg_txt.charAt(i)))
		break;
	if (i >= msg_txt.length())
	    throw new IOException("cannot find time controls in message text");
	j = i;
	while(Character.isDigit(msg_txt.charAt(j)))
	    j++;
	white_time_control = Integer.parseInt(msg_txt.substring(i, j));
	i = j;
	while(!Character.isDigit(msg_txt.charAt(i)))
	    i++;
	j = i;
	while(Character.isDigit(msg_txt.charAt(j)))
	    j++;
	black_time_control = Integer.parseInt(msg_txt.substring(i, j));
    }

    /**
     * get_time()
     */
    private int get_time()
      throws IOException
    {
	int i, j;
	for (i = 0; i < msg_txt.length(); i++)
	    if (Character.isDigit(msg_txt.charAt(i)))
		break;
	if (i >= msg_txt.length())
	    throw new IOException("cannot find time in message text");
	j = i;
	while(Character.isDigit(msg_txt.charAt(j)))
	    j++;
	return Integer.parseInt(msg_txt.substring(i, j));
    }

    /**
     * close()
     */
    private void close()
      throws IOException {
	if (sock == null)
	    return;
	try {
	    fsock_out.close();
	    fsock_in.close();
	    sock.close();
	} finally {
	    fsock_out = null;
	    fsock_in = null;
	    sock = null;
	}
    }

    /**
     * zeropad()
     *
     * @param int n
     */
    private String zeropad(int n) {
	if (n > 99)
	    return "" + n;
	if (n > 9)
	    return "0" + n;
	return "00" + n;
    }

    /**
     * flushout()
     */
    private void flushout()
      throws IOException {
	fsock_out.print("\r");
	fsock_out.flush();
    }

    /**
     * Construct a game client, connected to the specified
     * server and ready to play.
     *
     * @param s    (side)Should be either WHO_WHITE or WHO_BLACK.
     *             Side the client will play.
     * @param host Hostname of the server.
     * @param server Server number of server on host.
     * @throws IOException Unable to connect to specified server
     *                     as specified side.
     */
    public ClientConnection(int s, String host, int server)
      throws IOException {
    	side = s;
	    InetAddress addr = InetAddress.getByName(host);
	    sock = new Socket(addr, server_base );
	    InputStream instream = sock.getInputStream();
	    fsock_in = new BufferedReader(new InputStreamReader(instream));
	    OutputStream outstream = sock.getOutputStream();
	    fsock_out = new PrintWriter( new BufferedOutputStream(outstream));
    }
        
    public boolean NegotiateConnection() {
        int req_server = 0;
        try {
    	  get_msg();
        } catch (IOException e) {
          System.out.println(e);
          return false;
        }
        if (msg_code != 0) {
          System.out.println("illegal greeting " + zeropad(msg_code));
          return false;
        }
        try {
	      fsock_out.print(client_version + " player ");
	      fsock_out.print(req_server);
	      if (side == Board.PLAYER_WHITE)
	        fsock_out.print(" white ");
	      else
	        fsock_out.print(" black ");
	      flushout();
          System.out.println("Waiting sent message");
	      get_msg();
        } catch (IOException e) {
	      System.out.println(e);
	      return false;
	    }
        if (DEBUG)
          System.out.println(msg_code);
	    if (msg_code != 100 && msg_code != 101) {
	      System.out.println("side failure " + zeropad(msg_code));
	      return false;
	    }
	    if (msg_code == 101) {
	      time_controls = true;
	      try {
	        get_time_controls();
	      } catch (IOException e) {
	        System.out.println(e);
	        return false;
	      }
	      if (side == Board.PLAYER_WHITE) {
		    my_time = white_time_control;
		    opp_time = black_time_control;
	      } else {
		    opp_time = black_time_control;
		    my_time = white_time_control;
	      }
	    }
	    try {
	      get_msg();
	      if (DEBUG)
	    	System.out.println(msg_code);
        } catch (IOException e) {
          System.out.println(e);
          return false;
        }
	    if ((msg_code != 151 && side == Board.PLAYER_WHITE) ||
	        (msg_code != 152 && side == Board.PLAYER_BLACK)){
	      System.out.println( "side failure " + zeropad(msg_code));
	      return false;
        }
	    who = side;
	    return true;
    }

    /**
     * Make a move on the server.  The server must be expecting
     * a move (that is, it must be this client's turn), and
     * the move must be legal.
     * @param m What move to make.
     * @returns Will be either STATE_CONTINUE or STATE_DONE.
     * @throws IOException Move is illegal or communication failed.
     */
    public int make_move(Move m)
      throws IOException {
	    String ellipses = "";

	    if (who == Board.NONE)
	      throw new IOException("not initialized");
	    if (winner != Board.NONE)
	      throw new IOException("game over");
	    if (who == Board.PLAYER_BLACK)
	      serial++;
	    if (who == Board.PLAYER_WHITE)
	      ellipses = " ...";
	    if ( m == null)
	      fsock_out.print(serial + ellipses + " NONE");
	    else
	      fsock_out.print(serial + ellipses + " " + m.name());
	    flushout();
	    get_msg();
	    if ( DEBUG )
	      System.out.println("cc.make_move: " + msg_code + " " + msg_txt);
	    switch(msg_code) {
	    case 201:
	      winner = who;
	      break;
	    case 202:
	      winner = opponent(who);
	      break;
	    case 203:
	      winner = Board.NONE;
	      break;
	    case 321:
	    case 324:
	      winner = Board.PLAYER_BLACK;
	      break;
	    case 322:
	    case 323:
	      winner = Board.PLAYER_WHITE;
	      break;
	    case 325:
	      winner = Board.OBSERVER;
	    }
	    if (winner != Board.NONE) {
	      close();
	      return STATE_DONE;
	    }
	    if (msg_code != 200 && msg_code != 207)
	      throw new IOException("bad result code " + zeropad(msg_code));
	    if (msg_code == 207)
	      my_time = get_time();
	    get_msg();
	    if (msg_code < 311 || msg_code > 314)
	      throw new IOException("bad status code " + zeropad(msg_code));
	    return STATE_CONTINUE;
    }

    /**
     * Get a move from the server.  The server must be expecting
     * a move from the opponent (that is, it must be this client's
     * opponent's turn).  The <tt>move</tt> field will indicate
     * the returned move.
     * @returns Will be either STATE_CONTINUE or STATE_DONE.
     * @throws IOException Move is illegal or communication failed.
     */
    public int get_move()
      throws IOException {
	if (who == Board.NONE)
	    throw new IOException("not initialized");
	if (winner != Board.NONE)
	    throw new IOException("game over");
	if (who == Board.PLAYER_WHITE)
	    serial++;
	get_msg();
	if ( DEBUG )
        System.out.println("cc.get_move: " + msg_code + " " + msg_txt);
	if ((msg_code < 311 || msg_code > 326) &&
	    msg_code != 361 && msg_code != 362)
	    throw new IOException("bad status code " + zeropad(msg_code));
	if ((who == Board.PLAYER_WHITE &&
	     (msg_code == 312 || msg_code == 314 || msg_code == 323 ||
	      msg_code == 324 || msg_code == 326)) ||
	    (who == Board.PLAYER_BLACK &&
	     (msg_code == 311 || msg_code == 313 || msg_code == 321 ||
	      msg_code == 322 || msg_code == 325)))
	    throw new IOException("status code " + zeropad(msg_code) + " from wrong side");
	switch(who) {
	case Board.PLAYER_WHITE:
	    switch(msg_code) {
	    case 311:
	    case 313:
		move = parse_move();
		return STATE_CONTINUE;
	    case 321:
		move = parse_move();
		winner = Board.PLAYER_BLACK;
		return STATE_DONE;
	    case 361:
		winner = Board.PLAYER_BLACK;
		return STATE_DONE;
	    case 322:
		move = parse_move();
		winner = Board.PLAYER_WHITE;
		return STATE_DONE;
	    case 362:
		winner = Board.PLAYER_WHITE;
		return STATE_DONE;
	    case 325:
		move = parse_move();
		winner = Board.NONE;
		return STATE_DONE;
	    }
	    break;
	case Board.PLAYER_BLACK:
	    switch(msg_code) {
	    case 312:
	    case 314:
		move = parse_move();
		return STATE_CONTINUE;
	    case 323:
		move = parse_move();
		winner = Board.PLAYER_WHITE;
		return STATE_DONE;
	    case 362:
		winner = Board.PLAYER_WHITE;
		return STATE_DONE;
	    case 324:
		move = parse_move();
		winner = Board.PLAYER_BLACK;
		return STATE_DONE;
	    case 361:
		winner = Board.PLAYER_BLACK;
		return STATE_DONE;
	    case 326:
		move = parse_move();
		winner = Board.NONE;
		return STATE_DONE;
	    }
	    break;
	}
	throw new IOException("unknown status code " + zeropad(msg_code));
    }

    /**
     * Internal method.
     */
    protected void finalize()
	throws Throwable {
	close();
    }

}
