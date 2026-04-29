package com.restaurant.simulation;

import com.restaurant.model.CustomerGroup;
import com.restaurant.model.Table;
import java.util.List;

public class SimulationStep {
    public int time;
    public String event;
    public List<Table> tableSnapshot;
    public List<CustomerGroup> queueSnapshot;
    public CustomerGroup justSeated;
    public CustomerGroup justArrived;
    public Table tableUsed;

    public SimulationStep(int time, String event, List<Table> tableSnapshot, List<CustomerGroup> queueSnapshot, 
                            CustomerGroup justSeated, CustomerGroup justArrived, Table tableUsed) {
        this.time = time;
        this.event = event;
        this.tableSnapshot = tableSnapshot;
        this.queueSnapshot = queueSnapshot;
        this.justSeated = justSeated;
        this.justArrived = justArrived;
        this.tableUsed = tableUsed;
    }
}