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

    public Hazard(final HazardType theType) {
        myType = theType;
    }
    public HazardType getType() {
        return myType;
    }
    public int getPenalty() {
        return myType.getPenalty();
    }
}
