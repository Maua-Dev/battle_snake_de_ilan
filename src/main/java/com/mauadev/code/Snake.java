package com.mauadev.code;

import java.util.List;

public class Snake {
    private String id;
    private String name;
    private int health;
    private List<Coordinate> body;
    private Coordinate head;
    private int length;
    private String shout;

    public Snake() {
    }

    public Snake(String id, String name, int health, List<Coordinate> body, Coordinate head, int length, String shout) {
        this.id = id;
        this.name = name;
        this.health = health;
        this.body = body;
        this.head = head;
        this.length = length;
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

    public int getLength() {
        return length;
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

    public void setLength(int length) {
        this.length = length;
    }

    public void setName(String name) {
        this.name = name;
    }
}
