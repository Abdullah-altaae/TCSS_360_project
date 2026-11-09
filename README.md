# TCSS_360_project

WHAT THE PROJECT DOES
A self-driving car route planner. A city map is stored as a graph of
intersections (nodes) connected by one-way roads (edges). The route
planner finds the quickest route between two intersections, using each
road's travel time and skipping blocked roads and intersections. The
environment simulator changes weather, traffic delay and blocked state,
and a Swing dashboard draws the map and highlights the planned route.


WHO OWNS WHICH CLASSES (from the @author tags)
- Abdullah Altaae: Road, RoutePlanner, RoadTest, RoutePlannerTest
- Allia Hussain: EnvironmentSimulator, SafetyChecker, SafetyRating
- Anjana Virani: CarPlannerApp, PlannerDashboard
- Felix Kiama: Intersection, MapGraph

Anjana Virani

WHAT I WORKED ON
- CarPlannerApp (controller): program entry point that builds a sample
  city map and opens the dashboard.
- PlannerDashboard (view): main GUI with File/Help menus, start/end
  route input, map drawing with route highlighting, and a safety log.
- Javadoc for both classes, YouTrack tickets and time logging, and
  committing my code to GitHub.

ISSUES I HAD
1. My classes were in a different package from my teammates' classes.
   I moved them into the controller and view packages and fixed the
   imports.
2. My code used the UML method names, but MapGraph and Road were
   written differently (for example, Road stores intersection IDs
   instead of Intersection objects). I updated my code to match.
   
3. This was my first time using YouTrack and Git in IntelliJ, so it
   took time to learn them.

Abdullah Altaae

Issues I Had
1. i found it difficult to see what direction that i needed to go. wasn't sure if i shou[...]
   worked on the unit code or not, i did what i needed to fullfil 10 hours of work.
2. there was some changes that needed to be done as we worked compared to the UML versio[...]
   if changes were not made.

Allia hussain

Week 1:
1. What I worked on:
EnvironmentSimulator (model): implemented environmental conditions, including clear, rainy,
and snowy weather, with associated risk factors. Implemented weather-adjusted road speeds and
travel time calculations. Implemented traffic delay updates for individual roads.
Created a basic structure for SafetyChecker and adding necessary methods and fields.

3. Issues I had:
I initially wasn't sure which class should manage traffic conditions and which class should
store traffic delays. I worked through the responsibilities and determined that EnvironmentSimulator
manages environmental changes while Road stores the numerical traffic delay. The Road and Intersection
classes were implemented differently than I initially expected. For example, MapGraph stores road IDs rather
than Road objects, which required me to reconsider how my simulator interacts with the map. This was my first
time using YouTrack and Git in IntelliJ, so it took time to learn how to manage tickets, log hours, commit changes,
and update my project.

Week 2:
1. What I worked on:
Implemented traffic intensity tracking for individual roads using a numerical scale from 0.0 to 1.0.
Implemented the entirety of SafetyChecker class to account for weather and traffic conditions.
Implemented environmental safety penalties based on weather and traffic intensity.
Implemented sharp-turn detection using vector calculations.

No such Issues this week!


Felix Kiama

WHAT I WORKED ON
- Intersection (model): a map node identified by name, with equals and
  hashCode based on the name only. Holds x/y coordinates, a blocked flag
  and the connected roads. Kept getId(), getX(), getY(), isBlocked() and
  setBlocked() because RoutePlanner, EnvironmentSimulator and
  PlannerDashboard use them.
- MapGraph (model): stores intersections, roads and an adjacency map.
  addRoad records a road under both endpoints and in both endpoint
  Intersections (a self-loop is recorded once). getRoute returns the
  roads leaving an intersection.
- Input checks: Added NullPointerException and IllegalArgumentException for input saftey.
- Added full Javadoc on both classes.

ISSUES I HAD
1. Road stores its start and end as intersection names (Strings), not
   Intersection objects, so MapGraph looks endpoints up by name.
2. MapGraph used to store intersections in a Map, while RoutePlanner and EnvironmentSimulator loop over getIntersections() as a list. I changed it to a List, which fixes those two classes but means CarPlannerApp and PlannerDashboard need small updates.


Abdullah Altaae 
what i worked on 
1 road 
- One-way road between two intersections with distance (miles), speed limit (mph), traffic delay (hours) and a blocked flag
- Input validation for IDs, endpoints, distance, speed and traffic delay
- Travel time at the speed limit or at a given (weather-adjusted) speed, including traffic delay
- `equals()` / `hashCode()` based on road ID, so roads work correctly as map keys and in lists
2 RoutePlanner
- Finds the quickest route between two intersections using Dijkstra's algorithm, weighted by travel time
- Skips blocked roads and blocked intersections
- Reads roads from the map on every call, so newly added roads, traffic and blockages are always used
- added Weather-aware routing
- added Alternative routes
- Route comparison
3 GeoProjection
- Converts latitude/longitude to x/y map coordinates using Web Mercator, the projection OpenStreetMap tiles use
- set (0, 0) is the top left corner of the map area
- `distanceMiles()` computes the distance between two coordinates (haversine formula)
4 OpenStreetMap research
- Researched the OSM data format (nodes, ways, tags), the Overpass API and tile usage rules
- Exported a downtown Seattle map (`.osm`) to use as the real-world map

ISSUES I HAD
i had trouble getting the API working i did most of the time looking at the API and trying to understand how it works
i worked on GeoProject but didnt create any test, will still need to be worked on next week to finish.