

package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.tactical.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;





//  Okay.  I think that just leaves negotiation to iron out.  Implement the
//  'first impressions' system for relations, plus the modifier for different
//  bases, (and maybe backgrounds, et cetera.)

//  Then implement a system which sets relations for the base as a whole at the
//  average of relations for acquainted members.  In theory, this *should*
//  overcome the problem of individual members being hostile/attacking while
//  others are blithe and indifferent.

//  Just be sure to test this in Mission form too.


public class DebugMissions extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugMissions());
  }
  
  
  private DebugMissions() {
    super();
  }
  
  
  public DebugMissions(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    //super.beginGameSetup();
    super.initScenario("debug_combat");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_combat";
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
  

  //  TODO:  I want a situation where lictovores are usually hostile, vareen
  //  sometimes are, and quud basically never are, based on power and
  //  aggression.
  
  //  70%, 30%, and -10% chance to attack.  +30% if under attack.  -20% if
  //  matched by enemy, -50% if heavily outmatched.  +30% if they can't
  //  escape.
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = false;
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.fogFree   = true;
    
    if (false) strikeScenario  (world, base, UI);
    if (false) securityScenario(world, base, UI);
    if (false) contactScenario (world, base, UI);
    if (true ) reconScenario   (world, base, UI);
  }
  
  
  private void contactScenario(Stage world, Base base, BaseUI UI) {
    final Base natives = Base.baseWithName(world, Base.KEY_NATIVES, true);
    natives.relations.setRelation(base, -0.25f, true);
    
    final NativeHut hut = NativeHut.newHall(1, natives);
    Placement.establishVenue(hut, 4, 4, true, world);
    
    for (int i = 3; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.GATHERER, natives);
      actor.enterWorldAt(hut.mainEntrance(), world);
      actor.mind.setHome(hut);
      actor.mind.setWork(hut);
    }
    
    final Mission contact = new ContactMission(base, hut);
    contact.setMissionType(Mission.TYPE_SCREENED  );
    contact.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 1; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      actor.enterWorldAt(2, 2, world);
      actor.mind.assignMission(contact);
      contact.setApprovalFor(actor, true);
      UI.selection.pushSelection(actor, true);
    }
    
    base.addMission(contact);
    contact.beginMission();
  }
  
  
  private void securityScenario(Stage world, Base base, BaseUI UI) {
    final Base artilects = Base.baseWithName(world, Base.KEY_ARTILECTS, true);
    
    final Actor robot = Species.SPECIES_TRIPOD.newSpecimen(artilects);
    robot.enterWorldAt(32, 32, world);
    
    final Actor vet = new Human(Backgrounds.VETERAN, base);
    vet.enterWorldAt(4, 4, world);
    
    final Combat combat = new Combat(robot, vet);
    combat.setMotive(Plan.MOTIVE_EMERGENCY, 100);
    robot.mind.assignBehaviour(combat);
    
    final Mission security = new SecurityMission(base, vet);
    security.setMissionType(Mission.TYPE_SCREENED  );
    security.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 5; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(vet, 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      actor.mind.assignMission(security);
      security.setApprovalFor(actor, true);
      UI.selection.pushSelection(actor, true);
    }
    
    base.addMission(security);
    security.beginMission();
  }
  
  
  private void strikeScenario(Stage world, Base base, BaseUI UI) {
    final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true);
    
    final Actor fauna = Species.LICTOVORE.newSpecimen(wildlife);
    fauna.health.setupHealth(0.5f, 1, 0);
    fauna.enterWorldAt(10, 10, world);
    UI.selection.pushSelection(fauna, true);
    
    final Mission strike = new StrikeMission(base, fauna);
    strike.setMissionType(Mission.TYPE_SCREENED  );
    strike.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 4; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(world.tileAt(4, 4), 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      actor.mind.assignMission(strike);
      strike.setApprovalFor(actor, true);
    }
    
    base.addMission(strike);
    strike.beginMission();
  }
  
  
  private void reconScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = false;
    
    final Mission recon = new ReconMission(base, world.tileAt(20, 20));
    recon.setMissionType(Mission.TYPE_SCREENED);
    recon.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 3; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(world.tileAt(4, 4), 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      actor.mind.assignMission(recon);
      recon.setApprovalFor(actor, true);
      UI.selection.pushSelection(actor, true);
    }
    
    base.addMission(recon);
    recon.beginMission();
    
    //  TODO:  Let's introduce a nest of Yamagur.  See how they get along.
  }
  
  
  protected void afterCreation() {
  }
}









