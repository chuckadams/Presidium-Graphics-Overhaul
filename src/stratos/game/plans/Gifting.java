/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.util.*;



//  TODO:  There's a problem with the 'greeting' phase here if the other actor
//  isn't disposed to talk (e.g, is hostile, busy or asleep, etc.)

public class Gifting extends Plan implements Qualities {
  
  
  final static int
    STAGE_INIT  = -1,
    STAGE_GETS  =  0,
    STAGE_GIVES =  1,
    STAGE_DONE  =  2;
  
  private static boolean
    evalVerbose   = false,
    rateVerbose   = false,
    eventsVerbose = true ;
  
  
  final Item gift;
  final Actor receives;
  final Plan getting;
  final Proposal giving;
  private int stage = STAGE_INIT;
  
  
  
  public Gifting(Actor actor, Actor receives, Item gift, Plan getting) {
    super(actor, receives, MOTIVE_PERSONAL, NO_HARM);
    this.gift = gift;
    this.receives = receives;
    this.getting = getting;
    this.giving = new Proposal(actor, receives);
    giving.setTerms(Pledge.giftPledge(gift, actor, actor, receives), null);
  }
  
  
  public Gifting(Session s) throws Exception {
    super(s);
    gift     = Item.loadFrom(s);
    receives = (Actor) s.loadObject();
    getting  = (Plan) s.loadObject();
    stage    = s.loadInt();
    giving   = (Proposal) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo(s, gift);
    s.saveObject(receives);
    s.saveObject(getting );
    s.saveInt   (stage   );
    s.saveObject(giving  );
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  
  /**  Priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { EMPATHIC, GENEROUS, EXCITABLE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if ((! getting.finished()) && getting.priorityFor(actor) < 0) return 0;
    
    float modifier = NO_MODIFIER;
    final float
      novelty = receives.relations.noveltyFor(actor),
      rating = ActorMotives.rateDesire(gift, actor, receives);
    if (! hasBegun()) {
      modifier -= ROUTINE;
      modifier += (novelty + rating) * ROUTINE;
    }
    
    return PlanUtils.dialoguePriority(actor, receives, modifier, 1);
  }
  
  
  public boolean finished() {
    if (! getting.finished()) return false;
    return super.finished();
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false;
    return getting.finished() || getting.valid();
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next gifting step:");
    
    //  If the recipient has accepted the item, your job is complete.  (If it's
    //  refused, abort the behaviour.)
    if (this.stage == STAGE_DONE) {
      if (report) I.say("  Gifting complete!");
      return null;
    }
    
    //  If you've acquired the item, present it to the recipient, and await
    //  their response.
    if (actor.gear.hasItem(gift)) {
      if (giving.finished()) return null;
      this.stage = STAGE_GIVES;
      giving.setMotivesFrom(this, 0);
      if (giving.priorityFor(actor) <= 0) return null;
      if (report) {
        I.say("  Entering dialogue mode for gift-giving.");
        I.say("  Priority: "+giving.priorityFor(actor));
      }
      return giving;
    }
    
    //  If the 'get' action hasn't completed, carry it out.
    if (! getting.finished()) {
      this.stage = STAGE_GETS;
      final Behaviour nextS = getting.nextStepFor(actor);
      if (report) I.say("  Still getting, next step: "+nextS);
      if (nextS == null) return null;
      return getting;
    }
    
    if (report) I.say("  No next step!");
    return null;
  }
  
  
  
  /**  Returns the next suitable gift between the given actors-
    */
  //  TODO:  Cache ratings for all these items in some global location,
  //  including bonuses for hunger, et cetera.
  
  
  //  TODO:  Just iterate across all desires from the ActorDesires class.
  public static Gifting nextGifting(Plan parent, Actor buys, Actor receives) {
    final boolean report = rateVerbose && I.talkAbout == buys;
    if (report) I.say("\nGetting next gift from "+buys+" for "+receives);
    
    if (buys.mind.hasToDo(Gifting.class)) return null;
    final Dialogue d = new Dialogue(buys, receives);
    if (parent != null) d.setMotivesFrom(parent, 0);
    if (d.priorityFor(buys) <= 0) return null;
    
    float rating, bestRating = 0;
    Item gift = null;
    Plan getting = null;
    
    for (Traded f : Economy.ALL_FOOD_TYPES) {
      final Item food = Item.withAmount(f, 1);
      rating = ActorMotives.rateDesire(food, buys, receives);
      if (rating > bestRating) { bestRating = rating; gift = food; }
    }
    
    if (receives.mind.home() instanceof Venue) {
      final Venue home = (Venue) receives.mind.home();
      for (Item needs : home.stocks.shortages()) {
        rating = ActorMotives.rateDesire(needs, buys, receives);
        if (rating > bestRating) { bestRating = rating; gift = needs; }
      }
    }
    
    //  TODO:  Consider restoring this later.  (At the moment, there are
    //         problems with giving someone an outfit they already have!)
    
    //  TODO:  Exclude commissions if any other such commission is scheduled by
    //         the recipient, or at the same venue- or just *exists*?
    /*
    final Commission
      forDevice = Commission.forItem(buys, receives.gear.deviceEquipped()),
      forOutfit = Commission.forItem(buys, receives.gear.outfitEquipped());
    
    if (forDevice != null) {
      rating = rateGift(forDevice.item, buys, receives);
      if (rating > bestRating) {
        bestRating = rating;
        gift = forDevice.item;
        getting = forDevice;
      }
    }
    if (forOutfit != null) {
      rating = rateGift(forOutfit.item, buys, receives);
      if (rating > bestRating) {
        bestRating = rating;
        gift = forOutfit.item;
        getting = forOutfit;
      }
    }
    //*/
    
    if (gift != null && getting == null && ! receives.gear.hasItem(gift)) {
      final Stage world = buys.world();
      final Batch <Venue> origins = BringUtils.nearbyVendors(
        gift.type, buys, world
      );
      
      if (report) {
        I.say("\n  Potential vendors for "+gift+" are:");
        for (Object o : origins) I.say("    "+o);
      }
      getting = BringUtils.bestBulkCollectionFor(
        buys, new Traded[] {gift.type}, 1, 1, 5
      );
    }

    if (report) {
      I.say("\n  Gift is: "+gift);
      I.say("  Acquired by: "+getting);
    }
    if (gift == null || getting == null) return null;
    return new Gifting(buys, receives, gift, getting);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Getting gift for ")) {
      d.append(receives);
    }
  }
}




