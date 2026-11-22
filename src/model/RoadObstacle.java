package model;

import java.util.Objects;

/**
 * Represents a physical obstacle on a road, such as an accident,
 * debris, or construction.
 * An obstacle can be active or inactive and may affect road availability.
 */
public class RoadObstacle {

    /**
     * Represents the different types of physical obstacles
     */
    public enum ObstacleType {
        ACCIDENT,
        DEBRIS,
        CONSTRUCTION
    }

    /** The road affected by this obstacle. */
    private final Road myRoad;

    /** The type of physical obstacle. */
    private final ObstacleType myType;

    /** Indicates whether this obstacle is currently active. */
    private boolean myActive;


    /**
     * Constructs a RoadObstacle associated with a specific road
     * and obstacle type. The obstacle is initially active.
     *
     * @param theRoad the road affected by the obstacle
     * @param theType the type of obstacle
     * @throws NullPointerException if the road or obstacle type is null
     */
    public RoadObstacle(Road theRoad, ObstacleType theType) {
        myRoad = Objects.requireNonNull(theRoad, "Road cannot be null.");
        myType = Objects.requireNonNull(theType, "Obstacle type cannot be null.");
        myActive = true;
    }

    /**
     * Returns the road associated with this obstacle.
     *
     * @return the road affected by this obstacle
     */
    public Road getRoad() {
        return myRoad;
    }

    /**
     * Returns the type of this obstacle.
     *
     * @return the obstacle type
     */
    public ObstacleType getObstacleType() {
        return myType;
    }

    /**
     * Checks whether this obstacle is currently active.
     *
     * @return true if the obstacle is active, false otherwise
     */
    public boolean isActive() {
        return myActive;
    }

    /**
     * Updates whether this obstacle is currently active.
     *
     * @param theStatus true if the obstacle is active, false otherwise
     */
    public void setActive(boolean theStatus) {
        myActive = theStatus;
    }
}
