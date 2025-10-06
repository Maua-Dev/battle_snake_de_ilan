package com.mauadev.code;

public class GameState {
    private int turn;
    private Board board;
    private Snake you;

    public GameState() {
    }

    public GameState(int turn, Board board, Snake you) {
        this.turn = turn;
        this.board = board;
        this.you = you;
    }

    public Board getBoard() {
        return board;
    }

    public int getTurn() {
        return turn;
    }

    public Snake getYou(){
        return you;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public void setYou(Snake you) {
        this.you = you;
    }

    
}
