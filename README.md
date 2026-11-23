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

Week 3:
What I worked on:
- Added a FindHazards method to SafetyChecker that creates a list of hazards along a specific route.
  It uses hazards described in the Hazard class instead of creating hazards on its own (the way I previously had).
- Also added methods that accurately calculates the safety penalties by calling the methods from the Hazard class.
- Added a method that converts numerical safety ratings to descriptive rating for user to understand easily.
- Added tests for EnvironmentSimulator class. 



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

Week 2/Iteration 2

WHAT I WORKED ON
- Intersection and MapGraph (model): met with the team to plan how both
  classes should change to act as true model classes. Updated the input
  exceptions and Javadoc on both. addRoad in MapGraph no longer returns a
  list, and I improved the error checking in the class.
- RouteDatabase (model): researched options and chose SQLite, then set it
  up in the IntelliJ project. Built a 3-table structure (routes,
  simulation_runs, hazards) with methods to insert, update, load and
  delete data. Each simulation run belongs to a route, and each hazard
  belongs to a simulation run.
- DataBaseSandBox: a mock driver to test the database. Used DB Browser
  to check inserting, updating, reading and deleting, and that exceptions
  are thrown and handled correctly.
- Route (model): proposed Route, SimulationRun and Hazard classes to the
  team as a better way to organize our results, and planned their fields
  and methods. Implemented Route, which stores a route's ID, start, end,
  roads in travel order, total distance and a string of its road IDs.
  Added placeholder code for pending features and connected it to the
  database to generate IDs.
- Added full Javadoc on the database class, the driver and Route.

ISSUES I HAD
1. I had two GitHub accounts set up on my computer, and they got mixed
   up. Git kept logging in with the account that didn't have access to
   the repo, so it wouldn't let me push my work until I fixed it.
2. I struggled to find the right place for the insert calls for routes
   and the other saved objects. The database generates the IDs, so the
   insert has to happen somewhere that can pass the generated IDs to the
   Route, SimulationRun and Hazard constructors.

Week 3/Iteration 3

WHAT I WORKED ON
- RouteDatabase (model): set up the database to store SimulationRun and
  Hazard objects, with foreign keys to the Route each one belongs to.
  Added and updated the columns in the simulation_runs and hazards
  tables to hold the new data in the model classes, then updated the
  save and load methods.
- Route and SimulationRun (model): refactored both classes so a new
  route or run can be a temporary object with an ID of -1 until the
  user chooses to save it. Added a secondary constructor, helper
  methods and ID checks, so saving takes the data from the temporary
  object and makes a new permanent object with the ID the database
  generates. Worked with the teammate who set up the environment
  simulator to improve SimulationRun.
- Hazard (model): added enums for the different types of hazards, each
  with a penalty value used to rate a route, and used them when Hazard
  objects are created.
- PlannerController and CarPlannerApp (controller): added logic to
  handle requests to save generated routes, with helper methods that
  build the data needed to create Route objects. Updated both classes
  to support saving temporary routes, and planned how to finish
  connecting SimulationRun and Route in the controller so the view can
  use them.
- Updated the Javadoc on Hazard and the whole database class.

ISSUES I HAD
1. Keeping the Route, SimulationRun and Hazard classes, the database
   class and the controller classes in sync was tricky. The data, the
   types of data and the parameters kept changing as updates were made
   throughout the week and as the team shared what they needed.




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

Week 3/Iteration 3

Week 3 :
Anjana Virani

WHAT I WORKED ON
- Hazard display: safety rating next to Plan Route, hazards listed in the
  safety log and marked in orange on the map.
- Safer alternative route shown as a dashed green line.
- Updated CarPlannerApp and PlannerController for the new RoutePlanner
  and SafetyChecker.
- JUnit tests for the safety features 

ISSUES I HAD
1. Hazard does not store its location, so I used SafetyChecker's helper
   methods to find where hazards are.
2. Merge conflicts and out-of-date files when merging my branch into main.
3. RoutePlannerTest still used the old RoutePlanner constructor, which
   stopped all tests from compiling. I excluded it locally to run my
   tests and told the author.

Abdullah Altaae
what i worked on

1. road
i changed some of the code in road so it can bends between start and end intersection, so the map can draw the curved
area instead of just straight line.

2. Loads a real city map from an OpenStreetMap file so that the planner can work. it uses real streets
i added good amount of the code however i didnt really add javaDoc. i want able to finish the implementation
but it should be able to be ran.
what is included in the OsmMapLoader
- parse to read the file one tag at a time instead of loading the whole 14 MB file as a tree
- Filtering it keeps a way only if its a highway type and avoiding sidewalks
- Building the graph. doing so by clip each way where it leaves the bounds box, so nothing draws off-screen
- cut into roads walk each way, adding up distance with GeoProjection distance miles.
- create objects by converting lat/lon to x/y.

ISSUES I HAD
i was mostly looking into how this should be done, i spent a good amount of time, reading and
watching how this is going to work. i have an understanding of where thing should be in the future. but i
still need time to do the work required.


Week 4/Iteration 4

Anjana Virani
What i worked on
- File > Save Route (Ctrl+S): saves the current route to the SQLite database with a simulation run 
  (weather, traffic, safety score, normal and simulated travel times) and the hazards found.
- Normal vs simulated travel time shown in the safety log for every route.
- Fixes: traffic hazards now appear on the map, no pop-up on automatic re-plans, and a clear message when
  the start or end is blocked.
- JUnit tests for saving and the fixes (58 tests passing in PlannerControllerTest).

ISSUES I HAD
1. My branch was out of date with main, and IntelliJ settings files (.iml) blocked switching branches and pulling.
2. SQLite driver was not set up on my computer, so saving failed until I added libraries.
3. The database file in the repo used an old table layout, so I deleted it and let the program create a new one.
