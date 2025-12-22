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

    public SmartCityTraffic() {
        setTitle("Smart City Traffic Control System");
        setSize(1280, 960);
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

    @Override
    public String toString() { return name; }
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

    //Look      ~Yunus
    public double getCurrentWeight() {
        return baseWeight + (vehicleCount * 0.5);
    }
}

class TrafficLight {
    boolean northSouthGreen = true;
    int greenDuration = 30;
    int timer = 0;

    //What timer is represents?     ~Yunus
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
        boolean isNorthSouthRoad = (from.id % 2 != 0);
        return isNorthSouthRoad == northSouthGreen;
    }
}

class Vehicle {
    String id;
    VehicleType type;
    Node current, next, destination;
    List<Node> path;
    double progress = 0;

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
        /*int startX = 150, startY = 100, gap = 140;
        for (int i = 1; i <= 18; i++) {
            int row = (i - 1) / 6;
            int col = (i - 1) % 6;
            addNode(i, "INTR" + i, NodeType.INTERSECTION, startX + col * gap, startY + row * gap);
        }*/

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
                {100, 50}, {100, 500}, {270, 50}, {265, 360}, {270, 840},
                {410, 50}, {460, 360}, {360, 450}, {410, 840}, {560, 50},
                {510, 210}, {510, 360}, {720, 400}, {830, 840}, {1150, 640}
                /* 12(51), 11(52), 13(53), 18(54), 10(55),
                   14(56), 17(57), 7(58),  8(59),  15(60),
                   16(61), 1(62),  5(63),  9(64),  3(65)
                */
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
        /*
        for(int i=51; i<=65; i++) {
            int target = (i - 50);
            if(target > 18) target = 18;
            addEdge(i, target, 0.5);
            addEdge(target, i, 0.5);
        }*/
        //APARTMENT
        addEdge(51,12,0.5);
        addEdge(52,11,0.5);
        addEdge(53,13,0.5);
        addEdge(54,18,0.5);
        addEdge(55,10,0.5);
        addEdge(56,14,0.5);
        addEdge(57,17,0.5);
        addEdge(58,7,0.5);
        addEdge(59,8,0.5);
        addEdge(60,15,0.5);
        addEdge(61,16,0.5);
        addEdge(62,1,0.5);
        addEdge(63,5,0.5);
        addEdge(64,9,0.5);
        addEdge(65,3,0.5);

        //PARKING
        addEdge(71, 2, 0.5);
        addEdge(72, 18, 0.5);
        addEdge(73, 9, 0.2);
        //POLICE-HOSPITAL-FIRE_STATION
        addEdge(81, 6, 0.5);
        addEdge(82, 10, 3.4);
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

    public List<Node> findPath(Node start, Node end) {
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Node> previous = new HashMap<>();

        // --- FIX: Comparator DÜZELTİLDİ ---
        // Eski hatalı kod: Comparator.comparingDouble(distances::get)
        // Yeni doğru kod: n -> distances.get(n.id)
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(n -> distances.get(n.id)));

        for (int id : graph.nodes.keySet()) {
            distances.put(id, Double.MAX_VALUE);
        }
        distances.put(start.id, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current == end) break;

            if (distances.get(current.id) == Double.MAX_VALUE) break;

            for (Edge edge : graph.adjList.get(current.id)) {
                double newDist = distances.get(current.id) + edge.getCurrentWeight();
                if (newDist < distances.get(edge.target.id)) {
                    distances.put(edge.target.id, newDist);
                    previous.put(edge.target.id, current);

                    // PriorityQueue'yu güncellemek için silip ekliyoruz (Java PQ için basit çözüm)
                    queue.remove(edge.target);
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

        if (path.isEmpty() || path.get(0) != start) return null;

        return path;
    }

    public boolean spawnVehicle(Node start, Node end, VehicleType type) {
        List<Node> path = findPath(start, end);
        if (path != null) {
            String id = type.toString().substring(0, 3) + (carIdCounter++);
            Vehicle v = new Vehicle(id, type, start, end, path);
            vehicles.add(v);
            return true;
        }
        return false;
    }


    //Just 1 bus?
    private void spawnBusRoute(String driverId) {
        List<Node> route = new ArrayList<>();
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

                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void moveVehicle(Vehicle v) {
        if (v.path.isEmpty() || v.next == null) return;

        if (v.current.type == NodeType.INTERSECTION && v.progress < 0.1) {
            TrafficLight light = v.current.trafficLight;
            boolean isEmergency = (v.type == VehicleType.AMBULANCE || v.type == VehicleType.POLICE_CAR || v.type == VehicleType.FIRE_TRUCK);

            int curIdx = v.path.indexOf(v.current);
            Node prevNode = (curIdx > 0) ? v.path.get(curIdx - 1) : v.current;

            if (!isEmergency && !light.canPass(prevNode, v.next)) {
                return;
            }
        }

        //0.005
        v.progress += 0.05;

        if (v.progress >= 1.0) {
            v.progress = 0;
            v.current = v.next;
            int nextIdx = v.path.indexOf(v.current) + 1;
            if (nextIdx < v.path.size()) {
                v.next = v.path.get(nextIdx);
            } else {
                v.next = null;
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

//--- GÖRSELLEŞTİRME PANELİ ---

class SimulationPanel extends JPanel {
 private CityGraph graph;
 private SimulationEngine engine;
 
 // Panelleri ayırıyoruz
 private JPanel controlPanel; // Alt bar
 private MapPanel mapPanel;   // Harita çizim alanı (Inner Class)
 
 // Kontrol elemanları
 private JComboBox<Node> startBox, endBox;
 private JLabel statusLabel;

 public SimulationPanel(CityGraph graph, SimulationEngine engine) {
     this.graph = graph;
     this.engine = engine;
     
     // 1. Ana Panel Ayarları
     setLayout(new BorderLayout()); // Ekrana yayıl
     
     // 2. Alt Kontrol Paneli (Control Panel) Oluşturma
     initControlPanel();
     
     // 3. Harita Paneli (Map Panel) Oluşturma
     mapPanel = new MapPanel();
     
     // 4. Yerleşim (Layout)
     add(mapPanel, BorderLayout.CENTER); // Harita ortada kalan her yeri kaplar
     add(controlPanel, BorderLayout.SOUTH); // Kontrol paneli en alta yapışır
 }

 private void initControlPanel() {
     controlPanel = new JPanel();
     controlPanel.setBackground(new Color(50, 50, 50));
     
     // GridBagLayout ile öğeleri tam ortalıyoruz
     controlPanel.setLayout(new GridBagLayout());
     // Yükseklik 60px sabit, Genişlik esnek (0)
     controlPanel.setPreferredSize(new Dimension(0, 60));

     GridBagConstraints gbc = new GridBagConstraints();
     gbc.insets = new Insets(0, 10, 0, 10); // Elemanlar arası boşluk

     startBox = new JComboBox<>();
     endBox = new JComboBox<>();
     statusLabel = new JLabel("Ready");
     statusLabel.setForeground(Color.GREEN);
     statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

     // Node'ları sıralayıp ekle
     List<Node> sortedNodes = new ArrayList<>(graph.nodes.values());
     sortedNodes.sort((n1, n2) -> {

         // 1. KATEGORİ ÖNCELİĞİNE BAK
         int p1 = getNodePriority(n1.name);
         int p2 = getNodePriority(n2.name);

         if (p1 != p2) {
             return Integer.compare(p1, p2);
         }

         // 2. KATEGORİ AYNIYSA DOĞAL SIRALAMA (Natural Sort)
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
     // ----------------------------------

     for(Node n : sortedNodes) {
         if(n.type == NodeType.INTERSECTION) {continue;}
         startBox.addItem(n);
         endBox.addItem(n);
     }

     JButton goBtn = new JButton("Start Journey");
     goBtn.setBackground(new Color(0, 150, 0));
     goBtn.setForeground(Color.darkGray);
     goBtn.setFocusPainted(false);
     goBtn.setFont(new Font("Arial", Font.BOLD, 12));

     goBtn.addActionListener(e -> {
         Node s = (Node) startBox.getSelectedItem();
         Node d = (Node) endBox.getSelectedItem();
         VehicleType type = VehicleType.CAR;

         if ("EMERGENCY".equals(engine.currentUserRole)) {
             type = VehicleType.AMBULANCE;
         }

         boolean spawned = engine.spawnVehicle(s, d, type);
         if(spawned) {
             statusLabel.setText("Vehicle Spawned: " + s.name + " -> " + d.name);
             statusLabel.setForeground(Color.GREEN);
         } else {
             statusLabel.setText("NO PATH FOUND from " + s.name + " to " + d.name);
             statusLabel.setForeground(Color.RED);
         }
     });

     JLabel lblS = new JLabel("Start:"); lblS.setForeground(Color.WHITE);
     JLabel lblE = new JLabel("End:"); lblE.setForeground(Color.WHITE);

     // Elemanları ekle
     controlPanel.add(lblS, gbc);
     controlPanel.add(startBox, gbc);
     controlPanel.add(lblE, gbc);
     controlPanel.add(endBox, gbc);
     controlPanel.add(goBtn, gbc);
     
     gbc.insets = new Insets(0, 30, 0, 0); // Status için ekstra boşluk
     controlPanel.add(statusLabel, gbc);
 }

 public void enableControls(String role) {
     // Ana panel içinde kontrol panelini görünür yapıyoruz
     controlPanel.setVisible(true);
     if (role.equals("BUS_DRIVER") || role.equals("FREE_VIEW")) {
         startBox.setEnabled(false);
         endBox.setEnabled(false);
         statusLabel.setText("Monitoring Mode: " + role);
     }
     // Paneli yeniden çiz ki layout güncellensin
     revalidate();
     repaint();
 }
 
//--- SIRALAMA İÇİN YARDIMCI METOT ---
 private int getNodePriority(String name) {
     // 1. Grup: Apartmanlar (APT...)
     if (name.startsWith("APT")) return 1;

     // 2. Grup: Kamu Binaları (HOSP, FIRE, POLICE)
     if (name.equals("HOSP") || name.equals("POLICE") || name.equals("FIRE")) return 2;

     // 3. Grup: Otoparklar (P1, P2...) - HOSP'tan sonra gelir
     if (name.startsWith("P") && name.matches("P\\d+")) return 3;

     // Diğerleri en sona
     return 4;
 }
 // ---------------------------------------

 // Ana repaint çağrıldığında haritayı da tetikler
 @Override
 public void repaint() {
     super.repaint();
     if(mapPanel != null) mapPanel.repaint();
 }

 // --- HARİTA ÇİZİM PANELİ (INNER CLASS) ---
 // Bu sınıf sadece haritayı çizer, butonlarla işi yoktur.
 private class MapPanel extends JPanel {
     
     public MapPanel() {
         setBackground(new Color(30, 30, 30));
     }

     @Override
     protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D) g;
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

         // --- SCALE MANTIĞI ---
         double virtualWidth = 1250.0;
         double virtualHeight = 900.0;

         // Artık alt barı çıkarmamıza gerek yok çünkü bu panel (MapPanel)
         // zaten CENTER'da duruyor ve alt barın alanına karışmıyor.
         // Kendisine ayrılan alanın tamamını kullanabilir.
         double panelWidth = getWidth();
         double panelHeight = getHeight();

         double scaleX = panelWidth / virtualWidth;
         double scaleY = panelHeight / virtualHeight;
         double scale = Math.min(scaleX, scaleY);

         double translateX = (panelWidth - (virtualWidth * scale)) / 2;
         double translateY = (panelHeight - (virtualHeight * scale)) / 2;

         g2.translate(translateX, translateY);
         g2.scale(scale, scale);
         
         // --- ÇİZİM ---
         
         // Yollar
         g2.setColor(new Color(80, 80, 80));
         g2.setStroke(new BasicStroke(4));
         for (int id : graph.adjList.keySet()) {
             Node n1 = graph.nodes.get(id);
             for (Edge e : graph.adjList.get(id)) {
                 Node n2 = e.target;
                 g2.drawLine(n1.x, n1.y, n2.x, n2.y);
                 int midX = (n1.x + n2.x) / 2;
                 int midY = (n1.y + n2.y) / 2;
                 g2.fillOval(midX-2, midY-2, 4, 4);
             }
         }

         // Node'lar
         for (Node n : graph.nodes.values()) {
             switch (n.type) {
                 case INTERSECTION:
                     g2.setColor(new Color(0, 100, 200));
                     g2.fillOval(n.x - 12, n.y - 12, 24, 24);
                     if(n.trafficLight.northSouthGreen) g2.setColor(Color.GREEN); else g2.setColor(Color.RED);
                     g2.fillOval(n.x - 4, n.y - 4, 8, 8);
                     break;
                 case APARTMENT:
                     g2.setColor(new Color(200, 100, 0));
                     if(n.id==54) g2.fillRect(n.x - 30, n.y - 8, 15, 15);
                     else g2.fillRect(n.x - 8, n.y - 8, 15, 15);
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
             g2.setColor(Color.LIGHT_GRAY);
             g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
             g2.drawString(n.name, n.x + 14, n.y + 5);
         }

         // Araçlar
         for (Vehicle v : engine.vehicles) {
             if (v.next == null) continue;
             int curX = v.current.x; int curY = v.current.y;
             int nextX = v.next.x; int nextY = v.next.y;
             int drawX = (int) (curX + (nextX - curX) * v.progress);
             int drawY = (int) (curY + (nextY - curY) * v.progress);

             Color vehicleColor;
             if (v.type == VehicleType.CAR) vehicleColor = Color.YELLOW;
             else if (v.type == VehicleType.BUS) vehicleColor = Color.CYAN;
             else vehicleColor = Color.RED;

             int vWidth = 20; int vHeight = 14;
             g2.setColor(vehicleColor);
             g2.fillRoundRect(drawX - vWidth/2, drawY - vHeight/2, vWidth, vHeight, 6, 6);
             g2.setColor(Color.BLACK);
             g2.setStroke(new BasicStroke(1));
             g2.drawRoundRect(drawX - vWidth/2, drawY - vHeight/2, vWidth, vHeight, 6, 6);

             g2.setFont(new Font("Arial", Font.BOLD, 10));
             FontMetrics fm = g2.getFontMetrics();
             int idWidth = fm.stringWidth(v.id);
             g2.drawString(v.id, drawX - idWidth/2, drawY - 8);

             g2.setFont(new Font("Arial", Font.PLAIN, 10));
             g2.setColor(Color.WHITE);
             String destText = "> " + v.destination.name;
             int destWidth = fm.stringWidth(destText);
             g2.drawString(destText, drawX - destWidth/2, drawY + vHeight + 10);
         }
     }
 }
}