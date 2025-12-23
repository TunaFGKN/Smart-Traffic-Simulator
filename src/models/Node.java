package models;

public class Node {
	public int id;
    public String name;
    public NodeType type;
    public int x;
	public int y;
    public TrafficLight trafficLight;

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
