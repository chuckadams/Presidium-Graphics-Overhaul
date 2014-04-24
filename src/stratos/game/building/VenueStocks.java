/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



public class VenueStocks extends Inventory implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static float
    UPDATE_PERIOD = World.STANDARD_HOUR_LENGTH,
    DEMAND_DECAY  = UPDATE_PERIOD * 1f / (World.STANDARD_DAY_LENGTH / 2),
    SEARCH_RADIUS = 16,
    MAX_CHECKED   = 5 ;
  
  final public static int
    TIER_NONE     = -1,
    TIER_PRODUCER =  0,  //never deliver to a producer.
    TIER_TRADER   =  1,  //deliver to/from based on relative shortage.
    TIER_CONSUMER =  2 ; //never deliver from a consumer.
  
  private static boolean
    verbose = false;
  
  
  static class Demand {
    Service type ;
    int tierType = TIER_PRODUCER ;
    
    private float amountInc, demandAmount ;
    private float pricePaid ;
  }
  
  final Venue venue ;
  final Table <Service, Demand> demands = new Table <Service, Demand> () ;
  final List <Manufacture> specialOrders = new List <Manufacture> () ;
  
  
  VenueStocks(Venue v) {
    super(v) ;
    this.venue = v ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    s.loadObjects(specialOrders) ;
    int numC = s.loadInt() ;
    while (numC-- > 0) {
      final Demand d = new Demand() ;
      d.type = Economy.ALL_ITEM_TYPES[s.loadInt()] ;
      d.amountInc    = s.loadFloat() ;
      d.demandAmount = s.loadFloat() ;
      d.tierType     = (int) s.loadFloat() ;
      d.pricePaid    = s.loadFloat() ;
      demands.put(d.type, d) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(specialOrders) ;
    s.saveInt(demands.size()) ;
    for (Demand d : demands.values()) {
      s.saveInt(d.type.typeID) ;
      s.saveFloat(d.amountInc   ) ;
      s.saveFloat(d.demandAmount) ;
      s.saveFloat(d.tierType    ) ;
      s.saveFloat(d.pricePaid   ) ;
    }
  }
  
  
  public List <Manufacture> specialOrders() {
    return specialOrders ;
  }
  
  
  
  
  /**  Overrides of standard inventory methods-
    */
  public boolean addItem(Item item) {
    final int oldAmount = (int) amountOf(item) ;
    if (super.addItem(item)) {
      final int inc = ((int) amountOf(item)) - oldAmount ;
      if (venue.inWorld() && inc != 0 && item.type.form != FORM_PROVISION) {
        String phrase = inc >= 0 ? "+" : "-" ;
        phrase+=" "+inc+" "+item.type.name ;
        venue.chat.addPhrase(phrase) ;
      }
      return true ;
    }
    return false ;
  }
  
  
  public void incCredits(float inc) {
    if (Float.isNaN(inc)) I.complain("INC IS NOT-A-NUMBER!") ;
    if (Float.isNaN(credits)) credits = 0 ;
    if (inc == 0) return ;
    final int oldC = (int) credits() ;
    super.incCredits(inc) ;
    final int newC = (int) credits() ;
    if (! venue.inWorld() || oldC == newC) return ;
    String phrase = inc >= 0 ? "+" : "-" ;
    phrase+=" "+(int) Math.abs(inc)+" credits" ;
    venue.chat.addPhrase(phrase) ;
  }
  
  
  
  /**  Assigning and producing jobs-
    */
  public void addSpecialOrder(Manufacture newOrder) {
    specialOrders.add(newOrder) ;
  }
  
  
  public boolean hasOrderFor(Item made) {
    for (Manufacture m : specialOrders) {
      if (m.made().matchKind(made)) return true ;
    }
    return false ;
  }
  
  
  public Batch <Service> demanded() {
    final Batch <Service> batch = new Batch <Service> ();
    for (Demand d : demands.values()) batch.add(d.type);
    return batch;
  }
  
  
  public Batch <Item> shortages() {
    final Batch <Item> batch = new Batch <Item> () ;
    for (Demand d : demands.values()) {
      float amount = shortageOf(d.type) ;
      if (amount <= 0) continue ;
      batch.add(Item.withAmount(d.type, amount)) ;
    }
    return batch ;
  }
  
  
  public Manufacture nextSpecialOrder(Actor actor) {
    final Choice choice = new Choice(actor) ;
    for (Manufacture order : specialOrders) choice.add(order) ;
    return (Manufacture) choice.weightedPick() ;
  }
  
  
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = shortageOf(c.out.type) ;
    if (shortage <= 0) return null ;
    
    final Manufacture m = new Manufacture(
      actor, venue, c,
      Item.withAmount(c.out, shortage + 5)
    ) ;
    m.setMotive(Plan.MOTIVE_DUTY, shortageUrgency(c.out.type)) ;
    return m ;
  }
  
  
  public Manufacture bestManufacture(Actor actor, Conversion... cons) {
    final Conversion picked = bestConversion(cons) ;
    if (picked != null) return nextManufacture(actor, picked) ;
    else return null ;
  }
  
  
  
  /**  Public accessor methods-
    */
  public float demandFor(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 ;
    return d.demandAmount ;
  }
  
  
  public int demandTier(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return TIER_NONE ;
    return d.tierType ;
  }
  
  
  public boolean hasEnough(Service type) {
    return amountOf(type) < (demandFor(type) / 2) ;
  }
  
  
  public float shortagePenalty(Service type) {
    if (GameSettings.needsFree && type.form == FORM_PROVISION) return 0;
    final float
      amount = amountOf(type),
      demand = demandFor(type),
      shortage = demand - amount;
    return Visit.clamp(
      (shortage - ((demand + 1f) / 2)) / (amount + 1),
      0, 1
    );
  }
  
  
  public float shortageOf(Service type) {
    return demandFor(type) - amountOf(type) ;
  }
  
  
  public float surplusOf(Service type) {
    return amountOf(type) - demandFor(type) ;
  }
  
  
  public float shortageUrgency(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 ;
    final float amount = amountOf(type), shortage = d.demandAmount - amount ;
    if (shortage <= 0) return 0 ;
    final float urgency = shortage / ((amount + shortage) * (1 + d.tierType)) ;
    return urgency ;
  }
  
  
  public float priceFor(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return type.basePrice ;
    return (d.pricePaid + type.basePrice) / 2f ;
  }
  
  
  private Conversion bestConversion(Conversion... cons) {
    Item made = cons[0].out ;
    Conversion picked = null ;
    float minPrice = Float.POSITIVE_INFINITY ;
    for (Conversion c : cons) {
      if (c.out.type != made.type) I.complain("Only for same good!") ;
      float unitPrice = 0 ;
      for (Item raw : c.raw) {
        final float amount = amountOf(raw.type) ;
        ///if (amount == 0) continue consLoop ;
        float rawPrice = priceFor(raw.type) * raw.amount ;
        //I.say("Raw price for "+raw+" is "+rawPrice) ;
        rawPrice *= 5 / (5 + amount) ;
        rawPrice *= 1 + shortagePenalty(raw.type) ;
        unitPrice += rawPrice ;
      }
      unitPrice /= c.out.amount ;
      //I.say("Unit price for "+c+" is "+unitPrice) ;
      if (unitPrice < minPrice) { minPrice = unitPrice ; picked = c ; }
    }
    return picked ;
  }
  
  
  
  /**  Utility methods for setting and propagating various types of demand-
    */
  private Demand demandRecord(Service t) {
    final Demand d = demands.get(t) ;
    if (d != null) return d ;
    Demand made = new Demand() ;
    made.type = t ;
    made.pricePaid = t.basePrice ;
    demands.put(t, made) ;
    return made ;
  }
  
  
  public void forceDemand(Service type, float amount, int tier) {
    if (amount < 0) amount = 0 ;
    final Demand d = demandRecord(type) ;
    d.demandAmount = amount ;
    d.tierType = tier ;
    d.amountInc = 0 ;
    if (verbose) I.sayAbout(venue,
      "  "+type+" demand forced to: "+d.demandAmount
    ) ;
  }
  
  
  public void incDemand(
    Service type, float amount, int tier, int period, Owner source
  ) {
    if (amount == 0) return ;
    final Demand d = demandRecord(type) ;
    final float inc = period * 1f / UPDATE_PERIOD ;
    d.amountInc += amount * inc ;
    
    if (verbose && I.talkAbout == venue) {
      I.say(
        "  "+type+" demand inc is: "+d.amountInc+" raw amount: "+amount+
        "\n    Source is: "+source+", base inc: "+inc
      );
    }
    if (tier != TIER_NONE) d.tierType = tier;
  }
  
  
  public void translateDemands(int period, Conversion cons, Owner source) {
    
    //  Firstly, we check to see if the output good is in demand, and if so,
    //  reset demand for the raw materials-
    final boolean report = verbose && I.talkAbout == venue;
    final float demand = shortageOf(cons.out.type) ;
    if (demand <= 0) return ;
    
    if (report) {
      I.say("\nTranslating demand for "+cons.out.type+" at: "+demand) ;
    }
    float priceBump = 1 ;
    
    //  We adjust our prices to ensure we can make a profit, and adjust demand
    //  for the inputs to match demand for the outputs-
    final Demand o = demandRecord(cons.out.type) ;
    o.pricePaid = o.type.basePrice * priceBump / (1f + cons.raw.length) ;
    for (Item raw : cons.raw) {
      final float needed = raw.amount * demand / cons.out.amount ;
      if (report) I.say("  Need "+needed+" "+raw.type+" as raw materials") ;
      incDemand(raw.type, needed, TIER_CONSUMER, period, source) ;
    }
  }
  
  
  private void incDemand(
    Service type, float amount, int period, Owner source
  ) {
    if (amount == 0) return ;
    final Demand d = demandRecord(type) ;
    incDemand(type, amount, d.tierType, period, source) ;
  }
  
  
  private void incPrice(Service type, float toPrice) {
    final Demand d = demandRecord(type) ;
    d.pricePaid += (toPrice - type.basePrice) * DEMAND_DECAY ;
  }
  
  
  public void clearDemands() {
    for (Demand d : demands.values()) {
      d.demandAmount = 0 ;
      d.tierType = TIER_PRODUCER ;
    }
  }
  
  
  public void diffuseDemand(Service type, Batch <Venue> suppliers, int period) {
    final Demand d = demands.get(type) ;
    if (d == null) return ;
    
    final float
      shortage = shortageOf(type),
      urgency = shortageUrgency(type) ;
    if (shortage <= 0 || urgency <= 0) return;
    
    final int
      tier = demandTier(type) ;
    if (verbose) I.sayAbout(venue, 
      venue+" has shortage of "+shortage+
      " for "+type+" of urgency "+urgency
    ) ;
    final float
      ratings[] = new float[suppliers.size()],
      distances[] = new float[suppliers.size()] ;
    float sumRatings = 0 ;
    
    int i = 0 ; for (Venue supplies : suppliers) {
      final int ST = supplies.stocks.demandTier(type) ;
      if (ST > tier) { i++ ; continue ; }
      
      final float SU = supplies.stocks.shortageUrgency(type) ;
      if (tier == TIER_TRADER && ST == tier) {
        if (SU >= urgency) { i++ ; continue ; }
      }
      
      float rating = 10 / (1 + SU) ;
      distances[i] = Spacing.distance(supplies, venue) / SEARCH_RADIUS ;
      rating /= 1 + distances[i] ;
      rating *= (supplies.stocks.amountOf(type) + 5) / 10f ;
      ratings[i++] = rating ;
      sumRatings += rating ;
    }
    if (sumRatings == 0) return ;
    
    float avgPriceBump = 0 ;
    i = 0 ;
    
    for (Venue supplies : suppliers) {
      final float rating = ratings[i], distance = distances[i++] ;
      if (rating == 0) continue ;
      final float
        weight = rating / sumRatings,
        shortBump = shortage * weight,
        priceBump = type.basePrice * distance / 10f ;
      
      supplies.stocks.incDemand(type, shortBump, period, venue);
      final float price = supplies.priceFor(type) ;
      
      if (verbose && I.talkAbout == venue) {
        I.say(
          "  Considering supplier: "+supplies+", rating: "+rating+
          "\n  BUMP IS: "+shortBump
        ) ;
        I.say("  Price for "+type+" at "+supplies+" is "+price) ;
      }
      avgPriceBump += (price + priceBump) * weight ;
    }

    if (verbose && I.talkAbout == venue) {
      I.say("Average price bump for "+type+" is "+avgPriceBump) ;
    }
    incPrice(type, avgPriceBump) ;
  }
  
  
  public void diffuseDemand(Service type, int period) {
    final Batch <Venue> suppliers = Deliveries.nearbyVendors(
      type, venue, venue.world()
    ) ;
    diffuseDemand(type, suppliers, period) ;
  }
  
  
  
  /**  Calling regular updates-
    */
  protected void updateStocks(int numUpdates) {
    if (Float.isNaN(credits)) credits = 0 ;
    if (Float.isNaN(taxed)) taxed = 0 ;
    if (numUpdates % UPDATE_PERIOD == 0) diffuseExistingDemand() ;
    //
    //  Here, we clear out any expired orders or useless items.  (Consider
    //  recycling materials or sending elswhere?)
    for (Manufacture m : specialOrders) {
      if (m.finished()) specialOrders.remove(m) ;
    }
    //
    //  TODO:  Have all stocks wear out/go off, given enough time.
  }
  
  /*
  public void translateBest(int period, Conversion... cons) {
    final Conversion picked = bestConversion(cons) ;
    //if (verbose)
      //I.sayAbout(venue, "Best conversion: "+picked) ;
    if (picked != null) translateDemands(period, picked) ;
  }
  //*/
  
  
  private void diffuseExistingDemand() {
    final boolean report = verbose && I.talkAbout == venue;
    
    if (report) I.say("\nDIFFUSING DEMAND AT: "+venue);
    
    final Service vS[] = venue.services() ;
    if ((vS == null || vS.length == 0) && demands.size() == 0) return ;
    if (vS != null) for (Service s : vS) if (demandTier(s) == TIER_NONE) {
      demandRecord(s).tierType = TIER_PRODUCER ;
    }
    
    for (Demand d : demands.values()) {
      d.demandAmount += d.amountInc * DEMAND_DECAY ;
      d.demandAmount *= 1 - DEMAND_DECAY ;
      d.amountInc = 0 ;
      
      d.pricePaid -= d.type.basePrice ;
      d.pricePaid *= 1 - DEMAND_DECAY ;
      d.pricePaid += d.type.basePrice ;
      
      if (report) I.say("  "+d.type+" demand is: "+d.demandAmount);
      if (d.tierType == TIER_PRODUCER) continue ;
      diffuseDemand(d.type, (int) UPDATE_PERIOD) ;
    }
    
    for (Manufacture m : specialOrders) {
      final Item out = m.conversion.out ;
      final float amount = m.made().amount / (out == null ? 1 : out.amount) ;
      for (Item i : m.conversion.raw) {
        incDemand(i.type, i.amount * amount, -1, (int) UPDATE_PERIOD, venue) ;
      }
    }
  }
}









