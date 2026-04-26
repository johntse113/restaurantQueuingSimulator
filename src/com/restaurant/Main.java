package com.restaurant;

import com.restaurant.algorithm.*;
import com.restaurant.model.CustomerScenario;
import com.restaurant.model.RestaurantSetting;
import com.restaurant.simulation.SimulationEngine;
import com.restaurant.simulation.SimulationResult;
import com.restaurant.util.JsonLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String restaurantFile = args.length > 0 ? args[0] : "data/restaurant_settings.json";
        String scenarioFile = args.length > 1 ? args[1] : "data/customer_scenarios.json";

        List<RestaurantSetting> restaurants;
        List<CustomerScenario> scenarios;

        try {
            restaurants = JsonLoader.loadRestaurantSettings(restaurantFile);
            scenarios = JsonLoader.loadCustomerScenarios(scenarioFile);
        } catch (Exception e) {
            System.err.println("ERROR loading JSON files: " + e.getMessage());
            System.err.println("Usage: java Main [restaurant_settings.json] [customer_scenarios.json]");
            return;
        }

        List<QueueAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new SingleQueueStaffGuided());
        algorithms.add(new SingleQueueSelfService());
        algorithms.add(new GroupSizeBasedQueue());
        algorithms.add(new PriorityQueueAlgorithm());

        SimulationEngine engine = new SimulationEngine();

        int total = algorithms.size() * restaurants.size() * scenarios.size();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        RESTAURANT QUEUE SIMULATION — BATCH RUNNER             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.printf("Running %d algorithm(s) × %d restaurant(s) × %d scenario(s) = %d test cases%n%n",
                algorithms.size(), restaurants.size(), scenarios.size(), total);

        int caseNum = 0;
        for (QueueAlgorithm alg : algorithms) {
            for (RestaurantSetting restaurant : restaurants) {
                for (CustomerScenario scenario : scenarios) {
                    caseNum++;
                    System.out.printf("%n═══ Case %d / %d ══════════════════════════════════════%n", caseNum, total);
                    SimulationResult result = engine.run(alg, restaurant, scenario);
                    result.print();
                }
            }
        }

        System.out.println("\n✔ All simulation cases completed.");
        System.out.println("Press Enter to exit...");
        new Scanner(System.in).nextLine();
    }
}