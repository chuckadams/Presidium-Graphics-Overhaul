


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;


//  TODO:  You can most likely remove the admin requirement, at least for
//  public bounties.


public class FindMission extends Plan {
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = true ;
  final Mission mission;
  
  
  
  public static FindMission attemptFor(Actor actor) {
    if (actor.mind.mission() != null) {
      return null;
    }
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    //  Find a mission that seems appealing at the moment (we disable culling
    //  of invalid plans, since missions might not have steps available until
    //  approved-)
    if (report) I.say("\nEvaluating missions...");
    final Choice choice = new Choice(actor) {
      protected boolean checkPlanValid(Behaviour b) {
        return true;
      }
    };
    for (Mission mission : actor.base().allMissions()) {
      final Venue HQ = missionHQ(actor, mission);
      final boolean open = mission.openToPublic();
      if (! open) continue;
      
      if (report) {
        I.say("\n  mission is: "+mission);
        I.say("  headquarters: "+HQ+", open to public? "+open);
        I.say("  priority: "+mission.priorityFor(actor));
        I.say("  next step: "+mission.nextStepFor(actor));
      }
      choice.add(mission);
    }
    final Mission picked = (Mission) choice.weightedPick();
    
    //  And try to apply for it-
    if (picked == null) return null;
    if (report) I.say("Mission picked: "+picked);
    return new FindMission(actor, picked);
  }
  

  private FindMission(Actor actor, Mission mission) {
    super(actor, mission.subject(), true);
    this.mission = mission;
    //this.admin = admin;
  }


  public FindMission(Session s) throws Exception {
    super(s);
    mission = (Mission) s.loadObject();
    //admin = (Venue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mission);
    //s.saveObject(admin);
  }
  
  
  public Plan copyFor(Actor other) {
    return new FindMission(other, mission);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    if (actor.mind.mission() == mission) return 0;
    return mission.priorityFor(actor);
  }
  
  
  private static Venue missionHQ(Actor actor, Mission mission) {
    final Actor client = mission.base().ruler();
    if (client == null) return null;
    
    final Object HQ = client.mind.home();
    if (HQ instanceof Venue) return (Venue) HQ;
    else return null;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next step for joining "+mission);
    
    if (! canStillApply()) {
      if (report) {
        I.say("  Cannot apply!");
        I.say("  Finished/Begun: "+mission.finished()+"/"+mission.hasBegun());
        I.say("  Has approval:   "+mission.isApproved(actor));
      }
      abortBehaviour();
      return null;
    }
    
    if (mission.isApproved(actor)) {
      if (report) I.say("  Joining now.");
      final Action joins = new Action(
        actor, actor,
        this, "actionJoins",
        Action.LOOK, "Joining mission"
      );
      return joins;
    }

    //  TODO:  This needs to be a generalised rally point.
    final Venue HQ = missionHQ(actor, mission);
    if (HQ == null && mission.missionType() != Mission.TYPE_PUBLIC) {
      if (report) I.say("  Nowhere to apply at!");
      return null;
    }
    
    if (actor.mind.mission() != mission) {
      if (report) I.say("  Applying at "+HQ);
      final Action applies = new Action(
        actor, HQ,
        this, "actionApplies",
        Action.LOOK, "Applying for mission"
      );
      return applies;
    }
    else {
      if (report) I.say("  Waiting for OK at "+HQ);
      final Action waitForOK = new Action(
        actor, HQ,
        this, "actionWait",
        Action.STAND, "Waiting for party"
      );
      return waitForOK;
    }
  }
  
  
  private boolean canStillApply() {
    if (mission.finished()) return false;
    if (mission.hasBegun() && ! mission.isApproved(actor)) return false;
    return true;
  }
  
  
  public boolean actionJoins(Actor client, Actor self) {
    
    I.say(actor+" joining mission: "+mission);
    
    if (! canStillApply()) return false;
    final boolean report = stepsVerbose && I.talkAbout == client;
    
    if (report) {
      I.say("\nJoining mission: "+mission);
      I.say("  Has begun?      "+mission.hasBegun());
      I.say("  Open to public? "+mission.openToPublic());
      I.say("  Applicants:     "+mission.totalApplied());
    }
    client.mind.assignMission(mission);
    client.mind.assignBehaviour(mission);
    return true;
  }
  
  
  public boolean actionApplies(Actor client, Venue admin) {
    if (! canStillApply()) return false;
    client.mind.assignMission(mission);
    return true;
  }
  
  
  public boolean actionWait(Actor client, Venue admin) {
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Joining mission: ");
    mission.describeBehaviour(d);
    //d.append(mission);
  }
}



