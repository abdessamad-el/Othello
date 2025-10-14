document.addEventListener("DOMContentLoaded", function() {
  // Element references
  const menuPage = document.getElementById("menuPage");
  const gamePage = document.getElementById("gamePage");
  const newGameBtn = document.getElementById("newGameBtn");
  const joinGameBtn = document.getElementById("joinGameBtn");
  const overlay = document.getElementById("overlay");
  const overlayTitle = document.getElementById("overlayTitle");
  const overlayBody = document.getElementById("overlayBody");
  const scoresDiv = document.getElementById("scores");
  const blackScoreVal = document.getElementById("blackScoreVal");
  const whiteScoreVal = document.getElementById("whiteScoreVal");
  const boardDiv = document.getElementById("board");
  const gameContainer = document.getElementById("gameContainer");

  // Debug logging
  console.log("newGameBtn:", newGameBtn);
  console.log("joinGameBtn:", joinGameBtn);
  console.log("overlay:", overlay);

  // Set up event listeners for menu buttons
  newGameBtn.addEventListener("click", showNewGameOptions);
  joinGameBtn.addEventListener("click", showJoinGameForm);

  // Global variable to hold the client Piece color
  let clientColor = "WHITE";
  // Global variable to hold the current session summary
  let currentSessionSummary = null;

  // Function to show overlay with new game options
  function showNewGameOptions() {
    console.log("showNewGameOptions called");
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Start a New Game";
    overlayBody.innerHTML = `
      <button id="pvpBtn">Player vs Player</button>
      <button id="pvcBtn">Player vs Computer</button>
    `;
    document.getElementById("pvpBtn").addEventListener("click", function() {
      startGame("PLAYER_VS_PLAYER");
    });
    document.getElementById("pvcBtn").addEventListener("click", function() {
      startGame("PLAYER_VS_COMPUTER");
    });
  }

  // Function to show overlay with join game form
  function showJoinGameForm() {
    console.log("showJoinGameForm called");
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Join a Game";
    overlayBody.innerHTML = `
      <input type="text" id="sessionIdInput" placeholder="Enter Session ID" />
      <button id="joinBtn">Join</button>
    `;
    document.getElementById("joinBtn").addEventListener("click", function() {
      joinGame();
    });
  }

  window.startGame = function(gameType) {
    console.log("startGame called with:", gameType);
    overlay.classList.add("hidden");
    // Call the backend endpoint for creating a new session
    fetch(`/api/session/create?gameType=${gameType}&color=WHITE`, { method: "POST" })
      .then(res => res.json())
      .then(data => {
      console.log("New Game:", data);
      clientColor = "WHITE";
      connectToSocket(data.sessionId);
      enterGame(data);
    })
      .catch(err => console.error("Error creating game:", err));
  };

  window.joinGame = function() {
    const sessionIdInput = document.getElementById("sessionIdInput");
    if (!sessionIdInput || !sessionIdInput.value) {
      alert("Please enter a session ID");
      return;
    }
    console.log("joinGame called with sessionId:", sessionIdInput.value);
    overlay.classList.add("hidden");
    // Call the backend endpoint for joining an existing session
    fetch(`/api/session/${sessionIdInput.value}/join`, { method: "POST" })
      .then(res => res.json())
      .then(data => {
      console.log("Joined Game:", data);
      clientColor = "BLACK";
      connectToSocket(data.sessionId);
      enterGame(data);
    })
      .catch(err => console.error("Error joining game:", err));
  };

  // Transition to game page and render game state
  function enterGame(sessionSummary) {
    console.log("enterGame called with:", sessionSummary);
    menuPage.classList.add("hidden");
    gamePage.classList.remove("hidden");
    gamePage.style.display = "block";
    // Make sure the game container is visible
    gameContainer.style.display = "inline-block";
    currentSessionSummary = sessionSummary;
    renderGame(sessionSummary);
  }

  function pollGameState(sessionId) {
    fetch(`/api/session/${sessionId}`)
      .then(response => response.json())
      .then(sessionSummary => {
      // Update global session summary and re-render the game
      currentSessionSummary = sessionSummary;
      // if the game is over, stop polling
      if (sessionSummary.gameState && sessionSummary.gameState !== "IN_PROGRESS") {
        clearInterval(pollingIntervalId);
        pollingIntervalId = null;
      }
      else {
        renderGame(sessionSummary);
      }
    })
      .catch(error => {
      console.error("Error polling game state:", error);
    });
  }


  let pollingIntervalId = null;
  //polling interval is 30s , lower the interval for multiplayer needs.
  function startPolling(sessionId) {
    pollingIntervalId = setInterval(() => {
      pollGameState(sessionId);
    }, 30000);
  }

  function renderGame(sessionSummary) {
    console.log("renderGame called with:", sessionSummary);
    if (!sessionSummary || !sessionSummary.board || !sessionSummary.board.boardCells) {
      console.error("Invalid session summary!");
      return;
    }

    // Update scores and show board
    scoresDiv.style.display = "block";
    boardDiv.style.display = "grid";
    blackScoreVal.textContent = sessionSummary.blackScore || 0;
    whiteScoreVal.textContent = sessionSummary.whiteScore || 0;

    // Check if game is over.
    if (sessionSummary.gameState && sessionSummary.gameState !== "IN_PROGRESS") {
      renderBoard(sessionSummary.board.boardCells, []);
      setTimeout(() => {
        alert("Game Over! Result: " + sessionSummary.gameState);
      }, 100);
      return;
    }

    // Fetch valid moves.
    fetchPossibleMoves(sessionSummary.sessionId, sessionSummary.currentPlayerColor)
      .then(validMoves => {
      console.log("Valid moves:", validMoves);
      if (validMoves.length === 0) {
        passTurn(sessionSummary.sessionId, sessionSummary.currentPlayerColor)
      } else {
        // Valid moves exist, so render the board with highlights.
        renderBoard(sessionSummary.board.boardCells, validMoves);
      }
    })
      .catch(err => {
      console.error("Error fetching possible moves:", err);
      renderBoard(sessionSummary.board.boardCells, []);
    });
  }




  // Render the board using CSS grid based on boardCells (2D array) and valid moves
  function renderBoard(boardCells, validMoves) {
    boardDiv.innerHTML = "";
    const rows = boardCells.length;
    const cols = boardCells[0].length;
    boardDiv.style.gridTemplateColumns = `repeat(${cols}, 50px)`;
    boardDiv.style.gridTemplateRows = `repeat(${rows}, 50px)`;

    // Create a lookup for valid moves if provided
    const validMovesLookup = {};
    if (Array.isArray(validMoves)) {
      validMoves.forEach(move => {
        validMovesLookup[`${move.row},${move.column}`] = true;
      });
    }

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const cellDiv = document.createElement("div");
        cellDiv.classList.add("cell");
        const piece = boardCells[r][c];
        if (piece === "B") {
          const pieceDiv = document.createElement("div");
          pieceDiv.classList.add("piece", "black");
          cellDiv.appendChild(pieceDiv);
        } else if (piece === "W") {
          const pieceDiv = document.createElement("div");
          pieceDiv.classList.add("piece", "white");
          cellDiv.appendChild(pieceDiv);
        }

        // If this cell is a valid move, add a highlight indicator (an empty blue circle)
        if (validMovesLookup[`${r},${c}`]) {
          const indicator = document.createElement("div");
          indicator.style.width = "80%";
          indicator.style.height = "80%";
          indicator.style.borderRadius = "50%";
          indicator.style.border = "2px solid blue";
          indicator.style.position = "absolute";
          indicator.style.top = "10%";
          indicator.style.left = "10%";
          cellDiv.appendChild(indicator);
        }

        cellDiv.addEventListener("click", () => onCellClick(r, c));
        boardDiv.appendChild(cellDiv);
      }
    }
  }

  // cell click handler for moves
  // Update your cell click handler to call makeMove
  function onCellClick(row, col) {
    console.log("Cell clicked:", row, col);
    if (!currentSessionSummary || !currentSessionSummary.sessionId) {
      console.error("No session available for making a move.");
      return;
    }
    console.log("Current turn from session:", currentSessionSummary.currentPlayerColor);
    console.log("Client color:", clientColor);
    // Only allow move if it's your turn
    if (currentSessionSummary.currentPlayerColor.toUpperCase() !== clientColor.toUpperCase()) {
      console.log("Not your turn!");
      return;
    }
    makeMove(currentSessionSummary.sessionId, row, col, clientColor);
  }

  // Function to call the move endpoint
  function makeMove(sessionId, row, col, color) {
    console.log(`makeMove called with sessionId: ${sessionId}, row: ${row}, col: ${col}, color: ${color}`);
    fetch('/api/game/move', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: sessionId,
        row: row,
        column: col,
        color: color,
        pass : false
      })
    })
      .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      return response.json();
    })
      .then(data => {
      console.log("Move response:", data);
    })
      .catch(error => {
      console.error("Error making move:", error);
    });
  }

  function fetchPossibleMoves(sessionId, color) {
    return fetch('/api/game/possible-moves', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: sessionId,
        color: color
      })
    })
      .then(response => {
      if (!response.ok) {
        throw new Error('Failed to fetch possible moves');
      }
      return response.json();
    })
      .catch(err => {
      console.error('Error fetching possible moves:', err);
      return [];
    });
  }

  // Pass turn function: calls the move endpoint with pass: true and returns a promise.
  function passTurn(sessionId, color) {
    return fetch('/api/game/move', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: sessionId,
        color: color,
        pass: true // This tells the backend to pass the turn.
      })
    })
      .then(response => {
      if (!response.ok) {
        throw new Error(`Failed to pass turn: ${response.status}`);
      }
      return response.json();
    })
      .then(data => {
      console.log("Turn passed. Updated session:", data);
    })
      .catch(error => {
      console.error("Error passing turn:", error);
      throw error;
    });
  }

  function connectToSocket(gameId) {
    console.log("connecting to the game");
    let socket = new SockJS('/move');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
      console.log("connected to the frame: " + frame);
      stompClient.subscribe("/topic/game-progress/" + gameId, function (response) {
        let data = JSON.parse(response.body);
        console.log(data);
        currentSessionSummary = data.sessionSummary;
        renderGame(currentSessionSummary);
      })
    })
  }


  // Global functions for in-game buttons
  window.quitGame = function() {
    console.log("Quitting game...");
    if (pollingIntervalId !== null) {
      clearInterval(pollingIntervalId);
      pollingIntervalId = null;
    }
    gamePage.classList.add("hidden");
    menuPage.classList.remove("hidden");
    boardDiv.style.display = "none";
    boardDiv.innerHTML = "";
    scoresDiv.style.display = "none";
  };

  window.menuRedirect = function() {
    quitGame();
  };
});
