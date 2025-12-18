import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrafficSimulatorGUI extends JFrame {
    private CityTrafficSystem city;
    private JTextArea displayArea;
    private Timer timer;

    public TrafficSimulatorGUI() {
        city = new CityTrafficSystem();
        initializeCity(); // Build the graph

        setTitle("Smart City Traffic Control");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Top Panel: Controls ---
        JPanel controlPanel = new JPanel();
        JButton btnAddCar = new JButton("Add Car @ A");
        JButton btnAddAmb = new JButton("Add Ambulance @ A");
        JButton btnToggleLight = new JButton("Toggle Light @ A");

        controlPanel.add(btnAddCar);
        controlPanel.add(btnAddAmb);
        controlPanel.add(btnToggleLight);
        add(controlPanel, BorderLayout.NORTH);

        // --- Center Panel: Visualization ---
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        // --- Event Listeners ---
        btnAddCar.addActionListener(e -> {
            city.getIntersection("A").addVehicle(new Car("Car-" + System.currentTimeMillis() % 1000));
            updateDisplay();
        });

        btnAddAmb.addActionListener(e -> {
            city.getIntersection("A").addVehicle(new Ambulance("AMB-" + System.currentTimeMillis() % 1000));
            updateDisplay();
        });

        btnToggleLight.addActionListener(e -> {
            Intersection i = city.getIntersection("A");
            // Toggle logic just for simulation
            boolean current = i.getStatus().contains("GREEN");
            i.setTrafficLight(!current);
            updateDisplay();
        });

        // --- Simulation Loop (Timer) ---
        // Runs every 1 second to process traffic
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateTraffic();
                updateDisplay();
            }
        });
        timer.start();
    }

    private void initializeCity() {
        // Create a simple graph: A -> B -> C
        city.addIntersection("A");
        city.addIntersection("B");
        city.addIntersection("C");
        city.addRoad("A", "B", 10);
        city.addRoad("B", "C", 15);
    }

    private void simulateTraffic() {
        for (Intersection i : city.getAllIntersections().values()) {
            Vehicle v = i.processVehicle();
            if (v != null) {
                System.out.println("Processed: " + v);
            }
        }
    }

    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- LIVE TRAFFIC STATUS ---\n\n");
        for (Intersection i : city.getAllIntersections().values()) {
            sb.append(i.getStatus()).append("\n\n");
        }
        displayArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TrafficSimulatorGUI().setVisible(true);
        });
    }
}