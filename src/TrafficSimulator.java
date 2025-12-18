import java.util.*;

// 1. Vehicle Hierarchy
abstract class Vehicle implements Comparable<Vehicle> {
    private String id;
    private int priority; // 1 = Low (Car), 10 = High (Ambulance)

    public Vehicle(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public String getId() { return id; }
    public int getPriority() { return priority; }

    @Override
    public int compareTo(Vehicle other) {
        // Higher priority comes first (Descending order)
        return Integer.compare(other.priority, this.priority);
    }
    
    @Override
    public String toString() { return id + "(P:" + priority + ")"; }
}

class Car extends Vehicle {
    public Car(String id) { super(id, 1); }
}

class Ambulance extends Vehicle {
    public Ambulance(String id) { super(id, 10); }
}

// 2. Graph Components
class Intersection {
    private String name;
    private PriorityQueue<Vehicle> vehicleQueue;
    private boolean isGreenLight;

    public Intersection(String name) {
        this.name = name;
        this.vehicleQueue = new PriorityQueue<>();
        this.isGreenLight = false; // Default Red
    }

    public void addVehicle(Vehicle v) {
        vehicleQueue.add(v);
        System.out.println(v.getId() + " arrived at " + name);
    }

    public Vehicle processVehicle() {
        if (isGreenLight && !vehicleQueue.isEmpty()) {
            return vehicleQueue.poll();
        }
        return null;
    }

    public void setTrafficLight(boolean isGreen) {
        this.isGreenLight = isGreen;
    }

    public String getStatus() {
        return name + " [" + (isGreenLight ? "GREEN" : "RED") + "] Queue: " + vehicleQueue;
    }
    
    public String getName() { return name; }
}

class Road {
    Intersection destination;
    int weight; // Distance or congestion level

    public Road(Intersection destination, int weight) {
        this.destination = destination;
        this.weight = weight;
    }
}

// 3. The City Graph Manager
class CityTrafficSystem {
    private Map<String, Intersection> intersections;
    private Map<String, List<Road>> adjacencyList;

    public CityTrafficSystem() {
        intersections = new HashMap<>();
        adjacencyList = new HashMap<>();
    }

    public void addIntersection(String name) {
        Intersection i = new Intersection(name);
        intersections.put(name, i);
        adjacencyList.put(name, new ArrayList<>());
    }

    public void addRoad(String from, String to, int weight) {
        if (intersections.containsKey(from) && intersections.containsKey(to)) {
            adjacencyList.get(from).add(new Road(intersections.get(to), weight));
        }
    }

    public Intersection getIntersection(String name) {
        return intersections.get(name);
    }
    
    public Map<String, Intersection> getAllIntersections() {
        return intersections;
    }
}