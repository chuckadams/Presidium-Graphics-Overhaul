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
import stratos.user.*;
import stratos.util.*;
import stratos.content.civic.Airfield;
import static stratos.game.economic.Economy.*;



public class BaseVisits {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    verbose        = false,
    extraVerbose   = false,
    migrateVerbose = verbose && true ,
    tradeVerbose   = verbose && true ;
  
  final public static float
    APPLY_INTERVAL  = Stage.STANDARD_DAY_LENGTH / 2f,
    UPDATE_INTERVAL = 10,
    TIME_SLICE      = UPDATE_INTERVAL / APPLY_INTERVAL,
    MAX_APPLICANTS  = 3;
  
  
  final Base base;
  
  protected Sector homeworld = Verse.DEFAULT_HOMEWORLD;
  final List <SectorBase> partners = new List();
  
  protected int maxShipsPerDay = 0;
  final List <Actor> candidates = new List <Actor> ();
  
  
  
  
  public BaseVisits(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    
    homeworld = (Sector) s.loadObject();
    s.loadObjects(partners);
    maxShipsPerDay = s.loadInt();
    s.loadObjects(candidates);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(homeworld);
    s.saveObjects(partners);
    s.saveInt(maxShipsPerDay);
    s.saveObjects(candidates);
  }
  
  
  public void assignHomeworld(Sector s) {
    homeworld = s;
    togglePartner(s, true);
  }
  
  
  public Sector homeworld() {
    return homeworld;
  }
  
  
  public void togglePartner(Sector s, boolean is) {
    final SectorBase SB = base.world.offworld.baseForSector(s);
    if (is) partners.include(SB);
    else    partners.remove (SB);
  }
  
  
  public Series <SectorBase> partners() {
    return partners;
  }
  
  
  public Series <Actor> allCandidates() {
    return candidates;
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateVisits(int numUpdates) {
    final boolean report = verbose && BaseUI.current().played() == base;
    if (report && extraVerbose) I.say("\nUpdating commerce for base: "+base);
    if (base.isPrimal()) return;
    
    updateCandidates(numUpdates);
    updateActiveShipping(numUpdates);
  }
  
  
  
  /**  Dealing with migrants and cargo-
    */
  protected void updateCandidates(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    
    final boolean report = base == BaseUI.currentPlayed() && migrateVerbose;
    final Background demanded[] = Background.INDEX.allEntries(Background.class);
    
    final Tally <Background> jobSupply = new Tally <Background> ();
    for (Actor c : candidates) jobSupply.add(1, c.mind.vocation());
    
    if (report) I.say("\nChecking for new recruits (slice: "+TIME_SLICE+")");
    
    for (Background b : demanded) {
      final float jobDemand = base.demands.globalDemand(b);
      if (jobDemand < 0.5f) continue;
      final float
        appDemand = jobDemand * MAX_APPLICANTS,
        appSupply = jobSupply.valueFor(b);
      float applyChance = (appDemand - appSupply) * TIME_SLICE;
      
      if (report) {
        I.say("  Hire chance for "+b+" is "+applyChance);
        I.say("  Supply/demand "+appSupply+" / "+appDemand);
      }
      
      while (Rand.num() < applyChance) {
        final Human applies = new Human(b, base);
        if (report) I.say("  New candidate: "+applies);
        candidates.addFirst(applies);
        final FindWork a = FindWork.attemptFor(applies, b, base);
        
        if (a == null || a.position() == null) {
          if (report) I.say("  No application made!");
        }
        else {
          if (report) I.say("  Applying at: "+a.position());
          a.enterApplication();
        }
        applyChance--;
      }
    }
    
    //  TODO:  Consider time-slicing this again, at least for larger
    //  settlements.
    if (report) I.say("\nTotal candidates "+candidates.size());
    
    for (ListEntry <Actor> e = candidates; (e = e.nextEntry()) != candidates;) {
      //
      //  If there's a successful application, enter it.
      final Actor      actor = e.refers;
      final Background job   = actor.mind.vocation();
      final FindWork   finds = FindWork.attemptFor(actor, job, base);
      float quitChance = TIME_SLICE;
      if (finds == null || finds.wasHired()) {
        quitChance = 1;
      }
      else {
        final float
          supply = jobSupply.valueFor(job),
          demand = base.demands.globalDemand(job),
          total  = supply + demand;
        if (total > 0) quitChance *= supply / total;
        finds.enterApplication();
        if (report) I.say("  "+actor+" ("+job+") applying: "+finds.employer());
      }
      //
      //  Otherwise, quit chance is based on relative abundance.
      if (Rand.num() <= quitChance) {
        if (finds != null) finds.cancelApplication();
        candidates.removeEntry(e);
        if (report) I.say("  "+actor+" ("+job+") quitting...");
      }
    }
  }
  
  
  public void addCandidate(Actor applies, Venue at, Background position) {
    candidates.add(applies);
    FindWork finding = FindWork.assignAmbition(applies, position, at, 2.0f);
    finding.enterApplication();
  }
  
  
  public void addCandidate(Background position, Venue at) {
    final Actor applies = new Human(position, base);
    addCandidate(applies, at, position);
  }
  
  
  public void removeCandidate(Actor applies) {
    candidates.remove(applies);
  }
  
  
  public int numCandidates(Background position) {
    int count = 0;
    for (Actor a : candidates) if (a.mind.vocation() == position) count++;
    return count;
  }
  
  
  
  /**  And finally, utility methods for calibrating the volume of shipping to
    *  or from this particular base:
    */
  private void updateActiveShipping(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final boolean report = tradeVerbose && base == BaseUI.currentPlayed();
    if (report) I.say("\nUPDATING ACTIVE SHIPPING FOR "+base);
    //
    //  TODO:  At the moment, we're aggregating all supply and demand into a
    //  single channel from the homeworld.  Once the planet-map is sorted out,
    //  you should evaluate pricing for each world independently... and how
    //  many ships will come.
    final Stage world = base.world;
    int spaceLimit = 0;
    if (world.presences.numMatches(base) > 0) {
      spaceLimit = 1;
    }
    for (Object t : world.presences.allMatches(Airfield.class)) {
      final Airfield field = (Airfield) t;
      if (field.base() != base) continue;
      spaceLimit++;
    }
    togglePartner(homeworld, true);
    spaceLimit = Nums.min(spaceLimit, homeworld.population + 1);
    //
    //  At any rate, we simply adjust the number of current ships based on
    //  the space allowance-
    final VerseJourneys travel = world.offworld.journeys;
    final Sector locale = world.localSector();;
    final Series <Vehicle> running = travel.tradersBetween(
      locale, homeworld, base, true
    );
    Vehicle last = running.last();
    if (running.size() < spaceLimit) {
      travel.setupTrader(homeworld, locale, base, true);
    }
    if (running.size() > spaceLimit && ! last.inWorld()) {
      travel.retireTrader(last);
    }
    if (report) {
      I.say("  Ships available: "+running.size());
      I.say("  Ideal limit:     "+spaceLimit);
    }
    this.maxShipsPerDay = spaceLimit;
  }
  
  
  public void configCargo(Stocks forShipping, int fillLimit, boolean fillImp) {
    if (fillImp) forShipping.removeAllItems();
    
    final Batch <Item>
      imports = new Batch <Item> (),
      exports = new Batch <Item> ();
    float sumImp = 0, sumExp = 0, scaleImp = 1, scaleExp = 1;
    
    for (Traded type : ALL_MATERIALS) {
      float shortage = base.demands.importDemand(type);
      shortage -= base.demands.exportSupply(type);
      
      if (shortage > 0) {
        imports.add(Item.withAmount(type, shortage));
        sumImp += shortage;
      }
      if (shortage < 0) {
        exports.add(Item.withAmount(type, 0 - shortage));
        sumExp += 0 - shortage;
      }
    }
    
    if (sumImp > fillLimit) scaleImp = fillLimit / sumImp;
    for (Item i : imports) {
      final int amount = Nums.round(i.amount * scaleImp, 2, false);
      if (amount <= 0) continue;
      forShipping.forceDemand(i.type, 0, amount);
      if (fillImp) forShipping.bumpItem(i.type, amount);
    }
    
    if (sumExp > fillLimit) scaleExp = fillLimit / sumExp;
    for (Item e : exports) {
      final int amount = Nums.round(e.amount * scaleExp, 2, false);
      if (amount <= 0) continue;
      forShipping.forceDemand(e.type, amount, 0);
    }
  }
}






