package model;
import java.util.ArrayList;
import java.util.List;

public final class Intersection {

    private final String myId;

    private final int myX;

    private final int myY;

    private boolean myBlocked;

    private List<Integer> myConnectedRoads;

    public Intersection(final String theId, final int theX, final int theY, final boolean theBlocked) {
        myId = theId;
        myX = theX;
        myY = theY;
        myBlocked = theBlocked;
        myConnectedRoads = new ArrayList<>();
    }

     public String getId() {
        return myId;
     }

     public int getX() {
        return myX;
     }

     public int getY() {
        return myY;
     }

     public boolean isBlocked() {
        return myBlocked;
     }

     public List<Integer> getConnectedRoads() {
        return myConnectedRoads;
     }

     public void setBlocked(final boolean theBlocked) {
        myBlocked = theBlocked;
     }

     public void setConnectedRoads(final List<Integer> theConnectedRoads) {
        myConnectedRoads = theConnectedRoads;
     }

     public void addRoad(final Integer theRoad) {
        myConnectedRoads.add(theRoad);
     }




}