package model;

/**
 * Converts latitude and longitude to x and y map coordinates using the Web
 * Mercator projection, the same one OpenStreetMap tiles use.
 *
 * <p>Coordinates are pixels at a fixed zoom level, measured from the
 * top-left corner of a bounding box, so x grows east and y grows south.</p>
 *
 * @author Abdullah Altaae
 * @version 8 November 2026
 */
public final class GeoProjection {

    /** Size of one OpenStreetMap tile, in pixels. */
    public static final int TILE_SIZE = 256;

    /** Highest latitude Web Mercator can show. */
    private static final double MAX_LATITUDE = 85.05112878;

    /** The zoom level. */
    private final int myZoom;

    /** World x of the bounding box's west edge, in pixels. */
    private final double myOriginX;

    /** World y of the bounding box's north edge, in pixels. */
    private final double myOriginY;

    /**
     * Creates a projection whose (0, 0) is the north-west corner of a bounding box.
     *
     * @param theNorth the north edge latitude
     * @param theWest  the west edge longitude
     * @param theZoom  the zoom level, from 0 to 22
     * @throws IllegalArgumentException if a value is out of range
     */
    public GeoProjection(final double theNorth, final double theWest, final int theZoom) {
        if (theZoom < 0 || theZoom > 22) {
            throw new IllegalArgumentException("zoom must be 0 to 22, was " + theZoom);
        }
        checkLatitude(theNorth);
        checkLongitude(theWest);
        myZoom = theZoom;
        myOriginX = worldX(theWest, theZoom);
        myOriginY = worldY(theNorth, theZoom);
    }

    /**
     * Returns the x map coordinate of a longitude.
     *
     * @param theLon the longitude
     * @return the x coordinate, in pixels
     * @throws IllegalArgumentException if theLon is out of range
     */
    public double toX(final double theLon) {
        checkLongitude(theLon);
        return worldX(theLon, myZoom) - myOriginX;
    }

    /**
     * Returns the y map coordinate of a latitude.
     *
     * @param theLat the latitude
     * @return the y coordinate, in pixels
     * @throws IllegalArgumentException if theLat is out of range
     */
    public double toY(final double theLat) {
        checkLatitude(theLat);
        return worldY(theLat, myZoom) - myOriginY;
    }

    /**
     * Returns the zoom level.
     *
     * @return the zoom level
     */
    public int getZoom() {
        return myZoom;
    }

    /**
     * Returns the distance between two points along the earth's surface.
     *
     * @param theLat1 the first latitude
     * @param theLon1 the first longitude
     * @param theLat2 the second latitude
     * @param theLon2 the second longitude
     * @return the distance, in miles
     */
    public static double distanceMiles(final double theLat1, final double theLon1,
                                       final double theLat2, final double theLon2) {
        final double earthRadiusMiles = 3958.8;
        final double dLat = Math.toRadians(theLat2 - theLat1);
        final double dLon = Math.toRadians(theLon2 - theLon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(theLat1)) * Math.cos(Math.toRadians(theLat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * earthRadiusMiles * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Returns the world x of a longitude.
     *
     * @param theLon  the longitude
     * @param theZoom the zoom level
     * @return the world x, in pixels
     */
    private static double worldX(final double theLon, final int theZoom) {
        return (theLon + 180.0) / 360.0 * TILE_SIZE * (1L << theZoom);
    }

    /**
     * Returns the world y of a latitude.
     *
     * @param theLat  the latitude
     * @param theZoom the zoom level
     * @return the world y, in pixels
     */
    private static double worldY(final double theLat, final int theZoom) {
        final double sin = Math.sin(Math.toRadians(theLat));
        final double y = 0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI);
        return y * TILE_SIZE * (1L << theZoom);
    }

    /**
     * Checks that a latitude can be projected.
     *
     * @param theLat the latitude
     * @throws IllegalArgumentException if theLat is out of range
     */
    private static void checkLatitude(final double theLat) {
        if (!(Math.abs(theLat) <= MAX_LATITUDE)) {
            throw new IllegalArgumentException("latitude out of range: " + theLat);
        }
    }

    /**
     * Checks that a longitude is valid.
     *
     * @param theLon the longitude
     * @throws IllegalArgumentException if theLon is out of range
     */
    private static void checkLongitude(final double theLon) {
        if (!(Math.abs(theLon) <= 180.0)) {
            throw new IllegalArgumentException("longitude out of range: " + theLon);
        }
    }
}
