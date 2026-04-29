package com.restaurant;

import com.restaurant.util.JsonLoader;
import java.io.*;
import java.util.*;

public class PoissonGenerator {

    private static final Random RNG = new Random();
    private static final String OUTPUT_FILE = "data/custom_scenarios.json";
    private static final int[] TABLE_SIZES = {2, 4, 6, 8};
    private static final int[] GROUP_SIZES = {1, 2, 3, 4, 5, 6};
    private static final int SIMULATION_HOURS = 2;

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        List<Map<String, Object>> scenarios = loadExistingScenarios();

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   Poisson Customer Scenario Generator            ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        while (true) {
            System.out.println("\nCurrent scenarios: " + scenarios.size());
            for (int i = 0; i < scenarios.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + scenarios.get(i).get("name"));
            }
            System.out.println("\nOptions:");
            System.out.println("  [1] Add new scenarios");
            System.out.println("  [2] Delete a scenario");
            System.out.println("  [3] Save and quit");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim().toLowerCase();

            if (choice.equals("3")) break;

            if (choice.equals("2")) {
                if (scenarios.isEmpty()) { System.out.println("No scenarios to delete."); continue; }
                System.out.print("Enter scenario number to delete: ");
                try {
                    int num = Integer.parseInt(sc.nextLine().trim());
                    if (num < 1 || num > scenarios.size()) { System.out.println("Invalid number."); continue; }
                    String name = (String) scenarios.remove(num - 1).get("name");
                    System.out.println("Deleted: " + name);
                } catch (NumberFormatException e) { System.out.println("Invalid input."); }

            } else if (choice.equals("1")) {
                System.out.print("Enter mean arrivals/hour sample values separated by commas (e.g. 30,50,40): ");
                String line = sc.nextLine().trim();
                String[] parts = line.split(",");
                List<Double> means = new ArrayList<>();
                boolean valid = true;
                for (String p : parts) {
                    try {
                        double v = Double.parseDouble(p.trim());
                        if (v <= 0) { System.out.println("All means must be > 0."); valid = false; break; }
                        means.add(v);
                    } catch (NumberFormatException e) { System.out.println("Invalid number: " + p.trim()); valid = false; break; }
                }
                if (!valid || means.isEmpty()) continue;

                System.out.print("VIP probability 0.0-1.0 (default 0.1): ");
                double vipProb = 0.1;
                try {
                    String vipStr = sc.nextLine().trim();
                    if (!vipStr.isEmpty()) vipProb = Double.parseDouble(vipStr);
                    vipProb = Math.max(0, Math.min(1, vipProb));
                } catch (NumberFormatException e) { sc.nextLine(); }

                System.out.print("Avg dining duration minutes (default 45): ");
                int avgDining = 45;
                try {
                    String ds = sc.nextLine().trim();
                    if (!ds.isEmpty()) avgDining = Integer.parseInt(ds);
                } catch (NumberFormatException e) { sc.nextLine(); }

                for (double mean : means) {
                    int nextId = scenarios.size() + 1;
                    Map<String, Object> scenario = generateScenario(nextId, mean, vipProb, avgDining);
                    scenarios.add(scenario);
                    System.out.printf("  Generated scenario %d: λ=%.1f/hr, %d groups%n",
                            nextId, mean, ((List<?>) scenario.get("arrivals")).size());
                }
            } else {
                System.out.println("Unknown option.");
            }
        }

        saveScenarios(scenarios);
        System.out.println("Saved " + scenarios.size() + " scenario(s) to " + OUTPUT_FILE);
    }

    private static double expectedGroupSize() {
        double sum = 0;
        for (int s : GROUP_SIZES) sum += s;
        return sum / GROUP_SIZES.length;
    }

    private static Map<String, Object> generateScenario(int id, double customersPerHour, double vipProb, int avgDiningMin) {
        double groupsPerHour = customersPerHour / expectedGroupSize();
        double groupsPerMin = groupsPerHour / 60.0;
        int simMinutes = SIMULATION_HOURS * 60;

        List<Map<String, Object>> arrivals = new ArrayList<>();
        int groupCounter = 1;
        int t = 0;

        while (t < simMinutes) {
            int interArrival = poissonInterArrival(groupsPerMin);
            t += Math.max(1, interArrival);
            if (t >= simMinutes) break;

            int groupSize = GROUP_SIZES[RNG.nextInt(GROUP_SIZES.length)];
            int preferredTable = TABLE_SIZES[RNG.nextInt(TABLE_SIZES.length)];
            while (preferredTable < groupSize)
                preferredTable = TABLE_SIZES[RNG.nextInt(TABLE_SIZES.length)];

            boolean isVip = RNG.nextDouble() < vipProb;
            int duration = Math.max(10, (int)(avgDiningMin * (0.5 + RNG.nextDouble())));

            Map<String, Object> arrival = new LinkedHashMap<>();
            arrival.put("groupId", "G" + groupCounter++);
            arrival.put("groupSize", groupSize);
            arrival.put("preferredTableSize", preferredTable);
            arrival.put("isVip", isVip);
            arrival.put("arrivalTime", t);
            arrival.put("diningDuration", duration);
            arrivals.add(arrival);
        }


        int totalCustomers = arrivals.stream()
                .mapToInt(a -> (int) a.get("groupSize")).sum();

        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("id", "custom_" + id);
        scenario.put("name", String.format(
                "Poisson Scenario %d (λ=%.1f customers/hr, %d groups, %d customers)",
                id, customersPerHour, arrivals.size(), totalCustomers));
        scenario.put("arrivals", arrivals);
        return scenario;
    }

    private static int poissonInterArrival(double groupsPerMin) {
        if (groupsPerMin <= 0) return 60;
        double u = RNG.nextDouble();
        if (u == 0) u = 1e-10;
        double minutes = -Math.log(u) / groupsPerMin;
        return (int) Math.max(1, Math.round(minutes));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadExistingScenarios() throws IOException {
        File f = new File(OUTPUT_FILE);
        if (!f.exists()) return new ArrayList<>();
        try {
            String raw = JsonLoader.readFile(OUTPUT_FILE);
            List<Map<String, Object>> result = new ArrayList<>();
            for (String obj : extractTopObjects(raw)) {
                Map<String, Object> m = parseObject(obj);
                if (m != null) result.add(m);
            }
            return result;
        } catch (Exception e) {
            System.out.println("Warning: could not parse existing file, start from fresh.");
            return new ArrayList<>();
        }
    }

    private static void saveScenarios(List<Map<String, Object>> scenarios) throws IOException {
        new File("data").mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT_FILE))) {
            pw.println("[");
            for (int i = 0; i < scenarios.size(); i++) {
                pw.print(scenarioToJson(scenarios.get(i), "  "));
                if (i < scenarios.size() - 1) pw.print(",");
                pw.println();
            }
            pw.println("]");
        }
    }

    @SuppressWarnings("unchecked")
    private static String scenarioToJson(Map<String, Object> scenario, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("{\n");
        sb.append(indent).append("  \"id\": \"").append(scenario.get("id")).append("\",\n");
        sb.append(indent).append("  \"name\": \"").append(scenario.get("name")).append("\",\n");
        sb.append(indent).append("  \"arrivals\": [\n");
        List<Map<String, Object>> arrivals = (List<Map<String, Object>>) scenario.get("arrivals");
        for (int i = 0; i < arrivals.size(); i++) {
            Map<String, Object> a = arrivals.get(i);
            sb.append(indent).append("    {");
            sb.append("\"groupId\":\"").append(a.get("groupId")).append("\",");
            sb.append("\"groupSize\":").append(a.get("groupSize")).append(",");
            sb.append("\"preferredTableSize\":").append(a.get("preferredTableSize")).append(",");
            sb.append("\"isVip\":").append(a.get("isVip")).append(",");
            sb.append("\"arrivalTime\":").append(a.get("arrivalTime")).append(",");
            sb.append("\"diningDuration\":").append(a.get("diningDuration")).append("}");
            if (i < arrivals.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent).append("  ]\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private static List<String> extractTopObjects(String json) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start != -1) { parts.add(json.substring(start, i+1)); start = -1; } }
        }
        return parts;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> m = new LinkedHashMap<>();
        String id = JsonLoader.extractString(json, "id");
        String name = JsonLoader.extractString(json, "name");
        if (id == null || name == null) return null;
        m.put("id", id);
        m.put("name", name);

        int arrivalsStart = json.indexOf("\"arrivals\"");
        if (arrivalsStart < 0) return null;
        int arrStart = json.indexOf('[', arrivalsStart);
        if (arrStart < 0) return null;
        List<Map<String, Object>> arrivals = new ArrayList<>();
        for (String obj : JsonLoader.extractArrayObjects(json, arrStart)) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("groupId", JsonLoader.extractString(obj, "groupId"));
            a.put("groupSize", JsonLoader.extractInt(obj, "groupSize"));
            a.put("preferredTableSize", JsonLoader.extractInt(obj, "preferredTableSize"));
            a.put("isVip", JsonLoader.extractBoolean(obj, "isVip"));
            a.put("arrivalTime", JsonLoader.extractInt(obj, "arrivalTime"));
            a.put("diningDuration", JsonLoader.extractInt(obj, "diningDuration"));
            arrivals.add(a);
        }
        m.put("arrivals", arrivals);
        return m;
    }
}