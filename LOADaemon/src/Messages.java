import java.io.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 *
 * The message class controls the input and output socket streams for communication
 * to the clients
 */

public class Messages {
    private InputStream instream;
    private OutputStream outstream;
    private BufferedInputStream reader;
    private PrintWriter writer;
    private static final boolean request_debug = false;

    public static final String pit_name[] = {
	"a", "b", "c", "d", "e", "f",
	"A", "B", "C", "D", "E", "F"
    };
    
    public int server;

    /**
     * Constructor
     * Initializes the input and output streams associated with the socket.
     */
    public Messages(InputStream instream, OutputStream outstream) {
	this.instream = instream;
	this.reader = new BufferedInputStream(instream);
	this.outstream = outstream;
        this.writer = new PrintWriter(new BufferedOutputStream(outstream));
    }

    /**
     * Helper function to pull the next white space deliminated identifier from
     * the string.
     */
    public String get_id(StringChars req) {
	if (req == null)
	    return null;
	char ch;
	while(req.length() > 0 && ((ch = req.charAt(0)) == ' ' || ch == '\t'))
	    req.deleteFirstChar();
	int n = req.length();
	if (n == 0)
	    return null;
	int i;
	for (i = 0; i < n; i++) {
	    ch = req.charAt(i);
	    if (ch == ' ' || ch == '\t')
		break;
	}
	String result = req.substring(0, i);
	req.deleteFirstChars(i);
	return result;
    }

    /**
     *
     */
    public StringChars request()
      throws IOException {
	if (request_debug)
	    System.err.println("entering request()");
	// cr-terminated ASCII on instream
	StringBuffer result = new StringBuffer();
	int chi;
	boolean garbage;
	do {
	    chi = reader.read();
	    if (chi == -1)
		return null;
	    char ch = (char) chi;
	    garbage = (ch == '\r') || (ch == '\n');
	    if (request_debug) {
			if (garbage)
				System.err.println("discarding garbage");
		}
	} while (garbage);
	while(chi != -1) {
	    char ch = (char) chi;
	    if (ch == '\r')
		break;
	    result.append(ch);
	    chi = reader.read();
	}
	if (request_debug)
	    System.err.println("leaving request() result: " + result);
	return new StringChars(result);
    }

    public void response(String m) {
	  // crlf-terminated ASCII on outstream
      System.out.println( "  Message: "+m);
	  writer.print(m + "\r\n");
	  writer.flush();
    }

    @SuppressWarnings("unused")
	public void send_greeting() {
	  if (false) {
		if (daemon.time_controls) {
		    response("001 " + daemon.version + " " + daemon.secs(daemon.white_msecs) +
			     " " + daemon.secs(daemon.black_msecs) + " version white-secs black-secs " +
			     daemon.name + " server says hello!");
		    return;
		  }
	}
	  response("000 " + daemon.version + " version " + daemon.name + " server says hello!");
    }

    /**
     * Request side dialogue
     * want_white	<version> player white <optional-name>	Will play white
     * want_black	<version> player black <optional-name>	Will play black
     * want_observe	<version> observer <optional-name>	Will observer
     *
     * seat_granted	100	                Request accepted
     * set_granted_tc	101 <ses> <opp_secs>	Request accepted with time controls
     *          	19x	                Request not accepted
     * seat_taken	191	                Other player holds requested side
     * seat_private	193	                Cannot observe
     * seat_illegal	198	                Illegal version number
     * seat_garbled	199	                Request not understood
     */
    @SuppressWarnings("unused")
	public int req_side() {
	  StringChars req;
	  try {
	    req = request();
	    if (request_debug == true)
	      System.out.println("Request ***"+req.substring(0, req.length())+"***");
	  } catch(IOException e) {
	    return -9;
	  }
	  String version = get_id(req);
	  if (version == null || !version.equals(daemon.version)) {
	    response( "198 Illegal version number (expected " + daemon.version +
		          " got " + version + ")");
	    return -9;
	  }
	  String player = get_id(req);
      Integer i = new Integer(get_id(req));
      server = i.intValue();
      if (request_debug == true)
        System.out.println ("Server: " +server);
      if ( server < 0 || server > daemon.max_servers )
        response("197 Invalid server number");
	  if (player != null && player.equals("observer")) {
        return 10+server;
      }
	  if (player == null || !player.equals("player")) {
	    response("199 Request not understood");
	    return -9;
	  }
	  String which_player = get_id(req);
	  if (which_player != null && which_player.equals("white"))
	    return Board.PLAYER_WHITE;
	  if (which_player != null && which_player.equals("black"))
	    return Board.PLAYER_BLACK;
      response("199 Request not understood");
	  return -9;
    }

    /**
     * waits for move request from the player.
     * The message should be in the format:
     *  <move-number> <ellipses-if-player-white> <move>
     */
    public Move req_move(int serial, int to_move)
      throws IOException {
        StringChars req = request();
	if (req == null)
	    throw new IOException("failed read for player " + to_move);
        // Get and check the serials to make sure that they are moving on the correct move.
	String rserial = get_id(req);
	if (rserial == null || !rserial.equals(serial + "")) {
	    response("299 Illegal serial number (got " + rserial + " expected " +
		     serial + ")");
	    return null;
	}
	if (to_move == Board.PLAYER_WHITE) {
	    String ellipses = get_id(req);
	    if (ellipses == null || !ellipses.equals("...")) {
		response("299 Ellipses (...) expected before move");
		return null;
	    }
	}
	String move = get_id(req);
	try {
	    Move m = new Move(move);
	    return m;
	} catch (IllegalArgumentException e) {
	    System.out.println("Illegal move " + move + ": " + e.getMessage());
	    response("199 Request not understood");
	}
	return null;
    }

    /**
     * For shutdown, closes the input and output streams. MUST be called before
     * destroying the socket.
     */
    public void close_streams() {
      try {
        instream.close();
        outstream.close();
      } catch ( IOException e ) {
        e.printStackTrace();
      }
    }
}
