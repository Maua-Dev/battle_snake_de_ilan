package com.mauadev.code;

import java.util.List;

/**
 * Function to decide priority of which move to check firs
 
 * Up = 1
 
 * Down = 2
 
 * Right = 3
 
 * Left = 4
 */
public  class Direction {
    private int up, down, right,left;
    private int[] priority;


    public Direction() {
        up = 1;
        down = 2;
        right = 3;
        left = 4;
        priority = new int[]{0,0,0,0};
    }
    
    public int[] finalPriority(){
        int[] resp = new int[4];
        for (int i = 0; i < 4; i++) {
            int index = priority[0];
            for (int j=0;j<4;j++) {
                if(priority[j]>index){
                    index = j;
                }
            }
            switch (index) {
                case 1:
                    resp[i] = up;
                    break;
                case 2:
                    resp[i] = down;
                    break;
                case 3:
                    resp[i] = right;
                    break;
                case 4:
                    resp[i] = left;
                    break;
            }
            priority[index] = -1;
        }
        return resp;
    }

    public void setDown(int down) {
        this.down = down;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public void setUp(int up) {
        this.up = up;
    }

    public int getDown() {
        return down;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public int getUp() {
        return up;
    }

    public void setPriority(int index) {
        priority[index-1]++;
    }

    
    
    
}
