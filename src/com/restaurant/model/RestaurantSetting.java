package com.restaurant.model;

import java.util.ArrayList;
import java.util.List;

public class RestaurantSetting {
    private String id;
    private String name;
    private List<Table> tables;
    private int timeLimit;

    public RestaurantSetting(String id, String name, List<Table> tables, int timeLimit) {
        this.id = id;
        this.name = name;
        this.tables = tables;
        this.timeLimit = timeLimit;
    }

    public String getId()      { return id; }
    public String getName()    { return name; }
    public List<Table> getTables() { return tables; }
    public int getTimeLimit()  { return timeLimit; }

    public int getTotalSeats() {
        return tables.stream().mapToInt(Table::getCapacity).sum();
    }

    public List<Table> getCopiedTables() {
        List<Table> copied = new ArrayList<>();
        for (Table t : tables) copied.add(t.copy());
        return copied;
    }
}