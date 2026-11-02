package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import model.Intersection;
import model.MapGraph;
import model.Road;
import model.RoutePlanner;

/**
 * The main window of the Self-Driving Car Route Planner.
 * <p>
 * The dashboard contains a menu bar (File and Help), a route input form for
 * choosing start and end intersections, a map panel that draws the city graph
 * and highlights the planned route, and a safety log area for messages.
 * </p>
 * <p>
 * Roads refer to their intersections by ID, so the dashboard looks up the
 * matching {@link Intersection} whenever it needs coordinates.
 * </p>
 *
 * @author Anjana Virani
 * @version 1 November 2026
 */
public class PlannerDashboard extends JFrame {

    /** Default width of the window in pixels. */
    private static final int WINDOW_WIDTH = 850;

    /** Default height of the window in pixels. */
    private static final int WINDOW_HEIGHT = 650;

    /** Number of minutes in one hour. */
    private static final double MINUTES_PER_HOUR = 60.0;

    /** The city map displayed by this dashboard. */
    private final MapGraph map;

    /** The planner used to compute routes. */
    private final RoutePlanner planner;

    /** Drop-down list of possible start intersections. */
    private JComboBox<String> startBox;

    /** Drop-down list of possible end intersections. */
    private JComboBox<String> endBox;

    /** Text area that shows route details, hazards and errors. */
    private JTextArea logArea;

    /** Panel that draws the map and the highlighted route. */
    private final MapPanel mapPanel;

    /**
     * Creates the dashboard window for the given map and route planner.
     *
     * @param theMap the city map to display
     * @param thePlanner the route planner used to compute routes
     */
    public PlannerDashboard(final MapGraph theMap, final RoutePlanner thePlanner) {
        super("Self-Driving Car Route Planner");
        map = theMap;
        planner = thePlanner;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setLayout(new BorderLayout(8, 8));

        mapPanel = new MapPanel();
        add(buildInputPanel(), BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(buildLogPanel(), BorderLayout.SOUTH);

        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
    }

    /**
     * Builds the menu bar with File (Save Route, Load Route, Exit) and
     * Help (About, Instructions) menus.
     *
     * @return the completed menu bar
     */
    private JMenuBar buildMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem saveItem = new JMenuItem("Save Route");
        saveItem.addActionListener(theEvent -> JOptionPane.showMessageDialog(this,
                "Saving routes will be available once the database is connected.",
                "Save Route", JOptionPane.INFORMATION_MESSAGE));

        final JMenuItem loadItem = new JMenuItem("Load Route");
        loadItem.addActionListener(theEvent -> JOptionPane.showMessageDialog(this,
                "Loading routes will be available once the database is connected.",
                "Load Route", JOptionPane.INFORMATION_MESSAGE));

        final JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(theEvent -> {
            dispose();
            System.exit(0);
        });

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        final JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(theEvent -> JOptionPane.showMessageDialog(this,
                "Self-Driving Car Route Planner and Safety Checker\n"
                        + "Team: Abdullah, Felix, Allia, Anjana\n"
                        + "Version: Iteration 1",
                "About", JOptionPane.INFORMATION_MESSAGE));

        final JMenuItem instructionsItem = new JMenuItem("Instructions");
        instructionsItem.addActionListener(theEvent -> JOptionPane.showMessageDialog(this,
                "1. Choose a start intersection.\n"
                        + "2. Choose an end intersection.\n"
                        + "3. Click \"Plan Route\".\n"
                        + "The route is highlighted in blue on the map and its\n"
                        + "details appear in the safety log at the bottom.",
                "Instructions", JOptionPane.INFORMATION_MESSAGE));

        helpMenu.add(aboutItem);
        helpMenu.add(instructionsItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    /**
     * Builds the route input form with start and end drop-downs and a
     * "Plan Route" button.
     *
     * @return the input panel
     */
    private JPanel buildInputPanel() {
        final List<String> ids = new ArrayList<>();
        for (final Intersection intersection : map.getIntersections()) {
            ids.add(intersection.getId());
        }
        final String[] idArray = ids.toArray(new String[0]);

        startBox = new JComboBox<>(idArray);
        endBox = new JComboBox<>(idArray);
        if (idArray.length > 1) {
            endBox.setSelectedIndex(idArray.length - 1);
        }

        final JButton planButton = new JButton("Plan Route");
        planButton.addActionListener(theEvent -> onPlanRoute());

        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Route Input"));
        panel.add(new JLabel("Start:"));
        panel.add(startBox);
        panel.add(new JLabel("End:"));
        panel.add(endBox);
        panel.add(planButton);
        return panel;
    }

    /**
     * Builds the scrollable safety log / report area.
     *
     * @return the log panel
     */
    private JScrollPane buildLogPanel() {
        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setText("Welcome! Choose a start and end intersection, then click Plan Route.\n");

        final JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Safety Log / Report"));
        return scroll;
    }

    /**
     * Finds the intersection with the given ID on the map.
     *
     * @param theId the intersection ID to look for
     * @return the matching intersection, or null if none exists
     */
    private Intersection findIntersection(final String theId) {
        for (final Intersection intersection : map.getIntersections()) {
            if (intersection.getId().equals(theId)) {
                return intersection;
            }
        }
        return null;
    }

    /**
     * Handles the "Plan Route" button. Reads the selected intersections,
     * asks the planner for a route and displays the result or an error.
     */
    private void onPlanRoute() {
        final String startId = (String) startBox.getSelectedItem();
        final String endId = (String) endBox.getSelectedItem();

        if (startId == null || endId == null) {
            showError("Please choose both a start and an end intersection.");
            return;
        }
        if (startId.equals(endId)) {
            showError("Start and end intersections must be different.");
            return;
        }

        final Intersection start = findIntersection(startId);
        final Intersection end = findIntersection(endId);

        try {
            final List<Road> route = planner.findRoute(start, end);
            if (route == null || route.isEmpty()) {
                showError("No route found from " + startId + " to " + endId + ".");
            } else {
                showRoute(route);
            }
        } catch (final RuntimeException ex) {
            showError("Route planning failed: " + ex.getMessage());
        }
    }

    /**
     * Highlights the given route on the map and writes its details
     * (each road, total distance and estimated time) to the log.
     * Travel time includes any traffic delay on each road.
     *
     * @param theRoute the ordered list of roads in the route
     */
    public void showRoute(final List<Road> theRoute) {
        mapPanel.setRoute(theRoute);

        double totalDistance = 0;
        double totalMinutes = 0;
        final StringBuilder sb = new StringBuilder();
        sb.append("\nRoute from ").append(theRoute.get(0).getStart())
                .append(" to ").append(theRoute.get(theRoute.size() - 1).getEnd())
                .append(":\n");

        for (final Road road : theRoute) {
            totalDistance += road.getDistance();
            totalMinutes += road.getTravelTime() * MINUTES_PER_HOUR;
            sb.append(String.format("  %s -> %s  %.1f mi @ %.0f mph%s%n",
                    road.getStart(), road.getEnd(), road.getDistance(),
                    road.getSpeedLimit(), road.isBlocked() ? "  (BLOCKED)" : ""));
        }
        sb.append(String.format("Total: %.1f mi, about %.1f minutes%n",
                totalDistance, totalMinutes));

        logArea.append(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Shows an error message in a dialog and records it in the log.
     *
     * @param theMessage the error message to show
     */
    public void showError(final String theMessage) {
        logArea.append("\nERROR: " + theMessage + "\n");
        JOptionPane.showMessageDialog(this, theMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Panel that draws intersections and roads, scaled to fit, and
     * highlights the current route. Blocked intersections and roads are
     * drawn in red.
     */
    private final class MapPanel extends JPanel {

        /** Margin around the drawing in pixels. */
        private static final int PAD = 40;

        /** Diameter of an intersection circle in pixels. */
        private static final int NODE_SIZE = 16;

        /** Color used for the highlighted route. */
        private static final Color ROUTE_COLOR = new Color(30, 110, 230);

        /** The route currently highlighted, or an empty list. */
        private List<Road> myRoute = new ArrayList<>();

        /** Creates the map panel with a white background. */
        MapPanel() {
            super();
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(800, 400));
            setBorder(BorderFactory.createTitledBorder("Map"));
        }

        /**
         * Sets the route to highlight and redraws the map.
         *
         * @param theRoute the route to highlight
         */
        void setRoute(final List<Road> theRoute) {
            myRoute = new ArrayList<>(theRoute);
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics theGraphics) {
            super.paintComponent(theGraphics);
            final Graphics2D g2 = (Graphics2D) theGraphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int maxX = 1;
            int maxY = 1;
            for (final Intersection i : map.getIntersections()) {
                maxX = Math.max(maxX, i.getX());
                maxY = Math.max(maxY, i.getY());
            }
            final double scale = Math.min((getWidth() - 2.0 * PAD) / maxX,
                    (getHeight() - 2.0 * PAD) / maxY);

            // All roads in thin gray (red if blocked), then the route in thick
            // blue on top so two-way roads don't cover the highlight.
            g2.setStroke(new BasicStroke(3));
            for (final Integer roadId : map.getRoads()) {
                final Road road = map.getRoad(roadId);
                if (road != null) {
                    g2.setColor(road.isBlocked() ? Color.RED : Color.LIGHT_GRAY);
                    drawRoad(g2, road, scale);
                }
            }
            g2.setColor(ROUTE_COLOR);
            g2.setStroke(new BasicStroke(6));
            for (final Road road : myRoute) {
                drawRoad(g2, road, scale);
            }

            // Intersections: dark circles, red if blocked, with ID labels.
            g2.setStroke(new BasicStroke(1));
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            for (final Intersection i : map.getIntersections()) {
                final int px = toPx(i.getX(), scale);
                final int py = toPx(i.getY(), scale);
                g2.setColor(i.isBlocked() ? Color.RED : Color.DARK_GRAY);
                g2.fillOval(px - NODE_SIZE / 2, py - NODE_SIZE / 2, NODE_SIZE, NODE_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawString(i.getId(), px + NODE_SIZE / 2 + 2, py - NODE_SIZE / 2);
            }
        }

        /**
         * Draws one road as a straight line using the current color and stroke.
         * Roads whose intersections can't be found are skipped.
         *
         * @param theG2 the graphics context
         * @param theRoad the road to draw
         * @param theScale the scale factor
         */
        private void drawRoad(final Graphics2D theG2, final Road theRoad,
                              final double theScale) {
            final Intersection start = findIntersection(theRoad.getStart());
            final Intersection end = findIntersection(theRoad.getEnd());
            if (start != null && end != null) {
                theG2.drawLine(toPx(start.getX(), theScale), toPx(start.getY(), theScale),
                        toPx(end.getX(), theScale), toPx(end.getY(), theScale));
            }
        }

        /**
         * Converts a map coordinate to a pixel position on this panel.
         *
         * @param theCoord the map coordinate
         * @param theScale the scale factor
         * @return the pixel position
         */
        private int toPx(final int theCoord, final double theScale) {
            return PAD + (int) Math.round(theCoord * theScale);
        }
    }
}