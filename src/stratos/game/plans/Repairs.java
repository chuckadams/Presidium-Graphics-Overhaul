


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.PavingMap;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class Repairs extends Plan {
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
  final public static float
    TIME_PER_25_HP     = Stage.STANDARD_HOUR_LENGTH / 2,
    MIN_SERVICE_DAMAGE = 0.25f;
  
  final Installation built;
  
  
  public Repairs(Actor actor, Installation repaired) {
    super(actor, (Target) repaired, true);
    this.built = repaired;
  }
  
  
  public Repairs(Session s) throws Exception {
    super(s);
    built = (Installation) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(built);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Repairs(other, built);
  }
  
  
  
  /**  Assessing targets and priority-
    */
  public static float needForRepair(Installation built) {
    float needRepair;
    final Structure structure = built.structure();
    if (! structure.intact()) needRepair = 1.0f;
    else needRepair = (1 - structure.repairLevel()) * 1.5f;
    if (structure.burning()) needRepair += 1.0f;
    if (structure.needsUpgrade()) needRepair = Math.max(needRepair, 1);
    return needRepair;
  }
  
  
  public static Plan getNextRepairFor(Actor client, boolean asDuty) {
    final Stage world = client.world();
    final Choice choice = new Choice(client);
    final boolean report = evalVerbose && I.talkAbout == client;
    choice.isVerbose = report;
    //
    //  First, sample any nearby venues that require repair, and add them to
    //  the list.
    final Batch <Installation> toRepair = new Batch <Installation> ();
    world.presences.sampleFromMaps(
      client, world, 3, toRepair, Structure.DAMAGE_KEY
    );
    for (Installation near : toRepair) {
      if (near.base() != client.base()) continue;
      if (needForRepair(near) <= 0) continue;
      final Repairs b = new Repairs(client, near);
      if (asDuty) b.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(b);
      
      if (report) {
        I.say("\n  "+near+" needs repair?");
        I.say("  Need is: "+needForRepair(near));
        I.say("  Needs upgrade? "+near.structure().needsUpgrade());
      }
    }
    //
    //  Then, see if there are tiles which require paving nearby-
    final PavingMap p = client.base().paveRoutes.map;
    final Tile toPave = p.nextTileToPave(client, RoadsRepair.class);
    if (toPave != null) {
      final RoadsRepair r = new RoadsRepair(client, toPave);
      if (asDuty) r.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(r);
    }
    //
    //  Evaluate the most urgent task and return-
    if (report) I.say("\n  Total choices: "+choice.size());
    return (Plan) choice.pickMostUrgent();
  }
  

  /**  Target evaluation and prioritisation-
    */
  final static Trait BASE_TRAITS[] = { URBANE, ENERGETIC };
  final static Skill BASE_SKILLS[] = { ASSEMBLY, HARD_LABOUR };
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    float urgency = needForRepair(built);
    urgency *= actor.relations.valueFor(built);
    urgency *= (1 + actor.base().relations.communitySpirit()) / 2f;
    
    //  TODO:  Factor this in elsewhere, such as general citizen satisfaction
    //  ratings.
    /*
    if (! GameSettings.buildFree) {
      final float debt = 0 - built.base().credits();
      if (report) I.say("  Basic urgency: "+urgency+", debt level: "+debt);
      if (debt > 0 && urgency > 0) urgency -= debt / 500f;
    }
    //*/
    if (urgency <= 0) return 0;
    
    float competition = FULL_COMPETITION;
    competition /= 1 + (built.structure().maxIntegrity() / 100f);
    
    final float priority = priorityForActorWith(
      actor, (Target) built,
      ROUTINE * Visit.clamp(urgency, 0, 1), NO_MODIFIER,
      REAL_HELP, competition,
      BASE_SKILLS, BASE_TRAITS,
      NORMAL_DISTANCE_CHECK, MILD_FAIL_RISK,
      report
    );
    if (report) {
      I.say("  Repairing "+built+", base: "+built.base()+"?");
      I.say("  Intrinsic urgency: "+urgency);
      I.say("  PRIORITY:          "+(ROUTINE * Visit.clamp(urgency, 0, 1)));
      I.say("  Community spirit:  "+actor.base().relations.communitySpirit());
      I.say("  Competition:       "+competition);
      I.say("  Final priority:    "+priority);
    }
    return priority;
  }
  
  
  protected float successChance() {
    float chance = 1;
    //  TODO:  Base this on the conversion associated with the structure type.
    chance *= actor.skills.chance(HARD_LABOUR, 0);
    chance *= actor.skills.chance(ASSEMBLY   , 5);
    return (chance + 1) / 2;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true;
    if (built.structure().hasWear()) return false;
    if (built.structure().needsUpgrade()) return false;
    return true;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = eventsVerbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("\nGetting next build step?");
    final Structure structure = built.structure();
    final Element basis = (Element) built;
    
    if (structure.needsUpgrade() && structure.goodCondition()) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "+built
      );
      if (report) I.say("  Returning next upgrade action.");
      return upgrades;
    }
    
    if (structure.hasWear()) {
      final Action building = new Action(
        actor, built,
        this, "actionBuild",
        Action.BUILD, "Assembling "+built
      );
      if (! Spacing.adjacent(actor.origin(), basis) || Rand.num() < 0.2f) {
        final Tile t = Spacing.pickFreeTileAround(built, actor);
        if (t == null) {
          abortBehaviour();
          return null;
        }
        building.setMoveTarget(t);
      }
      else building.setMoveTarget(actor.origin());
      if (report) I.say("  Returning next build action.");
      return building;
    }
    
    if (report) I.say("NOTHING TO BUILD");
    return null;
  }
  
  
  public boolean actionBuild(Actor actor, Installation built) {
    final boolean report = eventsVerbose && I.talkAbout == actor && hasBegun();
    
    //  TODO:  Double the rate of repair again if you have proper tools and
    //  materials.
    final Structure structure = built.structure();
    final boolean salvage = structure.needsSalvage();
    final boolean free = GameSettings.buildFree;
    final Base base = built.base();
    
    float success = actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0;
    success *= 25f / TIME_PER_25_HP;
    
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  Require construction materials for full efficiency.
    if (salvage) {
      success *= actor.skills.test(ASSEMBLY, 5, 1) ? 1 : 0.5f;
      final float amount = structure.repairBy(0 - success);
      final float cost = amount * structure.buildCost();
      if (! free) base.incCredits(cost * 0.5f);
      if (report) I.say("Salvage sucess: "+success);
      if (report) I.say("Repair level: "+structure.repairLevel());
    }
    
    else {
      success *= actor.skills.test(ASSEMBLY, 10, 0.5f) ? 1 : 0.5f;
      success *= actor.skills.test(ASSEMBLY, 20, 0.5f) ? 2 : 1;
      final boolean intact = structure.intact();
      final float amount = structure.repairBy(success);
      final float cost = amount * structure.buildCost();
      if (! free) base.incCredits((0 - cost) * (intact ? 0.5f : 1));
    }
    return true;
  }
  
  
  public boolean actionUpgrade(Actor actor, Installation built) {
    final Structure structure = built.structure();
    final Upgrade upgrade = structure.upgradeInProgress();
    ///if (upgrade == null) I.say("NO UPGRADE!");
    if (upgrade == null) return false;
    ///I.say("Advancing upgrade: "+upgrade.name);
    int success = 1;
    success *= actor.skills.test(ASSEMBLY, 10, 0.5f) ? 2 : 1;
    success *= actor.skills.test(ASSEMBLY, 20, 0.5f) ? 2 : 1;
    final float amount = structure.advanceUpgrade(success * 1f / 100);
    final float cost = amount * upgrade.buildCost;
    built.base().incCredits((0 - cost));
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ");
    d.append(built);
  }
}







