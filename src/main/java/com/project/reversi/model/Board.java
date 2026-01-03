package com.project.reversi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {

  private final int numRows;
  private final int numColumns;
  private final Piece[][] board;
  private int blackCount;
  private int whiteCount;


  public Board(int nbRows, int nbColumns) {
    numRows = nbRows;
    numColumns = nbColumns;
    board = new Piece[numRows][numColumns];
    int middleRow = (numRows - 1) / 2;
    int middleColumn = (numColumns - 1) / 2;
    board[middleRow][middleColumn] = new Piece(PlayerColor.WHITE);
    board[middleRow + 1][middleColumn] = new Piece(PlayerColor.BLACK);
    board[middleRow + 1][middleColumn + 1] = new Piece(PlayerColor.WHITE);
    board[middleRow][middleColumn + 1] = new Piece(PlayerColor.BLACK);
    blackCount = 2;
    whiteCount = 2;
  }


  public static Board fromSnapshot(List<List<String>> snapshot) {
    if (snapshot == null || snapshot.isEmpty()) {
      throw new IllegalArgumentException("Snapshot cannot be null or empty");
    }
    int rows = snapshot.size();
    int cols = snapshot.get(0).size();
    Board board = new Board(rows, cols);
    board.loadState(snapshot);
    return board;
  }

  public Board copyBoard() {
    List<List<String>> snapshot = new ArrayList<>();
    for (int i = 0; i < numRows; i++) {
      snapshot.add(new ArrayList<>(numColumns));
    }
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {

        if (board[i][j] != null && board[i][j].getColor().equals(PlayerColor.WHITE)) {
          snapshot.get(i).add("W");
        } else if (board[i][j] != null) {
          snapshot.get(i).add("B");
        } else {
          snapshot.get(i).add(" ");
        }
      }
    }
    Board boardCopy = new Board(numRows, numColumns);
    boardCopy.loadState(snapshot);
    return boardCopy;
  }


  public void loadState(List<List<String>> snapshot) {
    if (snapshot.size() != numRows || snapshot.get(0).size() != numColumns) {
      throw new IllegalArgumentException("Snapshot dimensions do not match board size");
    }
    blackCount = 0;
    whiteCount = 0;
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
        String val = snapshot.get(i).get(j);
        if ("B".equalsIgnoreCase(val)) {
          board[i][j] = new Piece(PlayerColor.BLACK);
          blackCount++;
        } else if ("W".equalsIgnoreCase(val)) {
          board[i][j] = new Piece(PlayerColor.WHITE);
          whiteCount++;
        } else {
          board[i][j] = null;
        }
      }
    }
  }

  public void printBoard() {
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
        System.out.println(board[i][j] != null ? board[i][j].toString() : "-");
        System.out.print(",");
      }
      System.out.println();
    }

  }

  private boolean isOutOfBounds(int row, int column) {
    return row < 0 || row >= numRows || column < 0 || column >= numColumns;
  }

  public List<Piece> makeMove(final int row, final int column, final PlayerColor color, boolean simuMode) {
    List<Piece> flippedPieces = new ArrayList<>();
    // Check if the move is out-of-bounds or the cell is already occupied.
    if (isOutOfBounds(row, column) || board[row][column] != null) {
      return flippedPieces;
    }
    // Define all 8 possible directions.
    int[][] directions = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1}, {0, 1},
        {1, -1}, {1, 0}, {1, 1}
    };
    // For each direction, calculate flips if the immediate neighbor is an opponent.
    for (int[] dir : directions) {
      int deltaRow = dir[0];
      int deltaCol = dir[1];
      int neighborRow = row + deltaRow;
      int neighborCol = column + deltaCol;

      if (isOutOfBounds(neighborRow, neighborCol)) {
        continue;
      }
      Piece neighborPiece = board[neighborRow][neighborCol];
      if (neighborPiece == null || neighborPiece.getColor() == color) {
        continue;
      }

      // Get flips in this direction.
      List<Piece> flips = getFlipsInDirection(row, column, deltaRow, deltaCol, color);
      flippedPieces.addAll(flips);
    }

    if (simuMode || flippedPieces.isEmpty()) {
      return flippedPieces;
    }
    for (Piece piece : flippedPieces) {
      piece.flip();
    }
    board[row][column] = new Piece(color);
    updateScore(color, flippedPieces.size() + 1);
    return flippedPieces;

  }


  public void updateScore(PlayerColor newColor, int newPieces) {
    /* If we added x pieces of a color, then we actually removed x - 1 pieces of the other
     * color. The -1 is because one of the new pieces was the just-placed one.
     */
    if (newColor == PlayerColor.BLACK) {
      whiteCount -= newPieces - 1;
      blackCount += newPieces;
    } else {
      blackCount -= newPieces - 1;
      whiteCount += newPieces;
    }
  }

  public void updateScoreUndo(PlayerColor newColor, int newPieces) {
    if (newColor == PlayerColor.BLACK) {
      whiteCount -= newPieces + 1;
      blackCount += newPieces;
    } else {
      blackCount -= newPieces + 1;
      whiteCount += newPieces;
    }
  }

  public void undoMove(int row, int col, PlayerColor color, List<Piece> cellsCaptured) {
    for (Piece piece : cellsCaptured) {
      piece.flip();
    }
    board[row][col] = null;
    updateScoreUndo(color.opposite(), cellsCaptured.size());
  }

  /**
   * Returns the list of opponent pieces that would be flipped in the given direction,
   * starting from (row, col). If the direction does not end with a piece of the same PlayerColor,
   * an empty list is returned.
   */
  private List<Piece> getFlipsInDirection(int row, int col, int deltaRow, int deltaCol, PlayerColor color) {
    List<Piece> flips = new ArrayList<>();
    int r = row + deltaRow;
    int c = col + deltaCol;

    while (!isOutOfBounds(r, c)) {
      Piece piece = board[r][c];
      if (piece == null) {
        // No enclosing piece; invalid direction.
        return Collections.emptyList();
      } else {
        if (piece.getColor() == color) {
          // Only valid if at least one opponent piece is in between.
          return flips.isEmpty() ? Collections.emptyList() : flips;
        } else {
          flips.add(piece);
        }
      }
      r += deltaRow;
      c += deltaCol;
    }
    // Went off-board without closing with a piece of the same PlayerColor.
    return Collections.emptyList();
  }

  public int getPieceCount(PlayerColor color) {
    if (color == PlayerColor.BLACK) {
      return blackCount;
    } else {
      return whiteCount;
    }
  }

  public int getNumRows() {
    return numRows;
  }

  public int getNumColumns() {
    return numColumns;
  }

  public Piece getPiece(int row, int column) {
    return board[row][column];
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
        sb.append(board[i][j] != null ? board[i][j] : "-");
        sb.append(",");
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
