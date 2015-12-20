/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.util.*;


//
//  TODO:  Move these out to the individual Mission types and implement each...
//
//  TODO:  Also, you will need to implement AI Pledge-settings for Contact
//         missions!

//  Strike mission:  Structures the enemy values.  Vulnerability/lack of risk.
//                   Dislike of base.
//  Security mission:  The converse- structures valued highly for bases you
//                     like, in dangerous areas.
//  Contact mission:  Basically, where they have a good chance of success, but
//                    low motivation, for tasks where you have a low chance of
//                    success, but high motivation.
//  Recon mission:  Fog rating.  Demand for resources you haven't found.



public class FactionAI {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    MIN_MISSIONS = 1,
    MAX_MISSIONS = 5,
    
    //  TODO:  Make these variable?  Like raid-frequency/force-strength?
    MISSION_WAIT_DURATION = Stage.STANDARD_DAY_LENGTH  * 2,
    DEFAULT_EVAL_INTERVAL = Stage.STANDARD_DAY_LENGTH  / 3,
    SHORT_EVAL_INTERVAL   = Stage.STANDARD_HOUR_LENGTH * 2,
    SHORT_WAIT_DURATION   = SHORT_EVAL_INTERVAL + 2       ;
  
  final static float
    DEFAULT_MIN_PARTY_STRENGTH = 1 / 1.5f,
    DEFAULT_MAX_PARTY_STRENGTH = 1 * 1.5f,
    MISSION_BEGUN_RATING_MULT  = 1.5f;
  
  
  final protected Base base;
  final protected SectorBase sector;
  
  final List <Mission> missions = new List <Mission> ();
  private float forceStrength;
  
  
  public FactionAI(Base base) {
    this.base   = base;
    this.sector = null;
  }
  
  
  public FactionAI(SectorBase demo) {
    this.base   = null;
    this.sector = demo;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(missions);
    forceStrength = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(missions);
    s.saveFloat(forceStrength);
  }
  
  
  
  /**  Basic access methods-
    */
  public void addMission(Mission m) {
    missions.include(m);
  }
  
  
  public void removeMission(Mission m) {
    missions.remove(m);
  }
  
  
  public Series <Mission> allMissions() {
    return missions;
  }
  
  
  public float forceStrength() {
    return forceStrength;
  }
  
  
  
  /**  Calling regular updates-
    */
  protected int updateInterval() {
    return shortWaiting ? SHORT_EVAL_INTERVAL : DEFAULT_EVAL_INTERVAL;
  }
  
  
  protected int missionWaitInterval() {
    return shortWaiting ? SHORT_WAIT_DURATION : MISSION_WAIT_DURATION;
  }
  
  
  public void updateForBase(int numUpdates) {
    if (base == null) return;
    if (numUpdates % updateInterval() != 0) return;
    
    forceStrength = 0;
    for (Mobile m : base.allUnits()) if (m instanceof Actor) {
      final Actor actor = (Actor) m;
      forceStrength += actor.senses.powerLevel();
    }
    
    if (base.isBaseAI()) updateDecisions();
  }
  
  
  public void updateForSector() {
    if (sector == null) return;
    
    forceStrength = sector.powerLevel(sector.faction());
    if (base.isBaseAI()) updateDecisions();
  }
  
  
  protected void updateDecisions() {
    
    final int maxMissions = (int) Nums.clamp(
      forceStrength * 2f / CombatUtils.MAX_POWER,
      MIN_MISSIONS, MAX_MISSIONS
    );
    
    if (missions.size() < maxMissions) {
      final Batch <Object > sampled = assembleSampleTargets();
      final Pick <Mission> pick = new Pick(0);
      
      for (Object target : sampled) {
        assessMissionsForTarget(target, base, pick);
      }
      final Mission newMission = pick.result();
      final float rating = pick.bestRating();
      
      if (newMission != null) {
        int priority  = Mission.PRIORITY_NOMINAL;
        priority     += Mission.PRIORITY_PARAMOUNT * rating;
        newMission.assignPriority(priority);
        newMission.setMissionType(Mission.TYPE_BASE_AI);
        missions.add(newMission);
        
        //  TODO:  In the case of a sector-settlement, generate applicants and
        //  launch immediately.
      }
    }
    for (Mission m : missions) {
      if (m.hasBegun()) {
        continue;
      }
      else if (shouldLaunch(m)) {
        m.beginMission();
      }
      else if (m.timeOpen() >= missionWaitInterval()) {
        m.endMission(false);
        continue;
      }
    }
  }
  
  
  
  /**  Obtaining samples of objects that could be suitable targets for fresh 
    *  missions...
    */
  protected Batch <Object> assembleSampleTargets() {
    final Batch <Object> sampled = new Batch();
    final Verse verse = base == null ? sector.universe : base.world.offworld;
    final Faction faction = base == null ? sector.faction() : base.faction();
    
    if (base != null) {
      addSamples(sampled, Venue .class, verse.world);
      addSamples(sampled, Mobile.class, verse.world);
    }
    for (SectorBase b : verse.sectorBases()) {
      if (b.faction() == faction) continue;
      sampled.add(b);
    }
    return sampled;
  }
  
  
  protected Batch addSamples(Batch sampled, Object typeKey, Stage world) {
    
    final Boarding origin = base == null ? null : base.HQ();
    if (origin == null && base != null) {
      if (I.logEvents()) {
        I.say("\nWARNING: "+this+" has no origin, cannot get mission targets.");
      }
      return sampled;
    }
    
    final PresenceMap sampFrom = base.world.presences.mapFor(typeKey);
    final int limit = Nums.max(10, sampFrom.population() / 100);
    
    for (Target t : sampFrom.visitNear(null, -1, null)) {
      if (origin != null && ! checkReachability(t, origin)) continue;
      sampled.add(t);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  protected boolean checkReachability(Target t, Boarding origin) {
    final Tile reachPoint = Spacing.nearestOpenTile(t, origin);
    return base.world.pathingMap.hasPathBetween(
      origin, reachPoint, base, false
    );
  }
  
  
  
  /**  Generating missions for the various targets assembled:
    */
  protected void assessMissionsForTarget(
    Object subject, Base base, Pick <Mission> pick
  ) {
    final Mission strike  = MissionStrike  .strikeFor  (subject, base);
    final Mission secure  = MissionSecurity.securityFor(subject, base);
    final Mission recon   = MissionRecon   .reconFor   (subject, base);
    final Mission contact = MissionContact .contactFor (subject, base);
    
    if (strike  != null) pick.compare(strike , rateMission(strike ));
    if (secure  != null) pick.compare(secure , rateMission(secure ));
    if (recon   != null) pick.compare(recon  , rateMission(recon  ));
    if (contact != null) pick.compare(contact, rateMission(contact));
  }
  
  
  protected void generateOffworldApplicants(Mission mission) {
    //
    //  Finally, once missions have been declared, then if you happen to be
    //  off-world, you generate applicants, generate a journey, generate an
    //  entry-point, check for pathability to the target, assign the applicants
    //  as passengers, and launch...
  }
  
  
  
  /**  Evaluation of missions and applicants-
    */
  public boolean allowsApplicant(Actor actor, Mission mission) {
    final boolean report = shouldReport();
    final float
      actorChance = MissionUtils.competence(actor, mission),
      actorPower  = actor.senses.powerLevel(),
      partyChance = MissionUtils.successChance(mission),
      partyPower  = MissionUtils.partyPower   (mission);
    
    final boolean approves = allowsApplicant(
      actor, mission, actorChance, actorPower, partyChance, partyPower
    );
    if (report) {
      I.say("\nChecking to allow mission-application...");
      I.say("  Mission is:   "+mission    +" ("+mission.base+")");
      I.say("  Applicant:    "+actor      +" ("+actor.base()+")");
      I.say("  Actor chance: "+actorChance+" (power "+actorPower+")");
      I.say("  Party chance: "+partyChance+" (power "+partyPower+")");
      I.say("  Will approve? "+approves);
    }
    return approves;
  }
  
  
  protected boolean allowsApplicant(
    Actor actor, Mission mission,
    float actorChance, float actorPower,
    float partyChance, float partyPower
  ) {
    float strength = (actorChance + partyChance) * 2;
    //  TODO:  BASE OFF COURAGE, etc.
    return strength <= DEFAULT_MAX_PARTY_STRENGTH;
  }
  
  
  protected boolean shouldLaunch(Mission mission) {
    final boolean timeUp = mission.timeOpen() >= missionWaitInterval();
    final float
      partyChance = MissionUtils.successChance(mission),
      partyPower  = MissionUtils.partyPower   (mission);
    return shouldLaunch(mission, partyChance, partyPower, timeUp);
  }
  
  
  protected boolean shouldLaunch(
    Mission mission, float partyChance, float partyPower, boolean timeUp
  ) {
    float strength = partyChance * 2;
    //  TODO:  BASE OFF COURAGE, etc.
    return strength > DEFAULT_MIN_PARTY_STRENGTH;
  }
  
  
  protected float rateMission(Mission mission) {
    final Target target = mission.subjectAsTarget();
    final Base other = target == null ? null : target.base();
    final float
      relations  = Faction.factionRelation(base, other),
      value      = mission.targetValue(base),
      harmLevel  = mission.harmLevel(),
      baseForce  = 1 + base .tactics.forceStrength(),
      enemyForce = 1 + other.tactics.forceStrength(),
      risk       = enemyForce / (baseForce + enemyForce);
    return rateMission(mission, relations, value, harmLevel, risk);
  }
  
  
  protected float rateMission(
    Mission mission,
    float relations, float targetValue,
    float harmLevel, float riskLevel
  ) {
    //
    //  In the case of 'helpful' missions, rate importance based on current
    //  relations.
    if (harmLevel < 0) {
      return targetValue <= 0 ? 0 : (targetValue * relations * 2);
    }
    //
    //  In the case of 'harmful' missions, modify based on dislike as well as
    //  the risks of provocation.
    else if (harmLevel > 0) {
      return (targetValue * (0 - relations) * 2) - riskLevel;
    }
    //
    //  Otherwise, just return the naked value:
    else return targetValue;
  }
  
  
  
  /**  Rendering, interface and debug functions-
    */
  public static boolean
    updatesVerbose = false,
    shortWaiting   = true ,
    extraVerbose   = false;
  protected static Faction
    verboseBase    = Faction.FACTION_ARTILECTS;

  
  protected boolean shouldReport() {
    if (! updatesVerbose) return false;
    if (base != null) return I.matchOrNull(base  .faction(), verboseBase);
    else              return I.matchOrNull(sector.faction(), verboseBase);
  }
}








