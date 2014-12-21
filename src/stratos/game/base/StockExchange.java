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
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;


//
//  TODO:  Implement Bulk Storage and Credits Reserve upgrades.


public class StockExchange extends Venue {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    StockExchange.class, "media/Buildings/merchant/stock_exchange.png", 3.7f, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    StockExchange.class, "media/GUI/Buttons/stock_exchange_button.gif"
  );
  
  /*
  final static FacilityProfile PROFILE = new FacilityProfile(
    StockExchange.class, Structure.TYPE_VENUE,
    4, 300, 2, 0,
    new TradeType[] {},
    new Background[] { STOCK_VENDOR },
    SERVICE_COMMERCE
  );
  //*/
  
  //  Todo:  Specialise only in food stocks and finished goods!
  final static Traded ALL_STOCKED[] = Economy.ALL_MATERIALS;
  
  private CargoBarge cargoBarge;
  private float catalogueSums[] = new float[ALL_STOCKED.length];
  
  
  
  public StockExchange(Base base) {
    super(4, 1, ENTRANCE_SOUTH, base);
    personnel.setShiftType(SHIFTS_BY_DAY);
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    attachSprite(MODEL.makeSprite());
  }
  
  
  public StockExchange(Session s) throws Exception {
    super(s);
    cargoBarge = (CargoBarge) s.loadObject();
    
    for (int i = ALL_STOCKED.length; i-- > 0;) {
      catalogueSums[i] = s.loadFloat();
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(cargoBarge);
    
    for (int i = ALL_STOCKED.length; i-- > 0;) {
      s.saveFloat(catalogueSums[i]);
    }
  }
  
  
  
  /**  Supplementary setup methods-
    */
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    cargoBarge = new CargoBarge();
    cargoBarge.assignBase(base());
    cargoBarge.setHangar(this);
    final Tile o = origin();
    cargoBarge.enterWorldAt(o.x, o.y, world);
    cargoBarge.goAboard(this, world);
    return true;
  }
  
  
  public CargoBarge cargoBarge() {
    return cargoBarge;
  }
  
  
  private float upgradeLevelFor(Traded type) {
    final int category = Economy.categoryFor(type);
    Upgrade upgrade = null;
    switch (category) {
      case (CATEGORY_FOOD ) : upgrade = RATIONS_STALL   ; break;
      case (CATEGORY_DRUG ) : upgrade = MEDICAL_EXCHANGE; break;
      case (CATEGORY_WARES) : upgrade = HARDWARE_STORE  ; break;
      case (CATEGORY_SPYCE) : upgrade = SPYCE_EMPORIUM  ; break;
    }
    return upgrade == null ? -1 : (structure.upgradeLevel(upgrade) / 3f);
  }
  
  
  public boolean adjustCatalogue(Traded good, float inc) {
    final int index = Visit.indexOf(good, ALL_STOCKED);
    if (index == -1) return false;
    float level = catalogueSums[index];
    level = Nums.clamp(level + inc, 0, stocks.amountOf(good));
    catalogueSums[index] = level;
    return true;
  }
  
  
  public float catalogueLevel(Traded good) {
    final int index = Visit.indexOf(good, ALL_STOCKED);
    if (index == -1) return 0;
    return catalogueSums[index] / stocks.amountOf(good);
  }
  
  
  public void afterTransaction(Item item, float amount) {
    super.afterTransaction(item, amount);
    final float level = catalogueLevel(item.type);
    if (level <= 0 || amount >= 0) return;
    
    adjustCatalogue(item.type, 0 - Nums.abs(amount));
    final float basePrice    = super.priceFor(item.type);
    final float upgradeLevel = upgradeLevelFor(item.type);
    stocks.incCredits(basePrice * DEFAULT_SALES_MARGIN * upgradeLevel);
  }
  
  
  public float priceFor(Traded service) {
    final float basePrice    = super.priceFor(service);
    final float upgradeLevel = upgradeLevelFor(service);
    return basePrice * (1f - (DEFAULT_SALES_MARGIN * upgradeLevel / 2f));
  }
  
  
  public int spaceFor(Traded good) {
    //  TODO:  Include bonuses for structure-upgrade level, and bulk storage.
    final float upgradeLevel = upgradeLevelFor(good);
    if (upgradeLevel <= 0) return 0;
    return 10 + (int) (upgradeLevel * 15);
  }
  
  
  
  /**  Upgrades, behaviour and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  final public static Upgrade
    
    RATIONS_STALL = new Upgrade(
      "Rations Stall",
      "Increases space available to carbs, greens and protein and augments "+
      "profits from their sale.",
      150, null, 1, null,
      StockExchange.class, ALL_UPGRADES
    ),
    
    MEDICAL_EXCHANGE = new Upgrade(
      "Medical Exchange",
      "Increases space available to reagents, soma and medicine, and augments"+
      "profits from their sale.",
      250, null, 1, RATIONS_STALL,
      StockExchange.class, ALL_UPGRADES
    ),
    
    HARDWARE_STORE = new Upgrade(
      "Hardware Store",
      "Increases space available to parts, plastics and datalinks, and "+
      "augments profits from their sale.",
      150, null, 1, null,
      StockExchange.class, ALL_UPGRADES
    ),
    
    BULK_STORAGE = new Upgrade(
      "Bulk Storage",
      "Expands inventory space for all goods and provides a measure of "+
      "security against theft.",
      200, null, 1, null,
      StockExchange.class, ALL_UPGRADES
    ),
    
    SPYCE_EMPORIUM = new Upgrade(
      "Spyce Emporium",
      "Permits trading in Natrizoral, Tinerazine, and Halebdynum- trace "+
      "compounds vital to complex chemistry.",
      300, null, 1, MEDICAL_EXCHANGE,
      StockExchange.class, ALL_UPGRADES
    ),
    
    CREDITS_RESERVE = new Upgrade(
      "Credits Adjustmemt",
      "Allows your subjects to deposit their hard-earned savings and take out "+
      "temporary loans, while investing a portion of profits to augment "+
      "revenue.",
      400, null, 1, BULK_STORAGE,
      StockExchange.class, ALL_UPGRADES
    );
  
  
  public int numOpenings(Background p) {
    final int nO = super.numOpenings(p);
    if ( p == Backgrounds.STOCK_VENDOR) return nO + 2;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    final Traded services[] = services();
    
    //  TODO:  Consider investment activities!
    
    //
    //  See if there's a bulk delivery to be made-
    final Batch <Venue> depots = DeliveryUtils.nearbyDepots(
      this, world, StockExchange.class, FRSD.class
    );
    final Delivery bD = DeliveryUtils.bestBulkDeliveryFrom(
      this, services, 5, 50, depots
    );
    if (bD != null && personnel.assignedTo(bD) < 1) {
      bD.setMotive(Plan.MOTIVE_DUTY, Plan.CASUAL);
      bD.driven = cargoBarge;
      choice.add(bD);
    }
    final Delivery bC = DeliveryUtils.bestBulkCollectionFor(
      this, services, 5, 50, depots
    );
    if (bC != null && personnel.assignedTo(bC) < 1) {
      bC.setMotive(Plan.MOTIVE_DUTY, Plan.CASUAL);
      bC.driven = cargoBarge;
      choice.add(bC);
    }
    //
    //  Otherwise, consider regular, local deliveries and supervision.
    if (choice.empty()) {
      choice.add(DeliveryUtils.bestBulkDeliveryFrom (this, services, 1, 5, 5));
      choice.add(DeliveryUtils.bestBulkCollectionFor(this, services, 1, 5, 5));
      choice.add(Supervision.inventory(this, actor));
    }
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    //  TODO:  Arrange for barges to be recovered if left unattended!
    cargoBarge.setHangar(this);
    
    final Batch <Venue> depots = DeliveryUtils.nearbyDepots(
      this, world, StockExchange.class, FRSD.class
    );
    for (Traded type : ALL_MATERIALS) {
      final int room = spaceFor(type);
      stocks.incDemand(type, room / 2f, TIER_TRADER, 1, this);
      stocks.diffuseDemand(type, depots, 1);
    }
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.STOCK_VENDOR };
  }
  
  
  public Traded[] services() {
    return ALL_STOCKED;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  //  TODO:  Try merging these lists into a single array?
  final static Traded DISPLAYED_GOODS[] = {
    CARBS   , PROTEIN , GREENS   ,
    REAGENTS, SOMA    , MEDICINE ,
    PARTS   , PLASTICS, DATALINKS,
    SPYCE_T , SPYCE_H , SPYCE_N  ,
  };
  //  TODO:  Include the full range of items:  Foods, Drugs, Wares, Spyce.
  final static float GOOD_DISPLAY_OFFSETS[] = {
    0, 0.5f,
    0, 1.5f,
    0, 2.5f,
    0.5f, 0,
    1.5f, 0,
    2.5f, 0,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS;
  }
  
  
  protected Traded[] goodsToShow() {
    return DISPLAYED_GOODS;
  }
  
  //
  //  TODO:  You have to show items in the back as well, behind a sprite
  //  overlay for the facade of the structure.
  protected float goodDisplayAmount(Traded good) {
    final float CL = catalogueLevel(good);
    return Nums.min(super.goodDisplayAmount(good), 25 * CL);
  }
  
  
  public String fullName() {
    return "Stock Exchange";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "stock_exchange");
  }


  public String helpInfo() {
    return
      "The Stock Exchange facilitates small-scale purchases within the "+
      "neighbourhood, and bulk transactions between local merchants.";
  }


  public String objectCategory() {
    return UIConstants.TYPE_HIDDEN;
    //return UIConstants.TYPE_MERCHANT;
  }
}





