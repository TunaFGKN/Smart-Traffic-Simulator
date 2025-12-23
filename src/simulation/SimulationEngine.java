package simulation;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import gui.SimulationPanel;
import models.*;

public class SimulationEngine extends Thread {
    CityGraph graph;
    public List<Vehicle> vehicles = new CopyOnWriteArrayList<>();
    SimulationPanel panel;

    String currentUserRole = "";
    String currentUserId = "";
    int carIdCounter = 1;
    int trafficLoopCount = 0;
    private Thread busScheduleThread;

    public SimulationEngine(CityGraph graph) {
        this.graph = graph;
    }

    public void setPanelToRefresh(SimulationPanel panel) {
        this.panel = panel;
    }

    public void setCurrentUser(String role, String id) {
        this.currentUserRole = role;
        this.currentUserId = id;
        panel.enableControls(role);
    }

    public void resetTraffic() {
        if (busScheduleThread != null && busScheduleThread.isAlive()) {
            busScheduleThread.interrupt();
        }

        vehicles.clear();
        carIdCounter = 1;

        if (graph != null && graph.adjList != null) {
            for (List<Edge> edges : graph.adjList.values()) {
                for (Edge e : edges) {
                    if (e.vehicleQueue != null) {
                        e.vehicleQueue.clear();
                    }
                }
            }
        }
    }

    public List<Node> findPath(Node start, Node end) {
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Node> previous = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        PriorityQueue<PQNode> queue = new PriorityQueue<>();

        for (int id : graph.nodes.keySet()) {
            distances.put(id, Double.MAX_VALUE);
        }
        distances.put(start.id, 0.0);
        queue.add(new PQNode(start, 0.0));

        while (!queue.isEmpty()) {
            PQNode currentPQ = queue.poll();
            Node current = currentPQ.node;

            if (visited.contains(current.id)) continue;
            visited.add(current.id);

            if (current == end) break;
            if (currentPQ.cost > distances.get(current.id)) continue;

            for (Edge edge : graph.adjList.get(current.id)) {
                double newDist = distances.get(current.id) + edge.getCurrentWeight();
                if (newDist < distances.get(edge.target.id)) {
                    distances.put(edge.target.id, newDist);
                    previous.put(edge.target.id, current);
                    queue.add(new PQNode(edge.target, newDist));
                }
            }
        }

        List<Node> path = new ArrayList<>();
        Node curr = end;
        if (distances.get(end.id) == Double.MAX_VALUE) return null;

        while (curr != null) {
            path.add(0, curr);
            curr = previous.get(curr.id);
        }

        if (path.isEmpty() || path.get(0) != start) return null;

        return path;
    }

    private static class PQNode implements Comparable<PQNode> {
        Node node;
        double cost;
        public PQNode(Node node, double cost) { this.node = node; this.cost = cost; }
        @Override public int compareTo(PQNode o) { return Double.compare(this.cost, o.cost); }
    }

    public boolean spawnVehicle(Node start, Node end, VehicleType type) {
        List<Node> path = findPath(start, end);
        if (path != null) {
            String id = type.toString().substring(0, 3) + (carIdCounter++);
            Vehicle v = new Vehicle(id, type, start, end, path);

            if (path.size() > 1) {
                Edge firstEdge = graph.getEdge(start.id, path.get(1).id);
                if (firstEdge != null) {
                    v.currentEdgeObj = firstEdge;
                    v.entryTime = System.nanoTime();
                    firstEdge.vehicleQueue.add(v);
                }
            }
            vehicles.add(v);
            return true;
        }
        return false;
    }

    public void spawnBusRoute(String driverId) {
        int[] ids1 = {71, 2, 16, 15, 14, 17, 7, 8, 9, 4, 3, 4, 2, 71};
        int[] ids2 = {72, 18, 17, 14, 15, 16, 1, 6, 5, 4, 9, 8, 7, 17, 18, 72};
        int[] ids3 = {73, 9, 8, 10, 82, 11, 12, 13, 14, 17, 7, 8, 9, 73};

        List<Node> route1 = new ArrayList<>(); for (int id : ids1) route1.add(graph.nodes.get(id));
        List<Node> route2 = new ArrayList<>(); for (int id : ids2) route2.add(graph.nodes.get(id));
        List<Node> route3 = new ArrayList<>(); for (int id : ids3) route3.add(graph.nodes.get(id));

        createBusAndAddToQueue("BUS-1A", route1);
        createBusAndAddToQueue("BUS-1B", route2);
        createBusAndAddToQueue("BUS-1C", route3);

        busScheduleThread = new Thread(() -> {
            try {
                Thread.sleep(25000);
                if (Thread.currentThread().isInterrupted()) return;
                createBusAndAddToQueue("BUS-2A", route1);
                createBusAndAddToQueue("BUS-2B", route2);
                createBusAndAddToQueue("BUS-2C", route3);

                Thread.sleep(25000);
                if (Thread.currentThread().isInterrupted()) return;
                createBusAndAddToQueue("BUS-3A", route1);
                createBusAndAddToQueue("BUS-3B", route2);
                createBusAndAddToQueue("BUS-3C", route3);

            } catch (InterruptedException e) {
            }
        });
        busScheduleThread.start();
    }

    private void createBusAndAddToQueue(String id, List<Node> route) {
        if (route.isEmpty()) return;
        Vehicle v = new Vehicle(id, VehicleType.BUS, route.get(0), route.get(route.size()-1), route);

        if (route.size() > 1) {
            Edge e = graph.getEdge(route.get(0).id, route.get(1).id);
            if (e != null) {
                v.currentEdgeObj = e;
                v.entryTime = System.nanoTime();
                e.vehicleQueue.add(v);
            }
        }
        vehicles.add(v);
    }

    @Override
    public void run() {
        while (true) {
            try {
                for (Vehicle v : vehicles) {
                    moveVehicle(v);
                }
                updateLights();
                if (panel != null) panel.repaint();
                Thread.sleep(50);
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    // --- DÜZELTİLEN METOT BURADA ---
    private void moveVehicle(Vehicle v) {
        if (v.path.isEmpty() || v.next == null) return;

        // 1. Önce bu tur ne kadar ilerleyeceğini hesapla
        double dx = v.next.x - v.current.x;
        double dy = v.next.y - v.current.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double normalizedSpeed = (v.type.speed * 300.0) / Math.max(distance, 1.0);

        // 2. KONTROL: Eğer bir sonraki adımda varış noktasına (Kavşağa) ulaşacaksam
        // Bu "look-ahead" (ileri bakma) mantığı frame atlamalarını önler.
        if (v.progress + normalizedSpeed >= 1.0 && v.next.type == NodeType.INTERSECTION) {
            TrafficLight light = v.next.trafficLight;
            boolean lightGreen = light.canPass(v.current, v.next);

            Vehicle topPriority = null;
            if (v.currentEdgeObj != null) {
                topPriority = v.currentEdgeObj.vehicleQueue.peek();
            }

            boolean amIPriority = (topPriority == null || topPriority == v);
            boolean isEmergency = (v.type.priority <= 3);

            if (isEmergency) {
                if (!amIPriority) return; // Acil durum aracı sadece önü tıkalıysa durur
            } else {
                // Normal araç: Işık Kırmızıysa VEYA Önümde araç varsa DUR
                if (!lightGreen || !amIPriority) return;
            }
        }

        // 3. İlerle
        v.progress += normalizedSpeed;

        if (v.progress >= 1.0) {
            v.progress = 0;
            if (v.currentEdgeObj != null) v.currentEdgeObj.vehicleQueue.remove(v);

            v.currentPathIndex++;
            if (v.currentPathIndex >= v.path.size() - 1) {
                handleEndOfPath(v);
            } else {
                v.current = v.path.get(v.currentPathIndex);
                v.next = v.path.get(v.currentPathIndex + 1);

                Edge newEdge = graph.getEdge(v.current.id, v.next.id);
                if (newEdge != null) {
                    v.currentEdgeObj = newEdge;
                    v.entryTime = System.nanoTime();
                    newEdge.vehicleQueue.add(v);
                }
            }
        }
    }

    private void handleEndOfPath(Vehicle v) {
        boolean isEmergency = (v.type == VehicleType.AMBULANCE || v.type == VehicleType.POLICE_CAR || v.type == VehicleType.FIRE_TRUCK);

        if (v.type == VehicleType.BUS) {
            v.currentPathIndex = 0;
            v.current = v.path.get(0);
            v.next = v.path.get(1);
            v.progress = 0;

            Edge newEdge = graph.getEdge(v.current.id, v.next.id);
            if (newEdge != null) {
                v.currentEdgeObj = newEdge;
                v.entryTime = System.nanoTime();
                newEdge.vehicleQueue.add(v);
            }
            return;
        }
        else if (isEmergency && !v.isReturning) {
            Node currentLoc = v.path.get(v.path.size()-1);
            Node base = v.path.get(0);
            List<Node> returnPath = findPath(currentLoc, base);
            if (returnPath != null) {
                v.path = returnPath;
                v.currentPathIndex = 0;
                v.current = returnPath.get(0);
                v.next = returnPath.get(1);
                v.destination = base;
                v.isReturning = true;
                Edge e = graph.getEdge(v.current.id, v.next.id);
                if(e != null) {
                    v.currentEdgeObj = e;
                    v.entryTime = System.nanoTime();
                    e.vehicleQueue.add(v);
                }
            } else {
                vehicles.remove(v);
            }
        }
        else {
            v.next = null;
            vehicles.remove(v);
        }
    }

    private void updateLights() {
        for (Node n : graph.nodes.values()) {
            if (n.type == NodeType.INTERSECTION) {
                int nsLoad = 0;
                int ewLoad = 0;

                for (Map.Entry<Integer, List<Edge>> entry : graph.adjList.entrySet()) {
                    Node fromNode = graph.nodes.get(entry.getKey());
                    List<Edge> edges = entry.getValue();

                    for (Edge e : edges) {
                        if (e.target == n) {
                            boolean isVertical = Math.abs(fromNode.y - n.y) > Math.abs(fromNode.x - n.x);
                            if (isVertical) {
                                nsLoad += e.vehicleQueue.size();
                            } else {
                                ewLoad += e.vehicleQueue.size();
                            }
                        }
                    }
                }
                n.trafficLight.update(nsLoad, ewLoad);
            }
        }
    }

    public void initializeTraffic() {
        System.out.println("Preparing the city traffic...");
        Random R = new Random();
        for(int i = 0; i < 20; i++) {
            Node s = graph.nodes.get(R.nextInt(15) + 51);
            Node e = graph.nodes.get(R.nextInt(15) + 51);
            if (s != e) spawnVehicle(s, e, VehicleType.CAR);
        }

        Node startNode_P = graph.nodes.get(81);
        Node endNode_P = graph.nodes.get(R.nextInt(15) + 51);
        if (startNode_P != endNode_P) spawnVehicle(startNode_P, endNode_P, VehicleType.POLICE_CAR);

        Node startNode_A = graph.nodes.get(82);
        Node endNode_A = graph.nodes.get(R.nextInt(15) + 51);
        if (startNode_A != endNode_A) spawnVehicle(startNode_A, endNode_A, VehicleType.AMBULANCE);

        Node startNode_F = graph.nodes.get(83);
        Node endNode_F = graph.nodes.get(R.nextInt(15) + 51);
        if (startNode_F != endNode_F) spawnVehicle(startNode_F, endNode_F, VehicleType.FIRE_TRUCK);

        scatterVehiclesOnPath();
        spawnBusRoute("SYSTEM_AUTO");

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    trafficLoopCount++;

                    if (!graph.nodes.isEmpty()) {
                        Node s = graph.nodes.get(R.nextInt(15) + 51);
                        Node e = graph.nodes.get(R.nextInt(15) + 51);
                        if (s != e) spawnVehicle(s, e, VehicleType.CAR);
                    }
                    if((trafficLoopCount % 10) == 0){
                        spawnVehicle(graph.nodes.get(81), graph.nodes.get(R.nextInt(15) + 51), VehicleType.POLICE_CAR);
                        spawnVehicle(graph.nodes.get(82), graph.nodes.get(R.nextInt(15) + 51), VehicleType.AMBULANCE);
                        spawnVehicle(graph.nodes.get(83), graph.nodes.get(R.nextInt(15) + 51), VehicleType.FIRE_TRUCK);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    private void scatterVehiclesOnPath() {
        for (Vehicle v : vehicles) {
            if (v.type == VehicleType.BUS) continue;

            if (v.path != null && v.path.size() > 2) {
                int randomPathIndex = (int) (Math.random() * (v.path.size() - 1));
                v.currentPathIndex = randomPathIndex;
                v.current = v.path.get(randomPathIndex);
                v.next = v.path.get(randomPathIndex + 1);
                v.progress = Math.random();
            }
        }
    }
}