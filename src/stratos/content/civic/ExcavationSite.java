/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class ExcavationSite extends HarvestVenue {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static String IMG_DIR = "media/Buildings/artificer/";
  final static CutoutModel SHAFT_MODEL = CutoutModel.fromImage(
    ExcavationSite.class, IMG_DIR+"excavation_shaft.gif", 4, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    ExcavationSite.class, "media/GUI/Buttons/excavation_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ExcavationSite.class, "excavation_site",
    "Excavation Site", UIConstants.TYPE_ENGINEER, ICON,
    "Excavation Sites expedite extraction of minerals and artifacts "+
    "from surrounding terrain.",
    4, 1, Structure.IS_NORMAL, Owner.TIER_FACILITY, 200, 15,
    METALS, FUEL_RODS, POLYMER, EXCAVATOR
  );
  
  final static int
    MIN_CLAIM_SIZE = BLUEPRINT.size + 4,
    MAX_CLAIM_SIZE = BLUEPRINT.size + 6;
  
  final public static Conversion
    LAND_TO_METALS = new Conversion(
      BLUEPRINT, "land_to_metals",
      15, HARD_LABOUR, 5, GEOPHYSICS, TO, 1, METALS
    ),
    LAND_TO_ISOTOPES = new Conversion(
      BLUEPRINT, "land_to_isotopes",
      15, HARD_LABOUR, 5, GEOPHYSICS, TO, 1, FUEL_RODS
    ),
    FLORA_TO_POLYMER = new Conversion(
      BLUEPRINT, "flora_to_polymer",
      10, HARD_LABOUR, TO, 1, POLYMER
    );
  
  
  public ExcavationSite(Base base) {
    super(BLUEPRINT, base, MIN_CLAIM_SIZE, MAX_CLAIM_SIZE);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(SHAFT_MODEL);
  }
  
  
  public ExcavationSite(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Siting and output-estimation:
    */
  final static Siting SITING = new Siting(BLUEPRINT) {
    
  };
  
  
  private Item[] estimateDailyOutput() {
    final Tile openFaces[] = claimDivision().reserved;
    if (openFaces == null) return new Item[0];
    float sumM = 0, sumF = 0, outM, outF;
    
    for (Tile t : openFaces) {
      final Item i = Outcrop.mineralsAt(t);
      if (i == null) continue;
      if (i.type == METALS   ) sumM++;
      if (i.type == FUEL_RODS) sumF++;
    }
    sumM /= openFaces.length;
    sumF /= openFaces.length;
    
    outM = sumM;
    outF = sumF;
    
    float mineMult = NewMining.HARVEST_MULT * staff.workforce() / 2f;
    mineMult *= Stage.STANDARD_SHIFT_LENGTH / NewMining.DEFAULT_TILE_DIG_TIME;
    outM *= mineMult * extractMultiple(METALS   );
    outF *= mineMult * extractMultiple(FUEL_RODS);
    
    return new Item[] {
      Item.withAmount(METALS   , outM),
      Item.withAmount(FUEL_RODS, outF)
    };
  }
  
  
  
  /**  Utility methods for handling dig-output and tile-assignment:
    */
  public boolean canDig(Tile at) {
    return claimDivision().useType(at, areaClaimed()) == 1;
  }
  
  
  public boolean canDump(Tile at) {
    return claimDivision().useType(at, areaClaimed()) == 2;
  }
  
  
  public float extractMultiple(Traded mineral) {
    if (mineral == METALS   ) {
      return 1 + (structure.upgradeLevel(METALS_SMELTING   ) / 3f);
    }
    if (mineral == FUEL_RODS) {
      return 1 + (structure.upgradeLevel(FUEL_RODS_SMELTING) / 3f);
    }
    if (mineral == FOSSILS  ) {
      return 1 + (structure.upgradeLevel(SAFETY_PROTOCOL   ) / 3f);
    }
    return 1;
  }
  
  
  protected boolean needsTending(Tile t) {
    return world.terrain().mineralsAt(t) > 0;
  }
  
  
  
  /**  Economic functions-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL, EngineerStation.LEVELS[0],
      new Object[] { 15, ASSEMBLY, 0, ANCIENT_LORE },
      400
    ),
    SAFETY_PROTOCOL = new Upgrade(
      "Safety Protocol",
      "Increases effective dig range while limiting pollution and improving "+
      "the chance to recover "+FOSSILS+".",
      100,
      Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    METALS_SMELTING = new Upgrade(
      "Metals Smelting",
      "Allows veins of heavy "+METALS+" to be sought out and processed "+
      "more reliably.",
      150,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, METALS
    ),
    
    FUEL_RODS_SMELTING = new Upgrade(
      "Fuel Rods Smelting",
      "Allows deposits of radioactive "+FUEL_RODS+" to be sought out and "+
      "extracted more reliably.",
      200,
      Upgrade.TWO_LEVELS, SAFETY_PROTOCOL, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, ANTIMASS
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to sustain an indefinite production of "+
      METALS+" and "+FUEL_RODS+" at the cost of heavy pollution.",
      350,
      Upgrade.SINGLE_LEVEL, METALS_SMELTING, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    );
  
  
  public int numOpenings(Background v) {
    final int NO = super.numOpenings(v);
    if (v == Backgrounds.EXCAVATOR) return NO + 3;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final boolean report =  verbose && I.talkAbout == actor;
    
    if (report) {
      I.say("\nGETTING NEXT EXCAVATION TASK...");
    }
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null) return d;
    
    final Choice choice = new Choice(actor);
    choice.add(NewMining.asMining(actor, this));
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(structure.upgradeLevel(SAFETY_PROTOCOL) - 3);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  private String compileOutputReport() {
    final StringBuffer report = new StringBuffer();
    report.append(super.helpInfo());
    
    final Item out[] = estimateDailyOutput();
    for (Item i : out) {
      final String amount = I.shorten(i.amount, 1);
      report.append("\n  Estimated "+i.type+" per day: "+amount);
    }
    return report.toString();
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}







