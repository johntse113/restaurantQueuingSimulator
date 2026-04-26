package com.restaurant.model;

import java.util.ArrayList;
import java.util.List;

public class CustomerScenario {
    private String id;
    private String name;
    private List<CustomerGroup> arrivals;

    public CustomerScenario(String id, String name, List<CustomerGroup> arrivals) {
        this.id = id;
        this.name = name;
        this.arrivals = arrivals;
    }

    public String getId()   { return id; }
    public String getName() { return name; }
    public List<CustomerGroup> getArrivals() { return arrivals; }

    public List<CustomerGroup> getCopiedArrivals() {
        List<CustomerGroup> copied = new ArrayList<>();
        for (CustomerGroup g : arrivals) copied.add(g.copy());
        return copied;
    }
}