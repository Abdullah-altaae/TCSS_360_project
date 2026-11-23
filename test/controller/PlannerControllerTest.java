package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.EnvironmentSimulator;
import model.EnvironmentSimulator.WeatherCondition;
import model.Hazard;
import model.Intersection;
import model.MapGraph;
import model.Road;
import model.RoutePlanner;
import model.SafetyRating;

/**
 * Unit tests for {@link PlannerController}.
 * <p>
 * The tests use a small map with three connected intersections in a line
 * (A - B - C) and one isolated intersection (H) that no road reaches.
 * Only the methods that do not need the GUI are tested, including the
 * simulation methods, which work without a view.
 * </p>
 *
 * @author Anjana Virani
 * @version 21 November 2026
 */
class PlannerControllerTest {

    /** The map used by each test. */
    private MapGraph myMap;

    /** The route planner used by each test. */
    private RoutePlanner myPlanner;

    /** The environment simulator used by each test. */
    private EnvironmentSimulator mySimulator;

    /** The controller under test. */
    private PlannerController myController;

    /**
     * Builds a fresh map and controller before each test:
     * A - B - C connected by two-way roads, and H with no roads.
     */
    @BeforeEach
    void setUp() {
        myMap = new MapGraph(new ArrayList<>(), new ArrayList<>());
        myMap.addIntersection(new Intersection("A", 0, 0, false));
        myMap.addIntersection(new Intersection("B", 100, 0, false));
        myMap.addIntersection(new Intersection("C", 200, 0, false));
        myMap.addIntersection(new Intersection("H", 300, 300, false));

        myMap.addRoad(new Road(0, "A", "B", 1.0, 30));
        myMap.addRoad(new Road(1, "B", "A", 1.0, 30));
        myMap.addRoad(new Road(2, "B", "C", 1.0, 30));
        myMap.addRoad(new Road(3, "C", "B", 1.0, 30));

        mySimulator = new EnvironmentSimulator(myMap);
        myPlanner = new RoutePlanner(myMap, mySimulator);
        myController = new PlannerController(myMap, myPlanner, mySimulator);
    }

    // ---------- constructor and setView ----------

    /** A null map is rejected. */
    @Test
    void testConstructorNullMap() {
        assertThrows(NullPointerException.class,
                () -> new PlannerController(null, myPlanner, mySimulator));
    }

    /** A null route planner is rejected. */
    @Test
    void testConstructorNullPlanner() {
        assertThrows(NullPointerException.class,
                () -> new PlannerController(myMap, null, mySimulator));
    }

    /** A null simulator is rejected. */
    @Test
    void testConstructorNullSimulator() {
        assertThrows(NullPointerException.class,
                () -> new PlannerController(myMap, myPlanner, null));
    }

    /** A null view is rejected. */
    @Test
    void testSetViewNull() {
        assertThrows(NullPointerException.class, () -> myController.setView(null));
    }

    /** Planning a route before a view is set is an error. */
    @Test
    void testPlanRouteWithoutView() {
        assertThrows(IllegalStateException.class,
                () -> myController.planRoute("A", "C"));
    }

    // ---------- getIntersectionNames ----------

    /** The names match the map's intersections, in the order they were added. */
    @Test
    void testGetIntersectionNames() {
        assertEquals(List.of("A", "B", "C", "H"), myController.getIntersectionNames());
    }

    /** The returned list cannot be changed by the caller. */
    @Test
    void testGetIntersectionNamesUnmodifiable() {
        final List<String> names = myController.getIntersectionNames();
        assertThrows(UnsupportedOperationException.class, () -> names.add("Z"));
    }

    // ---------- findIntersection ----------

    /** An intersection on the map is found by its name. */
    @Test
    void testFindIntersectionFound() {
        final Intersection found = myController.findIntersection("B");
        assertNotNull(found);
        assertEquals("B", found.getId());
    }

    /** A name that is not on the map gives null. */
    @Test
    void testFindIntersectionNotFound() {
        assertNull(myController.findIntersection("Z"));
    }

    /** A null name gives null instead of crashing. */
    @Test
    void testFindIntersectionNull() {
        assertNull(myController.findIntersection(null));
    }

    // ---------- computeRoute: valid routes ----------

    /** A route between neighbors is one road long. */
    @Test
    void testComputeRouteOneRoad() {
        final List<Road> route = myController.computeRoute("A", "B");
        assertEquals(1, route.size());
        assertEquals("A", route.get(0).getStart());
        assertEquals("B", route.get(0).getEnd());
    }

    /** A route from A to C starts at A, ends at C and passes through B. */
    @Test
    void testComputeRouteTwoRoads() {
        final List<Road> route = myController.computeRoute("A", "C");
        assertEquals(2, route.size());
        assertEquals("A", route.get(0).getStart());
        assertEquals("C", route.get(route.size() - 1).getEnd());
    }

    /** Each road in a route starts where the previous road ended. */
    @Test
    void testComputeRouteIsConnected() {
        final List<Road> route = myController.computeRoute("A", "C");
        for (int i = 1; i < route.size(); i++) {
            assertEquals(route.get(i - 1).getEnd(), route.get(i).getStart());
        }
    }

    /** Routes work in the reverse direction too. */
    @Test
    void testComputeRouteReverse() {
        final List<Road> route = myController.computeRoute("C", "A");
        assertFalse(route.isEmpty());
        assertEquals("C", route.get(0).getStart());
        assertEquals("A", route.get(route.size() - 1).getEnd());
    }

    // ---------- computeRoute: invalid input ----------

    /** The same start and end is an error with a clear message. */
    @Test
    void testComputeRouteSameStartAndEnd() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", "A"));
        assertTrue(ex.getMessage().contains("different"));
    }

    /** A missing start is an error. */
    @Test
    void testComputeRouteNullStart() {
        assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute(null, "C"));
    }

    /** A missing end is an error. */
    @Test
    void testComputeRouteNullEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", null));
    }

    /** A start name that is not on the map is an error that names it. */
    @Test
    void testComputeRouteUnknownStart() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("Z", "C"));
        assertTrue(ex.getMessage().contains("Z"));
    }

    /** An end name that is not on the map is an error that names it. */
    @Test
    void testComputeRouteUnknownEnd() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", "Z"));
        assertTrue(ex.getMessage().contains("Z"));
    }

    /** An intersection that no road reaches has no route. */
    @Test
    void testComputeRouteNoRoute() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", "H"));
        assertTrue(ex.getMessage().contains("No route"));
    }

    // ---------- findRoad and getRoads ----------

    /** A road on the map is found by its ID. */
    @Test
    void testFindRoadFound() {
        final Road road = myController.findRoad(2);
        assertNotNull(road);
        assertEquals("B", road.getStart());
        assertEquals("C", road.getEnd());
    }

    /** An ID that is not on the map gives null. */
    @Test
    void testFindRoadNotFound() {
        assertNull(myController.findRoad(99));
    }

    /** getRoads returns every road on the map. */
    @Test
    void testGetRoads() {
        assertEquals(4, myController.getRoads().size());
    }

    // ---------- weather ----------

    /** The weather starts clear. */
    @Test
    void testWeatherStartsClear() {
        assertEquals(WeatherCondition.CLEAR, myController.getWeather());
    }

    /** setWeather changes the simulator's weather. */
    @Test
    void testSetWeather() {
        myController.setWeather(WeatherCondition.SNOWY);
        assertEquals(WeatherCondition.SNOWY, mySimulator.getWeather());
    }

    /** A null weather is rejected. */
    @Test
    void testSetWeatherNull() {
        assertThrows(NullPointerException.class, () -> myController.setWeather(null));
    }

    /** Bad weather makes the adjusted travel time longer. */
    @Test
    void testAdjustedTravelTimeLongerInRain() {
        final Road road = myController.findRoad(0);
        final double clearTime = myController.getAdjustedTravelTime(road);
        myController.setWeather(WeatherCondition.RAINY);
        assertTrue(myController.getAdjustedTravelTime(road) > clearTime);
    }

    // ---------- blocking intersections ----------

    /** An intersection can be blocked and unblocked. */
    @Test
    void testBlockAndUnblockIntersection() {
        myController.setIntersectionBlocked("B", true);
        assertTrue(myController.findIntersection("B").isBlocked());
        myController.setIntersectionBlocked("B", false);
        assertFalse(myController.findIntersection("B").isBlocked());
    }

    /** Blocking an unknown intersection is an error. */
    @Test
    void testBlockUnknownIntersection() {
        assertThrows(IllegalArgumentException.class,
                () -> myController.setIntersectionBlocked("Z", true));
    }

    // ---------- blocking roads ----------

    /** A road can be blocked and unblocked. */
    @Test
    void testBlockAndUnblockRoad() {
        myController.setRoadBlocked(0, true);
        assertTrue(myController.findRoad(0).isBlocked());
        myController.setRoadBlocked(0, false);
        assertFalse(myController.findRoad(0).isBlocked());
    }

    /** Blocking an unknown road is an error. */
    @Test
    void testBlockUnknownRoad() {
        assertThrows(IllegalArgumentException.class,
                () -> myController.setRoadBlocked(99, true));
    }

    /** Blocking the only road from A to B means there is no route from A to C. */
    @Test
    void testBlockedRoadPreventsRoute() {
        myController.setRoadBlocked(0, true);
        assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", "C"));
    }

    // ---------- traffic ----------

    /** A traffic delay in minutes is stored on the road in hours. */
    @Test
    void testSetTrafficDelay() {
        myController.setTrafficDelay(0, 30);
        assertEquals(0.5, myController.findRoad(0).getTrafficDelay(), 1e-9);
    }

    /** A negative traffic delay is an error. */
    @Test
    void testSetTrafficDelayNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> myController.setTrafficDelay(0, -5));
    }

    /** Setting traffic on an unknown road is an error. */
    @Test
    void testSetTrafficDelayUnknownRoad() {
        assertThrows(IllegalArgumentException.class,
                () -> myController.setTrafficDelay(99, 10));
    }

    // ---------- reset ----------

    /** Reset clears the weather, blocked items and traffic. */
    @Test
    void testResetConditions() {
        myController.setWeather(WeatherCondition.SNOWY);
        myController.setIntersectionBlocked("B", true);
        myController.setRoadBlocked(2, true);
        myController.setTrafficDelay(0, 15);

        myController.resetConditions();

        assertEquals(WeatherCondition.CLEAR, myController.getWeather());
        assertFalse(myController.findIntersection("B").isBlocked());
        assertFalse(myController.findRoad(2).isBlocked());
        assertEquals(0.0, myController.findRoad(0).getTrafficDelay(), 1e-9);
    }

    // ---------- safety checking ----------

    /** A straight route in clear weather has no hazards and a perfect score. */
    @Test
    void testCheckSafetyNoHazards() {
        final PlannerController.SafetyReport report =
                myController.checkSafety(myController.computeRoute("A", "C"));
        assertEquals(100, report.getScore());
        assertEquals(SafetyRating.SAFE, report.getRating());
        assertFalse(report.hasHazards());
        assertFalse(report.hasAlternative());
    }

    /** Snow is reported as a hazard and lowers the score. */
    @Test
    void testCheckSafetySnow() {
        myController.setWeather(WeatherCondition.SNOWY);
        final PlannerController.SafetyReport report =
                myController.checkSafety(myController.computeRoute("A", "C"));
        assertTrue(report.getScore() < 100);
        assertTrue(report.hasHazards());
        assertTrue(report.getWarnings().get(0).contains("Snow"));
    }

    /** A blocked road on the route makes it UNSAFE and is marked by ID. */
    @Test
    void testCheckSafetyBlockedRoad() {
        final List<Road> route = myController.computeRoute("A", "C");
        myController.setRoadBlocked(2, true);
        final PlannerController.SafetyReport report = myController.checkSafety(route);
        assertEquals(0, report.getScore());
        assertEquals(SafetyRating.UNSAFE, report.getRating());
        assertTrue(report.getHazardRoadIds().contains(2));
    }

    /** A blocked intersection on the route is marked by name. */
    @Test
    void testCheckSafetyBlockedIntersection() {
        final List<Road> route = myController.computeRoute("A", "C");
        myController.setIntersectionBlocked("B", true);
        final PlannerController.SafetyReport report = myController.checkSafety(route);
        assertEquals(SafetyRating.UNSAFE, report.getRating());
        assertTrue(report.getHazardIntersections().contains("B"));
    }

    /** A null route is rejected. */
    @Test
    void testCheckSafetyNull() {
        assertThrows(NullPointerException.class, () -> myController.checkSafety(null));
    }

    /** A 90 degree turn is reported as a sharp turn at the corner intersection. */
    @Test
    void testCheckSafetySharpTurn() {
        final MapGraph map = new MapGraph(new ArrayList<>(), new ArrayList<>());
        map.addIntersection(new Intersection("A", 0, 0, false));
        map.addIntersection(new Intersection("B", 100, 0, false));
        map.addIntersection(new Intersection("C", 100, 100, false));
        map.addRoad(new Road(0, "A", "B", 1.0, 30));
        map.addRoad(new Road(1, "B", "C", 1.0, 30));
        final EnvironmentSimulator simulator = new EnvironmentSimulator(map);
        final PlannerController controller = new PlannerController(
                map, new RoutePlanner(map, simulator), simulator);

        final PlannerController.SafetyReport report =
                controller.checkSafety(controller.computeRoute("A", "C"));
        assertTrue(report.getHazardIntersections().contains("B"));
        assertTrue(report.getScore() < 100);
    }

    /**
     * When the fastest route has a sharp turn and a slower straight route
     * exists, the straight route is suggested as a safer alternative.
     */
    @Test
    void testCheckSafetySuggestsSaferRoute() {
        final MapGraph map = new MapGraph(new ArrayList<>(), new ArrayList<>());
        map.addIntersection(new Intersection("S", 0, 0, false));
        map.addIntersection(new Intersection("P", 100, 100, false));
        map.addIntersection(new Intersection("Q", 100, 0, false));
        map.addIntersection(new Intersection("E", 200, 0, false));
        // Fast route with a sharp turn at P.
        map.addRoad(new Road(0, "S", "P", 1.0, 60));
        map.addRoad(new Road(1, "P", "E", 1.0, 60));
        // Slower straight route through Q.
        map.addRoad(new Road(2, "S", "Q", 1.0, 20));
        map.addRoad(new Road(3, "Q", "E", 1.0, 20));
        final EnvironmentSimulator simulator = new EnvironmentSimulator(map);
        final PlannerController controller = new PlannerController(
                map, new RoutePlanner(map, simulator), simulator);

        final List<Road> fastest = controller.computeRoute("S", "E");
        assertEquals("P", fastest.get(0).getEnd());

        final PlannerController.SafetyReport report = controller.checkSafety(fastest);
        assertTrue(report.hasAlternative());
        assertEquals("Q", report.getAlternative().get(0).getEnd());
        assertTrue(report.getAlternativeScore() > report.getScore());
    }

    // ---------- blocked start and end ----------

    /** A blocked start intersection gives a clear message. */
    @Test
    void testComputeRouteBlockedStart() {
        myController.setIntersectionBlocked("A", true);
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", "C"));
        assertTrue(ex.getMessage().contains("Start intersection A is blocked"));
    }

    /** A blocked end intersection gives a clear message. */
    @Test
    void testComputeRouteBlockedEnd() {
        myController.setIntersectionBlocked("C", true);
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> myController.computeRoute("A", "C"));
        assertTrue(ex.getMessage().contains("End intersection C is blocked"));
    }

    // ---------- normal vs simulated time ----------

    /** In clear weather with no traffic, simulated time equals normal time. */
    @Test
    void testSimulatedTimeEqualsNormalWhenClear() {
        final List<Road> route = myController.computeRoute("A", "C");
        assertEquals(myController.getNormalTime(route),
                myController.getSimulatedTime(route), 1e-9);
    }

    /** Rain makes the simulated time longer than the normal time. */
    @Test
    void testSimulatedTimeLongerInRain() {
        final List<Road> route = myController.computeRoute("A", "C");
        myController.setWeather(WeatherCondition.RAINY);
        assertTrue(myController.getSimulatedTime(route) > myController.getNormalTime(route));
    }

    // ---------- traffic intensity ----------

    /** A traffic delay also sets the road's traffic intensity. */
    @Test
    void testTrafficDelaySetsIntensity() {
        myController.setTrafficDelay(0, 10);
        assertEquals(0.5, mySimulator.getTrafficIntensity(myController.findRoad(0)), 1e-9);
        myController.setTrafficDelay(0, 60);
        assertEquals(1.0, mySimulator.getTrafficIntensity(myController.findRoad(0)), 1e-9);
    }

    /** Reset clears the traffic intensity. */
    @Test
    void testResetClearsTrafficIntensity() {
        myController.setTrafficDelay(0, 20);
        myController.resetConditions();
        assertEquals(0.0, mySimulator.getTrafficIntensity(myController.findRoad(0)), 1e-9);
    }

    /** Heavy traffic on a road of the route is reported and marked by road ID. */
    @Test
    void testHeavyTrafficHazardIsLocated() {
        final List<Road> route = myController.computeRoute("A", "C");
        myController.setTrafficDelay(route.get(0).getId(), 20);
        final PlannerController.SafetyReport report = myController.checkSafety(route);
        assertTrue(report.getHazardRoadIds().contains(route.get(0).getId()));
        assertTrue(report.getWarnings().get(0).contains("Heavy traffic"));
        assertTrue(report.getScore() < 100);
    }

    /** Rain with moderate traffic is reported as rain combined with traffic. */
    @Test
    void testRainAndTrafficHazard() {
        final List<Road> route = myController.computeRoute("A", "C");
        myController.setWeather(WeatherCondition.RAINY);
        myController.setTrafficDelay(route.get(0).getId(), 10);
        final PlannerController.SafetyReport report = myController.checkSafety(route);
        assertTrue(report.getWarnings().get(0).contains("Rain combined with heavy traffic"));
    }

    // ---------- database: saving routes ----------

    /** The URL of the database used by the most recent databaseController call. */
    private String myDatabaseUrl;

    /**
     * Creates a controller on the test map that uses a new, empty database file.
     *
     * @return a controller whose database is ready
     * @throws IOException if the temporary file cannot be created
     */
    private PlannerController databaseController() throws IOException {
        final File file = Files.createTempFile("planner-test", ".db").toFile();
        file.delete();
        file.deleteOnExit();
        myDatabaseUrl = "jdbc:sqlite:" + file.getAbsolutePath();
        final PlannerController controller =
                new PlannerController(myMap, myPlanner, mySimulator, myDatabaseUrl);
        assertTrue(controller.initializeDatabase());
        return controller;
    }

    /**
     * Runs a query that returns one number, directly with JDBC, so the checks
     * do not depend on the code being tested.
     *
     * @param theSql the query
     * @return the number in the first column of the first row
     * @throws SQLException if the query fails
     */
    private double queryNumber(final String theSql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(myDatabaseUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(theSql)) {
            assertTrue(rs.next());
            return rs.getDouble(1);
        }
    }

    /** Without a database, saving is not available. */
    @Test
    void testNoDatabase() {
        assertFalse(myController.initializeDatabase());
        assertFalse(myController.hasDatabase());
        assertNotNull(myController.getDatabaseProblem());
        assertThrows(IllegalStateException.class,
                () -> myController.saveRoute(myController.computeRoute("A", "C")));
    }

    /** A new database is ready to use. */
    @Test
    void testDatabaseReady() throws IOException {
        final PlannerController controller = databaseController();
        assertTrue(controller.hasDatabase());
        assertEquals(null, controller.getDatabaseProblem());
    }

    /** Saving a route stores its start, end, path and distance. */
    @Test
    void testSaveRoute() throws IOException, SQLException {
        final PlannerController controller = databaseController();
        final int id = controller.saveRoute(controller.computeRoute("A", "C"));
        assertTrue(id >= 1);
        assertEquals(1.0, queryNumber("SELECT COUNT(*) FROM routes"), 1e-9);
        assertEquals(2.0, queryNumber("SELECT distance FROM routes WHERE route_id = " + id),
                1e-9);
        assertEquals(1.0, queryNumber("SELECT COUNT(*) FROM routes WHERE start_node = 'A' "
                + "AND end_node = 'C' AND path = '0,2'"), 1e-9);
    }

    /** Saving stores a simulation run with the safety score and both times. */
    @Test
    void testSaveStoresSimulationRun() throws IOException, SQLException {
        final PlannerController controller = databaseController();
        final List<Road> route = controller.computeRoute("A", "C");
        controller.setWeather(WeatherCondition.RAINY);
        final int id = controller.saveRoute(route);

        final String where = " FROM simulation_runs WHERE route_id = " + id;
        assertEquals(1.0, queryNumber("SELECT COUNT(*)" + where), 1e-9);
        assertEquals(controller.checkSafety(route).getScore(),
                queryNumber("SELECT safety_rating" + where), 1e-9);
        assertEquals(1.0, queryNumber("SELECT COUNT(*)" + where + " AND weather = 'RAINY'"),
                1e-9);
        assertTrue(queryNumber("SELECT simulated_time" + where)
                > queryNumber("SELECT normal_time" + where));
    }

    /** Saving stores the hazards found on the route. */
    @Test
    void testSaveStoresHazards() throws IOException, SQLException {
        final PlannerController controller = databaseController();
        final List<Road> route = controller.computeRoute("A", "C");
        controller.setWeather(WeatherCondition.SNOWY);
        controller.saveRoute(route);
        assertEquals(1.0, queryNumber("SELECT COUNT(*) FROM hazards "
                + "WHERE hazard_type = '" + Hazard.HazardType.SNOW.name() + "'"), 1e-9);
    }

    /** A route with no hazards saves no hazards. */
    @Test
    void testSaveSafeRouteHasNoHazards() throws IOException, SQLException {
        final PlannerController controller = databaseController();
        controller.saveRoute(controller.computeRoute("A", "C"));
        assertEquals(0.0, queryNumber("SELECT COUNT(*) FROM hazards"), 1e-9);
    }

    /** An empty route cannot be saved. */
    @Test
    void testSaveEmptyRoute() throws IOException {
        final PlannerController controller = databaseController();
        assertThrows(IllegalArgumentException.class, () -> controller.saveRoute(List.of()));
    }
}