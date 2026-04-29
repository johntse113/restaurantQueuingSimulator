package com.restaurant.model;

public class Table {
    private String tableId;
    private int capacity;
    private boolean occupied;
    private int freeAtTime;
    private CustomerGroup currentGroup;

    public Table(String tableId, int capacity) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.occupied = false;
        this.freeAtTime = 0;
        this.currentGroup = null;
    }

    public String getTableId() { return tableId; }
    public int getCapacity() { return capacity; }
    public boolean isOccupied() { return occupied; }
    public int getFreeAtTime() { return freeAtTime; }
    public CustomerGroup getCurrentGroup() { return currentGroup; }

    public void seat(CustomerGroup group, int currentTime, int timeLimit) {
        this.occupied = true;
        this.currentGroup = group;
        int rawEnd = currentTime + group.getDiningDuration();
        if (timeLimit > 0) {
            this.freeAtTime = Math.min(rawEnd, currentTime + timeLimit);
        } else {
            this.freeAtTime = rawEnd;
        }
    }

    public void free() {
        this.occupied = false;
        this.currentGroup = null;
    }

    public boolean isAvailableAt(int time) {
        return !occupied || freeAtTime <= time;
    }

    public Table copy() {
        Table t = new Table(tableId, capacity);
        t.occupied = this.occupied;
        t.freeAtTime = this.freeAtTime;
        t.currentGroup = this.currentGroup;
        return t;
    }

    @Override
    public String toString() {
        return "Table[" + tableId + ", cap=" + capacity + ", " + (occupied ? "occupied until t=" + freeAtTime : "free") + "]";
    }
}