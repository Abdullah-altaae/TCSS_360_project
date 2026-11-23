package model;

import java.util.List;
import java.util.Objects;

public class SimulationRun {

    public static final int UNSAVED_ID = -1;

    private final int myId;

    private final Route myRoute;

    private final double myAverageTraffic;

    private final EnvironmentSimulator.WeatherCondition myWeather;

    private final List<Hazard> myHazards;

    private final int mySafetyRating;

    private final double myNormalTime;

    private final double mySimulatedTime;

    public SimulationRun(final Route theRoute, final double theAverageTraffic,
                         final EnvironmentSimulator.WeatherCondition theWeather,
                         final List<Hazard> theHazards, final int theSafetyRating,
                         final double theNormalTime, final double theSimulatedTime) {
        this(UNSAVED_ID, theRoute, theAverageTraffic, theWeather, theHazards,
                theSafetyRating, theNormalTime, theSimulatedTime);
    }

    public SimulationRun(final int theId, final Route theRoute, final double theAverageTraffic,
                         final EnvironmentSimulator.WeatherCondition theWeather,
                         final List<Hazard> theHazards, final int theSafetyRating,
                         final double theNormalTime, final double theSimulatedTime) {
        if (theId != UNSAVED_ID && theId < 1) {
            throw new IllegalArgumentException(
                    "simulation run ID must be UNSAVED_ID or at least 1, was " + theId);
        }
        myId = theId;
        myRoute = Objects.requireNonNull(theRoute, "route must not be null");
        myAverageTraffic = theAverageTraffic;
        myWeather = Objects.requireNonNull(theWeather, "weather must not be null");
        myHazards = List.copyOf(theHazards);
        mySafetyRating = theSafetyRating;
        myNormalTime = theNormalTime;
        mySimulatedTime = theSimulatedTime;
    }

    public SimulationRun withSavedData(final int theId, final Route theRoute,
                                       final List<Hazard> theHazards) {
        return new SimulationRun(theId, theRoute, myAverageTraffic, myWeather, theHazards,
                mySafetyRating, myNormalTime, mySimulatedTime);
    }

    public int getId() {
        return myId;
    }

    public Route getRoute() {
        return myRoute;
    }

    public double getAverageTraffic() {
        return myAverageTraffic;
    }

    public EnvironmentSimulator.WeatherCondition getWeather() {
        return myWeather;
    }

    public List<Hazard> getHazards() {
        return myHazards;
    }

    public int getSafetyRating() {
        return mySafetyRating;
    }

    public double getNormalTime() {
        return myNormalTime;
    }

    public double getSimulatedTime() {
        return mySimulatedTime;
    }

    public boolean isSaved() {
        return myId != UNSAVED_ID;
    }
}
