package models;

import java.util.List;

public class Vehicle implements Comparable<Vehicle>{
    public String id;
    public VehicleType type;
    public Node current;
	public Node next;
	public Node destination;
    public List<Node> path;
    public double progress = 0;
    public int currentPathIndex = 0;
    public boolean isReturning = false;
    public long entryTime;
    public Edge currentEdgeObj = null;

    public Vehicle(String id, VehicleType type, Node start, Node dest, List<Node> path) {
        this.id = id;
        this.type = type;
        this.current = start;
        this.destination = dest;
        this.path = path;
        this.currentPathIndex = 0;
        this.isReturning = false;
        this.entryTime = System.nanoTime();
        if (path.size() > 1) this.next = path.get(1);
    }

    public int compareTo(Vehicle other) {
        int priorityComparison = Integer.compare(this.type.priority, other.type.priority);
        if (priorityComparison != 0) return priorityComparison;
        return Long.compare(this.entryTime, other.entryTime);
    }
}