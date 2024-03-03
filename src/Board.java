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
        cells[i][j] = new Cell(i, j);
      }
    }
    int middleRow = _nbRows / 2;
    int middleColumn = _nbColumns / 2;
    cells[middleRow][middleColumn] = new Piece(middleRow, middleColumn, Color.White);
    cells[middleRow][middleColumn + 1] = new Piece(middleRow + 1, middleColumn + 1, Color.Black);
    cells[middleRow + 1][middleColumn] = new Piece(middleRow + 1, middleColumn, Color.Black);
    cells[middleRow + 1][middleColumn + 1] = new Piece(middleRow + 1, middleColumn + 1, Color.White);
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


}
