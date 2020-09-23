package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.util.Queue;
import models.GameBoard;
import models.GameState;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;

class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;

  private static GameBoard gameBoard;

  private static final Gson gson = new Gson();

  /** Main method of the application.
   *
   * @param args Command line arguments
   */
  public static void main(final String[] args) {

    app = Javalin.create(config -> {
      config.addStaticFiles("/public");
    }).start(PORT_NUMBER);

    // Test Echo Server
    app.post("/echo", ctx -> {
      ctx.result(ctx.body());
    });

    // Hello test
    app.get("/hello", ctx -> {
      ctx.result("Hello ASE class Fall 2020.");
    });

    // Get a new game
    app.get("/newgame", ctx -> {
      ctx.redirect("tictactoe.html");
    });

    // Start a new game
    app.post("/startgame", ctx -> {
      //No need for check the option since frontend had already checked
      // Initialize Player 1
      char p1Type = ctx.formParam("type").charAt(0);
      int p1Id = 1;
      Player p1 = new Player(p1Type, p1Id);
      // Set a new Game board
      gameBoard = new GameBoard(p1);
      // gameBoard.setGameStarted(true);
      // Return the game board in JSON
      String gameBoardJson = gson.toJson(gameBoard);
      ctx.result(gameBoardJson);
    });

    // Join a game
    app.get("/joingame", ctx -> {
      // Check if there is a existing game
      try {
        Player p1 = gameBoard.getP1();
        char p1Type = p1.getType();
        // Initialize Player 2
        char p2Type = p1Type == 'X' ? 'O' : 'X';
        int p2Id = p1.getId() + 1; // =2
        Player p2 = new Player(p2Type, p2Id);
        gameBoard.setP2(p2);
        // Set game start
        gameBoard.setGameStarted(true);
        ctx.redirect("/tictactoe.html?p=2");
      } catch (NullPointerException e) {
        ctx.result("Please start a game first!");
      }
      // Send the game board JSON to all players
      sendGameBoardToAllPlayers(gson.toJson(gameBoard));
    });

    // Take a move
    app.post("/move/:playerId", ctx -> {
      int playerId = Integer.parseInt(ctx.pathParam("playerId"));
      int x = Integer.parseInt(ctx.formParam("x"));
      int y = Integer.parseInt(ctx.formParam("y"));
      // Validate the move
      Move move = null;
      if (playerId % 2 == 1) {
        move = new Move(gameBoard.getP1(), x, y);
      } else {
        move = new Move(gameBoard.getP2(), x, y);
      }
      Message msg = new Message();
      if (!isValid(move, msg)) {
        ctx.result(gson.toJson(msg));
        return;
      }
      // If is valid, update the game board
      char state = playerId % 2 == 1 ? gameBoard.getP1().getType() : gameBoard.getP2().getType();
      gameBoard.setBoardState(x, y, state);
      // Check and set game result
      GameState gameState = checkGameResult(gameBoard.getBoardState());
      if (gameState == GameState.CONTINUE) {
        gameBoard.setTurn(gameBoard.getTurn() == 1 ? 2 : 1);
      } else if (gameState == GameState.PLAYER1WIN) {
        gameBoard.setWinner(1);
        gameBoard.setGameStarted(false);
      } else if (gameState == GameState.PLAYER2WIN) {
        gameBoard.setWinner(2);
        gameBoard.setGameStarted(false);
      } else {
        gameBoard.setDraw(true);
        gameBoard.setGameStarted(false);
      }
      // Return msg and update game board view
      ctx.result(gson.toJson(msg));
      sendGameBoardToAllPlayers(gson.toJson(gameBoard));
    });

    // Web sockets - DO NOT DELETE or CHANGE
    app.ws("/gameboard", new UiWebSocket());
  }

  private static GameState checkGameResult(char[][] boardState) {
    // Check all rows
    for (int row = 0; row < boardState.length; row++) {
      if (boardState[row][0] == boardState[row][1] && boardState[row][1] == boardState[row][2]) {
        if (boardState[row][0] != '\u0000') {
          return getWinner(boardState[row][0]);
        }
      }
    }
    // Check all columns
    for (int col = 0; col < boardState[0].length; col++) {
      if (boardState[0][col] == boardState[1][col] && boardState[1][col] == boardState[2][col]) {
        if (boardState[0][col] != '\u0000') {
          return getWinner(boardState[0][col]);
        }
      }
    }
    // Check all diagonals
    if (boardState[0][0] == boardState[1][1] && boardState[1][1] == boardState[2][2]) {
      if (boardState[0][0] != '\u0000') {
        return getWinner(boardState[0][0]);
      }
    }
    if (boardState[0][2] == boardState[1][1] && boardState[1][1] == boardState[2][0]) {
      if (boardState[0][2] != '\u0000') {
        return getWinner(boardState[0][2]);
      }
    }
    // Check isDraw
    for (int i = 0; i < boardState.length; i++) {
      for (int j = 0; j < boardState[0].length; j++) {
        if (boardState[i][j] == '\u0000') {
          return GameState.CONTINUE;
        }
      }
    }
    return GameState.DRAW;
  }

  private static GameState getWinner(char type) {
    if (type == gameBoard.getP1().getType()) {
      return GameState.PLAYER1WIN;
    } else {
      return GameState.PLAYER2WIN;
    }
  }

  // Check move's validity and set message
  private static boolean isValid(Move move, Message msg) {
    // case 1: game not started yet or already ended
    if (!gameBoard.isGameStarted()) {
      if (gameBoard.getWinner() != 0 || gameBoard.isDraw()) {
        msg.setFullMessage(false, 400, "Bad Request: The game has already ended!");
        return false;
      }
      msg.setFullMessage(false, 400, "Bad Request: The game hasn't started yet!");
      return false;
    }
    // case 2: not your turn
    if (move.getPlayer().getId() % 2 != gameBoard.getTurn() % 2) {
      msg.setFullMessage(false, 400, "Bad Request: It's not your turn now!");
      return false;
    }
    // case 3: position is already token
    char state = gameBoard.getBoardState()[move.getMoveX()][move.getMoveY()];
    if (state == 'X' || state == 'Y') {
      msg.setFullMessage(false, 400, "Bad Request: This position had been token already!");
      return false;
    }
    msg.setFullMessage(true, 200, "");
    return true;
  }

  /** Send message to all players.
   *
   * @param gameBoardJson Gameboard JSON
   * @throws IOException Websocket message send IO Exception
   */
  private static void sendGameBoardToAllPlayers(final String gameBoardJson) {
    Queue<Session> sessions = UiWebSocket.getSessions();
    for (Session sessionPlayer : sessions) {
      try {
        sessionPlayer.getRemote().sendString(gameBoardJson);
      } catch (IOException e) {
        // Add logger here
      }
    }
  }

  public static void stop() {
    app.stop();
  }
}
