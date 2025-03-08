package com.app.myapp;

import java.util.ArrayList;

public class Edge {
    private ArrayList<Grid> trajectory =new ArrayList<>();
    private double travelTime;
    private double availableTime;
    private Grid head;
    private Grid tail;
    private int count;

    public Edge() {
        this.travelTime = 0;
        this.availableTime = 100000;
        this.count = 0;
    }

    public Edge(Edge edge){
        this.trajectory = edge.trajectory;
        this.travelTime = edge.travelTime;
        this.availableTime = edge.availableTime;
        this.head = edge.head;
        this.tail = edge.tail;
        this.count = edge.count;
    }

    public void AddTrajectory(Grid grid) {
        trajectory.add(grid);
        this.count++;
    }

    public ArrayList<Grid> getTrajectory() {
        return trajectory;
    }

    public int getCount() {
        return count;
    }

    public double getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(double avalibleTime) {
        this.availableTime = avalibleTime;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(double travelTime) {
        this.travelTime = travelTime;
    }

    public void addTravelTime(double travelTime) {
        this.travelTime += travelTime;
    }

    public Grid getHead() {
        return head;
    }

    public void setHead(Grid head) {
        this.head = head;
    }

    public Grid getTail() {
        return tail;
    }

    public void setTail(Grid tail) {
        this.tail = tail;
    }

}