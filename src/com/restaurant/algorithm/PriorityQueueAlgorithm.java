package com.restaurant.algorithm;

import com.restaurant.model.CustomerGroup;
import com.restaurant.model.Table;
import com.restaurant.simulation.SimulationStep;

import java.util.*;

public class PriorityQueueAlgorithm implements QueueAlgorithm {

    @Override
    public String getName() { return "Priority Queue (VIP First)"; }

    @Override
    public List<SimulationStep> simulate(List<Table> tables, List<CustomerGroup> groups) {
        List<SimulationStep> steps = new ArrayList<>();
        PriorityQueue<CustomerGroup> queue = new PriorityQueue<>(
            Comparator.<CustomerGroup, Integer>comparing(g -> g.isVip() ? 0 : 1)
                      .thenComparingInt(CustomerGroup::getArrivalTime)
        );

        List<CustomerGroup> pending = new ArrayList<>(groups);
        pending.sort(Comparator.comparingInt(CustomerGroup::getArrivalTime));

        int maxTime = pending.stream().mapToInt(g -> g.getArrivalTime() + g.getDiningDuration() + 60).max().orElse(200);
        Set<Integer> allTimes = new TreeSet<>();
        for (CustomerGroup g : pending) allTimes.add(g.getArrivalTime());
        for (int t = 0; t <= maxTime; t++) allTimes.add(t);

        for (int time : allTimes) {
            for (Table table : tables) {
                if (table.isOccupied() && table.getFreeAtTime() <= time) {
                    CustomerGroup prev = table.getCurrentGroup();
                    if (prev != null) prev.depart();
                    table.free();
                    steps.add(new SimulationStep(time, "Table " + table.getTableId() + " freed",
                            snapshot(tables), snapshot(queue), null, null, table));
                }
            }

            Iterator<CustomerGroup> it = pending.iterator();
            while (it.hasNext()) {
                CustomerGroup g = it.next();
                if (g.getArrivalTime() <= time) {
                    queue.add(g);
                    it.remove();
                    steps.add(new SimulationStep(time, "Group " + g.getGroupId() + " arrived" + (g.isVip() ? " [VIP]" : "") + " (size=" + g.getGroupSize() + ")",
                            snapshot(tables), snapshot(queue), null, g, null));
                }
            }

            boolean assigned;
            do {
                assigned = false;
                List<CustomerGroup> requeue = new ArrayList<>();
                while (!queue.isEmpty()) {
                    CustomerGroup group = queue.poll();
                    Table best = findBestTable(tables, group.getGroupSize());
                    if (best != null) {
                        group.seat(time);
                        best.seat(group, time);
                        steps.add(new SimulationStep(time, "Group " + group.getGroupId() + (group.isVip() ? " [VIP]" : "") + " seated at Table " + best.getTableId(),
                                snapshot(tables), snapshot(queue), group, null, best));
                        queue.addAll(requeue);
                        assigned = true;
                        break;
                    } else {
                        requeue.add(group);
                    }
                }
                queue.addAll(requeue);
            } while (assigned);

            if (pending.isEmpty() && queue.isEmpty() && tables.stream().noneMatch(Table::isOccupied)) break;
        }
        return steps;
    }

    private Table findBestTable(List<Table> tables, int groupSize) {
        Table best = null;
        for (Table t : tables) {
            if (!t.isOccupied() && t.getCapacity() >= groupSize) {
                if (best == null || t.getCapacity() < best.getCapacity()) best = t;
            }
        }
        return best;
    }

    private List<Table> snapshot(List<Table> tables) {
        List<Table> s = new ArrayList<>();
        for (Table t : tables) s.add(t.copy());
        return s;
    }

    private List<CustomerGroup> snapshot(java.util.PriorityQueue<CustomerGroup> q) {
        List<CustomerGroup> s = new ArrayList<>();
        for (CustomerGroup g : q) s.add(g.copy());
        return s;
    }
}