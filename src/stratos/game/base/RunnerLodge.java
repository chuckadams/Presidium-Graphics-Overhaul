

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



//  So... what does a runner do during their normal days?
//  *  Smuggle (to afar.)
//  *  Provide services (poison, vr, gene mods, etc. nearby)
//  *  Steal (from afar.)
//  *  Perform enforcement/taxation (nearby.)


//  TODO:  IMPLEMENT UPGRADES:

//  Silver Geist Lodge
//  IV Punks Lodge
//  Hudzin Baru Lodge

//  ...Once you invest in one gang/clan, you can't invest in others.

//  Silver Geist upgrades:  (+ Geist chips)
//    Cyberware Studio  (Drone hacks or attribute bonuses)
//    Simstims Studio   (VR entertainment)
//    Virtual Currency  (Extra cash and easy loaning)

//  IV Punks upgrades:      (+ Kill contracts)
//    Fast Toxin Lab    (Poison for physical weapons)
//    Slow Burn Lab     (Boosts reflexes, but addictive)
//    Heavy Enforcement (Boost to combat training)

//  Hudzin Baru upgrades:   (+ Gene samples)
//    Cosmetic Clinic   (Disguise or beautify)
//    G-Mod Clinic      (Trait bonus with mutation risk)
//    Fugitive Trade    (Kidnapping and cheap labour)

//  ...They can offer some of these specialties at the Cantina as well.


//  Sniper Kit.  Sticky Bombs.
//  Night Optics.  Urban Camo.
//  Loaning.  Public Bounties.



public class RunnerLodge extends Venue {
  
  
  /**  Setup and constructors-
    */
  final static String IMG_DIR = "media/Buildings/merchant/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    EcologistStation.class, "media/GUI/Buttons/runner_market_button.gif"
  );
  final static ModelAsset MODEL = CutoutModel.fromImage(
    RunnerLodge.class, IMG_DIR+"runner_market.png", 4, 3
  );
  
  final static int
    GANG_NONE       = -1,
    GANG_SILVERFISH =  0,
    GANG_IV_PUNKS   =  1,
    GANG_HUDZENA    =  2;
  final static Background
    ALL_BACKGROUNDS[][] = {
      { RUNNER_SILVERFISH, RUNNER_IV_PUNKS, RUNNER_HUDZENA },
      { JACK_ARTIST      , STREET_COOK    , FACE_FIXER     },
      { ASSASSIN         , BRUISER        , ANONYMOUS      },
    },
    RUNNER_BACKGROUNDS [] = ALL_BACKGROUNDS[0],
    SERVICE_BACKGROUNDS[] = ALL_BACKGROUNDS[1],
    SENIOR_BACKGROUNDS [] = ALL_BACKGROUNDS[2];
  final static int
    CLAIM_SIZE = 8;
  
  final static VenueProfile PROFILE = new VenueProfile(
    RunnerLodge.class, "runner_lodge", "Runner Market",
    4, 2, false, StockExchange.PROFILE
  );
  
  private int gangID = GANG_NONE;
  
  
  public RunnerLodge(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_HOURS);
    attachModel(MODEL);
  }
  
  
  public RunnerLodge(Session s) throws Exception {
    super(s);
    gangID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(gangID);
  }
  
  
  
  /**  Economic and behavioural overrides-
    */
  //  TODO:  Include corresponding upgrades and techniques for all of these!
  
  final static Traded
    NEURAL_IMPLANT = new Traded(
      RunnerLodge.class, "Neural Implant", null, Economy.FORM_USABLE, 100,
      "Neural implants boost cognitive ability and may allow hacking of "+
      "simple drones and cybrids."
    ),
    KINETIC_IMPLANT = new Traded(
      RunnerLodge.class, "Kinetic Implant", null, Economy.FORM_USABLE, 120,
      "Kinetic implants boost strength and dexterity, together with a degree "+
      "of natural armour."
    ),
    SIMSTIMS = new Traded(
      RunnerLodge.class, "Simstims", null, Economy.FORM_SPECIAL, 45,
      "Simstims provide voyeuristic virtual entertainment to the masses."
    ),
    
    FAST_TOXIN = new Traded(
      RunnerLodge.class, "Fast Toxin", null, Economy.FORM_USABLE, 85,
      "A fast-acting poison suitable for application to melee or kinetic "+
      "weaponry."
    ),
    SLOW_BURN = new Traded(
      RunnerLodge.class, "Slow Burn", null, Economy.FORM_USABLE, 55,
      "An addictive narcotic that greatly accelerates reaction times and "+
      "perception."
    ),
    
    //  Disguise, beauty, and cognitive/sensitive/physical DNA treatments are
    //  reserved for the Hudzin Baru.
    
    //  TODO:  Maybe these should be special abilities for the runner class?
    //  Yes.  Work that out.
    
    SNIPER_KIT = new Traded(
      RunnerLodge.class, "Sniper Kit", null, Economy.FORM_USABLE, 90,
      "Allows ranged attacks at far greater distances, particularly if the "+
      "target is surprised."
    ),
    STICKY_BOMB = new Traded(
      RunnerLodge.class, "Sticky Bomb", null, Economy.FORM_USABLE, 35,
      "Deals heavy damage to vehicles and buildings, if attached at point-"+
      "blank range."
    ),
    NIGHT_OPTICS = new Traded(
      RunnerLodge.class, "Night Optics", null, Economy.FORM_USABLE, 25,
      "Allows extended sight range in space or nocturnal conditions."
    ),
    GHOST_CAMO = new Traded(
      RunnerLodge.class, "Ghost Camo", null, Economy.FORM_USABLE, 40,
      "Improves stealth and cover in daytime or outdoor environments."
    );
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    
    //  TODO:  It seems a little odd that runners would be working strictly 'on
    //  the clock'.  Fudge this a little?
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    //
    //  Either collect protection money from nearby businesses, or loot from
    //  more distant properties:
    final Box2D territory = areaClaimed();
    final Batch <Venue> venues = new Batch <Venue> ();
    world.presences.sampleFromMaps(this, world, 5, venues, Venue.class);
    
    for (Venue venue : venues) {
      if (territory.contains(venue.position(null))) {
        choice.add(Audit.nextExtortionAudit(actor, venue));
      }
      else {
        choice.add(new Looting(actor, venue, null, this));
      }
    }
    //
    //  You also need to perform enforcement duties in the neighbourhood:
    choice.add(Arrest.nextOfficialArrest(this, actor));
    //
    //  Next, consider smuggling goods out of the settlement-
    for (Dropship ship : actor.base().commerce.allVessels()) {
      if (! ship.landed()) continue;
      final Item toMove[] = base.commerce.getBestCargo(stocks, 5, false);
      final Smuggling s = new Smuggling(actor, this, ship, toMove);
      //s.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      if (staff.assignedTo(s) == 0) choice.add(s);
    }
    return choice.weightedPick();
    //
    //  And lastly, consider manufacturing contraband from scratch:
    //  TODO:  This has to be customised by gang.  Work on that.
  }
  
  
  protected Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(CLAIM_SIZE);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Bastion     ) return true;
    if (other instanceof EnforcerBloc) return true;
    return false;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    //  For the IV Punks, add Slow Burn or Fast Toxin purchases.
    //  For the Silver Geist, add Implants surgery or Simstim recordings.
    //  For the Hudzin Baru, add Disguise or G-Mods purchases/surgery.
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    for (Traded type : ALL_MATERIALS) {
      stocks.incDemand(type, 5, Tier.PRODUCER, 1);
    }
    //  Demand either parts or reagents, depending on what you're making.
    //  Register as a producer of whatever you're making.
  }
  
  
  final static Traded[] SERVICES = (Traded[]) Visit.compose(
    Traded.class, ALL_MATERIALS,
    new Traded[] { SERVICE_SECURITY }
  );
  public Traded[] services() { return SERVICES; }
  
  
  final static Background[] CAREERS = (Background[]) Visit.compose(
    Background.class, RUNNER_BACKGROUNDS, SERVICE_BACKGROUNDS
  );
  public Background[] careers() { return CAREERS; }
  
  
  public int numOpenings(Background b) {
    if (gangID == GANG_NONE) return 0;
    final int nO = super.numOpenings(b);
    if (b == RUNNER_BACKGROUNDS [gangID]) return nO + 2;
    if (b == SERVICE_BACKGROUNDS[gangID]) return nO + 1;
    return 0;
  }
  
  
  
  /**  Rendering and interface-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "runner_market");
  }
  
  
  public String helpInfo() {
    return
      "Runner Markets can offer black market technology and other "+
      "clandestine services to settlements willing to overlook their "+
      "criminal connections.";
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_COMMERCE;
  }
}






