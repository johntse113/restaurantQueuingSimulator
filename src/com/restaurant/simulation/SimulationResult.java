package com.restaurant.simulation;

import java.util.*;

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

    public Map<Integer, Double> avgWaitByGroupSize;
    public Map<Integer, Double> maxWaitByGroupSize;
    public Map<Integer, Double> fairnessByGroupSize;

    public Map<Integer, Double> serviceLevelByMinute;

    public int simulationEndTime;

    public void print() {
        System.out.println("┌──────────────────────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %-81s│%n", "Algorithm   : " + algorithmName);
        System.out.printf("│ %-81s│%n", "Restaurant  : " + restaurantName);
        System.out.printf("│ %-81s│%n", "Scenario    : " + scenarioName);
        System.out.println("├──────────────────────────────────────────────────────────────────────────────────┤");
        System.out.printf("│ %-81s│%n", String.format("Groups Seated          : %d / %d", seatedGroups, totalGroups));
        System.out.printf("│ %-81s│%n", String.format("Customers Seated       : %d / %d", seatedCustomers, totalCustomers));
        System.out.printf("│ %-81s│%n", String.format("%% Seated (groups)      : %.1f%%", percentSeatedOverall));
        System.out.printf("│ %-81s│%n", String.format("Table Utilization      : %.1f%%", tableUtilization * 100));
        System.out.printf("│ %-81s│%n", String.format("Seat Utilization       : %.1f%%", seatUtilization * 100));
        System.out.printf("│ %-81s│%n", String.format("Avg Wait Time          : %.2f min", avgWaitTime));
        System.out.printf("│ %-81s│%n", String.format("Max Wait Time          : %.0f min", maxWaitTime));
        System.out.printf("│ %-81s│%n", String.format("Avg Queue Length       : %.2f", avgQueueLength));
        System.out.printf("│ %-81s│%n", String.format("Max Queue Length       : %d", maxQueueLength));
        System.out.printf("│ %-81s│%n", String.format("Throughput             : %.4f groups/min", throughput));
        System.out.printf("│ %-81s│%n", String.format("Fairness Index (Jain)  : %.4f", fairnessIndex));
        System.out.printf("│ %-81s│%n", String.format("VIP Seated %%           : %.1f%%", vipSeatedPercent));
        System.out.printf("│ %-81s│%n", String.format("Non-VIP Seated %%       : %.1f%%", nonVipSeatedPercent));

        System.out.println("├──────────────────── By Group Size ───────────────────────────────────────────────┤");
        if (seatedByGroupSize != null) {
            for (int sz : new TreeSet<>(seatedByGroupSize.keySet())) {
                Double seated = seatedByGroupSize.get(sz);
                Double avgW = avgWaitByGroupSize != null ? avgWaitByGroupSize.get(sz) : null;
                Double maxW = maxWaitByGroupSize != null ? maxWaitByGroupSize.get(sz) : null;
                Double fair = fairnessByGroupSize != null ? fairnessByGroupSize.get(sz) : null;
                String line = String.format("  Size %-2d │ Seated: %-6s │ AvgWait: %-6s │ MaxWait: %-6s │ Fairness: %-6s",
                        sz,
                        seated != null ? String.format("%.1f%%", seated) : "N/A",
                        avgW != null ? String.format("%.1f", avgW) : "N/A",
                        maxW != null ? String.format("%.0f", maxW) : "N/A",
                        fair != null ? String.format("%.3f", fair) : "N/A");
                System.out.printf("│ %-81s│%n", line);
            }
        }

        System.out.println("├───────────────────── Service Level (% seated within N min) ──────────────────────┤");
        if (serviceLevelByMinute != null) {
            List<Integer> times = new ArrayList<>(serviceLevelByMinute.keySet());
            Collections.sort(times);
            StringBuilder sb = new StringBuilder();
            for (int t : times) {
                String entry = String.format("≤%dmin:%.0f%%  ", t, serviceLevelByMinute.get(t));
                if (sb.length() + entry.length() > 81) {
                    System.out.printf("│ %-81s│%n", sb.toString().trim());
                    sb = new StringBuilder();
                }
                sb.append(entry);
            }
            if (sb.length() > 0) {
                System.out.printf("│ %-81s│%n", sb.toString().trim());
            }
        }
        System.out.println("└──────────────────────────────────────────────────────────────────────────────────┘");
    }
}