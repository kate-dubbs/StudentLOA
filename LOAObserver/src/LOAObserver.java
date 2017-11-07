import java.applet.Applet;
import java.awt.*;
import java.io.*;
import java.awt.event.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 */

public class LOAObserver extends Applet implements Runnable {
	static final long serialVersionUID = 200;
    Thread animation;
    Graphics offscreen;    // Declaration of offscreen buffer
    Image image;           // Image associated with the buffer
    private Image background, black_piece, white_piece; // The artwork
    private CustomCanvas canvas;   // The canvas that is drawn to, and mouse input received from

    ObserverConnection client;
    Board onscreen = new Board();      // The onscreen LOA board.

    private String status;

    private final int server = 0;
    private final String host = "BEATRICE";
    
    private boolean playing = true;

    Choice choice1 = new Choice();     // All of the wonderfull GUI info that is not used. yet.
    Choice choice2 = new Choice();
    List textPanel = new List();
    Label label1 = new Label();
    Label label2 = new Label();
    GridBagLayout gridBagLayout2 = new GridBagLayout();

    /**
     * The init function, instantiates all the miscellaneous variables, and
     * loads the graphics. Note that the double buffer is created in start,
     * this was because the buffer image object requires the Applet window to attach
     * to.
     */
    public void init(){
        System.out.println("<<init>>");
        MediaTracker imageTracker = new MediaTracker( this );

        background = getImage( getDocumentBase(), "LOA-Grid.png" );
        imageTracker.addImage( background, 0 );
        black_piece = getImage( getDocumentBase(), "LOA-Black.png");
        imageTracker.addImage( black_piece, 1 );
        white_piece = getImage( getDocumentBase(), "LOA-White.png" );
        imageTracker.addImage( white_piece, 2);

        try {
            for ( int i = 0; i < 3; i++ )
                imageTracker.waitForID(i);
        } catch( InterruptedException e ) {}
        System.out.println( "Connecting to server: " + server + " requesting observer");
        try {
          client = new ObserverConnection(host, server, onscreen);
        } catch( IOException e) {
          System.out.println("Unable to connect to server" );
          e.printStackTrace();
        }
    }

    /**
     * Start method for the thread, creates the offscreen image and Graphic
     * association and then maintains itself.
     */
    public void start() {
        System.out.println(">> start <<");
        image = createImage(300,300);       // allocation of offscreen
        offscreen = image.getGraphics();   //               buffer

        animation = new Thread(this);
        if (animation != null) {
            animation.start();
        }
    }

    /**
     * The overridden paint function, copies the background and all of the other
     * graphics bits to the background Graphic that will be updated when we
     * call canvas.repaint at the end.
     */
    public void paint( Graphics g ){
      showStatus( status );
      // Copy the background image
      offscreen.drawImage( background, 0, 0, 300, 300, this );
      // Place each piece in the correct location.
      for ( int x = 0; x < 8; x++ )
        for ( int y = 0; y < 8; y++ ) {
          if ( onscreen.checker_of( 1, x, y ) )
            offscreen.drawImage( black_piece, (x)*35+11, (7-y)*35+11, 30, 30, this );
          if ( onscreen.checker_of( 0, x, y ) )
            offscreen.drawImage( white_piece, (x)*35+11, (7-y)*35+11, 30, 30, this );
        }
      canvas.repaint();
    }

    /**
     * The run method from the thread, loops through the player order till the
     * game is over. Black player followed by white.
     */
    public void run() {
      repaint();
      showStatus( status );
      while (playing) {
	    get_comp_move();
      }
      while (true)
      	;
    }

    /**
     * get_comp_move()
     */
    public void get_comp_move() {
      int result= 0;

      try {
        result = client.get_move();
      } catch (IOException e ) {
        System.out.println("Unable to get move from server");
        e.printStackTrace();
      }
      if ( result == 0 )
        return;
      onscreen.print(System.out); // Print the board to the console
      textPanel.add( client.color + ": " +client.move.name() );
//      textPanel.add( "Time: " + (float)(startTime)/1000.0 + " s" );
      if ( onscreen.game_state == Board.GAME_OVER )
      {
          status = "GAME OVER " + onscreen.referee() + " wins!";
          textPanel.add( onscreen.referee() + " wins!");
          playing = false;
      }
      repaint();
    }

    /**
     * Thread stop function, just recalls itself.
     */
    public void stop() {
        System.out.println(">> stop <<");
            if (animation != null) {
                animation.interrupt();
                animation = null;
            }
    }

    /**
     * Most of this wonderful block of code was written by Borland JBuilder so
     * I wouldn't have to deal with tweaking the stupid GUI items.
     */
  public LOAObserver() {
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    this.setLayout(gridBagLayout2);
    label1.setText("Depth");
    choice1.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        choice1_itemStateChanged(e);
      }
    });
    // The depth choices 3, 5, 7, 8, 11, 13, 15 and 21.
    choice1.add( "3" );
    choice1.add( "5" );
    choice1.add( "7" );
    choice1.add( "9" );
    choice1.add( "11" );
    choice1.add( "13" );
    choice1.add( "15" );
    choice1.add( "21" );
    label2.setText("Time");
    choice2.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        choice2_itemStateChanged(e);
      }
    });
    canvas = new CustomCanvas( this );
    this.add(canvas,  new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 14, 0), 290, 290));
    this.add(textPanel,        new GridBagConstraints(1, 2, 2, 1, 1.0, 0.8
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 14, 9), 0, 0));
    this.add(choice1,    new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 0, 0, 9), 50, 2));
    this.add(choice2,          new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 0, 0, 9), 50, 2));
    this.add(label1,   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 10, 0, 0), 51, 6));
    this.add(label2,    new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(9, 10, 0, 0), 51, 6));
  }

  /**
   * If the choice1 box for the depth changes, change the search depth.
   */
  void choice1_itemStateChanged(ItemEvent e) {}
  void choice2_itemStateChanged(ItemEvent e) {}
}

