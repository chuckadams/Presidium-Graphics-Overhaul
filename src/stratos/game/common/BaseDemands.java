/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.user.BaseUI;
import stratos.util.*;

import java.util.Map.Entry;



public class BaseDemands {
  
  
  
  /**  Data fields, construction, setup and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final Stage world;
  final Base base;
  final int size, patchSize, updatePeriod;
  
  final Table <Object, BlurMap>
    supply = new Table <Object, BlurMap> (),
    demand = new Table <Object, BlurMap> ();
  
  private Table allTables[] = { supply, demand };
  private Vec3D temp = new Vec3D();
  
  
  
  public BaseDemands(Base base, Stage world) {
    this.world        = world;
    this.base         = base;
    this.size         = world.size;
    this.patchSize    = Stage.PATCH_RESOLUTION;
    this.updatePeriod = Stage.STANDARD_DAY_LENGTH;
  }
  
  
  public void loadState(Session s) throws Exception {
    
    for (Table <Object, BlurMap> table : allTables) {
      for (int n = s.loadInt(); n-- > 0;) {
        final Object key = s.loadkey();
        final BlurMap map = mapForKeyFrom(table, key);
        map.loadState(s);
        table.put(key, map);
      }
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    
    for (Table <Object, BlurMap> table : allTables) {
      s.saveInt(table.size());
      for (Entry <Object, BlurMap> entry : table.entrySet()) {
        s.saveKey(entry.getKey());
        entry.getValue().saveState(s);
      }
    }
  }
  
  
  private BlurMap mapForKeyFrom(
    Table <Object, BlurMap> table, Object key
  ) {
    //
    //  For the sake of rapid access, we permit direct access to any maps that
    //  have been manufactured earlier by this demand-set:
    if (key instanceof BlurMap) {
      final BlurMap map = (BlurMap) key;
      if (map.parent == table) return map;
      else return mapForKeyFrom(table, map.key);
    }
    //
    //  Otherwise, we check to see if the map has been cached, and initialise a
    //  fresh map if absent:
    BlurMap map = table.get(key);
    if (map != null) return map;
    
    if (! Session.isValidKey(key)) {
      I.complain("INVALID MAP KEY: "+key);
      return null;
    }
    
    table.put(key, map = new BlurMap(size, patchSize, table, key));
    return map;
  }
  
  
  public BlurMap mapForSupply(Object key) {
    return mapForKeyFrom(supply, key);
  }
  
  
  public BlurMap mapForDemand(Object key) {
    return mapForKeyFrom(demand, key);
  }
  
  
  
  /**  Updates and modifications-
    */
  public void updateAllMaps(int period) {
    final boolean report = verbose && BaseUI.currentPlayed() == base;
    if (report) I.say("\nUPDATING BASE DEMANDS FOR "+base);
    
    for (BlurMap map : supply.values()) {
      map.updateAllValues(period * 1f / updatePeriod);
      if (report) {
        I.say("  Global supply for "+map.key+" is: "+map.globalValue());
      }
    }
    for (BlurMap map : demand.values()) {
      map.updateAllValues(period * 1f / updatePeriod);
      if (report) {
        I.say("  Global demand for "+map.key+" is: "+map.globalValue());
      }
    }
    
    if (report) for (Object o : supply.keySet()) {
      I.say("  Relative shortage for "+o+" is "+relativeGlobalShortage(o));
    }
  }
  
  
  public void impingeSupply(
    Object key, float amount, float period, Target at
  ) {
    final BlurMap map = mapForSupply(key);
    at.position(temp);
    if (period > 0) amount *= period / updatePeriod;
    map.impingeValue(amount, (int) temp.x, (int) temp.y);
  }
  
  
  public void impingeDemand(
    Object key, float amount, float period, Target at
  ) {
    final BlurMap map = mapForDemand(key);
    at.position(temp);
    if (period > 0) amount *= period / updatePeriod;
    map.impingeValue(amount, (int) temp.x, (int) temp.y);
  }
  
  
  
  /**  Global and localised supply-and-demand queries-
    */
  public float globalShortage(Object key) {
    final float GD = globalDemand(key), GS = globalSupply(key);
    return GD - GS;
  }
  
  
  public float relativeGlobalShortage(Object key) {
    final float GD = globalDemand(key), GS = globalSupply(key);
    if (GD <= 0) return 0;
    return 1 - (GS / GD);
  }
  
  
  public float localShortage(Target point, Object key) {
    final float
      DA = demandAround(point, key, -1),
      SA = supplyAround(point, key, -1),
      GS = relativeGlobalShortage(key);
    if (GS <= 0) return 0;
    return GS * (DA - SA);
  }
  
  //  TODO:  Implement relativeLocalShortage...?
  
  
  public float globalSupply(Object key) {
    return mapForSupply(key).globalValue();
  }
  
  
  public float globalDemand(Object key) {
    return mapForDemand(key).globalValue();
  }
  
  
  public float supplyAround(Target point, Object key, float radius) {
    return sampleMapFrom(supply, key, point, radius);
  }
  
  
  public float demandAround(Target point, Object key, float radius) {
    return sampleMapFrom(demand, key, point, radius);
  }
  
  
  private float sampleMapFrom(
    Table <Object, BlurMap> table, Object key, Target point, float radius
  ) {
    final BlurMap map = mapForKeyFrom(table, key);
    point.position(temp);
    float value = map.sampleValue(temp.x, temp.y);
    if (radius > 0) value *= (radius * radius) / (patchSize * patchSize);
    return value;
  }
}



