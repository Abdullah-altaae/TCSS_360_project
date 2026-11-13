package controller;

import javax.swing.SwingUtilities;

import model.Intersection;
import model.MapGraph;
import model.Road;
import model.RoutePlanner;
import view.PlannerDashboard;

import java.util.ArrayList;

/**
 * Entry point for the Self-Driving Car Route Planner.
 * <p>
 * Builds a small sample city map, creates the route planner and opens
 * the {@link PlannerDashboard} on the Swing event thread.
 * </p>
 *
 * @author Anjana Virani
 * @version 4 November 2026
 */
public final class CarPlannerApp {

    /** Private constructor so this class cannot be instantiated. */
    private CarPlannerApp() {
        // not used
    }

    /**
     * Starts the application.
     *
     * @param theArgs command-line arguments (not used)
     */
    public static void main(final String[] theArgs) {
        final MapGraph map = buildSampleMap();
        final RoutePlanner planner = new RoutePlanner(map);

        SwingUtilities.invokeLater(() -> new PlannerDashboard(map, planner).setVisible(true));
    }

    /**
     * Builds a small sample city map with 7 intersections (A to G).
     * Distances are in miles and speed limits in mph.
     * This can be replaced by a file loader later.
     *
     * @return the sample map
     */
    private static MapGraph buildSampleMap() {
        final MapGraph map = new MapGraph(new ArrayList<>(), new ArrayList<>());

        map.addIntersection(new Intersection("A", 0, 0, false));
        map.addIntersection(new Intersection("B", 200, 0, false));
        map.addIntersection(new Intersection("C", 400, 0, false));
        map.addIntersection(new Intersection("D", 0, 200, false));
        map.addIntersection(new Intersection("E", 200, 200, false));
        map.addIntersection(new Intersection("F", 400, 200, false));
        map.addIntersection(new Intersection("G", 600, 100, false));

        addTwoWayRoad(map, "A", "B", 2.0, 35);
        addTwoWayRoad(map, "B", "C", 2.0, 35);
        addTwoWayRoad(map, "D", "E", 2.0, 25);
        addTwoWayRoad(map, "E", "F", 2.0, 25);
        addTwoWayRoad(map, "A", "D", 2.0, 25);
        addTwoWayRoad(map, "B", "E", 2.0, 25);
        addTwoWayRoad(map, "C", "F", 2.0, 25);
        addTwoWayRoad(map, "C", "G", 1.5, 45);
        addTwoWayRoad(map, "F", "G", 1.5, 45);

        return map;
    }

    /**
     * Adds a road in both directions so cars can travel either way.
     * Each direction is a separate one-way {@link Road} with its own ID.
     * Both intersections must already be on the map.
     *
     * @param theMap the map to add to
     * @param theFrom the name of one end of the road
     * @param theTo the name of the other end of the road
     * @param theDistance length of the road in miles
     * @param theSpeedLimit speed limit in mph
     */
    private static void addTwoWayRoad(final MapGraph theMap, final String theFrom,
                                      final String theTo, final double theDistance,
                                      final double theSpeedLimit) {
        final int id = theMap.getRoads().size();
        theMap.addRoad(new Road(id, theFrom, theTo, theDistance, theSpeedLimit));
        theMap.addRoad(new Road(id + 1, theTo, theFrom, theDistance, theSpeedLimit));
    }
}