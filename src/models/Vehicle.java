package models;

import java.util.List;

// Represents a moving entity in the simulation. Implements Comparable to allow sorting in PriorityQueues based on vehicle type and arrival time.
public class Vehicle implements Comparable<Vehicle>{
    public String id;
    public VehicleType type;
    public Node current;
	public Node next;
	public Node destination;
    public List<Node> path;
    public int currentPathIndex = 0;
    public double progress = 0; 		// Animation state: 0.0 (start of edge) to 1.0 (end of edge)    
    public boolean isReturning = false; // Specific logic for emergency vehicles returning to their station
    public long entryTime; 				// Used for FIFO ordering within the same priority level
    public Edge currentEdgeObj = null; 	// Reference to the road (Edge) the vehicle is currently on

    public Vehicle(String id, VehicleType type, Node start, Node dest, List<Node> path) {
        this.id = id;
        this.type = type;
        this.current = start;
        this.destination = dest;
        this.path = path;
        this.currentPathIndex = 0;
        this.isReturning = false;
        this.entryTime = System.nanoTime(); // Capture precise time for queue ordering
        if (path.size() > 1) this.next = path.get(1);
    }

    // Priority Queue sorting logic: 1. Priority (Emergency vehicles first). 2. Time (First-In-First-Out for vehicles of the same type).
    public int compareTo(Vehicle other) {
        int priorityComparison = Integer.compare(this.type.priority, other.type.priority);
        if (priorityComparison != 0) return priorityComparison;
        return Long.compare(this.entryTime, other.entryTime);
    }
}