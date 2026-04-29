package com.restaurant.util;

import com.restaurant.model.*;
import java.io.*;
import java.util.*;

public class JsonLoader {

    public static List<RestaurantSetting> loadRestaurantSettings(String filePath) throws IOException {
        String raw = readFile(filePath);
        List<RestaurantSetting> settings = new ArrayList<>();
        for (String obj : splitTopLevelObjects(raw)) {
            String id = extractString(obj, "id");
            String name = extractString(obj, "name");
            int timeLimit = extractInt(obj, "time_limit");
            if (timeLimit < 0) timeLimit = 0;
            List<Table> tables = parseTables(obj);
            if (id != null && name != null && !tables.isEmpty())
                settings.add(new RestaurantSetting(id, name, tables, timeLimit));
        }
        if (settings.isEmpty()) throw new IllegalArgumentException("No valid restaurant settings in: " + filePath);
        return settings;
    }

    public static List<CustomerScenario> loadCustomerScenarios(String filePath) throws IOException {
        String raw = readFile(filePath);
        List<CustomerScenario> scenarios = new ArrayList<>();
        for (String obj : splitTopLevelObjects(raw)) {
            String id = extractString(obj, "id");
            String name = extractString(obj, "name");
            List<CustomerGroup> arrivals = parseArrivals(obj);
            if (id != null && name != null && !arrivals.isEmpty())
                scenarios.add(new CustomerScenario(id, name, arrivals));
        }
        if (scenarios.isEmpty()) throw new IllegalArgumentException("No valid customer scenarios in: " + filePath);
        return scenarios;
    }

    public static List<CustomerScenario> tryLoadCustomerScenarios(String filePath) {
        try {
            File f = new File(filePath);
            if (!f.exists()) return new ArrayList<>();
            return loadCustomerScenarios(filePath);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    private static List<String> splitTopLevelObjects(String json) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start != -1) { parts.add(json.substring(start, i + 1)); start = -1; } }
        }
        return parts;
    }

    public static String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    public static int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) return -1;
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else if (num.length() > 0) break;
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : -1;
    }

    public static boolean extractBoolean(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return false;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) return false;
        return json.substring(colon + 1).trim().startsWith("true");
    }

    private static List<Table> parseTables(String json) {
        List<Table> tables = new ArrayList<>();
        int ts = json.indexOf("\"tables\"");
        if (ts < 0) return tables;
        int arrStart = json.indexOf('[', ts);
        if (arrStart < 0) return tables;
        for (String obj : extractArrayObjects(json, arrStart)) {
            String tableId = extractString(obj, "tableId");
            int capacity = extractInt(obj, "capacity");
            if (tableId != null && capacity > 0) tables.add(new Table(tableId, capacity));
        }
        return tables;
    }

    private static List<CustomerGroup> parseArrivals(String json) {
        List<CustomerGroup> groups = new ArrayList<>();
        int as = json.indexOf("\"arrivals\"");
        if (as < 0) return groups;
        int arrStart = json.indexOf('[', as);
        if (arrStart < 0) return groups;
        for (String obj : extractArrayObjects(json, arrStart)) {
            String groupId = extractString(obj, "groupId");
            int groupSize = extractInt(obj, "groupSize");
            int preferred = extractInt(obj, "preferredTableSize");
            boolean isVip = extractBoolean(obj, "isVip");
            int arrival = extractInt(obj, "arrivalTime");
            int duration = extractInt(obj, "diningDuration");
            if (groupId != null && groupSize > 0 && preferred > 0 && arrival >= 0 && duration > 0)
                groups.add(new CustomerGroup(groupId, groupSize, preferred, isVip, arrival, duration));
        }
        return groups;
    }

    public static List<String> extractArrayObjects(String json, int arrStart) {
        List<String> objs = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start != -1) { objs.add(json.substring(start, i + 1)); start = -1; } }
            else if (c == ']' && depth == 0) break;
        }
        return objs;
    }
}