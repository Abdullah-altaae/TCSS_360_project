package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentSimulatorTest {
    private MapGraph myGraph;
    private EnvironmentSimulator mySimulator;
    private Road myRoad;
    private Intersection myIntersection;

    @BeforeEach
    void setUp() {
        myGraph = new MapGraph();

        myIntersection = new Intersection("A");
        Intersection intersectionB = new Intersection("B");

        myGraph.addIntersection(myIntersection);
        myGraph.addIntersection(intersectionB);

        myRoad = new Road(1, "A", "B", 10.0, 60.0, 0.0);
        myGraph.addRoad(myRoad);

        mySimulator = new EnvironmentSimulator(myGraph);
    }

    // ==================== WEATHER TESTS ====================
    /**
     * Tests that the environment simulator initially uses clear weather.
     */
    @Test
    void testInitialWeather() {
        assertEquals(EnvironmentSimulator.WeatherCondition.CLEAR,
                mySimulator.getWeather());
    }

    /**
     * Tests that the weather condition can be changed.
     */
    @Test
    void testSetWeather() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.RAINY);

        assertEquals(EnvironmentSimulator.WeatherCondition.RAINY,
                mySimulator.getWeather());
    }

    /**
     * Tests that rainy weather reduces the road's adjusted speed.
     */
    @Test
    void testCalculateAdjustedSpeedRainy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.RAINY);

        double adjustedSpeed = mySimulator.calculateAdjustedSpeed(myRoad);

        assertEquals(48.0, adjustedSpeed, 0.001);
    }

    /**
     * Tests that snowy weather reduces the road's adjusted speed.
     */
    @Test
    void testCalculateAdjustedSpeedSnowy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.SNOWY);

        double adjustedSpeed = mySimulator.calculateAdjustedSpeed(myRoad);

        assertEquals(30.0, adjustedSpeed, 0.001);
    }

    /**
     * Tests that clear weather does not reduce the road's adjusted speed.
     */
    @Test
    void testCalculateAdjustedSpeedClear() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.CLEAR);

        double adjustedSpeed = mySimulator.calculateAdjustedSpeed(myRoad);

        assertEquals(60.0, adjustedSpeed, 0.001);
    }

    /**
     * Tests the adjusted travel time under clear weather conditions.
     */
    @Test
    void testCalculateAdjustedTravelTimeClear() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.CLEAR);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 60.0, travelTime, 0.001);
    }

    // ==================== TRAFFIC TESTS ====================
    /**
     * Tests that rainy weather increases the adjusted travel time.
     */
    @Test
    void testCalculateAdjustedTravelTimeRainy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.RAINY);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 48.0, travelTime, 0.001);
    }

    /**
     * Tests that snowy weather increases the adjusted travel time.
     */
    @Test
    void testCalculateAdjustedTravelTimeSnowy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.SNOWY);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 30.0, travelTime, 0.001);
    }

    /**
     * Tests that traffic delays are included in the adjusted travel time.
     */
    @Test
    void testCalculateAdjustedTravelTimeWithTrafficDelay() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.CLEAR);
        myRoad.setTrafficDelay(0.25);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 60.0 + 0.25, travelTime, 0.001);
    }

    // ==================== BLOCK ROAD TESTS ====================
    /**
     * Tests that a road can be blocked.
     */
    @Test
    void testBlockRoad() {
        mySimulator.blockRoad(myRoad);

        assertTrue(myRoad.isBlocked());
    }

    /**
     * Tests that a blocked road can be unblocked.
     */
    @Test
    void testUnBlockRoad() {
        mySimulator.blockRoad(myRoad);
        mySimulator.unBlockedRoad(myRoad);

        assertFalse(myRoad.isBlocked());
    }

    /**
     * Tests that the simulator returns roads that are currently blocked.
     */
    @Test
    void testGetBlockedRoads() {
        mySimulator.blockRoad(myRoad);

        List<Road> roads = mySimulator.getBlockedRoads();

        assertTrue(roads.contains(myRoad));
    }

    /**
     * Tests that the simulator excludes unblocked roads from the blocked-road list.
     */
    @Test
    void testGetBlockedRoadsExcludesUnblocked() {
        List<Road> roads = mySimulator.getBlockedRoads();

        assertFalse(roads.contains(myRoad));
    }

    // ==================== TRAFFIC INTENSITY TESTS ====================
    /**
     * Tests that a road has a default traffic intensity of zero.
     */
    @Test
    void testDefaultTrafficIntensity() {
        double intensity = mySimulator.getTrafficIntensity(myRoad);

        assertEquals(0.0, intensity, 0.001);
    }

    /**
     * Tests that a traffic intensity can be assigned to a road.
     */
    @Test
    void testSetTrafficIntensity() {
        mySimulator.setTrafficIntensity(myRoad, 0.5);

        double intensity = mySimulator.getTrafficIntensity(myRoad);

        assertEquals(0.5, intensity, 0.001);
    }

    /**
     * Tests that the traffic intensity of a road can be retrieved.
     */
    @Test
    void testGetTrafficIntensity() {
        mySimulator.setTrafficIntensity(myRoad, 0.5);

        double intensity = mySimulator.getTrafficIntensity(myRoad);

        assertEquals(0.5, intensity, 0.001);
    }

    /**
     * Tests that setting a negative traffic intensity throws an exception.
     */
    @Test
    void testSetTrafficIntensityNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            mySimulator.setTrafficIntensity(myRoad, -0.1);
        });
    }

    /**
     * Tests that setting a traffic intensity greater than one throws an exception.
     */
    @Test
    void testSetTrafficIntensityAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            mySimulator.setTrafficIntensity(myRoad, 1.1);
        });
    }


    // ==================== TRAFFIC LEVEL TESTS ====================
    /**
     * Tests that a low traffic intensity is classified as light traffic.
     */
    @Test
    void testGetTrafficLevelLight() {
        mySimulator.setTrafficIntensity(myRoad, 0.2);

        assertEquals(EnvironmentSimulator.TrafficLevel.LIGHT, mySimulator.getTrafficLevel(myRoad));
    }

    /**
     * TTests that a moderate traffic intensity is classified as moderate traffic.
     */
    @Test
    void testGetTrafficLevelModerate() {
        mySimulator.setTrafficIntensity(myRoad, 0.5);

        assertEquals(EnvironmentSimulator.TrafficLevel.MODERATE, mySimulator.getTrafficLevel(myRoad));
    }

    /**
     * TTests that a heavy traffic intensity is classified as heavy traffic.
     */
    @Test
    void testGetTrafficLevelHeavy() {
        mySimulator.setTrafficIntensity(myRoad, 0.9);

        assertEquals(EnvironmentSimulator.TrafficLevel.HEAVY, mySimulator.getTrafficLevel(myRoad));
    }

    /**
     * Tests that the moderate traffic threshold is classified correctly.
     */
    @Test
    void testGetTrafficLevelModerateAtThreshold() {
        mySimulator.setTrafficIntensity(myRoad, 0.3);

        assertEquals(EnvironmentSimulator.TrafficLevel.MODERATE, mySimulator.getTrafficLevel(myRoad));
    }

    /**
     * Tests that the heavy traffic threshold is classified correctly.
     */
    @Test
    void testGetTrafficLevelHeavyAtThreshold() {
        mySimulator.setTrafficIntensity(myRoad, 0.7);

        assertEquals(EnvironmentSimulator.TrafficLevel.HEAVY, mySimulator.getTrafficLevel(myRoad));
    }


    // ==================== BLOCK INTERSECTION TESTS ====================
    /**
     * Tests that an intersection can be blocked.
     */
    @Test
    void testBlockIntersection() {
        mySimulator.blockIntersection(myIntersection);

        assertTrue(myIntersection.isBlocked());
    }

    /**
     * Tests that a blocked intersection can be unblocked.
     */
    @Test
    void testUnBlockIntersection() {
        mySimulator.blockIntersection(myIntersection);
        mySimulator.unblockIntersection(myIntersection);

        assertFalse(myIntersection.isBlocked());
    }

    /**
     * Tests that the simulator returns intersections that are currently blocked.
     */
    @Test
    void testGetBlockedIntersections() {
        mySimulator.blockIntersection(myIntersection);

        List<Intersection> intersections = mySimulator.getBlockedIntersections();

        assertTrue(intersections.contains(myIntersection));
    }

    /**
     * Tests that the simulator excludes unblocked intersections from the blocked-intersection list.
     */
    @Test
    void testGetBlockedIntersectionsExcludesUnblocked() {
        List<Intersection> intersections = mySimulator.getBlockedIntersections();

        assertFalse(intersections.contains(myIntersection));
    }
}
