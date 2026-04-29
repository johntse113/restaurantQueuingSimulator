package com.restaurant;

import com.restaurant.algorithm.*;
import com.restaurant.model.*;
import com.restaurant.simulation.*;
import com.restaurant.util.*;

import java.util.*;

public class Main {

    private static final Scanner SC = new Scanner(System.in);

    public static void main(String[] args) {
        String restaurantFile = args.length > 0 ? args[0] : "data/restaurant_settings.json";
        String scenarioFile   = args.length > 1 ? args[1] : "data/customer_scenarios.json";
        String customFile     = "data/custom_scenarios.json";

        List<RestaurantSetting> restaurants;
        List<CustomerScenario>  scenarios;

        try {
            restaurants = JsonLoader.loadRestaurantSettings(restaurantFile);
            scenarios   = JsonLoader.loadCustomerScenarios(scenarioFile);
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

        printHeader();

        System.out.println("Run mode:");
        System.out.println("  [1] Run a single selected combination");
        System.out.println("  [2] Run all combinations");
        System.out.print("Choice: ");
        String modeInput = SC.nextLine().trim();

        if (modeInput.equals("1")) {
            runSelectiveMode(algorithms, restaurants, scenarios);
        } else if (modeInput.equals("2")) {
            runAllMode(algorithms, restaurants, scenarios);
        } else {
            System.out.println("Invalid choice. Exiting.");
        }
    }

    private static void runSelectiveMode(List<QueueAlgorithm> algorithms,
                                          List<RestaurantSetting> restaurants,
                                          List<CustomerScenario> scenarios) {
        SimulationEngine engine = new SimulationEngine();

        while (true) {
            System.out.println();

            QueueAlgorithm  alg        = pickAlgorithm(algorithms);
            RestaurantSetting restaurant = pickRestaurant(restaurants);
            CustomerScenario  scenario   = pickScenario(scenarios);

            System.out.println();
            System.out.printf("Running: [%s] × [%s] × [%s]%n",
                    alg.getName(), restaurant.getName(), scenario.getName());
            System.out.println();

            SimulationResult result = engine.run(alg, restaurant, scenario);
            result.print();

            System.out.println();
            System.out.println("Options:");
            System.out.println("  [1] Run another combination");
            System.out.println("  [2] Export this result to CSV and exit");
            System.out.println("  [3] Exit without exporting");
            System.out.print("Choice: ");
            String next = SC.nextLine().trim();

            if (next.equals("2")) {
                exportSingle(result);
                break;
            } else if (next.equals("3")) {
                break;
            }
        }
    }

    private static QueueAlgorithm pickAlgorithm(List<QueueAlgorithm> algorithms) {
        System.out.println("Select algorithm:");
        for (int i = 0; i < algorithms.size(); i++)
            System.out.printf("  [%d] %s%n", i + 1, algorithms.get(i).getName());
        return algorithms.get(pickIndex("Algorithm", algorithms.size()) - 1);
    }

    private static RestaurantSetting pickRestaurant(List<RestaurantSetting> restaurants) {
        System.out.println("Select restaurant setting:");
        for (int i = 0; i < restaurants.size(); i++) {
            RestaurantSetting r = restaurants.get(i);
            System.out.printf("  [%d] %s  (tables: %d, seats: %d, time limit: %s)%n",
                    i + 1, r.getName(), r.getTables().size(), r.getTotalSeats(),
                    r.getTimeLimit() > 0 ? r.getTimeLimit() + " min" : "none");
        }
        return restaurants.get(pickIndex("Restaurant", restaurants.size()) - 1);
    }

    private static CustomerScenario pickScenario(List<CustomerScenario> scenarios) {
        System.out.println("Select customer scenario:");
        for (int i = 0; i < scenarios.size(); i++) {
            CustomerScenario s = scenarios.get(i);
            long vipCount = s.getArrivals().stream().filter(CustomerGroup::isVip).count();
            System.out.printf("  [%d] %s  (groups: %d, VIP: %d)%n",
                    i + 1, s.getName(), s.getArrivals().size(), vipCount);
        }
        return scenarios.get(pickIndex("Scenario", scenarios.size()) - 1);
    }

    private static int pickIndex(String label, int max) {
        while (true) {
            System.out.print(label + " number: ");
            String input = SC.nextLine().trim();
            try {
                int n = Integer.parseInt(input);
                if (n >= 1 && n <= max) return n;
                System.out.println("  Please enter a number between 1 and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number.");
            }
        }
    }


    private static void runAllMode(List<QueueAlgorithm> algorithms,
                                    List<RestaurantSetting> restaurants,
                                    List<CustomerScenario> scenarios) {
        SimulationEngine engine = new SimulationEngine();
        List<SimulationResult> allResults = new ArrayList<>();

        int total   = algorithms.size() * restaurants.size() * scenarios.size();
        int caseNum = 0;

        System.out.printf("%nRunning %d alg × %d restaurant × %d scenario = %d cases%n%n",
                algorithms.size(), restaurants.size(), scenarios.size(), total);

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
        promptCsvExport(allResults);
        System.out.println("Press Enter to exit...");
        SC.nextLine();
    }

    private static void exportSingle(SimulationResult result) {
        System.out.print("Enter output filename (default: result.csv): ");
        String fname = SC.nextLine().trim();
        if (fname.isEmpty()) fname = "result.csv";
        try {
            CsvExporter.exportAll(Collections.singletonList(result), fname);
            System.out.println("✔ Exported to " + fname);
        } catch (Exception ex) {
            System.err.println("Export failed: " + ex.getMessage());
        }
    }

    private static void promptCsvExport(List<SimulationResult> results) {
        System.out.print("Export all results to CSV? (y/n): ");
        if (SC.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.print("Enter output filename (default: results.csv): ");
            String fname = SC.nextLine().trim();
            if (fname.isEmpty()) fname = "results.csv";
            try {
                CsvExporter.exportAll(results, fname);
                System.out.println("✔ Exported to " + fname);
            } catch (Exception ex) {
                System.err.println("Export failed: " + ex.getMessage());
            }
        }
    }

    private static void printHeader() {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  RESTAURANT QUEUE SIMULATION                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }
}