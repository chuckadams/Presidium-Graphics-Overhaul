/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import static stratos.game.wild.Fauna.BROWSER_TO_FLORA_RATIO;
import static stratos.game.wild.Fauna.PREDATOR_TO_PREY_RATIO;

import stratos.game.base.BaseDemands;
import stratos.game.common.*;
import stratos.game.verse.Faction;
import stratos.game.wild.*;
import stratos.user.BaseUI;
import stratos.util.*;



public class Ecology {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final Stage world;
  final int SR, SS;
  
  final RandomScan growthMap;
  final public Ambience ambience;
  
  private List <Species> allSpecies = new List();
  final Tally <Species> idealNumbers  = new Tally();
  final Tally <Species> actualNumbers = new Tally();
  
  
  
  public Ecology(final Stage world) {
    this.world = world;
    SR = Stage.PATCH_RESOLUTION;
    SS = world.size / SR;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)); }
    };
    ambience = new Ambience(world);
  }
  
  
  public void loadState(Session s) throws Exception {
    growthMap.loadState(s);
    ambience .loadState(s);
    
    s.loadObjects(allSpecies);
    s.loadTally(idealNumbers );
    s.loadTally(actualNumbers);
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s);
    ambience .saveState(s);
    
    s.saveObjects(allSpecies);
    s.saveTally(idealNumbers );
    s.saveTally(actualNumbers);
  }
  
  
  
  /**  Continuous updates-
    */
  public void updateEcology() {
    
    final int   size = world.size;
    final float time = world.currentTime();
    
    float growIndex = (time % Stage.GROWTH_INTERVAL);
    growIndex *= size * size * 1f / Stage.GROWTH_INTERVAL;
    growthMap.scanThroughTo((int) growIndex);
    
    updateAnimalCrowdEstimates();
  }
  
  
  public void includeSpecies(Species... species) {
    for (Species s : species) allSpecies.include(s);
  }
  
  
  public void updateTrophicPresence(Fauna fauna, int period) {
    checkMapsInit();
    final Tile    at = fauna.origin();
    final Species s  = fauna.species;
    abundances.impingeSupply(s             , s.metabolism(), period, at);
    abundances.impingeSupply(s.trophicKey(), s.metabolism(), period, at);
    abundances.impingeSupply(Fauna.class   , s.metabolism(), period, at);
  }
  
  
  public void updateAnimalCrowdEstimates() {
    final boolean report = false;//BaseUI.currentPlayed() == base;
    checkMapsInit();
    
    float numBrowseS = 0, numOtherS = 0;
    for (Species s : allSpecies) {
      if (! s.predator()) {
        if (s.preyedOn()) numBrowseS++;
        else numBrowseS += 0.5f;
      }
      else numOtherS++;
    }
    
    float sumFlora = world.terrain().globalFertility();
    float sumPreyedOn = 0, sumPredators = 0;
    idealNumbers .clear();
    actualNumbers.clear();
    
    for (Species s : allSpecies) if (! s.predator()) {
      float supports = sumFlora;
      supports /= s.metabolism() * numBrowseS * BROWSER_TO_FLORA_RATIO;
      if (! s.preyedOn()) supports /= 2;
      supports = Nums.round(supports, 1, false);
      idealNumbers.add(supports, s);
      if (s.preyedOn()) sumPreyedOn += supports * s.metabolism();
    }
    
    for (Species s : allSpecies) if (! s.browser()) {
      float supports = sumPreyedOn;
      supports /= s.metabolism() * numOtherS * PREDATOR_TO_PREY_RATIO;
      supports = Nums.round(supports, 1, false);
      idealNumbers.add(supports, s);
      if (s.predator()) sumPredators += supports * s.metabolism();
    }
    
    for (Species s : allSpecies) {
      actualNumbers.add(abundances.globalSupply(s), s);
    }
    
    if (report) {
      I.say("\nUpdating animal crowding-estimates:");
      I.say("  Sum preyed on: "+sumPreyedOn );
      I.say("  Sum predators: "+sumPredators);
      
      for (Species s : allSpecies) {
        I.say("  Ideal population of "+s+": "+idealNumbers.valueFor(s));
        I.say("  Real  population of "+s+": "+actualNumbers.valueFor(s));
      }
    }
  }
  
  
  private void growthAt(Tile t) {
    final Element owner = t.above();
    if (owner != null) owner.onGrowth(t);
    else Flora.tryGrowthAt(t, false);
    ambience.updateAt(t);
  }
  
  
  public float idealPopulation(Species s) {
    return idealNumbers.valueFor(s);
  }
  
  
  public float globalCrowding(Species s) {
    if (! idealNumbers.hasEntry(s)) return 1;
    final float idealNum  = idealNumbers .valueFor(s);
    final float actualNum = actualNumbers.valueFor(s);
    if (actualNum <= 0) return 0;
    if (idealNum  <= 0) return 1;
    return actualNum / idealNum;
  }
  
  
  
  /**  Querying sample values-
    */
  private BlurMap biomass = null;
  private BaseDemands abundances;
  
  private boolean checkMapsInit() {
    if (biomass != null) return true;
    abundances = Base.wildlife(world).demands;
    biomass    = abundances.mapForSupply("Biomass");
    return true;
  }
  
  
  public void impingeBiomass(Tile t, float amount, float duration) {
    checkMapsInit();
    abundances.impingeSupply(biomass, amount, duration, t);
  }
  
  
  public float biomassRating(Target t) {
    checkMapsInit();
    float sample = abundances.supplyAround(t, biomass, Stage.ZONE_SIZE) * 4;
    return sample / (Stage.ZONE_AREA * 4);
  }
  
  
  public float globalBiomass() {
    checkMapsInit();
    return abundances.globalSupply(biomass) * 4;
  }
  
  
  public float forestRating(Tile t) {
    checkMapsInit();
    float fertility = world.terrain().fertilitySample(t);
    float foresting = biomassRating(t) * 2 / Flora.MAX_GROWTH;
    if (fertility <= 0) return 1;
    return Nums.clamp(foresting / fertility, 0, 1);
  }
  
  
  
  /**  Terraforming methods-
    */
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
}












