package com.project.reversi;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.project.reversi.model.Board;
import com.project.reversi.model.Cell;
import com.project.reversi.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    assertEquals(2, board.getPieceCount(Color.WHITE), "Initial white count should be 2");
    assertEquals(2, board.getPieceCount(Color.BLACK), "Initial black count should be 2");
  }

  @Test
  public void testInvalidMoveOutOfBounds() {
    // An out-of-bounds move should return false.
    boolean result = board.makeMove(-1, -1, Color.WHITE, false);
    assertFalse(result, "Out-of-bounds move should be invalid");
  }

  @Test
  public void testInvalidMoveOnOccupiedCell() {
    // Pick one of the center cells that is already occupied.
    int middleRow = (10 - 1) / 2;      // For a 10x10 board, this is 4.
    int middleColumn = (10 - 1) / 2;   // Also 4.
    // The cell at (4,4) is occupied by a white piece initially.
    boolean result = board.makeMove(middleRow, middleColumn, Color.BLACK, false);
    assertFalse(result, "Attempting to move on an occupied cell should be invalid");
  }

  @Test
  public void testValidMoveAndFlips() {
    // Assuming the standard Othello initial configuration:
    // White pieces at (4,4) and (5,5); Black pieces at (4,5) and (5,4).
    // A valid move for white is at (3,5):
    //   It is adjacent to the black piece at (4,5) and then in line with the white piece at (5,5).
    boolean moveResult = board.makeMove(3, 5, Color.WHITE, false);
    assertTrue(moveResult, "The move should be valid");

    // After this move:
    // - White should place a new piece at (3,5)
    // - The black piece at (4,5) should flip to white.
    // Thus, white's count should increase by 2 (the flipped piece plus the new one)
    // and black's count should decrease by 1.
    assertEquals(4, board.getPieceCount(Color.WHITE), "White count should be updated to 4");
    assertEquals(1, board.getPieceCount(Color.BLACK), "Black count should be updated to 1");
  }

  @Test
  public void testSimulatedMoveDoesNotChangeState() {
    // Simulated moves (simuMode = true) should return true if valid
    // but not change the board state.
    int initialWhite = board.getPieceCount(Color.WHITE);
    int initialBlack = board.getPieceCount(Color.BLACK);

    boolean result = board.makeMove(3, 5, Color.WHITE, true);
    assertTrue(result, "Simulated move should be valid");

    // Verify piece counts remain unchanged after simulation.
    assertEquals(initialWhite, board.getPieceCount(Color.WHITE),
                 "White count should remain unchanged in simulation mode");
    assertEquals(initialBlack, board.getPieceCount(Color.BLACK),
                 "Black count should remain unchanged in simulation mode");
  }

  @Test
  public void testUndoMoveRestoresState() {
    String initialSnapshot = board.toString();
    int initialWhite = board.getPieceCount(Color.WHITE);
    int initialBlack = board.getPieceCount(Color.BLACK);

    int moveRow = 4;
    int moveCol = 3;

    boolean moveResult = board.makeMove(moveRow, moveCol, Color.BLACK, false);
    assertTrue(moveResult, "Expected a legal move for black at (4,3)");

    List<Piece> captured = new ArrayList<>(board.getCellsToFlip());
    board.undoMove(moveRow, moveCol, Color.BLACK, captured);

    assertEquals(initialWhite, board.getPieceCount(Color.WHITE), "White count should revert after undo");
    assertEquals(initialBlack, board.getPieceCount(Color.BLACK), "Black count should revert after undo");
    assertEquals(initialSnapshot, board.toString(), "Board state should match the snapshot before the move");

    Cell revertedCell = board.getCell(moveRow, moveCol);
    assertFalse(revertedCell instanceof Piece, "The played square should be empty again after undo");

    Cell originalPiece = board.getCell(4, 4);
    assertTrue(originalPiece instanceof Piece, "The previously flipped disc should be restored");
    assertEquals(Color.WHITE, ((Piece) originalPiece).getColor(), "The restored disc should regain its original colour");
  }
}
