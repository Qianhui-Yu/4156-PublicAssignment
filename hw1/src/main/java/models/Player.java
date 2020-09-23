package models;

public class Player {

  private char type;

  private int id;

  public Player(char type, int id) {
    this.type = type;
    this.id = id;
  }

  public char getType() {
    return type;
  }

  public void setType(char type) {
    this.type = type;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
