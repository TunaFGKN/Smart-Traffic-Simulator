package models;

import java.util.concurrent.PriorityBlockingQueue;

// Represents a directed road segment connecting two nodes. Contains a priority queue to manage traffic flow and congestion data.
public class Edge {
	public Node target;
    double baseWeight;
    public PriorityBlockingQueue<Vehicle> vehicleQueue; // Thread-safe queue that orders vehicles by priority (Emergency > Normal)

    public Edge(Node target, double weight) {
        this.target = target;
        this.baseWeight = weight;
        this.vehicleQueue = new PriorityBlockingQueue<>();
    }
    
    // Calculates the dynamic cost of this road for path finding. Formula = Base Distance + Queue Size.
    public double getCurrentWeight() {
        return baseWeight + (vehicleQueue.size() * 0.5);
    }
}