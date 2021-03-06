/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import static stratos.game.craft.Economy.*;

import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.sfx.*;
import stratos.game.plans.Bringing;



/**  An inventory allows for the storage, transfer and tracking of discrete
  *  items.
  */
public class Inventory {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  private static Traded
    tracked = null;
  
  
  final public Owner owner;
  protected float credits, taxed;
  
  final Table <Item, Item> itemTable = new Table <Item, Item> (10);
  //  private int sumGoods;  //TODO:  Break into increments of 1/100th bulk?
  
  
  public Inventory(Owner owner) {
    this.owner = owner;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(itemTable.size());
    for (Item item : itemTable.values()) Item.saveTo(s, item);
    s.saveFloat(credits);
    s.saveFloat(taxed  );
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int i = s.loadInt(); i-- > 0;) {
      final Item item = Item.loadFrom(s);
      itemTable.put(item, item);
    }
    credits = s.loadFloat();
    taxed   = s.loadFloat();
  }
  
  
  
  /**  Financial balance-
    */
  public void incCredits(float inc) {
    if (inc > 0) {
      credits += inc;
    }
    else {
      final float charge = 0 - inc;
      credits -= charge;
      final float debt = 0 - credits;
      if (debt > 0) {
        taxed -= debt;
        credits = 0;
        if (taxed < 0) { credits = taxed; taxed = 0; }
      }
    }
  }
  
  
  public void transferCredits(float inc, Owner other) {
    incCredits(0 - inc);
    other.inventory().incCredits(inc);
  }
  
  
  public float allCredits() {
    return credits + taxed;
  }
  
  
  public float unTaxed() {
    return credits;
  }
  
  
  public float taxedCredits() {
    return taxed;
  }
  
  
  public void taxDone() {
    taxed += credits;
    credits = 0;
    //owner.afterTransaction();
  }
  
  
  
  /**  Finding matches and quantities-
    */
  public Item bestSample(
    Traded type, Session.Saveable refers, float maxAmount
  ) {
    return bestSample(Item.asMatch(type, refers), maxAmount);
  }
  
  
  public Item bestSample(Item match, float maxAmount) {
    final Batch <Item> matches = matches(match);
    Item best = null;
    float bestQuality = -1;
    for (Item i : matches) {
      if (i.quality > bestQuality) { bestQuality = i.quality; best = i; }
    }
    if (best == null) return null;
    if (maxAmount > 0 && best.amount > maxAmount) {
      best = Item.withAmount(best, maxAmount);
    }
    return best;
  }
  
  
  public Batch <Item> matches(Item item) {
    final Batch <Item> matches = new Batch <Item> (4);
    for (Item found : itemTable.values()) {
      if (item.matchKind(found)) matches.add(found);
    }
    return matches;
  }
  
  
  public Batch <Item> matches(Traded type) {
    final Batch <Item> matches = new Batch <Item> (4);
    for (Item found : itemTable.values()) {
      if (found.type == type) matches.add(found);
    }
    return matches;
  }
  
  
  public Item matchFor(Item item) {
    return itemTable.get(item);
  }
  
  
  public float amountOf(Item item) {
    if (item == null) return 0;
    if (item.isMatch()) {
      float amount = 0;
      for (Item found : itemTable.values()) {
        if (item.matchKind(found)) amount += found.amount;
      }
      return amount;
    }
    else {
      final Item found = itemTable.get(item);
      return found == null ? 0 : found.amount;
    }
  }
  
  
  public float amountOf(Traded type) {
    return amountOf(Item.asMatch(type, null));
  }
  
  
  public boolean empty() {
    return itemTable.size() == 0 && (credits + taxed) <= 0;
  }
  
  
  public Batch <Item> allItems() {
    final Batch <Item> allItems = new Batch <Item> ();
    for (Item item : itemTable.values()) allItems.add(item);
    return allItems;
  }
  
  
  public Traded[] allItemTypes() {
    final Batch <Traded> allTypes = new Batch <Traded> ();
    for (Item item : itemTable.keySet()) allTypes.include(item.type);
    return allTypes.toArray(Traded.class);
  }
  
  
  public int size() {
    return itemTable.size();
  }
  
  
  public void clearItems(Traded type) {
    if (verbose && type == tracked) I.say("CLEARING "+tracked);
    itemTable.remove(type);
  }
  
  
  public void removeAllItems() {
    itemTable.clear();
    credits = taxed = 0;
    //owner.afterTransaction();
  }
  
  
  
  /**  Adds the given item to the inventory.  If the item is one with 'free
    *  terms' used for matching purposes, returns false- only fully-specified
    *  items can be added.
    */
  public boolean addItem(Item item) {
    if (item.isMatch() || item.amount <= 0) {
      if (item.isMatch()) I.complain(
        "ADDING ILLEGAL ITEM: "+item+" amount/quality:"+
        item.amount+"/"+item.quality
      );
      new Exception().printStackTrace();
      return false;
    }
    //
    //  Check to see if a similar item already exists.  If so, blend the new
    //  quality with the old-
    final Item oldItem = itemTable.get(item);
    //final int oldAmount = oldItem == null ? 0 : (int) oldItem.amount;
    
    float amount = item.amount, quality = item.quality;
    if (oldItem != null) {
      itemTable.remove(oldItem);
      if (oldItem.quality != item.quality) {
        quality = (quality * amount) + (oldItem.amount * oldItem.quality);
        quality /= amount + oldItem.amount;
      }
      amount += oldItem.amount;
      if (verbose && oldItem.type == tracked) I.say("REPLACING "+tracked);
    }
    
    final Item entered = Item.with(item.type, item.refers, amount, quality);
    itemTable.put(entered, entered);
    
    if (owner != null) owner.afterTransaction(item, item.amount);
    return true;
  }
  
  
  public void bumpItem(Traded type, float amount) {
    if (amount == 0) return;
    if (amount > 0) addItem(Item.withAmount(type, amount));
    else removeItem(Item.withAmount(type, 0 - amount));
  }
  
  
  public void bumpItem(Traded type, float amount, int max) {
    final float oldAmount = amountOf(type);
    bumpItem(type, Nums.clamp(amount, 0 - oldAmount, max - oldAmount));
  }
  
  
  public void setAmount(Traded type, float amount) {
    final Item sets = Item.withAmount(type, amount);
    final Item match = matchFor(sets);
    
    if (verbose && type == tracked) I.say("SETTING "+tracked+" TO "+amount);
    if (match != null) itemTable.remove(match);
    if (amount > 0) itemTable.put(sets, sets);
  }
  
  
  
  /**  Removes the given item from this inventory.  The item given must have a
    *  single unique match, and the match must be greater in amount.  Returns
    *  false otherwise.
    */
  public boolean removeItem(Item item) {
    if (item.amount <= 0) {
      I.say("Removing null item... "+item.amount);
      new Exception().printStackTrace();
      return false;
    }
    //
    //  If the item isn't recorded, ignore the transaction.  If it does, but
    //  we don't have the quantity, delete the entry.
    final Item oldItem = itemTable.get(item);
    if (oldItem == null) return false;
    if (oldItem.amount <= item.amount) {
      if (verbose && oldItem.type == tracked) I.say("REMOVING "+tracked);
      itemTable.remove(item);
      if (owner != null) owner.afterTransaction(item, oldItem.amount);
      return false;
    }
    //
    //  Otherwise, simply adjust the logged quantity:
    final float newAmount = oldItem.amount - item.amount;
    final Item entered = Item.withAmount(oldItem, newAmount);
    itemTable.put(entered, entered);
    if (owner != null) owner.afterTransaction(item, 0 - item.amount);
    return true;
  }
  
  
  public void removeMatch(Item item) {
    final Item match = matchFor(item);
    if (match != null) removeItem(match);
  }
  
  
  public void removeAllMatches(Traded type) {
    if (verbose && type == tracked) I.say("REMOVING ALL "+tracked);
    for (Item match : matches(type)) itemTable.remove(match);
  }
  

  public float transfer(Traded type, Owner to) {
    float amount = 0;
    for (Item item : matches(type)) {
      removeItem(item);
      to.inventory().addItem(item);
      amount += item.amount;
    }
    return amount;
  }
  
  
  public float transfer(Item item, Owner to) {
    final float amount = Nums.min(item.amount, amountOf(item));
    if (amount <= 0) return 0;
    final Item transfers = Item.withAmount(item, amount);
    removeItem(transfers);
    to.inventory().addItem(transfers);
    return amount;
  }
  
  
  
  /**  Default supply-and-demand functions intended for override by certain
    *  subclasses.
    */
  //  TODO:  Either make these abstract to force implementation, or merge the
  //  production/consumption records from Stocks in here.
  
  public float consumption(Traded type) { return 0; }
  public float production (Traded type) { return 0; }
  
  public float relativeShortage(Traded type, boolean production) { return 0; }
  public float absoluteShortage(Traded type, boolean production) { return 0; }
  public float totalDemand(Traded type) { return 0; }
  
  public boolean canDemand(Traded t) { return false; }
  public void setReservation(Bringing d, boolean is) {}
  public Series <Bringing> reservations() { return null; };
  
  
  /**  Returns whether this inventory has enough of the given item to satisfy
    *  match criteria.
    */
  public boolean hasItem(Item item) {
    final Item match = matchFor(item);
    return match != null && match.amount >= item.amount;
  }
  
  
  public boolean hasItems(Item... items) {
    for (Item item : items) if (! hasItem(item)) return false;
    return true;
  }
}











