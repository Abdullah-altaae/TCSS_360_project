package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Plans routes between intersections using Dijkstra's algorithm. Roads are
 * weighted by travel time, so the planner picks the quickest route, which
 * might not be the shortest one.
 *
 * <p>Blocked roads and intersections are skipped. The planner checks the current
 * state of the map every time findRoute is called, so new traffic or blocked
 * roads are always taken into account.</p>
 *
 * @author Abdullah Altaae
 * @version 1 November 2026
 */
public final class RoutePlanner {

    /** The map to plan routes on. */
    private final MapGraph myGraph;

    /** The roads that can be used in a route. */
    private final List<Road> myRoads;

    /**
     * Creates a route planner for a map and its roads.
     *
     * @param theGraph the map
     * @param theRoads the roads on the map
     * @throws IllegalArgumentException if theGraph or theRoads is null, or if
     *                                  any road is null
     */
    public RoutePlanner(final MapGraph theGraph, final Collection<Road> theRoads) {
        if (theGraph == null || theRoads == null) {
            throw new IllegalArgumentException("graph and roads must not be null");
        }
        for (final Road road : theRoads) {
            if (road == null) {
                throw new IllegalArgumentException("roads must not contain null");
            }
        }
        myGraph = theGraph;
        myRoads = new ArrayList<>(theRoads);
    }

    /**
     * Finds the quickest route from theStart to theEnd. Uses each road's travel
     * time and skips anything that is blocked.
     *
     * <p>Returns an empty list if there is no route, if the start or end is blocked
     * or not on the map, or if the start and end are the same.</p>
     *
     * @param theStart the starting intersection
     * @param theEnd   the destination intersection
     * @return the roads to take, in order, or an empty list if there is no route
     * @throws IllegalArgumentException if theStart or theEnd is null
     */
    public List<Road> findRoute(final Intersection theStart, final Intersection theEnd) {
        if (theStart == null || theEnd == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
        final Map<String, Intersection> intersections = indexIntersections();
        final String startId = theStart.getId();
        final String endId = theEnd.getId();
        if (!intersections.containsKey(startId) || !intersections.containsKey(endId)
                || theStart.isBlocked() || theEnd.isBlocked() || startId.equals(endId)) {
            return Collections.emptyList();
        }

        final Map<String, List<Road>> outgoing = indexOutgoingRoads();
        final Map<String, Double> bestTime = new HashMap<>();
        final Map<String, Road> previousRoad = new HashMap<>();
        final PriorityQueue<QueueEntry> queue = new PriorityQueue<>();

        bestTime.put(startId, 0.0);
        queue.add(new QueueEntry(startId, 0.0));

        while (!queue.isEmpty()) {
            final QueueEntry current = queue.poll();
            if (current.myTime > bestTime.get(current.myId)) {
                continue; // already found a faster way here
            }
            if (current.myId.equals(endId)) {
                break; // reached the end, no need to keep going
            }
            for (final Road road : outgoing.getOrDefault(current.myId,
                    Collections.emptyList())) {
                final Intersection next = intersections.get(road.getEnd());
                if (road.isBlocked() || next == null || next.isBlocked()) {
                    continue;
                }
                final double time = current.myTime + road.getTravelTime();
                final Double known = bestTime.get(next.getId());
                if (known == null || time < known) {
                    bestTime.put(next.getId(), time);
                    previousRoad.put(next.getId(), road);
                    queue.add(new QueueEntry(next.getId(), time));
                }
            }
        }

        return buildPath(previousRoad, startId, endId);
    }

    /**
     * Builds the route by going backwards from the end using the previous road
     * of each intersection, then reverses it.
     *
     * @param thePreviousRoad the road used to get to each intersection
     * @param theStartId      the start intersection's ID
     * @param theEndId        the end intersection's ID
     * @return the route in order, or an empty list if the end was never reached
     */
    private static List<Road> buildPath(final Map<String, Road> thePreviousRoad,
                                        final String theStartId, final String theEndId) {
        final List<Road> path = new ArrayList<>();
        String current = theEndId;
        while (!current.equals(theStartId)) {
            final Road road = thePreviousRoad.get(current);
            if (road == null) {
                return Collections.emptyList(); // no way to get to the end
            }
            path.add(road);
            current = road.getStart();
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Makes a map from intersection IDs to intersections.
     *
     * @return the intersections by ID
     */
    private Map<String, Intersection> indexIntersections() {
        final Map<String, Intersection> index = new HashMap<>();
        for (final Intersection intersection : myGraph.getIntersections()) {
            index.put(intersection.getId(), intersection);
        }
        return index;
    }

    /**
     * Groups the roads by their start intersection. Roads are one-way, so each road
     * only goes under the intersection it starts at.
     *
     * @return the roads leaving each intersection, by intersection ID
     */
    private Map<String, List<Road>> indexOutgoingRoads() {
        final Map<String, List<Road>> index = new HashMap<>();
        for (final Road road : myRoads) {
            index.computeIfAbsent(road.getStart(), key -> new ArrayList<>()).add(road);
        }
        return index;
    }

    /**
     * An intersection in the priority queue along with the best time found to
     * reach it so far.
     */
    private static final class QueueEntry implements Comparable<QueueEntry> {

        /** The intersection's ID. */
        private final String myId;

        /** The time to get here from the start, in hours. */
        private final double myTime;

        /**
         * Creates a queue entry.
         *
         * @param theId   the intersection's ID
         * @param theTime the time from the start, in hours
         */
        QueueEntry(final String theId, final double theTime) {
            myId = theId;
            myTime = theTime;
        }

        @Override
        public int compareTo(final QueueEntry theOther) {
            return Double.compare(myTime, theOther.myTime);
        }
    }
}