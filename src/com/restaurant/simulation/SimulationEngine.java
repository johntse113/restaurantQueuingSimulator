package com.restaurant.simulation;

import com.restaurant.algorithm.QueueAlgorithm;
import com.restaurant.model.*;

import java.util.*;

public class SimulationEngine {

    public SimulationResult run(QueueAlgorithm algorithm, RestaurantSetting restaurant, CustomerScenario scenario) {
        List<Table> tables = restaurant.getCopiedTables();
        List<CustomerGroup> groups = scenario.getCopiedArrivals();
        List<SimulationStep> steps = algorithm.simulate(tables, groups, restaurant.getTimeLimit());
        return computeStats(algorithm.getName(), restaurant, scenario, steps, groups);
    }

    public List<SimulationStep> runWithSteps(QueueAlgorithm algorithm, RestaurantSetting restaurant, CustomerScenario scenario) {
        List<Table> tables = restaurant.getCopiedTables();
        List<CustomerGroup> groups = scenario.getCopiedArrivals();
        return algorithm.simulate(tables, groups, restaurant.getTimeLimit());
    }

    private SimulationResult computeStats(String algName, RestaurantSetting restaurant,
                                           CustomerScenario scenario, List<SimulationStep> steps, List<CustomerGroup> originalGroups) {
        SimulationResult r = new SimulationResult();
        r.algorithmName = algName;
        r.restaurantName = restaurant.getName();
        r.scenarioName = scenario.getName();

        List<CustomerGroup> allSeated = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (SimulationStep step : steps) {
            if (step.justSeated != null && seenIds.add(step.justSeated.getGroupId()))
                allSeated.add(step.justSeated);
        }
        Set<String> seatedIds = new HashSet<>();
        for (CustomerGroup g : allSeated) seatedIds.add(g.getGroupId());

        r.totalGroups = originalGroups.size();
        r.seatedGroups= allSeated.size();
        r.totalCustomers = originalGroups.stream().mapToInt(CustomerGroup::getGroupSize).sum();
        r.seatedCustomers = allSeated.stream().mapToInt(CustomerGroup::getGroupSize).sum();
        r.percentSeatedOverall = r.totalGroups > 0 ? 100.0 * r.seatedGroups / r.totalGroups : 0;

        double totalWait = allSeated.stream().mapToInt(CustomerGroup::getWaitTime).sum();
        r.avgWaitTime = allSeated.isEmpty() ? 0 : totalWait / allSeated.size();
        r.maxWaitTime = allSeated.stream().mapToInt(CustomerGroup::getWaitTime).max().orElse(0);

        int maxQL = 0; double totalQL = 0; int qlCount = 0;
        for (SimulationStep step : steps) {
            int ql = step.queueSnapshot != null ? step.queueSnapshot.size() : 0;
            if (ql > maxQL) maxQL = ql;
            totalQL += ql; qlCount++;
        }
        r.maxQueueLength = maxQL;
        r.avgQueueLength = qlCount > 0 ? totalQL / qlCount : 0;
        int simEnd = steps.isEmpty() ? 1 : steps.get(steps.size() - 1).time + 1;
        r.simulationEndTime = simEnd;
        r.throughput = simEnd > 0 ? (double) r.seatedGroups / simEnd : 0;

        int tableCount = restaurant.getTables().size();
        int totalSeats = restaurant.getTotalSeats();
        long totalOccupied = 0; long occupiedSeats = 0;
        int snapshotCount = steps.size();
        for (SimulationStep step : steps) {
            if (step.tableSnapshot != null) {
                for (Table t : step.tableSnapshot) {
                    if (t.isOccupied()) {
                        totalOccupied++;
                        if (t.getCurrentGroup() != null)
                            occupiedSeats += t.getCurrentGroup().getGroupSize();
                    }
                }
            }
        }
        r.tableUtilization = (tableCount > 0 && snapshotCount > 0) ? (double) totalOccupied / (tableCount * snapshotCount) : 0;
        r.seatUtilization = (totalSeats > 0 && snapshotCount > 0) ? (double) occupiedSeats / (totalSeats * snapshotCount) : 0;

        Map<Integer, List<CustomerGroup>> bySize = new TreeMap<>();
        for (CustomerGroup g : originalGroups)
            bySize.computeIfAbsent(g.getGroupSize(), k -> new ArrayList<>()).add(g);

        Map<Integer, Double> seatedBySize = new TreeMap<>();
        Map<Integer, Double> avgWaitBySize = new TreeMap<>();
        Map<Integer, Double> maxWaitBySize = new TreeMap<>();
        Map<Integer, Double> fairBySize = new TreeMap<>();

        for (Map.Entry<Integer, List<CustomerGroup>> e : bySize.entrySet()) {
            int sz = e.getKey();
            List<CustomerGroup> inSize = e.getValue();
            List<CustomerGroup> seatedInSize = new ArrayList<>();
            for (CustomerGroup g : inSize)
                if (seatedIds.contains(g.getGroupId())) seatedInSize.add(g);

            seatedBySize.put(sz, inSize.isEmpty() ? 0.0 : 100.0 * seatedInSize.size() / inSize.size());

            if (!seatedInSize.isEmpty()) {
                double sw = seatedInSize.stream().mapToInt(CustomerGroup::getWaitTime).sum();
                avgWaitBySize.put(sz, sw / seatedInSize.size());
                maxWaitBySize.put(sz, (double) seatedInSize.stream().mapToInt(CustomerGroup::getWaitTime).max().orElse(0));
                if (seatedInSize.size() > 1) {
                    double sumW = sw, sumW2 = seatedInSize.stream().mapToDouble(g -> (double)g.getWaitTime()*g.getWaitTime()).sum();
                    double n = seatedInSize.size();
                    fairBySize.put(sz, Math.min(1.0, (sumW*sumW) / (n * sumW2 + 0.0001)));
                } else {
                    fairBySize.put(sz, 1.0);
                }
            } else {
                avgWaitBySize.put(sz, null);
                maxWaitBySize.put(sz, null);
                fairBySize.put(sz, null);
            }
        }
        r.seatedByGroupSize = seatedBySize;
        r.avgWaitByGroupSize = avgWaitBySize;
        r.maxWaitByGroupSize = maxWaitBySize;
        r.fairnessByGroupSize = fairBySize;

        List<CustomerGroup> vips = new ArrayList<>(), nonVips = new ArrayList<>();
        for (CustomerGroup g : originalGroups) { if (g.isVip()) vips.add(g); else nonVips.add(g); }
        long vs = vips.stream().filter(g -> seatedIds.contains(g.getGroupId())).count();
        long nvs = nonVips.stream().filter(g -> seatedIds.contains(g.getGroupId())).count();
        r.vipSeatedPercent = vips.isEmpty()? 0 : 100.0 * vs / vips.size();
        r.nonVipSeatedPercent = nonVips.isEmpty() ? 0 : 100.0 * nvs / nonVips.size();

        // Jain's fairness index for all seated groups
        if (allSeated.size() > 1) {
            double sumW = totalWait;
            double sumW2 = allSeated.stream().mapToDouble(g -> (double)g.getWaitTime()*g.getWaitTime()).sum();
            double n = allSeated.size();
            r.fairnessIndex = Math.min(1.0, (sumW*sumW) / (n * sumW2 + 0.0001));
        } else {
            r.fairnessIndex = 1.0;
        }

        Map<Integer, Double> sl = new LinkedHashMap<>();
        int step30 = 30;
        for (int t = step30; t <= simEnd + step30; t += step30) {
            int threshold = Math.min(t, simEnd);
            if (!sl.containsKey(threshold)) {
                long cnt = allSeated.stream().filter(g -> g.getWaitTime() <= threshold).count();
                sl.put(threshold, allSeated.isEmpty() ? 0.0 : 100.0 * cnt / allSeated.size());
            }
            if (threshold >= simEnd) break;
        }
        r.serviceLevelByMinute = sl;

        return r;
    }
}