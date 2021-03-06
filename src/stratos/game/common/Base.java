/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.base.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;




public abstract class Base extends SectorBase implements
  Session.Saveable, Schedule.Updates, Accountable
{
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MAX_BASES      = 8,
    ALL_BASE_IDS[] = {0, 1, 2, 3, 4, 5, 6, 7};
  
  final public Stage world;
  private int baseID = 0;
  
  final public BaseSetup     setup    ;
  final public BaseDemands   demands  ;
  final public BaseVisits    visits   ;
  final public BaseTransport transport;
  
  final public BaseFinance  finance  ;
  final public BaseProfiles profiles ;
  final public DangerMap    dangerMap;
  final public IntelMap     intelMap ;
  
  private Venue commandPost;
  
  final public BaseRatings  ratings   = initRatings ();
  final public BaseAdvice   advice    = initAdvice  ();
  final public BaseResearch research  = initResearch();
  final public BaseTactics  tactics   = initTactics ();
  
  
  private String title  = "Player Base";
  private Colour colour = new Colour();
  private Tally <Blueprint> venueIDTallies = new Tally();
  
  
  protected Base(Stage world, Faction faction) {
    super(world.offworld, world.localSector());
    this.world   = world  ;
    assignFaction(faction);
    
    setup      = new BaseSetup(this);
    demands    = new BaseDemands(this);
    visits     = new BaseVisits(this);
    transport  = new BaseTransport(world);
    finance    = new BaseFinance(this);
    profiles   = new BaseProfiles(this);
    dangerMap  = new DangerMap(world, this);
    intelMap   = new IntelMap(this);
    intelMap.initFog(world);
  }
  
  
  public Base(Session s) throws Exception {
    super(s);
    
    world      = (Stage) s.loadObject();
    setup      = new BaseSetup(this);
    demands    = new BaseDemands(this);
    visits     = new BaseVisits(this);
    transport  = new BaseTransport(world);
    finance    = new BaseFinance(this);
    profiles   = new BaseProfiles(this);
    dangerMap  = new DangerMap(world, this);
    intelMap   = new IntelMap(this);
    intelMap.initFog(world);
    
    setup    .loadState(s);
    demands  .loadState(s);
    visits   .loadState(s);
    transport.loadState(s);
    finance  .loadState(s);
    profiles .loadState(s);
    dangerMap.loadState(s);
    intelMap .loadState(s);

    commandPost = (Venue) s.loadObject();
    
    ratings  .loadState(s);
    tactics  .loadState(s);
    advice   .loadState(s);
    research .loadState(s);
    
    title = s.loadString();
    colour.loadFrom(s.input());
    s.loadTally(venueIDTallies);
    
    baseID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(world);
    setup    .saveState(s);
    demands  .saveState(s);
    visits   .saveState(s);
    transport.saveState(s);
    finance  .saveState(s);
    profiles .saveState(s);
    dangerMap.saveState(s);
    intelMap .saveState(s);
    
    s.saveObject(commandPost);
    
    ratings  .saveState(s);
    tactics  .saveState(s);
    advice   .saveState(s);
    research .saveState(s);
    
    s.saveString(title);
    colour.saveTo(s.output());
    s.saveTally(venueIDTallies);
    
    s.saveInt(baseID);
  }
  
  
  protected BaseRatings  initRatings () { return new BaseRatings (this); }
  protected BaseAdvice   initAdvice  () { return new BaseAdvice  (this); }
  protected BaseResearch initResearch() { return new BaseResearch(this); }
  protected BaseTactics  initTactics () { return new BaseTactics (this); }
  
  
  
  public static Base findBase(Stage world, String title, Faction belongs) {
    for (Base base : world.bases()) {
      if (belongs != null && base.faction() != belongs) continue;
      if (title != null && ! title.equals(base.title)) continue;
      return base;
    }
    return null;
  }
  
  
  private static int nextBaseID(Stage world) {
    final boolean maskIDs[] = new boolean[MAX_BASES];
    for (Base b : world.bases()) maskIDs[b.baseID] = true;
    for (int i = 0; i < MAX_BASES; i++) if (! maskIDs[i]) return i;
    I.complain("\nCOULD NOT ASSIGN ID TO BASE...");
    return -1;
  }
  
  
  private static Base registerBase(
    Base base, Stage world, Blueprint... canBuild
  ) {
    if (world.bases().size() >= MAX_BASES) {
      I.complain("\nCANNOT SUPPORT MORE THAN "+MAX_BASES+" BASES!");
      return null;
    }
    base.setup.setAvailableVenues(canBuild);
    
    base.baseID = nextBaseID(world);
    world.registerBase(base, true);
    
    base.title    = base.faction().name;
    base.colour.set(base.faction().bannerColour());
    
    if (I.logEvents()) I.say("\nREGISTERING NEW BASE: "+base);
    return base;
  }
  
  
  public static CivicBase settlement(
    Stage world, String customTitle, Faction faction
  ) {
    Base base = findBase(world, null, faction);
    if (base != null) return (CivicBase) base;
    else base = new CivicBase(world, faction);
    
    final Blueprint canBuild[] = Blueprint.allCivicBlueprints();
    if (customTitle != null) base.title = customTitle ;
    
    final Sector home = faction.startSite();
    if (home != null) {
      base.visits  .assignHomeworld  (home);
      base.research.initKnowledgeFrom(home);
    }
    return (CivicBase) registerBase(base, world, canBuild);
  }
  
  
  public static FaunaBase wildlife(Stage world) {
    Base base = findBase(world, null, Faction.FACTION_WILDLIFE);
    if (base != null) return (FaunaBase) base;
    else base = new FaunaBase(world);
    
    final Blueprint canBuild[] = new Blueprint[0];
    return (FaunaBase) registerBase(base, world, canBuild);
  }
  
  
  public static VerminBase vermin(Stage world) {
    Base base = findBase(world, null, Faction.FACTION_VERMIN);
    if (base != null) return (VerminBase) base;
    else base = new VerminBase(world);
    
    final Blueprint canBuild[] = new Blueprint[0];
    registerBase(base, world, canBuild);
    return (VerminBase) base;
  }
  
  
  public static ArtilectBase artilects(Stage world) {
    Base base = findBase(world, null, Faction.FACTION_ARTILECTS);
    if (base != null) return (ArtilectBase) base;
    else base = new ArtilectBase(world);
    
    final Blueprint canBuild[] = new Blueprint[0];
    registerBase(base, world, canBuild);
    return (ArtilectBase) base;
  }
  
  
  public static NativeBase natives(Stage world, int tribeID) {
    final String title = NativeHut.TRIBE_NAMES[tribeID];
    Base base = findBase(world, title, Faction.FACTION_NATIVES);
    
    if (base != null) return (NativeBase) base;
    else base = new NativeBase(world, title);
    
    final Blueprint canBuild[] = NativeHut.VENUE_BLUEPRINTS[tribeID];
    return (NativeBase) registerBase(base, world, canBuild);
  }
  
  
  
  /**  Dealing with missions, visits (spawning) and personnel-
    */
  public Property HQ() {
    return ruler() == null ? null : ruler().mind.home();
  }
  
  
  //  TODO:  Move to the tactics class?
  public Mission matchingMission(Object subject, Class typeClass) {
    for (Mission match : tactics.allMissions()) {
      if (typeClass != null && match.getClass() != typeClass) continue;
      if (match.subject() != subject) continue;
      return match;
    }
    return null;
  }
  
  
  public Mission matchingMission(Mission m) {
    return matchingMission(m.subject(), m.getClass());
  }
  
  
  public Base base() {
    return this;
  }
  
  
  public int baseID() {
    return baseID;
  }
  
  
  
  /**  Regular updates-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    final boolean report = false;
    
    if (report) I.say("\nUpdating "+this);
    long initTime, timeAfter;
    initTime = System.currentTimeMillis();
    
    setup.updatePlacements();
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after placements: "+timeAfter);
    
    demands.updateAllMaps(1, numUpdates);
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after demands: "+timeAfter);
    
    visits.updateVisits(numUpdates);
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after commerce: "+timeAfter);
    
    //  TODO:  THIS CAN, ON RARE OCCASIONS, BE HIGHLY INEFFICIENT FOR SOME
    //  REASON.  Everything else seems fine?
    transport.distributeProvisions(this);
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after transport: "+timeAfter);
    
    dangerMap.update();
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after danger: "+timeAfter);
    
    advice.updateAdvice(numUpdates);
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after advice: "+timeAfter);
    
    tactics.updateForBase(numUpdates);
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after tactics: "+timeAfter);

    finance.updateFinances();
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after finance: "+timeAfter);
    
    ratings.updateRelations(numUpdates);
    timeAfter = System.currentTimeMillis() - initTime;
    if (report) I.say("  Time after relations: "+timeAfter);
  }
  
  
  public void updateUnits() {
    for (Mobile m : allUnits()) m.updateAsMobile();
  }
  
  
  
  /**  Venue enumeration methods-
    */
  //  TODO:  Move these to the Blueprint class?
  
  public Batch <Venue> listInstalled(
    Blueprint type, boolean intact
  ) {
    final Batch <Venue> installed = new Batch <Venue> ();
    for (Object o : world.presences.matchesNear(type.baseClass, null, -1)) {
      final Venue v = (Venue) o;
      if (intact && ! v.structure.intact()) continue;
      if (v.base() == this) installed.add(v);
    }
    return installed;
  }
  
  
  public boolean checkPrerequisites(
    Blueprint print, Account reasons
  ) {
    if (print.isUnique()) {
      if (listInstalled(print, false).size() > 0) {
        return reasons.setFailure("You cannot have more than one "+print.name);
      }
    }
    if (! research.hasTheory(print.baseUpgrade())) {
      return reasons.setFailure("Not yet researched.");
    }
    return reasons.setSuccess();
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public static void renderMissions(
    Stage world, Rendering rendering, Base player
  ) {
    for (Mission mission : layoutMissions(world, rendering.view, player)) {
      mission.renderFlag(rendering);
    }
  }
  
  
  public static Mission pickedMission(
    Stage world, BaseUI UI, Viewport view, Base player
  ) {
    final Pick <Mission> pick = new Pick <Mission> ();
    final Vec3D centre = new Vec3D();
    final float scale = Mission.FLAG_SCALE;
    
    for (Mission mission : layoutMissions(world, view, player)) {
      centre.setTo(mission.flagSprite().position);
      centre.z += 1 * scale;
      if (! view.mouseIntersects(centre, scale, UI)) continue;
      final float dist = view.translateToScreen(centre).z;
      pick.compare(mission, 0 - dist);
    }
    return pick.result();
  }
  
  
  private static Series <Mission> layoutMissions(
    Stage world, Viewport view, Base player
  ) {
    final Table <Target, List <Mission>> seenTable = new Table();
    
    for (Base base : world.bases()) {
      for (Mission m : base.tactics.allMissions()) if (m.visibleTo(player)) {
        if (m.subjectAsTarget() == null || m.isOffworld()) continue;
        final Target t = (Target) m.subject();
        if (! view.intersects(t.position(null), t.radius())) continue;
        
        List <Mission> onT = seenTable.get(t);
        if (onT == null) seenTable.put(t, onT = new List());
        onT.add(m);
      }
    }
    
    final float spacing = Mission.FLAG_SCALE * 1.5f;
    final Batch <Mission> visible = new Batch();
    
    for (Target t : seenTable.keySet()) {
      final List <Mission> above = seenTable.get(t);
      final Batch <Sprite> sprites = new Batch();
      for (Mission m : above) sprites.add(m.flagSprite());
      
      final Vec3D point = new Vec3D();
      if (t instanceof Element) ((Element) t).viewPosition(point);
      else t.position(point);
      point.z += t.height();
      
      CutoutSprite.layoutAbove(point, 0, -0.15f, -1, view, spacing, sprites);
      Visit.appendTo(visible, above);
    }
    return visible;
  }
  
  
  public String toString() {
    return title;
  }
  
  
  public Colour colour() {
    return colour;
  }
  
  
  public void setColour(Colour c) {
    this.colour = c;
  }
  
  
  public String title() {
    return title;
  }
  
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  
  public int nextVenueID(Blueprint type) {
    venueIDTallies.add(1, type);
    return (int) venueIDTallies.valueFor(type);
  }
}








