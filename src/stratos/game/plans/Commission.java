


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Pledge;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;


//  Purchase commodities for home.
//  Purchase a device/weapon, or outfit/armour.
//  Purchase rations, fuel cells, ammo or medkits.


public class Commission extends Plan {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    evalVerbose  = true ,
    stepsVerbose = true ;
  
  final static int EXPIRE_TIME = Stage.STANDARD_DAY_LENGTH * 2;
  
  final Item item;
  final Venue shop;
  
  private float   price     = -1;
  private float   orderDate = -1;
  private boolean delivered = false;
  
  
  private Commission(Actor actor, Item baseItem, Venue shop) {
    super(actor, shop, MOTIVE_PERSONAL, NO_HARM);
    this.item = Item.withReference(baseItem, actor);
    this.shop = shop;
    
    if (item.type.materials() == null) {
      I.complain("COMMISSIONED ITEMS MUST HAVE A CONVERSION SPECIFIED!");
    }
  }
  
  
  public Commission(Session s) throws Exception {
    super(s);
    item = Item.loadFrom(s);
    shop = (Venue) s.loadObject();
    orderDate = s.loadFloat();
    delivered = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo(s, item);
    s.saveObject(shop);
    s.saveFloat(orderDate);
    s.saveBool(delivered);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    return ((Commission) p).item.type == this.item.type;
  }
  
  
  
  /**  Assessing and locating targets-
    */
  //  TODO:  Specify the actor who buys and the actor it's intended for
  //  separately!
  
  public static void addCommissions(
    Actor actor, Venue makes, Choice choice, Traded... itemTypes
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nChecking commissions for "+actor);
    
    final boolean hasCommission = actor.mind.hasToDo(Commission.class);
    if (hasCommission) return;
    
    if (Visit.arrayIncludes(itemTypes, actor.gear.deviceType())) {
      choice.add(nextCommission(actor, makes, actor.gear.deviceEquipped()));
    }
    if (Visit.arrayIncludes(itemTypes, actor.gear.outfitType())) {
      choice.add(nextCommission(actor, makes, actor.gear.outfitEquipped()));
    }
  }
  
  
  private static Commission nextCommission(
    Actor actor, Venue makes, Item baseItem
  ) {
    if (baseItem == null || ! makes.isManned()) return null;

    final boolean report = evalVerbose && I.talkAbout == actor;
    final int baseQuality = (int) baseItem.quality;
    
    int quality = Item.MAX_QUALITY + 1;
    Commission added = null;
    Item upgrade = null;
    
    while (--quality > 0) {
      upgrade = Item.withQuality(baseItem.type, quality);
      final float price = upgrade.priceAt(makes);
      if (price >= actor.gear.allCredits()) continue;
      if (quality <= baseQuality) continue;
      
      added = new Commission(actor, upgrade, makes);
      if (added.priorityFor(actor) <= 0) continue;
      break;
    }
    
    if (report) {
      I.say("\nConsidering commission for "+baseItem);
      I.say("  Owner cash:   "+actor.gear.allCredits());
      I.say("  New item:     "+upgrade);
      I.say("  Base price:   "+upgrade.defaultPrice());
      I.say("  Vended price: "+upgrade.priceAt(makes));
      if (added == null) I.say("  Can't afford replacement!");
    }
    return added;
  }
  
  
  final static Trait BASE_TRAITS[] = { ACQUISITIVE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("Getting priority for commission of "+item);
    //
    //  See if we're still waiting on completion-
    final boolean done = shop.stocks.hasItem(item);
    if (orderDate != -1 && ! done) {
      if (report) I.say("  Manufacture not complete.");
      return 0;
    }
    //
    //  Include effects of pricing and quality-
    final float price = calcPrice();
    float modifier = item.quality * ROUTINE * 1f / Item.MAX_QUALITY;
    if (orderDate == -1) {
      if (price > actor.gear.allCredits()) {
        if (report) I.say("  Can't afford item.");
        return 0;
      }
      modifier -= actor.motives.greedPriority(price / ITEM_WEAR_DURATION);
    }
    
    final float priority = priorityForActorWith(
      actor, shop,
      ROUTINE, modifier,
      MILD_HELP, MILD_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("  Price value:      "+price   );
      I.say("  Manufacture done: "+done    );
      I.say("  Final priority:   "+priority);
    }
    return Nums.clamp(priority, 0, ROUTINE);
  }
  
  
  private float calcPrice() {
    if (price != -1) return price;
    
    price = item.priceAt(shop);
    final Conversion m = item.type.materials();
    if (m != null) for (Item i : m.raw) price += i.priceAt(shop);
    
    return price;
  }
  
  
  private boolean expired() {
    if (orderDate == -1 || shop.stocks.hasItem(item)) return false;
    if (actor.world().currentTime() - orderDate > EXPIRE_TIME) {
      return true;
    }
    return ! shop.stocks.hasOrderFor(item);
  }
  
  
  public boolean finished() {
    return delivered;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (finished() || item.type.materials() == null) return null;
    
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next commission step for "+actor);

    if (! shop.isManned()) return null;
    if (! shop.structure.intact()) return null;
    
    if (expired()) {
      if (report) I.say("  Getting refund: "+(int) calcPrice()+" credits");
      final Action refund = new Action(
        actor, shop,
        this, "actionCollectRefund",
        Action.TALK_LONG, "Getting refund for "
      );
      return refund;
    }
    
    if (shop.stocks.hasItem(item)) {
      if (report) I.say("  Picking up "+item);
      final Action pickup = new Action(
        actor, shop,
        this, "actionPickupItem",
        Action.REACH_DOWN, "Collecting "
      );
      return pickup;
    }
    
    if (! shop.stocks.hasOrderFor(item)) {
      if (report) I.say("  Placing order for "+item);
      final Action placeOrder = new Action(
        actor, shop,
        this, "actionPlaceOrder",
        Action.TALK_LONG, "Placing order for "
      );
      return placeOrder;
    }
    return null;
  }
  
  
  public boolean actionPlaceOrder(Actor actor, Venue shop) {
    if (shop.stocks.hasOrderFor(item)) return false;
    
    shop.stocks.addSpecialOrder(item);
    orderDate = shop.world().currentTime();
    
    final int price = (int) calcPrice();
    shop .inventory().incCredits(    price);
    actor.inventory().incCredits(0 - price);
    return true;
  }
  
  
  public boolean actionPickupItem(Actor actor, Venue shop) {
    shop.inventory().removeMatch(item);
    actor.inventory().addItem(item);
    delivered = true;
    return true;
  }
  
  
  public boolean actionCollectRefund(Actor actor, Venue shop) {
    final int price = (int) calcPrice();
    shop .inventory().incCredits(0 - price);
    actor.inventory().incCredits(    price);
    shop.stocks.deleteSpecialOrder(item);
    delivered = true;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (shop.stocks.hasOrderFor(item) && ! shop.stocks.hasItem(item)) {
      d.append("Collecting ");
      item.describeFor(actor, d);
      d.append(" at ");
      d.append(shop);
      return;
    }
    if (super.needsSuffix(d, "Ordering ")) {
      item.describeFor(actor, d);
      d.append(" at ");
      d.append(shop);
    }
  }
}






