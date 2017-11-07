/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2004
 * Company:
 * @author: Bert Peterson
 * @version 1.0
 */

public class Move {
    /**
     * Source square coordinates in the range 0-7, 0-7,
     * where the first coordinate is the column (x) value,
     * and the second is the row (y) value.
     */
    public int x1, y1;
    /**
     * Destination square coordinates in the range 0-7, 0-7,
     * where the first coordinate is the column (x) value,
     * and the second is the row (y) value.
     */
    public int x2, y2;

    public int value;            // How good this move was

    public Piece piece;          // The piece that is being moved
    public Piece captured_piece; // If a piece is captured store it as well.
    public int captured;         // As well as what was in the board square

    public Move next;            // Next move in the list.
    /**
     * Create a move object from a description of the
     * form "coord-coord" where "coord" is an algebraic square number.
     *
     * @param desc Move description.
     */
    public Move(String desc) {
	if (desc.length() != 5 || desc.charAt(2) != '-')
		if ( !desc.contentEquals(new StringBuffer("NONE")))
	        throw new IllegalArgumentException("bad move format");
	if (desc.contentEquals(new StringBuffer("NONE"))) {
        x1 = 0;
        y1 = 0;
        x2 = 0;
        y2 = 0;
	} else {
        x1 = move_letter(desc.charAt(0));
        y1 = move_digit(desc.charAt(1));
        x2 = move_letter(desc.charAt(3));
        y2 = move_digit(desc.charAt(4));
	}
        value = 0;
        captured = -1;
        next = null;
    }

    /**
     * Create a move object from the given starting and
     * ending coordinates in the range 0-7.
     *
     * @param x1 Starting column.
     * @param y1 Starting row.
     * @param x2 Ending column.
     * @param y2 Ending row.
     */
    public Move(int x1, int y1, int x2, int y2) {
	this.x1 = x1;
	this.y1 = y1;
	this.x2 = x2;
	this.y2 = y2;
        this.value = 0;
        this.captured = -1;
    }

    /**
     * Create a move object from the given starting and
     * ending coordinates in the range 0-7.
     *
     * @param x1 Starting column.
     * @param y1 Starting row.
     * @param x2 Ending column.
     * @param y2 Ending row.
     * @param value Hashed value from transpositionTable.
     */
    public Move(int x1, int y1, int x2, int y2, int value) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.value = value;
        this.captured = -1;
    }

    /**
     * This constructor takes the from, to locations, value and the move piece
     *
     * @param x1 int
     * @param y1 int
     * @param x2 int
     * @param y2 int
     * @param value int
     * @param p Piece
     */
    public Move(int x1, int y1, int x2, int y2, int value, Piece p) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.value = value;
        this.piece = p;
        this.captured = -1;
    }

    /**
     * Convert the column character to an integer
     *
     * @param ch Column character [a..h].
     * @return Integer [0..6]
     */
    private static int move_letter(char ch) {
	switch (ch) {
	case 'a': return 0;
	case 'b': return 1;
	case 'c': return 2;
	case 'd': return 3;
	case 'e': return 4;
	case 'f': return 5;
	case 'g': return 6;
	case 'h': return 7;
	}
	throw new IllegalArgumentException("bad move letter");
    }

    /**
     * Convert the row character to an integer
     *
     * @param ch Row character [1..7].
     * @return Integer [0..6].
     */
    private static int move_digit(char ch) {
	switch (ch) {
	case '1': return 0;
	case '2': return 1;
	case '3': return 2;
	case '4': return 3;
	case '5': return 4;
	case '6': return 5;
	case '7': return 6;
	case '8': return 7;
	}
	throw new IllegalArgumentException("bad move digit");
    }

    /**
     * Convert the square information into the correct LOA notation
     *
     * @param x Row.
     * @param y Column.
     * @return String representing a move location.
     */
    private static String square_name(int x, int y) {
	if (y < 0 || y >= 8)
	    throw new IllegalArgumentException("bad y coordinate in square");
	switch(x + 1) {
	case 1: return "a" + (y + 1);
	case 2: return "b" + (y + 1);
	case 3: return "c" + (y + 1);
	case 4: return "d" + (y + 1);
	case 5: return "e" + (y + 1);
	case 6: return "f" + (y + 1);
	case 7: return "g" + (y + 1);
	case 8: return "h" + (y + 1);
	}
	throw new IllegalArgumentException("bad x coordinate in square");
    }

    /**
     * Get a description of the move object.
     *
     * @return String of the form "coord-coord" where
     *         the coords are the starting and ending
     *         move coordinates.
     */
    public String name() {
	return square_name(x1, y1) + "-" + square_name(x2, y2);
    }

    /**
     * Copied the from and to locations, the piece moved, the piece captured,
     * and the pointer to the next Move.
     * Does not copy any of the stored quad information.
     *
     * @param m Move
     */
    public void copy(Move m) {
      x1 = m.x1;
      y1 = m.y1;
      x2 = m.x2;
      y2 = m.y2;
      piece = m.piece;
      captured_piece = m.captured_piece;
      value = m.value;
      next = m.next;
    }

    /**
     * Compares the from and to locations, returning true if the same.
     *
     * @param m Move
     * @return boolean
     */
    public boolean equal(Move m) {
        if (m == null)
            return false;
        if (m.x1 == x1 && m.y1 == y1 && m.x2 == x2 && m.y2 == y2)
            return true;
        return false;
    }
}
