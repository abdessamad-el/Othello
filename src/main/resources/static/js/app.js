document.addEventListener("DOMContentLoaded", function() {
  const Reversi = window.Reversi = window.Reversi || {};
  Reversi.elements = Reversi.elements || {};
  Reversi.state = Reversi.state || {};
  Reversi.config = Reversi.config || {};

  Reversi.elements.menuPage = document.getElementById("menuPage");
  Reversi.elements.gamePage = document.getElementById("gamePage");
  Reversi.elements.newGameBtn = document.getElementById("newGameBtn");
  Reversi.elements.joinGameBtn = document.getElementById("joinGameBtn");
  Reversi.elements.findMatchBtn = document.getElementById("findMatchBtn");
  Reversi.elements.leaderBoardBtn = document.getElementById("leaderBoardBtn")
  Reversi.elements.overlay = document.getElementById("overlay");
  Reversi.elements.overlayTitle = document.getElementById("overlayTitle");
  Reversi.elements.overlayBody = document.getElementById("overlayBody");
  Reversi.elements.overlayCloseButton = document.getElementById("overlayCloseButton");
  Reversi.elements.scoresDiv = document.getElementById("scores");
  Reversi.elements.blackScoreVal = document.getElementById("blackScoreVal");
  Reversi.elements.whiteScoreVal = document.getElementById("whiteScoreVal");
  Reversi.elements.blackPlayerName = document.getElementById("blackPlayerName");
  Reversi.elements.whitePlayerName = document.getElementById("whitePlayerName");
  Reversi.elements.blackScoreBox = document.getElementById("blackScoreBox");
  Reversi.elements.whiteScoreBox = document.getElementById("whiteScoreBox");
  Reversi.elements.sessionDetails = document.getElementById("sessionDetails");
  Reversi.elements.sessionIdLabel = document.getElementById("sessionIdLabel");
  Reversi.elements.copySessionIdBtn = document.getElementById("copySessionIdBtn");
  Reversi.elements.boardDiv = document.getElementById("board");
  Reversi.elements.leaderboardPage = document.getElementById("leaderboardPage")
  Reversi.elements.gameContainer = document.getElementById("gameContainer");
  Reversi.elements.leaderboardMenuBtn = document.getElementById("leaderboardMenuBtn");
  Reversi.elements.leaderboardTableBody = document.getElementById("leaderboardBody");
  Reversi.elements.myStats = document.getElementById("myStats");
  Reversi.elements.myRank = document.getElementById("myRank");
  Reversi.elements.myWins = document.getElementById("myWins");
  Reversi.elements.myLosses = document.getElementById("myLosses");
  Reversi.elements.myWinRate = document.getElementById("myWinRate");
  Reversi.elements.leaderboardTableBody = document.getElementById("leaderboardBody");

  Reversi.config.WEBSOCKET_ENDPOINT = "/ws/game";

  Reversi.state.clientColor = Reversi.state.clientColor || "WHITE";
  Reversi.state.currentSessionSummary = Reversi.state.currentSessionSummary || null;
  Reversi.state.stompClient = Reversi.state.stompClient || null;
  Reversi.state.renderSequence = Reversi.state.renderSequence || 0;
  Reversi.state.matchmakingClient = Reversi.state.matchmakingClient || null;
  Reversi.state.matchmakingSubscription = Reversi.state.matchmakingSubscription || null;
  Reversi.state.matchmakingTicketId = Reversi.state.matchmakingTicketId || null;
  Reversi.state.copyFeedbackTimeout = Reversi.state.copyFeedbackTimeout || null;
  Reversi.state.replayingPendingAction = Reversi.state.replayingPendingAction || false;
  Reversi.state.lastAuthenticatedUsername = Reversi.state.lastAuthenticatedUsername || null;

  Reversi.elements.newGameBtn.addEventListener("click", window.Menu.showNewGameOptions);
  Reversi.elements.joinGameBtn.addEventListener("click", window.Menu.showJoinGameForm);
  Reversi.elements.findMatchBtn.addEventListener("click", () => window.Matchmaking.showMatchmakingForm());
  Reversi.elements.leaderBoardBtn.addEventListener("click",() => window.Leaderboard.showLeaderBoard())
  Reversi.elements.leaderboardMenuBtn.addEventListener("click", () => window.Leaderboard.showMenu())
  Reversi.elements.copySessionIdBtn.addEventListener("click", window.Game.copySessionId);
  Reversi.elements.overlayCloseButton.addEventListener("click",() => Reversi.elements.overlay.classList.add("hidden"))

  window.Auth.resumePendingActionIfAvailable();
});
