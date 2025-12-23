package gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import models.*;
import simulation.SimulationEngine;

// The main application entry point (JFrame). Manages the transition between the Login Screen and the Simulation Screen.
public class SmartCityTraffic extends JFrame {
    private CityGraph cityGraph;
    private SimulationEngine engine;
    private JPanel mainContainer;
    private CardLayout cardLayout;

    public SmartCityTraffic() {
        setTitle("Smart City Traffic Control System");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Full Screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Start the System
        cityGraph = new CityGraph();
        engine = new SimulationEngine(cityGraph);

        // Setup main container with CardLayout to switch between views
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        
        JPanel loginPanel = createLoginPanel();

        // Initialize SimulationPanel with a Logout callback function
        SimulationPanel simPanel = new SimulationPanel(cityGraph, engine, () -> {
            engine.resetTraffic(); // 
            cardLayout.show(mainContainer, "LOGIN");
        });

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(simPanel, "SIMULATION");

        add(mainContainer);

        // Link engine to UI and prepare initial data
        engine.setPanelToRefresh(simPanel);
        engine.initializeTraffic(); // 
        engine.start(); // 
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

        // Login Logic
        loginBtn.addActionListener(e -> {
            String role = (String) roleCombo.getSelectedItem();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword());

            if (role.equals("Free View")) {
                engine.resetTraffic();
                engine.spawnBusRoute("sys");
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
                engine.resetTraffic();
                engine.spawnBusRoute("sys");
                engine.setCurrentUser(userRole, email);
                cardLayout.show(mainContainer, "SIMULATION");
            } else {
                msgLabel.setText("Invalid User ID format!");
            }
        });

        return panel;
    }

    
}