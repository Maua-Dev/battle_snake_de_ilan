package com.mauadev.code;

import java.util.Objects;

public class HeadEntity {
    private int type; // head or taill
    private HeadEntity node; // point to head or tail
    private Coordinate position;
    private int length;

    /**
     * For head
     */
    public HeadEntity(int type, Coordinate position, int length) {
        this.type = type;
        this.node = null;
        this.position = position;
        this.length = length;
    }

    /**
     * For tail
     */
    public HeadEntity(int type, Coordinate position) {
        this.type = type;
        this.node = null;
        this.position = position;

    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setNode(HeadEntity node) {
        this.node = node;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setPosition(Coordinate position){
        this.position = position;
    }

    public int getLength() {
        return length;
    }

    public HeadEntity getNode() {
        return node;
    }

    public int getType() {
        return type;
    }

    public Coordinate getPosition() {
        return position;
    }

    public int getX() {
        return position.getX();
    }
    
    public int getY() {
        return position.getY();
    }

     @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HeadEntity that = (HeadEntity) obj;
        return getX() == that.getX() && getY() == that.getY();
    }
    
    @Override
    public int hashCode(){
        return Objects.hash(getX(),getY());
    }

    @Override
    public String toString(){
        return "("+getX()+","+getY()+")";
    }
}
