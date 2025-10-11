package com.mauadev.code;

public class Node {
    private Coordinate coord; 
    private int priority; 

    Node(Coordinate coord, int priority) { 
            this.coord = coord; 
            this.priority = priority; 
        } 

    public Coordinate getCoord() {
        return coord;
    }

    public int getPriority() {
        return priority;
    }

    public void setCoord(Coordinate coord) {
        this.coord = coord;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    
}
