package models;

// Controls traffic light state and adaptive timing based on vehicle queue density.
public class TrafficLight {
	public boolean northSouthGreen = true; // True = North/South Green; False = East/West Green
    public int timer = 0;
    private final int MIN_DURATION = 40;
    private final int MAX_DURATION = 250;
    private final int DEFAULT_DURATION = 100;

    // Adaptive logic: Extends green light duration if the current lane is busy, or switches early if the current lane is empty.
    public void update(int nsQueue, int ewQueue) {
        timer++;
        int currentTargetDuration = DEFAULT_DURATION;

        if (northSouthGreen) {
        	// If N-S is empty but E/W is waiting, switch fast
            if (nsQueue == 0 && ewQueue > 0) {
                currentTargetDuration = MIN_DURATION;
            }             
            // If N-S is heavy, extend duration
            else if (nsQueue > ewQueue + 2) {
                currentTargetDuration = MAX_DURATION;
            }
        } else {
        	// If E-W is empty but N/S is waiting, switch fast
            if (ewQueue == 0 && nsQueue > 0) {
                currentTargetDuration = MIN_DURATION;
            }             
            // If E-W is heavy, extend duration
            else if (ewQueue > nsQueue + 2) {
                currentTargetDuration = MAX_DURATION;
            }
        }

        if (timer > currentTargetDuration) {
            northSouthGreen = !northSouthGreen;
            timer = 0;
        }
    }

    // Determines if a vehicle approaching from a specific neighbor node has a green light based on its vertical vs horizontal approach angle.
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
