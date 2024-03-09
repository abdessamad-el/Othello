import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;

public class Othello
{


  public static void main(String[] args)
  {
    Board b = new Board(10, 10);
    b.printBoard();
    new Othello().buildGUI();
  }


  public void buildGUI()
  {
    JFrame frame = new JFrame("Othello");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    BorderLayout layout = new BorderLayout();
    JPanel background = new JPanel(layout);
    background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    frame.getContentPane().add(background);
    Grid mainPanel = new Grid(10, 10, 50);
    background.add(BorderLayout.CENTER, mainPanel);
    frame.setBounds(50, 50, 500, 500);
    frame.pack();
    frame.setVisible(true);


  }

  public class MyMouseListener extends MouseAdapter
  {
    private Grid grid;

    public MyMouseListener(Grid grid)
    {
      this.grid = grid;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
      if (e.getButton() == MouseEvent.BUTTON1) {
        grid.labelPressed((GridLabel) e.getSource());
      }
    }
  }


  public class Grid extends JPanel
  {
    private JLabel[][] cells;
    private int _nbRows;
    private int _nbColumns;

    private int _cellWidth;
    MyMouseListener myListener;


    public void initialize()
    {
      Dimension labelPrefSize = new Dimension(_cellWidth, _cellWidth);
      int middleRow = (_nbRows-1) / 2;
      int middleColumn = (_nbColumns-1) / 2;
      for (int row = 0; row < _nbRows; row++) {
        for (int column = 0; column < _nbColumns; column++) {
          JLabel myLabel;
          if(row == middleRow && column == middleColumn){
            myLabel = new PieceGui(middleRow, middleColumn, Color.WHITE);
          }
          else if(row == middleRow && column == middleColumn + 1){
            myLabel = new PieceGui(middleRow, middleColumn + 1, Color.BLACK);
          }

          else if(row == middleRow + 1 && column == middleColumn){
            myLabel = new PieceGui(middleRow + 1, middleColumn, Color.BLACK);
          }
          else if(row == middleRow + 1 && column == middleColumn + 1){
            myLabel = new PieceGui(middleRow + 1, middleColumn + 1, Color.WHITE);
          }
          else {
            myLabel = new GridLabel(row,column);
          }
          myLabel.setOpaque(true);
          myLabel.setBackground(Color.LIGHT_GRAY);
          Border blackLine = BorderFactory.createLineBorder(Color.black);
          myLabel.setBorder(blackLine);
          myLabel.addMouseListener(myListener);
          myLabel.setPreferredSize(labelPrefSize);
          add(myLabel);
          cells[row][column] = myLabel;
        }

        cells[middleRow][middleColumn] = new PieceGui(middleRow, middleColumn, Color.WHITE);
        cells[middleRow][middleColumn + 1] = new PieceGui(middleRow + 1, middleColumn + 1, Color.BLACK);
        cells[middleRow + 1][middleColumn] = new PieceGui(middleRow + 1, middleColumn, Color.BLACK);
        cells[middleRow + 1][middleColumn + 1] = new PieceGui(middleRow + 1, middleColumn + 1, Color.WHITE);

      }
    }

    public Grid(int rows, int cols, int cellWidth)
    {
      _nbRows = rows;
      _nbColumns = cols;
      _cellWidth = cellWidth;
      cells = new JLabel[rows][cols];
      myListener = new MyMouseListener(this);
      setLayout(new GridLayout(rows, cols));
      initialize();
    }


    public void labelPressed(GridLabel label)
    {
    }

  }

  class PieceGui extends GridLabel
  {
    private Color _color;

    public PieceGui(int row , int column, Color color){
      super(row ,column);
      _color = color;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      int nGap = 5;
      int nXPosition = nGap;
      int nYPosition = nGap;
      int nWidth = getWidth() - nGap * 2;
      int nHeight = getHeight() - nGap * 2;

      g.setColor(_color);
      g.drawOval(nXPosition, nYPosition, nWidth, nHeight);
      g.fillOval(nXPosition, nYPosition, nWidth, nHeight);
    }

  }

  class GridLabel extends JLabel {
    protected int _row;
    protected int _column;

    public GridLabel(int row , int column){
      super();
      _row = row;
      _column = column;
    }

    public int getRow(){
      return _row;
    }

    public int getColumn(){
      return _column;
    }

  }

}
