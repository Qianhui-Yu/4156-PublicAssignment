package models;

public class GameBoard {

  private Player p1;

  private Player p2;

  private boolean gameStarted;

  private int turn;

  private char[][] boardState;

  private int winner;

  private boolean isDraw;

  /** Construct and initialize the GameBoard with Player1.
   *
   * @param p1 Player
   */
  public GameBoard(Player p1) {
    this.p1 = p1;
    this.gameStarted = false;
    this.turn = 1;
    this.boardState = new char[3][3];
    this.winner = 0;
    this.isDraw = false;
  }

  public Player getP1() {
    return p1;
  }

  public void setP1(Player p1) {
    this.p1 = p1;
  }

  public Player getP2() {
    return p2;
  }

  public void setP2(Player p2) {
    this.p2 = p2;
  }

  public boolean isGameStarted() {
    return gameStarted;
  }

  public void setGameStarted(boolean gameStarted) {
    this.gameStarted = gameStarted;
  }

  public int getTurn() {
    return turn;
  }

  public void setTurn(int turn) {
    this.turn = turn;
  }

  public char[][] getBoardState() {
    return boardState;
  }

  public void setBoardState(int x, int y, char state) {
    this.boardState[x][y] = state;
  }

  public int getWinner() {
    return winner;
  }

  public void setWinner(int winner) {
    this.winner = winner;
  }

  public boolean isDraw() {
    return isDraw;
  }

  public void setDraw(boolean draw) {
    isDraw = draw;
  }
}
