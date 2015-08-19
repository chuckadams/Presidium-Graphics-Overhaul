/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class MissionResearch extends Mission {
  
  
  final Upgrade sought;
  
  
  public MissionResearch(
    Base base, Upgrade upgrade
  ) {
    super(base, upgrade, null, "Researching "+upgrade.name);
    this.sought = upgrade;
  }
  
  
  public MissionResearch(Session s) throws Exception {
    super(s);
    sought = (Upgrade) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(sought);
  }
  
  
  
  public float rateImportance(Base base) {
    return 1;
  }
  
  
  protected boolean shouldEnd() {
    return base.research.hasTheory(sought);
  }
  
  
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    
    final Studying study = Studying.asResearch(actor, sought, base);
    if (study == null) return null;
    study.addMotives(Plan.MOTIVE_MISSION, basePriority(actor));
    
    return cacheStepFor(actor, study);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected ImageAsset iconForMission(BaseUI UI) {
    return Mission.RESEARCH_ICON;
  }
  
  
  protected Composite compositeForSubject(BaseUI UI) {
    return Composite.withImage(sought.portraitImage(), "research_"+sought);
  }
  
  
  public void describeMission(Description d) {
    d.append("Researching ");
    d.append(sought);
    final float progLeft = base.research.researchRemaining(
      sought, BaseResearch.LEVEL_THEORY
    );
    d.append(" ("+(int) ((1 - progLeft) * 100)+"%)");
  }
  
  
  public String progressDescriptor() {
    final String progDesc = base.research.progressDescriptor(sought);
    final float progLeft = base.research.researchRemaining(
      sought, BaseResearch.LEVEL_THEORY
    );
    return (int) ((1 - progLeft) * 100)+"% complete ("+progDesc+")";
  }
}












