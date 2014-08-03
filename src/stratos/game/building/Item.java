/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.building.Economy.*;



/**  More representative of the abstract 'listing' of an item than a specific
  *  concrete object.
  */
public class Item {
  
  
  /**  Type definition.
    */
  final static String QUAL_NAMES[] = {
    "Crude", "Basic", "Standard", "Quality", "Luxury"
  };
  final static float PRICE_MULTS[] = {
    1.0f, 2.0f, 3.0f, 4.0f, 5.0f
  };
  
  
  
  /**  Field definitions, standard constructors and save/load functionality-
    */
  final public static int ANY = -1;
  final public static int MAX_QUALITY = 4;
  
  final public TradeType type;
  final public Saveable refers;
  final public float amount;
  final public float quality;
  
  
  private Item(
    TradeType type, Saveable refers, float amount, float quality
  ) {
    this.type = type;
    this.amount = amount;
    this.quality = quality;
    this.refers = refers;
  }
  
  
  public static Item loadFrom(Session s) throws Exception {
    final int typeID = s.loadInt();
    if (typeID == -1) return null;
    //
    //  TODO:  Save/load names instead of numeric IDs, so that you safely
    //  modify the listing between sessions.
    return new Item(
      ALL_ITEM_TYPES[typeID],
      s.loadObject(),
      s.loadFloat(),
      s.loadFloat()
    );
  }
  
  
  public static void saveTo(Session s, Item item) throws Exception {
    if (item == null) { s.saveInt(-1); return; }
    s.saveInt(item.type.typeID);
    s.saveObject(item.refers);
    s.saveFloat(item.amount);
    s.saveFloat(item.quality);
  }
  
  
  public static void saveItemsTo(Session s, Item items[]) throws Exception {
    if (items == null) { s.saveInt(-1); return; }
    s.saveInt(items.length);
    for (Item i : items) saveTo(s, i);
  }
  
  
  public static Item[] loadItemsFrom(Session s) throws Exception {
    final int count = s.loadInt();
    if (count == -1) return null;
    final Item items[] = new Item[count];
    for (int i = 0; i < count; i++) items[i] = loadFrom(s);
    return items;
  }
  
  
  
  /**  Outside-accessible factory methods-
    */
  public static Item withAmount(TradeType type, float amount) {
    return new Item(type, null, amount, 0);
  }
  
  
  public static Item withAmount(Item item, float amount) {
    final Item i = new Item(item.type, item.refers, amount, item.quality);
    return i;
  }
  
  
  public static Item withReference(TradeType type, Saveable refers) {
    return new Item(type, refers, 1, 0);
  }
  
  
  public static Item withReference(Item item, Saveable refers) {
    return new Item(item.type, refers, item.amount, item.quality);
  }
  
  
  public static Item withQuality(TradeType type, int quality) {
    return new Item(type, null, 1, Visit.clamp(quality, 5));
  }
  
  
  public static Item withQuality(Item item, int quality) {
    return new Item(
      item.type, item.refers, item.amount, Visit.clamp(quality, 5)
    );
  }
  
  
  public static Item with(
    TradeType type, Saveable refers, float amount, float quality
  ) {
    if (amount < 0) I.complain("Amount must be positive!");
    return new Item(
      type, refers, amount, Visit.clamp(quality, 0, 4)
    );
  }
  
  
  public float priceAt(Inventory.Owner venue) {
    return venue.priceFor(type) * amount * PRICE_MULTS[(int) quality];
  }
  
  
  public float defaultPrice() {
    return type.basePrice * amount * PRICE_MULTS[(int) quality];
  }
  
  
  
  /**  Matching/equality functions-
    */
  public static Item asMatch(TradeType type, Saveable refers) {
    return new Item(type, refers, ANY, ANY);
  }
  
  
  public static Item asMatch(TradeType type, int quality) {
    return new Item(type, null, ANY, quality);
  }
  
  
  public static Item asMatch(TradeType type, Saveable refers, int quality) {
    return new Item(type, refers, ANY, Visit.clamp(quality, 5));
  }
  
  
  protected boolean matchKind(Item item) {
    if (this.type != item.type) return false;
    if (this.refers != null) {
      if (item.refers == null) return false;
      if (! this.refers.equals(item.refers)) return false;
    }
    return true;
  }
  
  
  public boolean isMatch() {
    return amount == ANY || quality == ANY;
  }
  
  
  public boolean equals(Object o) {
    return matchKind((Item) o);
  }
  
  
  public int hashCode() {
    return
      (type.typeID * 13 * 5) +
      ((refers == null ? 0 : (refers.hashCode() % 13)) * 5);
  }
  
  
  
  /**  Rendering/interface functions-
    */
  public void describeTo(Description d) {
    String s = ""+type;
    if (quality != ANY && type.form != FORM_MATERIAL) {
      s = QUAL_NAMES[(int) (quality + 0.5f)]+" "+s;
    }
    if (amount != ANY) s = (I.shorten(amount, 1))+" "+s;
    d.append(s);
    if (refers != null) {
      d.append(" (");
      d.append(refers);
      d.append(")");
    }
  }

  public String toString() {
    final StringDescription SD = new StringDescription();
    describeTo(SD);
    return SD.toString();
  }
}












