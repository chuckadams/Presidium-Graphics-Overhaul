/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.wip;
import stratos.content.civic.CargoBarge;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Consider adding one or two upgrades, to allow for scrapping, granary
//  functions, construction materials etc.?

//  Polymer still.  Extra barges.
//  Hardware outlet.  Granary depot.


public class SupplyDepot extends Venue {
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromSplatImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_under.gif", 4
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_core.png", 4, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    SupplyDepot.class, "media/GUI/Buttons/supply_depot_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    SupplyDepot.class, "supply_depot",
    "Supply Depot", UIConstants.TYPE_WIP, ICON,
    "The Supply Depot allows for bulk storage and transport of raw materials "+
    "used in manufacturing.",
    4, 1, Structure.IS_NORMAL,
    Owner.TIER_TRADER, 100,
    2,  //integrity
    200  ,  //armour
    Structure.NORMAL_MAX_UPGRADES
  );
  
  private List <CargoBarge> barges = new List <CargoBarge> ();
  
  
  public SupplyDepot(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(MODEL_UNDER, 0, 0, 0);
    sprite.attach(MODEL_CORE , 0, 0, 0);
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s);
    s.loadObjects(barges);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(barges);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Traded
    ALL_TRADE_TYPES[] = {
      POLYMER, METALS, FUEL_RODS,
      REAGENTS, SOMA, DRY_SPYCE
    },
    ALL_SERVICES[] = (Traded[]) Visit.compose(Traded.class,
      ALL_MATERIALS, new Traded[] { SERVICE_COMMERCE }
    );
  
  final public static Conversion
    NIL_TO_POLYMER = new Conversion(
      BLUEPRINT, "nil_to_polymer",
      TO, 1, POLYMER,
      SIMPLE_DC, CHEMISTRY
    );
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Update all stock demands-
    structure.setAmbienceVal(Ambience.MILD_SQUALOR);
    for (Traded type : ALL_TRADE_TYPES) {
      final float stockBonus = 1 + upgradeLevelFor(type);
      stocks.updateTradeDemand(type, stockBonus, 1);
    }
    
    //  TODO:  You need to send those barges off to different settlements!
    for (CargoBarge b : barges) if (b.destroyed()) barges.remove(b);
    if (barges.size() == 0) {
      final CargoBarge b = new CargoBarge();
      b.setHangar(this);
      b.assignBase(base);
      b.enterWorldAt(this, world);
      b.structure.setState(Structure.STATE_INSTALL, 0);
      barges.add(b);
    }
  }
  
  
  private float upgradeLevelFor(Traded type) {
    //  TODO:  Fill this in?
    return 0;
  }
  
  
  public int spaceFor(Traded t) {
    //  TODO:  Return a limit based on existing total good stocks!
    return 20;
  }
  
  
  private boolean bargeReady(CargoBarge b) {
    return b != null && b.inWorld() && b.structure.goodCondition();
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  See if there's a bulk delivery to be made, or if the cargo barge is in
    //  need of repair.
    final Traded services[] = ALL_MATERIALS;
    final CargoBarge cargoBarge = barges.first();
    if (bargeReady(cargoBarge)) {
      final Batch <Venue> depots = BringUtils.nearbyDepots(
        this, world, SERVICE_COMMERCE
      );
      final Bringing bD = BringUtils.bestBulkDeliveryFrom(
        this, services, 5, 50, depots
      );
      if (checkCargoJobOkay(bD, cargoBarge)) choice.add(bD);
      final Bringing bC = BringUtils.bestBulkCollectionFor(
        this, services, 5, 50, depots
      );
      if (checkCargoJobOkay(bC, cargoBarge)) choice.add(bC);
    }
    else for (CargoBarge b : barges) {
      if (Repairs.needForRepair(b) > 0) choice.add(new Repairs(actor, b, true));
      else if (b.abandoned()) choice.add(new Bringing(b, this));
    }
    //
    //  Otherwise, consider local deliveries.
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null && staff.assignedTo(d) < 1) choice.add(d);
    
    final Bringing c = BringUtils.bestBulkCollectionFor(
      this, services(), 2, 10, 5
    );
    if (c != null && staff.assignedTo(c) < 1) choice.add(c);
    
    if (! choice.empty()) return choice.weightedPick();
    //
    //  If none of that needs doing, consider local repairs or supervision.
    choice.add(Repairs.getNextRepairFor(actor, true, 0.1f));
    choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  private boolean checkCargoJobOkay(Bringing job, CargoBarge driven) {
    if (job == null || driven.pilot() != null) return false;
    if (job.totalBulk() < 10) return false;
    
    final float dist = Spacing.distance(job.origin, job.destination);
    if (dist < Stage.ZONE_SIZE) return false;
    
    job.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL);
    job.driven = driven;
    return true;
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.SUPPLY_CORPS };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.SUPPLY_CORPS) return nO + 2;
    return 0;
  }
  
  
  public Traded[] services() {
    return ALL_SERVICES;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return ALL_TRADE_TYPES;
  }
  
  
  final static float OFFSETS[] = {
    0, 2,  0, 3,  1, 2,  1, 3,
    0, 2.5f,  0, 3.5f,  1, 2.5f,  1, 3.5f,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return OFFSETS;
  }
  

  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
    /*
    return VenuePane.configStandardPanel(
      this, panel, UI,
      ALL_TRADE_TYPES, VenuePane.CAT_STOCK, VenuePane.CAT_STAFFING
    );
    //*/
  }
}



