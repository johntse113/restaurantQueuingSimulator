package com.restaurant.util;

import com.restaurant.simulation.SimulationResult;
import java.io.*;
import java.util.*;

public class CsvExporter {

    public static void exportAll(List<SimulationResult> results, String filePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("Algorithm,Restaurant,Scenario,TotalGroups,SeatedGroups,TotalCustomers,SeatedCustomers," +
                    "PctSeated,TableUtil%,SeatUtil%,AvgWait,MaxWait,AvgQueueLen,MaxQueueLen,Throughput,FairnessIndex," +
                    "VIPSeated%,NonVIPSeated%,SimEndTime," +
                    "BySize_Seated,BySize_AvgWait,BySize_MaxWait,BySize_Fairness,ServiceLevel");

            for (SimulationResult r : results) {
                pw.print(escape(r.algorithmName) + ",");
                pw.print(escape(r.restaurantName) + ",");
                pw.print(escape(r.scenarioName) + ",");
                pw.print(r.totalGroups + ",");
                pw.print(r.seatedGroups + ",");
                pw.print(r.totalCustomers + ",");
                pw.print(r.seatedCustomers + ",");
                pw.print(fmt(r.percentSeatedOverall) + ",");
                pw.print(fmt(r.tableUtilization * 100) + ",");
                pw.print(fmt(r.seatUtilization * 100) + ",");
                pw.print(fmt(r.avgWaitTime) + ",");
                pw.print(fmt(r.maxWaitTime) + ",");
                pw.print(fmt(r.avgQueueLength) + ",");
                pw.print(r.maxQueueLength + ",");
                pw.print(fmt(r.throughput) + ",");
                pw.print(fmt(r.fairnessIndex) + ",");
                pw.print(fmt(r.vipSeatedPercent) + ",");
                pw.print(fmt(r.nonVipSeatedPercent) + ",");
                pw.print(r.simulationEndTime + ",");

                pw.print(escape(buildSizeMap(r.seatedByGroupSize, "%")) + ",");
                pw.print(escape(buildSizeMap(r.avgWaitByGroupSize, "min")) + ",");
                pw.print(escape(buildSizeMap(r.maxWaitByGroupSize, "min")) + ",");
                pw.print(escape(buildSizeMap(r.fairnessByGroupSize, "")) + ",");
                pw.print(escape(buildServiceLevel(r.serviceLevelByMinute)));
                pw.println();
            }
        }
    }

    private static String buildSizeMap(Map<Integer, Double> map, String unit) {
        if (map == null) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Double> e : map.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("Size ").append(e.getKey()).append(": ");
            sb.append(e.getValue() != null ? String.format("%.2f%s", e.getValue(), unit) : "N/A");
        }
        return sb.toString();
    }

    private static String buildServiceLevel(Map<Integer, Double> map) {
        if (map == null) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Double> e : map.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("<=").append(e.getKey()).append("min: ").append(String.format("%.1f%%", e.getValue()));
        }
        return sb.toString();
    }

    private static String fmt(double v) { return String.format("%.4f", v); }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}