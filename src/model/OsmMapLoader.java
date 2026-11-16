
package model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


public final class OsmMapLoader {

    /** The zoom level used to turn latitude and longitude into map coordinates. */
    public static final int DEFAULT_ZOOM = 17;

    /** Speed limits, in mph, for each kind of road we keep. */
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

    /** Prevents instantiation; use the static load methods. */
    private OsmMapLoader() {
    }


    public static MapGraph load(final Path theFile) throws IOException {
        Objects.requireNonNull(theFile, "file must not be null");
        try (InputStream in = Files.newInputStream(theFile)) {
            return load(in, DEFAULT_ZOOM);
        }
    }

    /**
     * Loads a map from OSM XML.
     */
    public static MapGraph load(final InputStream theInput, final int theZoom)
            throws IOException {
        Objects.requireNonNull(theInput, "input must not be null");
        final OsmData data = parse(theInput);
        if (data.myBounds == null) {
            throw new IOException("the OSM file has no <bounds> element");
        }
        return build(data, theZoom);
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    /**
     * Reads the bounds, every node's position and every drivable way.
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
        return data;
    }

    /**
     * Reads a decimal attribute.
     */
    private static double number(final XMLStreamReader theReader, final String theName) {
        final String value = theReader.getAttributeValue(null, theName);
        if (value == null) {
            throw new NumberFormatException("missing attribute " + theName);
        }
        return Double.parseDouble(value);
    }

    /**
     * Returns whether a car can drive on a way.
     *
     */
    private static boolean isDrivable(final OsmWay theWay) {
        final String highway = theWay.myTags.get("highway");
        if (highway == null || !DEFAULT_SPEEDS.containsKey(highway)) {
            return false;
        }
        final String access = theWay.myTags.getOrDefault("access", "yes");
        final String oneway = theWay.myTags.getOrDefault("oneway", "no");
        return !"no".equals(access) && !"private".equals(access)
                && !"reversible".equals(oneway) && theWay.myNodeIds.size() >= 2;
    }

    // ------------------------------------------------------------------
    // Building the graph
    // ------------------------------------------------------------------

    /**
     * Turns the parsed ways into intersections and roads.
     */
    private static MapGraph build(final OsmData theData, final int theZoom) {
        final double[] bounds = theData.myBounds;
        final GeoProjection projection = new GeoProjection(bounds[2], bounds[1], theZoom);

        // Split every way into pieces that stay inside the bounds box.
        final List<WayPiece> pieces = new ArrayList<>();
        for (final OsmWay way : theData.myWays) {
            List<Long> current = new ArrayList<>();
            for (final long id : way.myNodeIds) {
                final double[] position = theData.myNodes.get(id);
                if (position != null && inside(position, bounds)) {
                    current.add(id);
                } else {
                    addPiece(pieces, way, current);
                    current = new ArrayList<>();
                }
            }
            addPiece(pieces, way, current);
        }

        final Map<Long, Integer> uses = new HashMap<>();
        for (final WayPiece piece : pieces) {
            for (final long id : piece.myNodeIds) {
                uses.merge(id, 1, Integer::sum);
            }
            uses.merge(piece.myNodeIds.get(0), 1, Integer::sum);
            uses.merge(piece.myNodeIds.get(piece.myNodeIds.size() - 1), 1, Integer::sum);
        }
        final List<Stretch> stretches = new ArrayList<>();
        for (final WayPiece piece : pieces) {
            final boolean twoWay = !isOneWay(piece.myWay);
            final double speed = DEFAULT_SPEEDS.get(piece.myWay.myTags.get("highway"));
            long from = piece.myNodeIds.get(0);
            double miles = 0;
            for (int i = 1; i < piece.myNodeIds.size(); i++) {
                final double[] a = theData.myNodes.get(piece.myNodeIds.get(i - 1));
                final long id = piece.myNodeIds.get(i);
                final double[] b = theData.myNodes.get(id);
                miles += GeoProjection.distanceMiles(a[0], a[1], b[0], b[1]);
                if (uses.get(id) > 1 && id != from && miles > 0) {
                    stretches.add(new Stretch(from, id, twoWay, miles, speed));
                    from = id;
                    miles = 0;
                }
            }
        }

        final Map<Long, String> intersectionNames = new LinkedHashMap<>();
        for (final Stretch stretch : stretches) {
            intersectionNames.putIfAbsent(stretch.myFrom, "Node " + stretch.myFrom);
            intersectionNames.putIfAbsent(stretch.myTo, "Node " + stretch.myTo);
        }

        final MapGraph graph = new MapGraph();
        for (final Map.Entry<Long, String> entry : intersectionNames.entrySet()) {
            final double[] position = theData.myNodes.get(entry.getKey());
            graph.addIntersection(new Intersection(entry.getValue(),
                    (int) Math.round(projection.toX(position[1])),
                    (int) Math.round(projection.toY(position[0])), false));
        }
        int nextId = 0;
        for (final Stretch stretch : stretches) {
            final String from = intersectionNames.get(stretch.myFrom);
            final String to = intersectionNames.get(stretch.myTo);
            graph.addRoad(new Road(nextId++, from, to, stretch.myMiles, stretch.mySpeed));
            if (stretch.myTwoWay) {
                graph.addRoad(new Road(nextId++, to, from, stretch.myMiles, stretch.mySpeed));
            }
        }
        return graph;
    }

    /**
     * Adds a piece of a way if it has at least two nodes.
     */
    private static void addPiece(final List<WayPiece> thePieces, final OsmWay theWay,
                                 final List<Long> theNodes) {
        if (theNodes.size() >= 2) {
            thePieces.add(new WayPiece(theWay, theNodes));
        }
    }

    /**
     * Returns whether a position is inside the bounds box.
     */
    private static boolean inside(final double[] thePosition, final double[] theBounds) {
        return thePosition[0] >= theBounds[0] && thePosition[0] <= theBounds[2]
                && thePosition[1] >= theBounds[1] && thePosition[1] <= theBounds[3];
    }

    /**
     * Returns whether a way is one-way: tagged oneway=yes, or a motorway.
     */
    private static boolean isOneWay(final OsmWay theWay) {
        return "yes".equals(theWay.myTags.get("oneway"))
                || "motorway".equals(theWay.myTags.get("highway"));
    }

    /** Everything read from the OSM file. */
    private static final class OsmData {
        /** {minlat, minlon, maxlat, maxlon}, or null if the file had none. */
        private double[] myBounds;

        /** Each node's {latitude, longitude}, by node ID. */
        private final Map<Long, double[]> myNodes = new HashMap<>();

        /** The drivable ways. */
        private final List<OsmWay> myWays = new ArrayList<>();
    }

    /** One OSM way: its node IDs in order and its tags. */
    private static final class OsmWay {
        /** The node IDs, in order. */
        private final List<Long> myNodeIds = new ArrayList<>();

        /** The tags, such as highway, name, oneway and maxspeed. */
        private final Map<String, String> myTags = new HashMap<>();
    }

    /**
     * A stretch of road between two intersections.
     */
    private record Stretch(long myFrom, long myTo, boolean myTwoWay,
                           double myMiles, double mySpeed) { }

    /**
     * Part of a way that lies inside the bounds box.
     */
    private record WayPiece(OsmWay myWay, List<Long> myNodeIds) { }
}