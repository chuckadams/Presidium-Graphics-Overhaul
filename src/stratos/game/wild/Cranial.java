


package stratos.game.wild ;
import stratos.game.actors.* ;
import stratos.game.common.* ;
import stratos.game.maps.*;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.solids.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;



//  TODO:  Add the grapple, silver plague and psy strike abilities.


public class Cranial extends Artilect implements Economy {
  
  
  /**  Construction and save/load methods-
    */
  final String name ;
  
  
  public Cranial(Base base) {
    super(base, Species.SPECIES_CRANIAL) ;
    
    traits.initAtts(10, 20, 30) ;
    health.initStats(
      1000,//lifespan
      1.5f,//bulk bonus
      1.0f,//sight range
      0.6f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;
    
    gear.setDamage(20) ;
    gear.setArmour(15) ;
    gear.equipOutfit(Item.withQuality(ARTILECT_ARMOUR, 0)) ;
    gear.equipDevice(Item.withQuality(INTRINSIC_MELEE_WEAPON, 0)) ;
    traits.setLevel(HAND_TO_HAND, 15) ;
    traits.setLevel(ANATOMY, 10) ;
    traits.setLevel(ASSEMBLY, 20) ;
    traits.setLevel(INSCRIPTION, 10) ;
    
    traits.setLevel(IMPASSIVE, 1);
    traits.setLevel(CRUEL, 1);
    
    attachModel(MODEL_CRANIAL) ;
    name = nameWithBase("Cranial ") ;
  }
  
  
  public Cranial(Session s) throws Exception {
    super(s) ;
    name = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveString(name) ;
  }
  
  
  
  /**  Physical properties-
    */
  public float aboveGroundHeight() {
    return 0.25f ;
  }
  
  
  public float height() {
    return 1.5f;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name ;
  }
  
  
  public String helpInfo() {
    return
      "Cranials are cunning, quasi-organic machine intelligences that direct "+
      "the efforts of their lesser brethren.  They appear to have a marked "+
      "propensity for tortuous experiments on living creatures.";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
  }
}


