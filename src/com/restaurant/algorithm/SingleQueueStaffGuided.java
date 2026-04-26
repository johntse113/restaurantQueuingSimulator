package com.restaurant.algorithm;

import com.restaurant.model.CustomerGroup;
import com.restaurant.model.Table;
import com.restaurant.simulation.SimulationStep;

import java.util.*;

public class SingleQueueStaffGuided implements QueueAlgorithm {

    @Override
    public String getName() { return "Single Queue (Staff-Guided)"; }

    @Override
    public List<SimulationStep> simulate(List<Table> tables, List<CustomerGroup> groups) {
        List<SimulationStep> steps = new ArrayList<>();
        List<CustomerGroup> queue = new LinkedList<>();
        List<CustomerGroup> pending = new ArrayList<>(groups);
        pending.sort(Comparator.comparingInt(CustomerGroup::getArrivalTime));

        Set<Integer> allTimes = new TreeSet<>();
        for (CustomerGroup g : pending) allTimes.add(g.getArrivalTime());
        int maxTime = pending.stream().mapToInt(g -> g.getArrivalTime() + g.getDiningDuration() + 60).max().orElse(200);
        for (int t = 0; t <= maxTime; t++) allTimes.add(t);

        for (int time : allTimes) {
            for (Table table : tables) {
                if (table.isOccupied() && table.getFreeAtTime() <= time) {
                    CustomerGroup prev = table.getCurrentGroup();
                    if (prev != null) prev.depart();
                    table.free();
                    steps.add(new SimulationStep(time, "Table " + table.getTableId() + " freed (Group " + (prev != null ? prev.getGroupId() : "?") + " departed)",
                            snapshotTables(tables), snapshotQueue(queue), null, null, table));
                }
            }

            List<CustomerGroup> arriving = new ArrayList<>();
            Iterator<CustomerGroup> it = pending.iterator();
            while (it.hasNext()) {
                CustomerGroup g = it.next();
                if (g.getArrivalTime() <= time) { arriving.add(g); it.remove(); }
            }
            for (CustomerGroup g : arriving) {
                queue.add(g);
                steps.add(new SimulationStep(time, "Group " + g.getGroupId() + " arrived (size=" + g.getGroupSize() + (g.isVip() ? ", VIP" : "") + ")",
                        snapshotTables(tables), snapshotQueue(queue), null, g, null));
            }

            boolean assigned;
            do {
                assigned = false;
                for (Iterator<CustomerGroup> qi = queue.iterator(); qi.hasNext(); ) {
                    CustomerGroup group = qi.next();
                    Table best = findBestTable(tables, group.getGroupSize());
                    if (best != null) {
                        group.seat(time);
                        best.seat(group, time);
                        qi.remove();
                        steps.add(new SimulationStep(time, "Group " + group.getGroupId() + " seated at Table " + best.getTableId(),
                                snapshotTables(tables), snapshotQueue(queue), group, null, best));
                        assigned = true;
                        break;
                    }
                }
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

    private List<Table> snapshotTables(List<Table> tables) {
        List<Table> s = new ArrayList<>();
        for (Table t : tables) s.add(t.copy());
        return s;
    }

    private List<CustomerGroup> snapshotQueue(List<CustomerGroup> q) {
        List<CustomerGroup> s = new ArrayList<>();
        for (CustomerGroup g : q) s.add(g.copy());
        return s;
    }
}