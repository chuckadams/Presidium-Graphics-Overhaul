


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Retreat extends Plan implements Qualities {
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f;  //TODO:  use a time-limit instead?
  
  private static boolean
    evalVerbose  = false,
    havenVerbose = false,
    stepsVerbose = false;
  
  
  private float maxDanger = 0;
  private Boarding safePoint = null;
  
  
  public Retreat(Actor actor) {
    this(actor, actor.senses.haven());
  }
  
  
  public Retreat(Actor actor, Boarding safePoint) {
    super(actor, actor, false, NO_HARM);
    this.safePoint = safePoint;
  }


  public Retreat(Session s) throws Exception {
    super(s);
    this.maxDanger = s.loadFloat();
    this.safePoint = (Boarding) s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(maxDanger);
    s.saveTarget(safePoint);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Retreat(other, safePoint);
  }
  
  
  
  /**  Evaluation of priority and targets--
    */
  final Skill BASE_SKILLS[] = { ATHLETICS, STEALTH_AND_COVER };
  final Trait BASE_TRAITS[] = { NERVOUS, HUMBLE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (safePoint == null) return 0;
    
    final boolean emergency = actor.senses.isEmergency();
    
    float danger = Nums.max(
      actor.senses.fearLevel(), Plan.dangerPenalty(actor, actor)
    ) + actor.health.injuryLevel();
    float bonus = 0;
    
    //  Retreat becomes less attractive as you get closer to home and more
    //  exhausted.
    final Target haven = safePoint;
    final float homeBonus = CombatUtils.homeDefenceBonus(actor, actor);
    if (emergency) {
      bonus += PARAMOUNT - homeBonus;
      bonus -= actor.health.fatigueLevel() * PARAMOUNT;
      maxDanger = Nums.max(danger, maxDanger);
    }
    else {
      if (actor.aboard() == safePoint) return 0;
      maxDanger = danger;
      bonus += (haven == null) ? 0 : Plan.rangePenalty(haven, actor) * CASUAL;
    }
    
    final float priority = priorityForActorWith(
      actor, safePoint,
      maxDanger * PARAMOUNT, bonus,
      NO_HARM, NO_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NO_DISTANCE_CHECK,
      report
    );
    
    if (report) {
      I.say("\n  PLAN ID IS: "+hashCode());
      I.say("  Max Danger:     "+maxDanger);
      I.say("  Fear Level:     "+actor.senses.fearLevel());
      I.say("  Injury:         "+actor.health.injuryLevel());
      I.say("  Fatigue:        "+actor.health.fatigueLevel());
      I.say("  Bonus priority: "+bonus);
      I.say("  Endangered?     "+actor.senses.isEmergency());
    }
    return priority;
  }
  
  
  //  TODO:  Get rid of this if you can.  It's a little flaky.
  
  public static Boarding nearestHaven(
    final Actor actor, Class prefClass, final boolean emergency
  ) {
    //  TODO:  Use baseSpeed here instead?
    final float runRange = actor.health.sightRange() + Stage.SECTOR_SIZE;
    
    final Pick <Boarding> pick = new Pick <Boarding> () {
      
      public void compare(Boarding next, float rating) {
        if (next == null || ! next.allowsEntry(actor)) return;
        //  TODO:  Add some random salt here?
        if (PathSearch.blockedBy(next, actor)) return;
        final float dist = Spacing.distance(actor, next) / runRange;
        super.compare(next, rating - (dist * (emergency ? 5 : 2)));
      }
    };
    
    pick.compare(actor.senses.haven(), 5);
    pick.compare(actor.mind.home(), 10);
    pick.compare(actor.mind.work(), 5 );
    
    final Tile ground = emergency ? (Tile) pickHidePoint(
      actor, runRange,
      actor, false
    ) : null;
    pick.compare(ground, 0);
    
    final Presences presences = actor.world().presences;
    final Target refuge = presences.nearestMatch(
      Economy.SERVICE_REFUGE, actor, -1
    );
    final Target pref   = presences.nearestMatch(
      prefClass             , actor, -1
    );
    final Target cover  = presences.nearestMatch(
      Venue.class           , actor, -1
    );
    pick.compare((Boarding) refuge, emergency ? 5 : 10);
    pick.compare((Boarding) pref  , 10                );
    pick.compare((Boarding) cover , emergency ? 1 : 2 );
    
    return pick.result();
  }
  
  
  
  /**  Picks a nearby tile for the actor to temporarily withdraw to (as opposed
    *  to a full-blown long-distance retreat.)  Used to perform hit-and-run
    *  tactics, stealth while travelling, or an emergency hide.
    */
  public static Target pickHidePoint(
    final Actor actor, float range, Target from, final boolean advance
  ) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    if (report) I.say("\nPICKING POINT OF WITHDRAWAL FROM "+actor.origin());
    
    //  The idea here is to pick tiles at random at first, then as the actor
    //  gets closer to a given area, allow systematic scanning of nearby tiles
    //  to zero in on any strong cover available.
    final Stage world = actor.world();
    final Tile at = actor.origin();
    final Pick <Tile> pick = new Pick <Tile> () {
      public void compare(Tile next, float rating) {
        rating *= rateTileCover(actor, next, advance);
        if (report) I.say("  Rating for "+next+" was: "+rating);
        super.compare(next, rating);
      }
    };
    
    //  We provide a slight rating bonus for the actor's current location, then
    //  compare random tiles in each direction, and then compare any tiles
    //  within 2 units of the actor's origin.  Then return the most promising
    //  result.
    pick.compare(at, 1.1f);
    
    for (int n : TileConstants.T_ADJACENT) {
      Tile t = world.tileAt(
        Nums.clamp(at.x + (TileConstants.T_X[n] * range), 0, world.size),
        Nums.clamp(at.y + (TileConstants.T_Y[n] * range), 0, world.size)
      );
      t = Spacing.pickRandomTile(t, range, world);
      t = Spacing.nearestOpenTile(t, t);
      pick.compare(t, 1);
    }
    
    final Box2D around = actor.area(null).expandBy(2);
    for (Tile t : world.tilesIn(around, true)) pick.compare(t, 1);
    
    final Tile hides = pick.result();
    if (report) I.say("HIDING AT: "+hides);
    return hides;
  }
  
  
  //  TODO:  You need to log the output here, just to ensure that the right
  //  ratings are being returned.
  
  private static float rateTileCover(Actor actor, Tile t, boolean advance) {
    //  TODO:  Check to make sure the tile is reachable!
    if (t == null || t.blocked()) return 0;

    final boolean report = havenVerbose && I.talkAbout == actor;
    
    //  We confer a bonus to the rating if the tile in question has cover in
    //  the same direction as the actor's perceived sources of danger, while
    //  allowing clear sight to one side or the other (for easy retaliation.)
    float rating = 0.5f;
    final Tile allNear[] = t.allAdjacent(null);
    
    if (report) {
      for (int n : TileConstants.T_INDEX) if (isCover(allNear[n])) {
        I.say("      Blocked: "+TileConstants.DIR_NAMES[n]);
      }
    }
    
    for (int n : TileConstants.T_ADJACENT) {
      final Tile
        tile  = allNear[n],
        left  = allNear[(n + 1) % 8],
        right = allNear[(n + 7) % 8];
      if (isCover(tile) && ! (isCover(left) && isCover(right))) {
        final float danger = actor.senses.dangerFromDirection(n);
        if (report && danger > 0) I.say(
          "    Cover from "+TileConstants.DIR_NAMES[n]+
          "    Danger: "+actor.senses.dangerFromDirection(n)
        );
        rating += danger;
      }
    }
    
    //  We also favour locations that are either towards or away from danger,
    //  depending on whether an advance is called for:
    final int direction = Spacing.compassDirection(actor.origin(), t);
    final float distance = Spacing.distance(actor.origin(), t);
    final float maxMove = ActorHealth.DEFAULT_SIGHT * actor.health.baseSpeed();
    
    float dirBonus = actor.senses.dangerFromDirection(direction);
    dirBonus *= distance * (advance ? 1 : -1) / maxMove;
    if (report && dirBonus > 0) I.say("    Direction bonus: "+dirBonus);
    
    return rating * Nums.clamp(1 + dirBonus, 0, 2);
  }
  
  
  private static boolean isCover(Tile t) {
    return t != null && t.blocked();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final boolean urgent = actor.senses.isEmergency();
    
    if (
      safePoint == null || actor.aboard() == safePoint ||
      safePoint.pathType() == Tile.PATH_BLOCKS
    ) {
      safePoint = actor.senses.haven();
    }
    if (safePoint == null) {
      abortBehaviour();
      return null;
    }
    
    final Target home = actor.mind.home();
    final boolean goHome = (! urgent) && home != null;
    
    final Action flees = new Action(
      actor, goHome ? home : safePoint,
      this, "actionFlee",
      Action.MOVE_SNEAK, "Fleeing to "
    );
    if (urgent) flees.setProperties(Action.QUICK | Action.NO_LOOP);
    else flees.setProperties(Action.NO_LOOP);
    
    if (report) {
      I.say("\nFleeing to "+safePoint+", urgent? "+urgent);
    }
    return flees;
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    final boolean emergency = actor.senses.isEmergency();
    
    //  TODO:  Try to break line of sight with your pursuer, and thereby shake
    //  the tail.
    
    if (actor.indoors() && ! emergency) {
      final Resting rest = new Resting(actor, safePoint);
      rest.setMotive(Plan.MOTIVE_LEISURE, priorityFor(actor));
      maxDanger = 0;
      abortBehaviour();
      return true;
    }
    
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("Max. danger: "+maxDanger);
      I.say("Still in danger? "+emergency);
    }
    
    if (maxDanger < 0.5f || ! emergency) {
      maxDanger = 0;
    }
    else {
      maxDanger *= DANGER_MEMORY_FADE;
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (! actor.senses.isEmergency()) {
      d.append("Retiring to safety");
      return;
    }
    if (actor.aboard() == safePoint) d.append("Seeking refuge at ");
    else d.append("Retreating to ");
    d.append(safePoint);
  }
}





