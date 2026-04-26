package com.restaurant.simulation;

import java.util.List;
import java.util.Map;

public class SimulationResult {
    public String algorithmName;
    public String restaurantName;
    public String scenarioName;
    public int totalGroups;
    public int seatedGroups;
    public int totalCustomers;
    public int seatedCustomers;
    public double tableUtilization;
    public double seatUtilization;
    public double percentSeatedOverall;
    public double avgWaitTime;
    public double maxWaitTime;
    public int maxQueueLength;
    public double avgQueueLength;
    public double throughput;
    public double fairnessIndex;
    public Map<Integer, Double> seatedByGroupSize;
    public double vipSeatedPercent;
    public double nonVipSeatedPercent;

    public void print() {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.printf("│ Algorithm   : %-47s│%n", algorithmName);
        System.out.printf("│ Restaurant  : %-47s│%n", restaurantName);
        System.out.printf("│ Scenario    : %-47s│%n", scenarioName);
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Groups Seated       : %d / %d%n", seatedGroups, totalGroups);
        System.out.printf("│ Customers Seated    : %d / %d%n", seatedCustomers, totalCustomers);
        System.out.printf("│ %% Seated (groups)   : %.1f%%%n", percentSeatedOverall);
        System.out.printf("│ Table Utilization   : %.1f%%%n", tableUtilization * 100);
        System.out.printf("│ Seat Utilization    : %.1f%%%n", seatUtilization * 100);
        System.out.printf("│ Avg Wait Time       : %.2f min%n", avgWaitTime);
        System.out.printf("│ Max Wait Time       : %.0f min%n", maxWaitTime);
        System.out.printf("│ Avg Queue Length    : %.2f%n", avgQueueLength);
        System.out.printf("│ Max Queue Length    : %d%n", maxQueueLength);
        System.out.printf("│ Throughput          : %.4f groups/min%n", throughput);
        System.out.printf("│ Fairness Index      : %.4f%n", fairnessIndex);
        if (seatedByGroupSize != null && !seatedByGroupSize.isEmpty()) {
            System.out.println("│ Seated %% by Group Size:");
            for (Map.Entry<Integer, Double> e : seatedByGroupSize.entrySet()) {
                System.out.printf("│   Size %-2d : %.1f%%%n", e.getKey(), e.getValue());
            }
        }
        System.out.printf("│ VIP Seated %%        : %.1f%%%n", vipSeatedPercent);
        System.out.printf("│ Non-VIP Seated %%    : %.1f%%%n", nonVipSeatedPercent);
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
}