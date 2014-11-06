

package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



public class DebugTreating extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugTreating());
  }
  
  
  private DebugTreating() {
    super();
  }
  
  
  public DebugTreating(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_treating");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_treating";
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
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    
    final Actor treats = new Human(Backgrounds.PHYSICIAN, base);
    final Venue station = new PhysicianStation(base);
    Placement.establishVenue(station, 4, 4, true, world, treats);
    
    final Venue vats = new CultureLab(base);
    Placement.establishVenue(vats, 10, 4, true, world);
    
    final Actor treated = new Human(Backgrounds.EXCAVATOR, base);
    treated.enterWorldAt(4, 10, world);
    
    //  And now, we proceed to do various horrible things to the subject...
    //treated.traits.incLevel(Conditions.HIREX_PARASITE, 0.99f);
    treated.health.takeInjury(treated.health.maxHealth() + 1, true);
    
    UI.selection.pushSelection(treats, true);
  }
  
  
  protected void afterCreation() {
  }
}









