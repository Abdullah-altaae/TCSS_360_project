package controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * <p>
 * After every route is planned, the controller asks the
 * {@link SafetyChecker} for the route's hazards and safety rating and
 * sends a {@link SafetyReport} to the view.
 * </p>
 * <p>
 * When a database URL is given, routes can be saved together with a
 * simulation run and its hazards ({@link RouteDatabase}).
 * </p>
 *
 * @author Anjana Virani
 * @version 21 November 2026
 */
public class PlannerController {

    /** How many routes to ask the planner for when looking for a safer one. */
    private static final int ALTERNATIVES_TO_CHECK = 3;

    /**
     * The traffic delay, in minutes, that counts as the heaviest traffic
     * (intensity 1.0). A 10 minute delay is intensity 0.5.
     */
    private static final double MINUTES_FOR_FULL_TRAFFIC = 20.0;

    /** Traffic intensity at which SafetyChecker reports heavy traffic. */
    private static final double HEAVY_TRAFFIC = 0.7;

    /** Traffic intensity at which SafetyChecker reports rain with traffic. */
    private static final double RAIN_TRAFFIC = 0.5;

    /** Number of minutes in one hour. */
    private static final double MINUTES_PER_HOUR = 60.0;

    /** The Url used to create the RouteDatabase object. */
    private static final String myDBUrl = "jdbc:sqlite:CarPlanner_Database.db";

    /** The city map. */
    private final MapGraph myMap;

    /** The planner used to compute routes. */
    private final RoutePlanner myPlanner;

    /** The simulator that applies weather, traffic and obstacles. */
    private final EnvironmentSimulator mySimulator;

    /** The safety checker that finds hazards on a route. */
    private final SafetyChecker mySafetyChecker;

    /** Writes routes to the database, or null if there is no database. */
    private final RouteDatabase myDatabase;

    /** Whether the database tables are ready to use. */
    private boolean myDatabaseReady;

    /** Why the database cannot be used, or null if it can. */
    private String myDatabaseProblem = "No database was set up.";

    /** The view that displays results, or null until it is set. */
    private PlannerDashboard myView;

    /** The start of the last successfully planned route, or null. */
    private String myLastStartId;

    /** The end of the last successfully planned route, or null. */
    private String myLastEndId;

    /** The last route shown to the user, or null. */
    private Route myLastRoute;

    /** The last simulation run that was created or saved, or null. */
    private SimulationRun myLastSimulationRun;

    /** The last hazard that was selected or saved, or null. */
    private Hazard myLastHazard;

    /**
     * Creates a controller for the given map, route planner and simulator
     * that saves to the default database file. Call
     * {@link #initializeDatabase()} before saving routes.
     *
     * @param theMap the city map
     * @param thePlanner the route planner
     * @param theSimulator the environment simulator
     * @throws NullPointerException if any argument is null
     */
    public PlannerController(final MapGraph theMap, final RoutePlanner thePlanner,
                             final EnvironmentSimulator theSimulator) {
        this(theMap, thePlanner, theSimulator, myDBUrl);
    }

    /**
     * Creates a controller for the given map, route planner, simulator and
     * database. Call {@link #initializeDatabase()} before saving routes.
     *
     * @param theMap the city map
     * @param thePlanner the route planner
     * @param theSimulator the environment simulator
     * @param theDatabaseUrl the database URL, for example
     *                       "jdbc:sqlite:CarPlanner_DataBase.db", or null for
     *                       no database
     * @throws NullPointerException if theMap, thePlanner or theSimulator is null
     */
    public PlannerController(final MapGraph theMap, final RoutePlanner thePlanner,
                             final EnvironmentSimulator theSimulator,
                             final String theDatabaseUrl) {
        myMap = Objects.requireNonNull(theMap, "map must not be null");
        myPlanner = Objects.requireNonNull(thePlanner, "planner must not be null");
        mySimulator = Objects.requireNonNull(theSimulator, "simulator must not be null");
        mySafetyChecker = new SafetyChecker(mySimulator, myMap);
        myDatabase = theDatabaseUrl == null ? null : new RouteDatabase(theDatabaseUrl);
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
        if (start.isBlocked()) {
            throw new IllegalArgumentException(
                    "Start intersection " + theStartId + " is blocked.");
        }
        if (end.isBlocked()) {
            throw new IllegalArgumentException(
                    "End intersection " + theEndId + " is blocked.");
        }

        final List<Road> route = myPlanner.findRoute(start, end);
        if (route == null || route.isEmpty()) {
            throw new IllegalArgumentException(
                    "No route found from " + theStartId + " to " + theEndId + ".");
        }
        return new Route(theStartId, theEndId, route);
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
        planRoute(theStartId, theEndId, false);
    }

    /**
     * Plans a route and tells the view the result. Automatic re-plans (after
     * a condition changes) only log errors, so the user does not get a
     * pop-up for every change.
     *
     * @param theStartId the name of the start intersection
     * @param theEndId the name of the end intersection
     * @param theAutomatic true if this is an automatic re-plan
     * @throws IllegalStateException if no view has been set
     */
    private void planRoute(final String theStartId, final String theEndId,
                           final boolean theAutomatic) {
        if (myView == null) {
            throw new IllegalStateException("setView must be called before planRoute");
        }
        try {
            Route route = computeRoute(theStartId, theEndId);
            if (myLastRoute != null && myLastRoute.isSaved()
                    && myLastRoute.getStart().equals(theStartId)
                    && myLastRoute.getEnd().equals(theEndId)) {
                // Replanning a saved route (for example after the weather
                // changes) keeps its ID, so saving it updates the saved
                // route instead of adding a duplicate.
                route = route.withId(myLastRoute.getId());
            }
            myLastRoute = route;
            myLastStartId = theStartId;
            myLastEndId = theEndId;
            myView.showRoute(route.getPath());
            myView.showSafetyReport(checkSafety(route.getPath()));
        } catch (final RuntimeException ex) {
            final String message = ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "Route planning failed: " + ex.getMessage();
            myLastRoute = null;
            myView.clearRoute();
            if (theAutomatic) {
                myView.logError(message);
            } else {
                myView.showError(message);
            }
        }
    }

    /**
     * Returns a route's travel time at the speed limits, with no weather or
     * traffic.
     *
     * @param theRoute the route
     * @return the normal travel time, in hours
     */
    public double getNormalTime(final List<Road> theRoute) {
        return myPlanner.getNormalTime(theRoute);
    }

    /**
     * Returns a route's travel time under the current weather and traffic.
     *
     * @param theRoute the route
     * @return the simulated travel time, in hours
     */
    public double getSimulatedTime(final List<Road> theRoute) {
        return myPlanner.getSimulatedTime(theRoute);
    }

    // ------------------------------------------------------------------
    // Saving routes (database)
    // ------------------------------------------------------------------

    /**
     * Creates the database tables if they do not exist yet. Saving only works
     * after this returns true.
     *
     * @return true if the database is ready, false if there is no database
     *         or it cannot be used (see {@link #getDatabaseProblem()})
     */
    public boolean initializeDatabase() {
        myDatabaseReady = false;
        if (myDatabase == null) {
            myDatabaseProblem = "No database was set up.";
            return false;
        }
        try {
            myDatabase.createTables();
            myDatabaseReady = true;
            myDatabaseProblem = null;
        } catch (final SQLException ex) {
            myDatabaseProblem = "The database cannot be used (" + ex.getMessage() + ").";
        }
        return myDatabaseReady;
    }

    /**
     * Returns whether routes can be saved.
     *
     * @return true if the database is ready
     */
    public boolean hasDatabase() {
        return myDatabaseReady;
    }

    /**
     * Returns why the database cannot be used.
     *
     * @return a description of the problem, or null if the database is ready
     */
    public String getDatabaseProblem() {
        return myDatabaseProblem;
    }

    /**
     * Saves a route to the database together with one simulation run under the
     * current conditions (weather, average traffic, safety score, normal and
     * simulated travel times) and every hazard found on it. If the route is
     * the same as the last route and that one is already saved, it is not
     * saved again; only a new simulation run is added for it.
     *
     * @param theRoute the route to save
     * @return the saved route's database ID
     * @throws IllegalStateException if the database is not ready
     * @throws IllegalArgumentException if theRoute is empty
     * @throws NullPointerException if theRoute is null
     * @throws SQLException if the route cannot be saved
     */
    public int saveRoute(final List<Road> theRoute) throws SQLException {
        requireDatabase();
        Objects.requireNonNull(theRoute, "route must not be null");
        if (theRoute.isEmpty()) {
            throw new IllegalArgumentException("There is no route to save.");
        }
        final Route route = new Route(theRoute.get(0).getStart(),
                theRoute.get(theRoute.size() - 1).getEnd(), theRoute);
        if (myLastRoute == null || routeChanged(myLastRoute, route)) {
            myLastRoute = route;
        }
        storeRoute();
        myLastSimulationRun = new SimulationRun(myLastRoute,
                mySimulator.averageTrafficIntensity(myLastRoute), mySimulator.getWeather(),
                mySafetyChecker.findHazards(theRoute),
                mySafetyChecker.calculateSafetyRating(theRoute),
                getNormalTime(theRoute), getSimulatedTime(theRoute));
        storeSimulationRun();
        return myLastRoute.getId();
    }

    /**
     * Handles File &gt; Save Route: saves the route currently shown and
     * reports the result in the view.
     */
    public void handleSaveRoute() {
        if (!myDatabaseReady) {
            myView.showError("Routes cannot be saved. " + myDatabaseProblem);
            return;
        }
        if (myLastRoute == null) {
            myView.showError("Plan a route before saving it.");
            return;
        }
        try {
            final int id = saveRoute(myLastRoute.getPath());
            myView.showMessage("Route saved as #" + id + " ("
                    + myLastRoute.getStart() + " -> " + myLastRoute.getEnd()
                    + ", weather " + getWeather() + ").");
        } catch (final SQLException ex) {
            myView.showError("Could not save the route: " + ex.getMessage()
                    + ". If the database file was made by an older version of the "
                    + "program, delete it and restart.");
        }
    }

    /**
     * Checks that the database is ready.
     *
     * @throws IllegalStateException if it is not
     */
    private void requireDatabase() {
        if (!myDatabaseReady) {
            throw new IllegalStateException("The database is not ready: " + myDatabaseProblem);
        }
    }

    /** What happened when the controller was asked to save an object. */
    public enum SaveResult {
        /** The object was new and was inserted into the database. */
        SAVED,
        /** The object was already saved, had changes, and was updated. */
        UPDATED,
        /** The object was already saved and matched the database, so nothing was written. */
        NO_CHANGES
    }

    public void setLastRoute(final Route theRoute) {
        myLastRoute = Objects.requireNonNull(theRoute, "route must not be null");
    }

    /**
     * Returns the last route shown to the user.
     *
     * @return the last route, or null if no route is shown
     */
    public Route getLastRoute() {
        return myLastRoute;
    }

    public void setLastSimulationRun(final SimulationRun theRun) {
        myLastSimulationRun = Objects.requireNonNull(theRun, "simulation run must not be null");
    }

    public SimulationRun getLastSimulationRun() {
        return myLastSimulationRun;
    }

    public void setLastHazard(final Hazard theHazard) {
        myLastHazard = Objects.requireNonNull(theHazard, "hazard must not be null");
    }

    public Hazard getLastHazard() {
        return myLastHazard;
    }

    /**
     * Saves the last planned route. A new route is inserted, and a saved
     * route is updated only if it differs from the database copy. This
     * method does not touch the view, so it can be unit tested.
     *
     * @return whether the route was saved, updated or unchanged
     * @throws IllegalStateException if there is no route to save
     * @throws SQLException if the database cannot be read or written
     */
    public SaveResult storeRoute() throws SQLException {
        if (myLastRoute == null) {
            throw new IllegalStateException("Create a route before attempting to save.");
        }
        requireDatabase();
        final Route route = myLastRoute;
        if (!route.isSaved()) {
            final int routeId = myDatabase.saveRoute(route.getStart(), route.getEnd(),
                    route.getPathString(), route.getDistance());
            myLastRoute = route.withId(routeId);
            return SaveResult.SAVED;
        }
        final Route stored = myDatabase.loadRoute(route.getId(), myMap);
        if (!routeChanged(stored, route)) {
            return SaveResult.NO_CHANGES;
        }
        myDatabase.updateRoute(route.getId(), route.getStart(), route.getEnd(),
                route.getPathString(), route.getDistance());
        return SaveResult.UPDATED;
    }

    /**
     * Saves the last simulation run and its hazards. A new run is inserted
     * with its hazards, and a saved run is updated only if it or its hazards
     * differ from the database copy. The run's route must be saved first.
     * This method does not touch the view, so it can be unit tested.
     *
     * @return whether the run was saved, updated or unchanged
     * @throws IllegalStateException if there is no run to save, or its route
     *                               has not been saved
     * @throws SQLException if the database cannot be read or written
     */
    public SaveResult storeSimulationRun() throws SQLException {
        if (myLastSimulationRun == null) {
            throw new IllegalStateException(
                    "Run a simulation before attempting to save it.");
        }
        requireDatabase();
        final SimulationRun run = myLastSimulationRun;
        final Route savedRoute = findSavedRoute(run.getRoute());

        if (!run.isSaved()) {
            final int simId = myDatabase.saveSimulationRun(savedRoute.getId(),
                    run.getAverageTraffic(), run.getWeather(), run.getSafetyRating(),
                    run.getNormalTime(), run.getSimulatedTime());
            final List<Hazard> savedHazards = myDatabase.replaceHazards(simId, run.getHazards());
            myLastSimulationRun = run.withSavedData(simId, savedRoute, savedHazards);
            return SaveResult.SAVED;
        }

        final SimulationRun stored = myDatabase.loadSimulationRun(run.getId(), myMap);
        final boolean runChanged = simulationRunChanged(stored, run, savedRoute.getId());
        final boolean hazardsChanged = hazardsChanged(stored.getHazards(), run.getHazards());
        if (!runChanged && !hazardsChanged) {
            return SaveResult.NO_CHANGES;
        }
        if (runChanged) {
            myDatabase.updateSimulationRun(run.getId(), savedRoute.getId(),
                    run.getAverageTraffic(), run.getWeather(), run.getSafetyRating(),
                    run.getNormalTime(), run.getSimulatedTime());
        }
        final List<Hazard> hazards = hazardsChanged
                ? myDatabase.replaceHazards(run.getId(), run.getHazards())
                : stored.getHazards();
        myLastSimulationRun = run.withSavedData(run.getId(), savedRoute, hazards);
        return SaveResult.UPDATED;
    }

    /**
     * Saves the last selected hazard. A new hazard is inserted, and a saved
     * hazard is updated only if it differs from the database copy. This
     * method does not touch the view, so it can be unit tested.
     *
     * @return whether the hazard was saved, updated or unchanged
     * @throws IllegalStateException if there is no hazard to save, or its
     *                               simulation run has not been saved
     * @throws SQLException if the database cannot be read or written
     */
    public SaveResult storeHazard() throws SQLException {
        if (myLastHazard == null) {
            throw new IllegalStateException("Select a hazard before attempting to save.");
        }
        final Hazard hazard = myLastHazard;
        if (hazard.getSimId() == Hazard.UNSAVED_ID) {
            throw new IllegalStateException(
                    "Save the hazard's simulation run before saving the hazard.");
        }
        requireDatabase();
        if (!hazard.isSaved()) {
            final int hazardId = myDatabase.saveHazard(hazard.getSimId(), hazard.getType(),
                    hazard.getPenalty());
            myLastHazard = hazard.withIds(hazardId, hazard.getSimId());
            return SaveResult.SAVED;
        }
        final Hazard stored = myDatabase.loadHazard(hazard.getId());
        if (!hazardChanged(stored, hazard)) {
            return SaveResult.NO_CHANGES;
        }
        myDatabase.updateHazard(hazard.getId(), hazard.getSimId(), hazard.getType(),
                hazard.getPenalty());
        return SaveResult.UPDATED;
    }

    /**
     * Returns the saved version of a simulation run's route. The run may hold
     * an unsaved copy of a route that has since been saved: the route was
     * planned, the run was created, and then the route was saved, which gave
     * {@link #myLastRoute} an ID but left the run's copy without one. In that
     * case the saved last route is used if it matches the run's route.
     *
     * @param theRoute the route held by the simulation run
     * @return a saved route with the same data as theRoute
     * @throws IllegalStateException if no saved route matches theRoute
     */
    private Route findSavedRoute(final Route theRoute) {
        if (theRoute.isSaved()) {
            return theRoute;
        }
        if (myLastRoute != null && myLastRoute.isSaved() && !routeChanged(myLastRoute, theRoute)) {
            return myLastRoute;
        }
        throw new IllegalStateException(
                "Save the route before saving its simulation run.");
    }

    private static boolean routeChanged(final Route theStored, final Route theCurrent) {
        return !theStored.getStart().equals(theCurrent.getStart())
                || !theStored.getEnd().equals(theCurrent.getEnd())
                || !theStored.getPathString().equals(theCurrent.getPathString())
                || Double.compare(theStored.getDistance(), theCurrent.getDistance()) != 0;
    }

    private static boolean simulationRunChanged(final SimulationRun theStored,
                                                final SimulationRun theCurrent,
                                                final int theRouteId) {
        return theStored.getRoute().getId() != theRouteId
                || Double.compare(theStored.getAverageTraffic(), theCurrent.getAverageTraffic()) != 0
                || theStored.getWeather() != theCurrent.getWeather()
                || theStored.getSafetyRating() != theCurrent.getSafetyRating()
                || Double.compare(theStored.getNormalTime(), theCurrent.getNormalTime()) != 0
                || Double.compare(theStored.getSimulatedTime(), theCurrent.getSimulatedTime()) != 0;
    }

    /**
     * Returns whether two lists of hazards differ. Hazards only store their
     * type, so the lists are the same when they hold the same types the same
     * number of times, in any order. Database IDs are ignored.
     *
     * @param theStored the hazards saved in the database
     * @param theCurrent the hazards being saved
     * @return true if the lists differ, false otherwise
     */
    private static boolean hazardsChanged(final List<Hazard> theStored,
                                          final List<Hazard> theCurrent) {
        return !countTypes(theStored).equals(countTypes(theCurrent));
    }

    private static Map<Hazard.HazardType, Integer> countTypes(final List<Hazard> theHazards) {
        final Map<Hazard.HazardType, Integer> counts = new EnumMap<>(Hazard.HazardType.class);
        for (final Hazard hazard : theHazards) {
            counts.merge(hazard.getType(), 1, Integer::sum);
        }
        return counts;
    }

    private static boolean hazardChanged(final Hazard theStored, final Hazard theCurrent) {
        return theStored.getSimId() != theCurrent.getSimId()
                || theStored.getType() != theCurrent.getType();
    }

    // ------------------------------------------------------------------
    // Safety checking
    // ------------------------------------------------------------------

    /**
     * Checks a route for hazards using the {@link SafetyChecker}. The score
     * and rating come from the SafetyChecker. Because a {@link Hazard} only
     * stores its type, the locations of the hazards are found with the
     * SafetyChecker's helper methods so the view can mark them on the map.
     * If the route has hazards, the planner's alternative routes are checked
     * too, and the safest one with a higher score is suggested.
     * This method does not touch the view, so it can be unit tested.
     *
     * @param theRoute the route to check
     * @return a report of the route's hazards, score and rating
     * @throws NullPointerException if theRoute is null
     */
    public SafetyReport checkSafety(final List<Road> theRoute) {
        Objects.requireNonNull(theRoute, "route must not be null");
        final List<String> warnings = new ArrayList<>();
        final Set<String> hazardIntersections = new LinkedHashSet<>();
        final Set<Integer> hazardRoads = new LinkedHashSet<>();

        final List<Intersection> stops = mySafetyChecker.findRouteIntersections(theRoute);

        for (final Intersection blocked : mySafetyChecker.findBlockedIntersections(stops)) {
            warnings.add("Blocked intersection: " + blocked.getId());
            hazardIntersections.add(blocked.getId());
        }
        for (final Road blocked : mySafetyChecker.findBlockedRoads(theRoute)) {
            warnings.add("Blocked road: " + blocked.getStart() + " -> " + blocked.getEnd());
            hazardRoads.add(blocked.getId());
        }
        for (final Intersection turn : mySafetyChecker.findSharpTurns(stops)) {
            warnings.add("Sharp turn at " + turn.getId());
            hazardIntersections.add(turn.getId());
        }

        // Traffic hazards use the same rules as SafetyChecker.findHazards,
        // checked road by road so each one can be marked on the map.
        final boolean rainy = mySimulator.getWeather() == WeatherCondition.RAINY;
        for (final Road road : theRoute) {
            final double intensity = mySimulator.getTrafficIntensity(road);
            final String where = road.getStart() + " -> " + road.getEnd();
            if (rainy && intensity >= RAIN_TRAFFIC) {
                warnings.add("Rain combined with heavy traffic on " + where);
                hazardRoads.add(road.getId());
            } else if (intensity >= HEAVY_TRAFFIC) {
                warnings.add("Heavy traffic on " + where);
                hazardRoads.add(road.getId());
            }
        }

        for (final Hazard hazard : mySafetyChecker.findHazards(theRoute)) {
            if (hazard.getType() == Hazard.HazardType.SNOW) {
                warnings.add("Snowy weather: slower speeds and less traction");
            }
        }

        final int score = mySafetyChecker.calculateSafetyRating(theRoute);
        final List<Road> alternative = warnings.isEmpty()
                ? List.of() : findSaferRoute(theRoute, score);

        return new SafetyReport(score, mySafetyChecker.getSafetyRating(theRoute), warnings,
                hazardIntersections, hazardRoads, alternative,
                alternative.isEmpty() ? 0 : mySafetyChecker.calculateSafetyRating(alternative));
    }

    /**
     * Looks for a route between the same start and end with a higher safety
     * score than the given route.
     *
     * @param theRoute the current route
     * @param theScore the current route's safety score
     * @return the safest alternative route with a higher score, or an empty
     *         list if there is none
     */
    private List<Road> findSaferRoute(final List<Road> theRoute, final int theScore) {
        if (theRoute.isEmpty()) {
            return List.of();
        }
        final Intersection start = findIntersection(theRoute.get(0).getStart());
        final Intersection end = findIntersection(theRoute.get(theRoute.size() - 1).getEnd());
        if (start == null || end == null) {
            return List.of();
        }
        List<Road> best = List.of();
        int bestScore = theScore;
        for (final List<Road> candidate
                : myPlanner.findAlternativeRoutes(start, end, ALTERNATIVES_TO_CHECK)) {
            final int candidateScore = mySafetyChecker.calculateSafetyRating(candidate);
            if (!candidate.equals(theRoute) && candidateScore > bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }
        return best;
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
     * Sets the traffic delay on a road. The delay also sets the road's
     * traffic intensity for the SafetyChecker: 20 minutes or more is the
     * heaviest traffic (1.0), and 10 minutes is 0.5.
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
        mySimulator.setTrafficIntensity(road,
                Math.min(1.0, theMinutes / MINUTES_FOR_FULL_TRAFFIC));
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
            mySimulator.setTrafficIntensity(road, 0.0);
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
            planRoute(myLastStartId, myLastEndId, true);
        }
    }

    /**
     * The result of checking a route for hazards: a score from 0 to 100,
     * a rating (SAFE, MODERATE or UNSAFE), a readable warning for each
     * hazard, the intersections and roads where hazards were found, and a
     * safer alternative route if one exists.
     * All lists and sets are unmodifiable.
     */
    public static final class SafetyReport {

        /** The safety score from 0 to 100. */
        private final int myScore;

        /** The safety rating. */
        private final SafetyRating myRating;

        /** One readable warning per hazard. */
        private final List<String> myWarnings;

        /** Names of intersections where hazards were found. */
        private final Set<String> myHazardIntersections;

        /** IDs of roads where hazards were found. */
        private final Set<Integer> myHazardRoadIds;

        /** A safer alternative route, or an empty list if there is none. */
        private final List<Road> myAlternative;

        /** The alternative route's safety score, or 0 if there is none. */
        private final int myAlternativeScore;

        /**
         * Creates a safety report.
         *
         * @param theScore the safety score from 0 to 100
         * @param theRating the safety rating
         * @param theWarnings one readable warning per hazard
         * @param theHazardIntersections names of intersections with hazards
         * @param theHazardRoadIds IDs of roads with hazards
         * @param theAlternative a safer route, or an empty list if there is none
         * @param theAlternativeScore the safer route's score, or 0 if there is none
         */
        public SafetyReport(final int theScore, final SafetyRating theRating,
                            final List<String> theWarnings,
                            final Set<String> theHazardIntersections,
                            final Set<Integer> theHazardRoadIds,
                            final List<Road> theAlternative,
                            final int theAlternativeScore) {
            myScore = theScore;
            myRating = theRating;
            myWarnings = List.copyOf(theWarnings);
            myHazardIntersections = Collections.unmodifiableSet(
                    new LinkedHashSet<>(theHazardIntersections));
            myHazardRoadIds = Collections.unmodifiableSet(
                    new LinkedHashSet<>(theHazardRoadIds));
            myAlternative = List.copyOf(theAlternative);
            myAlternativeScore = theAlternativeScore;
        }

        /**
         * Returns a safer alternative route.
         *
         * @return the alternative route, or an empty list if there is none
         */
        public List<Road> getAlternative() {
            return myAlternative;
        }

        /**
         * Returns the alternative route's safety score.
         *
         * @return the score from 0 to 100, or 0 if there is no alternative
         */
        public int getAlternativeScore() {
            return myAlternativeScore;
        }

        /**
         * Returns whether a safer alternative route was found.
         *
         * @return true if there is an alternative route
         */
        public boolean hasAlternative() {
            return !myAlternative.isEmpty();
        }

        /**
         * Returns the safety score.
         *
         * @return the score from 0 to 100
         */
        public int getScore() {
            return myScore;
        }

        /**
         * Returns the safety rating.
         *
         * @return SAFE, MODERATE or UNSAFE
         */
        public SafetyRating getRating() {
            return myRating;
        }

        /**
         * Returns the warnings, one per hazard.
         *
         * @return an unmodifiable list of warnings
         */
        public List<String> getWarnings() {
            return myWarnings;
        }

        /**
         * Returns the names of intersections where hazards were found.
         *
         * @return an unmodifiable set of intersection names
         */
        public Set<String> getHazardIntersections() {
            return myHazardIntersections;
        }

        /**
         * Returns the IDs of roads where hazards were found.
         *
         * @return an unmodifiable set of road IDs
         */
        public Set<Integer> getHazardRoadIds() {
            return myHazardRoadIds;
        }

        /**
         * Returns whether any hazards were found.
         *
         * @return true if there is at least one warning
         */
        public boolean hasHazards() {
            return !myWarnings.isEmpty();
        }
    }
}