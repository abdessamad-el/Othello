import java.util.List;

public abstract class Cell
{

  protected int _row;
  protected int _column;

  public Cell(int row, int column)
  {
    _row = row;
    _column = column;
  }


  public abstract void printCell();

  public int getRow(){
    return _row;
  }
  public int getColumn(){
    return _column;
  }

}
