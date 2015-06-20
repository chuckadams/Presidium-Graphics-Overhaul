/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;



public class DebugEcology extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugEcology());
  }
  
  
  private DebugEcology() {
    super("debug_ecology", true);
  }
  
  
  public DebugEcology(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_ecology");
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
    return Base.settlement(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    GameSettings.noChat    = true;
    
    if (false) configHuntingScenario(world, base, UI);
    if (false) nestPlaceTest        (world, base, UI);
    if (false) configHarvestScenario(world, base, UI);
    if (true ) configReactionsTest  (world, base, UI);
  }
  
  
  private void configReactionsTest(Stage world, Base base, BaseUI UI) {
    
    GameSettings.noBlood = true;
    
    final Base wildlife = Base.wildlife(world);
    final Actor fauna = Lictovore.SPECIES.sampleFor(wildlife);
    fauna.health.setupHealth(1, 1, 0);
    fauna.enterWorldAt(6, 6, world);
    
    final Actor person = new Human(Backgrounds.ECOLOGIST, base);
    person.enterWorldAt(8, 8, world);
    
    float novelty = fauna.relations.noveltyFor(person.base());
    I.say("Base novelty is: "+novelty);
    
    UI.selection.pushSelection(fauna);
  }
  
  
  private void configHarvestScenario(Stage world, Base base, BaseUI UI) {
    
    final KommandoRedoubt lodge = new KommandoRedoubt(base);
    SiteUtils.establishVenue(lodge, world.tileAt(20, 2), true, world);
    base.setup.fillVacancies(lodge, true);
    lodge.stocks.forceDemand(Economy.PROTEIN, 20, false);
    
    Actor tracks = lodge.staff.workers().last();
    UI.selection.pushSelection(tracks);
    
    final Base wildlife = Base.wildlife(world);
    Nest.populateFauna(world, Qudu.SPECIES);
    Actor prey = Qudu.SPECIES.sampleFor(wildlife);
    prey.enterWorldAt(lodge, world);
    prey.health.setupHealth(0.5f, 1, 0);
    prey.health.takeInjury(100, false);
    
    Hunting harvest = Hunting.asHarvest(tracks, prey, lodge, false);
    harvest.addMotives(Plan.MOTIVE_JOB, 100);
    tracks.mind.assignBehaviour(harvest);
  }
  
  
  private void configHuntingScenario(Stage world, Base base, BaseUI UI) {
    final Base wildlife = Base.wildlife(world);
    
    final Actor hunts = Lictovore.SPECIES.sampleFor(wildlife);
    hunts.enterWorldAt(3, 3, world);
    hunts.health.setCaloryLevel(0.5f);
    
    final Actor prey = Qudu.SPECIES.sampleFor(wildlife);
    prey.health.setupHealth(1, 1, 0);
    prey.enterWorldAt(world.tileAt(6, 6), world);
    prey.health.takeFatigue(prey.health.maxHealth());
    
    hunts.mind.assignBehaviour(Hunting.asFeeding(hunts, prey));
    UI.selection.pushSelection(hunts);
  }
  
  
  private void nestPlaceTest(Stage world, Base base, BaseUI UI) {
    
    final Outcrop o = new Outcrop(2, 2, Outcrop.TYPE_MESA);
    o.setPosition(10, 10, world);
    o.attachModel(Outcrop.modelFor(o, world));
    o.enterWorld();
    
    final Base wildlife = Base.wildlife(world);
    final Nest n = (Nest) Qudu.SPECIES.nestBlueprint().createVenue(wildlife);
    n.setPosition(10, 8, world);
    
    I.say("Placement possible? "+n.canPlace());
    
    n.enterWorld();
    n.structure.setState(Structure.STATE_INTACT, 1);
    wildlife.setup.fillVacancies(n, true);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}













