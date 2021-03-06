/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Devices.*;
import static stratos.game.craft.Outfits.*;



public class Tripod extends Artilect {
  
  final public static ModelAsset
    MODEL_TRIPOD = MS3DModel.loadFrom(
      FILE_DIR, "Tripod.ms3d", Tripod.class,
      XML_FILE, "Tripod"
    );
  
  final public static Species SPECIES = new Species(
    Tripod.class,
    "Tripod",
    "Tripods are among the more feared of the artilect guardians wandering "+
    "the landscape.  Even in a decrepit state, they are well-armed and "+
    "will attack organics with scant provocation.",
    null,
    null,
    Species.Type.ARTILECT, 1, 1, 1
  ) {
    public Actor sampleFor(Base base) { return new Tripod(base); }
  };
  
  final String name;
  
  
  public Tripod(Base base) {
    super(base, SPECIES);
    
    traits.initAtts(30, 10, 5);
    health.initStats(
      100, //lifespan
      5.0f,//bulk bonus
      1.0f,//sight range
      0.4f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    );
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2));
    
    gear.setBaseDamage(20);
    gear.setBaseArmour(20);
    traits.setLevel(MARKSMANSHIP, 10 + Rand.index(5) - 2);
    traits.setLevel(HAND_TO_HAND, 10 + Rand.index(5) - 2);
    gear.equipDevice(Item.withQuality(INTRINSIC_BEAM     , 0));
    gear.equipOutfit(Item.withQuality(INTRINSIC_SHIELDING, 0));
    
    traits.setLevel(FEARLESS, 1);
    
    skills.addTechnique(SELF_ASSEMBLY    );
    skills.addTechnique(POSITRON_BEAM    );
    skills.addTechnique(SHIELD_ABSORPTION);
    
    attachSprite(MODEL_TRIPOD.makeSprite());
    name = nameWithBase("Tripod ");
  }
  
  
  public Tripod(Session s) throws Exception {
    super(s);
    name = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(name);
  }
  
  
  
  /**  Physical properties-
    */
  public float radius() {
    return 0.5f;
  }
  
  
  public float height() {
    return 2.5f * super.height();
  }
  
  
  protected float aboveGroundHeight() {
    //  TODO:  Figure out why the model is above-ground!
    return -0.1f;
  }
  
  


  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name;
  }
  
  
  public Composite portrait(HUD UI) {
    return null;
  }
}




