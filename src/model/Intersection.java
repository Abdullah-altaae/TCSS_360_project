/*
 * TCSS 360 - Software Development and Quality Assurance Techniques
 * Group Project: Car Route Planner
 */

package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single intersection on the city map, modeled as a node within the road
 * graph. The roads attached to an intersection form the edges of that graph.
 *
 * <p>An intersection is identified by its name, which must be unique across the map.
 * Two intersections are equal exactly when their names are equal. An intersection also
 * carries a coordinate pair used by the view to draw it, and a blocked flag that the
 * environment simulator sets and the route planner reads.</p>
 *
 * <p>An intersection with no connected roads is valid.</p>
 *
 * @author Felix Kiama
 * @version 2 November 2026
 */
public final class Intersection {

    /** The unique name by which this intersection is referenced. */
    private final String myName;

    /** The roads connected to this intersection, in the order they were added. */
    private final List<Road> myRoadsConnected;

    /** The horizontal map coordinate of this intersection. */
    private final int myX;

    /** The vertical map coordinate of this intersection. */
    private final int myY;

    /** Whether this intersection is currently impassable. */
    private boolean myBlocked;

    /**
     * Constructs an open intersection at map position (0, 0) with no connected roads.
     *
     * @param theName the unique name of this intersection, for example
     *                {@code "Main and 5th"}
     * @throws NullPointerException     if theName is null
     * @throws IllegalArgumentException if theName is empty or contains only whitespace
     */
    public Intersection(final String theName) {
        this(theName, new ArrayList<Road>(), 0, 0, false);
    }

    /**
     * Constructs an open intersection at map position (0, 0) connected to the supplied
     * roads. The list is copied, so later changes the caller makes to it are not
     * reflected in this intersection. An empty list is allowed.
     *
     * @param theName           the unique name of this intersection
     * @param theRoadsConnected the roads initially connected to this intersection
     * @throws NullPointerException     if theName or theRoadsConnected is null, or if
     *                                  theRoadsConnected contains a null element
     * @throws IllegalArgumentException if theName is empty or contains only whitespace,
     *                                  or if theRoadsConnected contains the same road
     *                                  more than once
     */
    public Intersection(final String theName, final List<Road> theRoadsConnected) {
        this(theName, theRoadsConnected, 0, 0, false);
    }

    /**
     * Constructs an intersection at the specified map position with no connected roads.
     *
     * @param theName    the unique name of this intersection
     * @param theX       the horizontal map coordinate
     * @param theY       the vertical map coordinate
     * @param theBlocked {@code true} if this intersection is initially blocked
     * @throws NullPointerException     if theName is null
     * @throws IllegalArgumentException if theName is empty or contains only whitespace
     */
    public Intersection(final String theName, final int theX, final int theY,
                        final boolean theBlocked) {
        this(theName, new ArrayList<Road>(), theX, theY, theBlocked);
    }

    /**
     * Constructs an intersection from every attribute. All public constructors delegate
     * here so that validation happens in exactly one place.
     *
     * @param theName           the unique name of this intersection
     * @param theRoadsConnected the roads initially connected to this intersection; the
     *                          list is copied
     * @param theX              the horizontal map coordinate
     * @param theY              the vertical map coordinate
     * @param theBlocked        {@code true} if this intersection is initially blocked
     * @throws NullPointerException     if theName or theRoadsConnected is null, or if
     *                                  theRoadsConnected contains a null element
     * @throws IllegalArgumentException if theName is empty or contains only whitespace,
     *                                  or if theRoadsConnected contains the same road
     *                                  more than once
     */
    private Intersection(final String theName, final List<Road> theRoadsConnected,
                         final int theX, final int theY, final boolean theBlocked) {
        Objects.requireNonNull(theName, "intersection name must not be null");
        if (theName.isBlank()) {
            throw new IllegalArgumentException(
                    "intersection name must not be blank, was \"" + theName + "\"");
        }
        Objects.requireNonNull(theRoadsConnected, "connected road list must not be null");
        myName = theName;
        myX = theX;
        myY = theY;
        myBlocked = theBlocked;
        myRoadsConnected = new ArrayList<Road>();
        for (final Road road : theRoadsConnected) {
            insertRoad(road);
        }
    }

    /**
     * Returns the name of this intersection.
     *
     * @return the name of this intersection
     */
    public String getName() {
        return myName;
    }

    /**
     * Returns the identifier of this intersection, which is its name. Roads refer to
     * their endpoints by this value. This method is kept for the route planner and the
     * view, which look intersections up by identifier.
     *
     * @return the name of this intersection
     */
    public String getId() {
        return myName;
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
     * cannot be traversed by the route planner.
     *
     * @return {@code true} if this intersection is blocked, {@code false} otherwise
     */
    public boolean isBlocked() {
        return myBlocked;
    }

    /**
     * Sets the blocked state of this intersection. The environment simulator invokes
     * this method when an obstacle is placed or cleared.
     *
     * @param theBlocked {@code true} to block this intersection, {@code false} to clear it
     */
    public void setBlocked(final boolean theBlocked) {
        myBlocked = theBlocked;
    }

    /**
     * Returns an unmodifiable view of the roads connected to this intersection. The view
     * reflects roads added later, but any attempt to modify it throws
     * {@link UnsupportedOperationException}. The list is empty if no road is connected.
     *
     * @return an unmodifiable view of the connected roads
     */
    public List<Road> getRoadsConnected() {
        return Collections.unmodifiableList(myRoadsConnected);
    }

    /**
     * Returns the number of roads connected to this intersection. Zero is a valid result.
     *
     * @return the number of connected roads
     */
    public int getConnectedRoadCount() {
        return myRoadsConnected.size();
    }

    /**
     * Connects a road to this intersection.
     *
     * @param theRoad the road to connect
     * @throws NullPointerException     if theRoad is null
     * @throws IllegalArgumentException if theRoad is already connected to this
     *                                  intersection
     */
    public void addRoad(final Road theRoad) {
        insertRoad(theRoad);
    }

    /**
     * Returns whether the given road is connected to this intersection.
     *
     * @param theRoad the road to look for
     * @return {@code true} if theRoad is connected to this intersection,
     *         {@code false} otherwise
     * @throws NullPointerException if theRoad is null
     */
    public boolean hasRoad(final Road theRoad) {
        Objects.requireNonNull(theRoad, "road must not be null");
        return myRoadsConnected.contains(theRoad);
    }

    /**
     * Validates a road and appends it to the connected roads. The constructor uses this
     * helper so that it never calls the public {@link #addRoad(Road)}.
     *
     * @param theRoad the road to connect
     * @throws NullPointerException     if theRoad is null
     * @throws IllegalArgumentException if theRoad is already connected to this
     *                                  intersection
     */
    private void insertRoad(final Road theRoad) {
        Objects.requireNonNull(theRoad, "road must not be null");
        if (myRoadsConnected.contains(theRoad)) {
            throw new IllegalArgumentException(
                    theRoad + " is already connected to intersection " + myName);
        }
        myRoadsConnected.add(theRoad);
    }

    /**
     * Returns whether the given object is an intersection with the same name. The
     * coordinates, blocked state and connected roads are not compared.
     *
     * @param theOther the object to compare with
     * @return {@code true} if theOther is an intersection with an equal name,
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object theOther) {
        if (this == theOther) {
            return true;
        }
        if (!(theOther instanceof Intersection)) {
            return false;
        }
        return myName.equals(((Intersection) theOther).myName);
    }

    /**
     * Returns a hash code based only on the name, consistent with
     * {@link #equals(Object)}.
     *
     * @return the hash code of this intersection
     */
    @Override
    public int hashCode() {
        return myName.hashCode();
    }

    /**
     * Returns a short description of this intersection, for debugging.
     *
     * @return a string giving the name, position, blocked state and road count
     */
    @Override
    public String toString() {
        return "Intersection " + myName + " (" + myX + ", " + myY + ")"
                + (myBlocked ? ", BLOCKED" : "") + ", " + myRoadsConnected.size()
                + " road(s)";
    }
}
