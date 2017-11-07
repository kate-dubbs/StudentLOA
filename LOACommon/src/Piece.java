/**
 * Title:
 * Description:
 * Copyright: Copyright (c) 2004
 * Company:
 * @author: Bert Peterson
 * @version 1.0
 */

public class Piece {
  public int x;
  public int y;
  public int owner;
  public Piece prev;
  public Piece next;

  // Constructor
  Piece(int X, int Y, int own) {
      x = X;
      y = Y;
      owner = own;
      next = null;
  }

  // Constructor
  Piece(int X, int Y, int own, Piece n, Piece p) {
      x = X;
      y = Y;
      owner = own;
      next = n;
      prev = p;
      if (next != null)
        next.prev = this;
  }
}
