package model;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A planned path from a start intersection to an end intersection, made up of
 * the roads to travel in order, as produced by {@link RoutePlanner}.
 *
 * <p>Routes are used by the simulator, the safety checker, and the dashboard,
 * which highlights the route on the map.</p>
 *
 * @author Felix Kiama
 * @version 12 November 2026
 */
public final class Route {

    /** The ID a route has until it is saved to the database. */
    public static final int UNSAVED_ID = -1;

    /** The separator placed between road IDs in a path string. */
    private static final String PATH_SEPARATOR = ",";

    /** The ID of this route, or {@link #UNSAVED_ID} if it has not been saved. */
    private final int myId;

    /** The ID of the intersection where this route starts. */
    private final String myStart;

    /** The ID of the intersection where this route ends. */
    private final String myEnd;

    /** The IDs of the roads in this route in travel order, separated by commas. */
    private final String myPathString;

    /** The roads that make up this route, in travel order. */
    private final List<Road> myPath;

    /** The total length of this route in miles. */
    private final double myDistance;

    /**
     * Creates a route with the given ID, start, end, and path, and works out
     * its total distance and path string from the path.
     *
     * @param theId the ID of this route
     * @param theStart the ID of the intersection where this route starts
     * @param theEnd the ID of the intersection where this route ends
     * @param thePath the roads that make up this route, in travel order
     * @throws NullPointerException if theStart, theEnd or thePath is null, or if
     *                              thePath contains a null road
     * @throws IllegalArgumentException if theId is not UNSAVED_ID or at least 1
     */
    public Route(final int theId, final String theStart, final String theEnd,
                 final List<Road> thePath) {
        if (theId != UNSAVED_ID && theId < 1) {
            throw new IllegalArgumentException(
                    "route ID must be UNSAVED_ID or at least 1, was " + theId);
        }
        Objects.requireNonNull(theStart, "start must not be null");
        Objects.requireNonNull(theEnd, "end must not be null");
        Objects.requireNonNull(thePath, "path must not be null");
        myId = theId;
        myStart = theStart;
        myEnd = theEnd;
        myPath = List.copyOf(thePath);
        myDistance = calculateDistance();
        myPathString = buildPathString();
    }

    /**
     * Creates a new route that has not been saved to the database yet. Its ID is
     * {@link #UNSAVED_ID} until it is saved. The total distance and path string
     * are worked out from the path.
     *
     * @param theStart the ID of the intersection where this route starts
     * @param theEnd the ID of the intersection where this route ends
     * @param thePath the roads that make up this route, in travel order
     * @throws NullPointerException if theStart, theEnd or thePath is null, or if
     *                              thePath contains a null road
     */
    public Route(final String theStart, final String theEnd,
                 final List<Road> thePath) {
        this(UNSAVED_ID, theStart, theEnd, thePath);
    }

    /**
     * Returns a copy of this route with the given database ID, for use once the
     * route has been saved.
     *
     * @param theId the ID the database gave this route
     * @return a new route with the same start, end and path, and the given ID
     * @throws IllegalArgumentException if theId is less than 1
     */
    public Route withId(final int theId) {
        if (theId < 1) {
            throw new IllegalArgumentException(
                    "a saved route's ID must be at least 1, was " + theId);
        }
        return new Route(theId, myStart, myEnd, myPath);
    }

    /**
     * Returns the ID of this route.
     *
     * @return the database ID of this route, or {@link #UNSAVED_ID} if it has
     *         not been saved
     */
    public int getId() {
        return myId;
    }

    /**
     * Returns the intersection where this route starts.
     *
     * @return the ID of the start intersection
     */
    public String getStart() {
        return myStart;
    }

    /**
     * Returns the intersection where this route ends.
     *
     * @return the ID of the end intersection
     */
    public String getEnd() {
        return myEnd;
    }

    /**
     * Returns the IDs of the roads in this route in travel order, separated by
     * commas, for example "1,2,3".
     *
     * @return the road IDs in travel order, separated by commas
     */
    public String getPathString() {
        return myPathString;
    }

    /**
     * Returns the roads that make up this route.
     *
     * @return an unmodifiable list of this route's roads, in travel order
     */
    public List<Road> getPath() {
        return myPath;
    }

    /**
     * Returns the total length of this route.
     *
     * @return the total distance, in miles
     */
    public double getDistance() {
        return myDistance;
    }

    /**
     * Returns whether this route has been saved to the database. A route is
     * saved once it has a real database ID instead of {@link #UNSAVED_ID}.
     *
     * @return true if this route has been saved, false otherwise
     */
    public boolean isSaved() {
        return myId != UNSAVED_ID;
    }

    /**
     * Adds up the lengths of all roads in this route's path.
     *
     * @return the total distance of the path, in miles
     */
    private double calculateDistance() {
        double total = 0.0;
        for (final Road road : myPath) {
            total += road.getDistance();
        }
        return total;
    }

    /**
     * Builds a string of this route's road IDs in travel order, separated by
     * commas, for example "1,2,3". Returns an empty string if the path is empty.
     *
     * @return the road IDs in travel order, separated by commas
     */
    private String buildPathString() {
        final StringJoiner joiner = new StringJoiner(PATH_SEPARATOR);
        for (final Road road : myPath) {
            joiner.add(String.valueOf(road.getId()));
        }
        return joiner.toString();
    }
}
