# Spectator mode focused manual scenarios

1. Create a PVP game in one authenticated browser and join it with a second account. In a signed-out window, choose **Watch Game**, enter the session ID, and confirm that no player seat changes.
2. Open `/?spectate=<sessionId>` in a signed-out window. Confirm the board, both names, scores, and active turn render without a login redirect.
3. Make moves from both player windows. Confirm the spectator updates through `/topic/game-progress/<sessionId>` without refreshing and never shows blue legal-move indicators.
4. Click every type of spectator board cell and inspect the Network panel. Confirm no move request is sent.
5. Reach a position in which one color passes. Confirm the spectator status names the passed color and the player who moves next.
6. Complete a game, then open its spectator link in a new window. Confirm the final board, scores, names, and result render immediately.
7. Enter an invalid session ID and open an invalid direct link. Confirm a clear “Game not found or no longer available” error appears and no game view opens.
8. Send `POST /api/v1/sessions/<sessionId>/moves` manually as a signed-out user and as an authenticated non-player. Confirm responses are respectively 401 and 403 and the board is unchanged.
