/*
 * TCSS 360 - Software Development and Quality Assurance Techniques
 * Group Project: Car Route Planner
 */

package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the city map as a graph. Intersections form the nodes of the graph and
 * roads form the edges that connect them.
 *
 * <p>This is the central model class for the application. The route planner traverses
 * the graph to compute a path between two intersections, and the environment simulator
 * alters the conditions of the roads and intersections it contains.</p>
 *
 * <p>A road names its endpoints by intersection name (see {@link Road#getStart()} and
 * {@link Road#getEnd()}), so both endpoints must already be in the graph before the road
 * is added. Adding a road records it in the adjacency map and in both endpoint
 * intersections; a road that starts and ends at the same intersection is recorded
 * once.</p>
 *
 * <p>An empty graph, and an intersection with no roads, are both valid. Every list this
 * class returns is an unmodifiable view.</p>
 *
 * @author Felix Kiama
 * @version 2 November 2026
 */
public final class MapGraph {

    /** The intersections forming the nodes of this graph, in insertion order. */
    private final List<Intersection> myIntersections;

    /** The roads forming the edges of this graph, in insertion order. */
    private final List<Road> myRoads;

    /** The roads connected to each intersection, keyed by intersection. */
    private final Map<Intersection, List<Road>> myAdjacency;

    /**
     * Constructs an empty map graph with no intersections and no roads.
     */
    public MapGraph() {
        myIntersections = new ArrayList<Intersection>();
        myRoads = new ArrayList<Road>();
        myAdjacency = new HashMap<Intersection, List<Road>>();
    }

    /**
     * Constructs a map graph from the supplied intersections and roads. Both lists are
     * copied, so later changes the caller makes to them are not reflected in this graph.
     * Either list may be empty. All intersections are added before any road, so a road
     * may refer to any intersection in theIntersections.
     *
     * @param theIntersections the intersections forming the nodes of the graph
     * @param theRoads         the roads forming the edges of the graph
     * @throws NullPointerException     if either list is null, if either list contains a
     *                                  null element, or if a road has a null endpoint
     * @throws IllegalArgumentException if theIntersections contains a duplicate, if
     *                                  theRoads contains a duplicate, or if a road's
     *                                  endpoint is not in theIntersections
     */
    public MapGraph(final List<Intersection> theIntersections, final List<Road> theRoads) {
        this();
        Objects.requireNonNull(theIntersections, "intersection list must not be null");
        Objects.requireNonNull(theRoads, "road list must not be null");
        for (final Intersection intersection : theIntersections) {
            insertIntersection(intersection);
        }
        for (final Road road : theRoads) {
            insertRoad(road);
        }
    }

    /**
     * Adds an intersection to this graph. The intersection starts with no roads in the
     * adjacency map, which is valid.
     *
     * @param theIntersection the intersection to add
     * @return an unmodifiable view of all intersections in this graph
     * @throws NullPointerException     if theIntersection is null
     * @throws IllegalArgumentException if an intersection with the same name is already
     *                                  in this graph
     */
    public List<Intersection> addIntersection(final Intersection theIntersection) {
        insertIntersection(theIntersection);
        return getIntersections();
    }

    /**
     * Adds a road to this graph. The road is recorded under both of its endpoints in the
     * adjacency map and in both endpoint intersections; a self-loop is recorded once.
     *
     * @param theRoad the road to add
     * @return an unmodifiable view of all roads in this graph
     * @throws NullPointerException     if theRoad is null or has a null endpoint
     * @throws IllegalArgumentException if a road with the same identifier is already in
     *                                  this graph, or if either endpoint is not in this
     *                                  graph
     */
    public List<Road> addRoad(final Road theRoad) {
        insertRoad(theRoad);
        return getRoads();
    }

    /**
     * Returns the roads leaving the given intersection, that is, the connected roads
     * whose start is that intersection. The result is empty if no road leaves it.
     *
     * @param theIntersection the intersection to look up
     * @return an unmodifiable list of the roads that start at theIntersection
     * @throws NullPointerException     if theIntersection is null
     * @throws IllegalArgumentException if theIntersection is not in this graph
     */
    public List<Road> getRoute(final Intersection theIntersection) {
        Objects.requireNonNull(theIntersection, "intersection must not be null");
        final List<Road> connected = myAdjacency.get(theIntersection);
        if (connected == null) {
            throw new IllegalArgumentException(
                    "intersection " + theIntersection.getName() + " is not in the graph");
        }
        final List<Road> leaving = new ArrayList<Road>();
        for (final Road road : connected) {
            if (road.getStart().equals(theIntersection.getName())) {
                leaving.add(road);
            }
        }
        return Collections.unmodifiableList(leaving);
    }

    /**
     * Returns an unmodifiable view of the intersections in this graph, in the order they
     * were added. The list is empty if the graph has no intersections.
     *
     * @return an unmodifiable view of the intersections
     */
    public List<Intersection> getIntersections() {
        return Collections.unmodifiableList(myIntersections);
    }

    /**
     * Returns an unmodifiable view of the roads in this graph, in the order they were
     * added. The list is empty if the graph has no roads.
     *
     * @return an unmodifiable view of the roads
     */
    public List<Road> getRoads() {
        return Collections.unmodifiableList(myRoads);
    }

    /**
     * Returns whether the given intersection, compared by name, is in this graph.
     *
     * @param theIntersection the intersection to look for
     * @return {@code true} if theIntersection is in this graph, {@code false} otherwise
     * @throws NullPointerException if theIntersection is null
     */
    public boolean containsIntersection(final Intersection theIntersection) {
        Objects.requireNonNull(theIntersection, "intersection must not be null");
        return myAdjacency.containsKey(theIntersection);
    }

    /**
     * Returns whether this graph has no intersections. A graph with no intersections
     * cannot have roads either.
     *
     * @return {@code true} if this graph has no intersections, {@code false} otherwise
     */
    public boolean isEmpty() {
        return myIntersections.isEmpty();
    }

    /**
     * Validates an intersection and adds it to the graph with an empty adjacency entry.
     * The constructor uses this helper so that it never calls a public method.
     *
     * @param theIntersection the intersection to add
     * @throws NullPointerException     if theIntersection is null
     * @throws IllegalArgumentException if an intersection with the same name is already
     *                                  in this graph
     */
    private void insertIntersection(final Intersection theIntersection) {
        Objects.requireNonNull(theIntersection, "intersection must not be null");
        if (myAdjacency.containsKey(theIntersection)) {
            throw new IllegalArgumentException("duplicate intersection "
                    + theIntersection.getName());
        }
        myIntersections.add(theIntersection);
        myAdjacency.put(theIntersection, new ArrayList<Road>());
    }

    /**
     * Validates a road and adds it to the graph, recording it under both endpoints in the
     * adjacency map and in both endpoint intersections. A self-loop is recorded once.
     * Both endpoints are checked before anything is changed, so a rejected road leaves
     * the graph untouched. The constructor uses this helper so that it never calls a
     * public method.
     *
     * @param theRoad the road to add
     * @throws NullPointerException     if theRoad is null or has a null endpoint
     * @throws IllegalArgumentException if a road with the same identifier is already in
     *                                  this graph, or if either endpoint is not in this
     *                                  graph
     */
    private void insertRoad(final Road theRoad) {
        Objects.requireNonNull(theRoad, "road must not be null");
        for (final Road existing : myRoads) {
            if (existing.getId() == theRoad.getId()) {
                throw new IllegalArgumentException("duplicate road with id "
                        + theRoad.getId());
            }
        }
        final Intersection start = requireKnownEndpoint(theRoad.getStart(), theRoad);
        final Intersection end = requireKnownEndpoint(theRoad.getEnd(), theRoad);
        myRoads.add(theRoad);
        myAdjacency.get(start).add(theRoad);
        start.addRoad(theRoad);
        if (!start.equals(end)) {
            myAdjacency.get(end).add(theRoad);
            end.addRoad(theRoad);
        }
    }

    /**
     * Returns the intersection in this graph with the given name, which is one endpoint
     * of the given road.
     *
     * @param theEndpoint the endpoint name taken from theRoad
     * @param theRoad     the road being added, used in the exception messages
     * @return the intersection in this graph named theEndpoint
     * @throws NullPointerException     if theEndpoint is null
     * @throws IllegalArgumentException if no intersection in this graph is named
     *                                  theEndpoint
     */
    private Intersection requireKnownEndpoint(final String theEndpoint, final Road theRoad) {
        Objects.requireNonNull(theEndpoint, "endpoint of " + theRoad + " must not be null");
        Intersection found = null;
        for (final Intersection intersection : myIntersections) {
            if (intersection.getName().equals(theEndpoint)) {
                found = intersection;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("endpoint " + theEndpoint + " of "
                    + theRoad + " is not in the graph; add the intersection first");
        }
        return found;
    }

    /**
     * Returns a short description of this graph, for debugging.
     *
     * @return a string giving the number of intersections and roads
     */
    @Override
    public String toString() {
        return "MapGraph [" + myIntersections.size() + " intersection(s), "
                + myRoads.size() + " road(s)]";
    }
}
