package com.project.reversi.model;

import javax.persistence.*;
import java.awt.Color;

@Entity
@Table(name = "player")
public class Player {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id")
  private GameSession session;

  @Column(name = "seat_index", nullable = false)
  private int seatIndex;

  @Column(name = "color", nullable = false)
  private String colorCode;

  @Column(name = "is_computer", nullable = false)
  private boolean computer;

  private String nickName;

  private String userId;

  protected Player() {
  }

  public Player(Color color, boolean computer, String nickName, int seatIndex) {
    setColor(color);
    this.computer = computer;
    this.nickName = nickName;
    this.seatIndex = seatIndex;
  }

  public Player(Color color) {
    this(color, false, null, -1);
  }

  public Player(Color color, boolean computer) {
    this(color, computer, null, -1);
  }

  public Player(Color color, String nickName) {
    this(color, false, nickName, -1);
  }

  public Long getId() {
    return id;
  }

  public GameSession getSession() {
    return session;
  }

  public void setSession(GameSession session) {
    this.session = session;
  }

  public int getSeatIndex() {
    return seatIndex;
  }

  public void setSeatIndex(int seatIndex) {
    this.seatIndex = seatIndex;
  }

  public Color getColor() {
    if ("BLACK".equalsIgnoreCase(colorCode)) {
      return Color.BLACK;
    }
    return Color.WHITE;
  }

  public void setColor(Color color) {
    if (Color.BLACK.equals(color)) {
      this.colorCode = "BLACK";
    } else {
      this.colorCode = "WHITE";
    }
  }

  public boolean isComputer() {
    return computer;
  }

  public void setComputer(boolean computer) {
    this.computer = computer;
  }

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public String toString() {
    return (computer ? "Computer" : "Player") + "(" + colorCode + ")";
  }
}
