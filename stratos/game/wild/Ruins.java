

package stratos.game.wild ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;




public class Ruins extends Venue {
  
  
  
  /**  Construction and save/load methods-
    */
  final static ModelAsset MODEL_RUINS[] = CutoutModel.fromImages(
    "media/Buildings/lairs and ruins/", Ruins.class, 4, 2, false,
    "ruins_a.png",
    "ruins_b.png",
    "ruins_c.png"
  ) ;
  private static int NI = (int) (Math.random() * 3) ;
  
  
  public Ruins() {
    super(4, 2, ENTRANCE_EAST, null) ;
    structure.setupStats(1000, 100, 0, 0, Structure.TYPE_ANCIENT) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    final int index = (NI++ + Rand.index(1)) % 3 ;
    attachSprite(MODEL_RUINS[index].makeSprite()) ;
  }
  
  
  public Ruins(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behavioural routines-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  
  
  
  /**  Siting and placement-
    */
  public static void placeRuins(
    final World world, final int maxPlaced
  ) {
    final Presences presences = world.presences;
    final float minSpacing = World.SECTOR_SIZE;
    
    final SitingPass siting = new SitingPass() {
      int numSited = 0;
      
      protected float rateSite(Tile centre) {
        if (presences.nearestMatch(Ruins.class, centre, minSpacing) != null) {
          return -1;
        }
        float rating = 2;
        rating -= world.terrain().fertilitySample(centre);
        rating += world.terrain().habitatSample(centre, Habitat.CURSED_EARTH);
        return rating;
      }
      
      protected boolean createSite(Tile centre) {
        final boolean minor = numSited >= maxPlaced / 2;
        int maxRuins = (minor ? 3 : 1) + Rand.index(3) ;
        final Batch <Venue> ruins = new Batch <Venue> () ;
        while (maxRuins-- > 0) {
          final Ruins r = new Ruins() ;
          Placement.establishVenue(r, centre.x, centre.y, true, world) ;
          if (r.inWorld()) ruins.add(r) ;
        }
        
        //  TODO:  Slag/wreckage must be done in a distinct pass...
        for (Venue r : ruins) for (Tile t : world.tilesIn(r.area(), true)) {
          Habitat h = Rand.yes() ? Habitat.CURSED_EARTH : Habitat.DUNE ;
          world.terrain().setHabitat(t, h) ;
        }
        populateArtilects(world, ruins, minor) ;
        numSited++;
        return ruins.size() > 0;
      }
    };
    siting.applyPassTo(world, maxPlaced);
  }
  
  
  static void populateArtilects(
    World world, Batch <Venue> ruins, boolean minor
  ) {
    final Base artilects = world.baseWithName(Base.KEY_ARTILECTS, true, true) ;
    //
    //  TODO:  Generalise this, too?  Using pre-initialised actors?
    int lairNum = 0 ; for (Venue r : ruins) {
      r.assignBase(artilects) ;
      if (lairNum++ > 0 && Rand.yes()) continue ;
      
      final Tile e = r.mainEntrance() ;
      int numT = Rand.index(3) == 0 ? 1 : 0, numD = 1 + Rand.index(2) ;
      if (minor && Rand.yes()) { numT = 0 ; numD-- ; }
      
      while (numT-- > 0) {
        final Tripod tripod = new Tripod() ;
        tripod.assignBase(artilects) ;
        tripod.enterWorldAt(e.x, e.y, world) ;
        tripod.mind.setHome(r) ;
      }
      
      while (numD-- > 0) {
        final Drone drone = new Drone() ;
        drone.assignBase(artilects) ;
        drone.enterWorldAt(e.x, e.y, world) ;
        drone.mind.setHome(r) ;
      }
      
      if (lairNum == 1 && Rand.yes() && ! minor) {
        final Cranial cranial = new Cranial() ;
        cranial.assignBase(artilects) ;
        cranial.enterWorldAt(e.x, e.y, e.world) ;
        cranial.mind.setHome(r) ;
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Ancient Ruins" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Ancient ruins cover the landscape of many worlds in regions irradiated "+
      "by nuclear fire or blighted by biological warfare.  Clans of ravenous "+
      "infected, deformed mutant primitives or even decaying artilect "+
      "sentinels may haunt such forsaken places." ;
  }
  
  
  public String buildCategory() { return UIConstants.TYPE_HIDDEN ; }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    return VenueDescription.configSimplePanel(this, panel, UI, null);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}







