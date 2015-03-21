


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class Drone extends Artilect {
  
  
  /**  Construction and save/load methods-
    */
  final static Species SPECIES = new Species(
    Drone.class,
    "Drone",
    "Defence Drones are simple, disposable automatons capable of limited "+
    "field operations without supervision.",
    null,
    null,
    Type.ARTILECT, 1, 1, 1
  ) {
    public Actor sampleFor(Base base) { return new Drone(base); }
  };

  final public static ModelAsset
    MODEL_DEFENCE_DRONE = MS3DModel.loadFrom(
      FILE_DIR, "DefenceDrone.ms3d", Drone.class,
      XML_FILE, "Defence Drone"
    ),
    MODEL_RECON_DRONE = MS3DModel.loadFrom(
      FILE_DIR, "ReconDrone.ms3d", Drone.class,
      XML_FILE, "Recon Drone"
    ),
    MODEL_BLAST_DRONE = MS3DModel.loadFrom(
      FILE_DIR, "BlastDrone.ms3d", Drone.class,
      XML_FILE, "Blast Drone"
    ),
    DRONE_MODELS[] = {
      MODEL_DEFENCE_DRONE, MODEL_RECON_DRONE, MODEL_BLAST_DRONE
    };
  
  
  final String name;
  
  
  public Drone(Base base) {
    super(base, SPECIES);
    
    traits.initAtts(15, 10, 5);
    health.initStats(
      10,  //lifespan
      0.65f,//bulk bonus
      1.00f,//sight range
      1.25f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    );
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2));

    gear.setBaseDamage(10);
    gear.setBaseArmour(10);
    traits.setLevel(MARKSMANSHIP     , 5  + Rand.index(5) - 2);
    traits.setLevel(STEALTH_AND_COVER, 10 + Rand.index(5) - 2);
    gear.equipDevice(Item.withQuality(Economy.INTRINSIC_BEAM  , 0));
    gear.equipOutfit(Item.withQuality(Economy.INTRINSIC_ARMOUR, 0));
    
    traits.setLevel(CURIOUS, 1);
    
    final ModelAsset model = DRONE_MODELS[Rand.index(3)];
    attachSprite(model.makeSprite());
    name = nameWithBase("Drone ");
  }
  
  
  public Drone(Session s) throws Exception {
    super(s);
    name = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(name);
  }
  
  
  
  /**  Physical properties-
    */
  public float aboveGroundHeight() {
    return health.conscious() ? 0.5f : 0;
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
}



