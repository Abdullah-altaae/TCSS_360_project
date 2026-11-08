package view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import controller.PlannerController;
import model.EnvironmentSimulator.WeatherCondition;
import model.Road;

/**
 * The simulation control panel shown on the side of the dashboard.
 * <p>
 * It lets the user change the weather, block or unblock intersections and
 * roads, set traffic delays and reset everything back to normal. Every
 * action is passed to the {@link PlannerController}; this panel does not
 * change the model itself.
 * </p>
 *
 * @author Anjana Virani
 * @version 7 November 2026
 */
public class SimulationPanel extends JPanel {

    /** Width of the panel in pixels. */
    private static final int PANEL_WIDTH = 250;

    /** The largest traffic delay the user can choose, in minutes. */
    private static final int MAX_TRAFFIC_MINUTES = 120;

    /** How much the traffic spinner changes per click, in minutes. */
    private static final int TRAFFIC_STEP_MINUTES = 5;

    /** The controller that handles the user's actions. */
    private final PlannerController myController;

    /** Drop-down of weather conditions. */
    private JComboBox<WeatherCondition> myWeatherBox;

    /** Drop-down of intersection names. */
    private JComboBox<String> myIntersectionBox;

    /** Drop-down of road labels, in the same order as myRoadIds. */
    private JComboBox<String> myRoadBox;

    /** The road ID for each entry in myRoadBox. */
    private final List<Integer> myRoadIds = new ArrayList<>();

    /** Spinner for the traffic delay, in minutes. */
    private JSpinner myTrafficSpinner;

    /**
     * Creates the simulation panel.
     *
     * @param theController the controller that handles the user's actions
     */
    public SimulationPanel(final PlannerController theController) {
        super();
        myController = theController;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Simulation"));
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));

        add(buildWeatherSection());
        add(buildIntersectionSection());
        add(buildRoadSection());
        add(Box.createVerticalStrut(8));
        add(buildResetButton());
        add(Box.createVerticalGlue());
    }

    /**
     * Builds the weather section with a drop-down of weather conditions.
     *
     * @return the weather section
     */
    private JPanel buildWeatherSection() {
        myWeatherBox = new JComboBox<>(WeatherCondition.values());
        myWeatherBox.setSelectedItem(myController.getWeather());
        myWeatherBox.addActionListener(theEvent -> myController.setWeather(
                (WeatherCondition) myWeatherBox.getSelectedItem()));

        final JPanel section = createSection("Weather");
        section.add(row(myWeatherBox));
        return section;
    }

    /**
     * Builds the intersection section with a drop-down of intersections and
     * Block and Unblock buttons.
     *
     * @return the intersection section
     */
    private JPanel buildIntersectionSection() {
        myIntersectionBox = new JComboBox<>(
                myController.getIntersectionNames().toArray(new String[0]));

        final JButton block = new JButton("Block");
        block.addActionListener(theEvent -> myController.setIntersectionBlocked(
                (String) myIntersectionBox.getSelectedItem(), true));

        final JButton unblock = new JButton("Unblock");
        unblock.addActionListener(theEvent -> myController.setIntersectionBlocked(
                (String) myIntersectionBox.getSelectedItem(), false));

        final JPanel section = createSection("Intersection");
        section.add(row(myIntersectionBox));
        section.add(row(block, unblock));
        return section;
    }

    /**
     * Builds the road section with a drop-down of roads, Block and Unblock
     * buttons, and a traffic delay setting.
     *
     * @return the road section
     */
    private JPanel buildRoadSection() {
        final List<String> labels = new ArrayList<>();
        for (final Road road : myController.getRoads()) {
            labels.add(road.getStart() + " -> " + road.getEnd());
            myRoadIds.add(road.getId());
        }
        myRoadBox = new JComboBox<>(labels.toArray(new String[0]));

        final JButton block = new JButton("Block");
        block.addActionListener(theEvent -> {
            if (hasSelectedRoad()) {
                myController.setRoadBlocked(selectedRoadId(), true);
            }
        });

        final JButton unblock = new JButton("Unblock");
        unblock.addActionListener(theEvent -> {
            if (hasSelectedRoad()) {
                myController.setRoadBlocked(selectedRoadId(), false);
            }
        });

        myTrafficSpinner = new JSpinner(
                new SpinnerNumberModel(0, 0, MAX_TRAFFIC_MINUTES, TRAFFIC_STEP_MINUTES));

        final JButton setTraffic = new JButton("Set");
        setTraffic.addActionListener(theEvent -> {
            if (hasSelectedRoad()) {
                myController.setTrafficDelay(selectedRoadId(),
                        ((Number) myTrafficSpinner.getValue()).doubleValue());
            }
        });

        final JPanel section = createSection("Road");
        section.add(row(myRoadBox));
        section.add(row(block, unblock));
        section.add(row(new JLabel("Traffic delay (min):")));
        section.add(row(myTrafficSpinner, setTraffic));
        return section;
    }

    /**
     * Builds the button that resets all conditions.
     *
     * @return the reset button
     */
    private JButton buildResetButton() {
        final JButton reset = new JButton("Reset All Conditions");
        reset.setAlignmentX(Component.LEFT_ALIGNMENT);
        reset.addActionListener(theEvent -> {
            myController.resetConditions();
            myWeatherBox.setSelectedItem(WeatherCondition.CLEAR);
            myTrafficSpinner.setValue(0);
        });
        return reset;
    }

    /**
     * Creates an empty titled section that stacks rows from top to bottom.
     *
     * @param theTitle the section title
     * @return the new section
     */
    private JPanel createSection(final String theTitle) {
        final JPanel section = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(theTitle));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    /**
     * Creates one left-aligned row holding the given components.
     *
     * @param theComponents the components to place in the row
     * @return the new row
     */
    private JPanel row(final Component... theComponents) {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (final Component component : theComponents) {
            row.add(component);
        }
        return row;
    }

    /**
     * Returns whether a road is selected in the road drop-down.
     *
     * @return true if a road is selected
     */
    private boolean hasSelectedRoad() {
        return myRoadBox.getSelectedIndex() >= 0;
    }

    /**
     * Returns the ID of the road selected in the road drop-down.
     *
     * @return the selected road's ID
     */
    private int selectedRoadId() {
        return myRoadIds.get(myRoadBox.getSelectedIndex());
    }
}