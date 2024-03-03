
public class Piece extends Cell
{
  Color _color;


  public Piece(int row, int column, Color color)
  {
    super(row, column);
    _color = color;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Piece)) {
      return false;
    }
    return _row == ((Piece) obj)._row && _column == ((Piece) obj)._column && _color == ((Piece) obj)._color;
  }

  @Override
  public void printCell()
  {
    if (_color == Color.White) {
      System.out.print("W");
    } else {
      System.out.print("B");
    }
  }
}
