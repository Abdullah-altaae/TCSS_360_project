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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import javax.swing.KeyStroke;

import controller.PlannerController;
import controller.PlannerController.SafetyReport;
import model.Intersection;
import model.MapGraph;
import model.Road;
import model.SafetyRating;

/**
 * The main window of the Self-Driving Car Route Planner (the view in MVC).
 * <p>
 * The dashboard contains a menu bar (File and Help), a route input form for
 * choosing start and end intersections, a map panel that draws the city graph
 * and highlights the planned route, and a safety log area for messages.
 * </p>
 * <p>
 * User actions are passed to the {@link PlannerController}, which talks to
 * the model and calls {@link #showRoute(List)}, {@link #showSafetyReport}
 * or {@link #showError(String)} with the result. The dashboard only reads
 * the map to draw it.
 * </p>
 * <p>
 * Map colors: blue is the route, orange marks hazards, red marks blocked
 * roads and intersections, and a dashed green line shows a safer
 * alternative route.
 * </p>
 *
 * @author Anjana Virani
 * @version 21 November 2026
 */
public class PlannerDashboard extends JFrame {

    /** Default width of the window in pixels. */
    private static final int WINDOW_WIDTH = 1100;

    /** Default height of the window in pixels. */
    private static final int WINDOW_HEIGHT = 680;

    /** Color used for hazards and the MODERATE rating. */
    private static final Color HAZARD_COLOR = new Color(245, 140, 0);

    /** Color used for the safer alternative route and the SAFE rating. */
    private static final Color SAFE_COLOR = new Color(20, 150, 60);

    /** Number of minutes in one hour. */
    private static final double MINUTES_PER_HOUR = 60.0;

    /** The city map displayed by this dashboard. */
    private final MapGraph map;

    /** The controller that handles user actions. */
    private final PlannerController controller;

    /** Drop-down list of possible start intersections. */
    private JComboBox<String> startBox;

    /** Drop-down list of possible end intersections. */
    private JComboBox<String> endBox;

    /** Label that shows the current route's safety rating. */
    private JLabel mySafetyLabel;

    /** Text area that shows route details, hazards and errors. */
    private JTextArea logArea;

    /** Panel that draws the map and the highlighted route. */
    private final MapPanel mapPanel;

    /**
     * Creates the dashboard window for the given map and controller.
     *
     * @param theMap the city map to display
     * @param theController the controller that handles user actions
     */
    public PlannerDashboard(final MapGraph theMap, final PlannerController theController) {
        super("Self-Driving Car Route Planner");
        map = theMap;
        controller = theController;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setLayout(new BorderLayout(8, 8));

        mapPanel = new MapPanel();
        add(buildInputPanel(), BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(buildLogPanel(), BorderLayout.SOUTH);
        add(new SimulationPanel(controller), BorderLayout.EAST);

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
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(theEvent -> controller.handleSaveRoute());

        final JMenuItem loadItem = new JMenuItem("Load Route...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                InputEvent.CTRL_DOWN_MASK));
        loadItem.addActionListener(theEvent -> JOptionPane.showMessageDialog(this,
                "Loading saved routes will be added in the next version.",
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
                        + "Version: Iteration 4",
                "About", JOptionPane.INFORMATION_MESSAGE));

        final JMenuItem instructionsItem = new JMenuItem("Instructions");
        instructionsItem.addActionListener(theEvent -> JOptionPane.showMessageDialog(this,
                "1. Choose a start intersection.\n"
                        + "2. Choose an end intersection.\n"
                        + "3. Click \"Plan Route\".\n"
                        + "The route is highlighted in blue on the map and its\n"
                        + "details appear in the safety log at the bottom.\n\n"
                        + "Use the Simulation panel on the right to change the\n"
                        + "weather, block intersections or roads, and add traffic.\n"
                        + "The route is planned again after every change.\n\n"
                        + "Every route gets a safety check. Its rating appears\n"
                        + "next to the Plan Route button and hazards are listed\n"
                        + "in the safety log.\n\n"
                        + "Map colors:\n"
                        + "  Blue = your route\n"
                        + "  Orange = hazard (sharp turn, traffic)\n"
                        + "  Red = blocked\n"
                        + "  Dashed green = safer alternative route\n\n"
                        + "File > Save Route (Ctrl+S) saves the current route with\n"
                        + "its conditions, travel times and hazards.",
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
        final String[] idArray = controller.getIntersectionNames().toArray(new String[0]);

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

        mySafetyLabel = new JLabel(" ");
        mySafetyLabel.setFont(mySafetyLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(Box.createHorizontalStrut(20));
        panel.add(mySafetyLabel);
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
     * Handles the "Plan Route" button by passing the selected intersections
     * to the controller. The controller checks the input, computes the route
     * and calls back {@link #showRoute(List)} or {@link #showError(String)}.
     */
    private void onPlanRoute() {
        controller.planRoute((String) startBox.getSelectedItem(),
                (String) endBox.getSelectedItem());
    }

    /**
     * Highlights the given route on the map and writes its details
     * (each road, total distance and estimated time) to the log.
     * Travel time is adjusted for the current weather and includes any
     * traffic delay on each road.
     *
     * @param theRoute the ordered list of roads in the route
     */
    public void showRoute(final List<Road> theRoute) {
        mapPanel.setRoute(theRoute);

        double totalDistance = 0;
        final StringBuilder sb = new StringBuilder();
        sb.append("\nRoute from ").append(theRoute.get(0).getStart())
                .append(" to ").append(theRoute.get(theRoute.size() - 1).getEnd())
                .append(" (weather: ").append(controller.getWeather()).append("):\n");

        for (final Road road : theRoute) {
            totalDistance += road.getDistance();
            sb.append(String.format("  %s -> %s  %.1f mi @ %.0f mph%s%s%n",
                    road.getStart(), road.getEnd(), road.getDistance(),
                    road.getSpeedLimit(),
                    road.getTrafficDelay() > 0
                            ? String.format("  +%.0f min traffic",
                            road.getTrafficDelay() * MINUTES_PER_HOUR)
                            : "",
                    road.isBlocked() ? "  (BLOCKED)" : ""));
        }
        sb.append(String.format("Total: %.1f mi%n", totalDistance));
        final double normal = controller.getNormalTime(theRoute) * MINUTES_PER_HOUR;
        final double simulated = controller.getSimulatedTime(theRoute) * MINUTES_PER_HOUR;
        sb.append(String.format("Normal conditions: %.1f min | current conditions: "
                + "%.1f min (%+.1f min)%n", normal, simulated, simulated - normal));

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
     * Shows the result of a route's safety check: the rating next to the
     * Plan Route button, the hazards in the safety log and on the map, and
     * a safer alternative route if there is one.
     *
     * @param theReport the safety report from the controller
     */
    public void showSafetyReport(final SafetyReport theReport) {
        mySafetyLabel.setText("Safety: " + theReport.getRating()
                + " (" + theReport.getScore() + "/100)");
        mySafetyLabel.setForeground(ratingColor(theReport.getRating()));

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("Safety check: %d/100 (%s)%n",
                theReport.getScore(), theReport.getRating()));
        if (theReport.hasHazards()) {
            for (final String warning : theReport.getWarnings()) {
                sb.append("  ! ").append(warning).append('\n');
            }
        } else {
            sb.append("  No hazards found on this route.\n");
        }
        if (theReport.hasAlternative()) {
            final List<Road> alt = theReport.getAlternative();
            sb.append(String.format("  Safer alternative (%d/100): %s",
                    theReport.getAlternativeScore(), alt.get(0).getStart()));
            for (final Road road : alt) {
                sb.append(" -> ").append(road.getEnd());
            }
            sb.append("  (dashed green on the map)\n");
        }
        logArea.append(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());

        mapPanel.setSafety(theReport.getHazardIntersections(),
                theReport.getHazardRoadIds(), theReport.getAlternative());
    }

    /**
     * Returns the color used to show a safety rating.
     *
     * @param theRating the rating
     * @return green for SAFE, orange for MODERATE, red for UNSAFE
     */
    private static Color ratingColor(final SafetyRating theRating) {
        if (theRating == SafetyRating.SAFE) {
            return SAFE_COLOR;
        } else if (theRating == SafetyRating.MODERATE) {
            return HAZARD_COLOR;
        }
        return Color.RED;
    }

    /**
     * Records an error in the safety log without a pop-up. Used for errors
     * from automatic re-planning, so the user is not interrupted.
     *
     * @param theMessage the error message to record
     */
    public void logError(final String theMessage) {
        logArea.append("\nERROR: " + theMessage + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Records an information message in the safety log.
     *
     * @param theMessage the message to record
     */
    public void showMessage(final String theMessage) {
        logArea.append("\n" + theMessage + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Redraws the map, for example after a road or intersection is blocked.
     */
    public void refreshMap() {
        mapPanel.repaint();
    }

    /**
     * Removes the highlighted route, hazard markers and safety rating.
     */
    public void clearRoute() {
        mapPanel.setRoute(new ArrayList<>());
        mapPanel.setSafety(Set.of(), Set.of(), List.of());
        mySafetyLabel.setText(" ");
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

        /** Diameter of the orange ring around a hazard intersection, in pixels. */
        private static final int RING_SIZE = 30;

        /** Color used for the highlighted route. */
        private static final Color ROUTE_COLOR = new Color(30, 110, 230);

        /** The route currently highlighted, or an empty list. */
        private List<Road> myRoute = new ArrayList<>();

        /** Names of intersections marked as hazards. */
        private Set<String> myHazardIntersections = new HashSet<>();

        /** IDs of roads marked as hazards. */
        private Set<Integer> myHazardRoadIds = new HashSet<>();

        /** A safer alternative route drawn dashed, or an empty list. */
        private List<Road> myAlternative = new ArrayList<>();

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

        /**
         * Sets the hazards and the alternative route to show, and redraws.
         *
         * @param theIntersections names of intersections with hazards
         * @param theRoadIds IDs of roads with hazards
         * @param theAlternative a safer route to draw dashed, or an empty list
         */
        void setSafety(final Set<String> theIntersections, final Set<Integer> theRoadIds,
                       final List<Road> theAlternative) {
            myHazardIntersections = new HashSet<>(theIntersections);
            myHazardRoadIds = new HashSet<>(theRoadIds);
            myAlternative = new ArrayList<>(theAlternative);
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
            for (final Road road : map.getRoads()) {
                g2.setColor(road.isBlocked() ? Color.RED : Color.LIGHT_GRAY);
                drawRoad(g2, road, scale);
            }
            // Safer alternative route: dashed green, under the main route.
            g2.setColor(SAFE_COLOR);
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[] {12f, 8f}, 0f));
            for (final Road road : myAlternative) {
                drawRoad(g2, road, scale);
            }

            g2.setColor(ROUTE_COLOR);
            g2.setStroke(new BasicStroke(6));
            for (final Road road : myRoute) {
                drawRoad(g2, road, scale);
            }

            // Hazard roads: orange on top of the route.
            g2.setColor(HAZARD_COLOR);
            g2.setStroke(new BasicStroke(6));
            for (final Road road : myRoute) {
                if (myHazardRoadIds.contains(road.getId())) {
                    drawRoad(g2, road, scale);
                }
            }

            // Intersections: dark circles, red if blocked, with ID labels.
            // Hazard intersections get an orange ring.
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            for (final Intersection i : map.getIntersections()) {
                final int px = toPx(i.getX(), scale);
                final int py = toPx(i.getY(), scale);
                if (myHazardIntersections.contains(i.getId())) {
                    g2.setColor(HAZARD_COLOR);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawOval(px - RING_SIZE / 2, py - RING_SIZE / 2, RING_SIZE, RING_SIZE);
                }
                g2.setStroke(new BasicStroke(1));
                g2.setColor(i.isBlocked() ? Color.RED : Color.DARK_GRAY);
                g2.fillOval(px - NODE_SIZE / 2, py - NODE_SIZE / 2, NODE_SIZE, NODE_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawString(i.getId(), px + NODE_SIZE / 2 + 2, py - NODE_SIZE / 2);
            }

            drawLegend(g2);
        }

        /**
         * Draws a small color legend in the bottom-left corner of the map.
         *
         * @param theG2 the graphics context
         */
        private void drawLegend(final Graphics2D theG2) {
            final String[] labels = {"Route", "Hazard", "Blocked", "Safer route"};
            final Color[] colors = {ROUTE_COLOR, HAZARD_COLOR, Color.RED, SAFE_COLOR};
            theG2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            int x = PAD / 2;
            final int y = getHeight() - PAD / 3;
            for (int i = 0; i < labels.length; i++) {
                theG2.setColor(colors[i]);
                theG2.fillRect(x, y - 9, 14, 8);
                theG2.setColor(Color.DARK_GRAY);
                theG2.drawString(labels[i], x + 18, y);
                x += 18 + theG2.getFontMetrics().stringWidth(labels[i]) + 16;
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
            final Intersection start = controller.findIntersection(theRoad.getStart());
            final Intersection end = controller.findIntersection(theRoad.getEnd());
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