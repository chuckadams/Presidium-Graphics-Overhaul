/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.util.*;




public class ArtilectBase extends Base {
  
  
  private static boolean verbose = true ;
  
  final static float
    MAX_MISSION_POWER = CombatUtils.MAX_POWER * Mission.MAX_PARTY_LIMIT,
    ONLINE_WAKE_TIME  = Stage.STANDARD_YEAR_LENGTH / 2,
    CHECK_INTERVAL    = Stage.STANDARD_HOUR_LENGTH * 2;
  
  private float onlineLevel = 0;
  
  
  
  public ArtilectBase(Stage world) {
    super(world, true);
  }
  
  
  public ArtilectBase(Session s) throws Exception {
    super(s);
    this.onlineLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(onlineLevel);
  }
  
  
  public void setOnlineLevel(float toLevel) {
    this.onlineLevel = toLevel;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    //
    //  As long as there's a technologically-advanced non-artilect base on the
    //  map, increment the 'wakeup' level.
    if (numUpdates % CHECK_INTERVAL == 0) {
      boolean hasFoe = false;
      for (Base base : world.bases()) if (! base.primal) {
        if (world.presences.nearestMatch(base, null, -1) != null) {
          hasFoe = true;
        }
      }
      final float inc = CHECK_INTERVAL * 1f / ONLINE_WAKE_TIME;
      onlineLevel += inc * (hasFoe ? 1 : -1);
      onlineLevel = Nums.clamp(onlineLevel, 0, 1);
    }
  }
  
  
  protected BaseTactics initTactics() {
    return new BaseTactics(this) {
      
      protected boolean shouldAllow(
        Actor actor, Mission mission,
        float actorChance, float actorPower,
        float partyChance, float partyPower
      ) {
        final boolean report = verbose;
        float powerLimit = MAX_MISSION_POWER * onlineLevel;
        if (report) {
          I.say("\nChecking to allow mission-application...");
          I.say("  Mission is:   "+mission    +" ("+mission.base()+")");
          I.say("  Applicant:    "+actor      +" ("+actor.base()+")");
          I.say("  Actor chance: "+actorChance+" (power "+actorPower+")");
          I.say("  Party chance: "+partyChance+" (power "+partyPower+")");
          I.say("  On-line level: "+onlineLevel);
          I.say("  Power limit:   "+powerLimit );
        }
        return actorPower + partyPower <= powerLimit;
      }
    };
  }
}






