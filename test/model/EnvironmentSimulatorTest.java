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

    @Test
    void testInitialWeather() {
        assertEquals(EnvironmentSimulator.WeatherCondition.CLEAR,
                mySimulator.getWeather());
    }

    @Test
    void testSetWeather() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.RAINY);

        assertEquals(EnvironmentSimulator.WeatherCondition.RAINY,
                mySimulator.getWeather());
    }

    @Test
    void testCalculateAdjustedSpeedRainy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.RAINY);

        double adjustedSpeed = mySimulator.calculateAdjustedSpeed(myRoad);

        assertEquals(48.0, adjustedSpeed, 0.001);
    }

    @Test
    void testCalculateAdjustedSpeedSnowy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.SNOWY);

        double adjustedSpeed = mySimulator.calculateAdjustedSpeed(myRoad);

        assertEquals(30.0, adjustedSpeed, 0.001);
    }

    @Test
    void testCalculateAdjustedSpeedClear() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.CLEAR);

        double adjustedSpeed = mySimulator.calculateAdjustedSpeed(myRoad);

        assertEquals(60.0, adjustedSpeed, 0.001);
    }
    @Test
    void testCalculateAdjustedTravelTimeClear() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.CLEAR);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 60.0, travelTime, 0.001);
    }

    @Test
    void testCalculateAdjustedTravelTimeRainy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.RAINY);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 48.0, travelTime, 0.001);
    }

    @Test
    void testCalculateAdjustedTravelTimeSnowy() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.SNOWY);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 30.0, travelTime, 0.001);
    }

    @Test
    void testCalculateAdjustedTravelTimeWithTrafficDelay() {
        mySimulator.setWeather(EnvironmentSimulator.WeatherCondition.CLEAR);
        myRoad.setTrafficDelay(0.25);

        double travelTime = mySimulator.calculateAdjustedTravelTime(myRoad);

        assertEquals(10.0 / 60.0 + 0.25, travelTime, 0.001);
    }

    @Test
    void testBlockRoad() {
        mySimulator.blockRoad(myRoad);

        assertTrue(myRoad.isBlocked());
    }

    @Test
    void testUnBlockRoad() {
        mySimulator.blockRoad(myRoad);
        mySimulator.unBlockedRoad(myRoad);

        assertFalse(myRoad.isBlocked());
    }
    @Test
    void testDefaultTrafficIntensity() {
        double intensity = mySimulator.getTrafficIntensity(myRoad);

        assertEquals(0.0, intensity, 0.001);
    }

    @Test
    void testSetTrafficIntensity() {
        mySimulator.setTrafficIntensity(myRoad, 0.5);

        double intensity = mySimulator.getTrafficIntensity(myRoad);

        assertEquals(0.5, intensity, 0.001);
    }

    @Test
    void testGetTrafficIntensity() {
        mySimulator.setTrafficIntensity(myRoad, 0.5);

        double intensity = mySimulator.getTrafficIntensity(myRoad);

        assertEquals(0.5, intensity, 0.001);
    }

    @Test
    void testGetTrafficLevel() {
        mySimulator.setTrafficIntensity(myRoad, 0.2);

        assertEquals(EnvironmentSimulator.TrafficLevel.LIGHT, mySimulator.getTrafficLevel(myRoad));
    }

    @Test
    void testGetTrafficLevelModerate() {
        mySimulator.setTrafficIntensity(myRoad, 0.5);

        assertEquals(EnvironmentSimulator.TrafficLevel.MODERATE, mySimulator.getTrafficLevel(myRoad));
    }

    @Test
    void testGetTrafficLevelHeavy() {
        mySimulator.setTrafficIntensity(myRoad, 0.9);

        assertEquals(EnvironmentSimulator.TrafficLevel.HEAVY, mySimulator.getTrafficLevel(myRoad));
    }

    @Test
    void testGetTrafficLevelModerateBelowThreshold() {
        mySimulator.setTrafficIntensity(myRoad, 0.3);

        assertEquals(EnvironmentSimulator.TrafficLevel.MODERATE, mySimulator.getTrafficLevel(myRoad));
    }

    @Test
    void testGetTrafficLevelHeavyAboveThreshold() {
        mySimulator.setTrafficIntensity(myRoad, 0.7);

        assertEquals(EnvironmentSimulator.TrafficLevel.HEAVY, mySimulator.getTrafficLevel(myRoad));
    }
    @Test
    void testSetTrafficIntensityNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            mySimulator.setTrafficIntensity(myRoad, -0.1);
        });
    }

    @Test
    void testSetTrafficIntensityAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            mySimulator.setTrafficIntensity(myRoad, 1.1);
        });
    }

    @Test
    void testBlockIntersection() {
        mySimulator.blockIntersection(myIntersection);

        assertTrue(myIntersection.isBlocked());
    }

    @Test
    void testUnBlockIntersection() {
        mySimulator.blockIntersection(myIntersection);
        mySimulator.unblockIntersection(myIntersection);

        assertFalse(myIntersection.isBlocked());
    }
    @Test
    void testGetBlockedIntersections() {
        mySimulator.blockIntersection(myIntersection);

        List<Intersection> intersections = mySimulator.getBlockedIntersections();

        assertTrue(intersections.contains(myIntersection));
    }

    @Test
    void testGetBlockedIntersectionsExcludesUnblocked() {
        List<Intersection> intersections = mySimulator.getBlockedIntersections();

        assertFalse(intersections.contains(myIntersection));
    }

    @Test
    void testGetBlockedRoads() {
        mySimulator.blockRoad(myRoad);

        List<Road> roads = mySimulator.getBlockedRoads();

        assertTrue(roads.contains(myRoad));
    }

    @Test
    void testGetBlockedRoadsExcludesUnblocked() {
        List<Road> roads = mySimulator.getBlockedRoads();

        assertFalse(roads.contains(myRoad));
    }
}
