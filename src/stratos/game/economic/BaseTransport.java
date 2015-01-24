/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;


//  TODO:  Also check if these routes are fully-paved (for distribution
//  purposes.)

public class BaseTransport {
  
  
  /**  Field definitions, constructor and save/load methods-
    */
  final static int PATH_RANGE = Stage.SECTOR_SIZE / 2;
  private static boolean
    paveVerbose      = false,
    distroVerbose    = false,
    checkConsistency = false;
  
  final Stage world;
  final public PavingMap map;
  protected PresenceMap junctions;
  
  Table <Tile, List <Route>> tileRoutes = new Table(1000);
  Table <Route, Route> allRoutes = new Table <Route, Route> (1000);
  
  
  
  public BaseTransport(Stage world) {
    this.world = world;
    this.map = new PavingMap(world, this);
    junctions = new PresenceMap(world, "junctions");
  }
  
  
  public void loadState(Session s) throws Exception {
    junctions = (PresenceMap) s.loadObject();
    map.loadState(s);
    
    int numR = s.loadInt();
    for (int n = numR; n-- > 0;) {
      final Route r = Route.loadRoute(s);
      allRoutes.put(r, r);
      toggleRoute(r, r.start, true);
      toggleRoute(r, r.end  , true);
    }
    
    provSupply = s.loadFloatArray(null);
    provDemand = s.loadFloatArray(null);
    allSupply  = s.loadFloatArray(null);
    allDemand  = s.loadFloatArray(null);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(junctions);
    map.saveState(s);
    
    s.saveInt(allRoutes.size());
    for (Route r : allRoutes.keySet()) {
      Route.saveRoute(r, s);
    }
    
    s.saveFloatArray(provSupply);
    s.saveFloatArray(provDemand);
    s.saveFloatArray(allSupply );
    s.saveFloatArray(allDemand );
  }
  
  
  public void checkConsistency() {
    //
    //  Note:  This method only works when you only have a single base in the
    //         world...
    if (! checkConsistency) return;
    
    //I.say("CHECKING PAVING CONSISTENCY:");
    
    final byte mask[][] = new byte[world.size][world.size];
    boolean okay = true;
    
    for (Route route : allRoutes.keySet()) {
      for (Tile t : route.path) mask[t.x][t.y]++;
      if (route.start == route.end) continue;
      //
      //  Check if non-perimeter routes are sane:
      Tile first = route.path[0], last = route.path[route.path.length - 1];
      final boolean
        noFirst = tileRoutes.get(first) == null,
        noLast  = tileRoutes.get(last ) == null;
      
      if (noFirst || noLast) {
        if (noFirst) I.say("NO FIRST JUNCTION");
        if (noLast ) I.say("NO LAST JUNCTION" );
        this.reportPath("  on path: ", route);
      }
    }
    
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final int pM = mask[c.x][c.y], tM = map.roadCounter(t);
      if (pM != tM) {
        I.say("Discrepancy at: "+c.x+" "+c.y+", "+pM+" =/= "+tM);
        okay = false;
      }
    }
    //if (okay) I.say("No discrepancies in paving map found.");
  }
  
  
  
  /**  Methods related to installation, updates and deletion of junctions-
    */
  private void reportPath(String title, Route path) {
    I.add(""+title+": ");
    if (path == null) I.add("No path.");
    else {
      I.add("Route length: "+path.path.length+"\n  ");
      int i = 0; for (Tile t : path.path) {
        I.add(t.x+"|"+t.y+" ");
        if (((++i % 10) == 0) && (i < path.path.length)) I.add("\n  ");
      }
    }
    I.add("\n");
  }
  
  
  public void updatePerimeter(Fixture v, boolean isMember) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (report) I.say("\nUpdating perimeter for "+v);
    
    if (! isMember) {
      updatePerimeter(v, null, false);
      return;
    }
    
    final Batch <Tile> around = new Batch <Tile> ();
    for (Tile t : Spacing.perimeter(v.footprint(), world)) {
      if (t == null || t.reserved()) continue;
      else around.add(t);
    }
    updatePerimeter(v, around, true);
  }
  

  public void updatePerimeter(
    Fixture v, Batch <Tile> around, boolean isMember
  ) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (report) I.say("Updating perimeter for "+v+", member? "+isMember);
    
    final Tile o = v.origin();
    final Route key = new Route(o, o), match = allRoutes.get(key);
    
    if (isMember) {
      key.path = around.toArray(Tile.class);
      key.cost = -1;
      if (key.routeEquals(match) && map.refreshPaving(key.path)) return;
      if (report) I.say("Installing perimeter for "+v);
      
      if (match != null) {
        map.flagForPaving(match.path, false);
        allRoutes.remove(match);
      }
      map.flagForPaving(key.path, true);
      allRoutes.put(key, key);
    }
    else if (match != null) {
      if (report) I.say("Discarding perimeter for "+v);
      //reportPath("Old route", match);
      map.flagForPaving(match.path, false);
      allRoutes.remove(key);
    }
  }
  
  
  public void updateJunction(Fixture v, Tile t, boolean isMember) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (t == null) {
      if (report) I.say("CANNOT SUPPLY NULL TILE AS JUNCTION");
      return;
    }
    
    if (isMember) {
      final Batch <Tile> routesTo = new Batch <Tile> ();
      final int HS = v.size / 2;
      final Tile c = v.origin(), centre = world.tileAt(c.x + HS, c.y + HS);
      final int range = PATH_RANGE + 1 + HS;
      
      if (report) I.say("\nUpdating road junction: "+t+", range: "+range);
      //
      //  First, we visit all registered junctions nearby, and include those
      //  for subsequent routing to-
      for (Target o : junctions.visitNear(centre, range, null)) {
        final Tile jT = (Tile) o;
        if (o == t || jT.flaggedWith() != null) continue;
        jT.flagWith(routesTo);
        routesTo.add(jT);
      }
      //
      //  We also include all nearby base venues with entrances registered as
      //  junctions.  (Any results are flagged to avoid duplicated work.)
      for (Object o : t.world.presences.matchesNear(Venue.class, v, range)) {
        final Venue n = (Venue) o;
        if (n == v || n.base() != v.base()) continue;
        final Tile jT = n.mainEntrance();
        if (jT == null || jT == t || jT.flaggedWith() != null) continue;
        if (! junctions.hasMember(jT, jT)) continue;
        jT.flagWith(routesTo);
        routesTo.add(jT);
      }
      
      updateJunction(v, t, routesTo, true);
    }
    else updateJunction(v, t, null, false);
  }
  
  
  public void updateJunction(
    Fixture v, Tile t, Batch <Tile> routesTo, boolean isMember
  ) {
    final boolean report = paveVerbose && I.talkAbout == v;
    final List <Route> oldRoutes = tileRoutes.get(t);
    final Batch <Route> toDelete = new Batch <Route> ();
    junctions.toggleMember(t, t, isMember);
    
    if (isMember) {
      //
      //  Any old routes that lack termini are assumed to be obsolete, and must
      //  be deleted-
      if (oldRoutes != null) for (Route r : oldRoutes) {
        final Tile end = r.opposite(t);
        if (! junctions.hasMember(end, end)) toDelete.add(r);
      }
      //
      //  (NOTE:  We perform the un-flag op in a separate pass to avoid any
      //  interference with pathing-searches.)  Otherwise, establish routes to
      //  all the nearby junctions compiled.
      for (Tile jT : routesTo) jT.flagWith(null);
      for (Tile jT : routesTo) {
        if (report) I.say("  Paving to: "+jT);
        routeBetween(t, jT, report);
      }
    }
    //
    //  All routes are flagged as obsolete if the fixture is no longer a
    //  map-member.  Either way, any obsolete routes are finally deleted.
    else if (oldRoutes != null) {
      if (report) I.say("\nDeleting road junction: "+t);
      Visit.appendTo(toDelete, oldRoutes);
    }
    for (Route r : toDelete) {
      if (report) I.say("  Discarding unused route: "+r);
      deleteRoute(r);
    }
  }
  
  
  private boolean routeBetween(Tile a, Tile b, boolean report) {
    if (a == b || a == null || b == null) return false;
    //
    //  Firstly, determine the correct current route.
    final Route route = new Route(a, b);
    final RoadSearch search = new RoadSearch(route.start, route.end);
      //route.start, route.end//, Element.FIXTURE_OWNS
    //);
    search.doSearch();
    route.path = search.fullPath(Tile.class);
    route.cost = search.totalCost();
    //
    //  If the new route differs from the old, delete it, and install the new
    //  version.  Otherwise return.
    final Route oldRoute = allRoutes.get(route);
    if (route.routeEquals(oldRoute) && map.refreshPaving(route.path)) {
      return false;
    }
    if (report) {
      I.say("Route between "+a+" and "+b+" has changed!");
      this.reportPath("Old route", oldRoute);
      this.reportPath("New route", route   );
    }
    //
    //  If the route needs an update, clear the tiles and store the data:
    if (oldRoute != null) deleteRoute(oldRoute);
    if (search.success()) {
      allRoutes.put(route, route);
      toggleRoute(route, route.start, true);
      toggleRoute(route, route.end  , true);
      map.flagForPaving(route.path, true);
    }
    else if (report) {
      I.say("Could not find route between "+a+" and "+b+"!");
    }
    return true;
  }
  
  
  private void deleteRoute(Route route) {
    if (route.cost < 0) return;
    map.flagForPaving(route.path, false);
    allRoutes.remove(route);
    toggleRoute(route, route.start, false);
    toggleRoute(route, route.end  , false);
    route.cost = -1;
  }
  
  
  private void toggleRoute(Route route, Tile t, boolean is) {
    List <Route> atTile = tileRoutes.get(t);
    if (atTile == null) tileRoutes.put(t, atTile = new List <Route> ());
    if (is) atTile.add(route);
    else atTile.remove(route);
    if (atTile.size() == 0) tileRoutes.remove(t);
  }
  
  
  
  /**  Methods related to distribution of provisional goods (power, water, and
    *  life support.)
    */
  final private Batch <Target> tried = new Batch <Target> (40);
  final private Stack <Target> agenda = new Stack <Target> ();
  private float
    provSupply[], provDemand[],
    allSupply [], allDemand [];
  
  
  public float allSupply(Traded t) {
    int index = Visit.indexOf(t, Economy.ALL_PROVISIONS);
    if (index == -1 || allSupply == null) return -1;
    return allSupply[index];
  }
  
  
  public float allDemand(Traded t) {
    int index = Visit.indexOf(t, Economy.ALL_PROVISIONS);
    if (index == -1 || allDemand == null) return -1;
    return allDemand[index];
  }
  
  
  private void insertAgenda(Target t) {
    if (t.flaggedWith() != null) return;
    t.flagWith(agenda);
    agenda.add(t);
    tried.add(t);
  }
  
  
  private Batch <Venue> venuesReached(Structural init, Base base) {
    if (init.flaggedWith() != null) return null;
    final boolean report = distroVerbose;
    if (report) I.say("\nDetermining provision access from "+init);
    
    final Batch <Venue> reached = new Batch <Venue> ();
    insertAgenda(init);
    final Box2D tempB = new Box2D();
    final Tile edgeB[] = new Tile[4];
    
    //  The agenda could include either tiles or structures, depending on how
    //  they are encountered.
    while (agenda.size() > 0) {
      final Target next = agenda.removeFirst();
      final List <Route> routes = tileRoutes.get(next);
      
      //  In the case of a structure, check every tile along the perimeter
      //  and add any adjacent structures or road junctions.
      if (routes == null) {
        final Venue v = (Venue) next;
        if (v.base() != base) continue;
        reached.add(v);
        if (report) I.say("  Have reached: "+v);
        
        tempB.setTo(v.footprint()).expandBy(2);
        for (Venue c : world.claims.venuesClaiming(tempB)) {
          if (c.footprint().overlaps(tempB)) insertAgenda(c);
        }
        for (Boarding t : v.canBoard()) {
          if (tileRoutes.get(t) != null) insertAgenda(t);
        }
      }
      
      //  In the case of a road junction, add whatever structures lie at the
      //  other end of the route.
      else for (Route r : routes) {
        final Tile o = r.opposite((Tile) next);
        if (o == null) continue;
        insertAgenda(o);
        for (Tile b : o.edgeAdjacent(edgeB)) if (b != null) {
          if (b.onTop() instanceof Venue) insertAgenda((Venue) b.onTop());
        }
      }
    }
    
    //  Clean up afterwards, and return-
    for (Target t : tried) t.flagWith(null);
    tried.clear();
    agenda.clear();
    for (Structural v : reached) v.flagWith(reached);
    return reached;
  }
  
  
  private void distributeTo(Batch <Venue> reached, Traded provided[]) {
    //
    //  First, tabulate total supply and demand within the area-
    final boolean report = distroVerbose;
    if (report) I.say("\nDistributing provisions through paving network-");
    
    provSupply = new float[provided.length];
    provDemand = new float[provided.length];
    
    for (Venue s : reached) {
      if (report) I.say("  Have reached: "+s);
      
      for (int i = provided.length; i-- > 0;) {
        final Traded type = provided[i];
        
        final float in = s.structure.outputOf(type);
        if (in > 0) {
          provSupply[i] += in;
          if (report) I.say("    "+type+" supply: "+in);
        }
        
        final float out = s.stocks.demandFor(type);
        if (out > 0) {
          if (report) I.say("    "+type+" demand: "+out);
          provDemand[i] += out;
        }
      }
    }
    if (report) {
      I.say("\nSupply/demand tally complete.");
      for (int i = provided.length; i-- > 0;) {
        final Traded type = provided[i];
        final float supply = provSupply[i], demand = provDemand[i];
        if (supply + demand == 0) continue;
        I.say("  Supply|Demand of "+type+": "+supply+"|"+demand);
      }
    }
    //
    //  Then top up demand in whole or in part, depending on how much supply
    //  is available-
    for (int i = provided.length; i-- > 0;) {
      allDemand[i] += provDemand[i];
      allSupply[i] += provSupply[i];
      
      if (provDemand[i] == 0) continue;
      final Traded type = provided[i];
      float supplyRatio = Nums.clamp(provSupply[i] / provDemand[i], 0, 1);
      
      for (Venue venue : reached) {
        final float d = venue.stocks.demandFor(type);
        venue.stocks.setAmount(type, d * supplyRatio);
      }
    }
  }
  
  
  public void distributeProvisions(Base base) {
    final boolean report = distroVerbose;
    if (report) I.say("\n\nDistributing provisions for base: "+base);
    final Batch <Batch <Venue>> allReached = new Batch();
    final Traded provided[] = Economy.ALL_PROVISIONS;

    allSupply = new float[provided.length];
    allDemand = new float[provided.length];
    
    //
    //  First, divide the set of all venues into discrete partitions based on
    //  mutual paving connections-
    for (Object o : world.presences.matchesNear(base, null, -1)) {
      final Batch <Venue> reached = venuesReached((Venue) o, base);
      if (reached != null) allReached.add(reached);
    }
    //
    //  Then, distribute water/power/et cetera within that area-
    for (Batch <Venue> reached : allReached) {
      distributeTo(reached, provided);
      for (Structural v : reached) v.flagWith(null);
    }
  }
}






