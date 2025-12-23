package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import javax.swing.JPanel;
import models.*;
import simulation.SimulationEngine;

// Handles the graphical rendering of the simulation. It draws the map, roads, nodes, traffic lights, and vehicles. Supports zoom/scaling and role-based view filtering.
public class MapPanel extends JPanel {
    
	private final int BASE_LANE_OFFSET = 7; // Offset for drawing vehicles in lanes (to avoid overlap on bidirectional roads)
    private CityGraph graph;
    private SimulationEngine engine;
    private String currentRole = "";

    public MapPanel(CityGraph graph, SimulationEngine engine) { 
    	this.graph = graph;
        this.engine = engine;
    	setBackground(new Color(30, 30, 30)); 
    }
    
    // Updates the current user role to apply view filters (e.g., Bus Driver mode).
    public void setCurrentRole(String role) {
        this.currentRole = role;
        repaint();
    }
    
    private Color getBusColor(String vehicleId) {
        if (vehicleId.endsWith("A")) return new Color(255, 0, 255);
        if (vehicleId.endsWith("B")) return new Color(255, 140, 0);
        if (vehicleId.endsWith("C")) return new Color(0, 255, 255);
        return Color.YELLOW;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Centers and scales the map to fit the current window size while maintaining aspect ratio.
        double virtualWidth = 1250.0;
        double virtualHeight = 900.0;
        double panelWidth = getWidth();
        double panelHeight = getHeight();
        double scale = Math.min(panelWidth / virtualWidth, panelHeight / virtualHeight);
        double translateX = (panelWidth - (virtualWidth * scale)) / 2;
        double translateY = (panelHeight - (virtualHeight * scale)) / 2;

        g2.translate(translateX, translateY);
        g2.scale(scale, scale);

        // 1. ROADS
        g2.setStroke(new BasicStroke(2));

        for (int id : graph.adjList.keySet()) {
            Node n1 = graph.nodes.get(id);
            for (Edge e : graph.adjList.get(id)) {
                Node n2 = e.target;
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

        // BUS ROUTES
        if ("BUS_DRIVER".equals(currentRole)) {
            g2.setStroke(new BasicStroke(2));
            for (Vehicle v : engine.vehicles) {
                if (v.type == VehicleType.BUS) {
                    Color busColor = getBusColor(v.id);
                    g2.setColor(new Color(busColor.getRed(), busColor.getGreen(), busColor.getBlue(), 200));

                    double specificOffset;
                    if (v.id.endsWith("A")) specificOffset = 6.0;
                    else if (v.id.endsWith("B")) specificOffset = 10.0;
                    else specificOffset = 14.0;

                    drawPath(g2, v, specificOffset);
                }
            }
        }

        // EMERGENCY ROUTES
        if ("EMERGENCY".equals(currentRole)) {
            g2.setStroke(new BasicStroke(2));
            for (Vehicle v : engine.vehicles) {
                if (v.type == VehicleType.AMBULANCE || v.type == VehicleType.POLICE_CAR || v.type == VehicleType.FIRE_TRUCK) {
                    if (v.isReturning && v.type != VehicleType.AMBULANCE) {
                        continue;
                    }

                    if (v.type == VehicleType.AMBULANCE) g2.setColor(new Color(255, 0, 0, 180));
                    else if (v.type == VehicleType.POLICE_CAR) g2.setColor(new Color(0, 0, 255, 180));
                    else g2.setColor(new Color(255, 165, 0, 180));

                    drawPath(g2, v, 4.0);
                }
            }
        }

        // 2. NODES AND LIGHTS
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

        // 3. VEHICLES
        for (Vehicle v : engine.vehicles) {
            if (v.next == null) continue;

            if ("BUS_DRIVER".equals(currentRole) && v.type != VehicleType.BUS) {
                continue;
            }

            double specificOffset = BASE_LANE_OFFSET;
            if (v.type == VehicleType.BUS) {
                if (v.id.endsWith("A")) specificOffset = 6.0;
                else if (v.id.endsWith("B")) specificOffset = 10.0;
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

    // Helper to draw the full path line for a vehicle
    private void drawPath(Graphics2D g2, Vehicle v, double offset) {
        if (v.path != null && v.path.size() > 1) {
            for (int i = 0; i < v.path.size() - 1; i++) {
                Node n1 = v.path.get(i);
                Node n2 = v.path.get(i+1);
                double[] offsets = calculateOffset(n1.x, n1.y, n2.x, n2.y, offset);
                g2.drawLine((int)(n1.x + offsets[0]), (int)(n1.y + offsets[1]),
                        (int)(n2.x + offsets[0]), (int)(n2.y + offsets[1]));
            }
        }
    }

    // Calculates perpendicular offset vector for lane positioning
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