import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Board
{

  private final int numRows;
  private final int numColumns;
  private Cell[][] cells;

  private HashSet<Piece> cellsToFlip;
  private HashSet<Piece> cellsChanged;
  private HashSet<Cell> cellsToHighlight;

  private int blackCount;
  private int whiteCount;

  private Map<Color,PositionStats> positionStats;
  private final static String ROW_MAX = "row_max";
  private final static String ROW_MIN = "row_min";
  private final static String COLUMN_MAX = "column_max";
  private final static String COLUMN_MIN = "column_min";


  public Board(int nbRows, int nbColumns)
  {
    numRows = nbRows;
    numColumns = nbColumns;
    initBoard();

  }

  private void initBoard()
  {
    cells = new Cell[numRows][numColumns];
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
        cells[i][j] = new EmptyCell(i, j);
      }
    }
    int middleRow = (numRows - 1) / 2;
    int middleColumn = (numColumns - 1) / 2;
    cells[middleRow][middleColumn] = new Piece(middleRow, middleColumn, Color.WHITE);
    cells[middleRow + 1][middleColumn + 1] = new Piece(middleRow + 1, middleColumn + 1, Color.WHITE);
    cells[middleRow + 1][middleColumn] = new Piece(middleRow + 1, middleColumn, Color.BLACK);
    cells[middleRow][middleColumn + 1] = new Piece(middleRow, middleColumn + 1, Color.BLACK);
    blackCount = 2;
    whiteCount = 2;
    cellsToFlip = new HashSet<>();
    cellsChanged = new HashSet<>();
    cellsToHighlight = new HashSet<>();
    initPositionStats(middleRow, middleColumn);
  }

  public void initPositionStats(int middleRow, int middleColumn)
  {
    positionStats = new HashMap<>();
    // Both colors start with the same bounds based on the initial positions.
    PositionStats whiteStats = new PositionStats(middleRow, middleRow + 1, middleColumn, middleColumn + 1);
    PositionStats blackStats = new PositionStats(middleRow, middleRow + 1, middleColumn, middleColumn + 1);
    positionStats.put(Color.WHITE, whiteStats);
    positionStats.put(Color.BLACK, blackStats);
  }

  public void printBoard()
  {
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
        cells[i][j].printCell();
        System.out.print(",");
      }
      System.out.println();
    }

  }

  private boolean isOutOfBounds(int row, int column){
    return row < 0 || row >= numRows || column < 0 || column >= numColumns;
  }

  public boolean makeMove(final int row, final int column, final Color color, boolean simuMode)
  {
    cellsToFlip.clear();
    cellsChanged.clear();
    // Check if the move is out-of-bounds or the cell is already occupied.
    if (isOutOfBounds(row, column) || cells[row][column] instanceof Piece) {
      return false;
    }

    // Define all 8 possible directions.
    int[][] directions = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},          { 0, 1},
        { 1, -1}, { 1, 0}, { 1, 1}
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
      if (!(cells[neighborRow][neighborCol] instanceof Piece)) {
        continue;
      }
      Piece neighborPiece = (Piece) cells[neighborRow][neighborCol];
      if (neighborPiece.getColor() == color) {
        continue;
      }
      // Get flips in this direction.
      List<Piece> flips = getFlipsInDirection(row, column, deltaRow, deltaCol, color);
      cellsToFlip.addAll(flips);
    }

    if (cellsToFlip.isEmpty()) {
      return false;
    }

    if (simuMode) {
      return true;
    }
    for (Piece piece : cellsToFlip) {
      piece.flip();
      updatePositionStats(piece.getRow(), piece.getColumn(), piece.getColor());
      cellsChanged.add(piece);
    }
    updatePieceCount(color, cellsToFlip.size());
    updatePieceCount(getOppositeColor(color), -cellsToFlip.size());

    cells[row][column] = new Piece(row, column, color);
    updatePositionStats(row, column, color);
    cellsChanged.add((Piece) cells[row][column]);
    updatePieceCount(color, 1);

    return true;

  }

  /**
   *  Returns the list of opponent pieces that would be flipped in the given direction,
   * starting from (row, col). If the direction does not end with a piece of the same color,
   * an empty list is returned.
 */
  private List<Piece> getFlipsInDirection(int row, int col, int deltaRow, int deltaCol, Color color) {
    List<Piece> flips = new ArrayList<>();
    int r = row + deltaRow;
    int c = col + deltaCol;

    while (!isOutOfBounds(r, c)) {
      Cell cell = cells[r][c];
      if (cell instanceof EmptyCell) {
        // No enclosing piece; invalid direction.
        return Collections.emptyList();
      }
      if (cell instanceof Piece) {
        Piece piece = (Piece) cell;
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
    // Went off-board without closing with a piece of the same color.
    return Collections.emptyList();
  }


  private void highlightPossibleMoves(Color color) {
    PositionStats stats = positionStats.get(color);
    int rowMin = stats.getRowMin();
    int rowMax = stats.getRowMax();
    int colMin = stats.getColMin();
    int colMax = stats.getColMax();
    for (int row = rowMin - 1; row <= rowMax + 1; row++) {
      for (int col = colMin - 1; col <= colMax + 1; col++) {
        if (!isOutOfBounds(row, col) && makeMove(row, col, color, true)) {
          cellsToHighlight.add(cells[row][col]);
        }
      }
    }
  }

  public HashSet<Cell> getCellsToHighLight(Color color)
  {
    cellsToHighlight.clear();
    highlightPossibleMoves(color);
    return cellsToHighlight;
  }

  public int getPieceCount(Color color)
  {
    if (color == Color.BLACK) {
      return blackCount;
    } else {
      return whiteCount;
    }
  }

  private void updatePieceCount(Color color, int number)
  {

    if (color == Color.WHITE) {
      whiteCount += number;
    } else if (color == Color.BLACK) {
      blackCount += number;
    }
  }

  public Color getOppositeColor(Color color)
  {
    return color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
  }

  public HashSet<Piece> getCellsChanged()
  {
    return cellsChanged;
  }

  public void updatePositionStats(int row, int column, Color color)
  {
    PositionStats stats = positionStats.get(color);
    stats.update(row, column);
  }
}
