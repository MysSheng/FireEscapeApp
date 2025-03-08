package com.app.myapp;

import java.util.HashMap;
import java.util.Map;

public class Room {
    private int ID;
    private int pivot_X;
    private int pivot_Y;
    private int rows;
    private int cols;
    private Grid[][] gridMap;
    private HashMap<Grid,Grid[][]> navigators = new HashMap<>();

    public Room(int ID, int pivot_X, int pivot_Y, int rows, int cols) {
        this.ID = ID;
        this.pivot_X = pivot_X;
        this.pivot_Y = pivot_Y;
        this.rows = rows;
        this.cols = cols;
        this.gridMap = new Grid[rows][cols];
    }

    public int getID() {
        return ID;
    }

    public int getPivot_X() {
        return pivot_X;
    }

    public int getPivot_Y() {
        return pivot_Y;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Grid[][] getGridMap() {
        return gridMap;
    }

    public void setGridMap(Grid[][] gridMap) {
        this.gridMap = gridMap;
    }

    public HashMap<Grid,Grid[][]> getNavigators() {
        return navigators;
    }

    public void setNavigators(HashMap<Grid,Grid[][]> navigators){
        this.navigators=navigators;
    }
}
