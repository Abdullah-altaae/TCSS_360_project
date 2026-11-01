package model;

/**
 * Simulates environmental conditions that may affect route planning,
 * including weather, traffic, and road obstacles. Manages environmental
 * conditions and calculate the adjusted travel time.
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
    private WeatherCondition myWeather;
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
     * Adds a road to the collection of blocked roads.
     *
     * @param theRoad the ID of the road to block
     */
//    public void blockRoad(String theRoad) { //change to "road theRoad" when road is implemented
//        myBlockedRoads.add(theRoad);
//    }

    /**
     * Removes a road from the collection of blocked roads.
     *
     * @param theRoad the ID of the road to unblock
     */
//    public void unBlockedRoad(String theRoad) { //change to "road theRoad" when road is implemented
//        myBlockedRoads.remove(theRoad);
//    }

    /**
     * Calculates the adjusted speed limit based on current weather conditions.
     *
     * @param theRoad the road whose speed limit will be adjusted
     * @return the adjusted speed limit
     */
//    public double calculateAdjustedSpeed(Road theRoad) {
//      double originalSpeed = theRoad.getSpeedLimit();
//      if (myWeather == WeatherCondition.CLEAR) {
//          return originalSpeed;
//      } else if (myWeather == WeatherCondition.RAINY) {
//          return originalSpeed * 0.80;
//      } else if (myWeather == WeatherCondition.SNOWY) {
//          return originalSpeed * 0.5;
//      }
//      return originalSpeed;
//    }

    /**
     * Calculates the adjusted travel time based on the current traffic condition.
     *
     * @param theTime the original estimated travel time
     * @return the adjusted travel time after accounting for traffic
     */
    public double calculateAdjustedTrafficTime(double theTime) {
        return 0.0;
    }

    //public void changeTraffic(Road theRoad, double theDelay) {}


}