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
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ Algorithm   : %-51s│%n", algorithmName);
        System.out.printf("│ Restaurant  : %-51s│%n", restaurantName);
        System.out.printf("│ Scenario    : %-51s│%n", scenarioName);
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Groups Seated          : %d / %d%n", seatedGroups, totalGroups);
        System.out.printf("│ Customers Seated       : %d / %d%n", seatedCustomers, totalCustomers);
        System.out.printf("│ %% Seated (groups)      : %.1f%%%n", percentSeatedOverall);
        System.out.printf("│ Table Utilization      : %.1f%%%n", tableUtilization * 100);
        System.out.printf("│ Seat Utilization       : %.1f%%%n", seatUtilization * 100);
        System.out.printf("│ Avg Wait Time          : %.2f min%n", avgWaitTime);
        System.out.printf("│ Max Wait Time          : %.0f min%n", maxWaitTime);
        System.out.printf("│ Avg Queue Length       : %.2f%n", avgQueueLength);
        System.out.printf("│ Max Queue Length       : %d%n", maxQueueLength);
        System.out.printf("│ Throughput             : %.4f groups/min%n", throughput);
        System.out.printf("│ Fairness Index (Jain)  : %.4f%n", fairnessIndex);
        System.out.printf("│ VIP Seated %%           : %.1f%%%n", vipSeatedPercent);
        System.out.printf("│ Non-VIP Seated %%       : %.1f%%%n", nonVipSeatedPercent);

        System.out.println("├── By Group Size ────────────────────────────────────────────────┤");
        if (seatedByGroupSize != null) {
            for (int sz : new TreeSet<>(seatedByGroupSize.keySet())) {
                Double seated = seatedByGroupSize.get(sz);
                Double avgW = avgWaitByGroupSize != null ? avgWaitByGroupSize.get(sz) : null;
                Double maxW = maxWaitByGroupSize != null ? maxWaitByGroupSize.get(sz) : null;
                Double fair = fairnessByGroupSize != null ? fairnessByGroupSize.get(sz) : null;
                System.out.printf("│  Size %-2d │ Seated: %-6s │ AvgWait: %-6s │ MaxWait: %-6s │ Fairness: %-6s│%n",
                        sz,
                        seated != null ? String.format("%.1f%%", seated) : "N/A",
                        avgW != null ? String.format("%.1f", avgW) : "N/A",
                        maxW != null ? String.format("%.0f", maxW) : "N/A",
                        fair != null ? String.format("%.3f", fair) : "N/A");
            }
        }

        System.out.println("├── Service Level (% seated within N min) ───────────────────────┤");
        if (serviceLevelByMinute != null) {
            List<Integer> times = new ArrayList<>(serviceLevelByMinute.keySet());
            Collections.sort(times);
            StringBuilder sb = new StringBuilder("│  ");
            int col = 0;
            for (int t : times) {
                String entry = String.format("≤%dmin:%.0f%%  ", t, serviceLevelByMinute.get(t));
                if (col + entry.length() > 63) {
                    while (sb.length() < 66) sb.append(' ');
                    sb.append("│");
                    System.out.println(sb);
                    sb = new StringBuilder("│  ");
                    col = 2;
                }
                sb.append(entry);
                col += entry.length();
            }
            while (sb.length() < 66) sb.append(' ');
            sb.append("│");
            System.out.println(sb);
        }
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
    }
}