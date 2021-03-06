/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.base.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import static stratos.game.craft.Economy.*;



//  TODO:  At this point, you might as well write some custom widgets here...

public class VenuePane extends SelectionPane {
  
  
  final public Venue v;  //  TODO:  Apply to Properties, like, e.g, vehicles?
 
  
  protected VenuePane(HUD UI, Venue v, String... categories) {
    super(UI, v, v.portrait(UI), false, categories);
    this.v = v;
  }
  
  
  public static SelectionPane configStandardPanel(
    Venue venue, SelectionPane panel, HUD UI,
    Traded setStock[], String... cats
  ) {
    if (panel == null) panel = new VenuePane(UI, venue, cats);
    final VenuePane VD = (VenuePane) panel;
    final Description d = panel.detail();
    
    VD.describeCondition(d, UI);
    Text.cancelBullet(d);
    d.append("\n");
    VD.describeOrders(d, UI);
    Text.cancelBullet(d);
    d.append("\n");
    VD.describeStocks(d, UI, setStock);
    Text.cancelBullet(d);
    d.append("\n");
    VD.describeStaffing(d, UI);
    
    return panel;
  }
  
  
  private void describeCondition(Description d, HUD UI) {
    final Stage world = v.origin().world();
    d.append("Condition and Repair:");
    
    final int repair = Nums.round(v.structure.repair(), 1, true);
    d.append("\n  Integrity: ");
    d.append(repair+" / "+v.structure().maxIntegrity());
    
    final Inventory i = v.inventory();
    d.append("\n  Credits: "+(int) i.allCredits());
    d.append(" ("+(int) i.unTaxed()+" Untaxed)");
    
    final float squalor = 0 - world.ecology().ambience.valueAt(v);
    if (squalor > 0) {
      final String SN = " ("+I.shorten(squalor, 1)+")";
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Squalor"+SN);
    }
    else {
      final String AN = " ("+I.shorten(0 - squalor, 1)+")";
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Ambience"+AN);
    }
    
    final float danger = v.base().dangerMap.sampleAround(v, Stage.ZONE_SIZE);
    if (danger > 0) {
      final String DN = " ("+I.shorten(danger, 1)+")";
      d.append("\n  "+Ambience.dangerDesc(danger)+" Hazards"+DN);
    }
    else {
      final String SN = " ("+I.shorten(0 - danger, 1)+")";
      d.append("\n  "+Ambience.dangerDesc(danger)+" Safety"+SN);
    }
    
    Base played = BaseUI.currentPlayed();
    Faction f = played == null ? null : played.faction();
    float relations = 0, numB = 0;
    for (Actor a : v.staff.workers()) {
      relations += a.relations.valueFor(f);
      numB++;
    }
    for (Actor a : v.staff.lodgers()) {
      relations += a.relations.valueFor(f);
      numB++;
    }
    if (numB > 0) relations /= numB;
    final String relDesc = Relation.describeRelation(relations);
    final int relPC = (int) (relations * 100);
    d.append("\n  Relations: "+relPC+"% ("+relDesc+")");
    
    d.append("\n\n");
    d.append(v.helpInfo(), Colour.LITE_GREY);
  }
  
  
  
  /**  Listing demands, inventory and special items-
    */
  final public static Traded ITEM_LIST_ORDER[] = (Traded[]) Visit.compose(
    Traded.class, ALL_PROVISIONS, ALL_MATERIALS, ALL_SPECIAL_ITEMS
  );
  
  protected void describeStocks(Description d, HUD UI, Traded setStock[]) {
    
    final Traded[] demands = setStock == null ? v.stocks.demanded() : setStock;
    final Traded listOrder[] = setStock == null ? ITEM_LIST_ORDER : setStock;
    final Batch <Traded> special = new Batch();
    if (v.stocks.empty() && Visit.empty(demands)) return;
    
    d.append("Stocks and Production:");
    for (Traded t : listOrder) {
      final float needed = v.stocks.totalDemand(t);
      final float amount = v.stocks.amountOf   (t);
      
      if (Visit.arrayIncludes(demands, t) && t.common()) {
        describeStocks(t, d, setStock != null);
      }
      else if (needed > 0 || amount > 0) special.add(t);
    }
    for (Item i : v.stocks.allItems()) {
      if (Visit.arrayIncludes(listOrder, i.type)) continue;
      if (v.stocks.hasOrderFor(i)) continue;
      special.include(i.type);
    }
    
    if (! special.empty()) {
      Text.cancelBullet(d);
      d.append("\nOther Items:");
      for (Traded t : special) {
        if (t.common()) describeStocks(t, d, false);
        else for (Item i : v.stocks.matches(t)) {
          d.append("\n  ");
          i.describeTo(d);
        }
      }
    }
    if (v.stocks.specialOrders().size() > 0) {
      Text.cancelBullet(d);
      d.append("\nSpecial Orders:");
      for (Item i : v.stocks.specialOrders()) {
        d.append("\n  ");
        i.describeTo(d);
        final float progress = v.stocks.amountOf(i);
        d.append(" ("+(int) (progress * 100)+"%)");
      }
    }
  }
  
  
  protected boolean describeStocks(
    final Traded type, Description d, boolean set
  ) {
    final float
      consumption = v.stocks.consumption     (type),
      production  = v.stocks.production      (type),
      dailyCons   = v.stocks.dailyConsumption(type),
      dailyProd   = v.stocks.dailyProduction (type),
      amount      = v.stocks.amountOf        (type),
      dailySum    = dailyProd - dailyCons,
      stockMax    = Nums.min(25, v.spaceCapacity());
    
    Text.insert(type.icon.asTexture(), 20, 20, true, d);
    d.append("  "+I.shorten(amount, 1)+" ");
    d.append(type);
    
    if (set && v.stocks.isDemandFixed(type)) {
      d.append("/"+I.shorten(production + consumption, 1));
      if (dailySum > 0) d.append(" (+"+I.shorten( dailySum, 1)+"/day)");
      if (dailySum < 0) d.append(" (-"+I.shorten(-dailySum, 1)+"/day)");
      d.append("\n    ");
      final float
        nextCons = Nums.round((consumption + 5) % stockMax, 5, false),
        nextProd = Nums.round((production  + 5) % stockMax, 5, false)
      ;
      d.append(new Description.Link("Buys "+consumption) {
        public void whenClicked(Object context) {
          v.stocks.forceDemand(type, nextCons, 0);
        }
      });
      d.append(" ");
      d.append(new Description.Link("Sell "+production) {
        public void whenClicked(Object context) {
          v.stocks.forceDemand(type, 0, nextProd);
        }
      });
    }
    else {
      d.append("/"+I.shorten(production + consumption, 1));
      if (dailySum > 0) d.append(" (+"+I.shorten( dailySum, 1)+"/day)");
      if (dailySum < 0) d.append(" (-"+I.shorten(-dailySum, 1)+"/day)");
    }
    return true;
  }
  
  
  
  /**  Describing personnel, visitors and residents-
    */
  private void describeStaffing(Description d, HUD UI) {
    final Background c[] = v.careers();
    final Staff s = v.staff();
    if (Visit.empty(c) && s.lodgers().empty() && s.visitors().empty()) {
      return;
    }
    
    d.append("Staff and Visitors:");
    
    final Batch <Actor> mentioned = new Batch <Actor> ();
    if (c != null && c.length > 0) {
      
      for (Background b : c) {
        final int
          hired = v.staff.numHired    (b),
          total = v.staff.numPositions(b),
          apps  = v.staff.numApplied  (b);
        if (total == 0 && hired == 0) continue;
        
        Text.cancelBullet(d);
        d.append("  "+b.name+": ("+hired+"/"+total+")");
        if (apps > 0) d.append(" ("+apps+" applied)");
        
        for (FindWork a : v.staff.applications()) if (a.position() == b) {
          mentioned.include(a.actor());
          descApplicant(a.actor(), a, d, UI);
        }
        for (final Actor a : v.staff.workers()) if (a.mind.vocation() == b) {
          descActor(a, d, UI, v);
          mentioned.include(a);
        }
      }
    }
    
    Text.cancelBullet(d);
    final Batch <Actor> lodging = new Batch();
    for (Actor a : v.staff.lodgers()) {
      if (mentioned.includes(a)) continue;
      lodging.add(a);
    }
    if (lodging.size() > 0) {
      d.append("  Residents: ");
      for (Actor a : lodging) descActor(a, d, UI, v);
    }
    
    Text.cancelBullet(d);
    final Batch <Mobile> visits = new Batch();
    for (Mobile m : v.inside()) {
      if (! Staff.doesBelong(m, v)) visits.add(m);
    }
    if (! visits.empty()) {
      d.append("  Visitors: ");
      for (Mobile m : visits) descActor(m, d, UI, v);
      d.append("\n");
    }
  }
  
  
  private void describeOrders(Description d, final HUD UI) {
    final Base base = v.base();
    final int
      mainLevel = v.structure.mainUpgradeLevel(),
      maxLevel  = v.structure.maxMainLevel(),
      numU      = v.structure.numOptionalUpgrades(),
      maxU      = v.structure.maxOptionalUpgrades();
    
    final Series <Upgrade> UA = Upgrade.upgradesAvailableFor(v);
    final Upgrade inProg = v.structure.upgradeInProgress();
    final float   upProg = v.structure.upgradeProgress(inProg);
    final boolean canUp = maxU > 0 && UA.size() > 0;
    
    d.append("Orders and Upgrades:");
    if (canUp) {
      d.append("\n  Upgrades Installed: "+numU+"/"+maxU);
      for (final Upgrade upgrade : UA) {
        if (upgrade.type != Upgrade.Type.TECH_MODULE) continue;
        upgrade.appendVenueOrders(d, v, base);
        
        if (inProg == upgrade && upProg > 0) {
          d.append("\n    Progress: ");
          d.append(((int) (upProg * 100))+"%");
        }
      }
      
      if (maxLevel > 1) {
        d.append("\n  Structure Level: "+mainLevel+"/"+maxLevel);
      }
      if (v.structure.intact() && mainLevel < maxLevel) {
        final Upgrade VL[] = v.blueprint.venueLevels();
        final int level = v.structure.mainUpgradeLevel();
        final Upgrade next = VL[Nums.clamp(level, VL.length)];
        next.appendVenueOrders(d, v, base);
        
        if (inProg == next && upProg > 0) {
          d.append("\n    Progress: ");
          d.append(((int) (upProg * 100))+"%");
        }
      }
    }
    
    final Batch <Description.Link> orders = new Batch();
    addOrdersTo(orders);
    if (orders.size() > 0) {
      for (Description.Link link : orders) {
        d.append("\n  ");
        d.append(link);
      }
    }
    
    if (inProg != null && upProg <= 0) d.append(
      "\nUpgrades will be installed once your engineering staff arrive "+
      "on-site.", Colour.LITE_GREY
    );
  }
  
  
  protected void addOrdersTo(Series <Description.Link> orderList) {
    final Base played = BaseUI.currentPlayed();
    
    if (played == v.base()) {
      if (v.structure.needsSalvage()) {
        orderList.add(new Description.Link("Cancel Salvage") {
          public void whenClicked(Object context) {
            v.structure.cancelSalvage();
            if (I.logEvents()) I.say("\nCANCELLED SALVAGE: "+v);
          }
        });
      }
      else {
        orderList.add(new Description.Link("Salvage Structure") {
          public void whenClicked(Object context) {
            v.structure.beginSalvage();
            if (I.logEvents()) I.say("\nBEGAN SALVAGE: "+v);
          }
        });
      }
    }
  }
  
  
  
  /**  Utility methods for actor-description that tend to get re-used
    *  elsewhere...
    */
  public static void descActor(Mobile m, Description d, HUD UI, Venue v) {
    
    final Composite p = m.portrait(UI);
    final String ID = ""+m.hashCode();
    if (p != null) Text.insert(p.delayedImage(UI, ID), 40, 40, true, d);
    else d.append("\n ");
    
    d.append(m);
    
    d.append("\n");
    m.describeStatus(d, v);
    
    if (m instanceof Actor) {
      final Actor a = (Actor) m;
      final Profile profile = a.base().profiles.profileFor(a);
      
      String actionDesc = "("+a.mind.vocation()+")";
      if (a.mind.work() == v) {
        if (profile.downtimeDays() > 0) {
          actionDesc = "(On Leave)";
        }
        else if (v.staff.onShift(a)) {
          actionDesc = "(On-Duty)";
        }
        else {
          actionDesc = "(Off-Duty)";
        }
      }
      d.append("\n  "+actionDesc, Colour.LITE_GREY);
    }
  }
  
  
  public static void descApplicant(
    Actor a, final FindWork sought, Description d, HUD UI
  ) {
    final Composite comp = a.portrait(UI);
    if (comp != null) Text.insert(comp.texture(), 40, 40, true, d);
    else d.append("\n");
    
    d.append(a);
    d.append(a.inWorld() ? " (" : " (Offworld ");
    d.append(a.mind.vocation().name+")");
    if (sought == null || sought.wasHired()) return;
    
    final Series <Trait>
      TD = ActorDescription.sortTraits(a.traits.personality(), a),
      SD = ActorDescription.sortTraits(a.traits.skillSet()   , a);
    
    int numS = 0;
    for (Trait s : SD) {
      if (sought.position().skillLevel((Skill) s) <= 0) continue;
      if (++numS > 3) break;
      d.append("\n  "+s+" ("+((int) a.traits.traitLevel(s))+") ");
    }
    d.append("\n  ");
    int numT = 0;
    for (Trait t : TD) {
      if (++numT > 3) break;
      d.append(t+" ");
    }
    
    d.append("\n  ");
    final String hireDesc = "Hire for "+sought.hiringFee()+" credits";
    d.append(new Description.Link(hireDesc) {
      public void whenClicked(Object context) { sought.confirmApplication(); }
    });
    d.append(new Description.Link("  Dismiss") {
      public void whenClicked(Object context) { sought.cancelApplication(); }
    });
  }
}









