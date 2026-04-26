package com.restaurant.model;

import java.util.ArrayList;
import java.util.List;

public class RestaurantSetting {
    private String id;
    private String name;
    private List<Table> tables;

    public RestaurantSetting(String id, String name, List<Table> tables) {
        this.id = id;
        this.name = name;
        this.tables = tables;
    }

    public String getId()   { return id; }
    public String getName() { return name; }
    public List<Table> getTables() { return tables; }

    public int getTotalSeats() {
        return tables.stream().mapToInt(Table::getCapacity).sum();
    }

    public List<Table> getCopiedTables() {
        List<Table> copied = new ArrayList<>();
        for (Table t : tables) copied.add(t.copy());
        return copied;
    }
}