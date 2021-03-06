/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.solids.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.actors.Backgrounds.*;




public abstract class Species extends Background {
  
  
  /**  Type, instance and media definitions-
    */
  //  TODO:  Include these as arguments as one would for normal Backgrounds!
  protected static enum Stat {
    BULK, SIGHT, SPEED,
    HEALTH, SENSES, COGNITION,
    ARMOUR, DAMAGE
  }
  public static enum Type {
    BROWSER ,
    PREDATOR,
    VERMIN  ,
    FLORA   ,
    SAPIENT ,
    ARTILECT,
  }
  final public static String
    KEY_FLORA     = "FLORA"    ,
    KEY_PREYED_ON = "PREYED_ON",
    KEY_PREDATOR  = "PREDATOR" ,
    KEY_NON_PREY  = "NON_PREY" ;
  
  
  
  /**  Lists and enumeration-
    */
  final public static Species
    HUMANOID_SPECIES[] = {
      Human.SPECIES
    },
    ANIMAL_SPECIES[] = {
      Qudu.SPECIES, Hareen.SPECIES, Lictovore.SPECIES, Yamagur.SPECIES
    },
    VERMIN_SPECIES[] = {
      Roach.SPECIES, Roachman.SPECIES, Avrodil.SPECIES
    },
    ARTILECT_SPECIES[] = {
      Drone.SPECIES, Tripod.SPECIES, Cranial.SPECIES
    }
  ;
  
  
  /**  Fields and constructors.
    */
  final public String name, info;
  final public ImageAsset portrait;
  final public ModelAsset modelSequence[];
  
  final public Type type;
  private Item stageNutrients[][];
  
  //  TODO:  Use a table filled with generic string keys, so that it's more
  //  self-descriptive?
  final public float
    baseBulk, speedMult, baseSight;
  final public float
    growRate, waterNeed;
  final public boolean
    domesticated;
  
  
  public Species(
    Class baseClass,
    String name, String info, String portraitTex, ModelAsset model,
    Type type, float bulk, float speedMult, float sight
  ) {
    super(
      baseClass,
      name, info, null, null,
      NOT_A_CLASS, NOT_A_GUILD
    );
    if (portraitTex == null) this.portrait = null;
    else this.portrait = ImageAsset.fromImage(
      baseClass, name+"_portrait_img", portraitTex
    );
    this.name  = name ;
    this.info  = info ;
    this.modelSequence = new ModelAsset[] { model };
    
    this.type      = type     ;
    this.baseBulk  = bulk     ;
    this.speedMult = speedMult;
    this.baseSight = sight    ;
    
    this.growRate  = 1.0f;
    this.waterNeed = 0.5f;
    
    this.stageNutrients = new Item[4][1];
    for (int i = 4; i-- > 0;) {
      float meat = i * bulk / 4f;
      stageNutrients[i] = new Item[] { Item.withAmount(PROTEIN, meat) };
    }
    this.domesticated = false;  //  TODO:  FIX THIS
  }
  
  
  protected Species(
    Class baseClass,
    String name, String info, String portraitTex, ModelAsset sequence[],
    Type type, float growRate, float waterNeed, boolean domesticated,
    Object... args
  ) {
    super(
      baseClass,
      name, info, null, null,
      NOT_A_CLASS, NOT_A_GUILD
    );
    
    if (portraitTex == null) this.portrait = null;
    else this.portrait = ImageAsset.fromImage(
      baseClass, name+"_portrait_img", portraitTex
    );
    this.name  = name;
    this.info  = name;
    this.modelSequence = sequence;
    
    int amount = 0;
    Batch <Item> n = new Batch <Item> ();
    for (Object o : args) {
      if (o instanceof Integer) amount = (Integer) o;
      if (o instanceof Traded ) n.add(Item.withAmount((Traded) o, amount));
    }
    
    this.type         = type;
    this.baseBulk     = 1;
    this.speedMult    = 0;
    this.baseSight    = 0;
    this.growRate     = growRate;
    this.waterNeed    = waterNeed;
    this.domesticated = domesticated;
    
    final Item nutrients[] = n.toArray(Item.class);
    this.stageNutrients = new Item[4][nutrients.length];
    for (int i = 4; i-- > 0;) for (int l = nutrients.length; l-- > 0;) {
      final Item base = nutrients[l];
      final float mult = (i + 1) / 4f;
      stageNutrients[i][l] = Item.withAmount(base, base.amount * mult);
    }
  }
  
  
  protected Actor init(Actor f) {
    f.health.setupHealth(Rand.num(), 0.9f, 0.1f);
    f.health.setCaloryLevel((1.5f + Rand.num()) / 2);
    f.relations.setupRelation(f.base(), 0.5f, 0);
    return f;
  }
  
  
  public Blueprint nestBlueprint() { return null; }
  public boolean   fixedNesting () { return true; }
  
  public boolean browser () { return type == Type.BROWSER ; }
  public boolean predator() { return type == Type.PREDATOR; }
  public boolean sapient () { return type == Type.SAPIENT ; }
  public boolean vermin  () { return type == Type.VERMIN  ; }
  public boolean artilect() { return type == Type.ARTILECT; }
  public boolean floral  () { return type == Type.FLORA   ; }
  
  public boolean animal  () { return browser() || predator() || vermin(); }
  public boolean living  () { return sapient() || animal(); }
  public boolean preyedOn() { return browser(); }
  
  
  /**  Metabolism and trophic levels-
    */
  public Item[] nutrients(int stage) {
    return stageNutrients[Nums.clamp(stage, 4)];
  }
  
  
  public float metabolism() {
    return baseBulk * speedMult;
  }
  
  
  final static Traded[]
    MEAT_ONLY  = new Traded[] { PROTEIN },
    VEGETABLES = new Traded[] { CARBS, GREENS },
    NONE_EATEN = new Traded[0];
  
  
  public Traded[] canEat() {
    if (predator()) return MEAT_ONLY;
    if (browser ()) return VEGETABLES;
    if (living  ()) return Economy.ALL_FOOD_TYPES;
    return NONE_EATEN;
  }
  
  
  public String trophicKey() {
    if      (floral()  ) return KEY_FLORA    ;
    else if (preyedOn()) return KEY_PREYED_ON;
    else if (predator()) return KEY_PREDATOR ;
    else                 return KEY_NON_PREY ;
  }
}




/*
final public static Species
  //
  //  Friendlies first-
  HUMAN       = new Species("Human"),
  CHANGELING  = new Species("Changeling"),
  KRECH       = new Species("Krech"),
  JOVIAN      = new Species("Jovian"),
  //
  //  Insectiles-
  GIANT_ROACH = new Species("Giant Roach"),
  ROACHMAN    = new Species("Roachman"),
  ARAK_LANCER = new Species("Arak Lancer"),
  TERMAGANT   = new Species("Termagant"),
  //
  //  Browsers and Predators-
  QUD         = new Species("Qud"), //X
  HIREX       = new Species("Hirex"),
  LORGOX      = new Species("Trigox"),
  HAREEN      = new Species("Hareen"), //X
  DRIVODIL    = new Species("Drivodil"),
  GIGANS      = new Species("Gigans"), //Y
  LICTOVORE   = new Species("Lictovore"), //X
  DESERT_MAW  = new Species("Desert Maw"),
  CYAN_CLADE  = new Species("Cyan Clade"),
  //
  //  Artilects-
  DRONE       = new Species("Drone"), //Y
  TRIPOD      = new Species("Tripod"), //Y
  CRANIAL     = new Species("Cranial"), //Y
  MANIFOLD    = new Species("Manifold"),
  ORACLE      = new Species("Oracle"),
  OBELISK     = new Species("Obelisk"),
  //
  //  Silicates-
  REM_LEECH   = new Species("Rem Leech"),
  SILVER_HULK = new Species("Silver Hulk"),
  AGGREGANT   = new Species("Aggregant"),
  ARCHON      = new Species("Archon")
;
//*/
