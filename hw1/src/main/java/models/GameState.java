package models;

public enum GameState {
  CONTINUE(0),
  PLAYER1WIN(1),
  PLAYER2WIN(2),
  DRAW(3);

  int gameState;

  GameState(int gameState) {
    this.gameState = gameState;
  }

  public int getGameState() {
    return gameState;
  }

  public void setGameState(int gameState) {
    this.gameState = gameState;
  }
}
