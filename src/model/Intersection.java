package model;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single intersection on the city map, modeled as a node within the road
 * graph. The roads attached to an intersection form the edges of that graph.
 *
 * <p>An intersection maintains an identifier used to locate it elsewhere in the
 * application, a coordinate pair describing its position on the map, a flag indicating
 * whether it is currently passable, and the collection of roads connected to it. The
 * blocked flag is set by the environment simulator when an obstacle is placed, and is
 * evaluated by the safety checker when a route is assessed.</p>
 *
 * @author Felix Kiama
 * @version 31 October 2026
 */
public final class Intersection {

    /** The unique identifier by which this intersection is referenced. */
    private final String myId;

    /** The horizontal map coordinate of this intersection. */
    private final int myX;

    /** The vertical map coordinate of this intersection. */
    private final int myY;

    /** Whether this intersection is currently impassable. */
    private boolean myBlocked;

    /** The roads connected to this intersection. */
    private List<Road> myConnectedRoads;

    /**
     * Constructs an intersection at the specified map position. The intersection is
     * created with no connected roads; use {@link #addRoad(Road)} to establish
     * connections once the corresponding roads exist.
     *
     * @param theId      the unique identifier for this intersection, for example
     *                   {@code "Main and 5th"}
     * @param theX       the horizontal map coordinate
     * @param theY       the vertical map coordinate
     * @param theBlocked {@code true} if this intersection is initially blocked
     */
    public Intersection(final String theId, final int theX, final int theY, final boolean theBlocked) {
        myId = theId;
        myX = theX;
        myY = theY;
        myBlocked = theBlocked;
        myConnectedRoads = new ArrayList<Road>();
    }

    /**
     * Returns the identifier for this intersection. The route planner and the user
     * interface both reference intersections by this value, so it must be unique across
     * the map.
     *
     * @return the identifier for this intersection
     */
     public String getId() {
        return myId;
     }

    /**
     * Returns the horizontal coordinate of this intersection. The value is a map
     * coordinate rather than a pixel coordinate; scaling it to the display is the
     * responsibility of the view.
     *
     * @return the horizontal map coordinate
     */
     public int getX() {
        return myX;
     }

    /**
     * Returns the vertical coordinate of this intersection. As with the horizontal
     * coordinate, the value is a map coordinate rather than a pixel coordinate.
     *
     * @return the vertical map coordinate
     */
     public int getY() {
        return myY;
     }

    /**
     * Returns whether this intersection is currently blocked. A blocked intersection
     * cannot be traversed, and any route containing it is flagged by the safety checker.
     *
     * @return {@code true} if this intersection is blocked, {@code false} otherwise
     */
     public boolean isBlocked() {
        return myBlocked;
     }

    /**
     * Returns the roads connected to this intersection.
     *
     * <p>This method returns a direct reference to the internal list rather than a
     * defensive copy. Modifications made to the returned list are reflected in this
     * intersection.</p>
     *
     * @return the list of roads connected to this intersection
     */
     public List<Road> getConnectedRoads() {
        return myConnectedRoads;
     }

    /**
     * Sets the blocked state of this intersection. The environment simulator invokes
     * this method when an obstacle is placed or cleared, and the resulting state is read
     * by the safety checker during route evaluation.
     *
     * @param theBlocked {@code true} to block this intersection, {@code false} to clear it
     */
     public void setBlocked(final boolean theBlocked) {
        myBlocked = theBlocked;
     }

    /**
     * Replaces the collection of roads connected to this intersection.
     *
     * <p>This method retains a reference to the supplied list rather than copying its
     * contents. Subsequent modifications made by the caller are reflected in this
     * intersection.</p>
     *
     * @param theConnectedRoads the new list of connected roads
     */
     public void setConnectedRoads(final List<Road> theConnectedRoads) {
        myConnectedRoads = theConnectedRoads;
     }

    /**
     * Adds a single road to the collection of roads connected to this intersection.
     * This is the standard means of building up connectivity as the map is assembled.
     *
     * @param theRoad the road to connect to this intersection
     */
     public void addRoad(final Road theRoad) {
        myConnectedRoads.add(theRoad);
     }




}