package model;
import java.util.Map;
import java.util.List;

/**
 * Represents the city map as a graph. Intersections form the nodes of the graph and
 * roads form the edges that connect them.
 *
 * <p>This is the central model class for the application. The route planner traverses
 * the graph to compute a path between two points, the safety checker inspects it when
 * evaluating a route, and the environment simulator alters the conditions of the roads
 * it contains.</p>
 *
 * @author Felix Kiama
 * @version 31 October 2026
 */
public final class MapGraph {


    /** The intersections forming the nodes of this graph. */
    private final List<Intersection> myIntersections;

    /** The roads forming the edges of this graph. */
    private final List<Integer> myRoads;

    /** The roads reachable from each intersection. */
    private Map<Intersection, List<Integer>> myAdjacency;

    /**
     * Constructs a map graph from the supplied intersections and roads.
     *
     * <p>The supplied lists are retained by reference rather than copied, so subsequent
     * modifications made by the caller are reflected in this graph.</p>
     *
     * @param theIntersections the intersections forming the nodes of the graph
     * @param theRoads         the roads forming the edges of the graph
     */
    public MapGraph(final List<Intersection> theIntersections, final List<Integer> theRoads) {
        myIntersections = theIntersections;
        myRoads = theRoads;
    }

    /**
     * Returns the intersections contained in this graph.
     *
     * <p>This method returns a direct reference to the internal list rather than a
     * defensive copy. Modifications made to the returned list are reflected in this
     * graph.</p>
     *
     * @return the list of intersections in this graph
     */
    public List<Intersection> getIntersections() {
        return myIntersections;
    }

    /**
     * Returns the roads contained in this graph.
     *
     * <p>This method returns a direct reference to the internal list rather than a
     * defensive copy. Modifications made to the returned list are reflected in this
     * graph.</p>
     *
     * @return the list of roads in this graph
     */
    public List<Integer> getRoads() {
        return myRoads;
    }

    /**
     * Returns the number of intersections currently in this graph.
     *
     * @return the number of intersections
     */
    public int getIntersectionsSize() {
        return myIntersections.size();
    }

    /**
     * Returns the adjacency map, which associates each intersection with the roads
     * connected to it. The route planner relies on this structure to determine which
     * roads may be taken from a given intersection.
     *
     * <p>This class does not yet populate the adjacency map, so the method returns
     * {@code null} until the map has been initialized.</p>
     *
     * @return the adjacency map, or {@code null} if it has not been initialized
     */
    public Map<Intersection, List<Integer>> getAdjacency() {
        return myAdjacency;
    }

    /**
     * Determines whether this graph contains an intersection with the given identifier.
     * The search is performed as a linear scan over the intersection list.
     *
     * @param theName the identifier to search for
     * @return {@code true} if a matching intersection exists in this graph,
     *         {@code false} otherwise
     */
    public boolean containsIntersection(final String theName) {
        for (Intersection i : myIntersections) {
            if (i.getId().equals(theName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a road to this graph.
     *
     * <p>This method does not update the adjacency map; connectivity between
     * intersections must be established separately.</p>
     *
     * @param theRoad the road to add to the graph
     */
    public void addRoad(final int theRoad) {
        myRoads.add(theRoad);
    }

    /**
     * Adds an intersection to this graph.
     *
     * <p>This method does not update the adjacency map; connectivity between
     * intersections must be established separately.</p>
     *
     * @param theIntersection the intersection to add to the graph
     */
    public void addIntersection(final Intersection theIntersection) {
        myIntersections.add(theIntersection);
    }

    //Will add method for establishing and updating Adjacency






}