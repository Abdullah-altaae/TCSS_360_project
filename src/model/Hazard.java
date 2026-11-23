package model;

import java.util.Objects;

public class Hazard {

    public static final int UNSAVED_ID = -1;

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

    private final int myId;

    private final int mySimId;

    private final HazardType myType;

    public Hazard(final HazardType theType) {
        this(UNSAVED_ID, UNSAVED_ID, theType);
    }

    public Hazard(final int theId, final int theSimId, final HazardType theType) {
        if (theId != UNSAVED_ID && theId < 1) {
            throw new IllegalArgumentException(
                    "hazard ID must be UNSAVED_ID or at least 1, was " + theId);
        }
        if (theSimId != UNSAVED_ID && theSimId < 1) {
            throw new IllegalArgumentException(
                    "simulation run ID must be UNSAVED_ID or at least 1, was " + theSimId);
        }
        myId = theId;
        mySimId = theSimId;
        myType = Objects.requireNonNull(theType, "hazard type must not be null");
    }

    public Hazard withIds(final int theId, final int theSimId) {
        return new Hazard(theId, theSimId, myType);
    }

    public int getId() {
        return myId;
    }

    public int getSimId() {
        return mySimId;
    }

    public HazardType getType() {
        return myType;
    }

    public int getPenalty() {
        return myType.getPenalty();
    }

    public boolean isSaved() {
        return myId != UNSAVED_ID;
    }
}
