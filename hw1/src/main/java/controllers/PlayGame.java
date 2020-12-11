package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import models.GameBoard;
import models.GameState;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;

public class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;

  private static GameBoard gameBoard;

  private static final Gson gson = new Gson();

  private static Connection connection;

  private static Statement stmt;

  private static ResultSet res;

  /** Main method of the application.
   *
   * @param args Command line arguments
   */
  public static void main(final String[] args) throws SQLException {
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:tictactoe.db");
      connection.setAutoCommit(false);
      stmt = connection.createStatement();
      System.out.println("Opened database successfully");

      // Create game_history Table if not exists
      String sql = "CREATE TABLE IF NOT EXISTS GAMEHISTORY"
                    + "(moveId INT PRIMARY KEY NOT NULL,"
                    + " moveType VARCHAR(1) NOT NULL,"
                    + " playerId INT NOT NULL,"
                    + " moveX INT NOT NULL,"
                    + " moveY INT NOT NULL,"
                    + " gameStarted INT NOT NULL,"
                    + " isDraw INT NOT NULL,"
                    + " winner INT NOT NULL)";
      stmt.executeUpdate(sql);
      connection.commit();

      // Check if need to reload game history
      sql = "SELECT COUNT(*) AS count FROM GAMEHISTORY;";
      res = stmt.executeQuery(sql);
      int moveCount = res.getInt("count");
      if (gameBoard == null && moveCount > 0) {
        // reload players and all moves history
        // moveCount >= 1: reboot the whole game with p1 and p2 and all moves
        sql = "SELECT * FROM GAMEHISTORY;";
        res = stmt.executeQuery(sql);
        while (res.next()) {
          int moveId = res.getInt("moveId");
          if (moveId == 0) {
            int p1Id = res.getInt("playerId");
            char p1Type = res.getString("moveType").charAt(0);
            int gameStarted = res.getInt("gameStarted");
            if (gameStarted == 0 && p1Type == 'N' && p1Id == 0) {
              gameBoard = new GameBoard();
            } else if (gameStarted == 0) {
              // reload player1
              gameBoard = new GameBoard(new Player(p1Type, p1Id));
              System.out.println("Successfully reload player1.");
            } else {
              // reload player1 and player2 and set the game as started
              int p2Id = p1Id + 1;
              char p2Type = p1Type == 'X' ? 'O' : 'X';
              // Set the game as started at the same time
              gameBoard = new GameBoard(new Player(p1Type, p1Id), new Player(p2Type, p2Id));
              System.out.println("Successfully restart the game with two players.");
            }
          } else {
            // moveId > 0: reload the move and set turn for the next move
            char type = res.getString("moveType").charAt(0);
            int x = res.getInt("moveX");
            int y = res.getInt("moveY");
            gameBoard.setBoardState(x, y, type);
            System.out.println("Successfully reload the moveId = " + moveId);
            int isDraw = res.getInt("isDraw");
            int winner = res.getInt("winner");
            if (isDraw == 1) {
              gameBoard.setDraw(true);
              gameBoard.setGameStarted(false);
            } else if (winner != 0) {
              gameBoard.setWinner(winner);
              gameBoard.setGameStarted(false);
            } else {
              gameBoard.setTurn(moveId % 2 + 1);
            }
          }
        }
        System.out.println("Game reload successfully");
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      res.close();
      stmt.close();
      connection.close();
      System.exit(0);
    } finally {
      res.close();
      stmt.close();
      connection.close();
    }

    app = Javalin.create(config -> {
      config.addStaticFiles("/public");
    }).start(PORT_NUMBER);

    // Test Echo Server
    app.post("/echo", ctx -> {
      ctx.result(ctx.body());
    });

    // Get a new game
    app.get("/newgame", ctx -> {
      try {
        // Connect to DB
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:tictactoe.db");
        System.out.println("Opened database successfully");
        connection.setAutoCommit(false);
        stmt = connection.createStatement();
        // Clean table for the new game
        String sql = "DELETE FROM GAMEHISTORY;";
        stmt.executeUpdate(sql);
        System.out.println("Old game history deleted successfully.");
        // Insert the initial row for the new game
        insertGameHistory(stmt, 0, 'N', 0, -1, -1, 0, 0, 0);
        System.out.println("New game is ready but waiting for player1 to start.");
        // response
        ctx.redirect("tictactoe.html");
        connection.commit();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        stmt.close();
        connection.close();
        System.exit(0);
      } finally {
        stmt.close();
        connection.close();
      }
    });

    // Start a new game
    app.post("/startgame", ctx -> {
      try {
        // Connect to DB
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:tictactoe.db");
        System.out.println("Opened database successfully");

        // Clean the old game history
        connection.setAutoCommit(false);
        stmt = connection.createStatement();
        String sql = "DELETE from GAMEHISTORY;";
        stmt.executeUpdate(sql);
        //connection.commit();
        System.out.println("Old game history deleted successfully.");

        // No need for check the option since frontend had already checked
        // Initialize Player 1
        char p1Type = ctx.formParam("type").charAt(0);
        int p1Id = 1;
        Player p1 = new Player(p1Type, p1Id);
        // Set a new Game board
        gameBoard = new GameBoard(p1);

        // Insert the initial row for the new game to store player1's info
        insertGameHistory(stmt, 0, p1Type, p1Id, -1, -1, 0, 0, 0);
        System.out.println("Stored player1's info in DB.");

        // Return the game board in JSON
        String gameBoardJson = gson.toJson(gameBoard);
        ctx.result(gameBoardJson);
        // commit all transactions and close
        connection.commit();
      } catch (Exception e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        stmt.close();
        connection.close();
        System.exit(0);
      } finally {
        stmt.close();
        connection.close();
      }
    });

    // Join a game
    app.get("/joingame", ctx -> {
      try {
        if (gameBoard.getP2() != null) {
          ctx.result("Don't join the same game again.");
          return;
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:tictactoe.db");
        System.out.println("Opened database successfully");
        connection.setAutoCommit(false);

        // Check if there is a existing game
        Player p1 = gameBoard.getP1();
        char p1Type = p1.getType();
        // Initialize Player 2
        char p2Type = p1Type == 'X' ? 'O' : 'X';
        int p2Id = p1.getId() + 1; // =2
        Player p2 = new Player(p2Type, p2Id);
        gameBoard.setP2(p2);
        // Set game start
        gameBoard.setGameStarted(true);
        // Update the game history that game started
        String sql = "UPDATE GAMEHISTORY set gameStarted = 1 where moveId = 0;";
        stmt = connection.createStatement();
        stmt.executeUpdate(sql);

        ctx.redirect("/tictactoe.html?p=2");
        // commit and close the db connection
        connection.commit();
      } catch (NullPointerException e1) {
        ctx.result("Please start a game first!");
      } catch (Exception e2) {
        System.err.println(e2.getClass().getName() + ": " + e2.getMessage());
        stmt.close();
        connection.close();
        System.exit(0);
      } finally {
        stmt.close();
        connection.close();
      }

      // Send the game board JSON to all players
      sendGameBoardToAllPlayers(gson.toJson(gameBoard));
    });

    // Take a move
    app.post("/move/:playerId", ctx -> {
      try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:tictactoe.db");
        System.out.println("Opened database successfully");
        stmt = connection.createStatement();
        connection.setAutoCommit(false);

        String sql = "SELECT COUNT(*) AS count FROM GAMEHISTORY;";
        res = stmt.executeQuery(sql);
        int moveCount = res.getInt("count");

        // After reloading, Deal with the current move
        int playerId = Integer.parseInt(ctx.pathParam("playerId"));
        int x = Integer.parseInt(ctx.formParam("x"));
        int y = Integer.parseInt(ctx.formParam("y"));
        // Validate the move
        Move move = null;
        if (playerId % 2 != 0) {
          move = new Move(gameBoard.getP1(), x, y);
        } else {
          move = new Move(gameBoard.getP2(), x, y);
        }
        Message msg = new Message();
        if (!isValid(move, msg)) {
          ctx.result(gson.toJson(msg));
          res.close();
          stmt.close();
          connection.close();
          return;
        }
        // If is valid, update the game board
        char type = playerId % 2 != 0 ? gameBoard.getP1().getType() : gameBoard.getP2().getType();
        gameBoard.setBoardState(x, y, type);
        // Check and set game result
        GameState gameState = checkGameResult(gameBoard.getBoardState());
        if (gameState == GameState.CONTINUE) {
          gameBoard.setTurn(gameBoard.getTurn() == 1 ? 2 : 1);
          insertGameHistory(stmt, moveCount, type, playerId, x, y, 1, 0, 0);
        } else if (gameState == GameState.PLAYER1WIN) {
          gameBoard.setWinner(1);
          gameBoard.setGameStarted(false);
          insertGameHistory(stmt, moveCount, type, playerId, x, y, 0, 0, 1);
        } else if (gameState == GameState.PLAYER2WIN) {
          gameBoard.setWinner(2);
          gameBoard.setGameStarted(false);
          insertGameHistory(stmt, moveCount, type, playerId, x, y, 0, 0, 2);
        } else if (gameState == GameState.DRAW) {
          gameBoard.setDraw(true);
          gameBoard.setGameStarted(false);
          insertGameHistory(stmt, moveCount, type, playerId, x, y, 0, 1, 0);
        }

        // Return msg and update game board view
        ctx.result(gson.toJson(msg));
        sendGameBoardToAllPlayers(gson.toJson(gameBoard));
        // Commit and Close the db connection
        connection.commit();
      } catch (Exception e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        res.close();
        stmt.close();
        connection.close();
        System.exit(0);
      } finally {
        res.close();
        stmt.close();
        connection.close();
      }
    });

    // Get Game Board
    app.get("/gameboard", ctx -> {
      // Return the game board in JSON
      String gameBoardJson = gson.toJson(gameBoard);
      ctx.result(gameBoardJson);
    });

    // Web sockets - DO NOT DELETE or CHANGE
    app.ws("/gameboard", new UiWebSocket());
  }

  // Insert a history row in GAMEHISTORY
  private static void insertGameHistory(Statement stmt,
                                        int moveId, char type, int playerId, int x, int y,
                                        int gameStarted, int isDraw, int winner)
                                        throws SQLException {
    String sql = "INSERT INTO GAMEHISTORY "
        + "(moveId, moveType, playerId, moveX, moveY, gameStarted, isDraw, winner) "
        + String.format("VALUES (%d, '%s', %d, %d, %d, %d, %d, %d);",
                        moveId, type, playerId, x, y, gameStarted, isDraw, winner);
    stmt.executeUpdate(sql);
    //connection.commit();
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
