
package model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OsmMapLoader}. Each test builds a tiny OSM file in memory,
 * so the tests do not depend on any file on disk.
 *
 * <p>Most tests use a "plus" shape: A Ave runs north to south and B St runs
 * west to east, crossing at node 1 in the middle of the bounds box.</p>
 *
 * @author YOUR NAME
 * @version 22 November 2026
 */
public class OsmMapLoaderTest {

    /** The bounds used by every test map. */
    private static final String BOUNDS =
            "<bounds minlat=\"47.0\" minlon=\"-122.0\" maxlat=\"47.01\" maxlon=\"-121.99\"/>\n";

    /** The five nodes of the plus shape: centre, north, south, west and east. */
    private static final String PLUS_NODES = node(1, 47.005, -121.995)
            + node(2, 47.009, -121.995) + node(3, 47.001, -121.995)
            + node(4, 47.005, -121.999) + node(5, 47.005, -121.991);

    /** Allowed difference when comparing distances, in miles. */
    private static final double TOLERANCE = 1e-6;

    @Test
    void crossingRoadsMakeFiveIntersectionsAndEightRoads() throws IOException {
        final MapGraph map = load(PLUS_NODES
                + way("highway=residential|name=A Ave", 3, 1, 2)
                + way("highway=residential|name=B St", 4, 1, 5));
        // The crossing plus four dead ends; four stretches, each two-way.
        assertEquals(5, map.getIntersections().size());
        assertEquals(8, map.getRoads().size());
    }

    @Test
    void intersectionsAreNamedAfterTheirStreets() throws IOException {
        final MapGraph map = load(PLUS_NODES
                + way("highway=residential|name=B St", 4, 1, 5)
                + way("highway=residential|name=A Ave", 3, 1, 2));
        final List<String> names = names(map);
        assertTrue(names.contains("A Ave & B St"), names.toString());
        // Each dead end is named after its one street; repeats get a number.
        assertTrue(names.contains("A Ave"), names.toString());
        assertTrue(names.contains("A Ave (2)"), names.toString());
        assertTrue(names.contains("B St"), names.toString());
        assertTrue(names.contains("B St (2)"), names.toString());
    }

    @Test
    void intersectionsAreAddedInAlphabeticalOrder() throws IOException {
        final MapGraph map = load(PLUS_NODES
                + way("highway=residential|name=Zebra Rd", 3, 1, 2)
                + way("highway=residential|name=apple Ln", 4, 1, 5));
        final List<String> names = names(map);
        final List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(sorted, names);
    }

    @Test
    void unnamedRoadsUseTheRefTagOrTheNodeId() throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.995) + node(2, 47.008, -121.995)
                + node(3, 47.002, -121.993) + node(4, 47.008, -121.993)
                + way("highway=motorway|ref=I 5|oneway=no", 1, 2)
                + way("highway=residential", 3, 4));
        final List<String> names = names(map);
        assertTrue(names.contains("I 5"), names.toString());

        final MapGraph unnamed = load(node(3, 47.002, -121.993) + node(4, 47.008, -121.993)
                + way("highway=residential", 3, 4));
        assertTrue(names(unnamed).contains("Node 3"), names(unnamed).toString());
    }

    @Test
    void bendsInARoadAreNotIntersections() throws IOException {

        final MapGraph map = load(node(1, 47.002, -121.998) + node(6, 47.005, -121.995)
                + node(2, 47.008, -121.998)
                + way("highway=residential|name=Curve Rd", 1, 6, 2));
        assertEquals(2, map.getIntersections().size());
        assertEquals(2, map.getRoads().size());
    }

    @Test
    void aStreetSplitIntoTwoWaysIsJoined() throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.995) + node(6, 47.005, -121.995)
                + node(2, 47.008, -121.995)
                + way("highway=residential|name=Long St", 1, 6)
                + way("highway=residential|name=Long St", 2, 6));
        assertEquals(2, map.getIntersections().size());
        assertEquals(2, map.getRoads().size());
        assertEquals(3, map.getRoads().get(0).getShape().size());
    }

    @Test
    void waysWithDifferentDetailsAreNotJoined() throws IOException {
        final String nodes = node(1, 47.002, -121.995) + node(6, 47.005, -121.995)
                + node(2, 47.008, -121.995);

        assertEquals(3, load(nodes + way("highway=residential|name=Long St", 1, 6)
                + way("highway=residential|name=Short St", 6, 2)).getIntersections().size());
        // One-way roads
        assertEquals(3, load(nodes
                + way("highway=residential|name=Long St|oneway=yes", 1, 6)
                + way("highway=residential|name=Long St|oneway=yes", 2, 6))
                .getIntersections().size());

        assertEquals(2, load(nodes
                + way("highway=residential|name=Long St|oneway=yes", 1, 6)
                + way("highway=residential|name=Long St|oneway=yes", 6, 2))
                .getIntersections().size());
    }

    @Test
    void roadsKeepTheirShapeInBothDirections() throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.998) + node(6, 47.005, -121.995)
                + node(2, 47.008, -121.998)
                + way("highway=residential|name=Curve Rd", 1, 6, 2));
        final Road there = map.getRoads().get(0);
        final Road back = map.getRoads().get(1);
        final List<int[]> shape = there.getShape();
        assertEquals(3, shape.size());

        final Intersection start = find(map, there.getStart());
        final Intersection end = find(map, there.getEnd());
        assertArrayEquals(new int[] {start.getX(), start.getY()}, shape.get(0));
        assertArrayEquals(new int[] {end.getX(), end.getY()}, shape.get(2));

        final List<int[]> backShape = back.getShape();
        for (int i = 0; i < shape.size(); i++) {
            assertArrayEquals(shape.get(i), backShape.get(shape.size() - 1 - i));
        }
    }

    @Test
    void distanceFollowsTheCurve() throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.998) + node(6, 47.005, -121.995)
                + node(2, 47.008, -121.998)
                + way("highway=residential|name=Curve Rd", 1, 6, 2));
        final double expected = GeoProjection.distanceMiles(47.002, -121.998, 47.005, -121.995)
                + GeoProjection.distanceMiles(47.005, -121.995, 47.008, -121.998);
        assertEquals(expected, map.getRoads().get(0).getDistance(), TOLERANCE);
    }

    @Test
    void positionsStartFromTheNorthWestCorner() throws IOException {
        final MapGraph map = load(node(1, 47.01, -122.0) + node(2, 47.0, -121.99)
                + way("highway=residential|name=Diagonal St", 1, 2));
        final Intersection northWest = find(map, "Diagonal St");
        final Intersection southEast = find(map, "Diagonal St (2)");
        assertEquals(0, northWest.getX());
        assertEquals(0, northWest.getY());
        assertTrue(southEast.getX() > 0 && southEast.getY() > 0);
    }


    @Test
    void roadsCarsCannotUseAreIgnored() throws IOException {
        final MapGraph map = load(PLUS_NODES
                + way("highway=residential|name=A Ave", 3, 1, 2)
                + way("highway=footway|name=Walk", 4, 1)
                + way("highway=residential|name=Private Ln|access=private", 1, 5));
        // Only A Ave is left, and node 1 is now just a bend in it.
        assertEquals(2, map.getIntersections().size());
        for (final Road road : map.getRoads()) {
            assertFalse(road.getStart().contains("Walk") || road.getStart().contains("Private"));
        }
    }

    @Test
    void roadsLeavingTheBoundsAreCut() throws IOException {
        // Node 9 is outside the bounds box, so the road stops at node 2.
        final MapGraph map = load(node(1, 47.002, -121.995) + node(2, 47.008, -121.995)
                + node(9, 47.5, -121.995)
                + way("highway=residential|name=Long Rd", 1, 2, 9));
        assertEquals(2, map.getIntersections().size());
        assertEquals(2, map.getRoads().size());
    }

    @Test
    void onlyTheLargestConnectedPartIsKept() throws IOException {
        // The plus shape, plus a separate little road in the corner.
        final MapGraph map = load(PLUS_NODES
                + node(7, 47.0005, -121.9995) + node(8, 47.0009, -121.9991)
                + way("highway=residential|name=A Ave", 3, 1, 2)
                + way("highway=residential|name=B St", 4, 1, 5)
                + way("highway=residential|name=Lonely Ln", 7, 8));
        assertEquals(5, map.getIntersections().size());
        assertFalse(names(map).contains("Lonely Ln"));
    }


    @Test
    void oneWayRoadsOnlyGoForward() throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.995) + node(2, 47.008, -121.995)
                + way("highway=residential|name=One Way St|oneway=yes", 1, 2));
        assertEquals(1, map.getRoads().size());
        final Road road = map.getRoads().get(0);
        // Node 1 is further south, so it has the larger y.
        assertTrue(find(map, road.getStart()).getY() > find(map, road.getEnd()).getY());
    }

    @Test
    void reversedOneWayRoadsGoBackward() throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.995) + node(2, 47.008, -121.995)
                + way("highway=residential|name=Back St|oneway=-1", 1, 2));
        assertEquals(1, map.getRoads().size());
        final Road road = map.getRoads().get(0);
        assertTrue(find(map, road.getStart()).getY() < find(map, road.getEnd()).getY());
    }

    @Test
    void motorwaysAreOneWayUnlessTaggedOtherwise() throws IOException {
        final String nodes = node(1, 47.002, -121.995) + node(2, 47.008, -121.995);
        assertEquals(1, load(nodes + way("highway=motorway|ref=I 5", 1, 2))
                .getRoads().size());
        assertEquals(2, load(nodes + way("highway=motorway|ref=I 5|oneway=no", 1, 2))
                .getRoads().size());
    }


    @Test
    void speedLimitsComeFromTheMaxspeedTag() throws IOException {
        assertEquals(40.0, speedOf("highway=residential|maxspeed=40 mph"), TOLERANCE);
        // A plain number is km/h: 50 km/h is about 31 mph.
        assertEquals(31.0, speedOf("highway=residential|maxspeed=50"), TOLERANCE);
        assertEquals(31.0, speedOf("highway=residential|maxspeed=50 km/h"), TOLERANCE);
    }

    @Test
    void speedLimitsFallBackToTheRoadType() throws IOException {
        assertEquals(25.0, speedOf("highway=residential"), TOLERANCE);
        assertEquals(25.0, speedOf("highway=residential|maxspeed=signals"), TOLERANCE);
        assertEquals(30.0, speedOf("highway=primary|maxspeed=none"), TOLERANCE);
    }


    @Test
    void routePlannerFindsARouteOnALoadedMap() throws IOException {
        final MapGraph map = load(PLUS_NODES
                + way("highway=residential|name=A Ave", 3, 1, 2)
                + way("highway=residential|name=B St", 4, 1, 5));
        final RoutePlanner planner = new RoutePlanner(map);
        final List<Road> route = planner.findRoute(find(map, "A Ave"), find(map, "B St"));
        assertEquals(2, route.size());
        assertEquals("A Ave & B St", route.get(0).getEnd());
    }

    @Test
    void fileWithoutBoundsIsRejected() {
        final String xml = "<osm>" + PLUS_NODES
                + way("highway=residential|name=A Ave", 3, 1, 2) + "</osm>";
        final IOException ex = assertThrows(IOException.class, () -> loadXml(xml));
        assertTrue(ex.getMessage().contains("bounds"));
    }

    @Test
    void fileWithoutDrivableRoadsIsRejected() {
        final IOException ex = assertThrows(IOException.class,
                () -> load(PLUS_NODES + way("highway=footway", 3, 1, 2)));
        assertTrue(ex.getMessage().contains("no drivable roads"));
    }

    @Test
    void brokenXmlIsRejected() {
        assertThrows(IOException.class, () -> loadXml("<osm><bounds minlat=\"47"));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> OsmMapLoader.load((java.nio.file.Path) null));
        assertThrows(NullPointerException.class,
                () -> OsmMapLoader.load(null, OsmMapLoader.DEFAULT_ZOOM));
    }

    /**
     * Loads a map from OSM XML elements placed after the standard bounds.
     *
     * @param theBody the node and way elements
     * @return the loaded map
     * @throws IOException if the loader rejects the map
     */
    private static MapGraph load(final String theBody) throws IOException {
        return loadXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<osm version=\"0.6\">\n"
                + BOUNDS + theBody + "</osm>\n");
    }

    /**
     * Loads a map from a complete OSM XML document.
     *
     * @param theXml the document
     * @return the loaded map
     * @throws IOException if the loader rejects the map
     */
    private static MapGraph loadXml(final String theXml) throws IOException {
        return OsmMapLoader.load(new ByteArrayInputStream(
                theXml.getBytes(StandardCharsets.UTF_8)), OsmMapLoader.DEFAULT_ZOOM);
    }

    /**
     * Returns the speed limit of a single road with the given tags.
     *
     * @param theTags the way's tags, as key=value pairs separated by |
     * @return the speed limit of the loaded road, in mph
     * @throws IOException if the loader rejects the map
     */
    private static double speedOf(final String theTags) throws IOException {
        final MapGraph map = load(node(1, 47.002, -121.995) + node(2, 47.008, -121.995)
                + way(theTags + "|name=Test St|oneway=no", 1, 2));
        return map.getRoads().get(0).getSpeedLimit();
    }

    /**
     * Returns an OSM node element.
     *
     * @param theId the node ID
     * @param theLat the latitude
     * @param theLon the longitude
     * @return the XML for the node
     */
    private static String node(final long theId, final double theLat, final double theLon) {
        return "<node id=\"" + theId + "\" lat=\"" + theLat + "\" lon=\"" + theLon + "\"/>\n";
    }

    /**
     * Returns an OSM way element. Way IDs are not used by the loader, so every
     * way gets the same one.
     *
     * @param theTags the tags, as key=value pairs separated by |
     * @param theNodes the node IDs, in order
     * @return the XML for the way
     */
    private static String way(final String theTags, final long... theNodes) {
        final StringBuilder sb = new StringBuilder("<way id=\"100\">\n");
        for (final long id : theNodes) {
            sb.append("  <nd ref=\"").append(id).append("\"/>\n");
        }
        for (final String pair : theTags.split("\\|")) {
            final String[] parts = pair.split("=", 2);
            sb.append("  <tag k=\"").append(parts[0]).append("\" v=\"").append(parts[1])
                    .append("\"/>\n");
        }
        return sb.append("</way>\n").toString();
    }

    /**
     * Returns the names of the map's intersections, in the map's order.
     *
     * @param theMap the map
     * @return the names
     */
    private static List<String> names(final MapGraph theMap) {
        final List<String> names = new ArrayList<>();
        for (final Intersection intersection : theMap.getIntersections()) {
            names.add(intersection.getName());
        }
        return names;
    }

    /**
     * Returns the intersection with the given name, failing the test if there
     * is none.
     *
     * @param theMap the map
     * @param theName the intersection name
     * @return the intersection
     */
    private static Intersection find(final MapGraph theMap, final String theName) {
        for (final Intersection intersection : theMap.getIntersections()) {
            if (intersection.getName().equals(theName)) {
                return intersection;
            }
        }
        return fail("no intersection named " + theName);
    }
}