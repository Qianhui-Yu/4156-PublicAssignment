package models;

public class Move {

  private Player player;

  private int moveX;

  private int moveY;

  /** Constructor for Move with all parameters.
   *
   * @param player Player: The player who made this move
   * @param x int: Row position
   * @param y int: Column position
   */
  public Move(Player player, int x, int y) {
    this.player = player;
    this.moveX = x;
    this.moveY = y;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public int getMoveX() {
    return moveX;
  }

  public void setMoveX(int moveX) {
    this.moveX = moveX;
  }

  public int getMoveY() {
    return moveY;
  }

  public void setMoveY(int moveY) {
    this.moveY = moveY;
  }
}
