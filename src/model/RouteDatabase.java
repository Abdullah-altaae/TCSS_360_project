/*
 * TCSS 360 - Software Development and Quality Assurance Techniques
 * Group Project: Car Route Planner
 */

package model;
import java.sql.*;
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
     * Loads a route by its ID.
     *
     * @param theRouteId the ID of the route to load
     * @return the distance of the route
     * @throws SQLException if the route cannot be found or loaded
     */
    public double loadRoute(final int theRouteId) throws SQLException {
        final String sql = "SELECT * FROM routes WHERE route_id = ?;";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, theRouteId);
            try (final ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    final String startNode = rs.getString("start_node");
                    final String endNode = rs.getString("end_node");
                    final  String path = rs.getString("path");
                    final double distance = rs.getDouble("distance");
                    return distance;
                }
            }
        }
        throw new SQLException("Route loading failed");
    }

    /**
     * Loads a simulation run by its ID.
     *
     * @param theSimId the ID of the simulation run to load
     * @throws SQLException if the simulation run cannot be loaded
     */
    public void loadSimulationRun(final int theSimId) throws SQLException {
        final String sql = "SELECT * FROM simulation_runs WHERE sim_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, theSimId);
            try (final ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    final int routeId = rs.getInt("route_id");
                    final double averageTraffic = rs.getDouble("average_traffic");
                    final EnvironmentSimulator.WeatherCondition weather =
                            EnvironmentSimulator.WeatherCondition.valueOf(rs.getString("weather"));
                    final int safetyRating = rs.getInt("safety_rating");
                    final double normalTime = rs.getDouble("normal_time");
                    final double simulatedTime = rs.getDouble("simulated_time");
                }
            }
        }
        throw new SQLException("Simulation Run loading failed");

    }

    /**
     * Loads a hazard by its ID.
     *
     * @param theHazardId the ID of the hazard to load
     * @throws SQLException if the hazard cannot be loaded
     */
    public void loadHazard(final int theHazardId) throws SQLException {
        final String sql = "SELECT * FROM hazards WHERE hazard_id = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, theHazardId);
            try (final ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    final int simId = rs.getInt("sim_id");
                    final Hazard.HazardType type = Hazard.HazardType.valueOf(rs.getString("hazard_type"));
                    final int penalty = rs.getInt("penalty");
                }
            }
        }
        throw new SQLException("Hazard loading failed");

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
