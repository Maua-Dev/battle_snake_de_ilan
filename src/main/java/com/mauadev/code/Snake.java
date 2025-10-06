package com.mauadev.code;

import java.util.List;

public class Snake {
    private String id;
    private String name;
    private int health;
    private List<Coordinate> body;
    private Coordinate head;
    private int lenght;
    private String shout;

    public Snake() {
    }

    public Snake(String id, String name, int health, List<Coordinate> body, Coordinate head, int lenght, String shout) {
        this.id = id;
        this.name = name;
        this.health = health;
        this.body = body;
        this.head = head;
        this.lenght = lenght;
        this.shout = shout;
    }

    public List<Coordinate> getBody() {
        return body;
    }

    public Coordinate getHead() {
        return head;
    }

    public int getHealth() {
        return health;
    }

    public String getId() {
        return id;
    }

    public int getLenght() {
        return lenght;
    }

    public String getName() {
        return name;
    }

    public String getShout() {
        return shout;
    }

    public void setBody(List<Coordinate> body) {
        this.body = body;
    }

    public void setHead(Coordinate head) {
        this.head = head;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLenght(int lenght) {
        this.lenght = lenght;
    }

    public void setName(String name) {
        this.name = name;
    }
}
