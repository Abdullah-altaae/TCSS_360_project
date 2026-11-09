package model;

import java.util.List;
import java.util.StringJoiner;

/**
 * A planned path from a start intersection to an end intersection, made up of
 * the roads to travel in order, as produced by {@link RoutePlanner}.
 *
 * <p>Routes are used by the simulator, the safety checker, and the dashboard,
 * which highlights the route on the map.</p>
 *
 * @author Felix Kiama
 * @version 8 November 2026
 */
public class Route {

    /** The ID of this route. */
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

    /** The separator placed between road IDs in a path string. */
    private static final String PATH_SEPARATOR = ",";

    /**
     * Creates a route with the given ID, start, end, and path, and works out
     * its total distance and path string from the path.
     *
     * @param theId the ID of this route
     * @param theStart the ID of the intersection where this route starts
     * @param theEnd the ID of the intersection where this route ends
     * @param thePath the roads that make up this route, in travel order
     */
    public Route(final int theId, final String theStart, final String theEnd,
                 final List<Road> thePath) {
        myId = theId;
        myStart = theStart;
        myEnd = theEnd;
        myPath = thePath;
        myDistance = calculateDistance();
        myPathString = createPathString();
    }



    /**
     * Adds up the lengths of all roads in this route's path.
     *
     * @return the total distance of the path in miles
     */
    private double calculateDistance() {
        double distance = 0.0;
        for (final Road road : myPath) {
            distance += road.getDistance();
        }
        return distance;
    }

    /**
     * Builds a string of this route's road IDs in travel order, separated by
     * commas, for example "1,2,3". Returns an empty string if the path is empty.
     *
     * @return the road IDs in travel order, separated by commas
     */
    private String createPathString() {
        final StringJoiner joiner = new StringJoiner(PATH_SEPARATOR);
        for (final Road road : myPath) {
            joiner.add(String.valueOf(road.getId()));
        }
        return joiner.toString();
    }
}
