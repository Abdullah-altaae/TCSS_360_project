/*
 * TCSS 360 - Software Development and Quality Assurance Techniques
 * Group Project: Car Route Planner
 */

package model;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Saves, loads, and deletes generated routes, the simulation runs done on
 * each route, and the hazards found during each run.
 *
 * <p>Each simulation run belongs to one route, and each hazard belongs to
 * one simulation run.</p>
 *
 * <p>The columns of each table match the data stored in the {@link Route},
 * {@link SimulationRun}, and {@link Hazard} classes. A route's roads are
 * stored as its path string, and weather conditions and hazard types are
 * stored by their enum names.</p>
 *
 * @author Felix Kiama
 * @version 15 November 2026
 */
public class RouteDatabase {

    /** The connection URL of the database. */
    private final String myUrl;

    /**
     * Creates a database helper for the given database URL.
     *
     * @param theUrl the connection URL of the database
     */
    public RouteDatabase(final String theUrl) {
        myUrl = theUrl;
    }

    /**
     * Creates the routes, simulation_runs, and hazards tables if they do not
     * already exist. Tables that already exist are not changed, so a database
     * file made with an older version of these tables must be deleted first.
     *
     * @throws SQLException if the tables cannot be created
     */
    public void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(
            """
            CREATE TABLE if not exists routes (
                route_id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_node TEXT NOT NULL,
                end_node TEXT NOT NULL,
                path TEXT NOT NULL,
                distance REAL NOT NULL);
            """);

            stmt.execute(
            """
            CREATE TABLE if not exists simulation_runs (
                sim_id INTEGER  PRIMARY KEY AUTOINCREMENT,
                route_id INTEGER NOT NULL,
                average_traffic REAL NOT NULL,
                weather TEXT NOT NULL,
                safety_rating INTEGER NOT NULL,
                normal_time REAL NOT NULL,
                simulated_time REAL NOT NULL,
                FOREIGN KEY (route_id) REFERENCES routes(route_id) ON DELETE CASCADE);
            """);

            stmt.execute(
            """
            CREATE TABLE if not exists hazards (
                hazard_id INTEGER PRIMARY KEY AUTOINCREMENT,
                sim_id INTEGER NOT NULL,
                hazard_type TEXT NOT NULL,
                penalty INTEGER NOT NULL,
                FOREIGN KEY (sim_id) REFERENCES simulation_runs(sim_id) ON DELETE CASCADE);
            """);
        }
    }

    /**
     * Saves a new route.
     *
     * @param theStartNode the name of the starting intersection
     * @param theEndNode the name of the ending intersection
     * @param thePath the path of the route
     * @param theDistance the total distance of the route
     * @return the ID of the saved route
     * @throws SQLException if the route cannot be saved
     */
    public int saveRoute(final String theStartNode, final String theEndNode,
                          final String thePath, final double theDistance) throws SQLException {
        final String sql = "INSERT INTO routes (start_node, end_node, path, distance) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, theStartNode);
                pstmt.setString(2, theEndNode);
                pstmt.setString(3, thePath);
                pstmt.setDouble(4, theDistance);
                pstmt.executeUpdate();
                try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);           // Returns the generated ID for the route if one was produced.
                    }
                }
        }
        throw new SQLException("Insert new route succeeded but no generated id was returned");  //Error sent to controller if no ID is produced.
    }

    /**
     * Saves a new simulation run for a route.
     *
     * @param theRouteId the ID of the route that was simulated
     * @param theAverageTraffic the average traffic intensity of the route's
     *                          roads during the run, from 0.0 to 1.0
     * @param theWeather the weather during the run
     * @param theSafetyRating the safety rating of the route during the run,
     *                        from 0 to 100
     * @param theNormalTime the travel time with normal conditions
     * @param theSimulatedTime the travel time with simulated conditions
     * @return the ID of the saved simulation run
     * @throws NullPointerException if theWeather is null
     * @throws SQLException if the simulation run cannot be saved
     */
    public int saveSimulationRun(final int theRouteId, final double theAverageTraffic,
                                  final EnvironmentSimulator.WeatherCondition theWeather,
                                  final int theSafetyRating, final double theNormalTime,
                                  final double theSimulatedTime) throws SQLException {
        Objects.requireNonNull(theWeather, "weather must not be null");
        final String sql = "INSERT INTO simulation_runs (route_id, average_traffic, weather, safety_rating, normal_time, simulated_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, theRouteId);
            pstmt.setDouble(2, theAverageTraffic);
            pstmt.setString(3, theWeather.name());
            pstmt.setInt(4, theSafetyRating);
            pstmt.setDouble(5, theNormalTime);
            pstmt.setDouble(6, theSimulatedTime);
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Insert of new simulated run succeeded but no generated id was returned");
    }

    /**
     * Saves a new hazard found during a simulation run.
     *
     * @param theSimId the ID of the simulation run the hazard belongs to
     * @param theType the type of hazard
     * @param thePenalty the number of points the hazard takes off the route's
     *                   safety rating
     * @return the ID of the saved hazard
     * @throws NullPointerException if theType is null
     * @throws SQLException if the hazard cannot be saved
     */
    public int saveHazard(final int theSimId, final Hazard.HazardType theType,
                          final int thePenalty) throws SQLException {
        Objects.requireNonNull(theType, "hazard type must not be null");
        final String sql = "INSERT INTO hazards (sim_id, hazard_type, penalty) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, theSimId);
            pstmt.setString(2, theType.name());
            pstmt.setInt(3, thePenalty);
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Insert of new hazard succeeded but no generated id was returned");
    }

    /**
     * Loads a saved route by its ID. The stored path string is turned back
     * into {@link Road} objects by looking up each road ID in the given map.
     *
     * @param theRouteId the ID of the route to load
     * @param theMap the map that holds the route's roads
     * @return the saved route
     * @throws NullPointerException if theMap is null
     * @throws SQLException if no route has that ID, or one of its roads is not
     *                      on the map
     */
    public Route loadRoute(final int theRouteId, final MapGraph theMap) throws SQLException {
        Objects.requireNonNull(theMap, "map must not be null");
        final String sql = "SELECT * FROM routes WHERE route_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theRouteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Route(theRouteId, rs.getString("start_node"),
                            rs.getString("end_node"), parsePath(rs.getString("path"), theMap));
                }
            }
        }
        throw new SQLException("No saved route has ID " + theRouteId);
    }

    /**
     * Loads a saved simulation run by its ID, together with its route and
     * all of its hazards.
     *
     * @param theSimId the ID of the simulation run to load
     * @param theMap the map that holds the run's route's roads
     * @return the saved simulation run
     * @throws NullPointerException if theMap is null
     * @throws SQLException if no simulation run has that ID, or its route
     *                      cannot be loaded
     */
    public SimulationRun loadSimulationRun(final int theSimId, final MapGraph theMap)
            throws SQLException {
        final String sql = "SELECT * FROM simulation_runs WHERE sim_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theSimId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    final Route route = loadRoute(rs.getInt("route_id"), theMap);
                    return new SimulationRun(theSimId, route,
                            rs.getDouble("average_traffic"),
                            EnvironmentSimulator.WeatherCondition.valueOf(rs.getString("weather")),
                            loadHazardsForSimulationRun(theSimId),
                            rs.getInt("safety_rating"),
                            rs.getDouble("normal_time"),
                            rs.getDouble("simulated_time"));
                }
            }
        }
        throw new SQLException("No saved simulation run has ID " + theSimId);
    }

    public Hazard loadHazard(final int theHazardId) throws SQLException {
        final String sql = "SELECT * FROM hazards WHERE hazard_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theHazardId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Hazard(theHazardId, rs.getInt("sim_id"),
                            Hazard.HazardType.valueOf(rs.getString("hazard_type")));
                }
            }
        }
        throw new SQLException("No saved hazard has ID " + theHazardId);
    }

    public List<Hazard> loadHazardsForSimulationRun(final int theSimId) throws SQLException {
        final String sql = "SELECT * FROM hazards WHERE sim_id = ? ORDER BY hazard_id";
        final List<Hazard> hazards = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theSimId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    hazards.add(new Hazard(rs.getInt("hazard_id"), theSimId,
                            Hazard.HazardType.valueOf(rs.getString("hazard_type"))));
                }
            }
        }
        return hazards;
    }

    public void updateRoute(final int theRouteId, final String theStartNode,
                            final String theEndNode, final String thePath,
                            final double theDistance) throws SQLException {
        final String sql = "UPDATE routes SET start_node = ?, end_node = ?, path = ?, distance = ? WHERE route_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, theStartNode);
            pstmt.setString(2, theEndNode);
            pstmt.setString(3, thePath);
            pstmt.setDouble(4, theDistance);
            pstmt.setInt(5, theRouteId);
            requireOneRowChanged(pstmt.executeUpdate(), "route", theRouteId);
        }
    }

    public void updateSimulationRun(final int theSimId, final int theRouteId,
                                    final double theAverageTraffic,
                                    final EnvironmentSimulator.WeatherCondition theWeather,
                                    final int theSafetyRating, final double theNormalTime,
                                    final double theSimulatedTime) throws SQLException {
        Objects.requireNonNull(theWeather, "weather must not be null");
        final String sql = "UPDATE simulation_runs SET route_id = ?, average_traffic = ?, weather = ?, safety_rating = ?, normal_time = ?, simulated_time = ? WHERE sim_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theRouteId);
            pstmt.setDouble(2, theAverageTraffic);
            pstmt.setString(3, theWeather.name());
            pstmt.setInt(4, theSafetyRating);
            pstmt.setDouble(5, theNormalTime);
            pstmt.setDouble(6, theSimulatedTime);
            pstmt.setInt(7, theSimId);
            requireOneRowChanged(pstmt.executeUpdate(), "simulation run", theSimId);
        }
    }

    public void updateHazard(final int theHazardId, final int theSimId,
                             final Hazard.HazardType theType, final int thePenalty)
            throws SQLException {
        Objects.requireNonNull(theType, "hazard type must not be null");
        final String sql = "UPDATE hazards SET sim_id = ?, hazard_type = ?, penalty = ? WHERE hazard_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theSimId);
            pstmt.setString(2, theType.name());
            pstmt.setInt(3, thePenalty);
            pstmt.setInt(4, theHazardId);
            requireOneRowChanged(pstmt.executeUpdate(), "hazard", theHazardId);
        }
    }

    /**
     * Replaces all of a simulation run's hazards with the given ones. The old
     * hazards are deleted and the new ones inserted in a single transaction,
     * so if anything fails the run keeps its old hazards.
     *
     * @param theSimId the ID of the simulation run the hazards belong to
     * @param theHazards the hazards the run should have from now on
     * @return the given hazards, in the same order, with their new database
     *         IDs and the simulation run's ID filled in
     * @throws NullPointerException if theHazards is null
     * @throws SQLException if the hazards cannot be replaced, for example
     *                      because no simulation run has that ID
     */
    public List<Hazard> replaceHazards(final int theSimId, final List<Hazard> theHazards)
            throws SQLException {
        Objects.requireNonNull(theHazards, "hazards must not be null");
        final String deleteSql = "DELETE FROM hazards WHERE sim_id = ?";
        final String insertSql = "INSERT INTO hazards (sim_id, hazard_type, penalty) VALUES (?, ?, ?)";
        final List<Hazard> saved = new ArrayList<>();
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(deleteSql);
                 PreparedStatement insert = conn.prepareStatement(insertSql,
                         Statement.RETURN_GENERATED_KEYS)) {
                delete.setInt(1, theSimId);
                delete.executeUpdate();
                for (final Hazard hazard : theHazards) {
                    insert.setInt(1, theSimId);
                    insert.setString(2, hazard.getType().name());
                    insert.setInt(3, hazard.getPenalty());
                    insert.executeUpdate();
                    try (ResultSet rs = insert.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Insert of hazard succeeded but no generated id was returned");
                        }
                        saved.add(hazard.withIds(rs.getInt(1), theSimId));
                    }
                }
                conn.commit();
            } catch (final SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
        return saved;
    }

    /**
     * Deletes a route by its ID.
     *
     * @param theRouteId the ID of the route to delete
     * @throws SQLException if the route cannot be deleted
     */
    public void deleteRoute(final int theRouteId) throws SQLException {
        final String sql = "DELETE FROM routes WHERE route_id = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theRouteId);
            pstmt.execute ();
        }
    }

    /**
     * Deletes a simulation run by its ID.
     *
     * @param theSimId the ID of the simulation run to delete
     * @throws SQLException if the simulation run cannot be deleted
     */
    public void deleteSimulationRun(final int theSimId) throws SQLException {
        final String sql = "DELETE FROM simulation_runs WHERE sim_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theSimId);
            pstmt.execute ();
        }
    }

    /**
     * Deletes a hazard by its ID.
     *
     * @param theHazardId the ID of the hazard to delete
     * @throws SQLException if the hazard cannot be deleted
     */
    public void deleteHazard(final int theHazardId) throws SQLException {
        final String sql = "DELETE FROM hazards WHERE hazard_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, theHazardId);
            pstmt.execute ();
        }
    }



    /**
     * Turns a stored path string such as "1,2,3" back into the roads it names,
     * in travel order.
     *
     * @param thePath the stored path string
     * @param theMap the map to look the road IDs up in
     * @return the roads in travel order, which is empty for an empty path
     * @throws SQLException if the path names a road that is not on the map
     */
    private List<Road> parsePath(final String thePath, final MapGraph theMap)
            throws SQLException {
        final Map<Integer, Road> roadsById = new HashMap<>();
        for (final Road road : theMap.getRoads()) {
            roadsById.put(road.getId(), road);
        }
        final List<Road> path = new ArrayList<>();
        if (thePath.isEmpty()) {
            return path;
        }
        for (final String part : thePath.split(",")) {
            final Road road = roadsById.get(Integer.parseInt(part.trim()));
            if (road == null) {
                throw new SQLException("Saved route uses road " + part
                        + ", which is not on the map");
            }
            path.add(road);
        }
        return path;
    }

    private void requireOneRowChanged(final int theRowCount, final String theName,
                                      final int theId) throws SQLException {
        if (theRowCount == 0) {
            throw new SQLException("No saved " + theName + " has ID " + theId);
        }
    }

    /**
     * Opens a connection to the database with foreign keys turned on.
     *
     * @return an open connection to the database
     * @throws SQLException if the connection cannot be opened
     */
    private Connection getConnection() throws SQLException {
        final Connection conn = DriverManager.getConnection(myUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

}
