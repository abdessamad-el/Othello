document.addEventListener("DOMContentLoaded", function() {
  // Element references
  const menuPage = document.getElementById("menuPage");
  const gamePage = document.getElementById("gamePage");
  const newGameBtn = document.getElementById("newGameBtn");
  const joinGameBtn = document.getElementById("joinGameBtn");
  const findMatchBtn = document.getElementById("findMatchBtn");
  const overlay = document.getElementById("overlay");
  const overlayTitle = document.getElementById("overlayTitle");
  const overlayBody = document.getElementById("overlayBody");
  const scoresDiv = document.getElementById("scores");
  const blackScoreVal = document.getElementById("blackScoreVal");
  const whiteScoreVal = document.getElementById("whiteScoreVal");
  const blackPlayerName = document.getElementById("blackPlayerName");
  const whitePlayerName = document.getElementById("whitePlayerName");
  const blackScoreBox = document.getElementById("blackScoreBox");
  const whiteScoreBox = document.getElementById("whiteScoreBox");
  const sessionDetails = document.getElementById("sessionDetails");
  const sessionIdLabel = document.getElementById("sessionIdLabel");
  const copySessionIdBtn = document.getElementById("copySessionIdBtn");
  const boardDiv = document.getElementById("board");
  const gameContainer = document.getElementById("gameContainer");

  // Debug logging
  console.log("newGameBtn:", newGameBtn);
  console.log("joinGameBtn:", joinGameBtn);
  console.log("overlay:", overlay);

  // Set up event listeners for menu buttons
  newGameBtn.addEventListener("click", showNewGameOptions);
  joinGameBtn.addEventListener("click", showJoinGameForm);
  findMatchBtn.addEventListener("click", showMatchmakingForm);

  // Global variables for gameplay state
  let clientColor = "WHITE";
  let currentSessionSummary = null;
  let stompClient = null;
  let renderSequence = 0;

  // Matchmaking state
  let matchmakingClient = null;
  let matchmakingSubscription = null;
  let matchmakingTicketId = null;
  let matchmakingNickname = null;
  let copyFeedbackTimeout = null;

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

  function showMatchmakingForm() {
    console.log("showMatchmakingForm called");
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Find a Match";
    overlayBody.innerHTML = `
      <label for="matchNicknameInput">Nickname</label>
      <input type="text" id="matchNicknameInput" placeholder="Enter nickname" />
      <label for="matchColorSelect">Preferred Color</label>
      <select id="matchColorSelect">
        <option value="">No preference</option>
        <option value="WHITE">White</option>
        <option value="BLACK">Black</option>
      </select>
      <div class="overlay-actions">
        <button id="startMatchmakingBtn">Find Match</button>
        <button id="closeMatchmakingBtn">Close</button>
      </div>
    `;
    document.getElementById("startMatchmakingBtn").addEventListener("click", function() {
      const nicknameInput = document.getElementById("matchNicknameInput");
      const colorSelect = document.getElementById("matchColorSelect");
      const nicknameValue = nicknameInput.value.trim();
      const nickname = nicknameValue.length > 0 ? nicknameValue : `Player-${Math.floor(Math.random() * 1000)}`;
      const preferredColor = colorSelect.value ? colorSelect.value : null;
      beginMatchmaking(nickname, preferredColor);
    });
    document.getElementById("closeMatchmakingBtn").addEventListener("click", function() {
      overlay.classList.add("hidden");
    });
  }

  function beginMatchmaking(nickname, preferredColor) {
    matchmakingNickname = nickname;
    overlayTitle.textContent = "Searching for Opponent";
    overlayBody.innerHTML = `
      <p>Looking for a match as <strong>${nickname}</strong>${preferredColor ? ` (${preferredColor})` : ""}</p>
      <p class="matchmaking-status">Waiting for an opponent...</p>
      <button id="cancelMatchmakingBtn">Cancel</button>
    `;
    document.getElementById("cancelMatchmakingBtn").addEventListener("click", cancelMatchmaking);
    enqueueMatchmaking(nickname, preferredColor);
  }

  function enqueueMatchmaking(nickname, preferredColor) {
    fetch('/api/matchmaking/enqueue', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        nickName: nickname,
        preferredColor: preferredColor
      })
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`Failed to enqueue matchmaking: ${response.status}`);
        }
        return response.text();
      })
      .then(text => {
        const cleaned = text.replace(/^"|"$/g, '').trim();
        matchmakingTicketId = cleaned;
        subscribeToMatchmaking(cleaned);
        // Ensure we don't miss a quick match resolution by checking status immediately
        fetchMatchmakingStatus(cleaned);
      })
      .catch(error => {
        console.error("Error enqueuing matchmaking:", error);
        overlayBody.innerHTML = `
          <p class="error">Unable to start matchmaking. Please try again later.</p>
          <button id="closeMatchmakingBtn">Close</button>
        `;
        document.getElementById("closeMatchmakingBtn").addEventListener("click", function() {
          overlay.classList.add("hidden");
        });
      });
  }

  function subscribeToMatchmaking(ticketId) {
    console.log("Subscribing to matchmaking ticket", ticketId);
    const socket = new SockJS('/move');
    const stomp = Stomp.over(socket);
    matchmakingClient = stomp;
    stomp.connect({}, function() {
      if (matchmakingClient !== stomp) {
        try {
          stomp.disconnect();
        } catch (err) {
          console.debug("Stale matchmaking connection closed", err);
        }
        return;
      }
      if (!matchmakingTicketId || matchmakingTicketId !== ticketId) {
        try {
          stomp.disconnect();
        } catch (err) {
          console.debug("Matchmaking connection ended before subscribe", err);
        }
        return;
      }
      matchmakingSubscription = stomp.subscribe(`/topic/matchmaking/${ticketId}`, function(message) {
        try {
          const payload = JSON.parse(message.body);
          handleMatchmakingUpdate(payload);
        } catch (err) {
          console.error("Failed to parse matchmaking update", err);
        }
      });
    }, function(error) {
      console.error("Matchmaking websocket connection error", error);
      // Fall back to polling if websocket fails
      fetchMatchmakingStatus(ticketId);
    });
  }

  function fetchMatchmakingStatus(ticketId) {
    fetch(`/api/matchmaking/${ticketId}`)
      .then(response => {
        if (!response.ok) {
          throw new Error(`Status request failed: ${response.status}`);
        }
        return response.json();
      })
      .then(payload => {
        handleMatchmakingUpdate(payload);
      })
      .catch(error => {
        console.error("Error fetching matchmaking status", error);
      });
  }

  function handleMatchmakingUpdate(payload) {
    if (!payload || !payload.status) {
      return;
    }
    console.log("Matchmaking update", payload);
    const status = payload.status;
    if (status === "FOUND" && payload.gameSession) {
      cleanupMatchmakingConnection();
      overlay.classList.add("hidden");
      currentSessionSummary = payload.gameSession;
      if (payload.assignedColor) {
        clientColor = payload.assignedColor.toUpperCase();
      }
      connectToSocket(currentSessionSummary.sessionId);
      enterGame(currentSessionSummary);
    } else if (status === "CANCELED" || status === "EXPIRED") {
      cleanupMatchmakingConnection();
      overlayTitle.textContent = status === "CANCELED" ? "Matchmaking Cancelled" : "Matchmaking Expired";
      overlayBody.innerHTML = `
        <p>${status === "CANCELED" ? "You cancelled matchmaking." : "Matchmaking timed out."}</p>
        <button id="closeMatchmakingBtn">Close</button>
      `;
      document.getElementById("closeMatchmakingBtn").addEventListener("click", function() {
        overlay.classList.add("hidden");
      });
    }
  }

  function cleanupMatchmakingConnection() {
    matchmakingTicketId = null;
    matchmakingNickname = null;
    if (matchmakingSubscription) {
      matchmakingSubscription.unsubscribe();
      matchmakingSubscription = null;
    }
    if (matchmakingClient) {
      try {
        if (matchmakingClient.connected) {
          matchmakingClient.disconnect(() => {
            console.log("Matchmaking websocket disconnected");
          });
        } else {
          // underlying stomp will call error callback; just drop reference
        }
      } catch (err) {
        console.warn("Error disconnecting matchmaking websocket", err);
      }
      matchmakingClient = null;
    }
  }

  function cancelMatchmaking() {
    if (!matchmakingTicketId) {
      overlay.classList.add("hidden");
      return;
    }
    fetch(`/api/matchmaking/cancel/${matchmakingTicketId}`, {
      method: 'DELETE'
    })
      .catch(error => console.error("Error cancelling matchmaking", error))
      .finally(() => {
        cleanupMatchmakingConnection();
        overlayTitle.textContent = "Matchmaking Cancelled";
        overlayBody.innerHTML = `
          <p>You have left the matchmaking queue.</p>
          <button id="closeMatchmakingBtn">Close</button>
        `;
        document.getElementById("closeMatchmakingBtn").addEventListener("click", function() {
          overlay.classList.add("hidden");
        });
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
    showSessionInfo(sessionSummary);
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
    const renderToken = ++renderSequence;
    console.log("renderGame called with:", sessionSummary);
    if (!sessionSummary || !sessionSummary.board || !sessionSummary.board.boardCells) {
      console.error("Invalid session summary!");
      return;
    }

    showSessionInfo(sessionSummary);
    // Update scores and show board
    scoresDiv.style.display = "block";
    boardDiv.style.display = "grid";
    blackScoreVal.textContent = sessionSummary.blackScore || 0;
    whiteScoreVal.textContent = sessionSummary.whiteScore || 0;

    // Check if game is over.
    if (sessionSummary.gameState && sessionSummary.gameState !== "IN_PROGRESS") {
      renderBoard(sessionSummary.board.boardCells, []);
      updateScoreboardNames(sessionSummary);
      updateTurnHighlight(sessionSummary);
      setTimeout(() => {
        alert("Game Over! Result: " + sessionSummary.gameState);
      }, 100);
      return;
    }

    updateScoreboardNames(sessionSummary);
    updateTurnHighlight(sessionSummary);

    // Only fetch possible moves if it's this client's turn
    const isMyTurn = sessionSummary.currentPlayerColor && clientColor &&
      sessionSummary.currentPlayerColor.toUpperCase() === clientColor.toUpperCase();
    if (!isMyTurn) {
      renderBoard(sessionSummary.board.boardCells, []);
      return;
    }

    // Fetch valid moves for the current player (this client)
    fetchPossibleMoves(sessionSummary.sessionId, sessionSummary.currentPlayerColor)
      .then(validMoves => {
        if (renderToken !== renderSequence) {
          return;
        }
        console.log("Valid moves:", validMoves);
        if (validMoves.length === 0) {
        // Auto-pass only if it's our turn and no moves are available
        passTurn(sessionSummary.sessionId, sessionSummary.currentPlayerColor)
        } else {
          // Valid moves exist, so render the board with highlights.
          renderBoard(sessionSummary.board.boardCells, validMoves);
        }
      })
      .catch(err => {
        if (renderToken !== renderSequence) {
          return;
        }
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

  function updateScoreboardNames(sessionSummary) {
    if (!sessionSummary) {
      return;
    }
    const blackName = getPlayerDisplayName(sessionSummary, "BLACK");
    const whiteName = getPlayerDisplayName(sessionSummary, "WHITE");
    if (blackPlayerName) {
      blackPlayerName.textContent = blackName;
    }
    if (whitePlayerName) {
      whitePlayerName.textContent = whiteName;
    }
  }

  function updateTurnHighlight(sessionSummary) {
    if (!sessionSummary) {
      return;
    }
    const activeColor = sessionSummary.currentPlayerColor ? sessionSummary.currentPlayerColor.toUpperCase() : null;
    if (blackScoreBox) {
      blackScoreBox.classList.toggle("active", activeColor === "BLACK");
    }
    if (whiteScoreBox) {
      whiteScoreBox.classList.toggle("active", activeColor === "WHITE");
    }
  }

  function getPlayerDisplayName(summary, color) {
    const fallback = color === "BLACK" ? "Black Player" : "White Player";
    if (!summary) {
      return fallback;
    }
    const colors = Array.isArray(summary.playerColors) ? summary.playerColors : [];
    const names = Array.isArray(summary.playerNicknames) ? summary.playerNicknames : [];
    const index = colors.findIndex(entry => entry && entry.toUpperCase() === color);
    if (index >= 0) {
      const name = names[index];
      if (name && name.trim().length > 0 && name !== "Waiting...") {
        return name;
      }
      if (name === "Waiting...") {
        return "Waiting...";
      }
    }
    if (summary.currentPlayerColor && summary.currentPlayerColor.toUpperCase() === color && summary.currentPlayerNickname) {
      return summary.currentPlayerNickname;
    }
    return fallback;
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

  function showSessionInfo(summary) {
    if (!sessionDetails || !sessionIdLabel) {
      return;
    }
    if (summary && summary.sessionId) {
      sessionIdLabel.textContent = `Session ID: ${summary.sessionId}`;
      sessionDetails.classList.remove("hidden");
      if (copySessionIdBtn) {
        copySessionIdBtn.textContent = "Copy";
        copySessionIdBtn.disabled = false;
      }
    }
  }

  function hideSessionInfo() {
    if (!sessionDetails) {
      return;
    }
    sessionDetails.classList.add("hidden");
    if (sessionIdLabel) {
      sessionIdLabel.textContent = "Session ID: -";
    }
    if (copySessionIdBtn) {
      copySessionIdBtn.textContent = "Copy";
      copySessionIdBtn.disabled = false;
    }
    if (copyFeedbackTimeout) {
      clearTimeout(copyFeedbackTimeout);
      copyFeedbackTimeout = null;
    }
  }

  function copySessionId() {
    if (!currentSessionSummary || !currentSessionSummary.sessionId) {
      return;
    }
    const sessionId = currentSessionSummary.sessionId;
    const canUseClipboard = navigator.clipboard && navigator.clipboard.writeText;
    const promise = canUseClipboard ? navigator.clipboard.writeText(sessionId)
      : Promise.reject(new Error("Clipboard API unavailable"));

    promise
      .then(() => provideCopyFeedback("Copied!"))
      .catch(() => {
        const manual = window.prompt("Copy the session ID", sessionId);
        if (manual !== null) {
          provideCopyFeedback("Copied!");
        }
      });
  }

  function provideCopyFeedback(message) {
    if (!copySessionIdBtn) {
      return;
    }
    copySessionIdBtn.textContent = message;
    copySessionIdBtn.disabled = true;
    if (copyFeedbackTimeout) {
      clearTimeout(copyFeedbackTimeout);
    }
    copyFeedbackTimeout = setTimeout(() => {
      copySessionIdBtn.textContent = "Copy";
      copySessionIdBtn.disabled = false;
      copyFeedbackTimeout = null;
    }, 2000);
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
    if (blackPlayerName) {
      blackPlayerName.textContent = "Black Player";
    }
    if (whitePlayerName) {
      whitePlayerName.textContent = "White Player";
    }
    if (blackScoreBox) {
      blackScoreBox.classList.remove("active");
    }
    if (whiteScoreBox) {
      whiteScoreBox.classList.remove("active");
    }
    blackScoreVal.textContent = "0";
    whiteScoreVal.textContent = "0";
    clientColor = "WHITE";
    hideSessionInfo();
  };

  window.menuRedirect = function() {
    quitGame();
  };

  if (copySessionIdBtn) {
    copySessionIdBtn.addEventListener("click", copySessionId);
  }
});
