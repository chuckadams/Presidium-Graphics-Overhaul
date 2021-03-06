/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.content.civic.*;
import stratos.content.hooks.StratosSetting;
import stratos.content.wip.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.content.hooks.StratosSetting.SECTOR_ELYSIUM;
import static stratos.game.actors.Backgrounds.*;
import stratos.graphics.common.Colour;



public class DebugSocial extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugSocial());
  }
  
  
  private DebugSocial() {
    super("debug_social", true);
  }
  
  
  public DebugSocial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected Stage createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.5f,
      Habitat.CURSED_EARTH, 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f,
      Habitat.SAVANNAH    , 1f
    );
    final Verse verse = new StratosSetting();
    final Sector at = SECTOR_ELYSIUM;
    final Stage world = Stage.createNewWorld(verse, at, TG.generateTerrain());
    TG.setupOutcrops(world);
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Faction.FACTION_SUHAIL);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.cashFree = true;
    
    if (false) testCareers(base);
    if (true ) configContactScenario (world, base, UI);
    if (false) configDialogueScenario(world, base, UI);
    if (false) configArtilectScenario(world, base, UI);
    if (false) configWildScenario    (world, base, UI);
    if (false) applyJobScenario      (world, base, UI);
    if (false) multiJobsScenario     (world, base, UI);
  }
  
  
  protected void afterCreation() {
    world().readyAfterPopulation();
  }
  
  
  private void configContactScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree  = true;
    GameSettings.hireFree = true;
    GameSettings.noBlood  = true;
    //
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    final Actor
      ruler   = new Human(Backgrounds.KNIGHTED     , base),
      consort = new Human(Backgrounds.FIRST_CONSORT, base);
    
    bastion.setupWith(world.tileAt(11, 11), null);
    bastion.doPlacement(true);
    base.setup.fillVacancies(bastion, true, ruler, consort);
    if (bastion.inWorld()) {
      base.assignRuler(ruler);
      bastion.updateAsScheduled(0, false);
      for (Item i : bastion.stocks.shortages()) bastion.stocks.addItem(i);
    }
    final TrooperLodge garrison = new TrooperLodge(base);
    SiteUtils.establishVenue(garrison, world.tileAt(3, 15), -1, true, world);
    
    final int tribeID = NativeHut.TRIBE_FOREST;
    final Base natives = Base.natives(world, tribeID);
    NativeHut hut = NativeHut.newHut(tribeID, natives);
    hut.setupWith(world.tileAt(28, 28), null);
    hut.doPlacement(true);
    natives.setup.fillVacancies(hut, true);
    
    Actor talks = hut.staff.lodgers().first();
    
    //  Negotiators will have to be willing to wait outside a venue until a
    //  subject emerges.
    
    /*
    //
    //  Introduce some natives to contact, some distance away-
    final Base natives = Base.natives(world, NativeHut.TRIBE_FOREST);
    final Actor talks = new Human(Backgrounds.GATHERER, natives);
    talks.enterWorldAt(18, 18, world);
    //
    //  Then configure a contact mission asking to secure audience with the
    //  natives.
    final MissionContact peaceMission = new MissionContact(base, talks);
    peaceMission.assignPriority(Mission.PRIORITY_ROUTINE);
    peaceMission.setMissionType(Mission.TYPE_SCREENED);
    final Item gift = Item.withAmount(Economy.PROTEIN, 5);
    peaceMission.setTerms(
      Pledge.giftPledge(gift, bastion, ruler, talks),
      Pledge.joinBasePledge(talks, base)
    );
    base.tactics.addMission(peaceMission);
    consort.mind.assignMission(peaceMission);
    peaceMission.setApprovalFor(consort, true);
    peaceMission.beginMission();
    Selection.pushSelection(peaceMission, null);
    //*/
  }
  
  
  private void configDialogueScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.noBlood  = true;
    GameSettings.hireFree = true;
    GameSettings.fogFree  = true;
    
    Actor a1 = null, a2 = null;
    for (int n = 2; n-- > 0;) {
      a1 = new Human(Backgrounds.CULTIVATOR, base);
      a1.enterWorldAt(2 + n, 3 + n, world);
      if (a2 == null) a2 = a1;
    }
    
    a1.motives.setSolitude(1);
    a2.motives.setSolitude(1);
    
    final Dialogue d = Dialogue.dialogueFor(a1, a2);
    a1.mind.assignBehaviour(d);
    Selection.pushSelection(a1, null);
    
    Actor a3 = new Human(Backgrounds.TECHNICIAN, base);
    Actor a4 = new Human(Backgrounds.TECHNICIAN, base);
    a3.enterWorldAt(7 , 7, world);
    a4.enterWorldAt(10, 5, world);
    
    final Item gift = Item.withAmount(Economy.GREENS, 4);
    a3.gear.addItem(gift);
    
    final Negotiation d2 = new Negotiation(a3, a4);
    d2.addMotives(Plan.MOTIVE_LEISURE, Plan.ROUTINE);
    d2.setTerms(Pledge.giftPledge(gift, a3, a3, a4), null);
    a3.mind.assignBehaviour(d2);
    
    Selection.pushSelection(a3, null);
    
    //  TODO:  RE-TEST THE ENTIRE GIFT-GETTING ROUTINE, INCLUDING PURCHASES AND
    //  COMMISSIONS.
  }
  
  
  private void configArtilectScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree   = true;
    GameSettings.buildFree = true;
    
    final Base artilects = Base.artilects(world);
    final Ruins ruins = new Ruins(artilects);
    SiteUtils.establishVenue(ruins, 20, 20, -1, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    
    UI.assignBaseSetup(artilects, ruins.position(null));
    artilects.setup.fillVacancies(ruins, true);
    
    final Career career = new Career(
      Backgrounds.COMPANION,
      Backgrounds.BORN_GELDER,
      StratosSetting.PLANET_AXIS_NOVENA,
      null
    );
    final Human subject = new Human(career, base.faction());
    subject.enterWorldAt(22, 29, world);
    subject.health.takeInjury(subject.health.maxHealth() + 1, true);
    subject.health.setState(ActorHealth.STATE_DEAD);
    Selection.pushSelection(subject, null);
    
    final Actor finds = ruins.staff.hiredAs(Tripod.SPECIES).first();
    if (finds != null) finds.goAboard(world.tileAt(20, 25), world);
  }
  
  
  private void applyJobScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree   = true;
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    
    final Venue applyAt = new EngineerStation(base);
    SiteUtils.establishVenue(applyAt, 4, 4, -1, true, world);
    
    final Venue secondary = new Cantina(base);
    SiteUtils.establishVenue(secondary, 4, 9, -1, true, world);
    base.setup.fillVacancies(secondary, true);
    
    final Venue applyFrom = new EngineerStation(base);
    SiteUtils.establishVenue(applyFrom, 9, 9, -1, true,
      world, new Human(Backgrounds.TECHNICIAN, base)
    );
    
    final Venue powers = new SolarBank(base);
    SiteUtils.establishVenue(powers, 9, 4, -1, true, world);
    
    final Actor applies = applyFrom.staff.workers().first();
    FindWork.assignAmbition(applies, Backgrounds.ENGINEER, applyAt, 2);
    Selection.pushSelection(applies, null);
  }
  
  
  private void configWildScenario(Stage world, Base base, BaseUI UI) {
    
    GameSettings.fogFree = true;
    final Base wild = Base.wildlife(world);
    final Species species = Yamagur.SPECIES;
    
    Venue nests = species.nestBlueprint().createVenue(wild);
    SiteUtils.establishVenue(nests, 9, 9, -1, true, world);
    
    Actor fauna = species.sampleFor(wild);
    fauna.enterWorldAt(7, 7, world);
    fauna.mind.setHome(nests);
    
    Actor meets = new Human(Backgrounds.VOLUNTEER, base);
    meets.enterWorldAt(12, 12, world);
    
    Actor watch = new Human(Backgrounds.ECOLOGIST, base);
    watch.enterWorldAt(13, 13, world);
    
    MissionUtils.quickSetup(
      MissionStrike.strikeFor(nests, base),
      Mission.PRIORITY_ROUTINE, Mission.TYPE_SCREENED,
      meets
    );
    Selection.pushSelection(watch, null);
  }
  

  private void multiJobsScenario(Stage world, Base base, BaseUI UI) {
    
    GameSettings.hireFree = false;
    GameSettings.fogFree  = true ;
    FindWork.rateVerbose  = true ;
    Tile start = world.tileAt(20, 20);
    
    Bastion bastion = new Bastion(base);
    SiteUtils.establishVenue(bastion, start, -1, true, world);
    
    Background jobs[] = { TROOPER, TECHNICIAN, SUPPLY_CORPS };
    Actor tracked = null;
    for (Background b : jobs) {
      Actor a = b.sampleFor(base);
      a.enterWorldAt(bastion, world);
      if (tracked == null) tracked = a;
    }
    
    SiteUtils.establishVenue(new TrooperLodge   (base), start, -1, true, world);
    SiteUtils.establishVenue(new EngineerStation(base), start, -1, true, world);
    SiteUtils.establishVenue(new SupplyDepot    (base), start, -1, true, world);
    Selection.pushSelection(tracked, null);
  }
  
  
  private void testCareers(Base base) {
    //
    //  Just checking on the probability distribution in the careers system-
    I.say("\nGenerating random companions:");
    final int runs = 125;
    int numM = 0, numF = 0;
    final Tally <Trait> frequencies = new Tally <Trait> ();
    
    for (int n = runs; n-- > 0;) {
      I.say("\nGENERATING NEW ACTOR...");
      final Human comp = new Human(Backgrounds.COMPANION, base);
      I.say("  Gender for "+comp+" is "+comp.traits.genderDescription());
      if (comp.traits.female()) numF++;
      if (comp.traits.male  ()) numM++;
      frequencies.add(1, Human.raceFor(comp));
    }
    I.say("\nFinal results: ("+runs+" total)");
    I.say("  "+numF+" female");
    I.say("  "+numM+" male"  );
    for (Trait t : frequencies.keysToArray(Trait.class)) {
      I.say("  "+frequencies.valueFor(t)+" "+t);
    }
  }
  
}



