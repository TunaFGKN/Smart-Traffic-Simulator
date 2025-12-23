package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Represents the entire city layout as a graph. Manages nodes (intersections/buildings) and edges (roads).
public class CityGraph {
	public Map<Integer, Node> nodes = new HashMap<>(); // Maps node IDs to Node objects for quick lookup
    public Map<Integer, List<Edge>> adjList = new HashMap<>(); // Adjacency list representing road connections: Node ID -> List of outgoing Edges

    public CityGraph() {
        initializeNodes();
        initializeConnections();
    }
    
    // Creates a node.
    private void addNode(int id, String name, NodeType type, int x, int y) {
        nodes.put(id, new Node(id, name, type, x, y));
        adjList.put(id, new ArrayList<>());
    }

    // Creates a directed edge (road) between two nodes with a specific base weight (distance).
    private void addEdge(int from, int to, double w) {
        if (nodes.containsKey(from) && nodes.containsKey(to)) {
            adjList.get(from).add(new Edge(nodes.get(to), w));
        }
    }

    // Retrieves the edge object connecting two specific nodes. Useful for accessing queue data on a specific road segment.
    public Edge getEdge(int fromId, int toId) {
        if (!adjList.containsKey(fromId)) return null;
        for (Edge e : adjList.get(fromId)) {
            if (e.target.id == toId) {
                return e;
            }
        }
        return null;
    }
    
    // Initialises all nodes (intersections, apartments, services) with their specific coordinates for the GUI.
    private void initializeNodes() {
    	// --- Intersections (Nodes 1-18) ---
        addNode(1, "INTR" + 1, NodeType.INTERSECTION, 560, 360);
        addNode(2, "INTR" + 2, NodeType.INTERSECTION, 830, 360);
        addNode(3, "INTR" + 3, NodeType.INTERSECTION, 1100, 640);
        addNode(4, "INTR" + 4, NodeType.INTERSECTION, 830, 640);
        addNode(5, "INTR" + 5, NodeType.INTERSECTION, 720, 450);
        addNode(6, "INTR" + 6, NodeType.INTERSECTION, 560, 450);
        addNode(7, "INTR" + 7, NodeType.INTERSECTION, 410, 450);
        addNode(8, "INTR" + 8, NodeType.INTERSECTION, 410, 790);
        addNode(9, "INTR" + 9, NodeType.INTERSECTION, 830, 790);
        addNode(10, "INTR" + 10, NodeType.INTERSECTION, 270, 790);
        addNode(11, "INTR" + 11, NodeType.INTERSECTION, 100, 450);
        addNode(12, "INTR" + 12, NodeType.INTERSECTION, 100, 100);
        addNode(13, "INTR" + 13, NodeType.INTERSECTION, 270, 100);
        addNode(14, "INTR" + 14, NodeType.INTERSECTION, 410, 100);
        addNode(15, "INTR" + 15, NodeType.INTERSECTION, 560, 100);
        addNode(16, "INTR" + 16, NodeType.INTERSECTION, 560, 210);
        addNode(17, "INTR" + 17, NodeType.INTERSECTION, 410, 360);
        addNode(18, "INTR" + 18, NodeType.INTERSECTION, 270, 360);

        // --- Residential Buildings (Nodes 51-65) ---
        int[][] aptCoords = {
                {100, 50}, {100, 500}, {270, 50}, {220, 360}, {270, 840},
                {410, 50}, {460, 360}, {360, 450}, {410, 840}, {560, 50},
                {510, 210}, {510, 360}, {720, 400}, {830, 840}, {1150, 640}
        };
        for (int i = 0; i < 15; i++) {
            addNode(51 + i, "APT" + (i + 1), NodeType.APARTMENT, aptCoords[i][0], aptCoords[i][1]);
        }

        // --- Parking Lots (Nodes 71-73) ---
        addNode(71, "P1", NodeType.PARKING, 880, 360);
        addNode(72, "P2", NodeType.PARKING, 270, 310);
        addNode(73, "P3", NodeType.PARKING, 880, 790);

        // --- Emergency Services (Nodes 81-83) ---
        addNode(81, "POLICE", NodeType.POLICE, 560, 490);
        addNode(82, "HOSP", NodeType.HOSPITAL, 270, 450);
        addNode(83, "FIRE", NodeType.FIRE_STATION, 1100, 590);
    }

    // Defines the road network topology. Most roads are bidirectional (added as two directed edges).
    private void initializeConnections() {
        //INTERSECTION
        addEdge(1, 2, 2.7); addEdge(1, 6, 0.9); addEdge(1, 16, 1.5); addEdge(1, 62, 0.5);
        addEdge(2, 1, 2.7); addEdge(2, 4, 2.8); addEdge(2, 16, 3.1); addEdge(2, 71, 0.5);
        addEdge(3, 4, 2.7); addEdge(3, 65, 0.5); addEdge(3, 83, 0.5);
        addEdge(4, 2, 2.8); addEdge(4, 3, 2.7); addEdge(4, 5, 2.1); addEdge(4, 9, 1.5);
        addEdge(5, 4, 2.1); addEdge(5, 6, 1.6); addEdge(5, 63, 0.5);
        addEdge(6, 5, 1.6); addEdge(6, 7, 1.6); addEdge(6, 1, 0.9); addEdge(6, 81, 0.5);
        addEdge(7, 6, 1.6); addEdge(7, 8, 3.4); addEdge(7, 17, 0.9); addEdge(7, 58, 0.5);
        addEdge(8, 7, 3.4); addEdge(8, 9, 4.3); addEdge(8, 10, 1.4); addEdge(8, 59, 0.5);
        addEdge(9, 4, 1.5); addEdge(9, 8, 4.3); addEdge(9, 64, 0.5); addEdge(9, 73, 0.5);
        addEdge(10, 8, 1.4); addEdge(10, 82, 3.4); addEdge(10, 55, 0.5);
        addEdge(11, 12, 3.5); addEdge(11, 82, 1.7); addEdge(11, 52, 0.5);
        addEdge(12, 11, 3.5); addEdge(12, 13, 1.7); addEdge(12, 51, 0.5);
        addEdge(13, 12, 1.7); addEdge(13, 14, 1.4); addEdge(13, 53, 0.5);
        addEdge(14, 13, 1.4); addEdge(14, 17, 2.6); addEdge(14, 15, 1.6); addEdge(14, 56, 0.5);
        addEdge(15, 16, 1.1); addEdge(15, 14, 1.6); addEdge(15, 60, 0.5);
        addEdge(16, 1, 1.5); addEdge(16, 2, 3.1); addEdge(16, 15, 1.1); addEdge(16, 61, 0.5);
        addEdge(17, 7, 0.9); addEdge(17, 18, 1.4); addEdge(17, 14, 2.6); addEdge(17, 57, 0.5);
        addEdge(18, 17, 1.4); addEdge(18, 54, 0.5); addEdge(18, 72, 0.5);

        //APARTMENT
        addEdge(51,12,0.5); addEdge(52,11,0.5); addEdge(53,13,0.5);
        addEdge(54,18,0.5); addEdge(55,10,0.5); addEdge(56,14,0.5);
        addEdge(57,17,0.5); addEdge(58,7,0.5); addEdge(59,8,0.5);
        addEdge(60,15,0.5); addEdge(61,16,0.5); addEdge(62,1,0.5);
        addEdge(63,5,0.5);  addEdge(64,9,0.5);  addEdge(65,3,0.5);

        //PARKING
        addEdge(71, 2, 0.5); addEdge(72, 18, 0.5); addEdge(73, 9, 0.2);

        //POLICE-HOSPITAL-FIRE_STATION
        addEdge(81, 6, 0.5);
        addEdge(82, 10, 3.4); addEdge(82, 11, 1.7);
        addEdge(83, 3, 0.5);
    }
}
