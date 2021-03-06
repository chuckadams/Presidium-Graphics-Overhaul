



package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Pledge;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.wild.Fauna;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



public class Resting extends Plan {
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static int
    MODE_NONE  = -1,
    MODE_DINE  =  0,
    MODE_LODGE =  1,
    MODE_SLEEP =  2,
    RELAX_TIME =  Stage.STANDARD_HOUR_LENGTH / 2,
    DINE_TIME  =  Stage.STANDARD_HOUR_LENGTH / 2;
  
  final Owner restPoint;
  public int cost;
  
  private int currentMode = MODE_NONE;
  private float relaxTime = 0;
  
  
  public Resting(Actor actor, Target point) {
    super(actor, point, MOTIVE_LEISURE, NO_HARM);
    this.restPoint = (point instanceof Owner) ? (Owner) point : actor;
  }
  
  
  public Resting(Session s) throws Exception {
    super(s);
    this.restPoint = (Owner) s.loadObject();
    this.cost = s.loadInt();
    this.currentMode = s.loadInt();
    this.relaxTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(restPoint);
    s.saveInt(cost);
    s.saveInt(currentMode);
    s.saveFloat(relaxTime);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Resting(other, restPoint);
  }
  
  
  public static float sleepPriority(Actor actor) {
    final float fatigue = Nums.clamp(actor.health.fatigueLevel() + 0.25f, 0, 1);
    return fatigue * ROUTINE * 2;
  }
  
  
  public static void checkForWaking(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    
    final float fatigue = Nums.clamp(actor.health.fatigueLevel() + 0.25f, 0, 1);
    final Behaviour root = actor.mind.rootBehaviour();
    float wakePriority = root == null ? 0 : root.priority();
    wakePriority += CASUAL * actor.traits.relativeLevel(ABSTINENT);
    
    if (wakePriority <= 0) return;
    if ((wakePriority * (1 - fatigue)) > (fatigue * ROUTINE)) {
      if (report) I.say("\nWaking actor up...");
      actor.health.setState(ActorHealth.STATE_ACTIVE);
    }
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { RELAXED, CALM };
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && hasBegun() && verbose;
    if (report) {
      I.say("\nGetting resting priority for "+actor);
      I.say("  Begun? "+hasBegun());
    }
    
    float urgency = CASUAL;
    
    if (restPoint instanceof Venue) {
      final Venue venue = (Venue) restPoint;
      if (! venue.allowsEntry(actor)) return -1;
    }
    
    //  Include effects of fatigue-
    final float stress = Nums.clamp(
      actor.health.fatigueLevel () +
      actor.health.stressPenalty() +
      actor.health.injuryLevel  (),
      0, 1
    );
    if (report) {
      I.say("  Stress level: "+stress);
      I.say("  Rest point:   "+restPoint);
    }
    
    if (stress < 0.5f) {
      urgency *= stress * 2;
    }
    else {
      final float f = (stress - 0.5f) * 2;
      urgency = (urgency * (1 - f)) + (PARAMOUNT * f);
    }
    
    //  Include effects of hunger-
    float sumFood = 0, hunger = Nums.clamp(actor.health.hungerLevel(), 0, 1);
    for (Traded s : menuFor(actor, restPoint)) {
      sumFood += restPoint.inventory().amountOf(s);
    }
    for (Traded s : menuFor(actor, actor)) {
      sumFood += actor.inventory().amountOf(s);
    }
    if (sumFood > 1) sumFood = 1;
    urgency += hunger * sumFood * PARAMOUNT;
    
    //  Include pricing effects-
    if (cost > 0) {
      if (cost > actor.gear.allCredits() / 2) urgency -= ROUTINE;
      urgency -= actor.motives.greedPriority(cost);
    }
    
    //  Include day/night effects, but only if off-duty:
    float nightVal = (1 - Planet.dayValue(actor.world())) * 2 * CASUAL;
    final Property work = actor.mind.work();
    if (work != null && work.staff().onShift(actor)) nightVal = 0;
    urgency += nightVal;
    
    //  Include location effects-
    if (restPoint == actor && ! actor.indoors()) {
      urgency = Nums.clamp(urgency - CASUAL, 0.1f, PARAMOUNT);
    }
    
    if (report) {
      I.say("  Relax time: "+relaxTime);
      I.say("  Urgency: "+urgency);
    }
    
    //  TODO:  INCLUDE LAZINESS!
    return urgency;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor;
    if (restPoint == null) return null;
    
    //  TODO:  Split dining off into a separate behaviour?
    if (hasMenu(restPoint) || hasMenu(actor)) {
      if (actor.health.hungerLevel() > 1.1f - ActorHealth.MAX_CALORIES) {
        final Action eats = new Action(
          actor, restPoint,
          this, "actionEats",
          Action.BUILD, "Eating at "+restPoint
        );
        currentMode = MODE_DINE;
        return eats;
      }
    }
    //
    //  If you're tired, put your feet up.
    if (actor.health.fatigueLevel() > Planet.dayValue(actor.world()) + 0.1f) {
      currentMode = MODE_SLEEP;
    }
    else if (relaxTime > (Rand.num() + 1) * RELAX_TIME) {
      return null;
    }
    else currentMode = MODE_LODGE;
    final Action relax = new Action(
      actor, restPoint,
      this, "actionRest",
      Action.FALL, "Resting at "+restPoint
    );
    relax.setProperties(Action.NO_LOOP);
    return relax;
  }
  
  
  public boolean actionRest(Actor actor, Owner place) {
    //
    //  If you're resting at home, deposit any taxes due and transfer any
    //  incidental groceries-
    if (place == actor.mind.home() && place instanceof Venue) {
      Audit.payIncomeTax(actor, (Venue) place);
    }
    //
    //  Otherwise, pay any initial fees required-
    else if (cost > 0) {
      place.inventory().incCredits(cost);
      actor.gear.incCredits(0 - cost);
      cost = 0;
    }
    if (currentMode == MODE_SLEEP) {
      actor.health.setState(ActorHealth.STATE_RESTING);
    }
    else {
      //  TODO:  Improve morale?
      float relief = actor.health.maxHealth() / Stage.STANDARD_DAY_LENGTH;
      actor.health.liftFatigue(relief);
      relaxTime += 1.0f;
    }
    return true;
  }

  
  public boolean actionEats(Actor actor, Owner place) {
    return dineFrom(actor, place) || dineFrom(actor, actor);
  }
  
  
  
  /**  Utility methods to support dining-
    */
  private static Batch <Traded> menuFor(Actor actor, Owner place) {
    Batch <Traded> menu = new Batch <Traded> ();
    for (Traded type : actor.species().canEat()) {
      if (place.inventory().amountOf(type) >= 0.1f) menu.add(type);
    }
    return menu;
  }
  
  
  private boolean hasMenu(Owner place) {
    return menuFor(actor, place).size() > 0;
  }
  
  
  public static boolean dineFrom(Actor actor, Owner stores) {
    final Batch <Traded> menu = menuFor(actor, stores);
    final float minHunger = 1.1f - ActorHealth.MAX_CALORIES;
    if (menu.empty() || actor.health.hungerLevel() <= minHunger) return false;
    
    final int FTC = ActorHealth.FOOD_TO_CALORIES;
    float bite = ActorHealth.DEFAULT_HEALTH * 1f / (DINE_TIME * FTC);
    bite /= menu.size();
    float sumFood = 0, sumTypes = 0;
    
    for (Traded type : menu) {
      final Item portion = Item.withAmount(type, bite);
      stores.inventory().removeItem(portion);
      sumFood += portion.amount;
      sumTypes++;
    }
    if (stores.inventory().amountOf(MEDICINE) > 0) {
      sumTypes++;
    }
    
    if (actor.species().animal()) sumTypes = 1;
    else sumTypes /= Economy.ALL_FOOD_TYPES.length;
    
    actor.health.takeCalories(sumFood * FTC, sumTypes);
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (restPoint == actor || restPoint == null) {
      if (currentMode == MODE_DINE) d.append("Eating");
      else d.append("Resting");
      return;
    }
    if (! actor.health.goodHealth()) {
      d.append(actor.health.stateDesc());
      return;
    }
    boolean aboard = actor.aboard() == restPoint;
    boolean isHome = restPoint == actor.mind.home();
    if (currentMode == MODE_DINE) {
      final String desc = aboard ? "Eating" : "Going to eat";
      d.appendAll(desc, " at ", restPoint);
    }
    else {
      String desc = aboard ?
        (isHome ? "Resting"       : "Sheltering"      ) :
        (isHome ? "Going to rest" : "Going to shelter")
      ;
      d.appendAll(desc, " at ", restPoint);
    }
  }
}











