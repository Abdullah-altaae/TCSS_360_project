package controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import model.*;
import model.EnvironmentSimulator.WeatherCondition;
import view.PlannerDashboard;

/**
 * The controller in the MVC design. It sits between the
 * {@link PlannerDashboard} (view) and the model classes.
 * <p>
 * The view passes user actions to this class, such as "plan a route from
 * A to G" or "make the weather snowy". The controller checks the input,
 * asks the model to do the work and tells the view what to display. The
 * view never calls the model's logic directly.
 * </p>
 * <p>
 * When a condition changes (weather, traffic or a blocked road or
 * intersection), the last planned route is planned again so the user
 * immediately sees the effect.
 * </p>
 *
 * @author Anjana Virani
 * @version 7 November 2026
 */
public class PlannerController {

    /** Number of minutes in one hour. */
    private static final double MINUTES_PER_HOUR = 60.0;

    /** The city map. */
    private final MapGraph myMap;

    /** The planner used to compute routes. */
    private final RoutePlanner myPlanner;

    /** The simulator that applies weather, traffic and obstacles. */
    private final EnvironmentSimulator mySimulator;

    /** The database the stores the routes, simulations, and hazards. */
    private RouteDatabase myDatabase;

    /** The view that displays results, or null until it is set. */
    private PlannerDashboard myView;

    /** The start of the last successfully planned route, or null. */
    private String myLastStartId;

    /** The end of the last successfully planned route, or null. */
    private String myLastEndId;

   /** The temp object of the last successfully planned route or null.*/
    private Route myLastRoute;


    /**
     * Creates a controller for the given map, route planner and simulator.
     *
     * @param theMap the city map
     * @param thePlanner the route planner
     * @param theSimulator the environment simulator
     * @throws NullPointerException if any argument is null
     */
    public PlannerController(final MapGraph theMap, final RoutePlanner thePlanner,
                             final EnvironmentSimulator theSimulator) {
        myMap = Objects.requireNonNull(theMap, "map must not be null");
        myPlanner = Objects.requireNonNull(thePlanner, "planner must not be null");
        mySimulator = Objects.requireNonNull(theSimulator, "simulator must not be null");
        myDatabase = new RouteDatabase("jdbc:sqlite:RouteDB");
    }

    /**
     * Sets the view that this controller sends results to.
     *
     * @param theView the dashboard to update
     * @throws NullPointerException if theView is null
     */
    public void setView(final PlannerDashboard theView) {
        myView = Objects.requireNonNull(theView, "view must not be null");
    }

    // ------------------------------------------------------------------
    // Data for the view
    // ------------------------------------------------------------------

    /**
     * Returns the names of all intersections on the map, in the order they
     * were added. The view uses these to fill its drop-downs.
     *
     * @return an unmodifiable list of intersection names
     */
    public List<String> getIntersectionNames() {
        final List<String> names = new ArrayList<>();
        for (final Intersection intersection : myMap.getIntersections()) {
            names.add(intersection.getId());
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Returns all roads on the map. The view uses these to fill its road
     * drop-down.
     *
     * @return an unmodifiable list of roads
     */
    public List<Road> getRoads() {
        return Collections.unmodifiableList(new ArrayList<>(myMap.getRoads()));
    }

    /**
     * Finds the intersection with the given name on the map.
     *
     * @param theId the intersection name to look for
     * @return the matching intersection, or null if there is none
     */
    public Intersection findIntersection(final String theId) {
        for (final Intersection intersection : myMap.getIntersections()) {
            if (intersection.getId().equals(theId)) {
                return intersection;
            }
        }
        return null;
    }

    /**
     * Finds the road with the given ID on the map.
     *
     * @param theId the road ID to look for
     * @return the matching road, or null if there is none
     */
    public Road findRoad(final int theId) {
        for (final Road road : myMap.getRoads()) {
            if (road.getId() == theId) {
                return road;
            }
        }
        return null;
    }

    /**
     * Returns the current weather condition.
     *
     * @return the current weather
     */
    public WeatherCondition getWeather() {
        return mySimulator.getWeather();
    }

    /**
     * Returns the travel time of a road adjusted for the current weather
     * and the road's traffic delay.
     *
     * @param theRoad the road
     * @return the adjusted travel time, in hours
     */
    public double getAdjustedTravelTime(final Road theRoad) {
        return mySimulator.calculateAdjustedTravelTime(theRoad);
    }

    // ------------------------------------------------------------------
    // Route planning
    // ------------------------------------------------------------------

    /**
     * Checks the input and computes a route between two intersections.
     * This method does not touch the view, so it can be unit tested
     * without opening a window.
     *
     * @param theStartId the name of the start intersection
     * @param theEndId the name of the end intersection
     * @return the route as an ordered list of roads
     * @throws IllegalArgumentException if either name is missing or unknown,
     *                                  if both names are the same, or if no
     *                                  route exists
     */
    public Route computeRoute(final String theStartId, final String theEndId) {
        if (theStartId == null || theEndId == null) {
            throw new IllegalArgumentException(
                    "Please choose both a start and an end intersection.");
        }
        if (theStartId.equals(theEndId)) {
            throw new IllegalArgumentException(
                    "Start and end intersections must be different.");
        }

        final Intersection start = findIntersection(theStartId);
        final Intersection end = findIntersection(theEndId);
        if (start == null || end == null) {
            throw new IllegalArgumentException("Unknown intersection: "
                    + (start == null ? theStartId : theEndId));
        }

        final List<Road> path = myPlanner.findRoute(start, end);
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException(
                    "No route found from " + theStartId + " to " + theEndId + ".");
        }
        return new Route(theStartId, theEndId, path);
    }

    /**
     * Handles a "Plan Route" request from the view. Computes the route and
     * tells the view to show it, or tells the view to show an error and
     * clears the old route from the map.
     *
     * @param theStartId the name of the start intersection
     * @param theEndId the name of the end intersection
     * @throws IllegalStateException if no view has been set
     */
    public void planRoute(final String theStartId, final String theEndId) {
        if (myView == null) {
            throw new IllegalStateException("setView must be called before planRoute");
        }
        try {
            Route tempRoute = computeRoute(theStartId, theEndId);
            myView.showRoute(tempRoute.getPath());
            myLastStartId = theStartId;
            myLastEndId = theEndId;
            myLastRoute = tempRoute;
        } catch (final IllegalArgumentException ex) {
            myView.clearRoute();
            myView.showError(ex.getMessage());
        } catch (final RuntimeException ex) {
            myView.clearRoute();
            myView.showError("Route planning failed: " + ex.getMessage());
        }
    }

    /**
     * Saves the most recently planned route to the database and tells the view
     * whether the save worked. If no route has been planned yet, the view shows
     * an error; if this route is already saved, the view shows a message and
     * nothing is saved again. A failed save is reported to the view as an error.
     */
    public void saveRoute() {
        if (myLastRoute == null) {
            myView.showError("Plan a route before saving.");
        } else if (myLastRoute.isSaved()) {
            showMessage("This route is already saved.");
        }
        else {
            try {
                int id = myDatabase.saveRoute(myLastRoute.getStart(), myLastRoute.getEnd(), myLastRoute.getPathString(), myLastRoute.getDistance());
                myLastRoute.withId(id);
                showMessage("Route saved.");
            } catch (SQLException ex) {
                myView.showError("Could not save the route: " + ex.getMessage());
            }
        }
    }


    // ------------------------------------------------------------------
    // Environment simulation
    // ------------------------------------------------------------------

    /**
     * Changes the weather.
     *
     * @param theWeather the new weather condition
     * @throws NullPointerException if theWeather is null
     */
    public void setWeather(final WeatherCondition theWeather) {
        Objects.requireNonNull(theWeather, "weather must not be null");
        mySimulator.setWeather(theWeather);
        conditionsChanged("Weather set to " + theWeather
                + " (risk factor " + theWeather.getRiskFactor() + ").");
    }

    /**
     * Blocks or unblocks an intersection.
     *
     * @param theId the name of the intersection
     * @param theBlocked true to block it, false to unblock it
     * @throws IllegalArgumentException if no intersection has that name
     */
    public void setIntersectionBlocked(final String theId, final boolean theBlocked) {
        final Intersection intersection = findIntersection(theId);
        if (intersection == null) {
            throw new IllegalArgumentException("Unknown intersection: " + theId);
        }
        if (theBlocked) {
            mySimulator.blockIntersection(intersection);
        } else {
            mySimulator.unblockIntersection(intersection);
        }
        conditionsChanged("Intersection " + theId
                + (theBlocked ? " blocked." : " unblocked."));
    }

    /**
     * Blocks or unblocks a road.
     *
     * @param theId the ID of the road
     * @param theBlocked true to block it, false to unblock it
     * @throws IllegalArgumentException if no road has that ID
     */
    public void setRoadBlocked(final int theId, final boolean theBlocked) {
        final Road road = requireRoad(theId);
        if (theBlocked) {
            mySimulator.blockRoad(road);
        } else {
            mySimulator.unBlockedRoad(road);
        }
        conditionsChanged("Road " + road.getStart() + " -> " + road.getEnd()
                + (theBlocked ? " blocked." : " unblocked."));
    }

    /**
     * Sets the traffic delay on a road.
     *
     * @param theId the ID of the road
     * @param theMinutes the extra travel time caused by traffic, in minutes
     * @throws IllegalArgumentException if no road has that ID, or if
     *                                  theMinutes is negative or not a number
     */
    public void setTrafficDelay(final int theId, final double theMinutes) {
        if (!Double.isFinite(theMinutes) || theMinutes < 0) {
            throw new IllegalArgumentException(
                    "Traffic delay must be zero or more minutes, was " + theMinutes);
        }
        final Road road = requireRoad(theId);
        mySimulator.changeTraffic(road, theMinutes / MINUTES_PER_HOUR);
        conditionsChanged(String.format("Traffic on road %s -> %s set to %.0f minute(s).",
                road.getStart(), road.getEnd(), theMinutes));
    }

    /**
     * Resets every condition: clear weather, no traffic, and no blocked
     * roads or intersections.
     */
    public void resetConditions() {
        mySimulator.setWeather(WeatherCondition.CLEAR);
        for (final Intersection intersection : myMap.getIntersections()) {
            mySimulator.unblockIntersection(intersection);
        }
        for (final Road road : myMap.getRoads()) {
            mySimulator.unBlockedRoad(road);
            mySimulator.changeTraffic(road, 0.0);
        }
        conditionsChanged("All conditions reset to normal.");
    }

    /**
     * Returns the road with the given ID.
     *
     * @param theId the road ID
     * @return the matching road
     * @throws IllegalArgumentException if no road has that ID
     */
    private Road requireRoad(final int theId) {
        final Road road = findRoad(theId);
        if (road == null) {
            throw new IllegalArgumentException("Unknown road: " + theId);
        }
        return road;
    }

    /**
     * Updates the view after a condition changes: logs the change, redraws
     * the map and plans the last route again. Does nothing to the view if
     * none has been set, so the simulation methods can be unit tested.
     *
     * @param theMessage a description of the change for the safety log
     */
    private void conditionsChanged(final String theMessage) {
        if (myView == null) {
            return;
        }
        myView.showMessage(theMessage);
        myView.refreshMap();
        if (myLastStartId != null && myLastEndId != null) {
            planRoute(myLastStartId, myLastEndId);
        }
    }


}