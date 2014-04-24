

package stratos.game.campaign;
import java.io.*;

import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.wild.Nest;
import stratos.game.wild.Ruins;
import stratos.user.*;
import stratos.util.*;



//Planitia Sector (robots + ruins, artifact.)
//Pavonis Sector (mild settlement, pacified.)
//Aeolis Sector (desert plus wildlife.)
//Marineris Sector (neutral + water/islands.  Some wildlife.)



public class StartupScenario extends Scenario {
  
  
  final public static int
    MAX_SKILLS    = 3,
    MAX_TRAITS    = 2,
    MAX_ADVISORS  = 2,
    MAX_COLONISTS = 9,
    MAX_PERKS     = 3 ;
  final public static Background ADVISOR_BACKGROUNDS[] = {
    Backgrounds.FIRST_CONSORT,
    Backgrounds.MINISTER_FOR_ACCOUNTS,
    Backgrounds.WAR_MASTER
  } ;
  final public static Background COLONIST_BACKGROUNDS[] = {
    Backgrounds.VOLUNTEER,
    Backgrounds.SUPPLY_CORPS,
    Backgrounds.FABRICATOR,
    Backgrounds.TECHNICIAN,
    Backgrounds.CULTIVATOR,
    Backgrounds.MINDER
  } ;
  final public static String
    SITE_DESC[] = {
      "Wasteland",
      "Wilderness",
      "Settled"
    },
    FUNDING_DESC[] = {
      "Minimal  (7500  Credits, 3% interest)",
      "Standard (10000 Credits, 2% interest)",
      "Generous (12500 Credits, 1% interest)"
    },
    TITLE_MALE[] = {
      "Knighted Lord (Small Estate)",
      "Count (Typical Estate)",
      "Baron (Large Estate)"
    },
    TITLE_FEMALE[] = {
      "Knighted Lady (Small Estate)",
      "Countess (Typical Estate)",
      "Baroness (Large Estate)"
    } ;
  final public static int
    MAP_SIZES[] = { 64, 128, 256 },
    
    SITE_WASTELAND  = 0,
    SITE_WILDERNESS = 1,
    SITE_SETTLED    = 2,
    
    FUNDING_MINIMAL  = 0,
    FUNDING_STANDARD = 1,
    FUNDING_GENEROUS = 2,
    
    TITLE_KNIGHTED = 0,
    TITLE_COUNT    = 1,
    TITLE_BARON    = 2;
  
  public static class Config {
    //  TODO:  Just pick House, Province, Options.  And a few perks.
    
    public Background house;
    
    public boolean male;
    public List <Trait> chosenTraits = new List <Trait> ();
    public List <Skill> chosenSkills = new List <Skill> ();
    public List <Background> advisors = new List <Background> ();
    public Table <Background, Integer> numCrew = new Table();
    
    public int siteLevel, fundsLevel, titleLevel;
    
    public int numCrew(Background b) {
      final Integer num = numCrew.get(b);
      return num == null ? 0 : num;
    }
  }
  
  final Config config;
  
  
  
  public StartupScenario(Config config) {
    super();
    this.config = config;
  }
  
  
  public StartupScenario(Session s) throws Exception {
    super(s);
    this.config = new Config();
    config.house = (Background) s.loadObject();
    config.male = s.loadBool();
    s.loadObjects(config.chosenTraits);
    s.loadObjects(config.chosenSkills);
    s.loadObjects(config.advisors);
    for (int i = s.loadInt(); i-- > 0;) {
      config.numCrew.put((Background) s.loadObject(), s.loadInt());
    }
    config.siteLevel  = s.loadInt();
    config.fundsLevel = s.loadInt();
    config.titleLevel = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(config.house);
    s.saveBool(config.male);
    s.saveObjects(config.chosenTraits);
    s.saveObjects(config.chosenSkills);
    s.saveObjects(config.advisors);
    s.saveInt(config.numCrew.size());
    for (Background b : config.numCrew.keySet()) {
      s.saveObject(b);
      s.saveInt(config.numCrew.get(b));
    }
    s.saveInt(config.siteLevel);
    s.saveInt(config.fundsLevel);
    s.saveInt(config.titleLevel);
  }
  
  
  
  /**  Required setup methods-
    */
  protected World createWorld() {
    final int station = config.titleLevel ;
    float water = 2 ;
    float forest = 0, meadow = 0, barrens = 0, desert = 0, wastes = 0 ;
    
    switch (config.siteLevel) {
      case(0) : wastes = 3 ; desert  = 2 ; barrens = 4 ; water = 0 ; break ;
      case(1) : meadow = 4 ; barrens = 2 ; desert  = 2 ; water = 3 ; break ;
      case(2) : forest = 2 ; meadow  = 3 ; barrens = 2 ; water = 1 ; break ;
    }
    
    //  TODO:  the terrain setup algorithm should not be directly interacting
    //  with the world, only passing data onto the constructor.  The
    //  'readyAllMeshes()' method should be called automatically then.
    final TerrainGen TG = new TerrainGen(
      MAP_SIZES[station], 0.2f,
      Habitat.OCEAN       , water  ,
      Habitat.ESTUARY     , forest ,
      Habitat.MEADOW      , meadow ,
      Habitat.BARRENS     , barrens,
      Habitat.DUNE        , desert ,
      Habitat.CURSED_EARTH, wastes
    );
    final World world = new World(TG.generateTerrain());
    TG.setupMinerals(world, 0, 0, 0);
    TG.setupOutcrops(world);
    world.terrain().readyAllMeshes();
    Flora.populateFlora(world);
    return world ;
  }
  
  
  protected Base createBase(World world) {
    final Base base = Base.baseWithName(world, "Player", false) ;
    
    int funding = -1, interest = -1 ;
    switch (config.fundsLevel) {
      case(0) : funding = 7500  ; interest = 3 ; break ;
      case(1) : funding = 10000 ; interest = 2 ; break ;
      case(2) : funding = 12500 ; interest = 1 ; break ;
    }
    base.incCredits(funding) ;
    base.setInterestPaid(interest) ;
    base.commerce.assignHomeworld((System) config.house) ;
    return base ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    //
    //  Determine relevant attributes for the ruler-
    final Human ruler = ruler(base);
    
    //
    //  Try to pick out some complementary advisors-
    final List <Human> advisors = advisors(base, ruler);
    
    //
    //  Pick out some random colonists-
    final List <Human> colonists = colonists(base);
    
    //  Establish the position of the base site-
    final Bastion bastion = establishBastion(
      world, base, ruler, advisors, colonists
    );
    UI.assignBaseSetup(base, bastion.position(null));
    
    //  And establish the locals too-
    establishLocals(world);
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    String title = base.ruler().fullName();
    while (true) {
      File match = new File(Scenario.fullSavePath(title, null));
      if (! match.exists()) break;
      title = title+"I";
    }
    return title;
  }
  
  
  protected void afterCreation() {
    //saveProgress(false);
  }
  
  
  
  /**  Private helper methods-
    */
  protected Human ruler(Base base) {
    
    final int station = config.titleLevel ;
    final float promoteChance = (25 - (station * 10)) / 100f ;
    final Background vocation = Backgrounds.RULING_POSITIONS[station] ;
    final Background birth ;
    if (Rand.num() < promoteChance) {
      if (Rand.num() < promoteChance) birth = Backgrounds.FREE_BIRTH ;
      else birth = Backgrounds.GELDER_BIRTH ;
    }
    else birth = Backgrounds.HIGH_BIRTH ;
    
    final Background house = config.house;
    final Career rulerCareer = new Career(config.male, vocation, birth, house);
    final Human ruler = new Human(rulerCareer, base) ;
    for (Skill s : house.skills()) ruler.traits.incLevel(s, 5) ;
    
    return ruler ;
  }
  
  
  protected List <Human> advisors(Base base, Actor ruler) {
    final List <Human> advisors = new List <Human> () ;
    final int numTries = 5 ;
    for (Background b : config.advisors) {
      Human picked = null ;
      float bestRating = Float.NEGATIVE_INFINITY ;
      
      for (int i = numTries ; i-- > 0 ;) {
        final Human candidate = new Human(b, base) ;
        float rating = Career.ratePromotion(b, candidate) ;
        
        if (b == Backgrounds.FIRST_CONSORT) {
          rating +=
            ruler.mind.attraction(candidate) +
            (candidate.mind.attraction(ruler) / 2) ;
        }
        if (rating > bestRating) { picked = candidate ; bestRating = rating ; }
      }
      if (picked != null) advisors.add(picked) ;
    }
    
    return advisors;
  }
  
  
  protected List <Human> colonists(Base base) {
    final List <Human> colonists = new List <Human> () ;
    final Background house = config.house;
    
    for (int i = COLONIST_BACKGROUNDS.length ; i-- > 0 ;) {
      final Background b = COLONIST_BACKGROUNDS[i];
      for (int n = config.numCrew(b) ; n-- > 0 ;) {
        final Human c = new Human(b, base) ;
        for (Skill s : house.skills()) if (c.traits.traitLevel(s) > 0) {
          c.traits.incLevel(s, 5) ;
        }
        colonists.add(c) ;
      }
    }
    
    return colonists;
  }
  
  
  protected Bastion establishBastion(
    final World world, Base base,
    Human ruler, List <Human> advisors, List <Human> colonists
  ) {
    final Bastion bastion = new Bastion(base);
    advisors.add(ruler);
    base.assignRuler(ruler);
    final Human AA[] = advisors.toArray(Human.class);
    
    final Tile target = world.tileAt(world.size / 2, world.size / 2);
    final SitingPass siting = new SitingPass() {
      protected float rateSite(Tile centre) {
        float rating = world.terrain().fertilitySample(centre);
        rating -= Spacing.distance(centre, target) / World.SECTOR_SIZE;
        return rating;
      }
      protected boolean createSite(Tile centre) {
        Placement.establishVenue(
          bastion, centre.x, centre.y, true, world, AA
        );
        return bastion.inWorld();
      }
    };
    siting.applyPassTo(world, 1);

    if (! bastion.inWorld()) I.complain("NO LANDING SITE FOUND!");
    bastion.clearSurrounds();
    for (Actor a : advisors) {
      a.mind.setHome(bastion);
    }
    for (Actor a : colonists) {
      a.assignBase(base);
      a.enterWorldAt(bastion, world);
      a.goAboard(bastion, world);
    }
    
    //Placement.establishRelations(advisors, colonists);
    return bastion;
  }
  
  
  protected void establishLocals(World world) {
    if (config.siteLevel == SITE_WASTELAND) {
      final int maxRuins = world.size / (World.SECTOR_SIZE * 2);
      Ruins.placeRuins(world, maxRuins);
    }
    if (config.siteLevel == SITE_SETTLED) {
      Nest.placeNests(world, Species.QUD, Species.HAREEN);
    }
    if (config.siteLevel == SITE_WILDERNESS) {
      final int maxRuins = world.size / (World.SECTOR_SIZE * 4);
      Ruins.placeRuins(world, maxRuins);
      Nest.placeNests(world, Species.QUD, Species.HAREEN, Species.LICTOVORE);
    }
  }
}





