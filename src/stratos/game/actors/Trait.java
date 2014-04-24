/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import stratos.game.common.*;
import stratos.util.*;



public class Trait implements Qualities, Session.Saveable {
  
  
  private static boolean verboseInit = false;
  
  static Batch <Trait>
    traitsSoFar = new Batch <Trait> (),
    allTraits   = new Batch <Trait> () ;
  
  static Trait[] from(Batch <Trait> types) {
    final Trait t[] = (Trait[]) types.toArray(Trait.class) ;
    types.clear() ;
    return t ;
  }
  
  static Skill[] skillsSoFar() {
    final Skill t[] = (Skill[]) traitsSoFar.toArray(Skill.class) ;
    traitsSoFar.clear() ;
    return t ;
  }
  
  static Trait[] traitsSoFar() {
    final Trait t[] = traitsSoFar.toArray(Trait.class) ;
    traitsSoFar.clear() ;
    return t ;
  }
  
  
  public static Trait loadConstant(Session s) throws Exception {
    return ALL_TRAIT_TYPES[s.loadInt()] ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(traitID) ;
  }
  
  
  private static int nextID = 0 ;
  final public int traitID ;
  
  final int type ;
  final int minVal, maxVal ;
  final String descriptors[] ;
  final int descValues[] ;
  
  private Trait correlates[];
  private float weightings[];
  
  
  
  protected Trait(int type, String... descriptors) {
    this.traitID = nextID++ ;
    this.type = type ;
    this.descriptors = descriptors ;
    this.descValues = new int[descriptors.length] ;
    if (verboseInit) I.say("\n  Initialising new trait");
    
    int zeroIndex = 0, min = 100, max = 0, val ;
    for (String s : descriptors) { if (s == null) break ; else zeroIndex++ ; }
    for (int i = descriptors.length ; i-- > 0 ;) {
      val = descValues[i] = zeroIndex - i ;
      
      final String desc = descriptors[i] ;
      if (verboseInit) {
        if (desc != null) I.say("  Value for "+desc+" is "+val) ;
        else I.say("  Empty value: "+val);
      }
      
      if (val > max) max = val ;
      if (val < min) min = val ;
    }
    this.minVal = min == max ? 0 : min ;
    this.maxVal = max ;
    
    if (verboseInit) I.say("  Min/max vals: "+minVal+"/"+maxVal);
    traitsSoFar.add(this) ;
    allTraits.add(this) ;
  }
  
  
  public static Trait loadFrom(Session s) throws Exception {
    final int ID = s.loadInt() ;
    if (ID == -1) return null ;
    return ALL_TRAIT_TYPES[ID] ;
  }
  
  
  public static void saveTo(Session s, Trait t) throws Exception {
    if (t == null) { s.saveInt(-1) ; return ; }
    s.saveInt(t.traitID) ;
  }
  
  
  public void affect(Actor a) {
  }
  
  
  protected void assignCorrelates(Trait t[], float w[]) {
    this.correlates = t;
    this.weightings = w;
  }
  
  
  protected float correlation(Trait other) {
    final int index = Visit.indexOf(other, correlates);
    return (index == -1) ? 0 : weightings[index];
  }
  
  
  
  /**  Returns the appropriate description for the given trait-level.
    */
  public static String descriptionFor(Trait trait, float level) {
    
    if (trait.descriptors.length == 1) {
      if (level == 0) return null;
      return (level > 0 ? "" : "Not ") + trait.descriptors[0];
    }
    
    String bestDesc = null ;
    float minDiff = Float.POSITIVE_INFINITY ;
    
    int i = 0 ; for (String s : trait.descriptors) {
      float value = trait.descValues[i] ;
      if (value > 0) value /= trait.maxVal;
      if (value < 0) value /= 0 - trait.minVal;
      
      final float diff = Math.abs(level - value) ;
      if (diff < minDiff) { minDiff = diff ; bestDesc = s ; }
      i++ ;
    }
    return bestDesc ;
  }
  
  
  public String toString() {
    return descriptionFor(this, 2) ;
  }
}



