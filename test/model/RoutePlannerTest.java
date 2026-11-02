package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the RoutePlanner class.
 *
 * @author Abdullah Altaae
 * @version 1 November 2026
 */
class RoutePlannerTest {

    private Intersection myA;
    private Intersection myB;
    private Intersection myC;
    private Intersection myD;
    private Intersection myE;

    private Road myAB;
    private Road myBD;
    private Road myAC;
    private Road myCD;
    private Road myAD;
    private Road myDA;

    private MapGraph myGraph;
    private RoutePlanner myPlanner;

    @BeforeEach
    void setUp() {
        myA = new Intersection("A", 0, 0, false);
        myB = new Intersection("B", 1, 1, false);
        myC = new Intersection("C", 1, -1, false);
        myD = new Intersection("D", 2, 0, false);
        myE = new Intersection("E", 9, 9, false);

        // A to D through B takes 4 hours, through C takes 1 hour,
        // and the direct road takes 3 hours
        myAB = new Road(1, "A", "B", 120.0, 60.0);
        myBD = new Road(2, "B", "D", 120.0, 60.0);
        myAC = new Road(3, "A", "C", 30.0, 60.0);
        myCD = new Road(4, "C", "D", 30.0, 60.0);
        myAD = new Road(5, "A", "D", 90.0, 30.0);
        myDA = new Road(6, "D", "A", 6.0, 60.0);

        myGraph = new MapGraph(
                new ArrayList<>(Arrays.asList(myA, myB, myC, myD, myE)), new ArrayList<>());
        myPlanner = new RoutePlanner(myGraph,
                Arrays.asList(myAB, myBD, myAC, myCD, myAD, myDA));
    }

    @Test
    void testNullGraph() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanner(null, Collections.emptyList()));
    }

    @Test
    void testNullRoads() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanner(myGraph, null));
    }

    @Test
    void testNullRoadInList() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanner(myGraph, Arrays.asList(myAB, null)));
    }

    @Test
    void testNullStart() {
        assertThrows(IllegalArgumentException.class,
                () -> myPlanner.findRoute(null, myD));
    }

    @Test
    void testNullEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> myPlanner.findRoute(myA, null));
    }

    @Test
    void testFastestRoute() {
        assertEquals(List.of(myAC, myCD), myPlanner.findRoute(myA, myD));
    }

    @Test
    void testRouteIsConnected() {
        List<Road> route = myPlanner.findRoute(myA, myD);
        assertEquals("A", route.get(0).getStart());
        assertEquals("D", route.get(route.size() - 1).getEnd());
        for (int i = 1; i < route.size(); i++) {
            assertEquals(route.get(i - 1).getEnd(), route.get(i).getStart());
        }
    }

    @Test
    void testOneRoadRoute() {
        assertEquals(List.of(myAB), myPlanner.findRoute(myA, myB));
    }

    @Test
    void testTrafficChangesRoute() {
        myCD.setTrafficDelay(5.0);
        assertEquals(List.of(myAD), myPlanner.findRoute(myA, myD));
    }

    @Test
    void testOneWayRoads() {
        assertEquals(List.of(myDA), myPlanner.findRoute(myD, myA));
        assertEquals(List.of(myBD, myDA), myPlanner.findRoute(myB, myA));
        myDA.setBlocked(true);
        assertTrue(myPlanner.findRoute(myD, myA).isEmpty());
    }

    @Test
    void testBlockedRoad() {
        myAC.setBlocked(true);
        assertEquals(List.of(myAD), myPlanner.findRoute(myA, myD));
    }

    @Test
    void testBlockedIntersection() {
        myC.setBlocked(true);
        assertEquals(List.of(myAD), myPlanner.findRoute(myA, myD));
    }

    @Test
    void testSlowRouteWhenOthersBlocked() {
        myAC.setBlocked(true);
        myAD.setBlocked(true);
        assertEquals(List.of(myAB, myBD), myPlanner.findRoute(myA, myD));
    }

    @Test
    void testUnblockRoad() {
        myAC.setBlocked(true);
        myPlanner.findRoute(myA, myD);
        myAC.setBlocked(false);
        assertEquals(List.of(myAC, myCD), myPlanner.findRoute(myA, myD));
    }

    @Test
    void testBlockedStart() {
        myA.setBlocked(true);
        assertTrue(myPlanner.findRoute(myA, myD).isEmpty());
    }

    @Test
    void testBlockedEnd() {
        myD.setBlocked(true);
        assertTrue(myPlanner.findRoute(myA, myD).isEmpty());
    }

    @Test
    void testAllRoutesBlocked() {
        myAC.setBlocked(true);
        myAD.setBlocked(true);
        myAB.setBlocked(true);
        assertTrue(myPlanner.findRoute(myA, myD).isEmpty());
    }

    @Test
    void testNoRoute() {
        assertTrue(myPlanner.findRoute(myA, myE).isEmpty());
    }

    @Test
    void testSameStartAndEnd() {
        assertTrue(myPlanner.findRoute(myA, myA).isEmpty());
    }

    @Test
    void testIntersectionNotOnMap() {
        Intersection other = new Intersection("Z", 5, 5, false);
        assertTrue(myPlanner.findRoute(myA, other).isEmpty());
        assertTrue(myPlanner.findRoute(other, myA).isEmpty());
    }

    @Test
    void testEmptyMap() {
        RoutePlanner planner = new RoutePlanner(
                new MapGraph(new ArrayList<>(), new ArrayList<>()), Collections.emptyList());
        assertTrue(planner.findRoute(myA, myD).isEmpty());
    }

    @Test
    void testIntersectionAddedLater() {
        Intersection f = new Intersection("F", 3, 3, false);
        Road df = new Road(7, "D", "F", 60.0, 60.0);
        RoutePlanner planner = new RoutePlanner(myGraph, Arrays.asList(myAC, myCD, df));
        assertTrue(planner.findRoute(myA, f).isEmpty());
        myGraph.addIntersection(f);
        assertEquals(List.of(myAC, myCD, df), planner.findRoute(myA, f));
    }
}