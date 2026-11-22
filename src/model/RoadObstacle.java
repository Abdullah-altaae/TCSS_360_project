package model;

import java.util.Objects;

public class RoadObstacle {
    public enum ObstacleType {
        ACCIDENT,
        DEBRIS,
        CONSTRUCTION
    }

    private final Road myRoad;
    private final ObstacleType myType;
    private boolean myActive;

    public RoadObstacle(Road theRoad, ObstacleType theType) {
        myRoad = Objects.requireNonNull(theRoad, "Road cannot be null.");
        myType = Objects.requireNonNull(theType, "Obstacle type cannot be null.");
        myActive = true;
    }

    public Road getRoad() {
        return myRoad;
    }
    public ObstacleType getObstacleType() {
        return myType;
    }
    public boolean isActive() {
        return myActive;
    }
    public void setActive(boolean theStatus) {
        myActive = theStatus;
    }
}
