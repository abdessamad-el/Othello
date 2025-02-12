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

  public Color getColor()
  {
    return _color;
  }
}
