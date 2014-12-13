

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import stratos.game.economic.*;
import stratos.game.economic.Inventory.Owner;
import stratos.game.politic.Commerce;
import static stratos.game.economic.Economy.*;



public class DeliveryUtils {
  
  
  //  NOTE:  See the rateTrading method below for how these are used...
  private static boolean
    sampleVerbose = false,
    rateVerbose   = false,
    shipsVerbose  = false,
    dispVerbose   = true ;
  
  private static Traded verboseGoodType = null;
  private static Class  verboseDestType = null;
  private static Class  verboseOrigType = null;
  
  
  /**  Helper methods for getting suitable distribution targets-
    */
  public static Batch <Venue> nearbyDepots(
    Target t, Stage world, Class... venueClasses
  ) {
    final Batch <Venue> depots = new Batch <Venue> ();
    world.presences.sampleFromMaps(
      t, world, 5, depots, (Object[]) venueClasses
    );
    return depots;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Traded type, Target target, Stage world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> ();
    world.presences.sampleFromMap(target, world, 5, vendors, type.supplyKey);
    return vendors;
  }
  
  
  private static Batch <Item> compressOrder(
    float amounts[], Traded s[], float sumAmount, int maxAmount
  ) {
    final Batch <Item> order = new Batch <Item> ();
    int sumOrder = 0;
    
    for (int i = s.length; i-- > 0;) {
      int amount = (int) Nums.ceil(amounts[i] * maxAmount / sumAmount);
      sumOrder += amount;
      if (sumOrder > maxAmount) amount -= sumOrder - maxAmount;
      
      final Item item = Item.withAmount(s[i], amount);
      order.add(item);
      if (sumOrder >= maxAmount) break;
    }
    return order;
  }
  
  
  private static Delivery bestShipDelivery(
    Vehicle ship, Batch <Venue> depots, int maxAmount, boolean export
  ) {
    final boolean report = shipsVerbose && I.talkAbout == ship;
    
    final Pick <Delivery> pick = new Pick <Delivery> ();
    if (report) {
      I.say("\nGetting best ship delivery for "+ship);
      I.say("  Depots found: "+depots.size());
    }
    
    for (Venue depot : depots) {
      if (report) I.say("  Assessing "+depot);
      
      final Traded s[] = depot.services();
      if (s == null) continue;
      final float amounts[] = new float[s.length];
      float sumAmount = 0;
      
      for (int i = s.length; i-- > 0;) {
        final Traded t = s[i];
        if (export) {
          if (depot.stocks.demandTier(t) != TIER_EXPORTER) continue;
          sumAmount += amounts[i] = depot.stocks.amountOf(t);
        }
        else {
          if (depot.stocks.demandTier(t) != TIER_IMPORTER) continue;
          sumAmount += amounts[i] = ship.inventory().amountOf(t);
        }
        if (report) I.say("    "+amounts[i]+" of "+t+" available");
      }
      if (sumAmount <= 0) continue;
      
      final Commerce c = ship.base().commerce;
      Batch <Item> order = compressOrder(amounts, s, sumAmount, maxAmount);
      float sumValue = 0;
      for (Item item : order) sumValue += item.amount * (export ?
        c.exportPrice(item.type) :
        c.importPrice(item.type)
      );
      if (report) I.say("   Total value: "+sumValue);
      
      final Delivery d = export ?
        new Delivery(order, depot, ship ) :
        new Delivery(order, ship , depot) ;
      pick.compare(d, sumValue);
    }
    
    if (report) I.say("  Final pick: "+pick.result());
    return pick.result();
  }
  
  
  public static Delivery bestImportDelivery(
    Vehicle ship, Batch <Venue> depots, int maxAmount
  ) {
    return bestShipDelivery(ship, depots, maxAmount, false);
  }
  
  
  public static Delivery bestExportDelivery(
    Vehicle ship, Batch <Venue> depots, int maxAmount
  ) {
    return bestShipDelivery(ship, depots, maxAmount, true );
  }
  
  
  
  /**  Utility methods for dealing with domestic orders-
    */
  public static Delivery nextDisposalFor(Actor actor) {
    final boolean report = dispVerbose && I.talkAbout == actor;
    //
    //  We allow the actor to dispose of any 'excess' items in their own
    //  inventory, their home, or their workplace.
    final Owner origins[] = {actor, actor.mind.home(), actor.mind.work()};
    //
    //  Excess items can, in principle, be disposed of anywhere that accepts
    //  them- so we just fall back on whatever the actor can see.
    final Batch <Property> depots = new Batch <Property> ();
    for (Target t : actor.senses.awareOf()) if (t instanceof Property) {
      depots.add((Property) t);
    }
    for (Owner origin : origins) if (origin != null) {
      //
      //  We include anything 'unnecesary' from the inventory in question- any
      //  kind of bulk commodity which isn't in demand- as up for disposal.
      final Pick <Delivery> pick = new Pick <Delivery> ();
      final Batch <Item> excess = new Batch <Item> ();
      for (Item i : origin.inventory().allItems()) {
        if (i.type.form != Economy.FORM_MATERIAL) continue;
        if (origin.inventory().demandFor(i.type) > 0) continue;
        if (report) I.say("  Excess item: "+i);
        excess.add(i);
      }
      if (excess.size() == 0) continue;
      if (report) {
        I.say("\nGetting next item disposal: "+actor);
        I.say("Origin is: "+origin);
      }
      //
      //  Then, we iterate over any candidate depots and see if they'll accept
      //  the good in question.  (If the depot is one of the origin points,
      //  only take goods in demand- otherwise, make sure the depot deals in
      //  goods of that type.)
      for (Property depot : depots) for (Item i : excess) {
        final Traded t = i.type;
        final boolean sells = ! Visit.arrayIncludes(origins, depot);
        
        if ((! sells) && depot.inventory().demandFor(t) <= 0) continue;
        if (sells && ! Visit.arrayIncludes(depot.services(), t)) continue;
        
        final Item moved = Item.withAmount(t, Nums.min(5, i.amount));
        final Delivery d = new Delivery(moved, origin, depot);
        d.withPayment(sells ? depot : null, false);
        
        if (report) {
          I.say("  Candidate depot: "+depot+" ("+i.type+")");
          I.say("  Rating (sells):  "+d.pricePaid()+" ("+sells+")");
        }
        pick.compare(d, d.pricePaid());
      }
      //
      //  Return whatever option seems most profitable (if any)-
      final Delivery disposal = pick.result();
      if (report) I.say("Final choice: "+disposal);
      if (disposal != null) return disposal;
    }
    return null;
  }
  
  
  /**  Returns the best bulk delivery from the given origin point.  (Note: the
    *  numSamples argument is only used if destinations is null.)  Helper
    *  methods with shorter contracts follow below.
    */
  public static Delivery bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> destinations, int numSamples
  ) {
    final Stage world = origin.world();
    Tally <Owner> ratings = new Tally <Owner> ();
    
    for (Traded good : goods) {
      final Batch <? extends Owner> sampled;
      if (destinations != null) sampled = destinations;
      else {
        sampled = new Batch <Owner> ();
        world.presences.sampleFromMap(
          origin, world, numSamples, sampled, good.demandKey
        );
      }
      final Owner bestDest = bestDestination(origin, sampled, good, baseUnit);
      if (bestDest == null) continue;
      final float rating = rateTrading(origin, bestDest, good, baseUnit);
      ratings.add(rating, bestDest);
    }
    if (ratings.size() == 0) return null;
    
    final Owner destination = ratings.highestValued();
    return fillBulkOrder(origin, destination, goods, baseUnit, amountLimit);
  }
  
  
  public static Delivery bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> destinations
  ) {
    return bestBulkDeliveryFrom(
      origin, goods, baseUnit, amountLimit, destinations, -1
    );
  }
  
  
  public static Delivery bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    int numSamples
  ) {
    return bestBulkDeliveryFrom(
      origin, goods, baseUnit, amountLimit, null, numSamples
    );
  }
  
  

  /**  Returns the best bulk delivery to the given destination.  (Note: the
    *  numSamples argument is only used if origins is null.)  Helper methods
    *  with shorter contracts follow below.
    */
  public static Delivery bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> origins, int numSamples
  ) {
    final boolean report = sampleVerbose && I.talkAbout == destination;
    final Stage world = destination.world();
    Tally <Owner> ratings = new Tally <Owner> ();
    if (report) I.say("\nGetting bulk delivery for "+destination);
    
    for (Traded good : goods) {
      final Batch <? extends Owner> sampled;
      if (origins != null) sampled = origins;
      else {
        sampled = new Batch <Owner> ();
        world.presences.sampleFromMap(
          destination, world, numSamples, sampled, good.supplyKey
        );
      }
      if (report) I.say("  Sample size for "+good+" is "+sampled.size());
      
      final Owner bestOrig = bestOrigin(sampled, destination, good, baseUnit);
      if (bestOrig == null) continue;
      final float rating = rateTrading(bestOrig, destination, good, baseUnit);
      ratings.add(rating, bestOrig);
    }
    if (ratings.size() == 0) return null;
    
    final Owner origin = ratings.highestValued();
    return fillBulkOrder(origin, destination, goods, baseUnit, amountLimit);
  }
  
  
  public static Delivery bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> origins
  ) {
    return bestBulkCollectionFor(
      destination, goods, baseUnit, amountLimit, origins, -1
    );
  }
  
  
  public static Delivery bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    int numSamples
  ) {
    return bestBulkCollectionFor(
      destination, goods, baseUnit, amountLimit, null, numSamples
    );
  }
  
  
  
  /**  Fills a bulk order of specified good types between the given origin and
    *  destination points.
    */
  public static Delivery fillBulkOrder(
    Owner origin, Owner destination, Traded goods[],
    int baseUnit, int amountLimit
  ) {
    int goodAmounts[] = new int[goods.length];
    int sumAmounts = 0;
    
    while (true) {
      int   bestIndex  = -1;
      float bestRating =  0;
      
      for (int i = goods.length; i-- > 0;) {
        final int nextAmount = goodAmounts[i] + baseUnit;
        final float rating = rateTrading(
          origin, destination, goods[i], nextAmount
        );
        if (rating > bestRating) { bestRating = rating; bestIndex = i; }
      }
      
      if (bestIndex == -1) break;
      goodAmounts[bestIndex] += baseUnit;
      sumAmounts += baseUnit;
      if (sumAmounts >= amountLimit) break;
    }
    if (sumAmounts <= 0) return null;
    
    final Batch <Item> toTake = new Batch <Item> ();
    for (int i = goods.length; i-- > 0;) {
      final int amount = goodAmounts[i];
      if (amount > 0) toTake.add(Item.withAmount(goods[i], amount));
    }
    
    final Delivery order = new Delivery(toTake, origin, destination);
    return order.withPayment(destination, false);
  }
  
  
  public static Owner bestOrigin(
    Batch <? extends Owner> origins, Owner destination,
    Traded good, int amount
  ) {
    if (origins.size() == 0) return null;
    if (destination.inventory().shortageOf(good) < 0) return null;
    
    final boolean report = rateVerbose && I.talkAbout == destination;
    if (report) {
      I.say("\nFinding best origin of "+good+" for "+destination);
      I.say("  ("+origins.size()+" available)");
    }
    
    Owner pick = null;
    float bestRating = 0;
    for (Owner origin : origins) {
      if (origin.privateProperty()) continue;
      
      final float rating = rateTrading(origin, destination, good, amount);
      if (rating > bestRating) { bestRating = rating; pick = origin; }
      if (report) I.say("  Rating for "+origin+" is "+rating);
    }
    
    if (report) I.say("  Final selection: "+pick);
    return pick;
  }
  
  
  public static Owner bestDestination(
    Owner origin, Batch <? extends Owner> destinations,
    Traded good, int amount
  ) {
    if (destinations.size() == 0) return null;
    if (origin.inventory().amountOf(good) <= amount) return null;
    
    final boolean report = rateVerbose && I.talkAbout == origin;
    if (report) {
      I.say("\nFinding best destination for "+good+" from "+origin);
      I.say("  ("+destinations.size()+" available)");
    }
    
    Owner pick = null;
    float bestRating = 0;
    for (Owner destination : destinations) {
      if (destination.privateProperty()) continue;
      
      final float rating = rateTrading(origin, destination, good, amount);
      if (rating > bestRating) { bestRating = rating; pick = destination; }
      if (report) I.say("  Rating for "+destination+" is "+rating);
    }
    
    if (report) I.say("  Final selection: "+pick);
    return pick;
  }
  
  
  
  /**  Rates the attractiveness of trading a particular good types between
    *  the given origin and destination venues-
    */
  /*
  static float rateSimpleTrading(
    Owner orig, Owner dest, Traded good, int amount
  ) {
    if (orig == dest) return -1;
    return
      Nums.max(dest.inventory().shortageOf(good), 1) /
      Nums.max(orig.inventory().shortageOf(good), 1);
  }
  //*/
  
  //  TODO:  In the case of personal purchases, you'll want to limit by
  //  cash available.  TODO:  Also include bulk limits.
  
  static float rateTrading(
    Owner orig, Owner dest, Traded good, int amount
  ) {
    if (orig == dest) return -1;
    
    //  Supply and demand can quickly get very hairy, so to help in tracking it
    //  we have some moderately elaborate reporting criteria.
    final boolean report;
    if (verboseGoodType != null) {
      report =
        (verboseGoodType == good) &&
        (verboseDestType == null || dest.getClass() == verboseDestType) &&
        (verboseOrigType == null || orig.getClass() == verboseOrigType);
    }
    else {
      report = rateVerbose && (I.talkAbout == orig || I.talkAbout == dest);
    }
    
    //  First of all secure some preliminary variables.
    //  OS/DS == origin/destination stocks
    //  OA/DA == origin/destination amount
    //  OD/DD == origin/destination demand
    final Inventory
      OS = orig.inventory(),
      DS = dest.inventory();
    final float
      OA = OS.amountOf(good),
      DA = DS.amountOf(good);
    if (OA < amount) return -1;
    
    final int
      OT = OS.demandTier(good),
      DT = DS.demandTier(good);
    if (OT == TIER_NONE || DT == TIER_NONE) return -1;
    if (OT >= TIER_CONSUMER || DT <= TIER_PRODUCER) return -1;
    if (DS.shortageOf(good) <= 0) return -1;
    
    final float
      OD = OS.demandFor(good),
      DD = DS.demandFor(good);
    
    if (DD == 0 || (DD + 1) < (amount / 2f) || amount > (DD + 1)) {
      if (report) {
        I.say("\nAmounts out of proportion at "+dest+": "+amount+"/"+DD);
      }
      return -1;
    }
    
    //  Secondly, obtain an estimate of stocks before and after the exchange-
    float origAfter = 0, destAfter = 0;
    origAfter = OA - (amount / 2f);
    destAfter = DA + (amount / 2f);
    origAfter -= futureBalance(orig, good, false);
    destAfter += futureBalance(dest, good, true );
    
    //  Then, assign ratings for relative shortages at the start/end points-
    float origShort = 0, destShort = 0;
    origShort = 1 - (origAfter / (OD + amount));
    destShort = 1 - (destAfter / (DD + amount));
    
    if (OT <= TIER_PRODUCER) origShort /= 2;
    if (OT >= TIER_CONSUMER) origShort += 1;
    
    if (DT <= TIER_PRODUCER) destShort /= 2;
    if (DT >= TIER_CONSUMER) destShort += 1;
    
    if (report) {
      I.say("\n  Getting trade rating for "+good+" between "+orig+" and "+dest);
      I.say("  Trade unit is "+amount);
      I.say("  Origin      after   : "+origAfter+"  Demand: "+OD);
      I.say("  Destination after   : "+destAfter+"  Demand: "+DD);
      I.say("  Origin      shortage: "+origShort);
      I.say("  Destination shortage: "+destShort);
    }
    
    //  And return an estimate of how much an exchange would equalise things,
    //  along with penalties for distance and base relations:
    float rating = destShort - origShort;
    if (rating <= 0) return -1;
    
    if (OT == TIER_TRADER) rating *= 1.5f;
    if (DT == TIER_TRADER) rating *= 1.5f;
    
    final float baseFactor = orig.base().relations.relationWith(dest.base());
    if (baseFactor <= 0) return -1;
    
    final int SS = Stage.SECTOR_SIZE;
    final float distFactor = SS / (SS + Spacing.distance(orig, dest));
    
    if (report) {
      I.say("  Final rating "+rating);
      I.say("  base/distance factors: "+baseFactor+"/"+distFactor);
    }
    return rating * distFactor * baseFactor;
  }
  
  
  static float futureBalance(Owner e, Traded good, boolean positive) {
    
    //  TODO:  Cache this locally if possible.
    final Activities a = e.world().activities;
    final Batch <Delivery> matches = (Batch) a.activePlanMatches(
      e, Delivery.class
    );
    float balance = 0;
    
    for (Delivery d : matches) {
      final Item itemMatch = d.delivered(good);
      if (itemMatch == null) continue;
      
      if (d.origin == e && d.stage() == Delivery.STAGE_PICKUP) {
        balance -= itemMatch.amount;
      }
      
      if (d.destination == e && d.stage() == Delivery.STAGE_DROPOFF) {
        balance += itemMatch.amount;
      }
    }
    
    if (positive) return (balance > 0) ? balance : 0;
    return (balance < 0) ? balance : 0;
  }
}



