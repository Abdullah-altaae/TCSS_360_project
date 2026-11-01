package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates environmental conditions that may affect route planning,
 * including weather, traffic, and road obstacles. Manages environmental
 * conditions and calculates the adjusted travel time.
 *
 * @author Allia Hussain
 * @version 31 October 2026
 */
public class EnvironmentSimulator {
    public enum WeatherCondition {
        CLEAR(0.0),
        RAINY(0.2),
        SNOWY(0.6);

        private final double myRiskFactor;

        WeatherCondition(final double theRisk) {
            myRiskFactor = theRisk;
        }
        public double getRiskFactor() {
            return myRiskFactor;
        }
    }
    /** The current weather condition in the simulation. */
    private WeatherCondition myWeather;

    /** The map graph containing the intersections and roads in the simulation. */
    private final MapGraph myMapGraph;

    /**
     * Constructs an EnvironmentSimulator with clear weather,
     * normal traffic, and no blocked roads.
     *
     * @param theGraph the graph containing roads and intersections.
     */
    public EnvironmentSimulator(MapGraph theGraph) {
        myMapGraph = theGraph;
        myWeather = WeatherCondition.CLEAR;
    }
    /**
     * Returns the current weather condition.
     *
     * @return the current weather condition
     */
    public WeatherCondition getWeather() {
        return myWeather;
    }

    /**
     * Changes the current weather condition.
     *
     * @param theChange the new weather condition
     */
    public void setWeather(WeatherCondition theChange) {
        myWeather = theChange;
    }

    /**
     * Blocks the specified road.
     *
     * @param theRoad the road to block
     */
    public void blockRoad(Road theRoad) {
        theRoad.setBlocked(true);
    }

    /**
     * Unblocks the specified road.
     *
     * @param theRoad the road to unblock
     */
    public void unBlockedRoad(Road theRoad) {
        theRoad.setBlocked(false);
    }

    /**
     * Updates the traffic delay for the specified road.
     *
     * @param theRoad the road whose traffic delay is being changed
     * @param theDelay the new traffic delay in hours
     */
    public void changeTraffic(Road theRoad, double theDelay) {
        theRoad.setTrafficDelay(theDelay);
    }

    /**
     * Calculates the adjusted speed limit based on current weather conditions.
     *
     * @param theRoad the road whose speed limit will be adjusted
     * @return the adjusted speed limit
     */
    public double calculateAdjustedSpeed(Road theRoad) {
      double originalSpeed = theRoad.getSpeedLimit();
      if (myWeather == WeatherCondition.CLEAR) {
          return originalSpeed;
      } else if (myWeather == WeatherCondition.RAINY) {
          return originalSpeed * 0.80;
      } else if (myWeather == WeatherCondition.SNOWY) {
          return originalSpeed * 0.5;
      }
      return originalSpeed;
   }

    /**
     * Calculates the adjusted travel time based on the current weather
     * and traffic conditions.
     *
     * @param theRoad the road whose adjusted travel time is being calculated
     * @return the adjusted travel time after accounting for weather and traffic
     */
    public double calculateAdjustedTravelTime(Road theRoad) {
        double adjustedSpeed = calculateAdjustedSpeed(theRoad);
        return theRoad.getTravelTime(adjustedSpeed);
    }

    /**
     * Blocks the specified intersection.
     *
     * @param theIntersection the intersection to block
     */
    public void blockIntersection(Intersection theIntersection) {
        theIntersection.setBlocked(true);
    }

    /**
     * Unblocks the specified intersection.
     *
     * @param theIntersection the intersection to unblock
     */
    public void unblockIntersection(Intersection theIntersection) {
        theIntersection.setBlocked(false);
    }

    /**
     * Retrieves all currently blocked intersections in the map graph.
     *
     * @return a list of intersections that are currently blocked
     */
    public List<Intersection> getBlockedIntersections() {
        List<Intersection> list = new ArrayList<>();
        for (Intersection intersection : myMapGraph.getIntersections()) {
            if(intersection.isBlocked()) {
                list.add(intersection);
            }
        }
        return list;
    }
}