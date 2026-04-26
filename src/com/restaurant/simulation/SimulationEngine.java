package com.restaurant.simulation;

import com.restaurant.algorithm.QueueAlgorithm;
import com.restaurant.model.CustomerGroup;
import com.restaurant.model.RestaurantSetting;
import com.restaurant.model.CustomerScenario;
import com.restaurant.model.Table;

import java.util.*;

public class SimulationEngine {

    public SimulationResult run(QueueAlgorithm algorithm, RestaurantSetting restaurant, CustomerScenario scenario) {
        List<Table> tables = restaurant.getCopiedTables();
        List<CustomerGroup> groups = scenario.getCopiedArrivals();

        List<SimulationStep> steps = algorithm.simulate(tables, groups);

        return computeStats(algorithm.getName(), restaurant, scenario, steps, groups, tables);
    }

    public List<SimulationStep> runWithSteps(QueueAlgorithm algorithm, RestaurantSetting restaurant, CustomerScenario scenario) {
        List<Table> tables = restaurant.getCopiedTables();
        List<CustomerGroup> groups = scenario.getCopiedArrivals();
        return algorithm.simulate(tables, groups);
    }

    private SimulationResult computeStats(String algName, RestaurantSetting restaurant,
                                           CustomerScenario scenario,
                                           List<SimulationStep> steps,
                                           List<CustomerGroup> originalGroups,
                                           List<Table> finalTables) {
        SimulationResult result = new SimulationResult();
        result.algorithmName = algName;
        result.restaurantName = restaurant.getName();
        result.scenarioName = scenario.getName();

        List<CustomerGroup> allGroups = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SimulationStep step : steps) {
            if (step.justSeated != null && !seen.contains(step.justSeated.getGroupId())) {
                allGroups.add(step.justSeated);
                seen.add(step.justSeated.getGroupId());
            }
        }

        Set<String> arrivedIds = new HashSet<>();
        for (CustomerGroup g : originalGroups) arrivedIds.add(g.getGroupId());

        result.totalGroups = originalGroups.size();
        result.seatedGroups = allGroups.size();
        result.totalCustomers = originalGroups.stream().mapToInt(CustomerGroup::getGroupSize).sum();
        result.seatedCustomers = allGroups.stream().mapToInt(CustomerGroup::getGroupSize).sum();
        result.percentSeatedOverall = result.totalGroups > 0 ? 100.0 * result.seatedGroups / result.totalGroups : 0;

        double totalWait = allGroups.stream().mapToInt(CustomerGroup::getWaitTime).sum();
        result.avgWaitTime = allGroups.isEmpty() ? 0 : totalWait / allGroups.size();
        result.maxWaitTime = allGroups.stream().mapToInt(CustomerGroup::getWaitTime).max().orElse(0);

        int maxQL = 0;
        double totalQL = 0;
        int qlCount = 0;
        for (SimulationStep step : steps) {
            int ql = step.queueSnapshot != null ? step.queueSnapshot.size() : 0;
            if (ql > maxQL) maxQL = ql;
            totalQL += ql;
            qlCount++;
        }
        result.maxQueueLength = maxQL;
        result.avgQueueLength = qlCount > 0 ? totalQL / qlCount : 0;

        int simEndTime = steps.isEmpty() ? 1 : steps.get(steps.size() - 1).time + 1;
        result.throughput = simEndTime > 0 ? (double) result.seatedGroups / simEndTime : 0;

        long totalOccupied = 0;
        int tableCount = restaurant.getTables().size();
        int totalSeats = restaurant.getTotalSeats();
        for (SimulationStep step : steps) {
            if (step.tableSnapshot != null) {
                for (Table t : step.tableSnapshot) if (t.isOccupied()) totalOccupied++;
            }
        }
        int snapshotCount = steps.size();
        result.tableUtilization = (tableCount > 0 && snapshotCount > 0) ? (double) totalOccupied / (tableCount * snapshotCount) : 0;

        long occupiedSeats = 0;
        for (SimulationStep step : steps) {
            if (step.tableSnapshot != null) {
                for (Table t : step.tableSnapshot) {
                    if (t.isOccupied() && t.getCurrentGroup() != null) {
                        occupiedSeats += t.getCurrentGroup().getGroupSize();
                    }
                }
            }
        }
        result.seatUtilization = (totalSeats > 0 && snapshotCount > 0) ? (double) occupiedSeats / (totalSeats * snapshotCount) : 0;

        Map<Integer, List<CustomerGroup>> bySize = new LinkedHashMap<>();
        for (CustomerGroup g : originalGroups) bySize.computeIfAbsent(g.getGroupSize(), k -> new ArrayList<>()).add(g);
        Set<String> seatedIds = new HashSet<>();
        for (CustomerGroup g : allGroups) seatedIds.add(g.getGroupId());

        Map<Integer, Double> seatedBySize = new TreeMap<>();
        for (Map.Entry<Integer, List<CustomerGroup>> e : bySize.entrySet()) {
            long cnt = e.getValue().stream().filter(g -> seatedIds.contains(g.getGroupId())).count();
            seatedBySize.put(e.getKey(), e.getValue().isEmpty() ? 0 : 100.0 * cnt / e.getValue().size());
        }
        result.seatedByGroupSize = seatedBySize;

        List<CustomerGroup> vipGroups = new ArrayList<>(), nonVipGroups = new ArrayList<>();
        for (CustomerGroup g : originalGroups) { if (g.isVip()) vipGroups.add(g); else nonVipGroups.add(g); }
        long vipSeated = vipGroups.stream().filter(g -> seatedIds.contains(g.getGroupId())).count();
        long nonVipSeated = nonVipGroups.stream().filter(g -> seatedIds.contains(g.getGroupId())).count();
        result.vipSeatedPercent = vipGroups.isEmpty() ? 0 : 100.0 * vipSeated / vipGroups.size();
        result.nonVipSeatedPercent = nonVipGroups.isEmpty() ? 0 : 100.0 * nonVipSeated / nonVipGroups.size();

        if (allGroups.size() > 1) {
            double sumW = 0, sumW2 = 0;
            for (CustomerGroup g : allGroups) { sumW += g.getWaitTime(); sumW2 += (double) g.getWaitTime() * g.getWaitTime(); }
            double n = allGroups.size();
            double jain = (sumW * sumW) / (n * sumW2 + 0.0001);
            result.fairnessIndex = Math.min(1.0, jain);
        } else {
            result.fairnessIndex = 1.0;
        }

        return result;
    }
}