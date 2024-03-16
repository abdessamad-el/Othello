import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Board
{

  private final int _nbRows;

  private final int _nbColumns;

  private Cell[][] cells;

  private HashSet<Piece> cellsToFlip;


  public Board(int nbRows, int nbColumns)
  {
    _nbRows = nbRows;
    _nbColumns = nbColumns;
    initBoard();

  }

  private void initBoard()
  {
    cells = new Cell[_nbRows][_nbColumns];
    for (int i = 0; i < _nbRows; i++) {
      for (int j = 0; j < _nbColumns; j++) {
        cells[i][j] = new EmptyCell(i, j);
      }
    }
    int middleRow = (_nbRows - 1) / 2;
    int middleColumn = (_nbColumns - 1) / 2;
    cells[middleRow][middleColumn] = new Piece(middleRow, middleColumn, Color.WHITE);
    cells[middleRow + 1][middleColumn + 1] = new Piece(middleRow + 1, middleColumn + 1, Color.WHITE);
    cells[middleRow + 1][middleColumn] = new Piece(middleRow + 1, middleColumn, Color.BLACK);
    cells[middleRow][middleColumn + 1] = new Piece(middleRow, middleColumn + 1, Color.BLACK);
    cellsToFlip = new HashSet<>();
  }

  public void printBoard()
  {
    for (int i = 0; i < _nbRows; i++) {
      for (int j = 0; j < _nbColumns; j++) {
        cells[i][j].printCell();
        System.out.print(",");
      }
      System.out.println("");
    }

  }

  public List<Cell> getNeighbors(Cell cell)
  {
    int row = cell.getRow();
    int column = cell.getColumn();
    List<Cell> neighbors = new ArrayList<>();

    int[][] deltas = {
        {1, 0}, {1, 1}, {1, -1},
        {0, 1}, {0, -1}, {-1, 0},
        {-1, 1}, {-1, -1}
    };

    for (int[] delta : deltas) {
      int newRow = row + delta[0];
      int newColumn = column + delta[1];

      if (!isOutOfBounds(newRow, newColumn)) {
        neighbors.add(cells[newRow][newColumn]);

      }
    }

    return neighbors;

  }

  private boolean isOutOfBounds(int row, int column)
  {
    return row < 0 || row > _nbRows || column < 0 || column > _nbColumns;
  }

  public boolean makeMove(int row, int column, Color color)
  {
    cellsToFlip.clear();
    if (isOutOfBounds(row, column)) {
      return false;
    }
    if (cells[row][column] instanceof Piece) {
      return false;
    }

    List<Cell> neighbors = getNeighbors(cells[row][column]);
    List<Piece> neighborsOfdifferntColor = new ArrayList<>();
    for (Cell neighbour : neighbors) {
      if (neighbour instanceof Piece && ((Piece) neighbour)._color != color) {
        neighborsOfdifferntColor.add((Piece) neighbour);
      }
    }
    if (neighborsOfdifferntColor.isEmpty()) {
      return false;
    }


    for (Piece neighbor : neighborsOfdifferntColor) {

      int[] delta = {neighbor.getRow() - row, neighbor.getColumn() - column};
      int newRow = row + delta[0];
      int newColumn = column + delta[1];
      while (!isOutOfBounds(newRow, newColumn)
             && cells[newRow][newColumn] instanceof Piece
             && ((Piece) cells[newRow][newColumn])._color != color) {
        cellsToFlip.add((Piece) cells[newRow][newColumn]);
        newRow += delta[0];
        newColumn += delta[1];

      }
      if (isOutOfBounds(newRow, newColumn) || cells[newRow][newColumn] instanceof EmptyCell) {
        //rollback flipped pieces
        newRow -= delta[0];
        newColumn -= delta[1];
        while (cells[newRow][newColumn] != cells[row][column]) {
          cellsToFlip.remove((Piece) cells[newRow][newColumn]);
          newRow -= delta[0];
          newColumn -= delta[1];
        }
      }

    }
    if (cellsToFlip.isEmpty()) {
      return false;
    }

    for (Piece piece : cellsToFlip) {
      piece.flip();
    }
    cells[row][column] = new Piece(row, column, color);


    return true;


  }


}
