package models;

public enum VehicleType {
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