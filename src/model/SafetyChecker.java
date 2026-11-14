package model;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks the safety of routes by identifying potential hazards,
 * blocked intersections, blocked roads, sharp turns,
 * and environmental risks.
 * Calculates a numerical safety rating for routes.
 *
 * @author Allia Hussain
 * @version 14 November 2026
 */
public class SafetyChecker {
    /** The environment simulator used to access current weather and traffic conditions. */
    private final EnvironmentSimulator myEnvironmentSimulator;

    /** The map graph containing the intersections and roads in the simulation. */
    private final MapGraph myMapGraph;

    /**
     * Constructs a SafetyChecker using the provided EnvironmentSimulator and mapGraph.
     *
     * @param theEn the environment simulator used to access environmental conditions
     * @param theGraph the map graph containing intersections and roads
     */
    public SafetyChecker(EnvironmentSimulator theEn, MapGraph theGraph) {
        myEnvironmentSimulator = theEn;
        myMapGraph = theGraph;
    }

    /**
     * Finds all blocked intersections within a given list of intersections.
     *
     * @param theIntersections the existing intersections
     * @return a list containing all blocked intersections
     */
    public List<Intersection> findBlockedIntersections(List<Intersection> theIntersections) {
        List<Intersection> blocked = new ArrayList<>();
        for (Intersection intersection : theIntersections) {
            if (intersection.isBlocked()) {
                blocked.add(intersection);
            }
        }
        return blocked;
    }
    /**
     * Finds all blocked roads within a given route.
     *
     * @param theRoads the roads to check
     * @return a list containing all blocked roads
     */
    public List<Road> findBlockedRoads(List<Road> theRoads) {
        List<Road> blocked = new ArrayList<>();
        for (Road road : theRoads) {
            if (road.isBlocked()) {
                blocked.add(road);
            }
        }
        return blocked;
    }

    /**
     * Collects all the intersections in a specified route in order.
     *
     * @param theRoads the roads making up the route
     * @return an ordered list of intersections along the route
     */
    public List<Intersection> findRouteIntersections(List<Road> theRoads) {
        List<Intersection> intersections = new ArrayList<>();
        if (theRoads.isEmpty()) {
            return intersections;
        }
        Road firstRoad = theRoads.get(0);
        String name = firstRoad.getStart();

        for (Intersection intersection : myMapGraph.getIntersections()) {
            if (intersection.getName().equals(name)) {
                intersections.add(intersection);
            }
        }

        for (Road road : theRoads) {
            String endName = road.getEnd();

            for (Intersection intersection : myMapGraph.getIntersections()) {
                if (intersection.getName().equals(endName)) {
                    intersections.add(intersection);
                }
            }
        }
        return intersections;
    }

    /**
     * Identifies potentially dangerous sharp turns along a route.
     *
     * @param theIntersections intersections along the route
     * @return a list of intersections where potentially dangerous turns occur
     */
    public List<Intersection> findSharpTurns(List<Intersection> theIntersections) {
        List<Intersection> turns = new ArrayList<>();
        for (int i = 1; i < theIntersections.size() - 1; i++) {
            Intersection previous = theIntersections.get(i - 1);
            Intersection current = theIntersections.get(i);
            Intersection next = theIntersections.get(i + 1);

            double vector1X = current.getX() - previous.getX();
            double vector1Y = current.getY() - previous.getY();
            double vector2X = next.getX() - current.getX();
            double vector2Y = next.getY() - current.getY();

            double dotProduct = (vector1X * vector2X) + (vector1Y * vector2Y);
            double magnitude = Math.sqrt(Math.pow(vector1X, 2) + Math.pow(vector1Y, 2));
            double magnitude2 = Math.sqrt(Math.pow(vector2X, 2) + Math.pow(vector2Y, 2));

            if (magnitude != 0 && magnitude2 != 0) {
                double cos = dotProduct / (magnitude * magnitude2);
                cos = Math.max(-1.0, Math.min(1.0, cos));
                double angle = Math.toDegrees(Math.acos(cos));

                if (angle >= 45) {
                    turns.add(current);
                }
            }
        }
        return turns;
    }

    /**
     * Finds all safety hazards present along the given route.
     *
     * @param theRoute the list of roads that make up the route
     * @return a list of hazards found along the route
     */
    public List<Hazard> findHazards(List<Road> theRoute) {
        List<Hazard> hazards = new ArrayList<>();
        if (theRoute.isEmpty()) {
            return hazards;
        }

        List<Intersection> routeIntersections = findRouteIntersections(theRoute);

        List<Intersection> sharpTurns = findSharpTurns(routeIntersections);

        for (Intersection intersection : sharpTurns) {
            hazards.add(new Hazard(Hazard.HazardType.SHARP_TURN));
        }
        List<Intersection> blockedIntersections =
                findBlockedIntersections(routeIntersections);

        for (Intersection intersection : blockedIntersections) {
            hazards.add(new Hazard(Hazard.HazardType.BLOCKED_INTERSECTION));
        }
        List<Road> blockedRoads = findBlockedRoads(theRoute);
        for (Road road : blockedRoads) {
            hazards.add(new Hazard(Hazard.HazardType.BLOCKED_ROAD));
        }
        if (myEnvironmentSimulator.getWeather()
                == EnvironmentSimulator.WeatherCondition.SNOWY) {
            hazards.add(new Hazard(Hazard.HazardType.SNOW));
        }
        for (Road road : theRoute) {
            double intensity = myEnvironmentSimulator.getTrafficIntensity(road);
            if (myEnvironmentSimulator.getWeather()
                    == EnvironmentSimulator.WeatherCondition.RAINY && intensity >= 0.5) {
                hazards.add(new Hazard(Hazard.HazardType.RAIN_AND_TRAFFIC));
            } else if (intensity >= 0.7) {
                hazards.add(new Hazard(Hazard.HazardType.HEAVY_TRAFFIC));
            }
        }
        return hazards;
    }

    /**
     * Evaluates the safety of a route by checking for blocked roads and blocked intersections.
     *
     * @param theRoute the route to evaluate
     * @return true if the route is safe, false otherwise
     */
    public boolean evaluateRoute(List<Road> theRoute) {
        if (theRoute.isEmpty()) {
            return false;
        }

        boolean isSafe = true;

        List<Road> blockedRoads = findBlockedRoads(theRoute);
        List<Intersection> routeIntersections = findRouteIntersections(theRoute);
        List<Intersection> blockedIntersections = findBlockedIntersections(routeIntersections);

        if (!blockedRoads.isEmpty()) {
            isSafe = false;
        }
        if (!blockedIntersections.isEmpty()) {
            isSafe = false;
        }

        return isSafe;
    }

    /**
     * Calculates a numerical safety rating for the specified route.
     * The rating starts at 100 and decreases based on sharp turns
     * and environmental risks. Each sharp turn deducts 15 points.
     * Routes containing blocked roads or intersections receive a
     * rating of 0.
     *
     * @param theRoute the route being evaluated
     * @return the safety rating between 0 and 100
     */
    public int calculateSafetyRating(List<Road> theRoute) {
        if (theRoute.isEmpty()) {
            return 0;
        }
        int safetyRating = 100;

        List<Hazard> hazards = findHazards(theRoute);

        for (Hazard hazard : hazards) {
            if (hazard.getType() == Hazard.HazardType.BLOCKED_ROAD
                    || hazard.getType() == Hazard.HazardType.BLOCKED_INTERSECTION) {
                return 0;
            }
            safetyRating -= hazard.getPenalty();
        }

        safetyRating = Math.max(0, safetyRating);
        return safetyRating;
    }

    /**
     * Converts the numerical safety score into a descriptive safety rating.
     *
     * @param theRoute the route being evaluated
     * @return the enum SAFE, MODERATE, or UNSAFE
     */
    public SafetyRating getSafetyRating(List<Road> theRoute) {
        int rating = calculateSafetyRating(theRoute);

        if (rating >= 80) {
            return SafetyRating.SAFE;
        } else if (rating >= 50) {
            return SafetyRating.MODERATE;
        } else {
            return SafetyRating.UNSAFE;
        }
    }
}
