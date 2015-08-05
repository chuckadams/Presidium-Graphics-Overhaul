/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Conditions.*;



public class FirstAid extends Treatment {
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  
  public FirstAid(Actor actor, Actor patient) {
    this(actor, patient, null);
  }
  
  
  public FirstAid(Actor actor, Actor patient, Boarding refuge) {
    super(actor, patient, INJURY, null, refuge);
  }
  
  
  public FirstAid(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public Plan copyFor(Actor other) {
    sickbay = findRefuge(actor, sickbay);
    return new FirstAid(other, patient, sickbay);
  }
  
  
  
  /**  Targeting and priority evaluation.
    */
  final static Skill BASE_SKILLS[] = { ANATOMY, PHARMACY };
  final static Trait BASE_TRAITS[] = { EMPATHIC, DUTIFUL };
  
  
  protected float severity() {
    if (! patient.health.alive()) return 0.5f;
    float severity = patient.health.injuryLevel() * ActorHealth.MAX_INJURY;
    if (patient.health.bleeding()) severity += 0.5f;
    return severity;
  }
  
  
  private Boarding findRefuge(Actor actor, Target compare) {
    final Boarding current = patient.aboard();
    if (compare == null) compare = Retreat.nearestHaven(
      actor, PhysicianStation.class, false
    );
    final float rC = rateHaven(current), rT = rateHaven(compare);
    return (rC >= rT) ? current : (Boarding) compare;
  }
  
  
  private float rateHaven(Target t) {
    //  TODO:  Consider putting this in a Pick object, and passing that on to
    //  the retreat class.
    if (! (t instanceof Venue)) return -1;
    final Venue v = (Venue) t;
    if (v.staff().unoccupied()) return -1;
    if (v instanceof PhysicianStation) return 5;
    return 2;
  }
  
  
  
  protected float getPriority() {
    final boolean report =
       I.talkAbout == actor && ! patient.health.conscious() &&
       evalVerbose && hasBegun();
    if (report) {
      I.say("\n"+actor+" getting first aid priority for: "+patient);
      I.say("  Conscious? "+patient.health.conscious());
      I.say("  Organic?   "+patient.health.organic  ());
      I.say("  Bleeding?  "+patient.health.bleeding ());
      I.say("  Severity?  "+severity());
    }
    //
    //  First of all, we screen out things you can't do medicine to-
    setCompetence(0);
    if (! patient.health.organic()) return 0;
    //
    //  Then, we ensure the patient is physically accessible/won't wander off-
    sickbay = findRefuge(actor, sickbay);
    final Actor carries = Suspensor.carrying(patient);
    final boolean outside = patient.aboard() != sickbay;
    if (
      (carries != null && carries != actor) ||
      (outside && patient.health.conscious())
    ) {
      return -1;
    }
    //
    //  Then we determine if this is an actual emergency, and how severe the
    //  overall injury is.  (This is also used to limit the overall degree of
    //  team attention required.)
    final boolean urgent = patient.health.bleeding() || outside;
    float urgency = severity();
    if (urgency <= 0) return 0;
    if (! urgent) urgency /= 2;
    
    if (PlanUtils.competition(this, patient, actor) > urgency) {
      return -1;
    }
    toggleMotives(MOTIVE_EMERGENCY,
      (urgent || urgency > 0.5f) && patient.health.alive()
    );
    setCompetence(successChanceFor(actor));
    //
    //  And finally, overall priority is determined and returned...
    float priority = PlanUtils.supportPriority(
      actor, patient, motiveBonus(), competence(), urgency
    );
    if (report) {
      I.say("  Sickbay is:     "+sickbay     );
      I.say("  Emergency?      "+urgent      );
      I.say("  Urgency rated:  "+urgency     );
      I.say("  Competence:     "+competence());
      I.say("  Final priority: "+priority    );
    }
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    if (! patient.health.alive()) return 1;
    return tryTreatment(
      actor, patient,
      INJURY, PhysicianStation.EMERGENCY_ROOM,
      ANATOMY, PHARMACY, false
    );
  }
  
  
  public int motionType(Actor actor) {
    return isEmergency() ? MOTION_FAST : MOTION_ANY;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report =
      I.talkAbout == actor && ! patient.health.conscious() &&
      stepsVerbose && hasBegun();
    //
    //  You can't perform actual treatment while under fire, but you can get
    //  the patient out of harm's way (see below.)
    final boolean underFire = actor.senses.underAttack();
    if (report) {
      I.say("\n"+actor+" getting next first aid step for "+patient);
      I.say("  Under fire?     "+underFire       );
      I.say("  Sickbay:        "+sickbay         );
      I.say("  Patient aboard: "+patient.aboard());
    }
    
    if (patient.health.bleeding() && ! underFire) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Giving first aid to "
      );
      if (report) I.say("\n  Returning first aid action...");
      return aids;
    }

    sickbay = findRefuge(actor, sickbay);
    if (sickbay != null && patient.aboard() != sickbay) {
      final BringStretcher d = new BringStretcher(
        actor, patient, sickbay
      );
      if (d.nextStepFor(actor) != null) {
        if (report) I.say("  Returning new stretcher delivery...");
        return d;
      }
      else if (report) I.say("  Could not do stretcher delivery!");
    }
    
    if (underFire || Treatment.hasTreatment(INJURY, patient, hasBegun())) {
      if (report) I.say("\n  Under fire or already treated.");
      return null;
    }
    final Action aids = new Action(
      actor, patient,
      this, "actionFirstAid",
      Action.BUILD, "Applying bandages to "
    );
    if (report) I.say("\n  Returning second aid action...");
    return aids;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    return tryTreatment(
      actor, patient,
      INJURY, PhysicianStation.EMERGENCY_ROOM,
      ANATOMY, PHARMACY, true
    ) > 0;
  }
  
  
  public void applyPassiveItem(Actor carries, Item from) {
    if (! patient.health.alive()) {
      patient.health.setState(ActorHealth.STATE_SUSPEND);
    }
    
    float bonus = (5 + from.quality) / 10f;
    float effect = 1.0f / STANDARD_EFFECT_TIME;
    float regen = ActorHealth.INJURY_REGEN_PER_DAY;
    regen *= 3 * effect * patient.health.maxHealth() * bonus;
    patient.health.liftInjury(regen);
    
    carries.gear.removeItem(Item.withAmount(from, effect));
  }
  
  
  
  /**  Rendering and interface methods
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Giving First Aid to ")) {
      d.append(patient);
    }
  }
}









