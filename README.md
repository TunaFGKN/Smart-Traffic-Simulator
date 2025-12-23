# Smart City Traffic Control System
SBTU Module Project Semester 3

A Java-based simulation application designed to model urban traffic flow, managing intersections, vehicle routing, and emergency priorities using efficient data structures and Object-Oriented Programming principles.

## 📌 Table of Contents

* [Overview](https://www.google.com/search?q=%23overview)
* [Key Features](https://www.google.com/search?q=%23key-features)
* [System Architecture](https://www.google.com/search?q=%23system-architecture)
* [Data Structures & Algorithms](https://www.google.com/search?q=%23data-structures--algorithms)
* [Design Patterns](https://www.google.com/search?q=%23design-patterns)
* [How to Run](https://www.google.com/search?q=%23how-to-run)

## 📖 Overview

This project simulates a smart city grid consisting of intersections, residential areas, parking lots, and emergency services. It visualizes traffic flow in real-time, handling collision avoidance at intersections using adaptive traffic lights and priority-based queuing for emergency vehicles (Ambulance, Police, Fire Trucks).

## 🚀 Key Features

1. **Role-Based Access Control:**
* **Personal Car Driver:** Select start/end points to navigate the city.
* **Bus Driver:** Visualizes specific public transport routes.
* **Emergency Service:** Dispatches high-priority vehicles that override traffic rules.
* **Free View:** God-mode monitoring of the entire city.


2. **Adaptive Traffic Lights:** Traffic lights adjust durations dynamically based on the queue length of incoming lanes.
3. **Priority Handling:** Emergency vehicles bypass red lights (if safe) and cut ahead of regular cars in road queues.
4. **Real-time Visualization:** Smooth animation of vehicles with rotational rendering and lane offsets.
5. **Graph-Based Routing:** Uses weighted graphs to calculate the most efficient paths.

## 🏗 System Architecture

The project follows a modular design separating the Model, View, and Controller logic (MVC pattern adaptation).

* **`models` Package:** Contains POJOs (Plain Old Java Objects) representing physical entities (`Node`, `Edge`, `Vehicle`, `TrafficLight`).
* **`simulation` Package:** Contains the `SimulationEngine`, which runs on a separate thread to handle logic updates, physics, and pathfinding.
* **`gui` Package:** Handles all Swing-based rendering. `MapPanel` draws the simulation state, while `SimulationPanel` manages user interaction.
* **`graph` Package:** Manages the topology of the city.

## 🧠 Data Structures & Algorithms

### 1. Graph (Adjacency List)

* **Implementation:** `HashMap<Integer, List<Edge>>` inside `CityGraph`.
* **Reasoning:** The city is a sparse graph (not every node connects to every other node). An adjacency list is memory efficient () and allows fast lookup of neighbors.

### 2. Priority Queue (Traffic Flow)

* **Implementation:** `PriorityBlockingQueue<Vehicle>` inside `Edge`.
* **Reasoning:** Roads are modeled as queues. However, a standard FIFO queue is insufficient because emergency vehicles must pass first. The `Vehicle` class implements `Comparable` to sort based on `VehicleType` priority (Ambulance > Car) and arrival time.

### 3. Dijkstra's Algorithm (Pathfinding)

* **Implementation:** `PriorityQueue<PQNode>` inside `SimulationEngine`.
* **Reasoning:** To find the shortest path between a start and end node.
* **Dynamic Weights:** The weight of an edge is calculated as `Base Distance + (QueueSize * 0.5)`. This means vehicles automatically avoid congested roads.

### 4. Thread-Safe Lists

* **Implementation:** `CopyOnWriteArrayList<Vehicle>`.
* **Reasoning:** The simulation logic runs on one thread (`SimulationEngine`) while the GUI paints on another (`AWT-EventQueue`). A standard `ArrayList` would cause `ConcurrentModificationException` when vehicles are added/removed while the GUI is drawing them.

## 🎨 Design Patterns

* **OOP Principles:**
* **Encapsulation:** All model fields are protected/private where appropriate.
* **Polymorphism:** `Vehicle` types share common behaviors but have different priorities and speeds.


* **Observer-like Pattern:** The `SimulationPanel` observes the `SimulationEngine` via callbacks (e.g., for Logout).
* **State Pattern (Simplified):** Traffic lights switch states between North-South Green and East-West Green.

## ▶️ How to Run

1. Ensure you have **Java Development Kit (JDK) 8** or higher installed.
2. Compile the source code:
```bash
javac -d bin src/main/Main.java src/gui/*.java src/models/*.java src/simulation/*.java

```


3. Run the application:
```bash
java -cp bin main.Main

```


4. **Login Credentials (Password: 1234):**
* Car: `cardriver1@example.com`
* Bus: `busdriver1@example.com`
* Emergency: `emergency1@example.com`
* Free View: No password required.



---
