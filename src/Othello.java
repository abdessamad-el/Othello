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


  private final Board board;

  private Player[] players;

  private JLabel _blackScore;

  private JLabel _whiteScore;


  private static final int nbRows = 10;

  private static final int nbColumns = 10;


  public static void main(String[] args)
  {
    new Othello().buildGUI();
  }

  public Othello()
  {
    board = new Board(nbRows, nbColumns);
    players = new Player[2];
    players[0] = new Player(Color.BLACK,this);
    players[1] = new Player(Color.WHITE,this);
  }


  public void buildGUI()
  {
    JFrame frame = new JFrame("Othello");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    BorderLayout layout = new BorderLayout();
    JPanel background = new JPanel(layout);
    background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    frame.getContentPane().add(background);
    JPanel scorePanel = createScorePanel();
    background.add(BorderLayout.NORTH, scorePanel);
    Grid mainPanel = new Grid(50);
    background.add(BorderLayout.CENTER, mainPanel);
    frame.setBounds(50, 50, 500, 500);
    frame.pack();
    frame.setVisible(true);
}

  private JPanel createScorePanel(Color color)
  {

    JPanel scorePanel = new JPanel();
    Dimension labelPrefSize = new Dimension(50, 50);
    JLabel piece = new PieceGui(color);
    piece.setOpaque(true);
    piece.setPreferredSize(labelPrefSize);

    if (color == Color.BLACK) {
      JLabel blackScore = createScoreLabel(labelPrefSize);
      _blackScore = blackScore;
      scorePanel.add(piece);
      scorePanel.add(blackScore);
    } else {
      JLabel whiteScore = createScoreLabel(labelPrefSize);
      _whiteScore = whiteScore;
      scorePanel.add(whiteScore);
      scorePanel.add(piece);
    }
    return scorePanel;

  }

  private JLabel createScoreLabel(Dimension labelPrefSize)
  {
    JLabel score = new JLabel("2", SwingConstants.CENTER);
    score.setOpaque(true);
    score.setPreferredSize(labelPrefSize);
    score.setFont(new Font("Calibri", Font.BOLD, 20));
    return score;
  }

  private JPanel createScorePanel()
  {
    JPanel scorePanel = new JPanel(new BorderLayout());
    scorePanel.add(BorderLayout.EAST, createScorePanel(Color.WHITE));
    scorePanel.add(BorderLayout.WEST, createScorePanel(Color.BLACK));
    scorePanel.add(Box.createVerticalStrut(70));
    scorePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY,5));
    return scorePanel;
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
      boolean isValid = board.makeMove(rowLabel, columnLabel, color,false);
      if (!isValid) {
        return;
      }
      board.printBoard();
      HashSet<Piece> changedCells = board.getCellsChanged();
      _grid.clearHighLightedCells();
      _grid.updateChangedCells(changedCells);
      _blackScore.setText(String.valueOf(board.getPieceCount(Color.BLACK)));
      _whiteScore.setText(String.valueOf(board.getPieceCount(Color.WHITE)));
      changePlayer();
      HashSet<Cell> cellsToHighLight = board.getCellsToHighLight(color);
      _grid.highLightCells(cellsToHighLight);
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
    private PieceGui[][] cells;
    private int _cellWidth;
    GridListener myListener;

    private HashSet<PieceGui> highLightedCells;


    public void initialize()
    {
      Dimension labelPrefSize = new Dimension(_cellWidth, _cellWidth);
      int middleRow = (nbRows - 1) / 2;
      int middleColumn = (nbColumns - 1) / 2;
      for (int row = 0; row < nbRows; row++) {
        for (int column = 0; column < nbColumns; column++) {
          PieceGui myLabel;
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
      cells = new PieceGui[nbRows][nbColumns];
      highLightedCells = new HashSet<>();
      myListener = new GridListener(this);
      setLayout(new GridLayout(nbColumns, nbColumns));
      initialize();
    }

    public void updateChangedCells(HashSet<Piece> changedCells)
    {
      for (Piece piece : changedCells) {
        int row = piece.getRow();
        int column = piece.getColumn();
        Color color = piece.getColor();
        cells[row][column].setColor(color);

      }
    }

    public void highLightCells(HashSet<Cell> cellsToHighLight)
    {
      for (Cell cell : cellsToHighLight) {
        int row = cell.getRow();
        int column = cell.getColumn();
        highLightedCells.add(cells[row][column]);
        cells[row][column].toHighlight(true);
      }
    }

    public void clearHighLightedCells()
    {
      for (PieceGui cell : highLightedCells) {
        cell.toHighlight(false);
      }
      highLightedCells.clear();
    }

  }


  class PieceGui extends JLabel
  {
    private Color _color;
    private int _row;
    private int _column;

    private boolean toHighlight;

    public PieceGui(int row, int column, Color color)
    {
      super();
      _row = row;
      _column = column;
      _color = color;
    }

    public PieceGui(Color color)
    {
      super();
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
      if (toHighlight) {
        g.drawOval(nXPosition, nYPosition, nWidth, nHeight);
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

    public void toHighlight(boolean isToHighLight)
    {
      toHighlight = isToHighLight;
      repaint();
    }

  }

  public Board getBoard()
  {
    return board;
  }

}

