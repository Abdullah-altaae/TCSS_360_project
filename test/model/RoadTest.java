package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Road class.
 *
 * @author Abdullah Altaae
 * @version 1 November 2026
 */
class RoadTest {

    private static final double DELTA = 0.0001;

    private Road myRoad;

    @BeforeEach
    void setUp() {
        myRoad = new Road(7, "A", "B", 30.0, 60.0);
    }

    @Test
    void testConstructor() {
        assertEquals(7, myRoad.getId());
        assertEquals("A", myRoad.getStart());
        assertEquals("B", myRoad.getEnd());
        assertEquals(30.0, myRoad.getDistance(), DELTA);
        assertEquals(60.0, myRoad.getSpeedLimit(), DELTA);
        assertEquals(0.0, myRoad.getTrafficDelay(), DELTA);
        assertFalse(myRoad.isBlocked());
    }

    @Test
    void testConstructorWithDelay() {
        Road road = new Road(1, "A", "B", 10.0, 20.0, 0.25);
        assertEquals(0.25, road.getTrafficDelay(), DELTA);
        assertFalse(road.isBlocked());
    }

    @Test
    void testIdZero() {
        Road road = new Road(0, "A", "B", 1.0, 1.0);
        assertEquals(0, road.getId());
    }

    @Test
    void testNegativeId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(-1, "A", "B", 1.0, 1.0));
    }

    @Test
    void testNullStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, null, "B", 1.0, 1.0));
    }

    @Test
    void testNullEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", null, 1.0, 1.0));
    }

    @Test
    void testZeroDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", 0.0, 1.0));
    }

    @Test
    void testNegativeDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", -5.0, 1.0));
    }

    @Test
    void testNaNDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", Double.NaN, 1.0));
    }

    @Test
    void testZeroSpeedLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", 1.0, 0.0));
    }

    @Test
    void testNegativeSpeedLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", 1.0, -5.0));
    }

    @Test
    void testInfiniteSpeedLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", 1.0, Double.POSITIVE_INFINITY));
    }

    @Test
    void testNegativeDelayInConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, "A", "B", 1.0, 1.0, -0.1));
    }

    @Test
    void testSetTrafficDelay() {
        myRoad.setTrafficDelay(0.5);
        assertEquals(0.5, myRoad.getTrafficDelay(), DELTA);
    }

    @Test
    void testSetTrafficDelayNegative() {
        myRoad.setTrafficDelay(0.5);
        assertThrows(IllegalArgumentException.class,
                () -> myRoad.setTrafficDelay(-1.0));
        assertEquals(0.5, myRoad.getTrafficDelay(), DELTA);
    }

    @Test
    void testSetTrafficDelayNaN() {
        assertThrows(IllegalArgumentException.class,
                () -> myRoad.setTrafficDelay(Double.NaN));
    }

    @Test
    void testSetBlocked() {
        myRoad.setBlocked(true);
        assertTrue(myRoad.isBlocked());
        myRoad.setBlocked(false);
        assertFalse(myRoad.isBlocked());
    }

    @Test
    void testTravelTime() {
        assertEquals(0.5, myRoad.getTravelTime(), DELTA);
    }

    @Test
    void testTravelTimeWithDelay() {
        myRoad.setTrafficDelay(0.25);
        assertEquals(0.75, myRoad.getTravelTime(), DELTA);
    }

    @Test
    void testTravelTimeAtSpeed() {
        assertEquals(1.0, myRoad.getTravelTime(30.0), DELTA);
    }

    @Test
    void testTravelTimeAtSpeedWithDelay() {
        myRoad.setTrafficDelay(0.5);
        assertEquals(1.5, myRoad.getTravelTime(30.0), DELTA);
    }

    @Test
    void testTravelTimeZeroSpeed() {
        assertThrows(IllegalArgumentException.class,
                () -> myRoad.getTravelTime(0.0));
    }

    @Test
    void testTravelTimeNegativeSpeed() {
        assertThrows(IllegalArgumentException.class,
                () -> myRoad.getTravelTime(-10.0));
    }

    @Test
    void testToString() {
        assertTrue(myRoad.toString().contains("A -> B"));
        assertFalse(myRoad.toString().contains("BLOCKED"));
    }

    @Test
    void testToStringBlocked() {
        myRoad.setBlocked(true);
        assertTrue(myRoad.toString().contains("BLOCKED"));
    }
}