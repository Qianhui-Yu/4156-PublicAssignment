import com.google.gson.Gson;
import controllers.PlayGame;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import models.GameBoard;
import models.Player;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class GameTest {
  /**
   * Runs only once before the testing starts.
   */
  @BeforeAll
  public static void init() throws SQLException {
    // Start Server
    PlayGame.main(null);
    System.out.println("Before All");
  }

  /**
   * This method starts a new game before every test run. It will run every time before a test.
   */
  @BeforeEach
  public void startNewGame() {
    // Test if server is running. You need to have an endpoint /
    // If you do not wish to have this end point, it is okay to not have anything in this method.
    HttpResponse response = Unirest.get("http://localhost:8080/").asString();
    int restStatus = response.getStatus();
    //assertEquals(200, restStatus);
    System.out.println("Before Each " + restStatus);
  }

  /**
   * This is a test case to evaluate the newgame endpoint.
   */
  @Test
  @Order(1)
  public void newGameTest() {

    // Create HTTP request and get response
    HttpResponse response = Unirest.get("http://localhost:8080/newgame").asString();
    int restStatus = response.getStatus();

    // Check assert statement (New Game has started)
    assertEquals(restStatus, 200);
    System.out.println("Test New Game");
  }

  /**
   * This is a test case to evaluate the startgame endpoint.
   */
  @Test
  @Order(2)
  public void startGameTest() {

    // Create a POST request to startgame endpoint and get the body
    // Remember to use asString() only once for an endpoint call. Every time you call asString(), a new request will be sent to the endpoint. Call it once and then use the data in the object.
    HttpResponse response = Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    String responseBody = (String) response.getBody();

    // --------------------------- JSONObject Parsing ----------------------------------

    System.out.println("Start Game Response: " + responseBody);

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);

    // Check if game started after player 1 joins: Game should not start at this point
    assertEquals(false, jsonObject.get("gameStarted"));

    // ---------------------------- GSON Parsing -------------------------

    // GSON use to parse data to object
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player player1 = gameBoard.getP1();

    // Check if player type is correct
    assertEquals('X', player1.getType());

    System.out.println("Test Start Game");
  }

  @Test
  @Order(3)
  public void player1MoveBeforePlayer2JoinTest() {
    // Create a POST request for Player1 to take a move before Player2 joining
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    String responseBody = (String) response.getBody();
    // --------------------------- JSONObject Parsing ----------------------------------
    System.out.println("Player1 Move Before Player2 Join Response: " + responseBody);
    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be Not Valid, Player1 can't move before Player2 joining
    // Code should be 400
    assertEquals(false, jsonObject.get("moveValidity"));
    assertEquals(400, jsonObject.get("code"));
  }

  @Test
  @Order(4)
  public void joinGameTest() {
    // Create a GET request to the /joingame endpoint and get the response
    HttpResponse response = Unirest.get("http://localhost:8080/joingame").asString();
    int restStatus = response.getStatus();
    System.out.println("Join Game Test: code = " + restStatus);
    // Check if redirect successfully
    assertEquals(restStatus, 200);

    // Check if game started after player2 joined the game: Game should started at this point
    // Create a GET request to /gameboard endpoint to get the game board
    HttpResponse gameBoardResponse = Unirest.get("http://localhost:8080/gameboard").asString();
    // Parse response string to JSON
    String gameBoardResponseBody = (String) gameBoardResponse.getBody();
    System.out.println("Join Game Response: " + gameBoardResponseBody);
    JSONObject jsonObject = new JSONObject(gameBoardResponseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
  }

  @Test
  @Order(5)
  public void player2TakeFirstMoveTest() {
    HttpResponse response = Unirest.post("http://localhost:8080/move/2").body("x=0&y=0").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be inValid, Player2 can't move first
    // Code should be 400
    assertEquals(false, jsonObject.get("moveValidity"));
    assertEquals(400, jsonObject.get("code"));
  }

  @Test
  @Order(6)
  public void player1TakeFirstMoveTest() {
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be Valid, Player1 always move first
    // Code should be 200
    assertEquals(true, jsonObject.get("moveValidity"));
    assertEquals(200, jsonObject.get("code"));
  }

  @Test
  @Order(7)
  public void player1TakeTwoMovesInOneTurnTest() {
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be inValid, Player can't move twice in one turn
    // Code should be 400
    assertEquals(false, jsonObject.get("moveValidity"));
    assertEquals(400, jsonObject.get("code"));

  }

  @Test
  @Order(8)
  public void player2TakeSecondMoveTest() {
    HttpResponse response = Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be Valid, Player2 can move in second turn
    // Code should be 200
    assertEquals(true, jsonObject.get("moveValidity"));
    assertEquals(200, jsonObject.get("code"));
    // Create a GET request to /gameboard endpoint to get the game board
    HttpResponse gameBoardResponse = Unirest.get("http://localhost:8080/gameboard").asString();
    // Parse response string to JSON
    String gameBoardResponseBody = (String) gameBoardResponse.getBody();
    JSONObject gameBoardJsonObject = new JSONObject(gameBoardResponseBody);
    // GSON use to parse data to object
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(gameBoardJsonObject.toString(), GameBoard.class);
    Player player2 = gameBoard.getP2();
    // Check if player2 type is correct
    assertEquals('O', player2.getType());
  }

  @Test
  @Order(9)
  public void moveAlreadyBeenTakenTest() {
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be inValid, Player can't take a move on the place that already been taken
    // Code should be 400
    assertEquals(false, jsonObject.get("moveValidity"));
    assertEquals(400, jsonObject.get("code"));
  }

  @Test
  @Order(10)
  public void gameWinTest() {
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();
    // Create a GET request to /gameboard endpoint to get the game board
    HttpResponse gameBoardResponse = Unirest.get("http://localhost:8080/gameboard").asString();
    // Parse response string to JSON
    String gameBoardResponseBody = (String) gameBoardResponse.getBody();
    JSONObject jsonObject = new JSONObject(gameBoardResponseBody);
    assertEquals(1, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("gameStarted"));
  }

  @Test
  @Order(10)
  public void moveAfterGameEndsTest() {
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    // Check this move's validity: Should be inValid, Player can't move after the game ends
    // Code should be 400
    assertEquals(false, jsonObject.get("moveValidity"));
    assertEquals(400, jsonObject.get("code"));
  }

  @Test
  @Order(11)
  public void gameDrawTest() {
    // Start a new game
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();

    // Create a GET request to /gameboard endpoint to get the game board
    HttpResponse gameBoardResponse = Unirest.get("http://localhost:8080/gameboard").asString();
    // Parse response string to JSON
    String gameBoardResponseBody = (String) gameBoardResponse.getBody();
    System.out.println("Game is Draw Response: " + gameBoardResponseBody);
    JSONObject jsonObject = new JSONObject(gameBoardResponseBody);
    assertEquals(true, jsonObject.get("isDraw"));
    assertEquals(false, jsonObject.get("gameStarted"));
  }


  /**
   * This will run every time after a test has finished.
   */
  @AfterEach
  public void finishGame() {
    System.out.println("After Each");
  }

  /**
   * This method runs only once after all the test cases have been executed.
   */
  @AfterAll
  public static void close() {
    // Stop Server
    PlayGame.stop();
    System.out.println("After All");
  }
}

