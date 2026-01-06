package com.project.reversi.services;

import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.Piece;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.Position;
import org.springframework.data.util.Pair;

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
  public Position execute(GameSession session, PlayerColor computerColor) {

    Board copyBoard = session.getBoard().copyBoard();
    GameSession copySession = new GameSession(copyBoard,new Player(computerColor.opposite()), GameType.PLAYER_VS_COMPUTER);
    var result = minMaxAlphaBeta(copySession,depth,Integer.MIN_VALUE,Integer.MAX_VALUE,computerColor,true);
    return result.getSecond();
  }


  public Pair<Integer,Position> minMaxAlphaBeta(GameSession game , int depth, int alpha, int beta, PlayerColor computerColor, boolean isComputer){

    Board board = game.getBoard();
    if(game.isGameOver() || depth == 0) {
      return Pair.of(evalFunction(game,computerColor),new Position(-1,-1));
    }
    if (isComputer){
      List<Position> validMoves = game.computeValidMoves(computerColor);
      int[] bestMove = new int[]{-1, -1};

      if(validMoves.isEmpty()){
        Pair<Integer, Position> passResult = minMaxAlphaBeta(game, depth, alpha, beta, computerColor, false);
        return Pair.of(passResult.getFirst(), new Position(-1, -1));
      }
      for(Position move : validMoves) {
        List<Piece> cellsCaptured = board.computeFlips(move.row(),move.col(),computerColor);
        if(!cellsCaptured.isEmpty()){
          board.applyMove(move.row(),move.col(),computerColor,cellsCaptured);
        }
        var score = minMaxAlphaBeta(game,depth - 1,alpha,beta,computerColor,false);
        board.undoMove(move.row(),move.col(),computerColor,cellsCaptured);
        if(score.getFirst() >= alpha){
          alpha = score.getFirst();
          bestMove[0] = move.row();
          bestMove[1] = move.col();
          if(alpha >= beta) {
            break;
          }
        }
      }
      return Pair.of(alpha,new Position(bestMove[0],bestMove[1]));
    }
    else {
      PlayerColor oppositeColor = computerColor.opposite();
      List<Position> validMoves = game.computeValidMoves(oppositeColor);

      if(validMoves.isEmpty()){
        Pair<Integer, Position> passResult = minMaxAlphaBeta(game, depth, alpha, beta, computerColor, true);
        return Pair.of(passResult.getFirst(), new Position(-1, -1));
      }
      for(Position move : validMoves){
        List<Piece> cellsCaptured = board.computeFlips(move.row(),move.col(),computerColor);
        if(!cellsCaptured.isEmpty()){
          board.applyMove(move.row(),move.col(),computerColor,cellsCaptured);
        }
        var score = minMaxAlphaBeta(game,depth - 1,alpha,beta,computerColor,true);
        board.undoMove(move.row(),move.col(),oppositeColor,cellsCaptured);
        if(score.getFirst() <= beta) {
          beta = score.getFirst();
          if(alpha >= beta){
            break;
          }
        }
      }
      return Pair.of(beta,new Position(-1,-1));
    }
  }
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

public int evalFunction(GameSession game, PlayerColor computerColor) {
  PlayerColor opponent = computerColor.opposite();
  Board board = game.getBoard();
  int score = 0;
  for (int row = 0; row < board.getNumRows(); row++) {
    for (int col = 0; col < board.getNumColumns(); col++) {
      Piece piece = board.getPiece(row, col);
      if (piece != null) {
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
