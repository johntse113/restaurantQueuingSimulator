package com.restaurant.algorithm;

import com.restaurant.model.CustomerGroup;
import com.restaurant.model.Table;
import com.restaurant.simulation.SimulationStep;

import java.util.*;

public class GroupSizeBasedQueue implements QueueAlgorithm {

    private static final int[][] BANDS = {{1, 2}, {3, 4}, {5, Integer.MAX_VALUE}};

    @Override
    public String getName() { return "Group-Size Based Queue"; }

    @Override
    public List<SimulationStep> simulate(List<Table> tables, List<CustomerGroup> groups) {
        List<SimulationStep> steps = new ArrayList<>();
        @SuppressWarnings("unchecked")
        LinkedList<CustomerGroup>[] queues = new LinkedList[BANDS.length];
        for (int i = 0; i < BANDS.length; i++) queues[i] = new LinkedList<>();

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
                            snapshot(tables), combinedQueue(queues), null, null, table));
                }
            }

            Iterator<CustomerGroup> it = pending.iterator();
            while (it.hasNext()) {
                CustomerGroup g = it.next();
                if (g.getArrivalTime() <= time) {
                    int band = getBand(g.getGroupSize());
                    queues[band].add(g);
                    it.remove();
                    steps.add(new SimulationStep(time, "Group " + g.getGroupId() + " arrived → queue band " + (band + 1) + " (" + BANDS[band][0] + "-" + (BANDS[band][1] == Integer.MAX_VALUE ? "+" : BANDS[band][1]) + ")",
                            snapshot(tables), combinedQueue(queues), null, g, null));
                }
            }

            boolean assigned;
            do {
                assigned = false;
                for (Table table : tables) {
                    if (table.isOccupied()) continue;
                    CustomerGroup best = findBestGroupForTable(queues, table);
                    if (best != null) {
                        int band = getBand(best.getGroupSize());
                        queues[band].remove(best);
                        best.seat(time);
                        table.seat(best, time);
                        steps.add(new SimulationStep(time, "Group " + best.getGroupId() + " (size=" + best.getGroupSize() + ") seated at Table " + table.getTableId() + " (cap=" + table.getCapacity() + ")",
                                snapshot(tables), combinedQueue(queues), best, null, table));
                        assigned = true;
                        break;
                    }
                }
            } while (assigned);

            boolean allEmpty = true;
            for (LinkedList<CustomerGroup> q : queues) if (!q.isEmpty()) { allEmpty = false; break; }
            if (pending.isEmpty() && allEmpty && tables.stream().noneMatch(Table::isOccupied)) break;
        }
        return steps;
    }

    private CustomerGroup findBestGroupForTable(LinkedList<CustomerGroup>[] queues, Table table) {
        for (int band = BANDS.length - 1; band >= 0; band--) {
            for (CustomerGroup g : queues[band]) {
                if (g.getGroupSize() <= table.getCapacity()) return g;
            }
        }
        return null;
    }

    private int getBand(int size) {
        for (int i = 0; i < BANDS.length; i++) {
            if (size >= BANDS[i][0] && size <= BANDS[i][1]) return i;
        }
        return BANDS.length - 1;
    }

    private List<Table> snapshot(List<Table> tables) {
        List<Table> s = new ArrayList<>();
        for (Table t : tables) s.add(t.copy());
        return s;
    }

    private List<CustomerGroup> combinedQueue(LinkedList<CustomerGroup>[] queues) {
        List<CustomerGroup> all = new ArrayList<>();
        for (LinkedList<CustomerGroup> q : queues) for (CustomerGroup g : q) all.add(g.copy());
        return all;
    }
}