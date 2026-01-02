(() => {
  const Reversi = window.Reversi = window.Reversi || {};
  Reversi.state = Reversi.state || {};

  const PENDING_ACTION_KEY = "reversi.pendingAction";
  const LOGIN_URL = "/login";

  function savePendingAction(action) {
    if (!action) {
      return;
    }
    try {
      localStorage.setItem(PENDING_ACTION_KEY, JSON.stringify(action));
    } catch (err) {
      console.warn("Failed to persist pending action", err);
    }
  }

  function consumePendingAction() {
    try {
      const raw = localStorage.getItem(PENDING_ACTION_KEY);
      if (!raw) {
        return null;
      }
      localStorage.removeItem(PENDING_ACTION_KEY);
      return JSON.parse(raw);
    } catch (err) {
      console.warn("Failed to read pending action", err);
      return null;
    }
  }

  function handleAuthRedirect(pendingAction) {
    if (pendingAction) {
      savePendingAction(pendingAction);
    }
    const next = encodeURIComponent(window.location.pathname + window.location.search + window.location.hash);
    window.location.href = `${LOGIN_URL}?next=${next}`;
  }

  function isAuthRedirectError(error) {
    return !!(error && error.authRedirected);
  }

  function authFetch(url, options, pendingAction) {
    return fetch(url, options).then(response => {
      const redirectTarget = response.redirected ? response.url : "";
      if ((response.status === 401 || response.status === 403) ||
          (redirectTarget && redirectTarget.includes("/login"))) {
        handleAuthRedirect(pendingAction);
        const error = new Error("Authentication required");
        error.authRedirected = true;
        throw error;
      }
      return response;
    });
  }

  function requireAuth(pendingAction, onSuccess) {
    authFetch("/api/matchmaking/auth-check", { method: "GET" }, pendingAction)
      .then(response => response.json())
      .then(user => {
        if (user && user.username) {
          Reversi.state.lastAuthenticatedUsername = user.username;
        }
        if (typeof onSuccess === "function") {
          onSuccess(user);
        }
      })
      .catch(err => {
        if (!isAuthRedirectError(err)) {
          console.error("Authentication check failed", err);
        }
      });
  }

  function resumePendingActionIfAvailable() {
    if (Reversi.state.replayingPendingAction) {
      return;
    }
    const pending = consumePendingAction();
    if (!pending) {
      return;
    }
    console.log("Resuming pending action", pending);
    Reversi.state.replayingPendingAction = true;
    try {
      switch (pending.type) {
        case "MATCHMAKING_SHOW_DIALOG":
          window.Matchmaking.showMatchmakingForm();
          break;
        case "SESSION_CREATE":
          window.Game.startGame(pending.payload.gameType);
          break;
        case "SESSION_JOIN":
          window.Game.joinGame(pending.payload.sessionId);
          break;
        default:
          console.warn("Unknown pending action type", pending);
      }
    } finally {
      Reversi.state.replayingPendingAction = false;
    }
  }

  window.Auth = {
    authFetch,
    requireAuth,
    resumePendingActionIfAvailable,
    isAuthRedirectError
  };
})();
