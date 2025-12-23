package models;

// Represents a vertex in the city graph (Intersection, Apartment, etc.) containing coordinates for GUI rendering.
public class Node {
	public int id, x, y;
    public String name;
    public NodeType type;
    public TrafficLight trafficLight; // Only initialised if this node is an INTERSECTION

    public Node(int id, String name, NodeType type, int x, int y) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        if (type == NodeType.INTERSECTION) {
            this.trafficLight = new TrafficLight();
        }
    }
    
    @Override public String toString() { return name; }
}