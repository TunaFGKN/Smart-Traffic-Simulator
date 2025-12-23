package models;

public class TrafficLight {
	public boolean northSouthGreen = true;
    int timer = 0;

    private final int MIN_DURATION = 40;
    private final int MAX_DURATION = 250;
    private final int DEFAULT_DURATION = 100;

    public void update(int nsQueue, int ewQueue) {
        timer++;
        int currentTargetDuration = DEFAULT_DURATION;

        if (northSouthGreen) {
            if (nsQueue == 0 && ewQueue > 0) {
                currentTargetDuration = MIN_DURATION;
            } else if (nsQueue > ewQueue + 2) {
                currentTargetDuration = MAX_DURATION;
            }
        } else {
            if (ewQueue == 0 && nsQueue > 0) {
                currentTargetDuration = MIN_DURATION;
            } else if (ewQueue > nsQueue + 2) {
                currentTargetDuration = MAX_DURATION;
            }
        }

        if (timer > currentTargetDuration) {
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
