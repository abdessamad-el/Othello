package com.project.reversi;

import com.project.reversi.model.Board;
import com.project.reversi.model.Piece;
import com.project.reversi.model.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardTest {
  private Board board;

  @BeforeEach
  public void setUp() {
    // Create a new board with 10 rows and 10 columns.
    board = new Board(10, 10);
  }

  @Test
  public void testInitialPieceCounts() {
    // Based on the standard initial Othello configuration,
    // there should be 2 white pieces and 2 black pieces.
    assertEquals(2, board.getPieceCount(PlayerColor.WHITE), "Initial white count should be 2");
    assertEquals(2, board.getPieceCount(PlayerColor.BLACK), "Initial black count should be 2");
  }

  @Test
  public void testInvalidMoveOutOfBounds() {
    // An out-of-bounds move should return false.
    List<Piece> result = board.makeMove(-1, -1, PlayerColor.WHITE, false);
    assertTrue(result.isEmpty(), "Out-of-bounds move should be invalid");
  }

  @Test
  public void testInvalidMoveOnOccupiedCell() {
    // Pick one of the center cells that is already occupied.
    int middleRow = (10 - 1) / 2;      // For a 10x10 board, this is 4.
    int middleColumn = (10 - 1) / 2;   // Also 4.
    // The cell at (4,4) is occupied by a white piece initially.
    List<Piece> result = board.makeMove(middleRow, middleColumn, PlayerColor.BLACK, false);
    assertTrue(result.isEmpty(), "Attempting to move on an occupied cell should be invalid");

  }

  @Test
  public void testValidMoveAndFlips() {
    // Assuming the standard Othello initial configuration:
    // White pieces at (4,4) and (5,5); Black pieces at (4,5) and (5,4).
    // A valid move for white is at (3,5):
    //   It is adjacent to the black piece at (4,5) and then in line with the white piece at (5,5).
    List<Piece> moveResult = board.makeMove(3, 5, PlayerColor.WHITE, false);
    assertFalse(moveResult.isEmpty(), "The move should be valid");

    // After this move:
    // - White should place a new piece at (3,5)
    // - The black piece at (4,5) should flip to white.
    // Thus, white's count should increase by 2 (the flipped piece plus the new one)
    // and black's count should decrease by 1.
    assertEquals(4, board.getPieceCount(PlayerColor.WHITE), "White count should be updated to 4");
    assertEquals(1, board.getPieceCount(PlayerColor.BLACK), "Black count should be updated to 1");
  }

  @Test
  public void testSimulatedMoveDoesNotChangeState() {
    // Simulated moves (simuMode = true) should return true if valid
    // but not change the board state.
    int initialWhite = board.getPieceCount(PlayerColor.WHITE);
    int initialBlack = board.getPieceCount(PlayerColor.BLACK);

    List<Piece> result = board.makeMove(3, 5, PlayerColor.WHITE, true);
    assertFalse(result.isEmpty(), "Simulated move should be valid");

    // Verify piece counts remain unchanged after simulation.
    assertEquals(initialWhite, board.getPieceCount(PlayerColor.WHITE),
                 "White count should remain unchanged in simulation mode");
    assertEquals(initialBlack, board.getPieceCount(PlayerColor.BLACK),
                 "Black count should remain unchanged in simulation mode");
  }

  @Test
  public void testUndoMoveRestoresState() {
    String initialSnapshot = board.toString();
    int initialWhite = board.getPieceCount(PlayerColor.WHITE);
    int initialBlack = board.getPieceCount(PlayerColor.BLACK);

    int moveRow = 4;
    int moveCol = 3;

    List<Piece> moveResult = board.makeMove(moveRow, moveCol, PlayerColor.BLACK, false);
    assertFalse(moveResult.isEmpty(), "Expected a legal move for black at (4,3)");

    board.undoMove(moveRow, moveCol, PlayerColor.BLACK, moveResult);

    assertEquals(initialWhite, board.getPieceCount(PlayerColor.WHITE), "White count should revert after undo");
    assertEquals(initialBlack, board.getPieceCount(PlayerColor.BLACK), "Black count should revert after undo");
    assertEquals(initialSnapshot, board.toString(), "Board state should match the snapshot before the move");

    Piece revertedCell = board.getPiece(moveRow, moveCol);
    assertNull(revertedCell, "The played square should be empty again after undo");

    Piece originalPiece = board.getPiece(4, 4);
    assertNotNull(originalPiece, "The previously flipped disc should be restored");
    assertEquals(PlayerColor.WHITE, originalPiece.getColor(), "The restored disc should regain its original colour");
  }
}
