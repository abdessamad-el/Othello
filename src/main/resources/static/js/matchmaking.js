(() => {
  const Reversi = window.Reversi = window.Reversi || {};
  Reversi.state = Reversi.state || {};
  Reversi.config = Reversi.config || {};

  function showMatchmakingForm(user) {
    if (!user) {
      window.Auth.requireAuth(
        { type: "MATCHMAKING_SHOW_DIALOG" },
        authenticatedUser => showMatchmakingForm(authenticatedUser)
      );
      return;
    }
    const { overlay, overlayTitle, overlayBody } = Reversi.elements;
    console.log("showMatchmakingForm called");
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Find a Match";
    overlayBody.innerHTML = `
      <p class="matchmaking-note">Signed in as <strong><span id="matchmakingUsername">-</span></strong></p>
      <label for="matchColorSelect">Preferred Color</label>
      <select id="matchColorSelect">
        <option value="">No preference</option>
        <option value="WHITE">White</option>
        <option value="BLACK">Black</option>
      </select>
      <div class="overlay-actions">
        <button id="startMatchmakingBtn">Find Match</button>
      </div>
    `;
    const usernameDisplay = document.getElementById("matchmakingUsername");
    const effectiveUsername = user && user.username ? user.username : Reversi.state.lastAuthenticatedUsername;
    if (usernameDisplay && effectiveUsername) {
      usernameDisplay.textContent = effectiveUsername;
    }
    document.getElementById("startMatchmakingBtn").addEventListener("click", function() {
      const colorSelect = document.getElementById("matchColorSelect");
      const preferredColor = colorSelect && colorSelect.value ? colorSelect.value : null;
      beginMatchmaking(preferredColor);
    });

  }

  function beginMatchmaking(preferredColor) {
    const { overlayTitle, overlayBody,overlay } = Reversi.elements;
    overlay.classList.remove("hidden");
    overlayTitle.textContent = "Searching for Opponent";
    overlayBody.innerHTML = `
      <p>Looking for a match${preferredColor ? ` as <strong>${preferredColor}</strong>` : ""}</p>
      <p class="matchmaking-status">Waiting for an opponent...</p>
      <button id="cancelMatchmakingBtn">Cancel</button>
    `;
    document.getElementById("cancelMatchmakingBtn").addEventListener("click", cancelMatchmaking);
    enqueueMatchmaking(preferredColor);

  }

  function enqueueMatchmaking(preferredColor) {
    const {  overlayBody } = Reversi.elements;
    const pendingAction = {
      type: "MATCHMAKING_ENQUEUE",
      payload: { preferredColor }
    };
    window.Auth.authFetch('/api/matchmaking/enqueue', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        preferredColor: preferredColor
      })
    }, pendingAction)
      .then(response => {
        if (!response.ok) {
          throw new Error(`Failed to enqueue matchmaking: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        const ticketId = String(data).trim();
        Reversi.state.matchmakingTicketId = ticketId;
        subscribeToMatchmaking(ticketId);
        fetchMatchmakingStatus(ticketId);
      })
      .catch(error => {
        console.error("Error enqueuing matchmaking:", error);
        overlayBody.innerHTML = `
          <p class="error">Unable to start matchmaking. Please try again later.</p>
        `;
      });
  }

  function subscribeToMatchmaking(ticketId) {
    console.log("Subscribing to matchmaking ticket", ticketId);
    const socket = new SockJS(Reversi.config.WEBSOCKET_ENDPOINT);
    const stomp = Stomp.over(socket);
    Reversi.state.matchmakingClient = stomp;
    stomp.connect({}, function() {
      Reversi.state.matchmakingSubscription = stomp.subscribe(`/topic/matchmaking/${ticketId}`, function(message) {
        try {
          const payload = JSON.parse(message.body);
          handleMatchmakingUpdate(payload);
        } catch (err) {
          console.error("Failed to parse matchmaking update", err);
        }
      });
    }, function(error) {
      console.error("Matchmaking websocket connection error", error);
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
    const { overlay, overlayTitle, overlayBody } = Reversi.elements;
    console.log("Matchmaking update", payload);
    const status = payload.status;
    if (status === "FOUND" && payload.gameSession) {
      cleanupMatchmakingConnection();
      overlay.classList.add("hidden");
      Reversi.state.currentSessionSummary = payload.gameSession;
      if (payload.assignedColor) {
        Reversi.state.clientColor = payload.assignedColor.toUpperCase();
      }
      window.Game.connectToSocket(Reversi.state.currentSessionSummary.sessionId);
      window.Game.enterGame(Reversi.state.currentSessionSummary);
    } else if (status === "CANCELED" || status === "EXPIRED") {
      cleanupMatchmakingConnection();
      overlayTitle.textContent = status === "CANCELED" ? "Matchmaking Cancelled" : "Matchmaking Expired";
      overlayBody.innerHTML = `
        <p>${status === "CANCELED" ? "You cancelled matchmaking." : "Matchmaking timed out."}</p>
      `;
    }
  }

  function cleanupMatchmakingConnection() {
    Reversi.state.matchmakingTicketId = null;
    if (Reversi.state.matchmakingSubscription) {
      Reversi.state.matchmakingSubscription.unsubscribe();
      Reversi.state.matchmakingSubscription = null;
    }
    if(Reversi.state.matchmakingClient) {
      try {
        if (Reversi.state.matchmakingClient.connected) {
          Reversi.state.matchmakingClient.disconnect(() => {
            console.log("Matchmaking websocket disconnected");
          });
        }
      } catch (err) {
        console.warn("Error disconnecting matchmaking websocket", err);
      }
    }
    Reversi.state.matchmakingClient = null;
  }

  function cancelMatchmaking() {
    const { overlay, overlayTitle, overlayBody } = Reversi.elements;
    if (!Reversi.state.matchmakingTicketId) {
      overlay.classList.add("hidden");
      return;
    }
    fetch(`/api/matchmaking/cancel/${Reversi.state.matchmakingTicketId}`, {
      method: 'DELETE'
    })
      .catch(error => console.error("Error cancelling matchmaking", error))
      .finally(() => {
        cleanupMatchmakingConnection();
        overlayTitle.textContent = "Matchmaking Cancelled";
        overlayBody.innerHTML = `
          <p>You have left the matchmaking queue.</p>
        `;
      });
  }

  window.Matchmaking = {
    showMatchmakingForm
  };
})();
