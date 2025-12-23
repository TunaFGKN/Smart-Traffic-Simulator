import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

// --- ANA SINIF ---
public class SmartCityTraffic extends JFrame {
    private CityGraph cityGraph;
    private SimulationEngine engine;
    private JPanel mainContainer;
    private CardLayout cardLayout;

    public SmartCityTraffic() {
        setTitle("Smart City Traffic Control System");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Tam ekran
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Sistemi Başlat
        cityGraph = new CityGraph();
        engine = new SimulationEngine(cityGraph);

        // GUI Ayarları
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Panelleri Oluştur
        JPanel loginPanel = createLoginPanel();

        // SimulationPanel oluşturulurken Logout aksiyonu veriliyor
        // Logout yapıldığında trafiği temizle
        SimulationPanel simPanel = new SimulationPanel(cityGraph, engine, () -> {
            engine.resetTraffic(); // Çıkışta araçları sil
            cardLayout.show(mainContainer, "LOGIN");
        });

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(simPanel, "SIMULATION");

        add(mainContainer);

        // Motoru panele bağla ve başlat
        engine.setPanelToRefresh(simPanel);
        engine.initializeTraffic(); // Trafiği ve otobüsleri hazırla
        engine.start(); // Motoru çalıştır
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(80, 84, 88));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 10, 20, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("City Traffic Login", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        String[] roles = {"Personal Car Driver", "Bus Driver", "Emergency Service", "Free View"};
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        gbc.gridy = 1;
        panel.add(roleCombo, gbc);

        JTextField emailField = new JTextField(20);
        emailField.setBorder(BorderFactory.createTitledBorder("Email / ID"));
        gbc.gridy = 2;
        panel.add(emailField, gbc);

        JPasswordField passField = new JPasswordField(20);
        passField.setBorder(BorderFactory.createTitledBorder("Password"));
        gbc.gridy = 3;
        panel.add(passField, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(80, 84, 82));
        loginBtn.setForeground(Color.DARK_GRAY);
        gbc.gridy = 4;
        panel.add(loginBtn, gbc);

        JLabel msgLabel = new JLabel(" ");
        msgLabel.setForeground(Color.RED);
        gbc.gridy = 5;
        panel.add(msgLabel, gbc);

        loginBtn.addActionListener(e -> {
            String role = (String) roleCombo.getSelectedItem();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword());

            if (role.equals("Free View")) {
                engine.setCurrentUser("FREE_VIEW", "guest");
                cardLayout.show(mainContainer, "SIMULATION");
                return;
            }

            if (!pass.equals("1234")) {
                msgLabel.setText("Invalid Password!");
                return;
            }

            boolean valid = false;
            String userRole = "";

            if (role.startsWith("Personal") && email.matches("cardriver\\d+@example\\.com")) {
                valid = true;
                userRole = "CAR_DRIVER";
            } else if (role.startsWith("Bus") && email.matches("busdriver\\d+@example\\.com")) {
                valid = true;
                userRole = "BUS_DRIVER";
            } else if (role.startsWith("Emergency") && email.matches("emergency\\d+@example\\.com")) {
                valid = true;
                userRole = "EMERGENCY";
            }

            if (valid) {
                engine.setCurrentUser(userRole, email);
                cardLayout.show(mainContainer, "SIMULATION");
            } else {
                msgLabel.setText("Invalid User ID format!");
            }
        });

        return panel;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SmartCityTraffic().setVisible(true));
    }
}

// --- VERİ YAPILARI ---

enum NodeType { INTERSECTION, APARTMENT, PARKING, POLICE, HOSPITAL, FIRE_STATION }
enum VehicleType {
    AMBULANCE(1, 0.03),
    FIRE_TRUCK(2, 0.02),
    POLICE_CAR(3, 0.022),
    BUS(4, 0.01),
    CAR(5, 0.014);

    public final int priority;
    public final double speed;

    VehicleType(int p, double s) {
        this.priority = p;
        this.speed = s;
    }
}

class Node {
    int id;
    String name;
    NodeType type;
    int x, y;
    TrafficLight trafficLight;

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

class Edge {
    Node target;
    double baseWeight;
    PriorityBlockingQueue<Vehicle> vehicleQueue;

    public Edge(Node target, double weight) {
        this.target = target;
        this.baseWeight = weight;
        this.vehicleQueue = new PriorityBlockingQueue<>();
    }
    public double getCurrentWeight() {
        return baseWeight + (vehicleQueue.size() * 0.5);
    }
}

class TrafficLight {
    boolean northSouthGreen = true;
    int greenDuration = 150;
    int timer = 0;

    public void update(int nsQueue, int ewQueue) {
        timer++;
        if (timer > greenDuration) {
            northSouthGreen = !northSouthGreen;
            timer = 0;
        }
    }

    public boolean canPass(Node from, Node intersection) {
        int dx = Math.abs(from.x - intersection.x);
        int dy = Math.abs(from.y - intersection.y);
        boolean approachingVertically = dy > dx;

        if (northSouthGreen) {
            return approachingVertically;
        } else {
            return !approachingVertically;
        }
    }
}

class Vehicle implements Comparable<Vehicle>{
    String id;
    VehicleType type;
    Node current, next, destination;
    List<Node> path;
    double progress = 0;
    int currentPathIndex = 0;
    boolean isReturning = false;
    long entryTime;
    Edge currentEdgeObj = null;

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

// --- GRAPH YAPISI ---

class CityGraph {
    Map<Integer, Node> nodes = new HashMap<>();
    Map<Integer, List<Edge>> adjList = new HashMap<>();

    public CityGraph() {
        initializeNodes();
        initializeConnections();
    }

    private void initializeNodes() {
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

        int[][] aptCoords = {
                {100, 50}, {100, 500}, {270, 50}, {220, 360}, {270, 840},
                {410, 50}, {460, 360}, {360, 450}, {410, 840}, {560, 50},
                {510, 210}, {510, 360}, {720, 400}, {830, 840}, {1150, 640}
        };
        for (int i = 0; i < 15; i++) {
            addNode(51 + i, "APT" + (i + 1), NodeType.APARTMENT, aptCoords[i][0], aptCoords[i][1]);
        }

        addNode(71, "P1", NodeType.PARKING, 880, 360);
        addNode(72, "P2", NodeType.PARKING, 270, 310);
        addNode(73, "P3", NodeType.PARKING, 880, 790);

        addNode(81, "POLICE", NodeType.POLICE, 560, 490);
        addNode(82, "HOSP", NodeType.HOSPITAL, 270, 450);
        addNode(83, "FIRE", NodeType.FIRE_STATION, 1100, 590);
    }

    private void addNode(int id, String name, NodeType type, int x, int y) {
        nodes.put(id, new Node(id, name, type, x, y));
        adjList.put(id, new ArrayList<>());
    }

    private void addEdge(int from, int to, double w) {
        if (nodes.containsKey(from) && nodes.containsKey(to)) {
            adjList.get(from).add(new Edge(nodes.get(to), w));
        }
    }

    public Edge getEdge(int fromId, int toId) {
        if (!adjList.containsKey(fromId)) return null;
        for (Edge e : adjList.get(fromId)) {
            if (e.target.id == toId) {
                return e;
            }
        }
        return null;
    }

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

// --- SİMÜLASYON MOTORU ---

class SimulationEngine extends Thread {
    CityGraph graph;
    List<Vehicle> vehicles = new CopyOnWriteArrayList<>();
    SimulationPanel panel;

    String currentUserRole = "";
    String currentUserId = "";
    int carIdCounter = 1;
    int trafficLoopCount = 0;

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
        vehicles.clear();
        carIdCounter = 1;
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

    private void spawnBusRoute(String driverId) {
        int[] ids1 = {71, 2, 16, 15, 14, 17, 7, 8, 9, 4, 3, 4, 2, 71};
        int[] ids2 = {72, 18, 17, 14, 15, 16, 1, 6, 5, 4, 9, 8, 7, 17, 18, 72};
        int[] ids3 = {73, 9, 8, 10, 82, 11, 12, 13, 14, 17, 7, 8, 9, 73};

        List<Node> route1 = new ArrayList<>(); for (int id : ids1) route1.add(graph.nodes.get(id));
        List<Node> route2 = new ArrayList<>(); for (int id : ids2) route2.add(graph.nodes.get(id));
        List<Node> route3 = new ArrayList<>(); for (int id : ids3) route3.add(graph.nodes.get(id));

        // 1. Grup Otobüsler (Hemen Başlar)
        createBusAndAddToQueue("BUS-1A", route1);
        createBusAndAddToQueue("BUS-1B", route2);
        createBusAndAddToQueue("BUS-1C", route3);

        new Thread(() -> {
            try {
                // 2. Grup için 25 saniye bekle
                Thread.sleep(25000);
                createBusAndAddToQueue("BUS-2A", route1);
                createBusAndAddToQueue("BUS-2B", route2);
                createBusAndAddToQueue("BUS-2C", route3);

                // 3. Grup için bir 25 saniye daha bekle (Toplam 50. saniye)
                Thread.sleep(25000);
                createBusAndAddToQueue("BUS-3A", route1);
                createBusAndAddToQueue("BUS-3B", route2);
                createBusAndAddToQueue("BUS-3C", route3);

            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
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

    private void moveVehicle(Vehicle v) {
        if (v.path.isEmpty() || v.next == null) return;

        if (v.progress > 0.95 && v.next.type == NodeType.INTERSECTION) {
            TrafficLight light = v.next.trafficLight;
            boolean lightGreen = light.canPass(v.current, v.next);

            Vehicle topPriority = null;
            if (v.currentEdgeObj != null) {
                topPriority = v.currentEdgeObj.vehicleQueue.peek();
            }

            boolean amIPriority = (topPriority == null || topPriority == v);
            boolean isEmergency = (v.type.priority <= 3);

            if (isEmergency) {
                if (!amIPriority) return;
            } else {
                if (!lightGreen || !amIPriority) return;
            }
        }

        double dx = v.next.x - v.current.x;
        double dy = v.next.y - v.current.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double normalizedSpeed = (v.type.speed * 300.0) / Math.max(distance, 1.0);

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
            // Otobüs seferini tamamladı, araç siliniyor (Program bitişi)
            vehicles.remove(v);
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
                int nsLoad = (int)(Math.random() * 15);
                int ewLoad = (int)(Math.random() * 15);
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

        // --- DÜZELTME: OTOBÜSLERİ RASTGELE DAĞITMADAN SONRA BAŞLATIYORUZ ---
        // Rastgele araçları rotalarına dağıt (Böylece harita başında boş kalmaz)
        scatterVehiclesOnPath();

        // Şimdi otobüsleri başlat (Böylece otobüsler durağından başlar, dağılmaz)
        spawnBusRoute("SYSTEM_AUTO");

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
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
            // --- DÜZELTME: OTOBÜSLERİ DAĞITMA İŞLEMİNDEN MUAF TUTUYORUZ ---
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

// --- GÖRSELLEŞTİRME PANELİ ---

class SimulationPanel extends JPanel {
    private CityGraph graph;
    private SimulationEngine engine;

    private JPanel controlPanel;
    private MapPanel mapPanel;

    private JComboBox<Node> startBox, endBox;
    private JLabel statusLabel;

    private String currentRole = "";
    private Runnable onLogout;

    public SimulationPanel(CityGraph graph, SimulationEngine engine, Runnable onLogout) {
        this.graph = graph;
        this.engine = engine;
        this.onLogout = onLogout;

        setLayout(new BorderLayout());

        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(50, 50, 50));
        controlPanel.setPreferredSize(new Dimension(0, 60));

        mapPanel = new MapPanel();

        add(mapPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
    }

    private void addLogoutButton(GridBagConstraints gbc) {
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(Color.DARK_GRAY);
        logoutBtn.setForeground(Color.BLACK);
        logoutBtn.addActionListener(e -> {
            if (onLogout != null) onLogout.run();
        });
        controlPanel.add(Box.createHorizontalStrut(20), gbc);
        controlPanel.add(logoutBtn, gbc);
    }

    public void enableControls(String role) {
        this.currentRole = role;
        controlPanel.setVisible(true);

        switch (role) {
            case "CAR_DRIVER": initCarDriverPanel(); break;
            case "BUS_DRIVER": initBusDriverPanel(); break;
            case "EMERGENCY": initEmergencyPanel(); break;
            case "FREE_VIEW": initFreeViewPanel(); break;
            default: resetControlPanel(); break;
        }
        controlPanel.revalidate();
        controlPanel.repaint();
    }

    private void resetControlPanel() {
        controlPanel.removeAll();
        controlPanel.setLayout(new GridBagLayout());
        statusLabel.setText("Ready");
    }

    private void initCarDriverPanel() {
        resetControlPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 10);

        startBox = new JComboBox<>(); endBox = new JComboBox<>();
        fillNodeBoxes(startBox, endBox);

        JButton goBtn = new JButton("Start Journey");
        goBtn.setBackground(new Color(34, 139, 34));
        goBtn.setForeground(Color.BLACK);
        goBtn.setFocusPainted(false);
        goBtn.addActionListener(e -> spawnVehicleAction(VehicleType.CAR));

        statusLabel.setForeground(Color.lightGray);

        JLabel lblStart = new JLabel("Start:");
        lblStart.setForeground(Color.WHITE);

        JLabel lblEnd = new JLabel("End:");
        lblEnd.setForeground(Color.WHITE);

        controlPanel.add(lblStart, gbc);
        controlPanel.add(startBox, gbc);
        controlPanel.add(lblEnd, gbc);

        controlPanel.add(endBox, gbc);
        controlPanel.add(goBtn, gbc);
        controlPanel.add(statusLabel, gbc);
        addLogoutButton(gbc);
    }

    private void initBusDriverPanel() {
        resetControlPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 20, 0, 20);

        JLabel title = new JLabel("BUS CONTROL DASHBOARD");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(Color.CYAN);

        JLabel info = new JLabel("Magenta: Route A | Orange: Route B | Cyan: Route C");
        info.setForeground(Color.LIGHT_GRAY);

        controlPanel.add(title, gbc);
        controlPanel.add(info, gbc);
        addLogoutButton(gbc);
    }

    private void initEmergencyPanel() {
        resetControlPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 10);

        startBox = new JComboBox<>(); endBox = new JComboBox<>();
        fillNodeBoxes(startBox, endBox);

        JButton emergencyBtn = new JButton("AMBULANCE");
        emergencyBtn.setBackground(Color.RED);
        emergencyBtn.setForeground(Color.RED);
        emergencyBtn.setFont(new Font("Arial", Font.BOLD, 12));
        emergencyBtn.setFocusPainted(false);

        emergencyBtn.addActionListener(e -> {
            spawnVehicleAction(VehicleType.AMBULANCE);
        });

        JButton policeBtn = new JButton("POLICE");
        policeBtn.setBackground(Color.BLUE); policeBtn.setForeground(Color.BLUE);
        policeBtn.addActionListener(e -> spawnVehicleAction(VehicleType.POLICE_CAR));

        JButton fireBtn = new JButton("FIRE");
        fireBtn.setBackground(Color.ORANGE); fireBtn.setForeground(Color.ORANGE);
        fireBtn.addActionListener(e -> spawnVehicleAction(VehicleType.FIRE_TRUCK));

        statusLabel.setForeground(Color.RED);
        JLabel lblS = new JLabel("Base:"); lblS.setForeground(Color.WHITE);
        JLabel lblE = new JLabel("Target:"); lblE.setForeground(Color.WHITE);

        controlPanel.add(lblS, gbc);
        controlPanel.add(startBox, gbc);
        controlPanel.add(lblE, gbc);
        controlPanel.add(endBox, gbc);
        controlPanel.add(emergencyBtn, gbc);
        controlPanel.add(policeBtn, gbc);
        controlPanel.add(fireBtn, gbc);
        controlPanel.add(statusLabel, gbc);
        addLogoutButton(gbc);
    }

    private void initFreeViewPanel() {
        resetControlPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 30, 0, 30);
        JLabel modeLabel = new JLabel("MONITORING MODE");
        modeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        modeLabel.setForeground(Color.lightGray);
        controlPanel.add(modeLabel, gbc);
        addLogoutButton(gbc);
    }

    private void spawnVehicleAction(VehicleType type) {
        Node s = (Node) startBox.getSelectedItem();
        Node d = (Node) endBox.getSelectedItem();
        boolean spawned = engine.spawnVehicle(s, d, type);
        if(spawned) statusLabel.setText("Dispatched: " + type + " -> " + d.name);
        else statusLabel.setText("ERROR: No Path!");
    }

    private void fillNodeBoxes(JComboBox<Node> b1, JComboBox<Node> b2) {
        List<Node> sortedNodes = new ArrayList<>(graph.nodes.values());
        sortedNodes.sort((n1, n2) -> {
            int p1 = getNodePriority(n1.name);
            int p2 = getNodePriority(n2.name);
            if (p1 != p2) return Integer.compare(p1, p2);
            String prefix1 = n1.name.replaceAll("[0-9]", "");
            String prefix2 = n2.name.replaceAll("[0-9]", "");
            int prefixCompare = prefix1.compareTo(prefix2);
            if (prefixCompare != 0) return prefixCompare;
            String numStr1 = n1.name.replaceAll("[^0-9]", "");
            String numStr2 = n2.name.replaceAll("[^0-9]", "");
            if (numStr1.isEmpty() && numStr2.isEmpty()) return n1.name.compareTo(n2.name);
            if (numStr1.isEmpty()) return -1;
            if (numStr2.isEmpty()) return 1;
            return Integer.compare(Integer.parseInt(numStr1), Integer.parseInt(numStr2));
        });
        for(Node n : sortedNodes) {
            if(n.type == NodeType.INTERSECTION) continue;
            b1.addItem(n); b2.addItem(n);
        }
    }

    private int getNodePriority(String name) {
        if (name.startsWith("APT")) return 1;
        if (name.equals("HOSP") || name.equals("POLICE") || name.equals("FIRE")) return 2;
        if (name.startsWith("P") && name.matches("P\\d+")) return 3;
        return 4;
    }

    @Override
    public void repaint() {
        super.repaint();
        if(mapPanel != null) mapPanel.repaint();
    }

    // --- RENK SEÇİMİ (KESİN AYRIM - endsWith ile çözüm) ---
    private Color getBusColor(String vehicleId) {
        // "BUS-1A" -> A ile bitiyor
        if (vehicleId.endsWith("A")) return new Color(255, 0, 255); // Magenta (Tip A)
        if (vehicleId.endsWith("B")) return new Color(255, 140, 0); // Dark Orange (Tip B)
        if (vehicleId.endsWith("C")) return new Color(0, 255, 255); // Cyan (Tip C)
        return Color.YELLOW;
    }

    // --- HARİTA PANELİ ---
    private class MapPanel extends JPanel {
        private final int BASE_LANE_OFFSET = 7;

        public MapPanel() { setBackground(new Color(30, 30, 30)); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double virtualWidth = 1250.0;
            double virtualHeight = 900.0;
            double panelWidth = getWidth();
            double panelHeight = getHeight();
            double scale = Math.min(panelWidth / virtualWidth, panelHeight / virtualHeight);
            double translateX = (panelWidth - (virtualWidth * scale)) / 2;
            double translateY = (panelHeight - (virtualHeight * scale)) / 2;

            g2.translate(translateX, translateY);
            g2.scale(scale, scale);

            // 1. YOLLAR
            g2.setStroke(new BasicStroke(2));

            for (int id : graph.adjList.keySet()) {
                Node n1 = graph.nodes.get(id);
                for (Edge e : graph.adjList.get(id)) {
                    Node n2 = e.target;
                    // Normal yollar
                    double[] offsets = calculateOffset(n1.x, n1.y, n2.x, n2.y, BASE_LANE_OFFSET);
                    int x1 = (int) (n1.x + offsets[0]);
                    int y1 = (int) (n1.y + offsets[1]);
                    int x2 = (int) (n2.x + offsets[0]);
                    int y2 = (int) (n2.y + offsets[1]);

                    g2.setColor(new Color(100, 100, 100));
                    g2.drawLine(x1, y1, x2, y2);

                    g2.setColor(new Color(120, 120, 120));
                    g2.fillOval((x1+x2)/2 - 1, (y1+y2)/2 - 1, 3, 3);
                }
            }

            // --- OTOBÜS ROTALARI (YAN YANA ÇİZİM) ---
            if ("BUS_DRIVER".equals(currentRole)) {
                g2.setStroke(new BasicStroke(2));
                for (Vehicle v : engine.vehicles) {
                    if (v.type == VehicleType.BUS) {
                        Color busColor = getBusColor(v.id);
                        g2.setColor(new Color(busColor.getRed(), busColor.getGreen(), busColor.getBlue(), 200));

                        // Şerit Kaydırma (Offset): endsWith kullanarak
                        double specificOffset;
                        if (v.id.endsWith("A")) {
                            specificOffset = 6.0;
                        } else if (v.id.endsWith("B")) {
                            specificOffset = 10.0;
                        } else if (v.id.endsWith("C")) {
                            specificOffset = 14.0;
                        } else {
                            specificOffset = 14.0;
                        }

                        if (v.path != null && v.path.size() > 1) {
                            for (int i = 0; i < v.path.size() - 1; i++) {
                                Node n1 = v.path.get(i);
                                Node n2 = v.path.get(i+1);

                                double[] offsets = calculateOffset(n1.x, n1.y, n2.x, n2.y, specificOffset);
                                g2.drawLine((int)(n1.x + offsets[0]), (int)(n1.y + offsets[1]),
                                        (int)(n2.x + offsets[0]), (int)(n2.y + offsets[1]));
                            }
                        }
                    }
                }
            }

            // 2. NODE'LAR VE IŞIKLAR
            for (Node n : graph.nodes.values()) {
                switch (n.type) {
                    case INTERSECTION:
                        g2.setColor(new Color(60, 60, 70));
                        g2.fillOval(n.x - 12, n.y - 12, 24, 24);

                        boolean nsGreen = n.trafficLight.northSouthGreen;

                        g2.setColor(nsGreen ? Color.GREEN : Color.RED);
                        g2.fillOval(n.x - 4, n.y - 16, 8, 8);
                        g2.fillOval(n.x - 4, n.y + 8, 8, 8);

                        g2.setColor(nsGreen ? Color.RED : Color.GREEN);
                        g2.fillOval(n.x - 16, n.y - 4, 8, 8);
                        g2.fillOval(n.x + 8, n.y - 4, 8, 8);
                        break;

                    case APARTMENT:
                        g2.setColor(new Color(200, 100, 0));
                        g2.fillRect(n.x - 8, n.y - 8, 15, 15);
                        break;
                    case PARKING:
                        g2.setColor(new Color(200, 50, 150));
                        g2.fillRect(n.x - 12, n.y - 8, 25, 15);
                        break;
                    case POLICE: case HOSPITAL: case FIRE_STATION:
                        g2.setColor(new Color(50, 180, 50));
                        g2.fillRect(n.x - 12, n.y - 12, 25, 25);
                        break;
                }
                g2.setColor(Color.lightGray);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                if(n.type != NodeType.INTERSECTION) {
                    g2.drawString(n.name, n.x - 10, n.y + 20);
                }
            }

            // 3. ARAÇLAR
            for (Vehicle v : engine.vehicles) {
                if (v.next == null) continue;
                // Herhangi bir filtre koymuyoruz, tüm araçlar herkes tarafından görülsün.
                // Sadece Bus Driver isek rotalar görünür, ama otobüsler hep görünür.

                double specificOffset = BASE_LANE_OFFSET;
                if (v.type == VehicleType.BUS) {
                    if (v.id.endsWith("A")) specificOffset = 6.0;
                    else if (v.id.endsWith("B")) specificOffset = 10.0;
                    else if (v.id.endsWith("C")) specificOffset = 14.0;
                    else specificOffset = 14.0;
                }

                double curLineX = v.current.x + (v.next.x - v.current.x) * v.progress;
                double curLineY = v.current.y + (v.next.y - v.current.y) * v.progress;

                double[] offsets = calculateOffset(v.current.x, v.current.y, v.next.x, v.next.y, specificOffset);
                int drawX = (int) (curLineX + offsets[0]);
                int drawY = (int) (curLineY + offsets[1]);

                Color vehicleColor;
                switch (v.type) {
                    case AMBULANCE: vehicleColor = Color.RED; break;
                    case POLICE_CAR: vehicleColor = Color.BLUE; break;
                    case FIRE_TRUCK: vehicleColor = Color.ORANGE; break;
                    case BUS: vehicleColor = getBusColor(v.id); break;
                    default: vehicleColor = Color.YELLOW; break;
                }

                int vWidth = 16; int vHeight = 10;
                double angle = Math.atan2(v.next.y - v.current.y, v.next.x - v.current.x);
                AffineTransform old = g2.getTransform();
                g2.translate(drawX, drawY);
                g2.rotate(angle);

                g2.setColor(vehicleColor);
                g2.fillRoundRect(-vWidth/2, -vHeight/2, vWidth, vHeight, 4, 4);
                g2.setColor(Color.lightGray);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(-vWidth/2, -vHeight/2, vWidth, vHeight, 4, 4);

                g2.rotate(-angle);
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                g2.setColor(Color.WHITE);
                g2.drawString(v.id, -10, -8);

                g2.setTransform(old);
            }
        }

        private double[] calculateOffset(int x1, int y1, int x2, int y2, double offsetAmount) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist == 0) return new double[]{0, 0};

            double uX = dx / dist;
            double uY = dy / dist;
            return new double[]{-uY * offsetAmount, uX * offsetAmount};
        }
    }
}