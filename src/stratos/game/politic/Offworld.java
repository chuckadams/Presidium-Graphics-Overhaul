/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.util.*;



//  TODO:  Work this into a reference to all Sectors in the setting and their
//         current state (rather than a single amalgam for everyone offworld.)

//  TODO:  It's even possible this data should be written to a separate file
//         from the world itself, and loaded distinctly.

public class Offworld {
  
  
  /**  Data fields, construction, and save/load methods-
    */
  private static boolean
    transitVerbose = true ,
    updatesVerbose = true ,
    extraVerbose   = false;
  
  
  final public static int
    STAY_UPDATE_INTERVAL = Stage.STANDARD_HOUR_LENGTH * 2,
    TRAVEL_DURATION      = Stage.STANDARD_HOUR_LENGTH * 2;
  //  TODO:  Replace with destination-specific travel times...
  
  
  public static interface Activity extends Behaviour {
    //  TODO:  Pass along the sector/world/stage in question as arguments.
    void onWorldExit();
    void onWorldEntry();
    void whileOffworld();
    boolean doneOffworld();
  }
  
  static class Journey {
    //  TODO:  Include origin and destination sectors!
    Vehicle vessel;
    Mobile passengers[];
    float arriveTime;
  }

  
  //  TODO:  Use a table instead, to handle larger numbers?
  //  TODO:  Have journeys in and journeys out?
  final List <Journey> journeys = new List <Journey> ();
  final List <Mobile > staying  = new List <Mobile > ();
  final List <Mobile > comeBack = new List <Mobile > ();
  private int updateCounter = 0;
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Journey j = new Journey();
      j.vessel     = (Vehicle) s.loadObject();
      j.passengers = (Mobile[]) s.loadObjectArray(Mobile.class);
      j.arriveTime = s.loadFloat();
      journeys.add(j);
    }
    s.loadObjects(staying );
    s.loadObjects(comeBack);
    updateCounter = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(journeys.size());
    for (Journey j : journeys) {
      s.saveObject     (j.vessel    );
      s.saveObjectArray(j.passengers);
      s.saveFloat      (j.arriveTime);
    }
    s.saveObjects(staying );
    s.saveObjects(comeBack);
    s.saveInt(updateCounter);
  }
  
  
  
  /**  Query and update/simulation methods-
    */
  public static Activity activityFor(Mobile mobile) {
    if (mobile instanceof Actor) {
      final Behaviour root = ((Actor) mobile).mind.rootBehaviour();
      if (root instanceof Activity) return (Activity) root;
    }
    else if (mobile instanceof Vehicle) {
      return activityFor(((Vehicle) mobile).pilot());
    }
    return null;
  }
  
  
  public boolean hasMigrantsFor(Stage world) {
    return comeBack.size() > 0;
  }
  
  
  public void updateOffworldFrom(Stage world) {
    final boolean report = updatesVerbose;
    //
    //  TODO:  Have journeys in and journeys out?
    for (Journey j : journeys) if (j.arriveTime <= world.currentTime()) {
      if (report) I.say("\nJourney complete for "+j.vessel);
      journeys.remove(j);
      for (Mobile m : j.passengers) staying.addLast(m);
    }
    //
    //  The idea here is to iterate over 1/nth of the available migrants each
    //  update, where n is the stay-update-interval.  (This is to time-slice
    //  the computation load across multiple offworld settlements.)
    final int
      SUI  = STAY_UPDATE_INTERVAL,
      t    = ((int) world.currentTime()) % SUI,
      numS = staying.size();
    updateCounter += (((t + 1) * numS) / SUI) - ((t * numS) / SUI);
    if (report && (extraVerbose || updateCounter > 0)) {
      I.say("\nUpdating "+updateCounter+"/"+numS+" migrants...");
    }
    //
    //  Anyone staying gets updated, and if not finished with their business,
    //  added back to the queue.  Otherwise, they get added to the immigrant
    //  queue back into the world.
    for (; updateCounter > 0; updateCounter--) {
      if (world.schedule.timeUp()) break;
      
      final Mobile m = staying.removeFirst();
      boolean willStay = false;
      
      final Activity a = activityFor(m);
      if (a != null) {
        a.whileOffworld();
        if (! a.doneOffworld()) willStay = true;
      }
      
      if (willStay) staying.addLast(m);
      else comeBack.add(m);
      if (report) {
        I.say("\nUpdating "+m);
        I.say("  Current business: "+a);
        I.say("  Done offworld?    "+(! willStay));
      }
    }
  }
  
  
  
  /**  Exchange methods for interfacing with the world-
    */
  public void addMigrant(Mobile migrant, Stage world) {
    staying.addLast(migrant);
  }
  
  
  //  TODO:  You need to specify an origin & destination here.
  public void beginJourney(Dropship ship, float journeyTime) {
    final boolean report = transitVerbose;

    final Journey journey = new Journey();
    journey.vessel     = ship;
    journey.arriveTime = journeyTime + ship.world().currentTime();
    if (report) {
      I.say("\n"+ship+" undertaking journey.");
      I.say("  Transit time: "+journeyTime+", ETA: "+journey.arriveTime);
    }
    
    final List <Mobile> passengers = new List <Mobile> ();
    for (Mobile migrant : ship.inside()) {
      final Activity a = activityFor(migrant);
      if (migrant.inWorld()) migrant.exitWorld();
      //
      //  Migrants with no business offworld will simply be forgotten-
      //  otherwise, record their journey.
      if (a != null) {
        if (report) I.say("  "+migrant+" is passenger");
        a.onWorldExit();
        passengers.add(migrant);
        //
        //  (NOTE:  This a small workaround for the problem that all behaviours
        //  in an actor's agenda get wiped once that actor exits the world.)
        if (migrant instanceof Actor) {
          ((Actor) migrant).mind.assignBehaviour(a);
        }
      }
    }
    journey.passengers = passengers.toArray(Mobile.class);
    journeys.add(journey);
  }
  
  
  //  TODO:  You need to specify an origin & destination here.
  public void addPassengersTo(Dropship ship) {
    for (Mobile p : comeBack) {
      final Activity a = activityFor(p);
      if (a != null) a.onWorldEntry();
      comeBack.remove(p);
      ship.setInside(p, true);
      if (ship.inside().size() >= Dropship.MAX_PASSENGERS) break;
    }
  }
}





