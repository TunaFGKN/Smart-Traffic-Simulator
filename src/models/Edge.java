package models;

import java.util.concurrent.PriorityBlockingQueue;

public class Edge {
	public Node target;
    double baseWeight;
    public PriorityBlockingQueue<Vehicle> vehicleQueue;

    public Edge(Node target, double weight) {
        this.target = target;
        this.baseWeight = weight;
        this.vehicleQueue = new PriorityBlockingQueue<>();
    }
    public double getCurrentWeight() {
        return baseWeight + (vehicleQueue.size() * 0.5);
    }
}
