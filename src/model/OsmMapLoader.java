package model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Loads a map from an OpenStreetMap (.osm) file.
 *
 * <p>Only roads cars can use are kept. Places where roads meet become
 * intersections, and the road between two intersections becomes a Road
 * (one each way unless it is one-way).</p>
 *
 * @author Abdullah Altaae
 * @version 22 November 2026
 */
public final class OsmMapLoader {

    /** The zoom level used for map coordinates. */
    public static final int DEFAULT_ZOOM = 17;

    /** Default speed limits in mph for each road type. */
    private static final Map<String, Double> DEFAULT_SPEEDS = Map.ofEntries(
            Map.entry("motorway", 60.0),
            Map.entry("motorway_link", 35.0),
            Map.entry("trunk", 45.0),
            Map.entry("trunk_link", 30.0),
            Map.entry("primary", 30.0),
            Map.entry("primary_link", 25.0),
            Map.entry("secondary", 30.0),
            Map.entry("secondary_link", 25.0),
            Map.entry("tertiary", 25.0),
            Map.entry("tertiary_link", 25.0),
            Map.entry("unclassified", 25.0),
            Map.entry("residential", 25.0),
            Map.entry("living_street", 15.0));

    /** Access values that mean cars are not allowed. */
    private static final Set<String> NO_ACCESS = Set.of("no", "private");

    /** Tags that can block cars from a road. */
    private static final List<String> ACCESS_TAGS =
            List.of("access", "vehicle", "motor_vehicle", "motorcar");

    /** Most street names used in one intersection name. */
    private static final int MAX_NAMES_IN_INTERSECTION = 3;

    /** Miles in one kilometre. */
    private static final double MILES_PER_KM = 0.621371;

    /** One-way in the order of the nodes. */
    private static final int FORWARD = 1;

    /** Two-way. */
    private static final int BOTH_WAYS = 0;

    /** One-way against the order of the nodes. */
    private static final int BACKWARD = -1;

    /** Prevents instantiation. */
    private OsmMapLoader() {
    }

    /**
     * Loads a map from an .osm file.
     *
     * @param theFile the file to load
     * @return the map
     * @throws IOException if the file can't be read or has no usable roads
     */
    public static MapGraph load(final Path theFile) throws IOException {
        Objects.requireNonNull(theFile, "file must not be null");
        try (InputStream in = Files.newInputStream(theFile)) {
            return load(in, DEFAULT_ZOOM);
        }
    }

    /**
     * Loads a map from OSM XML.
     *
     * @param theInput the XML to read
     * @param theZoom the zoom level, from 0 to 22
     * @return the map
     * @throws IOException if the XML is invalid or has no usable roads
     */
    public static MapGraph load(final InputStream theInput, final int theZoom)
            throws IOException {
        Objects.requireNonNull(theInput, "input must not be null");
        final OsmData data = parse(theInput);
        if (data.myBounds == null) {
            throw new IOException("the OSM file has no <bounds> element");
        }
        final MapGraph graph = build(data, theZoom);
        if (graph.getRoads().isEmpty()) {
            throw new IOException("the OSM file has no drivable roads inside its bounds");
        }
        return graph;
    }


    /**
     * Reads the bounds, nodes and drivable ways from the XML.
     *
     * @param theInput the XML to read
     * @return the data that was read
     * @throws IOException if the XML is invalid
     */
    private static OsmData parse(final InputStream theInput) throws IOException {
        final OsmData data = new OsmData();
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            final XMLStreamReader reader = factory.createXMLStreamReader(theInput);
            OsmWay way = null;
            while (reader.hasNext()) {
                final int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "bounds" -> data.myBounds = new double[] {
                                number(reader, "minlat"), number(reader, "minlon"),
                                number(reader, "maxlat"), number(reader, "maxlon")};
                        case "node" -> data.myNodes.put(
                                Long.parseLong(reader.getAttributeValue(null, "id")),
                                new double[] {number(reader, "lat"), number(reader, "lon")});
                        case "way" -> way = new OsmWay();
                        case "nd" -> {
                            if (way != null) {
                                way.myNodeIds.add(
                                        Long.parseLong(reader.getAttributeValue(null, "ref")));
                            }
                        }
                        case "tag" -> {
                            if (way != null) {
                                way.myTags.put(reader.getAttributeValue(null, "k"),
                                        reader.getAttributeValue(null, "v"));
                            }
                        }
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT
                        && "way".equals(reader.getLocalName())) {
                    if (way != null && isDrivable(way)) {
                        data.myWays.add(way);
                    }
                    way = null;
                }
            }
            reader.close();
        } catch (final XMLStreamException | NumberFormatException ex) {
            throw new IOException("could not read the OSM file: " + ex.getMessage(), ex);
        }
        if (data.myBounds != null && (data.myBounds[0] > data.myBounds[2]
                || data.myBounds[1] > data.myBounds[3])) {
            throw new IOException("the OSM file's <bounds> are the wrong way round");
        }
        return data;
    }

    /**
     * Reads a number attribute.
     *
     * @param theReader the XML reader
     * @param theName the attribute name
     * @return the value
     */
    private static double number(final XMLStreamReader theReader, final String theName) {
        final String value = theReader.getAttributeValue(null, theName);
        if (value == null) {
            throw new NumberFormatException("missing attribute " + theName);
        }
        return Double.parseDouble(value);
    }

    /**
     * Returns whether cars can drive on a way.
     *
     * @param theWay the way to check
     * @return true if cars can use it
     */
    private static boolean isDrivable(final OsmWay theWay) {
        final String highway = theWay.myTags.get("highway");
        if (highway == null || !DEFAULT_SPEEDS.containsKey(highway)
                || theWay.myNodeIds.size() < 2
                || "yes".equals(theWay.myTags.get("area"))) {
            return false;
        }
        for (final String tag : ACCESS_TAGS) {
            // Set.of(...).contains(null) throws, so check for a missing tag first.
            final String value = theWay.myTags.get(tag);
            if (value != null && NO_ACCESS.contains(value)) {
                return false;
            }
        }
        final String oneway = theWay.myTags.getOrDefault("oneway", "no");
        return !"reversible".equals(oneway) && !"alternating".equals(oneway);
    }


    /**
     * Builds the map's intersections and roads.
     *
     * @param theData the data read from the file
     * @param theZoom the zoom level
     * @return the map
     */
    private static MapGraph build(final OsmData theData, final int theZoom) {
        final double[] bounds = theData.myBounds;
        final GeoProjection projection = new GeoProjection(bounds[2], bounds[1], theZoom);

        final List<Segment> segments = joinSplitStreets(toSegments(splitAtBounds(theData)));
        final List<Stretch> stretches = keepLargestPart(splitAtIntersections(theData, segments));
        final Map<Long, String> names = nameIntersections(stretches);

        final List<Long> nodeIds = new ArrayList<>(names.keySet());
        nodeIds.sort((theA, theB) ->
                String.CASE_INSENSITIVE_ORDER.compare(names.get(theA), names.get(theB)));

        final MapGraph graph = new MapGraph();
        for (final long id : nodeIds) {
            final int[] point = project(projection, theData.myNodes.get(id));
            graph.addIntersection(new Intersection(names.get(id), point[0], point[1], false));
        }

        int nextId = 0;
        for (final Stretch stretch : stretches) {
            final List<int[]> shape = new ArrayList<>();
            for (final long id : stretch.myNodeIds) {
                shape.add(project(projection, theData.myNodes.get(id)));
            }
            final String from = names.get(stretch.from());
            final String to = names.get(stretch.to());

            final Road road = new Road(nextId++, from, to, stretch.myMiles, stretch.mySpeed);
            road.setShape(shape);
            graph.addRoad(road);
            if (stretch.myTwoWay) {
                final List<int[]> reversed = new ArrayList<>(shape);
                Collections.reverse(reversed);
                final Road back = new Road(nextId++, to, from, stretch.myMiles, stretch.mySpeed);
                back.setShape(reversed);
                graph.addRoad(back);
            }
        }
        return graph;
    }

    /**
     * Cuts each way into pieces that stay inside the map's bounds.
     *
     * @param theData the data read from the file
     * @return the pieces
     */
    private static List<WayPiece> splitAtBounds(final OsmData theData) {
        final List<WayPiece> pieces = new ArrayList<>();
        for (final OsmWay way : theData.myWays) {
            List<Long> current = new ArrayList<>();
            for (final long id : way.myNodeIds) {
                final double[] position = theData.myNodes.get(id);
                if (position != null && inside(position, theData.myBounds)) {
                    current.add(id);
                } else {
                    addPiece(pieces, way, current);
                    current = new ArrayList<>();
                }
            }
            addPiece(pieces, way, current);
        }
        return pieces;
    }

    /**
     * Turns way pieces into segments, with nodes in the direction of travel.
     *
     * @param thePieces the way pieces
     * @return the segments
     */
    private static List<Segment> toSegments(final List<WayPiece> thePieces) {
        final List<Segment> segments = new ArrayList<>();
        for (final WayPiece piece : thePieces) {
            final int direction = direction(piece.myWay);
            final List<Long> nodes = new ArrayList<>(piece.myNodeIds);
            if (direction == BACKWARD) {
                Collections.reverse(nodes);
            }
            segments.add(new Segment(nodes, direction == BOTH_WAYS,
                    speedLimit(piece.myWay), streetName(piece.myWay)));
        }
        return segments;
    }

    /**
     * Joins segments that are really one street. OSM often splits a street
     * into several ways, which would otherwise make fake intersections.
     *
     * @param theSegments the segments
     * @return the joined segments
     */
    private static List<Segment> joinSplitStreets(final List<Segment> theSegments) {
        final Set<Long> middles = new HashSet<>();
        final Map<Long, List<Segment>> byEnd = new LinkedHashMap<>();
        for (final Segment segment : theSegments) {
            middles.addAll(segment.myNodeIds.subList(1, segment.myNodeIds.size() - 1));
            byEnd.computeIfAbsent(segment.first(), key -> new ArrayList<>()).add(segment);
            byEnd.computeIfAbsent(segment.last(), key -> new ArrayList<>()).add(segment);
        }

        final Set<Segment> result = new LinkedHashSet<>(theSegments);
        for (final long node : new ArrayList<>(byEnd.keySet())) {
            final List<Segment> here = byEnd.get(node);
            if (middles.contains(node) || here.size() != 2 || here.get(0) == here.get(1)) {
                continue;
            }
            final Segment joined = join(here.get(0), here.get(1), node);
            if (joined == null) {
                continue;
            }
            for (final Segment old : here) {
                final long farEnd = old.first() == node ? old.last() : old.first();
                final List<Segment> atFarEnd = byEnd.get(farEnd);
                atFarEnd.set(atFarEnd.indexOf(old), joined);
                result.remove(old);
            }
            byEnd.remove(node);
            result.add(joined);
        }
        return new ArrayList<>(result);
    }

    /**
     * Joins two segments that meet at a node, if they are the same street.
     *
     * @param theA one segment
     * @param theB the other segment
     * @param theNode the node where they meet
     * @return the joined segment, or null if they can't be joined
     */
    private static Segment join(final Segment theA, final Segment theB, final long theNode) {
        if (theA.myTwoWay != theB.myTwoWay || theA.mySpeed != theB.mySpeed
                || !Objects.equals(theA.myStreet, theB.myStreet)) {
            return null;
        }
        List<Long> first = theA.myNodeIds;
        List<Long> second = theB.myNodeIds;
        if (theA.myTwoWay) {
            first = theA.last() == theNode ? first : reversed(first);
            second = theB.first() == theNode ? second : reversed(second);
        } else if (theA.last() == theNode && theB.first() == theNode) {
        } else if (theB.last() == theNode && theA.first() == theNode) {
            first = theB.myNodeIds;
            second = theA.myNodeIds;
        } else {
            return null;
        }
        final List<Long> nodes = new ArrayList<>(first);
        nodes.addAll(second.subList(1, second.size()));
        return new Segment(nodes, theA.myTwoWay, theA.mySpeed, theA.myStreet);
    }

    /**
     * Returns a reversed copy of a list of node IDs.
     *
     * @param theNodes the node IDs
     * @return the reversed copy
     */
    private static List<Long> reversed(final List<Long> theNodes) {
        final List<Long> copy = new ArrayList<>(theNodes);
        Collections.reverse(copy);
        return copy;
    }

    /**
     * Cuts the segments into stretches that go from one intersection to the next.
     *
     * @param theData the data read from the file
     * @param theSegments the segments
     * @return the stretches
     */
    private static List<Stretch> splitAtIntersections(final OsmData theData,
                                                      final List<Segment> theSegments) {
        final Map<Long, Integer> uses = new HashMap<>();
        for (final Segment segment : theSegments) {
            for (final long id : segment.myNodeIds) {
                uses.merge(id, 1, Integer::sum);
            }
            uses.merge(segment.first(), 1, Integer::sum);
            uses.merge(segment.last(), 1, Integer::sum);
        }

        final List<Stretch> stretches = new ArrayList<>();
        for (final Segment segment : theSegments) {
            List<Long> current = new ArrayList<>();
            current.add(segment.first());
            double miles = 0;
            for (int i = 1; i < segment.myNodeIds.size(); i++) {
                final double[] a = theData.myNodes.get(segment.myNodeIds.get(i - 1));
                final long id = segment.myNodeIds.get(i);
                final double[] b = theData.myNodes.get(id);
                miles += GeoProjection.distanceMiles(a[0], a[1], b[0], b[1]);
                current.add(id);
                if (uses.get(id) > 1 && id != current.get(0) && miles > 0) {
                    stretches.add(new Stretch(current, segment.myTwoWay,
                            miles, segment.mySpeed, segment.myStreet));
                    current = new ArrayList<>();
                    current.add(id);
                    miles = 0;
                }
            }
        }
        return stretches;
    }

    /**
     * Keeps only the largest connected group of roads.
     *
     * @param theStretches all the stretches
     * @return the stretches in the largest connected group
     */
    private static List<Stretch> keepLargestPart(final List<Stretch> theStretches) {
        final Map<Long, List<Long>> neighbours = new LinkedHashMap<>();
        for (final Stretch stretch : theStretches) {
            neighbours.computeIfAbsent(stretch.from(), key -> new ArrayList<>()).add(stretch.to());
            neighbours.computeIfAbsent(stretch.to(), key -> new ArrayList<>()).add(stretch.from());
        }

        final Set<Long> seen = new HashSet<>();
        Set<Long> largest = Set.of();
        for (final long start : neighbours.keySet()) {
            if (seen.contains(start)) {
                continue;
            }
            // Breadth-first search to collect everything connected to start.
            final Set<Long> part = new HashSet<>();
            final Deque<Long> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            while (!queue.isEmpty()) {
                final long current = queue.remove();
                part.add(current);
                for (final long next : neighbours.get(current)) {
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                }
            }
            if (part.size() > largest.size()) {
                largest = part;
            }
        }

        final List<Stretch> kept = new ArrayList<>();
        for (final Stretch stretch : theStretches) {
            if (largest.contains(stretch.from())) {
                kept.add(stretch);
            }
        }
        return kept;
    }

    /**
     * Names each intersection after the streets that meet there, such as
     * "1st Ave &amp; Pine St". Repeated names get (2), (3) and so on.
     *
     * @param theStretches the stretches
     * @return each intersection's name, by node ID
     */
    private static Map<Long, String> nameIntersections(final List<Stretch> theStretches) {
        final Map<Long, Set<String>> streets = new HashMap<>();
        for (final Stretch stretch : theStretches) {
            for (final long id : List.of(stretch.from(), stretch.to())) {
                final Set<String> names = streets.computeIfAbsent(id,
                        key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                if (stretch.myStreet != null) {
                    names.add(stretch.myStreet);
                }
            }
        }

        final List<Long> ids = new ArrayList<>(streets.keySet());
        Collections.sort(ids);
        final Map<String, Integer> timesUsed = new HashMap<>();
        final Map<Long, String> result = new HashMap<>();
        for (final long id : ids) {
            final List<String> names = new ArrayList<>(streets.get(id));
            final String base;
            if (names.isEmpty()) {
                base = "Node " + id;
            } else {
                base = String.join(" & ",
                        names.subList(0, Math.min(MAX_NAMES_IN_INTERSECTION, names.size())));
            }
            final int count = timesUsed.merge(base, 1, Integer::sum);
            result.put(id, count == 1 ? base : base + " (" + count + ")");
        }
        return result;
    }

    /**
     * Returns which way traffic can go on a way.
     *
     * @param theWay the way
     * @return FORWARD, BACKWARD or BOTH_WAYS
     */
    private static int direction(final OsmWay theWay) {
        final String oneway = theWay.myTags.getOrDefault("oneway", "")
                .toLowerCase(Locale.ROOT);
        switch (oneway) {
            case "yes", "true", "1":
                return FORWARD;
            case "-1", "reverse":
                return BACKWARD;
            case "no", "false", "0":
                return BOTH_WAYS;
            default:
                break;
        }
        final String junction = theWay.myTags.getOrDefault("junction", "");
        if ("motorway".equals(theWay.myTags.get("highway"))
                || "roundabout".equals(junction) || "circular".equals(junction)) {
            return FORWARD;
        }
        return BOTH_WAYS;
    }

    /**
     * Returns a way's speed limit in mph, from its maxspeed tag or the
     * default for its road type.
     *
     * @param theWay the way
     * @return the speed limit in mph
     */
    private static double speedLimit(final OsmWay theWay) {
        final double fallback = DEFAULT_SPEEDS.get(theWay.myTags.get("highway"));
        final String maxspeed = theWay.myTags.get("maxspeed");
        if (maxspeed == null) {
            return fallback;
        }
        String value = maxspeed.split(";")[0].trim().toLowerCase(Locale.ROOT);
        final boolean mph = value.endsWith("mph");
        value = value.replace("mph", "").replace("km/h", "").replace("kmh", "").trim();
        try {
            final double number = Double.parseDouble(value);
            final double speed = mph ? number : Math.round(number * MILES_PER_KM);
            return Double.isFinite(speed) && speed > 0 ? speed : fallback;
        } catch (final NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * Returns a way's street name, or its ref (like "I 5") if it has no name.
     *
     * @param theWay the way
     * @return the street name, or null if it has none
     */
    private static String streetName(final OsmWay theWay) {
        final String name = theWay.myTags.get("name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        final String ref = theWay.myTags.get("ref");
        return ref == null || ref.isBlank() ? null : ref.trim();
    }

    /**
     * Converts a latitude and longitude to map coordinates.
     *
     * @param theProjection the projection
     * @param thePosition {latitude, longitude}
     * @return {x, y}
     */
    private static int[] project(final GeoProjection theProjection, final double[] thePosition) {
        return new int[] {(int) Math.round(theProjection.toX(thePosition[1])),
                (int) Math.round(theProjection.toY(thePosition[0]))};
    }

    /**
     * Adds a piece if it has at least two nodes.
     *
     * @param thePieces the list to add to
     * @param theWay the way it came from
     * @param theNodes the node IDs
     */
    private static void addPiece(final List<WayPiece> thePieces, final OsmWay theWay,
                                 final List<Long> theNodes) {
        if (theNodes.size() >= 2) {
            thePieces.add(new WayPiece(theWay, theNodes));
        }
    }

    /**
     * Returns whether a position is inside the bounds.
     *
     * @param thePosition {latitude, longitude}
     * @param theBounds {minlat, minlon, maxlat, maxlon}
     * @return true if inside
     */
    private static boolean inside(final double[] thePosition, final double[] theBounds) {
        return thePosition[0] >= theBounds[0] && thePosition[0] <= theBounds[2]
                && thePosition[1] >= theBounds[1] && thePosition[1] <= theBounds[3];
    }

    /** The data read from the OSM file. */
    private static final class OsmData {
        /** {minlat, minlon, maxlat, maxlon}, or null if missing. */
        private double[] myBounds;

        /** Each node's {latitude, longitude}, by ID. */
        private final Map<Long, double[]> myNodes = new HashMap<>();

        /** The drivable ways. */
        private final List<OsmWay> myWays = new ArrayList<>();
    }

    /** An OSM way: its node IDs and tags. */
    private static final class OsmWay {
        /** The node IDs, in order. */
        private final List<Long> myNodeIds = new ArrayList<>();

        /** The tags, such as name and oneway. */
        private final Map<String, String> myTags = new HashMap<>();
    }

    /**
     * A stretch of road between two intersections.
     *
     * @param myNodeIds the node IDs, in the direction of travel
     * @param myTwoWay whether it is two-way
     * @param myMiles the length in miles
     * @param mySpeed the speed limit in mph
     * @param myStreet the street name, or null
     */
    private record Stretch(List<Long> myNodeIds, boolean myTwoWay,
                           double myMiles, double mySpeed, String myStreet) {

        /**
         * Returns the start node ID.
         *
         * @return the start node ID
         */
        long from() {
            return myNodeIds.get(0);
        }

        /**
         * Returns the end node ID.
         *
         * @return the end node ID
         */
        long to() {
            return myNodeIds.get(myNodeIds.size() - 1);
        }
    }

    /** A run of road with the same name, speed limit and direction. */
    private static final class Segment {
        /** The node IDs, in the direction of travel. */
        private final List<Long> myNodeIds;

        /** Whether it is two-way. */
        private final boolean myTwoWay;

        /** The speed limit in mph. */
        private final double mySpeed;

        /** The street name, or null. */
        private final String myStreet;

        /**
         * Creates a segment.
         *
         * @param theNodeIds the node IDs
         * @param theTwoWay whether it is two-way
         * @param theSpeed the speed limit in mph
         * @param theStreet the street name, or null
         */
        Segment(final List<Long> theNodeIds, final boolean theTwoWay,
                final double theSpeed, final String theStreet) {
            myNodeIds = theNodeIds;
            myTwoWay = theTwoWay;
            mySpeed = theSpeed;
            myStreet = theStreet;
        }

        /**
         * Returns the first node ID.
         *
         * @return the first node ID
         */
        long first() {
            return myNodeIds.get(0);
        }

        /**
         * Returns the last node ID.
         *
         * @return the last node ID
         */
        long last() {
            return myNodeIds.get(myNodeIds.size() - 1);
        }
    }

    /**
     * Part of a way that is inside the bounds.
     *
     * @param myWay the way it came from
     * @param myNodeIds the node IDs
     */
    private record WayPiece(OsmWay myWay, List<Long> myNodeIds) { }
}