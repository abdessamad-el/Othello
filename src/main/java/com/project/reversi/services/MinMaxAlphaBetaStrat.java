package com.project.reversi.services;

import com.project.reversi.model.Board;
import com.project.reversi.model.Cell;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.Piece;
import com.project.reversi.model.PlayerColor;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class MinMaxAlphaBetaStrat implements ComputerStrategy {

  private int depth;

  public MinMaxAlphaBetaStrat(int depth){
    this.depth = depth;
  }

  protected int getDepth() {
    return depth;
  }

  @Override
  public int[] execute(GameSession session, PlayerColor computerColor) {

    Board copyBoard = session.getBoard().copyBoard();
    var result = minMaxAlphaBeta(copyBoard,depth,Integer.MIN_VALUE,Integer.MAX_VALUE,computerColor,true);
    return result.getSecond();
  }


  public Pair<Integer,int[]> minMaxAlphaBeta(Board board , int depth, int alpha, int beta, PlayerColor computerColor, boolean isComputer){
    if(board.isGameOver() || depth == 0) {
      return Pair.of(evalFunction(board,computerColor),new int[]{-1,-1});
    }
    if (isComputer){
      List<int[]> validMoves = board.computeValidMoves(computerColor);
      int[] bestMove = new int[]{-1, -1};

      if(validMoves.isEmpty()){
        Pair<Integer, int[]> passResult = minMaxAlphaBeta(board, depth, alpha, beta, computerColor, false);
        return Pair.of(passResult.getFirst(), new int[]{-1, -1});
      }
      for(int[] move : validMoves) {
        board.makeMove(move[0],move[1],computerColor,false);
        List<Piece> cellsCaptured = new ArrayList<>(board.getCellsToFlip());
        var score = minMaxAlphaBeta(board,depth - 1,alpha,beta,computerColor,false);
        board.undoMove(move[0],move[1],computerColor,cellsCaptured);
        if(score.getFirst() >= alpha){
          alpha = score.getFirst();
          bestMove[0] = move[0];
          bestMove[1] = move[1];
          if(alpha >= beta) {
            break;
          }
        }
      }
      return Pair.of(alpha,bestMove);
    }
    else {
      PlayerColor oppositeColor = board.getOppositeColor(computerColor);
      List<int[]> validMoves = board.computeValidMoves(oppositeColor);

      if(validMoves.isEmpty()){
        Pair<Integer, int[]> passResult = minMaxAlphaBeta(board, depth, alpha, beta, computerColor, true);
        return Pair.of(passResult.getFirst(), new int[]{-1, -1});
      }
      for(int[] move : validMoves){
        board.makeMove(move[0],move[1],oppositeColor,false);
        List<Piece> cellsCaptured = new ArrayList<>(board.getCellsToFlip());
        var score = minMaxAlphaBeta(board,depth - 1,alpha,beta,computerColor,true);
        board.undoMove(move[0],move[1],oppositeColor,cellsCaptured);
        if(score.getFirst() <= beta) {
          beta = score.getFirst();
          if(alpha >= beta){
            break;
          }
        }
      }
      return Pair.of(beta,new int[]{-1,-1});
    }
  }
  
  /* 
  public int evalFunction(Board board ,PlayerColor computerColor){
    return board.getPieceCount(computerColor) - board.getPieceCount(board.getOppositeColor(computerColor));
  }
  */

  // 8x8 Reversi positional weights (corners high, X/C squares negative)
private static final int[][] POSITION_WEIGHTS = {
    {100, -20, 10, 5, 5, 10, -20, 100},
    {-20, -50, -2, -2, -2, -2, -50, -20},
    {10,  -2,  5,  1,  1,  5,  -2,  10},
    {5,   -2,  1,  0,  0,  1,  -2,   5},
    {5,   -2,  1,  0,  0,  1,  -2,   5},
    {10,  -2,  5,  1,  1,  5,  -2,  10},
    {-20, -50, -2, -2, -2, -2, -50, -20},
    {100, -20, 10, 5, 5, 10, -20, 100}
};

public int evalFunction(Board board, PlayerColor computerColor) {
  PlayerColor opponent = board.getOppositeColor(computerColor);
  int score = 0;
  for (int row = 0; row < board.getNumRows(); row++) {
    for (int col = 0; col < board.getNumColumns(); col++) {
      Cell cell = board.getCell(row, col);
      if (cell instanceof Piece piece) {
        if (piece.getColor().equals(computerColor)) {
          score += POSITION_WEIGHTS[row][col];
        } else if (piece.getColor().equals(opponent)) {
          score -= POSITION_WEIGHTS[row][col];
        }
      }
    }
  }
  return score;
}
}
