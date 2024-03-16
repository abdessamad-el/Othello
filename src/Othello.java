import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.util.HashSet;

public class Othello
{

  private Board board;

  private static final int nbRows = 10;

  private static final int nbColumns = 10;


  public static void main(String[] args)
  {
    new Othello().buildGUI();
  }

  public Othello()
  {
    board = new Board(nbRows, nbColumns);
  }


  public void buildGUI()
  {
    JFrame frame = new JFrame("Othello");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    BorderLayout layout = new BorderLayout();
    JPanel background = new JPanel(layout);
    background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    frame.getContentPane().add(background);
    Grid mainPanel = new Grid(50);
    background.add(BorderLayout.CENTER, mainPanel);
    frame.setBounds(50, 50, 500, 500);
    frame.pack();
    frame.setVisible(true);


  }

  public class GridListener extends MouseAdapter
  {
    // White begin first
    private Color color = Color.WHITE;

    private Grid _grid;


    public GridListener(Grid grid)
    {
      _grid = grid;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
      PieceGui label = (PieceGui) e.getSource();
      int rowLabel = label.getRow();
      int columnLabel = label.getColumn();
      boolean isValid = board.makeMove(rowLabel, columnLabel, color);
      if (!isValid) {
        return;
      }
      board.printBoard();
      HashSet<Piece> changedCells = board.getCellsChanged();
      _grid.updateGui(changedCells);
      changePlayer();


    }

    private void changePlayer()
    {
      if (color == Color.WHITE) {
        color = Color.BLACK;
      } else {
        color = Color.WHITE;
      }
    }
  }


  public class Grid extends JPanel
  {
    private JLabel[][] cells;
    private int _cellWidth;
    GridListener myListener;


    public void initialize()
    {
      Dimension labelPrefSize = new Dimension(_cellWidth, _cellWidth);
      int middleRow = (nbRows - 1) / 2;
      int middleColumn = (nbColumns - 1) / 2;
      for (int row = 0; row < nbRows; row++) {
        for (int column = 0; column < nbColumns; column++) {
          JLabel myLabel;
          if (row == middleRow && column == middleColumn) {
            myLabel = new PieceGui(middleRow, middleColumn, Color.WHITE);
          } else if (row == middleRow && column == middleColumn + 1) {
            myLabel = new PieceGui(middleRow, middleColumn + 1, Color.BLACK);
          } else if (row == middleRow + 1 && column == middleColumn) {
            myLabel = new PieceGui(middleRow + 1, middleColumn, Color.BLACK);
          } else if (row == middleRow + 1 && column == middleColumn + 1) {
            myLabel = new PieceGui(middleRow + 1, middleColumn + 1, Color.WHITE);
          } else {
            myLabel = new PieceGui(row, column, null);
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

      }
    }

    public Grid(int cellWidth)
    {
      _cellWidth = cellWidth;
      cells = new JLabel[nbRows][nbColumns];
      myListener = new GridListener(this);
      setLayout(new GridLayout(nbColumns, nbColumns));
      initialize();
    }

    public void updateGui(HashSet<Piece> changedCells)
    {
      for (Piece piece : changedCells) {
        int row = piece.getRow();
        int column = piece.getColumn();
        Color color = piece.getColor();
        ((PieceGui) cells[row][column]).setColor(color);


      }
    }
  }

  class PieceGui extends JLabel
  {
    private Color _color;
    private int _row;
    private int _column;

    public PieceGui(int row, int column, Color color)
    {
      super();
      _row = row;
      _column = column;
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
      if (_color != null) {
        g.setColor(_color);
        g.drawOval(nXPosition, nYPosition, nWidth, nHeight);
        g.fillOval(nXPosition, nYPosition, nWidth, nHeight);
      }
    }

    public void setColor(Color color)
    {
      _color = color;
      repaint();
    }

    public int getRow()
    {
      return _row;
    }

    public int getColumn()
    {
      return _column;
    }

  }

}

