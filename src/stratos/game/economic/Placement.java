

package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;



//
//  TODO:  Try merging this with the TileSpread class, or the PlacementGrid
//  class.  Placement2 can probably be got rid off completely.

public class Placement implements TileConstants {
  
  
  private static boolean verbose = false, cacheVerbose = false;
  
  final private static Coord footprintCache[][][] = new Coord[100][100][];
  //
  //  NOTE:  This method is intended to generate a sequence of coordinates to
  //  check that should eliminate unfit placement-sites for buildings faster
  //  than a naive sequential iteration.  It splits the x and y axes into
  //  successively finer quadrants, closing a 'net' on any potential obstacles
  //  within the area:
  //
  //  PASS 1:           PASS 2:           PASS 3:          ETC.
  //    *-----------*     *-----*-----*     *--*--*--*--*
  //    |           |     |     |     |     *--*--*--*--*
  //    |           |     *-----*-----*     *--*--*--*--*
  //    |           |     |     |     |     *--*--*--*--*
  //    *-----------*     *-----*-----*     *--*--*--*--*
  //
  //
  private static Coord[] footprintFor(int sizeX, int sizeY) {
    //
    //  Return the cached version if available, initialise otherwise-
    Coord offsets[] = footprintCache[sizeX][sizeY];
    if (offsets != null) return offsets;
    else offsets = footprintCache[sizeX][sizeY] = new Coord[sizeX * sizeY];
    //
    //  Find the initial 'step' size for the grid.
    int stepX = 1, stepY = 1, i = 0;
    while (stepX <= sizeX * 2) stepX *= 2;
    while (stepY <= sizeY * 2) stepY *= 2;
    final int maxX = sizeX - 1, maxY = sizeY - 1;
    final boolean mask[][] = new boolean[sizeX][sizeY];
    //
    //  Shrink the grid with every step, skipping over any previously-used
    //  coordinates, and return the sequence of coordinates compiled-
    while (stepX > 1 || stepY > 1) {
      if (stepX > 1) stepX /= 2;
      if (stepY > 1) stepY /= 2;
      for (int x = 0;;) {
        for (int y = 0;;) {
          if (! mask[x][y]) {
            if (cacheVerbose) I.say("X/Y: "+x+"/"+y);
            mask[x][y] = true;
            offsets[i++] = new Coord(x, y);
          }
          if (y == maxY) break;
          y += stepY;
          if (y >= sizeY) y = maxY;
        }
        if (x == maxX) break;
        x += stepX;
        if (x >= sizeX) x = maxX;
      }
    }
    if (cacheVerbose) I.say("SIZE X/Y: "+sizeX+"/"+sizeY);
    if (cacheVerbose) I.say("OFFSETS GENERATED: "+i);
    return offsets;
  }
  
  
  public static boolean checkAreaClear(
    Tile origin, int sizeX, int sizeY//, int owningPriority
  ) {
    if (origin == null || sizeX <= 0 || sizeY <= 0) return false;
    for (Coord c : footprintFor(sizeX, sizeY)) {
      final Tile t = origin.world.tileAt(origin.x + c.x, origin.y + c.y);
      if (t == null || t.reserved()) return false;
    }
    return true;
  }
  
  
  //
  //  NOTE:  This method assumes that the fixtures in question will occupy a
  //  contiguous 'strip' or bloc for placement purposes.
  public static boolean checkPlacement(
    Structure.Basis fixtures[], Stage world
  ) {
    Box2D limits = null;
    for (Structure.Basis f : fixtures) {
      if (limits == null) limits = new Box2D(f.footprint());
      else limits.include(f.footprint());
    }
    if (! checkAreaClear(
      world.tileAt(limits.xpos() + 0.5f, limits.ypos() + 0.5f),
      (int) limits.xdim(),
      (int) limits.ydim()
    )) return false;
    
    for (Structure.Basis f : fixtures) if (! f.canPlace()) return false;
    return true;
  }
  
  
  public static boolean findClearanceFor(
    final Venue v, final Target near, final Stage world
  ) {
    final float maxDist = Stage.SECTOR_SIZE / 2;
    Tile init = world.tileAt(near);
    init = Spacing.nearestOpenTile(init, init);
    if (init == null) return false;
    
    final TileSpread search = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, near) > maxDist) return false;
        return ! t.blocked();
      }
      protected boolean canPlaceAt(Tile t) {
        v.setPosition(t.x, t.y, world);
        return checkPlacement(v.structure.asGroup(), world);
      }
    };
    search.doSearch();
    return search.success();
  }
  
  
  public static Venue establishVenue(
    final Venue v, final Target near, boolean intact, Stage world
  ) {
    if (! findClearanceFor(v, near, world)) return null;
    
    for (Structure.Basis i : v.structure.asGroup()) {
      i.doPlacement();
      if (intact || GameSettings.buildFree) {
        i.structure().setState(Structure.STATE_INTACT, 1.0f);
      }
      else {
        i.structure().setState(Structure.STATE_INSTALL, 0.0f);
      }
      ((Element) i).setAsEstablished(true);
    }
    return v;
  }
  
  
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final Stage world,
    Actor... employed
  ) {
    if (establishVenue(v, world.tileAt(atX, atY), intact, world) == null) {
      return null;
    }
    for (Actor a : employed) {
      if (! a.inWorld()) {
        a.assignBase(v.base());
        a.enterWorldAt(v, world);
        a.goAboard(v, world);
      }
      a.mind.setWork(v);
    }
    if (GameSettings.hireFree) v.base.setup.fillVacancies(v, intact);
    return v;
  }
}


