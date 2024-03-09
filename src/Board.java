import java.util.ArrayList;
import java.util.List;

public class Board
{

  private final int _nbRows;

  private final int _nbColumns;

  Cell[][] cells;


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
    cells[middleRow][middleColumn] = new Piece(middleRow, middleColumn, Color.White);
    cells[middleRow + 1][middleColumn + 1] = new Piece(middleRow + 1, middleColumn + 1, Color.White);
    cells[middleRow + 1][middleColumn] = new Piece(middleRow + 1, middleColumn, Color.Black);
    cells[middleRow][middleColumn + 1] = new Piece(middleRow, middleColumn + 1, Color.Black);
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


}
