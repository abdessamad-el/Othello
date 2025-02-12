public class PositionStats {
  private int rowMin;
  private int rowMax;
  private int colMin;
  private int colMax;

  public PositionStats(int rowMin, int rowMax, int colMin, int colMax)
  {
    this.rowMin = rowMin;
    this.rowMax = rowMax;
    this.colMin = colMin;
    this.colMax = colMax;
  }

  public int getRowMin()
  {
    return rowMin;
  }

  public int getRowMax()
  {
    return rowMax;
  }

  public int getColMin()
  {
    return colMin;
  }

  public int getColMax()
  {
    return colMax;
  }

  public void update(int row, int col)
  {
    rowMin = Math.min(rowMin, row);
    rowMax = Math.max(rowMax, row);
    colMin = Math.min(colMin, col);
    colMax = Math.max(colMax, col);
  }
}
