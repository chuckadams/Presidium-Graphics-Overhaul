/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.verse.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.start.*;
import stratos.user.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;


//  TODO:  You could allow configuration of what items to take from within
//  the mission-pane.

public class MissionRecover extends Mission {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  
  public MissionRecover(Base base, Element subject) {
    super(base, subject, CLAIM_MODEL, "Recovering "+subject);
  }
  

  public MissionRecover(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Target-screening and priority-evaluation-
    */
  public static MissionRecover recoveryFor(Object target, Base base) {
    if (target instanceof Actor) {
      final Actor captive = (Actor) target;
      if (captive.base() == base) return null;
      return new MissionRecover(base, captive);
    }
    if (target instanceof Owner) {
      //  TODO:  Implement this!
      return null;
      /*
      final Owner mark = (Owner) target;
      if (mark.base() == base || ! (mark instanceof Element)) return null;
      return new MissionRecover(base, (Element) mark);
      //*/
    }
    if (target instanceof Element) {
      final Element mark = (Element) target;
      if (mark.base() == base) return null;
      return new MissionRecover(base, mark);
    }
    return null;
  }
  
  
  public float targetValue(Base base) {
    return 0;
  }
  
  
  public boolean resolveMissionOffworld() {
    return false;
  }
  
  
  public float harmLevel() {
    if (subject instanceof Actor) return Plan.MILD_HARM; 
    else return Plan.NO_HARM;
  }
  
  
  public float rateCompetence(Actor actor) {
    return 0;
  }
  
  
  
  
  /**  Behaviour implemetation-
    */
  protected Behaviour createStepFor(Actor actor) {
    final Property store = base().HQ();
    
    if (subject instanceof Actor) {
      final Actor captive = (Actor) subject;
      if (CombatUtils.isDowned(captive, Combat.OBJECT_SUBDUE)) {
        return new BringPerson(actor, captive, store);
      }
      else {
        final Combat capture = new Combat(
          actor, captive, Combat.STYLE_EITHER, Combat.OBJECT_SUBDUE
        );
        return capture;
      }
    }
    else {
      final Item sample = Item.withReference(Economy.SAMPLES, subject);
      return new Looting(actor, (Element) subject, sample, store);
    }
  }
  
  
  protected boolean shouldEnd() {
    final Property store = base().HQ();
    if (subject instanceof Actor) {
      final Actor captive = (Actor) subject;
      return captive.aboard() == store && captive.currentMount() == store;
    }
    else {
      final Item sample = Item.withReference(Economy.SAMPLES, subject);
      return store.inventory().hasItem(sample);
    }
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void describeMission(Description d) {
    d.appendAll("Recovering ", subject);
  }
}







