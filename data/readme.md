find src -name "*.java" -print | xargs javac -d out/

Run simulation:
java -cp out com.restaurant.Main data/restaurant_settings.json data/customer_scenarios.json


Run UI:
java -cp out com.restaurant.InteractiveUI data/restaurant_settings.json data/customer_scenarios.json