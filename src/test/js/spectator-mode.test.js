const test = require("node:test");
const assert = require("node:assert/strict");

const {
  spectateSessionIdFromSearch,
  shouldRequestPossibleMoves,
  canInteractWithBoard,
  gameStatusText,
  spectatorUrl
} = require("../../main/resources/static/js/spectator-mode.js");

test("direct spectator links return their trimmed session ID", () => {
  assert.equal(spectateSessionIdFromSearch("?spectate=%20game-123%20"), "game-123");
});

test("missing or blank spectator parameters do not start spectator mode", () => {
  assert.equal(spectateSessionIdFromSearch("?other=value"), null);
  assert.equal(spectateSessionIdFromSearch("?spectate=%20%20"), null);
});

test("spectators never request legal-move hints", () => {
  assert.equal(shouldRequestPossibleMoves({
    isSpectator: true,
    clientColor: "WHITE",
    currentPlayerColor: "WHITE"
  }), false);
});

test("spectators can never interact with board cells", () => {
  assert.equal(canInteractWithBoard({
    isSpectator: true,
    clientColor: "WHITE",
    currentPlayerColor: "WHITE"
  }), false);
});

test("spectator status identifies the active turn", () => {
  assert.equal(gameStatusText({
    gameState: "IN_PROGRESS",
    currentPlayerColor: "WHITE"
  }, true), "Watching live — White to move.");
});

test("spectator status identifies a passed turn", () => {
  assert.equal(gameStatusText({
    gameState: "IN_PROGRESS",
    currentPlayerColor: "WHITE",
    lastPassedPlayerColor: "BLACK"
  }, true), "Black passed. White to move.");
});

test("spectator status renders the completed-game result", () => {
  assert.equal(gameStatusText({ gameState: "BLACK_WINS" }, true), "Game over: Black wins.");
  assert.equal(gameStatusText({ gameState: "TIE" }, true), "Game over: Tie.");
});

test("shareable watch links encode the spectator session ID", () => {
  assert.equal(
    spectatorUrl("https://example.test/?other=value#board", "game / 123"),
    "https://example.test/?other=value&spectate=game+%2F+123#board"
  );
});
