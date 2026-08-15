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
        Reversi.state.isSpectator = false;
        Reversi.state.clientColor = "WHITE";
        Reversi.state.lastAnnouncedGameState = null;
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
        Reversi.state.isSpectator = false;
        Reversi.state.clientColor = "BLACK";
        Reversi.state.lastAnnouncedGameState = null;
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

  function watchGame(explicitSessionId) {
    const sessionIdInput = document.getElementById("spectatorSessionIdInput");
    const providedId = typeof explicitSessionId === "string" ? explicitSessionId.trim() : "";
    const enteredId = sessionIdInput && typeof sessionIdInput.value === "string"
      ? sessionIdInput.value.trim()
      : "";
    const targetSessionId = providedId || enteredId;
    if (!targetSessionId) {
      showSpectatorError("Please enter a session ID.");
      return;
    }

    clearSpectatorError();
    fetch(`/api/v1/sessions/${encodeURIComponent(targetSessionId)}`)
      .then(response => {
        if (!response.ok) {
          if (response.status === 404) {
            throw new Error("Game not found or no longer available.");
          }
          throw new Error("Unable to load this game. Please try again.");
        }
        return response.json();
      })
      .then(sessionSummary => {
        Reversi.state.isSpectator = true;
        Reversi.state.clientColor = null;
        Reversi.state.lastAnnouncedGameState = null;
        window.history.replaceState(
          null,
          "",
          window.SpectatorMode.spectatorUrl(window.location.href, sessionSummary.sessionId)
        );
        Reversi.elements.overlay.classList.add("hidden");
        connectToSocket(sessionSummary.sessionId);
        enterGame(sessionSummary);
      })
      .catch(error => {
        console.error("Error watching game:", error);
        showSpectatorError(error.message);
      });
  }

  function showSpectatorError(message) {
    const { overlay, overlayTitle } = Reversi.elements;
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Watch a Game";
    let errorElement = document.getElementById("spectatorError");
    if (!errorElement) {
      window.Menu.showWatchGameForm();
      errorElement = document.getElementById("spectatorError");
    }
    errorElement.textContent = message;
    errorElement.classList.remove("hidden");
  }

  function clearSpectatorError() {
    const errorElement = document.getElementById("spectatorError");
    if (errorElement) {
      errorElement.textContent = "";
      errorElement.classList.add("hidden");
    }
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
    Reversi.elements.gameStatus.textContent = window.SpectatorMode.gameStatusText(
      sessionSummary,
      Reversi.state.isSpectator
    );

    if (sessionSummary.gameState && sessionSummary.gameState !== "IN_PROGRESS") {
      renderBoard(sessionSummary.board.boardCells, []);
      updateScoreboardNames(sessionSummary);
      updateTurnHighlight(sessionSummary);
      if (!Reversi.state.isSpectator && Reversi.state.lastAnnouncedGameState !== sessionSummary.gameState) {
        Reversi.state.lastAnnouncedGameState = sessionSummary.gameState;
        setTimeout(() => {
          alert("Game Over! Result: " + sessionSummary.gameState);
        }, 100);
      }
      return;
    }

    updateScoreboardNames(sessionSummary);
    updateTurnHighlight(sessionSummary);

    const shouldRequestMoves = window.SpectatorMode.shouldRequestPossibleMoves({
      isSpectator: Reversi.state.isSpectator,
      clientColor: Reversi.state.clientColor,
      currentPlayerColor: sessionSummary.currentPlayerColor
    });
    if (!shouldRequestMoves) {
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
    const canInteract = window.SpectatorMode.canInteractWithBoard({
      isSpectator: Reversi.state.isSpectator,
      clientColor: Reversi.state.clientColor,
      currentPlayerColor: Reversi.state.currentSessionSummary
        ? Reversi.state.currentSessionSummary.currentPlayerColor
        : null
    });
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

        if (canInteract) {
          cellDiv.classList.add("interactive");
          cellDiv.addEventListener("click", () => onCellClick(r, c));
        }
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
    if (!window.SpectatorMode.canInteractWithBoard({
      isSpectator: Reversi.state.isSpectator,
      clientColor: Reversi.state.clientColor,
      currentPlayerColor: Reversi.state.currentSessionSummary.currentPlayerColor
    })) {
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
    disconnectFromSocket();
    let socket = new SockJS(Reversi.config.WEBSOCKET_ENDPOINT);
    const stompClient = Stomp.over(socket);
    Reversi.state.gameSocket = socket;
    Reversi.state.stompClient = stompClient;
    stompClient.connect({}, function (frame) {
      if (Reversi.state.stompClient !== stompClient) {
        if (stompClient.connected) {
          stompClient.disconnect();
        }
        return;
      }
      console.log("connected to the frame: " + frame);
      Reversi.state.gameSubscription = stompClient.subscribe(
        "/topic/game-progress/" + gameId,
        function (response) {
          let data = JSON.parse(response.body);
          Reversi.state.currentSessionSummary = data.sessionSummary;
          renderGame(Reversi.state.currentSessionSummary);
        }
      );
    });
  }

  function disconnectFromSocket() {
    if (Reversi.state.gameSubscription) {
      Reversi.state.gameSubscription.unsubscribe();
      Reversi.state.gameSubscription = null;
    }
    const clientWasConnected = Reversi.state.stompClient && Reversi.state.stompClient.connected;
    if (clientWasConnected) {
      Reversi.state.stompClient.disconnect();
    }
    if (!clientWasConnected && Reversi.state.gameSocket && Reversi.state.gameSocket.readyState !== 3) {
      Reversi.state.gameSocket.close();
    }
    Reversi.state.stompClient = null;
    Reversi.state.gameSocket = null;
  }

  function showSessionInfo(summary) {
    const { sessionDetails, sessionIdLabel, copySessionIdBtn, copyWatchLinkBtn } = Reversi.elements;
    if (summary && summary.sessionId) {
      sessionIdLabel.textContent = `Session ID: ${summary.sessionId}`;
      sessionDetails.classList.remove("hidden");
      copySessionIdBtn.textContent = "Copy";
      copySessionIdBtn.disabled = false;
      copyWatchLinkBtn.textContent = "Copy watch link";
      copyWatchLinkBtn.disabled = false;
    }
  }

  function hideSessionInfo() {
    const { sessionDetails, sessionIdLabel, copySessionIdBtn, copyWatchLinkBtn } = Reversi.elements;
    sessionDetails.classList.add("hidden");
    sessionIdLabel.textContent = "Session ID: -";
    copySessionIdBtn.textContent = "Copy";
    copySessionIdBtn.disabled = false;
    copyWatchLinkBtn.textContent = "Copy watch link";
    copyWatchLinkBtn.disabled = false;

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
      .then(() => provideCopyFeedback(Reversi.elements.copySessionIdBtn, "Copied!", "Copy"))
      .catch(() => {
        const manual = window.prompt("Copy the session ID", sessionId);
        if (manual !== null) {
          provideCopyFeedback(Reversi.elements.copySessionIdBtn, "Copied!", "Copy");
        }
      });
  }

  function copySpectatorLink() {
    if (!Reversi.state.currentSessionSummary || !Reversi.state.currentSessionSummary.sessionId) {
      return;
    }
    const link = window.SpectatorMode.spectatorUrl(
      window.location.href,
      Reversi.state.currentSessionSummary.sessionId
    );
    const canUseClipboard = navigator.clipboard && navigator.clipboard.writeText;
    const promise = canUseClipboard ? navigator.clipboard.writeText(link)
      : Promise.reject(new Error("Clipboard API unavailable"));

    promise
      .then(() => provideCopyFeedback(Reversi.elements.copyWatchLinkBtn, "Copied!", "Copy watch link"))
      .catch(() => {
        const manual = window.prompt("Copy the spectator link", link);
        if (manual !== null) {
          provideCopyFeedback(Reversi.elements.copyWatchLinkBtn, "Copied!", "Copy watch link");
        }
      });
  }

  function provideCopyFeedback(button, message, defaultLabel) {
    button.textContent = message;
    button.disabled = true;

    if (Reversi.state.copyFeedbackTimeout) {
      clearTimeout(Reversi.state.copyFeedbackTimeout);
    }

    Reversi.state.copyFeedbackTimeout = setTimeout(() => {
      button.textContent = defaultLabel;
      button.disabled = false;
      Reversi.state.copyFeedbackTimeout = null;
    }, 2000);


  }


  function quitGame() {
    const { gamePage, menuPage, gameContainer } = Reversi.elements;
    console.log("Quitting game...");
    disconnectFromSocket();
    gamePage.classList.add("hidden");
    menuPage.classList.remove("hidden");
    gameContainer.classList.add("hidden");
    Reversi.elements.blackPlayerName.textContent = "Black Player";
    Reversi.elements.whitePlayerName.textContent = "White Player";
    Reversi.elements.blackScoreBox.classList.remove("active");
    Reversi.elements.whiteScoreBox.classList.remove("active");
    Reversi.elements.blackScoreVal.textContent = "0";
    Reversi.elements.whiteScoreVal.textContent = "0";
    Reversi.elements.gameStatus.textContent = "";
    const wasSpectating = Reversi.state.isSpectator;
    Reversi.state.isSpectator = false;
    Reversi.state.clientColor = "WHITE";
    Reversi.state.currentSessionSummary = null;
    Reversi.state.lastAnnouncedGameState = null;
    hideSessionInfo();
    if (wasSpectating) {
      const menuUrl = new URL(window.location.href);
      menuUrl.searchParams.delete("spectate");
      window.history.replaceState(null, "", menuUrl.toString());
    }
  }

  function menuRedirect() {
    quitGame();
  }

  window.Game = {
    startGame,
    joinGame,
    watchGame,
    enterGame,
    connectToSocket,
    disconnectFromSocket,
    copySessionId,
    copySpectatorLink
  };

  window.startGame = startGame;
  window.joinGame = joinGame;
  window.watchGame = watchGame;
  window.quitGame = quitGame;
  window.menuRedirect = menuRedirect;
})();
