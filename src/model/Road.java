package model;

/**
 * A one-way road on the map. Each road goes from a start intersection to an end
 * intersection and is an edge in the MapGraph.
 *
 * <p>Roads are referenced by their ID in Intersection and MapGraph. The environment
 * simulator can block a road or add traffic delay, and the route planner uses
 * getTravelTime() to compare routes.</p>
 *
 * <p>Distances are measured in miles, speeds in miles per hour, and times in hours.</p>
 *
 * @author Abdullah Altaae
 * @version 31 October 2026
 */
public final class Road {

    /** The unique identifier by which this road is referenced. */
    private final int myId;

    /** The identifier of the intersection where this road begins. */
    private final String myStart;

    /** The identifier of the intersection where this road ends. */
    private final String myEnd;

    /** The length of this road, in miles. */
    private final double myDistance;

    /** The speed limit of this road, in miles per hour. */
    private final double mySpeedLimit;

    /** The extra travel time caused by traffic, in hours. */
    private double myTrafficDelay;

    /** Whether this road is currently blocked. */
    private boolean myBlocked;

    /**
     * Constructs an open road with no traffic delay.
     *
     * @param theId         the unique identifier
     * @param theStart      the identifier of the intersection where this road begins
     * @param theEnd        the identifier of the intersection where this road ends
     * @param theDistance   the length of this road, in miles
     * @param theSpeedLimit the speed limit of this road, in miles per hour
     * @throws IllegalArgumentException if theId is negative, theStart or theEnd is
     *                                  null, or theDistance or theSpeedLimit is not a
     *                                  positive number
     */
    public Road(final int theId, final String theStart, final String theEnd,
                final double theDistance, final double theSpeedLimit) {
        this(theId, theStart, theEnd, theDistance, theSpeedLimit, 0.0);
    }

    /**
     * Constructs an open road with the given traffic delay.
     *
     * @param theId           the unique identifier for this road
     * @param theStart        the identifier of the intersection where this road begins
     * @param theEnd          the identifier of the intersection where this road ends
     * @param theDistance     the length of this road, in miles
     * @param theSpeedLimit   the speed limit of this road, in miles per hour
     * @param theTrafficDelay the extra travel time caused by traffic, in hours
     * @throws IllegalArgumentException if theId is negative, theStart or theEnd is
     *                                  null, theDistance or theSpeedLimit is not a
     *                                  positive number, or theTrafficDelay is negative
     *                                  or not a number
     */
    public Road(final int theId, final String theStart, final String theEnd,
                final double theDistance, final double theSpeedLimit,
                final double theTrafficDelay) {
        if (theId < 0) {
            throw new IllegalArgumentException("id must not be negative, was " + theId);
        }
        if (theStart == null || theEnd == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
        if (!Double.isFinite(theDistance) || theDistance <= 0) {
            throw new IllegalArgumentException(
                    "distance must be a positive number, was " + theDistance);
        }
        if (!Double.isFinite(theSpeedLimit) || theSpeedLimit <= 0) {
            throw new IllegalArgumentException(
                    "speed limit must be a positive number, was " + theSpeedLimit);
        }
        myId = theId;
        myStart = theStart;
        myEnd = theEnd;
        myDistance = theDistance;
        mySpeedLimit = theSpeedLimit;
        setTrafficDelay(theTrafficDelay);
        myBlocked = false;
    }

    /**
     * Returns the identifier for this road. Intersections and the map graph reference
     * roads by this value, so it must be unique across the map.
     *
     * @return the identifier for this road
     */
    public int getId() {
        return myId;
    }

    /**
     * Returns the identifier of the intersection where this road begins.
     *
     * @return the start intersection identifier
     */

    public String getStart() {
        return myStart;
    }

    /**
     * Returns the identifier of the intersection where this road ends.
     *
     * @return the end intersection identifier
     */

    public String getEnd() {
        return myEnd;
    }

    /**
     * Returns the length of this road.
     *
     * @return the distance, in miles
     */

    public double getDistance() {
        return myDistance;
    }

    /**
     * Returns the posted speed limit of this road. The environment simulator may
     * compute a lower effective speed from this value based on the weather.
     *
     * @return the speed limit, in miles per hour
     */

    public double getSpeedLimit() {
        return mySpeedLimit;
    }

    /**
     * Returns the extra travel time currently caused by traffic on this road.
     *
     * @return the traffic delay, in hours
     */

    public double getTrafficDelay() {
        return myTrafficDelay;
    }

    /**
     * Sets the extra travel time caused by traffic on this road. The environment
     * simulator invokes this method when traffic conditions change.
     *
     * @param theTrafficDelay the traffic delay, in hours (15 minutes is 0.25)
     * @throws IllegalArgumentException if theTrafficDelay is negative or not a number
     */

    public void setTrafficDelay(final double theTrafficDelay) {
        if (!Double.isFinite(theTrafficDelay) || theTrafficDelay < 0) {
            throw new IllegalArgumentException(
                    "traffic delay must be a non-negative number, was " + theTrafficDelay);
        }
        myTrafficDelay = theTrafficDelay;
    }

    /**
     * Returns whether this road is currently blocked. A blocked road cannot be
     * traversed, and any route containing it is flagged by the safety checker.
     *
     * @return {@code true} if this road is blocked, {@code false} otherwise
     */
    public boolean isBlocked() {
        return myBlocked;
    }

    /**
     * Sets the blocked state of this road. The environment simulator invokes this
     * method when an obstacle is placed or cleared.
     *
     * @param theBlocked {@code true} to block this road, {@code false} to clear it
     */

    public void setBlocked(final boolean theBlocked) {
        myBlocked = theBlocked;
    }

    /**
     * Returns the time needed to travel this road at the speed limit, including the
     * traffic delay: distance / speed limit + traffic delay.
     *
     * @return the travel time, in hours
     */

    public double getTravelTime() {
        return myDistance / mySpeedLimit + myTrafficDelay;
    }

    /**
     * Returns the time needed to travel this road at the given speed, including the
     * traffic delay. The environment simulator can pass a weather-adjusted speed here.
     *
     * @param theSpeed the speed to travel at, in miles per hour
     * @return the travel time, in hours
     * @throws IllegalArgumentException if theSpeed is not a positive number
     */

    public double getTravelTime(final double theSpeed) {
        if (!Double.isFinite(theSpeed) || theSpeed <= 0) {
            throw new IllegalArgumentException(
                    "speed must be a positive number, was " + theSpeed);
        }
        return myDistance / theSpeed + myTrafficDelay;
    }

    /**
     * Returns a short description of this road, for debugging.
     *
     * @return a string describing this road
     */

    @Override
    public String toString() {
        return "Road " + myId + " [" + myStart + " -> " + myEnd + ", " + myDistance
                + " mi @ " + mySpeedLimit + " mph" + (myBlocked ? ", BLOCKED" : "") + "]";
    }
}