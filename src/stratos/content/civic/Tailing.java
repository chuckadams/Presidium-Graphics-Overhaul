/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.PavingMap;
import stratos.game.maps.StageTerrain;
import stratos.game.plans.Mining;
import stratos.game.wild.Habitat;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.Composite;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Tailing extends Element implements Selectable {
  
  
  final Traded wasteType;
  private float fillLevel = 0;
  
  
  public Tailing(Traded wasteType) {
    super();
    this.wasteType = wasteType;
    updateSprite();
  }
  
  
  public Tailing(Session s) throws Exception {
    super(s);
    this.wasteType = (Traded) s.loadObject();
    this.fillLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(wasteType);
    s.saveFloat (fillLevel);
  }
  
  
  
  /**  Life-cycle, updates and maintenance-
    */
  public boolean canPlace() {
    final Tile o = origin();
    if (o != null && o.reserves() instanceof ExcavationSite) return true;
    return false;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    
    final Tile o = origin();
    PavingMap.setPaveLevel(o, StageTerrain.ROAD_STRIP, true);
    
    for (Tile t : o.allAdjacent(null)) {
      if (t != null) t.clearUnlessOwned(intact);
    }
    return true;
  }
  
  
  public int owningTier() {
    return Owner.TIER_PRIVATE;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public boolean takeFill(float amount) {
    this.fillLevel = Nums.clamp(fillLevel + amount, 0, Mining.TAILING_LIMIT);
    updateSprite();
    return true;
  }
  
  
  public Traded wasteType() {
    return wasteType;
  }
  
  
  public float fillLevel() {
    return fillLevel / Mining.TAILING_LIMIT;
  }
  
  
  public void onGrowth(Tile at) {
    //
    //  TODO:  Extend Outcrop?
    
    if (Rand.num() < fillLevel) {
      world.terrain().setHabitat(origin(), Habitat.TOXIC_RUNOFF);
      float reduction = Mining.TAILING_LIMIT;
      reduction *= Stage.GROWTH_INTERVAL * 1f / Stage.STANDARD_DAY_LENGTH;
      reduction /= Mining.DEFAULT_MINE_LIFESPAN;
      takeFill(0 - reduction);
    }
    if (fillLevel <= 0) setAsDestroyed();
  }
  
  
  public static Tailing foundAt(Tile at) {
    if (at == null || ! (at.above() instanceof Tailing)) return null;
    return (Tailing) at.above();
  }
  
  
  
  /**  Graphical constants and update methods-
    */
  final static String IMG_DIR = "media/Buildings/artificer/";
  final public static ModelAsset
    ALL_TAILING_MODELS[][] = CutoutModel.fromImageGrid(
      Tailing.class, IMG_DIR+"mine_tailings.png",
      5, 5, 1, 1, false
    ),
    MAGMA_TAILINGS [] = ALL_TAILING_MODELS[0];
  final static Traded MODEL_ORDER[] = {
    null, SLAG, POLYMER, METALS, FUEL_RODS
  };
  
  
  private void updateSprite() {
    final int stage = Nums.min(4, 1 + (int) (fillLevel() * 3));
    final int index = Nums.max(1, Visit.indexOf(wasteType, MODEL_ORDER));
    final ModelAsset model = ALL_TAILING_MODELS[index][stage];
    
    final Sprite oldSprite = sprite();
    attachModel(model);
    if (oldSprite != null && oldSprite.model() != model) {
      world.ephemera.addGhost(this, 1, oldSprite, 0.5f);
    }
  }
  

  public String fullName() {
    return "Tailing ("+wasteType.name+")";
  }


  public Composite portrait(BaseUI UI) {
    //  TODO:  Fill this in.
    return null;
  }
  

  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionPane(UI, this, null, true);
    
    final Description d = panel.detail(), l = panel.listing();
    d.append("Total stored: "+I.shorten(fillLevel, 0)+"/"+Mining.TAILING_LIMIT);
    d.append("\n\n");
    d.append(helpInfo());
    
    return panel;
  }
  
  
  public String helpInfo() {
    return
      "Tailings are the rubble and waste products left behind after mining "+
      "operations.  They cannot be easily salvaged, and pose a persistent "+
      "pollution hazard.";
  }
}



//  TODO:  Try to find a place for these activities:

/*
public boolean actionReassemble(Actor actor, Venue site) {
  //  TODO:  Test and restore
  /*
  final Item sample = Item.withReference(SAMPLES, ARTIFACTS);
  final Structure s = site.structure();
  final float
    AAU = s.upgradeLevel(ExcavationSite.ARTIFACT_ASSEMBLY),
    SPU = s.upgradeLevel(ExcavationSite.SAFETY_PROTOCOL);
  
  float success = 1;
  if (actor.skills.test(ASSEMBLY, 10, 1)) success++;
  else success--;
  if (actor.skills.test(ANCIENT_LORE, 5, 1)) success++;
  else success--;

  site.stocks.removeItem(Item.withAmount(sample, 1.0f));
  if (success >= 0) {
    success *= 1 + (AAU / 2f);
    final Item result = Item.with(ARTIFACTS, null, 0.1f, success * 2);
    site.stocks.addItem(result);
  }
  if (site.stocks.amountOf(ARTIFACTS) >= 10) {
    site.stocks.removeItem(Item.withAmount(ARTIFACTS, 10));
    final Item match = site.stocks.matchFor(Item.withAmount(ARTIFACTS, 1));
    final float quality = (match.quality + AAU + 2) / 2f;
    if (Rand.num() < 0.1f * match.quality / (1 + SPU)) {
      final boolean hostile = Rand.num() < 0.9f / (1 + SPU);
      releaseArtilect(actor, hostile, quality);
    }
    else createArtifact(site, quality);
  }
  //*/
/*
  return true;
}


private void releaseArtilect(Actor actor, boolean hostile, float quality) {
  //  TODO:  TEST AND RESTORE THIS
  /*
  final int roll = (int) (Rand.index(5) + quality);
  final Artilect released = roll >= 5 ? new Tripod() : new Drone();
  
  final World world = actor.world();
  if (hostile) {
    released.assignBase(world.baseWithName(Base.KEY_ARTILECTS, true, true));
  }
  else {
    released.assignBase(actor.base());
    released.mind.assignMaster(actor);
  }
  released.enterWorldAt(actor.aboard(), world);
  released.goAboard(actor.aboard(), world);
  //*/
/*
}


private void createArtifact(Venue site, float quality) {
  final TradeType basis = Rand.yes() ?
    (TradeType) Rand.pickFrom(ALL_DEVICES) :
    (TradeType) Rand.pickFrom(ALL_OUTFITS);
  //
  //  TODO:  Deliver to artificer for sale or recycling!
  final Item found = Item.with(ARTIFACTS, basis, 1, quality);
  site.stocks.addItem(found);
}
//*/



