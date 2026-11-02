# TCSS_360_project
first Iteration
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

1. EnvironmentSimulator (model): implemented environmental conditions, including clear, rainy, and snowy weather, with associated risk factors.
Implemented weather-adjusted road speeds and travel time calculations.
Implemented traffic delay updates for individual roads.
Created a basic structure for SafetyChecker and adding necessary methods and fields.

2. Issues I had:
I initially wasn't sure which class should manage traffic conditions and which class should store traffic delays. I worked through the responsibilities and determined that EnvironmentSimulator manages environmental changes while Road stores the numerical traffic delay.
The Road and Intersection classes were implemented differently than I initially expected. For example, MapGraph stores road IDs rather than Road objects, which required me to reconsider how my simulator interacts with the map.
This was my first time using YouTrack and Git in IntelliJ, so it took time to learn how to manage tickets, log hours, commit changes, and update my project.