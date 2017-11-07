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

public class ObserverConnection {
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
    public String color;
    Board board;

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
     * @param side Should be either WHO_WHITE or WHO_BLACK.
     *             Side the client will play.
     * @param host Hostname of the server.
     * @param server Server number of server on host.
     * @throws IOException Unable to connect to specified server
     *                     as specified side.
     */
    public ObserverConnection(String host, int server, Board b)
      throws IOException {
        board = b;
	InetAddress addr = InetAddress.getByName(host);
	sock = new Socket(addr, server_base );
	InputStream instream = sock.getInputStream();
	fsock_in = new BufferedReader(new InputStreamReader(instream));
	OutputStream outstream = sock.getOutputStream();
	fsock_out = new PrintWriter( new BufferedOutputStream(outstream));

	get_msg();
	if (msg_code != 0)
	    throw new IOException("illegal greeeting " + zeropad(msg_code));
	fsock_out.print(client_version + " observer " + server);
	flushout();
        System.out.println("Waiting sent message");
	get_msg();
        System.out.println( msg_code);
	if (msg_code != 100 && msg_code != 101)
	    throw new IOException("side failure " + zeropad(msg_code));
	if (msg_code == 101) {
	    time_controls = true;
	    get_time_controls();
            my_time = white_time_control;
            opp_time = black_time_control;
	}
	get_msg();
        System.out.println(msg_code);
	if (msg_code != 153)
	    throw new IOException("side failure " + zeropad(msg_code));
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
	get_msg();
        System.out.println(msg_code);
	if ((msg_code < 320 || msg_code > 326) &&
            (msg_code < 380 || msg_code > 384) && msg_code != 153)
	    throw new IOException("bad status code " + zeropad(msg_code));
        switch(msg_code) {
          case 153:
            return 0;
	      case 381:
          case 383:
            color = "Black";
            break;
	      case 382:
          case 384:
            color = "White";
            break;
          case 321:
          case 324:
          case 322:
          case 323:
          	board.game_state = Board.GAME_OVER;
          	return 3;
        }
        StringTokenizer toks = new StringTokenizer(msg_txt);
        int nserial = Integer.parseInt(toks.nextToken());
        serial = nserial;
        String b = toks.nextToken();
        board.new_layout(b);
        String ms = toks.nextToken();
	move = new Move(ms);
	    /*
		int tc = Integer.parseInt(toks.nextToken());
		int whose_move = Board.PLAYER_BLACK;
		if (msg_code == 314)
		    whose_move = Board.PLAYER_WHITE;
		if (whose_move == who)
		    my_time = tc;
		else
		    opp_time = tc;
*/
//	throw new IOException("unknown status code " + zeropad(msg_code));
      return 1;
    }

    /**
     * Internal method.
     */
    protected void finalize()
	throws Throwable {
	close();
    }
}
