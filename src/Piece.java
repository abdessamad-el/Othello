import java.awt.*;

public class Piece extends Cell
{
  Color _color;


  public Piece(int row, int column, Color Color)
  {
    super(row, column);
    _color = Color;
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
    if (_color == Color.WHITE) {
      System.out.print("W");
    } else {
      System.out.print("B");
    }
  }
  
  public void flip(){
  if(_color == Color.WHITE){
    _color = Color.BLACK;
  }
  else {
    _color = Color.WHITE;
  }
  }
}
