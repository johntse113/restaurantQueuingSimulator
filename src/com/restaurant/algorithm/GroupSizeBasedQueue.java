package com.restaurant.algorithm;

import com.restaurant.model.*;
import com.restaurant.simulation.SimulationStep;
import java.util.*;

public class GroupSizeBasedQueue implements QueueAlgorithm {

    private static final int[] BAND_CEILINGS = {2, 4, 6, Integer.MAX_VALUE};

    @Override
    public String getName() { return "Group-Size Based Queue"; }

    @Override
    public List<SimulationStep> simulate(List<Table> tables, List<CustomerGroup> groups, int timeLimit) {
        List<SimulationStep> steps = new ArrayList<>();

        @SuppressWarnings("unchecked")
        LinkedList<CustomerGroup>[] queues = new LinkedList[BAND_CEILINGS.length];
        for (int i = 0; i < BAND_CEILINGS.length; i++) queues[i] = new LinkedList<>();

        List<CustomerGroup> pending = new ArrayList<>(groups);
        pending.sort(Comparator.comparingInt(CustomerGroup::getArrivalTime));

        int maxTime = pending.stream()
                .mapToInt(g -> g.getArrivalTime() + g.getDiningDuration() + 60)
                .max().orElse(200);
        Set<Integer> allTimes = new TreeSet<>();
        for (CustomerGroup g : pending) allTimes.add(g.getArrivalTime());
        for (int t = 0; t <= maxTime; t++) allTimes.add(t);

        for (int time : allTimes) {

            for (Table table : tables) {
                if (table.isOccupied() && table.getFreeAtTime() <= time) {
                    CustomerGroup prev = table.getCurrentGroup();
                    if (prev != null) prev.depart();
                    table.free();
                    steps.add(new SimulationStep(time,
                            "Table " + table.getTableId() + " freed (cap=" + table.getCapacity() + ")",
                            snapshotTables(tables), combinedQueue(queues), null, null, table));
                }
            }

            Iterator<CustomerGroup> it = pending.iterator();
            while (it.hasNext()) {
                CustomerGroup g = it.next();
                if (g.getArrivalTime() <= time) {
                    int band = getBand(g.getGroupSize());
                    queues[band].add(g);
                    it.remove();
                    steps.add(new SimulationStep(time,
                            "Group " + g.getGroupId() + " arrived (size=" + g.getGroupSize()
                                    + ") → Band " + (band + 1) + " (tables up to "
                                    + (BAND_CEILINGS[band] == Integer.MAX_VALUE ? "∞" : BAND_CEILINGS[band]) + "-seat)",
                            snapshotTables(tables), combinedQueue(queues), null, g, null));
                }
            }

            boolean assigned;
            do {
                assigned = false;
                for (int band = BAND_CEILINGS.length - 1; band >= 0; band--) {
                    if (queues[band].isEmpty()) continue;
                    for (Iterator<CustomerGroup> qi = queues[band].iterator(); qi.hasNext(); ) {
                        CustomerGroup group = qi.next();
                        Table match = findStrictTable(tables, group.getGroupSize(), band);
                        if (match != null) {
                            group.seat(time);
                            match.seat(group, time, timeLimit);
                            qi.remove();
                            steps.add(new SimulationStep(time,
                                    "Group " + group.getGroupId()
                                            + " (size=" + group.getGroupSize() + ", Band " + (band + 1) + ")"
                                            + " seated at Table " + match.getTableId()
                                            + " (cap=" + match.getCapacity() + ")"
                                            + (timeLimit > 0 ? " [limit=" + timeLimit + "min]" : ""),
                                    snapshotTables(tables), combinedQueue(queues), group, null, match));
                            assigned = true;
                            break;
                        }
                    }
                    if (assigned) break;
                }
            } while (assigned);

            boolean allEmpty = Arrays.stream(queues).allMatch(LinkedList::isEmpty);
            if (pending.isEmpty() && allEmpty && tables.stream().noneMatch(Table::isOccupied)) break;
        }
        return steps;
    }

    private Table findStrictTable(List<Table> tables, int groupSize, int band) {
        int bandCeiling = BAND_CEILINGS[band];
        Table best = null;
        for (Table t : tables) {
            if (t.isOccupied()) continue;
            int cap = t.getCapacity();
            if (cap < groupSize) continue;
            if (bandCeiling != Integer.MAX_VALUE && cap > bandCeiling) continue;
            if (best == null || cap < best.getCapacity()) best = t;
        }
        return best;
    }

    private int getBand(int size) {
        for (int i = 0; i < BAND_CEILINGS.length; i++)
            if (size <= BAND_CEILINGS[i]) return i;
        return BAND_CEILINGS.length - 1;
    }

    private List<Table> snapshotTables(List<Table> tables) {
        List<Table> s = new ArrayList<>();
        for (Table t : tables) s.add(t.copy());
        return s;
    }

    private List<CustomerGroup> combinedQueue(LinkedList<CustomerGroup>[] queues) {
        List<CustomerGroup> all = new ArrayList<>();
        for (LinkedList<CustomerGroup> q : queues)
            for (CustomerGroup g : q) all.add(g.copy());
        return all;
    }
}