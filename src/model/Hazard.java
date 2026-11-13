package model;

public class Hazard {

    public enum HazardType {
        BLOCKED_INTERSECTION(100),
        BLOCKED_ROAD(100),
        SHARP_TURN(15),
        RAIN_AND_TRAFFIC(15),
        HEAVY_TRAFFIC(10),
        SNOW(20);

        private final int myPenalty;

        HazardType(final int thePenalty) {
            myPenalty = thePenalty;
        }

        public int getPenalty() {
            return myPenalty;
        }
    }

    private final HazardType  myType;

    private final int myPenalty;

    public Hazard(final HazardType theType, final int thePenalty) {
        myType = theType;
        myPenalty = thePenalty;
    }
}
