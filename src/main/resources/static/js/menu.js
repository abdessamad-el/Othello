(() => {
  const Reversi = window.Reversi = window.Reversi || {};
  Reversi.state = Reversi.state || {};

  function showNewGameOptions() {
    const { overlay, overlayTitle, overlayBody } = Reversi.elements;
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Start a New Game";
    overlayBody.innerHTML = `
      <button id="pvpBtn">Player vs Player</button>
      <button id="pvcBtn">Player vs Computer</button>
    `;
    document.getElementById("pvpBtn").addEventListener("click", function() {
      window.Game.startGame("PLAYER_VS_PLAYER");
    });
    document.getElementById("pvcBtn").addEventListener("click", function() {
      window.Game.startGame("PLAYER_VS_COMPUTER");
    });
  }

  function showJoinGameForm() {
    const { overlay, overlayTitle, overlayBody } = Reversi.elements;
    console.log("showJoinGameForm called");
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Join a Game";
    overlayBody.innerHTML = `
      <input type="text" id="sessionIdInput" placeholder="Enter Session ID" />
      <button id="joinBtn">Join</button>
    `;
    document.getElementById("joinBtn").addEventListener("click", function() {
      window.Game.joinGame();
    });
  }

  window.Menu = {
    showNewGameOptions,
    showJoinGameForm
  };
})();
