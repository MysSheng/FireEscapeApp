package com.app.myapp;

/**
 * GRID類別包含:
 * properties:
 * 1.種類:ROAD,FIRE,WALL,EXIT
 * 2.座標:x,y
 * 3.方向指引:UP,DOWN,LEFT,RIGHT
 * 4.房間編號:Room_ID
 * 因為同一個點可能對應複數個房間所以用List
 * 5.時間相關:FireTime,DeadLine
 * method:
 * 1.getter setter
 * 2.setFireTime:透過直接輸入一個座標來計算
 */


public class Grid {
    //coordinate
    private int x;
    private int y;
    //type
    private int type;
    public static final int ROAD=0;
    public static final int FIRE=1;
    public static final int WALL=2;
    public static final int EXIT=3;
    //time
    private double fireTime;
    private double deadLine;
    //direction
    private int direction;
    public static final int UP=0;
    public static final int DOWN=1;
    public static final int LEFT=2;
    public static final int RIGHT=3;
    public static final int UP_LEFT=4;
    public static final int UP_RIGHT=5;
    public static final int DOWN_LEFT=6;
    public static final int DOWN_RIGHT=7;
    //outgoing edge
    private Edge outgoingEdge=null;
    private boolean selected = false;

    public Grid(Grid b){
        this.x = b.x;
        this.y = b.y;
        this.type = b.type;
        this.fireTime = b.fireTime;
        this.deadLine = b.deadLine;
        this.direction = b.direction;
    }

    public Grid(int x, int y){
        this.x = x;
        this.y = y;
        this.fireTime = 0;
        this.deadLine = 0;
        this.direction = -1;
        this.type = ROAD;
    }

    public void setSelected(boolean value){
        this.selected = value;
    }

    public boolean isSelected() {
        return selected;
    }

    public double getDeadLine(){
        return this.deadLine;
    }

    public void setDeadLine(double deadLine){
        this.deadLine=deadLine;
    }

    public double getFireTime() {
        return this.fireTime;
    }

    public void setFireTime(int x,int y){
        //suppose the fire's speed is 1m/s
        this.fireTime=Math.sqrt(Math.pow(Math.abs(this.x-x),2)+Math.pow(Math.abs(this.y-y),2));
    }

    public int getDirection() {
        return this.direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public Edge getOutgoingEdge() {
        return this.outgoingEdge;
    }

    public void setOutgoingEdge(Edge outgoingEdge) {
        this.outgoingEdge = outgoingEdge;
    }
}

