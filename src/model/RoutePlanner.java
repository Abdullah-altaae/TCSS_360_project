package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Plans routes between intersections using Dijkstra's algorithm. Roads are
 * weighted by travel time, so the planner picks the quickest route, which
 * might not be the shortest one.
 *
 * <p>With an {@link EnvironmentSimulator}, travel times are adjusted for weather.</p>
 *
 * <p>Blocked roads and intersections are skipped. The planner checks the current
 * state of the map every time findRoute is called, so new traffic or blocked
 * roads are always taken into account.</p>
 *
 * @author Abdullah Altaae
 * @version 8 November 2026
 */
public final class RoutePlanner {

    /** The map to plan routes on. Roads are read from it on every call. */
    private final MapGraph myGraph;

    /** Adjusts travel times for weather, or null if not set. */
    private EnvironmentSimulator mySimulator;

    /**
     * Creates a route planner for a map. The planner always uses the map's
     * current roads, so roads added later are included in new routes.
     *
     * @param theGraph the map
     * @throws IllegalArgumentException if theGraph is null
     */
    public RoutePlanner(final MapGraph theGraph) {
        if (theGraph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        myGraph = theGraph;
    }

    /**
     * Creates a route planner that accounts for weather.
     *
     * @param theGraph     the map
     * @param theSimulator the environment simulator
     * @throws IllegalArgumentException if theGraph or theSimulator is null
     */
    public RoutePlanner(final MapGraph theGraph, final EnvironmentSimulator theSimulator) {
        this(theGraph);
        setSimulator(theSimulator);
    }

    /**
     * Sets the simulator used to adjust travel times for weather.
     *
     * @param theSimulator the environment simulator
     * @throws IllegalArgumentException if theSimulator is null
     */
    public void setSimulator(final EnvironmentSimulator theSimulator) {
        if (theSimulator == null) {
            throw new IllegalArgumentException("simulator must not be null");
        }
        mySimulator = theSimulator;
    }

    /**
     * Returns a road's travel time, adjusted for weather if a simulator is set.
     *
     * @param theRoad the road
     * @return the travel time, in hours
     */
    public double getTravelTime(final Road theRoad) {
        if (mySimulator == null) {
            return theRoad.getTravelTime();
        }
        return mySimulator.calculateAdjustedTravelTime(theRoad);
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
        return findRoute(theStart, theEnd, Collections.emptySet());
    }

    /**
     * Finds up to theCount different routes from theStart to theEnd, fastest first.
     * The first route is the same one findRoute returns.
     *
     * @param theStart the starting intersection
     * @param theEnd   the destination intersection
     * @param theCount the most routes to return
     * @return the routes, fastest first, or an empty list if there is no route
     * @throws IllegalArgumentException if theStart or theEnd is null, or theCount
     *                                  is less than 1
     */
    public List<List<Road>> findAlternativeRoutes(final Intersection theStart,final Intersection theEnd, final int theCount) {
        if (theCount < 1) {
            throw new IllegalArgumentException("count must be at least 1, was " + theCount);
        }
        final List<Road> best = findRoute(theStart, theEnd);
        final List<List<Road>> routes = new ArrayList<>();
        if (best.isEmpty()) {
            return routes;
        }
        routes.add(best);

        // Skip one road of the best route at a time to find different routes.
        final List<List<Road>> candidates = new ArrayList<>();
        for (final Road road : best) {
            final List<Road> detour = findRoute(theStart, theEnd, Set.of(road));
            if (!detour.isEmpty() && !candidates.contains(detour)) {
                candidates.add(detour);
            }
        }
        candidates.sort((theA, theB) ->
                Double.compare(getSimulatedTime(theA), getSimulatedTime(theB)));

        for (final List<Road> candidate : candidates) {
            if (routes.size() == theCount) {
                break;
            }
            routes.add(candidate);
        }
        return routes;
    }

    /**
     * Finds the quickest route that does not use any of the excluded roads.
     *
     * @param theStart    the starting intersection
     * @param theEnd      the destination intersection
     * @param theExcluded roads the route must not use
     * @return the roads to take, in order, or an empty list if there is no route
     */
    private List<Road> findRoute(final Intersection theStart, final Intersection theEnd,
                                 final Set<Road> theExcluded) {
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
                if (road.isBlocked() || theExcluded.contains(road)
                        || next == null || next.isBlocked()) {
                    continue;
                }
                final double time = current.myTime + getTravelTime(road);
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
     * Returns the total distance of a route.
     *
     * @param theRoute the roads in the route
     * @return the total distance, in miles
     * @throws IllegalArgumentException if theRoute is null
     */
    public double getRouteDistance(final List<Road> theRoute) {
        requireRoute(theRoute);
        double total = 0;
        for (final Road road : theRoute) {
            total += road.getDistance();
        }
        return total;
    }

    /**
     * Returns a route's travel time at the speed limits with no weather or traffic.
     *
     * @param theRoute the roads in the route
     * @return the normal travel time, in hours
     * @throws IllegalArgumentException if theRoute is null
     */
    public double getNormalTime(final List<Road> theRoute) {
        requireRoute(theRoute);
        double total = 0;
        for (final Road road : theRoute) {
            total += road.getDistance() / road.getSpeedLimit();
        }
        return total;
    }

    /**
     * Returns a route's travel time under current weather and traffic.
     *
     * @param theRoute the roads in the route
     * @return the simulated travel time, in hours
     * @throws IllegalArgumentException if theRoute is null
     */
    public double getSimulatedTime(final List<Road> theRoute) {
        requireRoute(theRoute);
        double total = 0;
        for (final Road road : theRoute) {
            total += getTravelTime(road);
        }
        return total;
    }

    /**
     * Checks that a route is not null.
     *
     * @param theRoute the route to check
     * @throws IllegalArgumentException if theRoute is null
     */
    private static void requireRoute(final List<Road> theRoute) {
        if (theRoute == null) {
            throw new IllegalArgumentException("route must not be null");
        }
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
        for (final Road road : myGraph.getRoads()) {
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