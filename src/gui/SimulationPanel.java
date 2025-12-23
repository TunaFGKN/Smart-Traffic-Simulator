package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import models.*;
import simulation.SimulationEngine;

public class SimulationPanel extends JPanel {
    public CityGraph graph;
    private SimulationEngine engine;

    private JPanel controlPanel;
    private MapPanel mapPanel;

    private JComboBox<Node> startBox, endBox;
    private JLabel statusLabel;

    private Runnable onLogout;

    public SimulationPanel(CityGraph graph, SimulationEngine engine, Runnable onLogout) {
        this.graph = graph;
        this.engine = engine;
        this.onLogout = onLogout;

        setLayout(new BorderLayout());

        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(50, 50, 50));
        controlPanel.setPreferredSize(new Dimension(0, 60));

        mapPanel = new MapPanel(graph, engine);

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
        controlPanel.setVisible(true);
        
        if(mapPanel != null) {
            mapPanel.setCurrentRole(role);
        }

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

    public Color getBusColor(String vehicleId) {
        if (vehicleId.endsWith("A")) return new Color(255, 0, 255);
        if (vehicleId.endsWith("B")) return new Color(255, 140, 0);
        if (vehicleId.endsWith("C")) return new Color(0, 255, 255);
        return Color.YELLOW;
    }

    
}