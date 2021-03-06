/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;



public class Structure {
  
  
  /**  A couple of utility-uprades that might be used by any structure:
    */
  final public static Upgrade
    FACING_CHANGE = new Upgrade(
      "facing_change", "Facing Change",
      0, Upgrade.SINGLE_LEVEL, null, null, Upgrade.Type.MISC_CHANGE, null
    );
  
  
  /**  Fields, definitions and save/load methods-
    */
  final public static int
    DEFAULT_INTEGRITY  =  100,
    DEFAULT_ARMOUR     =  2  ,
    DEFAULT_CLOAKING   =  0  ,
    DEFAULT_AMBIENCE   =  0  ,
    DEFAULT_LEVELS     = -1  ,
    DEFAULT_BUILD_COST = -1  ,
    
    STARTING_UPGRADES  = 4,
    UPGRADES_PER_LEVEL = 2;
  
  final public static float
    BURN_PER_SECOND = 1.0f,
    REGEN_PER_DAY   = 0.2f;
  final public static String
    DAMAGE_KEY = "damaged";
  
  final public static int
    STATE_NONE    =  0,
    STATE_INSTALL =  1,
    STATE_INTACT  =  2,
    STATE_SALVAGE =  3,
    STATE_RAZED   =  4;
  final static String STATE_DESC[] = {
    "N/A",
    "Installing",
    "Complete",
    "Salvaging",
    "N/A"
  };

  final static String UPGRADE_STATE_DESC[] = {
    "N/A",
    "Queued",
    "Installed",
    "Will Resign",
    "N/A"
  };
  
  final public static int
    IS_NORMAL  = 0  ,
    IS_VEHICLE = 1  ,
    IS_FIXTURE = 2  ,
    IS_LINEAR  = 4  ,
    IS_ZONED   = 8  ,
    IS_PUBLIC  = 16 ,
    IS_UNIQUE  = 32 ,
    IS_CRAFTED = 64 ,
    IS_ANCIENT = 128,
    IS_ORGANIC = 256;
  
  final static float UPGRADE_HP_BONUSES[] = {
    0,
    0.15f, 0.25f, 0.35f,
    0.4f , 0.45f, 0.5f ,
    0.5f , 0.55f, 0.55f, 0.6f , 0.6f , 0.65f
  };
  private static boolean verbose = false;
  
  
  final Placeable basis;
  private Blueprint blueprint = null;
  
  private int properties    = IS_NORMAL        ;
  private int baseIntegrity = DEFAULT_INTEGRITY;
  private int maxLevels     = DEFAULT_LEVELS   ;
  
  private Item materials[];
  private int
    buildCost     = 0                 ,
    armouring     = DEFAULT_ARMOUR    ,
    cloaking      = DEFAULT_CLOAKING  ,
    ambienceVal   = DEFAULT_AMBIENCE  ;
  
  private int     state     = STATE_INSTALL;
  private float   integrity = baseIntegrity;
  private boolean burning   = false        ;
  
  private float   upgradeProgress =  0  ;
  private int     upgradeIndex    = -1  ;
  private Upgrade upgrades[]      = null;
  private int     upgradeStates[] = null;
  
  
  
  
  Structure(Placeable basis) {
    this.basis = basis;
  }
  
  
  public void loadState(Session s) throws Exception {
    
    blueprint = (Blueprint) s.loadObject();
    
    baseIntegrity = s.loadInt();
    maxLevels     = s.loadInt();
    buildCost     = s.loadInt();
    armouring     = s.loadInt();
    cloaking      = s.loadInt();
    ambienceVal   = s.loadInt();
    properties    = s.loadInt();
    
    state     = s.loadInt()  ;
    integrity = s.loadFloat();
    burning   = s.loadBool() ;
    
    final int maxU = s.loadInt();
    if (maxU > 0) {
      upgradeProgress = s.loadFloat();
      upgradeIndex    = s.loadInt()  ;
      upgrades        = new Upgrade[maxU];
      upgradeStates   = new int    [maxU];
      for (int i = 0; i < maxU; i++) {
        upgrades     [i] = (Upgrade) s.loadObject();
        upgradeStates[i] = s.loadInt();
      }
    }
    
    materials = Item.loadItemsFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(blueprint);
    
    s.saveInt(baseIntegrity);
    s.saveInt(maxLevels    );
    s.saveInt(buildCost    );
    s.saveInt(armouring    );
    s.saveInt(cloaking     );
    s.saveInt(ambienceVal  );
    s.saveInt(properties   );

    s.saveInt  (state    );
    s.saveFloat(integrity);
    s.saveBool (burning  );
    
    if (upgrades != null) {
      s.saveInt  (upgrades.length);
      s.saveFloat(upgradeProgress);
      s.saveInt  (upgradeIndex   );
      for (int i = 0; i < upgrades.length; i++) {
        s.saveObject(upgrades     [i]);
        s.saveInt   (upgradeStates[i]);
      }
    }
    else s.saveInt(-1);
    
    Item.saveItemsTo(s, materials);
  }
  
  
  public void setupStats(Blueprint blueprint) {
    this.blueprint = blueprint;
    
    adjustStats(
      blueprint.integrity,
      blueprint.armour,
      DEFAULT_CLOAKING,
      DEFAULT_BUILD_COST,
      blueprint.numLevels(),
      blueprint.properties
    );
    addUpgrade(blueprint.baseUpgrade());
  }
  
  
  public void adjustStats(
    int baseIntegrity,
    int armouring,
    int cloaking,
    int buildCost,
    int maxLevels,
    int properties
  ) {
    final float condition = integrity * 1f / maxIntegrity();
    this.baseIntegrity = baseIntegrity;
    this.integrity = condition * maxIntegrity();
    
    this.armouring  = armouring;
    this.cloaking   = cloaking;
    this.buildCost  = buildCost;
    this.properties = properties;
    
    final float oldMaxL = this.maxLevels;
    this.maxLevels = maxLevels;
    if (maxLevels > 0 && maxLevels != oldMaxL) {
      final int maxUAL = this.upgradesArrayLength();
      this.upgrades      = new Upgrade[maxUAL];
      this.upgradeStates = new int    [maxUAL];
    }
  }
  

  public void updateStats(
    int baseIntegrity,
    int armouring,
    int cloaking
  ) {
    adjustStats(
      baseIntegrity, armouring, cloaking, buildCost, maxLevels, properties
    );
  }
  
  
  public void setAmbienceVal(float val) {
    this.ambienceVal = (int) val;
  }
  
  
  public void assignMaterials(Item... materials) {
    this.materials = materials;
  }
  
  
  
  /**  Regular updates-
    */
  protected void updateStructure(int numUpdates) {
    final boolean report = I.talkAbout == basis && verbose;
    if (report) {
      I.say("\nUpdating structure for "+basis);
      I.say("  State:     "+state);
      I.say("  Integrity: "+integrity+"/"+maxIntegrity());
    }
    
    final int CHECK_PERIOD = 10;
    if (integrity <= 0 && state != STATE_INSTALL) {
      adjustRepair(-1);
      return;
    }
    if (numUpdates % CHECK_PERIOD == 0) checkMaintenance();
    //
    //  Firstly, check to see if you're still burning-
    if (burning) {
      takeDamage(Rand.num() * 2 * BURN_PER_SECOND);
      final float damage = maxIntegrity() - integrity;
      if (armouring * 0.1f > Rand.num() * damage) burning = false;
      //  TODO:  Consider spreading to nearby structures?
    }
    //
    //  Then, check for gradual wear and tear-
    if (
      (numUpdates % CHECK_PERIOD == 0) &&
      takesWear() && (integrity > 0)
    ) {
      float wear = baseIntegrity / GameSettings.ITEM_WEAR_DAYS;
      wear *= DEFAULT_ARMOUR * 2f / (DEFAULT_ARMOUR + armouring);
      if (Blueprint.hasProperty(this, IS_FIXTURE)) wear /= 5;
      if (Blueprint.hasProperty(this, IS_CRAFTED)) wear *= 2;
      if (report) {
        I.say("  Taking wear...");
        I.say("  Wear per day: "+wear+"/"+baseIntegrity);
      }
      wear *= CHECK_PERIOD * 1f / Stage.STANDARD_DAY_LENGTH;
      takeDamage(wear * Rand.num() * 2);
    }
    //
    //  And finally, organic structures can regenerate health-
    if (regenerates()) {
      final float regen = baseIntegrity * REGEN_PER_DAY;
      repairBy(regen / Stage.STANDARD_DAY_LENGTH);
    }
  }
  
  
  
  /**  General state queries-
    */
  public int maxIntegrity() { return baseIntegrity + upgradeHP(); }
  public int currentState() { return state; }
  
  public int cloaking()  { return cloaking ; }
  public int armouring() { return armouring; }
  
  public int ambienceVal() { return intact() ? ambienceVal : 0; }
  
  public boolean intact()     { return state == STATE_INTACT; }
  public boolean destroyed()  { return state == STATE_RAZED ; }
  public int     buildState() { return state; }
  
  public float   repair()      { return integrity; }
  public float   repairLevel() { return integrity / maxIntegrity(); }
  public boolean burning()     { return burning; }
  
  
  public int buildCost() {
    if (blueprint == null || blueprint.baseUpgrade() == null) return buildCost;
    return blueprint.baseUpgrade().buildCost(basis.base());
  }
  
  
  protected int upgradesArrayLength() {
    if (blueprint == null) return 0;
    int total = maxLevels + (UPGRADES_PER_LEVEL * (maxLevels - 1));
    return STARTING_UPGRADES + total;
  }
  
  
  protected int upgradeHP() {
    if (upgrades == null) return 0;
    int numUsed = 0;
    for (int i = 0; i < upgrades.length; i++) {
      if (upgrades[i] != null && upgradeStates[i] != STATE_INSTALL) numUsed++;
    }
    if (numUsed == 0) return 0;
    return (int) (baseIntegrity * UPGRADE_HP_BONUSES[numUsed]);
  }
  
  
  public Upgrade blueprintUpgrade() {
    if (blueprint == null) return null;
    return blueprint.baseUpgrade();
  }
  
  
  public boolean flammable() {
    return ! isFixture();
  }
  
  
  public boolean takesWear() {
    if (regenerates()) return false;
    if (Blueprint.hasProperty(this, IS_ANCIENT)) return false;
    return true;
  }
  
  
  public boolean isMechanical() {
    if (Blueprint.hasProperty(this, IS_CRAFTED)) return false;
    if (Blueprint.hasProperty(this, IS_ORGANIC)) return false;
    return true;
  }
  
  
  public boolean regenerates() {
    return Blueprint.hasProperty(this, IS_ORGANIC);
  }
  
  
  public boolean isFixture() {
    return Blueprint.hasProperty(this, IS_FIXTURE);
  }
  
  
  public boolean isLinear() {
    return Blueprint.hasProperty(this, IS_LINEAR);
  }
  
  
  public int properties() {
    return properties;
  }
  
  
  
  /**  State Modifications-
    */
  public void beginSalvage() {
    if (state == STATE_SALVAGE || ! basis.inWorld()) return;
    if (GameSettings.buildFree) basis.setAsDestroyed(true);
    else setState(Structure.STATE_SALVAGE, -1);
  }
  
  
  public void cancelSalvage() {
    if (state == STATE_INTACT) return;
    setState(Structure.STATE_INTACT, -1);
  }
  
  
  public void completeSalvage() {
    ((Element) basis).setAsDestroyed(false);
    integrity = 0;
    checkMaintenance();
  }
  
  
  public void setState(int state, float condition) {
    this.state = state;
    if (condition >= 0) this.integrity = maxIntegrity() * condition;
    checkMaintenance();
  }
  
  
  public float repairBy(float inc) {
    final int max = maxIntegrity();
    final float oldI = this.integrity;
    if (inc < 0 && integrity > max) {
      inc = Nums.min(inc, integrity - max);
    }
    adjustRepair(inc);
    if (inc > Rand.num() * maxIntegrity()) burning = false;
    return (integrity - oldI) / max;
  }
  
  
  public void takeDamage(float damage) {
    if (basis.destroyed()) return;
    if (damage < 0) I.complain("NEGATIVE DAMAGE!");
    adjustRepair(0 - damage);
    
    float burnChance = 2 * (1f - repairLevel());
    if (! flammable()) burnChance -= 0.5f;
    if (burnChance > 0) burnChance *= damage / 100f;
    
    if (verbose && I.talkAbout == basis) I.say("Burn chance: "+burnChance);
    if (Rand.num() < burnChance) burning = true;
    
    if (integrity <= 0) {
      if (I.logEvents()) I.say("\n"+basis+" WAS DESTROYED, DAMAGE: "+damage);
      state = STATE_RAZED;
      completeSalvage();
      basis.setAsDestroyed(false);
    }
  }
  
  
  public void setBurning(boolean burns) {
    if (! flammable()) return;
    burning = burns;
  }
  
  
  protected void adjustRepair(float inc) {
    final int max = maxIntegrity();
    integrity = Nums.clamp(integrity + inc, 0, max);
    
    if (integrity >= max) {
      if (state == STATE_INSTALL) basis.onCompletion();
      if (state != STATE_SALVAGE) state = STATE_INTACT;
      integrity = max;
    }
    if (integrity <= 0) {
      if (state == STATE_SALVAGE) basis.setAsDestroyed(true);
      if (state != STATE_INSTALL) state = STATE_RAZED;
      integrity = 0;
    }
    checkMaintenance();
  }
  
  
  public boolean hasWear() {
    return (state != STATE_INTACT) || integrity < (maxIntegrity() - 1);
  }
  
  
  public boolean needsSalvage() {
    return state == STATE_SALVAGE || integrity > maxIntegrity();
  }
  
  
  public boolean needsUpgrade() {
    return nextUpgradeIndex() != -1;
  }
  
  
  public boolean goodCondition() {
    return
      (state == STATE_INTACT) && (! burning) &&
      ((1 - repairLevel()) < Repairs.MIN_SERVICE_DAMAGE);
  }
  
  
  protected void checkMaintenance() {
    final Stage world = basis.world();
    if (world == null || basis.isMobile()) return;
    final boolean report = verbose && I.talkAbout == basis;
    
    final Tile o = world.tileAt(basis);
    final boolean needs = (
      Repairs.needForRepair(basis) > Repairs.MIN_SERVICE_DAMAGE
    );
    
    final PresenceMap damaged = world.presences.mapFor(DAMAGE_KEY);
    if (report) {
      I.say(basis+" needs maintenance: "+needs);
      I.say("In map? "+damaged.hasMember(basis, o));
    }
    damaged.toggleMember(basis, o, needs);
  }
  
  
  
  
  /**  Handling upgrades-
    */
  private int nextUpgradeIndex() {
    if (upgrades == null) return -1;
    for (int i = 0; i < upgrades.length; i++) {
      if (upgrades[i] != null && upgradeStates[i] != STATE_INTACT) return i;
    }
    return -1;
  }
  
  
  private void deleteUpgrade(int atIndex) {
    final int LI = upgrades.length - 1;
    for (int i = atIndex; i++ < LI;) {
      upgrades[i - 1] = upgrades[i];
      upgradeStates[i - 1] = upgradeStates[i];
    }
    upgrades[LI] = null;
    upgradeStates[LI] = STATE_INSTALL;
  }
  
  
  public Upgrade upgradeInProgress() {
    if (upgradeIndex == -1) upgradeIndex = nextUpgradeIndex();
    if (upgradeIndex == -1) return null;
    return upgrades[upgradeIndex];
  }
  
  
  public void setUpgradeLevel(Upgrade upgrade, int level) {
    while (upgradeLevel(upgrade) < level) addUpgrade(upgrade);
  }
  
  
  public void addUpgrade(Upgrade upgrade) {
    if (upgrade == null) return;
    beginUpgrade(upgrade, false);
    advanceUpgrade(1.0f);
  }
  
  
  public float advanceUpgrade(float progress) {
    if (upgradeIndex == -1) upgradeIndex = nextUpgradeIndex();
    if (upgradeIndex == -1) return 0;
    //
    //  Update progress, and store the change for return later-
    final int US = upgradeStates[upgradeIndex];
    final float oldP = upgradeProgress;
    upgradeProgress = Nums.clamp(upgradeProgress + progress, 0, 1);
    float amount = upgradeProgress - oldP;
    if (US == STATE_SALVAGE) amount *= -0.5f;
    //
    //  If progress is complete, change the current upgrade's state:
    if (upgradeProgress >= 1) {
      final float condition = integrity * 1f / maxIntegrity();
      if (US == STATE_SALVAGE) deleteUpgrade(upgradeIndex);
      else upgradeStates[upgradeIndex] = STATE_INTACT;
      upgradeProgress = 0;
      upgradeIndex = -1;
      integrity = maxIntegrity() * condition;
    }
    return amount;
  }
  
  
  public void beginUpgrade(Upgrade upgrade, boolean checkExists) {
    int atIndex = -1;
    for (int i = 0; i < upgrades.length; i++) {
      if (checkExists && upgrades[i] == upgrade) return;
      if (upgrades[i] == null) { atIndex = i; break; }
    }
    if (atIndex == -1) I.complain("NO ROOM FOR UPGRADE!");

    upgrades[atIndex] = upgrade;
    upgradeStates[atIndex] = STATE_INSTALL;
    if (upgradeIndex == atIndex) upgradeProgress = 0;
    upgradeIndex = nextUpgradeIndex();
    checkMaintenance();
    
    if (GameSettings.techsFree) advanceUpgrade(1);
  }
  
  
  public void resignUpgrade(int atIndex, boolean instant) {
    if (upgrades[atIndex] == null) I.complain("NO SUCH UPGRADE!");
    if (instant) {
      upgrades[atIndex] = null;
      upgradeStates[atIndex] = STATE_NONE;
      if (upgradeIndex == atIndex) upgradeProgress = 0;
    }
    else {
      upgradeStates[atIndex] = STATE_SALVAGE;
      if (upgradeIndex == atIndex) upgradeProgress = 1 - upgradeProgress;
    }
    checkMaintenance();
  }
  
  
  public void resignUpgrade(Upgrade upgrade, boolean instant) {
    for (int i = upgrades.length; i-- > 0;) {
      if (upgrades[i] == upgrade) { resignUpgrade(i, instant); return; }
    }
  }
  
  
  public Batch <Upgrade> workingUpgrades() {
    final Batch <Upgrade> working = new Batch <Upgrade> ();
    if (upgrades == null) return working;
    for (int i = 0; i < upgrades.length; i++) {
      if (upgrades[i] != null && upgradeStates[i] == STATE_INTACT) {
        working.add(upgrades[i]);
      }
    }
    return working;
  }
  
  
  public int upgradeLevel(Object refers, int state) {
    if (upgrades == null || refers == null) return 0;
    int num = 0;
    for (int i = 0; i < upgrades.length; i++) {
      final Upgrade u = upgrades[i];
      if (u == null) continue;
      if (u != refers && u.refers != refers) continue;
      if (state != STATE_NONE && upgradeStates[i] != state) continue;
      num++;
    }
    return num;
  }
  
  
  public int upgradeLevel(Object refers) {
    return upgradeLevel(refers, STATE_INTACT);
  }
  
  
  public int upgradeOrQueuedLevel(Object refers) {
    return
      upgradeLevel(refers, STATE_INTACT ) +
      upgradeLevel(refers, STATE_INSTALL);
  }
  
  
  public boolean hasUpgrade(Upgrade type) {
    return hasUpgrade(type, 1);
  }
  
  
  public boolean hasUpgrade(Upgrade type, int level) {
    return upgradeLevel(type) >= level;
  }
  
  
  public float upgradeProgress(Upgrade upgrade) {
    if (upgrade != upgradeInProgress()) return 0;
    return upgradeProgress;
  }
  
  
  public Batch <Upgrade> queued(int state) {
    final Batch <Upgrade> queued = new Batch <Upgrade> ();
    for (int i = 0; i < upgrades.length; i++) {
      if (upgradeStates[i] == state) queued.add(upgrades[i]);
    }
    return queued;
  }
  

  
  /**  Methods for dealing with the venue's main upgrade-sequence (in contrast
    *  with optional tech-modules.)  A certain amount of internal data-space
    *  has to be 'reserved' for the former, and isn't visible externally.
    */
  public void setMainUpgradeLevel(int level) {
    if (blueprint == null || blueprint.venueLevels() == null) {
      I.complain("\nNO VENUE-LEVELS ASSOCIATED WITH "+basis);
      return;
    }
    final Upgrade levels[] = blueprint.venueLevels();
    while (level-- > 0) {
      if (level >= levels.length || hasUpgrade(levels[level], 1)) continue;
      addUpgrade(levels[level]);
    }
  }
  
  
  public int mainUpgradeLevel() {
    if (maxMainLevel() <= 0) return -1;
    int level = 0;
    for (Upgrade u : blueprint.venueLevels()) {
      if (hasUpgrade(u, 1)) level++;
    }
    return level;
  }
  
  
  public int maxMainLevel() {
    if (blueprint == null || blueprint.venueLevels() == null) {
      return -1;
    }
    return blueprint.venueLevels().length;
  }
  
  
  public boolean hasSpaceFor(Upgrade up) {
    if (upgrades == null || up.origin != blueprint) return false;
    if (up.type == Upgrade.Type.VENUE_LEVEL) return true;
    
    int used = 0;
    for (Upgrade u : upgrades) {
      if (u != null && u.type == Upgrade.Type.TECH_MODULE) used++;
    }
    return used < maxOptionalUpgrades();
  }
  
  
  public int maxOptionalUpgrades() {
    if (upgrades == null) return 0;
    final int level = mainUpgradeLevel();
    return STARTING_UPGRADES + ((level - 1) * UPGRADES_PER_LEVEL);
  }
  
  
  public int numOptionalUpgrades() {
    if (upgrades == null) return 0;
    int num = 0;
    for (int i = 0; i < upgrades.length; i++) {
      if (upgradeStates[i] != STATE_INTACT) continue;
      final Upgrade u = upgrades[i];
      if (u == null || u.type == Upgrade.Type.VENUE_LEVEL) continue;
      num++;
    }
    return num;
  }
  
  
  
  
  /**  Rendering and interface-
    */
  public Batch <String> descOngoingUpgrades() {
    final Batch <String> desc = new Batch <String> ();
    if (upgrades == null) return desc;
    for (int i = 0; i < upgrades.length; i++) {
      if (i == upgradeIndex) { desc.add(currentUpgradeDesc()); continue; }
      if (upgrades[i] == null || upgradeStates[i] == STATE_INTACT) continue;
      final String name = upgrades[i].nameAt(basis, i, upgrades);
      desc.add(name+" ("+UPGRADE_STATE_DESC[upgradeStates[i]]+")");
    }
    return desc;
  }
  
  
  public String currentUpgradeDesc() {
    if (upgradeIndex == -1) return null;
    final Upgrade u = upgrades[upgradeIndex];
    final String name = u.nameAt(basis, upgradeIndex, upgrades);
    return ""+name+" ("+(int) (upgradeProgress * 100)+"%)";
  }
}

















