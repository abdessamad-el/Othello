(function(root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  if (root) {
    root.SpectatorMode = api;
  }
})(typeof window !== "undefined" ? window : null, function() {
  function spectateSessionIdFromSearch(search) {
    const params = new URLSearchParams(search || "");
    const value = params.get("spectate");
    if (value === null) {
      return null;
    }
    const sessionId = value.trim();
    return sessionId.length > 0 ? sessionId : null;
  }

  function isActiveColor(state) {
    return !!(state.clientColor && state.currentPlayerColor &&
      state.clientColor.toUpperCase() === state.currentPlayerColor.toUpperCase());
  }

  function shouldRequestPossibleMoves(state) {
    return !state.isSpectator && isActiveColor(state);
  }

  function canInteractWithBoard(state) {
    return !state.isSpectator && isActiveColor(state);
  }

  function displayColor(color) {
    if (!color) {
      return "Unknown";
    }
    const normalized = color.toLowerCase();
    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
  }

  function gameStatusText(summary, isSpectator) {
    if (!summary) {
      return "";
    }
    if (summary.gameState && summary.gameState !== "IN_PROGRESS") {
      const result = summary.gameState === "TIE"
        ? "Tie"
        : displayColor(summary.gameState.replace("_WINS", "")) + " wins";
      return `Game over: ${result}.`;
    }
    const activeTurn = `${displayColor(summary.currentPlayerColor)} to move.`;
    if (summary.lastPassedPlayerColor) {
      return `${displayColor(summary.lastPassedPlayerColor)} passed. ${activeTurn}`;
    }
    return isSpectator ? `Watching live — ${activeTurn}` : activeTurn;
  }

  function spectatorUrl(baseUrl, sessionId) {
    const url = new URL(baseUrl);
    url.searchParams.set("spectate", sessionId);
    return url.toString();
  }

  return {
    spectateSessionIdFromSearch,
    shouldRequestPossibleMoves,
    canInteractWithBoard,
    gameStatusText,
    spectatorUrl
  };
});
