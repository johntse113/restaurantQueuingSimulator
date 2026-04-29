package com.restaurant;

import com.restaurant.algorithm.*;
import com.restaurant.model.*;
import com.restaurant.simulation.*;
import com.restaurant.util.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        String restaurantFile = args.length > 0 ? args[0] : "data/restaurant_settings.json";
        String scenarioFile = args.length > 1 ? args[1] : "data/customer_scenarios.json";
        String customFile = "data/custom_scenarios.json";

        List<RestaurantSetting> restaurants;
        List<CustomerScenario> scenarios;

        try {
            restaurants = JsonLoader.loadRestaurantSettings(restaurantFile);
            scenarios = JsonLoader.loadCustomerScenarios(scenarioFile);
        } catch (Exception e) {
            System.err.println("ERROR loading JSON: " + e.getMessage());
            return;
        }

        List<CustomerScenario> custom = JsonLoader.tryLoadCustomerScenarios(customFile);
        if (!custom.isEmpty()) {
            System.out.println("Loaded " + custom.size() + " custom scenario(s) from " + customFile);
            scenarios.addAll(custom);
        }

        List<QueueAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new SingleQueueStaffGuided());
        algorithms.add(new SingleQueueSelfService());
        algorithms.add(new GroupSizeBasedQueue());
        algorithms.add(new PriorityQueueAlgorithm());

        SimulationEngine engine = new SimulationEngine();
        List<SimulationResult> allResults = new ArrayList<>();

        int total = algorithms.size() * restaurants.size() * scenarios.size();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        RESTAURANT QUEUE SIMULATION — BATCH RUNNER             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.printf("Running %d alg × %d restaurant × %d scenario = %d cases%n%n",
                algorithms.size(), restaurants.size(), scenarios.size(), total);

        int caseNum = 0;
        for (QueueAlgorithm alg : algorithms) {
            for (RestaurantSetting restaurant : restaurants) {
                for (CustomerScenario scenario : scenarios) {
                    caseNum++;
                    System.out.printf("%n═══ Case %d / %d ══%n", caseNum, total);
                    SimulationResult result = engine.run(alg, restaurant, scenario);
                    result.print();
                    allResults.add(result);
                }
            }
        }

        System.out.println("\n✔ All cases completed.");
        System.out.print("Export results to CSV? (y/n): ");
        Scanner sc = new Scanner(System.in);
        String answer = sc.nextLine().trim();
        if (answer.equalsIgnoreCase("y")) {
            System.out.print("Enter output filename (default: results.csv): ");
            String fname = sc.nextLine().trim();
            if (fname.isEmpty()) fname = "results.csv";
            try {
                CsvExporter.exportAll(allResults, fname);
                System.out.println("✔ Exported to " + fname);
            } catch (Exception ex) {
                System.err.println("Export failed: " + ex.getMessage());
            }
        }
        System.out.println("Press Enter to exit...");
        sc.nextLine();
    }
}