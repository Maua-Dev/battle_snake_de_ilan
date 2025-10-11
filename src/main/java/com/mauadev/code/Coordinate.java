package com.mauadev.code;

import java.util.Objects;

public class Coordinate {
    private int x;
    private int y;

    public Coordinate(){

    }

    public Coordinate(String x, String y){
        this.x = Integer.parseInt(x);
        this.y = Integer.parseInt(y);

    }

    public Coordinate(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate that = (Coordinate) obj;
        return x == that.x && y == that.y;
    }
    
    @Override
    public int hashCode(){
        return Objects.hash(x,y);
    }

    @Override
    public String toString(){
        return "("+x+","+y+")";
    }
}
