/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.base.BaseDemands.*;
import static stratos.game.craft.Economy.*;



//  Rations vending.  Repairs.  Medkits.
//  Advertising (morale boost and added purchase-attraction)
//  Security vault.  Currency exchange.


public class StockExchange extends Venue {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    StockExchange.class, "stock_exchange_model",
    "media/Buildings/merchant/stock_exchange.png", 4, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    StockExchange.class, "stock_exchange_icon",
    "media/GUI/Buttons/stock_exchange_button.gif"
  );
  
  
  final public static Traded
    ALL_STOCKED[] = {
      CARBS, PROTEIN, GREENS,
      MEDICINE, PLASTICS, PARTS
    };
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    StockExchange.class, "stock_exchange",
    "Stock Exchange", Target.TYPE_COMMERCE, ICON,
    "The Stock Exchange generates high  profits from the sale of finished and "+
    "luxury goods.",
    4, 1, Structure.IS_NORMAL,
    Owner.TIER_TRADER, 150, 3,
    Visit.compose(Object.class, ALL_STOCKED, new Object[] {
      SERVICE_COMMERCE, STOCK_VENDOR
    })
  );
  
  private float catalogueSums[] = new float[ALL_STOCKED.length];
  
  
  public StockExchange(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public StockExchange(Session s) throws Exception {
    super(s);
    for (int i = ALL_STOCKED.length; i-- > 0;) {
      catalogueSums[i] = s.loadFloat();
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    for (int i = ALL_STOCKED.length; i-- > 0;) {
      s.saveFloat(catalogueSums[i]);
    }
  }
  
  
  
  /**  Supplementary setup methods-
    */
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
    return Nums.clamp(catalogueSums[index] / stocks.amountOf(good), 0, 1);
  }
  
  
  //  TODO:  THIS SHOULD ONLY BE TRIGGERED BY BRINGING-PLANS, NOT THEFTS!
  public void afterTransaction(Item item, float amount) {
    super.afterTransaction(item, amount);
    if (amount >= 0) return;
    //
    //  For goods sold, you gain a variable bonus based on upgrade level, on
    //  top of normal full price for the good.  However, we only charge half-
    //  price to customers (see below.)
    final float
      basePrice    = super.priceFor(item.type, true),
      upgradeLevel = upgradeLevelFor(item.type),
      cashBonus    = basePrice * (BASE_SALE_MARGIN - 1) * (upgradeLevel + 1),
      catalogued   = catalogueLevel(item.type),
      sold         = 0 - amount,
      paidOn       = Nums.min(catalogued, sold),
      remainder    = sold - paidOn;
    
    final float totalBonus =
      (basePrice * sold / 2     ) +
      (cashBonus * paidOn       ) +
      (cashBonus * remainder / 2);
    stocks.incCredits(totalBonus);
    adjustCatalogue(item.type, paidOn);
  }
  
  
  public float priceFor(Traded good, boolean sold) {
    if (sold) return super.priceFor(good, sold) / 2;
    else return super.priceFor(good, sold);
  }
  
  
  public int spaceFor(Traded good) {
    final float upgradeLevel = upgradeLevelFor(good);
    if (upgradeLevel == -1) return 0;
    if (upgradeLevel ==  0) return 5;
    return 5 + (int) (upgradeLevel * 15);
  }
  
  
  
  /**  Upgrades, behaviour and economic functions-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.THREE_LEVELS, null,
      new Object[] { 10, ACCOUNTING, 0, HARD_LABOUR },
      450,
      650,
      850
    ),
    
    RATIONS_VENDING = new Upgrade(
      "Rations Vending",
      "Increases space available to "+CARBS+" and "+PROTEIN+" and augments "+
      "profits from their sale.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    //  TODO:  FAST FOOD!
    
    HARDWARE_STORE = new Upgrade(
      "Hardware Store",
      "Increases space available to "+PARTS+" and "+PLASTICS+", and augments "+
      "profits from their sale.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    //  TODO:  Use this to provide raw materials (metal, polymer, reagents.)
    SUPPLY_VAULT = null,
    
    //  TODO:  POWER CELLS!
    
    MEDICAL_EXCHANGE = new Upgrade(
      "Medical Exchange",
      "Increases space available to "+GREENS+" and "+MEDICINE+", and augments "+
      "profits from their sale.",
      250, Upgrade.THREE_LEVELS, RATIONS_VENDING, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    //  TODO:  MEDKITS!
    
    CREDITS_EXCHANGE = new Upgrade(
      "Credits Exchange",
      "Grants additional income based on total credit reserves within the "+
      "settlement relative to population.",
      400, Upgrade.TWO_LEVELS, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    ADVERTISEMENT = new Upgrade(
      "Advertisement",
      "Increases the likelihood of shoppers' visits and enhances morale when "+
      "doing so.",
      300, Upgrade.TWO_LEVELS, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    );
  
  
  private float upgradeLevelFor(Traded type) {
    Upgrade upgrade = null;
    if (type == CARBS  || type == PROTEIN ) upgrade = RATIONS_VENDING ;
    if (type == GREENS || type == MEDICINE) upgrade = MEDICAL_EXCHANGE;
    if (type == PARTS  || type == PLASTICS) upgrade = HARDWARE_STORE  ;
    return upgrade == null ? -1 : (structure.upgradeLevel(upgrade) / 3f);
  }
  
  
  public int numPositions(Background p) {
    final int level = structure.mainUpgradeLevel();
    if (p == Backgrounds.STOCK_VENDOR) return level + 1;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  ...You basically don't want the stock vendor wandering too far, because
    //  the venue has to be manned in order for citizens to come shopping.  So
    //  stick with jobs that happen within the venue.
    choice.add(BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5, true
    ));
    choice.add(BringUtils.bestBulkCollectionFor(
      this, services(), 2, 10, 5, true
    ));
    if (staff.assignedTo(Bringing.class) == 0 && ! choice.empty()) {
      return choice.pickMostUrgent();
    }
    //
    //  TODO:  Have the Supervision class delegate the precise behaviour you
    //  conduct back to the venue.
    choice.add(Supervision.inventory(this, actor));
    return choice.weightedPick();
  }
  
  
  protected void addServices(Choice choice, Actor actor) {
    final Property home = actor.mind.home();
    if (home instanceof Venue) {
      final Bringing d = BringUtils.fillBulkOrder(
        this, home, ((Venue) home).stocks.demanded(), 1, 5, true
      );
      if (d != null) {
        final float advertBonus = structure.upgradeLevel(ADVERTISEMENT) / 3f;
        d.setWithPayment(actor);
        d.addMotives(Plan.MOTIVE_LEISURE, Plan.ROUTINE * advertBonus);
        choice.add(d);
      }
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Update all stock demands-
    structure.setAmbienceVal(Ambience.MILD_AMBIENCE);
    stocks.updateStocksAsTrader(1, ALL_STOCKED);
    for (Traded type : ALL_STOCKED) {
      final float stockBonus = 1 + upgradeLevelFor(type);
      stocks.setConsumption(type, stocks.consumption(type) + stockBonus);
    }
    //
    //  In essence, we accumulate interest on savings per capita within the
    //  settlement.
    final int INTEREST_PERIOD = Stage.STANDARD_HOUR_LENGTH;
    if (numUpdates % INTEREST_PERIOD == 0) {
      float interest = structure.upgradeLevel(CREDITS_EXCHANGE) * 50 / 3f;
      interest *= INTEREST_PERIOD / (100f * Stage.STANDARD_DAY_LENGTH);
      float principle = base.finance.credits();
      principle /= 1 + base.ratings.population();
      stocks.incCredits(principle * interest);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  //  TODO:  Try merging these lists into a single array?
  final static Traded DISPLAYED_GOODS[] = {
    CARBS   , PROTEIN , GREENS  ,
    MEDICINE, PARTS   , PLASTICS,
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
  
  
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configStandardPanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    if (inWorld() && staff.manning() == 0) {
      return
        "The Stock Exchange cannot provide goods to local homeowners unless "+
        "someone is there to man the stalls!";
    }
    else return super.helpInfo();
  }
}





