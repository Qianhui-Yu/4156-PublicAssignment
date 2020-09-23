package models;

public class Message {

  private boolean moveValidity;

  private int code;

  private String message;

  /** Set the message's all fileds including move's validity, code, and message.
   *
   * @param moveValidity Boolean: Move's validity
   * @param code Integer: Response code: 200 OK, 400 Bad Request
   * @param message String: Response message
   */
  public void setFullMessage(boolean moveValidity, int code, String message) {
    this.moveValidity = moveValidity;
    this.code = code;
    this.message = message;
  }

  public boolean getMoveValidity() {
    return moveValidity;
  }

  public void setMoveValidity(boolean moveValidity) {
    this.moveValidity = moveValidity;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
