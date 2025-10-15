# Othello Reversi Game

This project is a web-based implementation of the classic board game **Othello (Reversi)**. It features a Spring Boot backend with REST endpoints and a simple HTML/CSS/JavaScript frontend. The game supports **Player vs. Computer**, **Player vs. Player**, and real-time **Matchmaking** for quick pairing.

<p align="center">
  <img src="images/screenshot.png" alt="Othello App Screenshot">
</p>


## Features

- **Game Setup:** Create, join, or find a game session using the in-app menu.
- **Matchmaking Queue:** Use the *Find Match* option to enter the queue, automatically pair with another player, and receive an assigned color via WebSocket updates.
- **Valid Moves Highlighting:** The frontend calls a backend endpoint to fetch and highlight valid moves.
- **Turn Management:** The backend automatically passes the turn when no valid moves exist.
- **Websockets & State Updates:** The server and client communicate via Websockets for updated game state for multiplayer scenarios.
- **Extensible Design:** Built to easily support future enhancements.

## Technologies

- **Backend:** Spring Boot, Java , Websockets
- **Frontend:** HTML, CSS, and vanilla JavaScript
- **Build Tool:** Maven

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- A modern web browser (Chrome, Firefox, etc.)

### Running the Backend

1. Clone the repository.
2. Navigate to the project directory.
3. Build the project with Maven:
```bash
  mvn clean install
```
4. Run the application :
```bash
  mvn spring-boot:run
```
The backend will start on http://localhost:8080. 

### Running the Frontend
Once the backend is running, open your browser and navigate to:
```bash
http://localhost:8080/index.html

```

## How to Play

- **Player vs Computer:** Choose *New Game* → *Player vs Computer* to start immediately as White against the AI.
- **Player vs Player (Manual):** Choose *New Game* → *Player vs Player*. Share the displayed Session ID with a friend; they can join via *Join Game* using that ID.
- **Matchmaking:** Click *Find Match*, optionally set a nickname and preferred color, and wait to be paired. When a match is found, both players are redirected into the game automatically.

During a match the scoreboard displays each player’s nickname, color, and turn indicator in real time. Use the *Copy* button next to the Session ID to quickly share your game link when playing manually.
