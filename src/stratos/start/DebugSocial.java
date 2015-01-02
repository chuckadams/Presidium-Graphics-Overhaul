


package stratos.start;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.game.base.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import stratos.graphics.common.Colour;




public class DebugSocial extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugSocial());
  }
  
  
  private DebugSocial() {
    super();
  }
  
  
  public DebugSocial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_social");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_social";
  }
  
  
  protected Stage createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.ESTUARY     , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final Stage world = new Stage(TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.terrain().readyAllMeshes();
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.withName(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    world.offworld.assignLocalSector(
      Sectors.SECTOR_PAVONIS,
      Sectors.PLANET_DIAPSOR
    );
    
    if (false) testCareers(base);
    if (true ) configDialogueScenario(world, base, UI);
    if (false) configArtilectScenario(world, base, UI);
    if (false) configContactScenario (world, base, UI);
    if (false) configWildScenario    (world, base, UI);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    if (base().finance.credits() < 0) base().finance.incCredits(100, "CHARITY");
  }
  
  
  private void configDialogueScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.noBlood  = true;
    GameSettings.hireFree = true;
    GameSettings.fogFree  = true;
    
    /*
    final EngineerStation venue = new EngineerStation(base);
    Placement.establishVenue(venue, world.tileAt(4, 4), true, world);
    venue.stocks.bumpItem(Economy.PARTS, 10);
    //*/
    
    //*
    Actor citizen = null, other = null;
    for (int n = 2; n-- > 0;) {
      citizen = new Human(Backgrounds.CULTIVATOR, base);
      citizen.enterWorldAt(2 + n, 3 + n, world);
      if (other == null) other = citizen;
    }
    
    final Dialogue d = new Dialogue(citizen, other);
    d.setMotive(Plan.MOTIVE_LEISURE, Plan.CASUAL);
    citizen.mind.assignBehaviour(d);
    UI.selection.pushSelection(citizen, true);
    //*/
    
    //  TODO:  RE-TEST THIS
    /*
    world.presences.togglePresence(
      venue, venue.origin(), true, Economy.PARTS.supplyKey
    );
    final Delivery getting = DeliveryUtils.bestCollectionFor(
      citizen, Economy.PARTS, 1, null, 5, false
    );
    getting.shouldPay = null;
    
    final Item gift = Item.withAmount(Economy.PARTS, 1);
    final Gifting gifting = new Gifting(
      citizen, other, gift, getting
    );
    citizen.mind.assignBehaviour(gifting);
    //*/
  }
  
  
  private void configArtilectScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;

    final Base artilects = Base.artilects(world);
    
    final Ruins ruins = new Ruins(artilects);
    Placement.establishVenue(ruins, 20, 20, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    
    UI.assignBaseSetup(artilects, ruins.position(null));
    artilects.setup.fillVacancies(ruins, true);
    
    final Career career = new Career(
      Backgrounds.COMPANION,
      Backgrounds.BORN_GELDER,
      Sectors.PLANET_AXIS_NOVENA,
      null
    );
    final Human subject = new Human(career, base);
    
    I.say("Subject race is: "+Human.raceFor(subject));
    I.say("Subject gender is: "+subject.traits.genderDescription());
    
    subject.enterWorldAt(22, 29, world);
    subject.health.takeInjury(subject.health.maxHealth() + 1, true);
    subject.health.setState(ActorHealth.STATE_DYING);
    UI.selection.pushSelection(subject, true);
  }
  
  
  private void configContactScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    GameSettings.hireFree = true;
    GameSettings.noBlood = true;
    world.advanceCurrentTime(Stage.STANDARD_DAY_LENGTH / 2);
    /*
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    bastion.stocks.bumpItem(Economy.PROTEIN, 20);
    bastion.stocks.bumpItem(Economy.PLASTICS, 10);
    bastion.stocks.bumpItem(Economy.SPYCE_T, 5);
    final Actor ruler = new Human(Backgrounds.KNIGHTED, base);
    Placement.establishVenue(
      bastion, 11, 11, true, world,
      ruler, new Human(Backgrounds.FIRST_CONSORT, base)
    );
    base.assignRuler(ruler);
    
    final TrooperLodge garrison = new TrooperLodge(base);
    Placement.establishVenue(garrison, world.tileAt(3, 15), true, world);
    //*/

    final Base natives = Base.natives(world, NativeHut.TRIBE_FOREST);
    final BaseSetup NS = natives.setup;
    
    /*
    Batch <Actor> allTalk = new Batch <Actor> ();
    Actor talks = null;
    for (int n = 3; n-- > 0;) {
      talks = Backgrounds.HUNTER.sampleFor(natives);
      talks.enterWorldAt(11 + n, 11 - n, world);
      allTalk.add(talks);
    }
    //NS.establishRelations(allTalk);
    //*/
    
    //*
    final Batch <Venue> halls = NS.doPlacementsFor(
      NativeHut.TRIBE_FOREST_PROFILES[0], 2//2 + Rand.index(2)
    );
    final Batch <Venue> huts  = NS.doPlacementsFor(
      NativeHut.TRIBE_FOREST_PROFILES[1], 3//3 + Rand.index(4)
    );
    
    NS.fillVacancies(huts , true);
    NS.fillVacancies(halls, true);
    for (Venue v : huts ) NS.establishRelationsAt(v);
    for (Venue v : halls) NS.establishRelationsAt(v);
    
    final Actor talks = huts.first().staff.workers().first();
    //*/
    
    final Relation withBase = talks.relations.relationWith(natives);
    I.say("BASE RELATION IS: "+withBase.value  ());
    I.say("BASE NOVELTY IS:  "+withBase.novelty());
    
    UI.selection.pushSelection(talks, true);
    
    /*
    final Mission peaceMission = new ContactMission(base, hut);
    peaceMission.assignPriority(Mission.PRIORITY_ROUTINE);
    peaceMission.setObjective(ContactMission.OBJECT_AUDIENCE);
    base.addMission(peaceMission);
    peaceMission.beginMission();
    
    UI.selection.pushSelection(peaceMission, true);
    //*/
  }
  
  
  private void configWildScenario(Stage world, Base base, BaseUI UI) {
    
    //  This is the last scenario to test.  Just introduce some animals, and
    //  see how they react to (A) eachother and (B) the nearby base.
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
  
  
  protected void afterCreation() {
  }
}


