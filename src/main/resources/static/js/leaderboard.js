(() => {
    const Reversi = (window.Reversi = window.Reversi || {});

    let page = 0;
    const size = 20;
    let loading = false;
    let last = false;
    let scrollBound = false;

    function showLeaderBoard() {
        const {menuPage, leaderboardPage} = Reversi.elements;
        menuPage.classList.add("hidden");

        // Show leaderboard view
        leaderboardPage.classList.remove("hidden");
        resetLeaderboard();
        // load user stats if authenticated
        loadMyStats();
        // Fill enough rows to allow scrolling
        fillToViewport();
        // Bind scroll only once
        if (!scrollBound) {
            scrollBound = true;
            window.addEventListener("scroll", onScroll);
        }
    }

    function showMenu() {
        const {menuPage, leaderboardPage} = Reversi.elements;
        leaderboardPage.classList.add("hidden");
        menuPage.classList.remove("hidden");
        if (scrollBound) {
            window.removeEventListener("scroll", onScroll);
            scrollBound = false;
        }
        window.scrollTo(0, 0);
    }

    function onScroll() {
        const doc = document.documentElement;
        const nearBottom =
            window.innerHeight + window.scrollY >= doc.scrollHeight - 200;
        if (nearBottom) loadMore();
    }

    function resetLeaderboard() {
        page = 0;
        loading = false;
        last = false;

        const {leaderboardTableBody} = Reversi.elements;
        if (leaderboardTableBody) leaderboardTableBody.innerHTML = ""; // clear old rows
    }

    function needsMoreRows() {
        const doc = document.documentElement;
        return doc.scrollHeight <= window.innerHeight + 1;
    }

    async function fillToViewport() {
        while (!last && needsMoreRows()) {
            const loaded = await loadMore();
            if (!loaded) break;
        }
    }

    async function loadMore() {
        if (loading || last) return false;
        loading = true;

        try {
            const res = await fetch(`/api/v1/leaderboard?page=${page}&size=${size}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if (!data) {
                last = true;
                return false;
            }

            appendRows(data.content || []);

            last = data.last === true || (data.content || []).length === 0;
            page += 1;
            return true;
        } catch (e) {
            console.error("Failed to load leaderboard:", e);
            last = true; // stop retry spam if endpoint fails
            return false;
        } finally {
            loading = false;
        }
    }

    function appendRows(rows) {
        const {leaderboardTableBody} = Reversi.elements;
        if (!leaderboardTableBody) {
            console.error("leaderboardTable tbody not found. Did you add it to HTML?");
            return;
        }
        for (const r of rows) {
            const tr = document.createElement("tr");
            const rankCell = document.createElement("td");
            rankCell.textContent = r.rank ?? "";
            const nameCell = document.createElement("td");
            nameCell.textContent = r.username ?? "";
            const winsCell = document.createElement("td");
            winsCell.textContent = r.wins ?? 0;
            const lossesCell = document.createElement("td");
            lossesCell.textContent = r.losses ?? 0;
            const winRateCell = document.createElement("td");
            winRateCell.textContent = r.winRate != null ? (r.winRate * 100).toFixed(1) + "%" : "";
            tr.append(rankCell, nameCell, winsCell, lossesCell, winRateCell);
            leaderboardTableBody.appendChild(tr);
        }
    }


    async function loadMyStats() {
        let res;
        try {
            res = await fetch("/api/v1/leaderboard/me", {credentials: "include"});
        } catch (e) {
            hideMyStats();
            return;
        }

        if (res.redirected && res.url.includes("/login")) {
            // not logged in
            hideMyStats();
            return;
        }

        // Logged in but no games yet
        if (res.status === 204) {
            showEmptyMyStats();
            return;
        }
        if (!res.ok) {
            hideMyStats();
            return;
        }

        const me = await res.json();

        const {myRank, myWins, myLosses, myWinRate, myStats} = Reversi.elements;
        if (!myStats || !myRank || !myWins || !myLosses || !myWinRate) {
            return;
        }
        setMyStatsMessage("");
        myRank.textContent = "#" + me.rank;
        myWins.textContent = me.wins;
        myLosses.textContent = me.losses;
        myWinRate.textContent = (me.winRate * 100).toFixed(1) + "%";
        myStats.classList.remove("hidden");
    }

    function hideMyStats() {
        const {myStats} = Reversi.elements;
        if (myStats) myStats.classList.add("hidden");
    }

    function showEmptyMyStats() {
        const {myStats} = Reversi.elements;
        if (!myStats) return;
        setMyStatsMessage("Play your first game to appear on the leaderboard!");
        myStats.classList.remove("hidden");
    }

    function setMyStatsMessage(message) {
        const {myStats} = Reversi.elements;
        if (!myStats) return;
        let messageNode = myStats.querySelector(".my-stats-message");
        if (!messageNode) {
            messageNode = document.createElement("p");
            messageNode.className = "my-stats-message";
            myStats.appendChild(messageNode);
        }
        messageNode.textContent = message;
        messageNode.classList.toggle("hidden", message.trim() === "");
    }

    window.Leaderboard = {showLeaderBoard, showMenu};
})();
