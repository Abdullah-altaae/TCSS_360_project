package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Sandbox for quick manual checks of RouteDatabase's insert and delete methods.
 * It uses its own test database file, which is deleted at the start of every run,
 * so each run starts from an empty database and never touches the real one.
 *
 * @author Felix Kiama
 * @version 8 November 2026
 */
public final class DataBaseSandBox {

    /** The test database file, kept separate from the real database. */
    private static final String TEST_FILE = "sandbox_test.db";

    /** The JDBC URL for the test database. */
    private static final String TEST_URL = "jdbc:sqlite:" + TEST_FILE;

    /** Prevents instantiation; this class only has a main method. */
    private DataBaseSandBox() {
    }

    /**
     * Runs the checks and prints PASS or FAIL for each one, then a summary.
     *
     * @param theArgs not used
     * @throws IOException if the old test file cannot be deleted (for example,
     *                     because DB Browser still has it open)
     * @throws SQLException if a database operation fails unexpectedly
     */
    public static void main(final String[] theArgs) throws IOException, SQLException {
        Files.deleteIfExists(Path.of(TEST_FILE));
        final RouteDatabase db = new RouteDatabase(TEST_URL);
        db.createTables();
        int failures = 0;

        // ----- Inserts -----
        final int routeA = db.saveRoute("A", "D", "R1,R2,R3", 7.5);
        failures += check(routeA > 0, "saveRoute returns a positive id");
        failures += check(countRows("routes") == 1, "routes has 1 row after the first insert");

        final int routeB = db.saveRoute("B", "E", "R4", 2.0);
        failures += check(routeB != routeA, "a second route gets a different id");
        failures += check(countRows("routes") == 2, "routes has 2 rows after the second insert");

        final int runA = db.saveSimulationRun(routeA, "RAIN", 1.5, 0.25, 0.40);
        failures += check(runA > 0, "saveSimulationRun returns a positive id");
        failures += check(countRows("simulation_runs") == 1, "simulation_runs has 1 row");

        final int hazardA = db.saveHazard(runA, "BLOCKED_ROAD", "R2", 3);
        failures += check(hazardA > 0, "saveHazard returns a positive id");
        failures += check(countRows("hazards") == 1, "hazards has 1 row");

        // ----- Foreign key enforcement -----
        boolean rejected = false;
        try {
            db.saveSimulationRun(999, "CLEAR", 0.0, 1.0, 1.0);
        } catch (final SQLException ex) {
            rejected = true;
        }
        failures += check(rejected, "a run pointing to nonexistent route 999 is rejected");

        // ----- Deletes -----
        db.deleteHazard(hazardA);
        failures += check(countRows("hazards") == 0, "deleteHazard removes the hazard");

        db.saveHazard(runA, "SHARP_TURNS", "R1-R3", 2);
        db.deleteRoute(routeA);
        failures += check(countRows("routes") == 1, "deleteRoute removes only that route");
        failures += check(countRows("simulation_runs") == 0,
                "deleting a route also deletes its simulation runs (cascade)");
        failures += check(countRows("hazards") == 0,
                "deleting a route also deletes its runs' hazards (cascade)");

        db.deleteRoute(999);
        failures += check(countRows("routes") == 1, "deleting a nonexistent id changes nothing");

        System.out.println();
        System.out.println(failures == 0 ? "All checks passed." : failures + " check(s) FAILED.");
    }

    /**
     * Prints whether a single check passed or failed.
     *
     * @param thePassed      true if the check passed
     * @param theDescription what was checked
     * @return 0 if the check passed, 1 if it failed, so the results can be added up
     */
    private static int check(final boolean thePassed, final String theDescription) {
        System.out.println((thePassed ? "PASS: " : "FAIL: ") + theDescription);
        return thePassed ? 0 : 1;
    }

    /**
     * Counts the rows in a table by querying the test database directly, without
     * going through RouteDatabase, so the checks don't rely on the code being tested.
     *
     * @param theTable the table name; must be a fixed name from this class, never
     *                 user input, because table names cannot use ? placeholders
     * @return the number of rows in the table
     * @throws SQLException if the query fails
     */
    private static int countRows(final String theTable) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM " + theTable;
        try (Connection conn = DriverManager.getConnection(TEST_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}