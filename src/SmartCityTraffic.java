import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// --- ANA SINIF ---
public class SmartCityTraffic extends JFrame {
    private CityGraph cityGraph;
    private SimulationEngine engine;
    private JPanel mainContainer;
    private CardLayout cardLayout;

    // Kullanıcı Oturumu
    private String currentUserRole = "";
    private String currentUserId = "";

    public SmartCityTraffic() {
        setTitle("Smart City Traffic Control System");
        setSize(1200, 850); // Biraz daha büyüttük
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Sistemi Başlat
        cityGraph = new CityGraph();
        engine = new SimulationEngine(cityGraph);

        // GUI Ayarları
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Panelleri Oluştur
        JPanel loginPanel = createLoginPanel();
        SimulationPanel simPanel = new SimulationPanel(cityGraph, engine);
        
        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(simPanel, "SIMULATION");

        add(mainContainer);
        
        // Motoru panele bağla ve başlat
        engine.setPanelToRefresh(simPanel);
        engine.start();
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 44, 52));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("City Traffic Login", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
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
        loginBtn.setBackground(new Color(0, 120, 215));
        loginBtn.setForeground(Color.WHITE);
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
                currentUserRole = "FREE_VIEW";
                engine.setInteractMode(false, false);
                cardLayout.show(mainContainer, "SIMULATION");
                return;
            }

            if (!pass.equals("1234")) {
                msgLabel.setText("Invalid Password!");
                return;
            }

            boolean valid = false;
            if (role.startsWith("Personal") && email.matches("cardriver\\d+@example\\.com")) {
                valid = true;
                currentUserRole = "CAR_DRIVER";
            } else if (role.startsWith("Bus") && email.matches("busdriver\\d+@example\\.com")) {
                valid = true;
                currentUserRole = "BUS_DRIVER";
            } else if (role.startsWith("Emergency") && email.matches("emergency\\d+@example\\.com")) {
                valid = true;
                currentUserRole = "EMERGENCY";
            }

            if (valid) {
                currentUserId = email;
                engine.setCurrentUser(currentUserRole, currentUserId);
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
enum VehicleType { CAR, BUS, AMBULANCE, POLICE_CAR, FIRE_TRUCK }

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

    // Dropdownlarda ismin düzgün görünmesi için bu eklendi:
    @Override
    public String toString() {
        return name;
    }
}

class Edge {
    Node target;
    double baseWeight;
    int vehicleCount; 

    public Edge(Node target, double weight) {
        this.target = target;
        this.baseWeight = weight;
        this.vehicleCount = 0;
    }

    public double getCurrentWeight() {
        return baseWeight + (vehicleCount * 0.5);
    }
}

class TrafficLight {
    boolean northSouthGreen = true;
    int greenDuration = 30;
    int timer = 0;

    public void update(int nsQueue, int ewQueue) {
        timer++;
        int currentLimit = greenDuration;
        if (northSouthGreen && nsQueue > 10) currentLimit += 20;
        if (!northSouthGreen && ewQueue > 10) currentLimit += 20;

        if (timer > currentLimit) {
            northSouthGreen = !northSouthGreen;
            timer = 0;
        }
    }
    
    public boolean canPass(Node from, Node to) {
        // Basit yön mantığı (Grid yapısına göre tek/çift ID kontrolü simülasyonu)
        boolean isNorthSouthRoad = (from.id % 2 != 0); 
        return isNorthSouthRoad == northSouthGreen;
    }
}

class Vehicle {
    String id;
    VehicleType type;
    Node current, next, destination;
    List<Node> path;
    double progress = 0; // 0.0 ile 1.0 arası

    // Speed değişkeni kaldırıldı.

    public Vehicle(String id, VehicleType type, Node start, Node dest, List<Node> path) {
        this.id = id;
        this.type = type;
        this.current = start;
        this.destination = dest;
        this.path = path;
        if (path.size() > 1) this.next = path.get(1);
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
        // Grid oluştur (18 Kavşak)
        int startX = 150, startY = 100, gap = 140;
        for (int i = 1; i <= 18; i++) {
            int row = (i - 1) / 6;
            int col = (i - 1) % 6;
            addNode(i, "INTR" + i, NodeType.INTERSECTION, startX + col * gap, startY + row * gap);
        }

        // Apartmanlar
        int[][] aptCoords = {
            {50, 50}, {300, 50}, {550, 50}, {800, 50}, {950, 150},
            {950, 300}, {950, 450}, {800, 600}, {550, 600}, {300, 600},
            {50, 600}, {50, 400}, {50, 250}, {250, 200}, {650, 200}
        };
        for (int i = 0; i < 15; i++) {
            addNode(51 + i, "APT" + (i + 1), NodeType.APARTMENT, aptCoords[i][0], aptCoords[i][1]);
        }

        // Otoparklar
        addNode(71, "P1", NodeType.PARKING, 250, 80);
        addNode(72, "P2", NodeType.PARKING, 850, 450);
        addNode(73, "P3", NodeType.PARKING, 450, 380);

        // Özel Binalar
        addNode(81, "POLICE", NodeType.POLICE, 700, 150);
        addNode(82, "HOSP", NodeType.HOSPITAL, 800, 150);
        addNode(83, "FIRE", NodeType.FIRE_STATION, 450, 150);
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

    private void initializeConnections() {
        // Prompt'taki adjacency list verileri
        addEdge(1, 2, 2.7); addEdge(1, 6, 0.9); addEdge(1, 16, 1.5); addEdge(1, 51, 0.5);
        addEdge(2, 1, 2.7); addEdge(2, 4, 2.8); addEdge(2, 16, 3.1); addEdge(2, 71, 0.5);
        addEdge(3, 4, 3.0); addEdge(3, 52, 0.5); addEdge(3, 83, 0.5);
        addEdge(4, 2, 2.8); addEdge(4, 3, 3.0); addEdge(4, 5, 1.6); addEdge(4, 9, 1.5);
        addEdge(5, 4, 1.6); addEdge(5, 6, 1.6); addEdge(5, 53, 0.5);
        addEdge(6, 5, 1.6); addEdge(6, 7, 1.6); addEdge(6, 1, 0.9); addEdge(6, 81, 0.5);
        addEdge(7, 6, 1.6); addEdge(7, 8, 0.4); addEdge(7, 17, 0.9); addEdge(7, 54, 0.5);
        addEdge(8, 7, 0.4); addEdge(8, 9, 4.0); addEdge(8, 10, 1.4); addEdge(8, 55, 0.5);
        addEdge(9, 4, 1.5); addEdge(9, 8, 4.0); addEdge(9, 56, 0.5); addEdge(9, 73, 0.5);
        addEdge(10, 8, 1.4); addEdge(10, 82, 3.5); addEdge(10, 57, 0.5);
        addEdge(11, 12, 5.0); addEdge(11, 82, 1.5); addEdge(11, 58, 0.5);
        addEdge(12, 11, 5.0); addEdge(12, 13, 1.7); addEdge(12, 59, 0.5);
        addEdge(13, 12, 1.7); addEdge(13, 14, 1.4); addEdge(13, 60, 0.5);
        addEdge(14, 13, 1.4); addEdge(14, 17, 2.6); addEdge(14, 15, 1.5); addEdge(14, 61, 0.5);
        addEdge(15, 16, 1.1); addEdge(15, 14, 1.5); addEdge(15, 62, 0.5);
        addEdge(16, 1, 1.5); addEdge(16, 2, 3.1); addEdge(16, 15, 1.1); addEdge(16, 63, 0.5);
        addEdge(17, 7, 0.9); addEdge(17, 18, 1.3); addEdge(17, 14, 2.6); addEdge(17, 64, 0.5);
        addEdge(18, 17, 1.3); addEdge(18, 65, 0.5); addEdge(18, 72, 0.5);

        // Apartman Bağlantıları (Basitleştirilmiş Çift Yönlü)
        for(int i=51; i<=65; i++) {
             // Mantıksal en yakın kavşağa bağlama (1-18 arası)
             int target = (i - 50); 
             if(target > 18) target = 18; // Index out of bound koruması
             addEdge(i, target, 0.5); 
             addEdge(target, i, 0.5); 
        }
        
        // Özel Bağlantılar
        addEdge(71, 2, 0.5); addEdge(2, 71, 0.5);
        addEdge(72, 18, 0.5); addEdge(18, 72, 0.5);
        addEdge(73, 9, 0.2); addEdge(9, 73, 0.2);
        addEdge(81, 6, 0.5); addEdge(6, 81, 0.5);
        addEdge(82, 10, 0.5); addEdge(10, 82, 0.5);
        addEdge(83, 3, 0.5); addEdge(3, 83, 0.5);
    }
}

// --- SİMÜLASYON MOTORU ---

class SimulationEngine extends Thread {
    CityGraph graph;
    List<Vehicle> vehicles = new CopyOnWriteArrayList<>();
    SimulationPanel panel;
    String currentUserRole;
    String currentUserId;
    int carIdCounter = 1;

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
        
        if(role.equals("BUS_DRIVER")) {
            spawnBusRoute(id);
        }
    }

    public void setInteractMode(boolean b, boolean c) {
        panel.enableControls("FREE_VIEW");
    }

    public List<Node> findPath(Node start, Node end) {
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Node> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (int id : graph.nodes.keySet()) {
            distances.put(id, Double.MAX_VALUE);
        }
        distances.put(start.id, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current == end) break;

            for (Edge edge : graph.adjList.get(current.id)) {
                double newDist = distances.get(current.id) + edge.getCurrentWeight();
                if (newDist < distances.get(edge.target.id)) {
                    distances.put(edge.target.id, newDist);
                    previous.put(edge.target.id, current);
                    queue.add(edge.target);
                }
            }
        }

        List<Node> path = new ArrayList<>();
        Node curr = end;
        while (curr != null) {
            path.add(0, curr);
            curr = previous.get(curr.id);
        }
        return path.size() > 0 && path.get(0) == start ? path : null;
    }

    public void spawnVehicle(Node start, Node end, VehicleType type) {
        List<Node> path = findPath(start, end);
        if (path != null) {
            String id = type.toString().substring(0, 3) + (carIdCounter++);
            Vehicle v = new Vehicle(id, type, start, end, path);
            vehicles.add(v);
        }
    }
    
    private void spawnBusRoute(String driverId) {
        List<Node> route = new ArrayList<>();
        // Rota (P3 -> 9 -> 8 -> 10 -> HPT1 ...)
        int[] ids = {73, 9, 8, 10, 82, 11, 12, 13, 14, 17, 7, 8, 9, 73};
        for(int id : ids) {
             if(graph.nodes.containsKey(id)) route.add(graph.nodes.get(id));
        }
        
        if(!route.isEmpty()) {
            Vehicle v = new Vehicle("BUS-" + driverId, VehicleType.BUS, route.get(0), route.get(route.size()-1), route);
            vehicles.add(v);
        }
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
                
                // YAVAŞLATILDI: 100ms uyku (Daha yavaş akış)
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void moveVehicle(Vehicle v) {
        if (v.path.isEmpty() || v.next == null) return;

        // Işık Kontrolü
        if (v.current.type == NodeType.INTERSECTION && v.progress < 0.1) {
            TrafficLight light = v.current.trafficLight;
            boolean isEmergency = (v.type == VehicleType.AMBULANCE || v.type == VehicleType.POLICE_CAR || v.type == VehicleType.FIRE_TRUCK);
            
            // Eğer ışık geçişe izin vermiyorsa bekle (return)
            if (!isEmergency && !light.canPass(v.path.get(Math.max(0, v.path.indexOf(v.current)-1)), v.next)) {
                return; 
            }
        }

        // HAREKET LOGİĞİ: Sabit ve yavaş artış (0.01)
        v.progress += 0.015; 

        if (v.progress >= 1.0) {
            v.progress = 0;
            v.current = v.next;
            int nextIdx = v.path.indexOf(v.current) + 1;
            if (nextIdx < v.path.size()) {
                v.next = v.path.get(nextIdx);
            } else {
                v.next = null; // Vardı
                vehicles.remove(v);
            }
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
}

// --- GÖRSELLEŞTİRME PANELİ ---

class SimulationPanel extends JPanel {
    CityGraph graph;
    SimulationEngine engine;
    JPanel controlPanel;
    JComboBox<Node> startBox, endBox;

    public SimulationPanel(CityGraph graph, SimulationEngine engine) {
        this.graph = graph;
        this.engine = engine;
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));

        controlPanel = new JPanel();
        controlPanel.setVisible(false);
        
        startBox = new JComboBox<>();
        endBox = new JComboBox<>();
        
        // Node'lar isme göre sıralı eklensin
        List<Node> sortedNodes = new ArrayList<>(graph.nodes.values());
        sortedNodes.sort(Comparator.comparing(n -> n.name));
        
        for(Node n : sortedNodes) {
            startBox.addItem(n);
            endBox.addItem(n);
        }

        JButton goBtn = new JButton("Start Journey");
        goBtn.addActionListener(e -> {
            Node s = (Node) startBox.getSelectedItem();
            Node d = (Node) endBox.getSelectedItem();
            VehicleType type = VehicleType.CAR;
            if (engine.currentUserRole.equals("EMERGENCY")) type = VehicleType.AMBULANCE;
            engine.spawnVehicle(s, d, type);
        });

        controlPanel.add(new JLabel("Start:"));
        controlPanel.add(startBox);
        controlPanel.add(new JLabel("End:"));
        controlPanel.add(endBox);
        controlPanel.add(goBtn);
        
        add(controlPanel, BorderLayout.SOUTH);
    }

    public void enableControls(String role) {
        controlPanel.setVisible(true);
        if (role.equals("BUS_DRIVER") || role.equals("FREE_VIEW")) {
            startBox.setEnabled(false);
            endBox.setEnabled(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Yolları Çiz
        g2.setColor(new Color(100, 100, 100)); // Daha açık gri
        g2.setStroke(new BasicStroke(3));
        for (int id : graph.adjList.keySet()) {
            Node n1 = graph.nodes.get(id);
            for (Edge e : graph.adjList.get(id)) {
                Node n2 = e.target;
                g2.drawLine(n1.x, n1.y, n2.x, n2.y);
            }
        }

        // Node'ları Çiz
        for (Node n : graph.nodes.values()) {
            switch (n.type) {
                case INTERSECTION:
                    g2.setColor(new Color(0, 150, 255)); // Mavi
                    g2.fillOval(n.x - 12, n.y - 12, 24, 24);
                    // Işık
                    if(n.trafficLight.northSouthGreen) g2.setColor(Color.GREEN); else g2.setColor(Color.RED);
                    g2.fillOval(n.x - 4, n.y - 4, 8, 8);
                    break;
                case APARTMENT:
                    g2.setColor(new Color(255, 140, 0)); // Turuncu
                    g2.fillRect(n.x - 10, n.y - 10, 20, 20);
                    break;
                case PARKING:
                    g2.setColor(new Color(255, 105, 180)); // Pembe
                    g2.fillRect(n.x - 12, n.y - 8, 24, 16);
                    break;
                case POLICE: case HOSPITAL: case FIRE_STATION:
                    g2.setColor(new Color(50, 205, 50)); // Yeşil
                    g2.fillRect(n.x - 12, n.y - 12, 24, 24);
                    break;
            }
            g2.setColor(Color.LIGHT_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(n.name, n.x + 14, n.y + 5);
        }

        // Araçları Çiz (Detaylı Görünüm)
        for (Vehicle v : engine.vehicles) {
            if (v.next == null) continue;
            
            // Konum Hesapla
            int curX = v.current.x;
            int curY = v.current.y;
            int nextX = v.next.x;
            int nextY = v.next.y;

            int drawX = (int) (curX + (nextX - curX) * v.progress);
            int drawY = (int) (curY + (nextY - curY) * v.progress);

            // Renk Seçimi
            Color vehicleColor;
            if (v.type == VehicleType.CAR) vehicleColor = Color.YELLOW;
            else if (v.type == VehicleType.BUS) vehicleColor = Color.CYAN;
            else vehicleColor = Color.RED;

            // Aracı Dikdörtgen (Kutu) Olarak Çiz
            int vWidth = 36;
            int vHeight = 20;
            g2.setColor(vehicleColor);
            g2.fillRoundRect(drawX - vWidth/2, drawY - vHeight/2, vWidth, vHeight, 5, 5);
            
            // Çerçeve
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(drawX - vWidth/2, drawY - vHeight/2, vWidth, vHeight, 5, 5);

            // Araç Bilgileri (Üzerine ve Altına Yazı)
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            
            // ID (Kutunun içine)
            FontMetrics fm = g2.getFontMetrics();
            int idWidth = fm.stringWidth(v.id);
            g2.drawString(v.id, drawX - idWidth/2, drawY + 3);

            // Destination (Kutunun altına)
            g2.setColor(Color.WHITE);
            String destText = "-> " + v.destination.name;
            int destWidth = fm.stringWidth(destText);
            g2.drawString(destText, drawX - destWidth/2, drawY + vHeight + 2);
        }
    }
}