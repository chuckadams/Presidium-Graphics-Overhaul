

package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Allow spontaneous turn-coat behaviours for both actors and venues...


public class ContactMission extends Mission implements Economy {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float
    MAX_DURATION = World.STANDARD_DAY_LENGTH;
  
  final public static int
    OBJECT_FRIENDSHIP = 0,
    OBJECT_AUDIENCE   = 1,
    OBJECT_SUBMISSION = 2;
  final static String SETTING_DESC[] = {
    "Offer friendship to ",
    "Secure audience with ",
    "Demand submission from "
  };
  
  private static boolean 
    evalVerbose  = false,
    eventVerbose = true ;
  
  
  private Actor[] talksTo = null;  //Refreshed on request, at most once/second
  private List <Actor> agreed = new List <Actor> ();
  private boolean done = false;
  
  
  public ContactMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.CONTACT_MODEL,
      "Making Contact with "+subject
    ) ;
  }
  
  
  public ContactMission(Session s) throws Exception {
    super(s) ;
    s.loadObjects(agreed);
    done = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(agreed);
    s.saveBool(done);
  }
  
  
  
  /**  Behaviour implementation-
    */
  private Actor[] talksTo() {
    if (talksTo != null) return talksTo;
    final Batch <Actor> batch = new Batch <Actor> ();
    if (subject instanceof Actor) {
      final Actor a = (Actor) subject;
      batch.add(a);
    }
    else if (subject instanceof Venue) {
      final Venue v = (Venue) subject;
      for (Actor a : v.personnel.residents()) batch.include(a);
      for (Actor a : v.personnel.workers()  ) batch.include(a);
    }
    return talksTo = batch.toArray(Actor.class);
  }
  
  
  private boolean doneTalking(Actor a) {
    if (agreed.includes(a)) return true;
    return a.memories.relationNovelty(base) < 0;
  }
  
  
  public void updateMission() {
    super.updateMission();
    talksTo = null;
    
    final boolean report = eventVerbose && (
      ((List) approved()).includes(I.talkAbout) ||
      Visit.arrayIncludes(talksTo(), I.talkAbout) ||
      I.talkAbout == this
    );
    
    boolean allDone = roles.size() > 0;
    for (Actor a : talksTo()) {
      if (! doneTalking(a)) allDone = false;
    }
    
    if (allDone) {
      applyContactEffects(report);
      done = true;
    }
  }
  
  
  public float priorityFor(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    final float basePriority = basePriority(actor);
    if (report) {
      I.say("\nAssessing priority of contact mission");
      I.say("  Base priority: "+basePriority);
    }
    
    final Actor with[] = talksTo();
    float avg = 0;
    for (Actor other : with) {
      final Dialogue d = new Dialogue(actor, other, Dialogue.TYPE_CONTACT);
      d.setMotive(Plan.MOTIVE_MISSION, basePriority);
      final float otherP = d.priorityFor(actor);
      if (report) {
        I.say("  Priority of contact with: "+other+": "+otherP);
      }
      avg += otherP / with.length;
    }
    
    if (report) I.say("  FINAL PRIORITY: "+avg);
    return avg ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (! isActive()) return null;
    final Choice choice = new Choice(actor);
    
    for (Actor a : talksTo()) {
      final float novelty = a.memories.relationNovelty(actor);
      if (novelty <= 0) continue;
      final Dialogue d = new Dialogue(actor, a, Dialogue.TYPE_CONTACT);
      d.setMotive(Plan.MOTIVE_DUTY, novelty * ROUTINE);
      choice.add(d);
    }
    
    if (choice.size() == 0) for (Actor a : talksTo) {
      if (doneTalking(a)) continue;
      final float relation = a.memories.relationValue(actor);
      final Action closeTalks = new Action(
        actor, a,
        this, "actionCloseTalks",
        Action.TALK_LONG, "Closing talks"
      );
      closeTalks.setPriority(ROUTINE * (2 + relation) / 2f);
      choice.add(closeTalks);
    }
    
    if (choice.size() == 0) {
      final Element around = (Element) subject ;
      return Patrolling.aroundPerimeter(actor, around, actor.world()) ;
    }
    else return choice.pickMostUrgent();
  }
  
  
  public boolean finished() {
    return done;
  }
  
  
  
  //  TODO:  Partial success might net you an informant...
  public boolean actionCloseTalks(Actor actor, Actor other) {
    final boolean report = eventVerbose;// && I.talkAbout == actor;
    
    float DC = other.memories.relationValue(actor) * 10;
    if (objectIndex() == OBJECT_FRIENDSHIP) DC += 0 ;
    if (objectIndex() == OBJECT_AUDIENCE  ) DC += 10;
    if (objectIndex() == OBJECT_SUBMISSION) DC += 20;
    
    final float danger = CombatUtils.dangerAtSpot(other, other, null);
    DC -= danger * 5;
    
    final float novelty = other.memories.relationNovelty(actor.base());
    if (novelty < 0) DC += novelty * 10;
    float success = Dialogue.talkResult(SUASION, DC, actor, other);
    
    if (report) I.say("Success rating was: "+success+" with "+other);
    //  Failed efforts annoy the subject.
    other.memories.incRelation(base, 0, 0);
    if (success < 1) {
      other.memories.incRelation(actor, 0 - Relation.MAG_CHATTING, 0.1f);
      return false;
    }
    else {
      agreed.add(other);
      return true;
    }
  }
  
  
  private void applyContactEffects(boolean report) {
    final Actor ruler = base.ruler();
    final boolean majority = agreed.size() > (talksTo().length / 2);
    
    if (report) {
      I.say("\nCONTACT MISSION COMPLETE "+this);
      I.say("  Following have agreed to terms: ");
      for (Actor a : agreed) I.say("    "+a);
    }
    
    for (Actor other : agreed) {
      if (objectIndex() == OBJECT_FRIENDSHIP) {
        //  TODO:  Actually modify relations between the bases, depending on
        //  how successful you were.  (This has the added benefit of making
        //  spontaneous combat less likely.)
        
        other.memories.incRelation(base, Relation.MAG_CHATTING, 0.5f);
      }
      if (objectIndex() == OBJECT_AUDIENCE && ruler != null) {
        I.say("Issuing summons to: "+other);
        final Summons summons = new Summons(other, ruler);
        other.mind.assignBehaviour(summons);
      }
      if (objectIndex() == OBJECT_SUBMISSION) {
        other.memories.incRelation(base, Relation.MAG_HARMING, 0.5f);
        other.assignBase(base);
      }
    }
    
    if (subject instanceof Venue && majority) {
      if (objectIndex() == OBJECT_SUBMISSION) {
        ((Venue) subject).assignBase(base);
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected String[] objectiveDescriptions() {
    return SETTING_DESC;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On mission: ", this);
    d.append(SETTING_DESC[objectIndex()]);
    d.append(subject);
  }
}


