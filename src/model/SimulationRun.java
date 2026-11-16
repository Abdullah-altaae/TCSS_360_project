package model;

import java.util.List;


public class SimulationRun {

    private final Route myRoute;

    private final double myAverageTraffic;

    private final EnvironmentSimulator.WeatherCondition myWeather;

    private final List<Hazard> myHazards;

    private int mySafteyRating;

    private final double myNormalTime;

    private final double mySimulatedTime;

    public  SimulationRun(final Route theRoute, final double theAverageTraffic, final  EnvironmentSimulator.WeatherCondition theWeather, final List<Hazard> theHazards, final int theSafteyRating, final double theNormalTime, final double theSimulatedTime) {
        myRoute = theRoute;
        myAverageTraffic = theAverageTraffic;
        myWeather = theWeather;
        myHazards =  List.copyOf(theHazards);
        mySafteyRating = theSafteyRating;
        myNormalTime = theNormalTime;
        mySimulatedTime = theSimulatedTime;
    }
}
