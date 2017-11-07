import java.awt.*;
import java.awt.event.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 */

/**
 * This is a custom canvas class so that the paint method of the canvas component
 * can be overridden. This is NOT the way to make a graphics intensive Java Applet
 * game. I am only doing it here because I need to have the other GUI components
 * available, and this game is more event driven than graphics driven.
 */
public class CustomCanvas extends Canvas implements MouseListener, MouseMotionListener {
  static final long serialVersionUID = 201;
  private LOAObserver l;
  public static final int NOT_MOVE = 1000;
  public static final int PICK_PIECE = 1001;
  public static final int PICK_MOVE = 1002;
  public static final int END_MOVE = 1003;

  /**
   * CustomCanvas constructor takes the main class as a parameter. The class has
   * the image that will be updating publicly available. This makes a small amount
   * of more sense than having this class perform all of the work.
   *
   * @param l loa the main games class.
   */
  public CustomCanvas( LOAObserver L) {
    l = L;
    addMouseListener( this );
    addMouseMotionListener( this );
  }

  /**
   * This is the overridden paint method. It just draws the offscreen image to
   * the graphic canvas. If you move this line to the paint() function in loa,
   * remove all components, and have the while(true) loop in loa call repaint()
   * you will have a graphics game core.
   */
  public void paint( Graphics g ) {
    if ( l != null )
      g.drawImage(l.image, 0, 0, null );
  }

  public void mouseClicked( MouseEvent e ) {
/*    int grid_x, grid_y;
    String temp;
    if ( l.user_move != NOT_MOVE ) {
      x = e.getX();
      y = e.getY();
      grid_x = (int)(x/35);
      grid_y = (7 - (int)(y/35));
      temp = l.user_move + " ["+grid_x+", "+grid_y+"]";
      System.out.println( temp );
      if (grid_x > 7 || grid_x < 0 || grid_y > 7 || grid_y < 0)
        return;
      if ( l.user_move == PICK_PIECE ) {
        l.x1 = grid_x;
        l.y1 = grid_y;
        l.user_move = PICK_MOVE;
        return;
      }
      else if (l.user_move == PICK_MOVE ) {
        if ( l.board.piece( grid_x, grid_y ) ) {
          l.x1 = grid_x;
          l.y1 = grid_y;
          l.x2 = l.y2 = -1;
          l.user_move = END_MOVE;
          return;
        }
        l.x2 = grid_x;
        l.y2 = grid_y;
        l.user_move = END_MOVE;
      }
    }*/
  }

  /**
   * All of the other mouse events that we are not going to worry about catching
   * but still have to have because of the Listeners.
   */
  public void mousePressed( MouseEvent e ) { }
  public void mouseReleased( MouseEvent e ) { }
  public void mouseEntered( MouseEvent e ) {}
  public void mouseExited( MouseEvent e ) {}
  public void mouseMoved( MouseEvent e ) {}
  public void mouseDragged( MouseEvent e) {}
}