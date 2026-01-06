(() => {
  const Reversi = window.Reversi = window.Reversi || {};
  Reversi.state = Reversi.state || {};
  Reversi.config = Reversi.config || {};

  function startGame(gameType) {
    const { overlay } = Reversi.elements;
    console.log("startGame called with:", gameType);
    overlay.classList.add("hidden");
    const url = `/api/v1/sessions?gameType=${gameType}&color=WHITE`;
    console.log(url);
    const requiresAuth = gameType === "PLAYER_VS_PLAYER";
    const pendingAction = requiresAuth ? { type: "SESSION_CREATE", payload: { gameType } } : null;
    const request = window.Auth.authFetch(url, { method: "POST" }, pendingAction);

    request
      .then(res => res.json())
      .then(data => {
        console.log("New Game:", data);
        Reversi.state.clientColor = "WHITE";
        connectToSocket(data.sessionId);
        enterGame(data);
      })
      .catch(err => {
        if (window.Auth.isAuthRedirectError(err)) {
          return;
        }
        console.error("Error creating game:", err);
      });
  }

  function joinGame(explicitSessionId) {
    const { overlay } = Reversi.elements;
    const sessionIdInput = document.getElementById("sessionIdInput");
    const providedId = typeof explicitSessionId === "string" ? explicitSessionId.trim() : "";
    const enteredId = sessionIdInput && typeof sessionIdInput.value === "string"
      ? sessionIdInput.value.trim()
      : "";
    const targetSessionId = providedId || enteredId;
    if (!targetSessionId) {
      alert("Please enter a session ID");
      return;
    }
    console.log("joinGame called with sessionId:", targetSessionId);
    overlay.classList.add("hidden");
    const pendingAction = {
      type: "SESSION_JOIN",
      payload: { sessionId: targetSessionId }
    };
    window.Auth.authFetch(`/api/v1/sessions/${targetSessionId}/join`, { method: "POST" }, pendingAction)
      .then(res => res.json())
      .then(data => {
        console.log("Joined Game:", data);
        Reversi.state.clientColor = "BLACK";
        connectToSocket(data.sessionId);
        enterGame(data);
      })
      .catch(err => {
        if (window.Auth.isAuthRedirectError(err)) {
          return;
        }
        console.error("Error joining game:", err);
      });
  }

  function enterGame(sessionSummary) {
    const { menuPage, gamePage, gameContainer } = Reversi.elements;
    console.log("enterGame called with:", sessionSummary);
    menuPage.classList.add("hidden");
    gamePage.classList.remove("hidden");
    gameContainer.classList.remove("hidden");
    Reversi.state.currentSessionSummary = sessionSummary;
    showSessionInfo(sessionSummary);
    renderGame(sessionSummary);
  }

  function renderGame(sessionSummary) {
    const { gameContainer } = Reversi.elements;
    const renderToken = ++Reversi.state.renderSequence;
    console.log("renderGame called with:", sessionSummary);
    if (!sessionSummary || !sessionSummary.board || !sessionSummary.board.boardCells) {
      console.error("Invalid session summary!");
      return;
    }

    showSessionInfo(sessionSummary);
    gameContainer.classList.remove("hidden");
    Reversi.elements.blackScoreVal.textContent = sessionSummary.blackScore || 0;
    Reversi.elements.whiteScoreVal.textContent = sessionSummary.whiteScore || 0;

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

    const isMyTurn = sessionSummary.currentPlayerColor && Reversi.state.clientColor &&
      sessionSummary.currentPlayerColor.toUpperCase() === Reversi.state.clientColor.toUpperCase();
    if (!isMyTurn) {
      renderBoard(sessionSummary.board.boardCells, []);
      return;
    }

    fetchPossibleMoves(sessionSummary.sessionId, sessionSummary.currentPlayerColor)
      .then(validMoves => {
        if (renderToken !== Reversi.state.renderSequence) {
          return;
        }
        console.log("Valid moves:", validMoves);
        renderBoard(sessionSummary.board.boardCells, validMoves);

      })
      .catch(err => {
        if (renderToken !== Reversi.state.renderSequence) {
          return;
        }
        console.error("Error fetching possible moves:", err);
        renderBoard(sessionSummary.board.boardCells, []);
      });
  }

  function renderBoard(boardCells, validMoves) {
    const { boardDiv } = Reversi.elements;
    boardDiv.innerHTML = "";
    const rows = boardCells.length;
    const cols = boardCells[0].length;
    boardDiv.style.gridTemplateColumns = `repeat(${cols}, 50px)`;
    boardDiv.style.gridTemplateRows = `repeat(${rows}, 50px)`;

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

        if (validMovesLookup[`${r},${c}`]) {
          const indicator = document.createElement("div");
          indicator.classList.add("valid-move-indicator")
          cellDiv.appendChild(indicator);
        }

        cellDiv.addEventListener("click", () => onCellClick(r, c));
        boardDiv.appendChild(cellDiv);
      }
    }
  }

  function onCellClick(row, col) {
    console.log("Cell clicked:", row, col);
    if (!Reversi.state.currentSessionSummary || !Reversi.state.currentSessionSummary.sessionId) {
      console.error("No session available for making a move.");
      return;
    }
    console.log("Current turn from session:", Reversi.state.currentSessionSummary.currentPlayerColor);
    console.log("Client color:", Reversi.state.clientColor);
    if (Reversi.state.currentSessionSummary.currentPlayerColor.toUpperCase() !== Reversi.state.clientColor.toUpperCase()) {
      console.log("Not your turn!");
      return;
    }
    makeMove(Reversi.state.currentSessionSummary.sessionId, row, col, Reversi.state.clientColor);
  }

  function makeMove(sessionId, row, col, color) {
    console.log(`makeMove called with sessionId: ${sessionId}, row: ${row}, col: ${col}, color: ${color}`);
    fetch(`/api/v1/sessions/${sessionId}/moves`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: sessionId,
        row: row,
        column: col,
        color: color,
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
    return fetch(`/api/v1/sessions/${sessionId}/possible-moves?color=${color}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
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

  function updateScoreboardNames(sessionSummary) {
    if (!sessionSummary) {
      return;
    }
    const blackName = getPlayerDisplayName(sessionSummary, "BLACK");
    const whiteName = getPlayerDisplayName(sessionSummary, "WHITE");
    Reversi.elements.blackPlayerName.textContent = blackName;
    Reversi.elements.whitePlayerName.textContent = whiteName;
  }

  function updateTurnHighlight(sessionSummary) {
    if (!sessionSummary) {
      return;
    }
    const activeColor = sessionSummary.currentPlayerColor ? sessionSummary.currentPlayerColor.toUpperCase() : null;
    Reversi.elements.blackScoreBox.classList.toggle("active", activeColor === "BLACK");
    Reversi.elements.whiteScoreBox.classList.toggle("active", activeColor === "WHITE");

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
      if (name && name.trim().length > 0) {
        return name;
      }
    }
    return fallback;
  }

  function connectToSocket(gameId) {
    //console.log("connecting to the game");
    let socket = new SockJS(Reversi.config.WEBSOCKET_ENDPOINT);
    Reversi.state.stompClient = Stomp.over(socket);
    Reversi.state.stompClient.connect({}, function (frame) {
      console.log("connected to the frame: " + frame);
      Reversi.state.stompClient.subscribe("/topic/game-progress/" + gameId, function (response) {
        let data = JSON.parse(response.body);
        //console.log(data);
        Reversi.state.currentSessionSummary = data.sessionSummary;
        renderGame(Reversi.state.currentSessionSummary);
      });
    });
  }

  function showSessionInfo(summary) {
    const { sessionDetails, sessionIdLabel, copySessionIdBtn } = Reversi.elements;
    if (summary && summary.sessionId) {
      sessionIdLabel.textContent = `Session ID: ${summary.sessionId}`;
      sessionDetails.classList.remove("hidden");
      copySessionIdBtn.textContent = "Copy";
      copySessionIdBtn.disabled = false;
    }
  }

  function hideSessionInfo() {
    const { sessionDetails, sessionIdLabel, copySessionIdBtn } = Reversi.elements;
    sessionDetails.classList.add("hidden");
    sessionIdLabel.textContent = "Session ID: -";
    copySessionIdBtn.textContent = "Copy";
    copySessionIdBtn.disabled = false;

    if (Reversi.state.copyFeedbackTimeout) {
      clearTimeout(Reversi.state.copyFeedbackTimeout);
      Reversi.state.copyFeedbackTimeout = null;
    }
  }

  function copySessionId() {
    if (!Reversi.state.currentSessionSummary || !Reversi.state.currentSessionSummary.sessionId) {
      return;
    }
    const sessionId = Reversi.state.currentSessionSummary.sessionId;
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
    const { copySessionIdBtn } = Reversi.elements;
    copySessionIdBtn.textContent = message;
    copySessionIdBtn.disabled = true;

    if (Reversi.state.copyFeedbackTimeout) {
      clearTimeout(Reversi.state.copyFeedbackTimeout);
    }

    Reversi.state.copyFeedbackTimeout = setTimeout(() => {
      copySessionIdBtn.textContent = "Copy";
      copySessionIdBtn.disabled = false;
      Reversi.state.copyFeedbackTimeout = null;
    }, 2000);


  }


  function quitGame() {
    const { gamePage, menuPage, gameContainer } = Reversi.elements;
    console.log("Quitting game...");
    gamePage.classList.add("hidden");
    menuPage.classList.remove("hidden");
    gameContainer.classList.add("hidden");
    Reversi.elements.blackPlayerName.textContent = "Black Player";
    Reversi.elements.whitePlayerName.textContent = "White Player";
    Reversi.elements.blackScoreBox.classList.remove("active");
    Reversi.elements.whiteScoreBox.classList.remove("active");
    Reversi.elements.blackScoreVal.textContent = "0";
    Reversi.elements.whiteScoreVal.textContent = "0";
    Reversi.state.clientColor = "WHITE";
    hideSessionInfo();
  }

  function menuRedirect() {
    quitGame();
  }

  window.Game = {
    startGame,
    joinGame,
    enterGame,
    connectToSocket,
    copySessionId
  };

  window.startGame = startGame;
  window.joinGame = joinGame;
  window.quitGame = quitGame;
  window.menuRedirect = menuRedirect;
})();
