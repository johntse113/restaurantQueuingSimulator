package com.restaurant.algorithm;

import com.restaurant.model.CustomerGroup;
import com.restaurant.model.Table;
import com.restaurant.simulation.SimulationStep;
import java.util.List;

public interface QueueAlgorithm {
    String getName();
    List<SimulationStep> simulate(List<Table> tables, List<CustomerGroup> groups);
}