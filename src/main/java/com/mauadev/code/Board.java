package com.mauadev.code;

import java.util.List;

public class Board {
    private int height;
    private int widht;
    private List<Coordinate> food;
    private List<Coordinate> hazards;
    private List<Snake> snakes;

    public Board() {
    }

    public Board(int height, int widht, List<Coordinate> food, List<Coordinate> hazards, List<Snake> snakes) {
        this.height = height;
        this.widht = widht;
        this.food = food;
        this.hazards = hazards;
        this.snakes = snakes;
    }

    public List<Coordinate> getFood() {
        return food;
    }

    public List<Coordinate> getHazards() {
        return hazards;
    }

    public int getHeight(){
        return height;
    }

    public List<Snake> getSnakes() {
        return snakes;
    }

    public int getWidht() {
        return widht;
    }

    public void setFood(List<Coordinate> food) {
        this.food = food;
    }

    public void setHazards(List<Coordinate> hazards) {
        this.hazards = hazards;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSnakes(List<Snake> snakes) {
        this.snakes = snakes;
    }

    public void setWidht(int widht) {
        this.widht = widht;
    }

}
