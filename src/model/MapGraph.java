package model;
import java.util.Map;
import java.util.List;

public final class MapGraph {

    private final List<Intersection> myIntersections;

    private final List<Integer> myRoads;

    private Map<Intersection, List<Integer>> myAdjacency;

    public MapGraph(final List<Intersection> theIntersections, final List<Integer> theRoads) {
        myIntersections = theIntersections;
        myRoads = theRoads;
    }

    public List<Intersection> getIntersections() {
        return myIntersections;
    }

    public List<Integer> getRoads() {
        return myRoads;
    }

    public int getIntersectionsSize() {
        return myIntersections.size();
    }

    public Map<Intersection, List<Integer>> getAdjacency() {
        return myAdjacency;
    }

    public boolean containsIntersection(final String theName) {
        for (Intersection i : myIntersections) {
            if (i.getId().equals(theName)) {
                return true;
            }
        }
        return false;
    }


    public void addRoad(final int theRoad) {
        myRoads.add(theRoad);
    }

    public void addIntersection(final Intersection theIntersection) {
        myIntersections.add(theIntersection);
    }

    //Will add method for establishing and updating Adjacency






}