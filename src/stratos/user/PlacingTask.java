/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.wild.Habitat;
import stratos.graphics.common.*;
import stratos.util.*;
import stratos.graphics.widgets.KeyInput;
import com.badlogic.gdx.Input.Keys;



//  TODO:  This needs to make direct use of, or merge with, the utility classes
//         in PlaceUtils (and/or a Siting class.)

public class PlacingTask implements UITask {
  
  private static boolean
    verbose = false;
  
  final static int
    MODE_POINT = 0,
    MODE_LINE  = 1,
    MODE_AREA  = 2;
  
  final BaseUI UI;
  final Blueprint placeType;
  final int mode;
  
  private Tile begins;
  private Tile endsAt;
  private boolean dragDone = false;
  private boolean placeOK  = false;
  private Table <Integer, Venue> placeItems = new Table <Integer, Venue> ();
  
  
  PlacingTask(BaseUI UI, Blueprint placeType) {
    this.UI = UI;
    this.placeType = placeType;
    
    if      (placeType.hasProperty(Structure.IS_ZONED )) mode = MODE_AREA;
    else if (placeType.hasProperty(Structure.IS_LINEAR)) mode = MODE_LINE;
    else mode = MODE_POINT;
  }
  
  
  public void doTask() {
    final boolean report = I.used60Frames && verbose;
    
    Tile picked = UI.selection.pickedTile();
    if (report) I.say("\nTile picked: "+picked);
    
    if (picked != null) {
      final int unit = Stage.UNIT_GRID_SIZE;
      int sub = Nums.round(placeType.size / 2, unit, false);
      if (mode != MODE_POINT) sub = 0;
      picked = picked.world.tileAt(
        Nums.round(picked.x - sub, unit, false),
        Nums.round(picked.y - sub, unit, false)
      );
    }
    if (report) I.say("  After clamp: "+picked);
    
    boolean tryPlacement = false;
    if (picked != null) {
      if (mode == MODE_POINT) {
        begins = endsAt = picked;
        if (UI.mouseClicked()) tryPlacement = true;
      }
      else if (UI.mouseDown()) {
        if (begins == null) begins = picked;
        endsAt = picked;
        dragDone = true;
      }
      else {
        if (dragDone) tryPlacement = true;
        else begins = endsAt = picked;
      }
    }
    if (KeyInput.wasTyped(Keys.ENTER)) {
      tryPlacement = true;
    }
    if (report) {
      I.say("  Start tile: "+begins  );
      I.say("  End tile:   "+endsAt  );
      I.say("  Drag done?  "+dragDone);
    }
    
    if (begins != null && endsAt != null) setupAreaClaim(tryPlacement);
    if (tryPlacement && ! placeOK) cancelTask();
  }
  
  
  private void setupAreaClaim(boolean tryPlacement) {
    final boolean report = I.used60Frames && verbose;
    //
    //  Set up some initial variables-
    final int baseSize = placeType.size;
    Box2D area = null;
    final Batch <Coord> placePoints = new Batch <Coord> ();
    if (report) {
      I.say("\nSetting up area claim...");
      I.say("  Start/end points: "+begins+"/"+endsAt);
      I.say("  Venue size: "+baseSize);
      I.say("  Place mode: "+mode    );
    }
    //
    //  If there's only one point to consider, just add that.
    if (mode == MODE_POINT) {
      placePoints.add(new Coord(begins.x, begins.y));
    }
    //
    //  In the case of line-placement, we create a sequence of place-points
    //  along either the X or Y axis (whichever is stretched furthest.)
    else if (mode == MODE_LINE) {
      int difX = endsAt.x - begins.x, difY = endsAt.y - begins.y;
      boolean lateral = Nums.abs(difX) > Nums.abs(difY);
      int sign  = (lateral ? difX : difY) > 0 ? 1 : -1;
      int limit = Nums.abs(lateral ? difX : difY);
      int x = begins.x, y = begins.y;
      
      for (int i = 0; i <= limit; i += baseSize) {
        placePoints.add(new Coord(x, y));
        if (lateral) x += baseSize * sign;
        else         y += baseSize * sign;
      }
    }
    //
    //  In the case of an area-placement, just grab a rectangle from one corner
    //  to another, and place the venue at the origin.
    else if (mode == MODE_AREA) {
      final int US = Stage.UNIT_GRID_SIZE;
      area = new Box2D(begins.x - 0.5f, begins.y - 0.5f, US, US);
      area.include(endsAt.x      - 0.5f, endsAt.y      - 0.5f, 0);
      area.include(endsAt.x + US - 0.5f, endsAt.y + US - 0.5f, 0);
      placePoints.add(new Coord(
        (int) (area.xpos() + 0.6f),
        (int) (area.ypos() + 0.6f)
      ));
    }
    //
    //  If an area hasn't been specified already, construct one to envelope
    //  all the place-points generated.
    if (area == null && begins != endsAt) for (Coord c : placePoints) {
      final Box2D foot = new Box2D(c.x, c.y, baseSize, baseSize);
      if (area == null) area = new Box2D(foot);
      else area.include(foot);
    }
    if (report) {
      I.say("  Final area:  "+area);
      I.say("  Coordinates: "+placePoints);
    }
    //
    //  Check to see if placement is possible, render the preview, and if
    //  confirmed, initiate construction.
    boolean canPlace = checkPlacingOkay(area, placePoints);
    renderPlacement(area, placePoints, canPlace);
    if (tryPlacement && canPlace) {
      performPlacement(area, placePoints);
      placeOK = true;
    }
  }
  
  
  private Venue placingAt(Coord c, Box2D area, Batch <Coord> placePoints) {
    final Base base = UI.played();
    final Coord points[] = placePoints.toArray(Coord.class);
    final int index = Visit.indexOf(c, points);
    
    Venue p = placeItems.get(index);
    if (p == null) {
      p = placeType.createVenue(base);
      placeItems.put(index, p);
    }
    
    p.setupWith(base.world.tileAt(c.x - 0.5f, c.y - 0.5f), area, points);
    return p;
  }
  
  
  private boolean checkPlacingOkay(Box2D area, Batch <Coord> placePoints) {
    final Account reasons = new Account();    
    boolean canPlace = true;
    
    for (Coord c : placePoints) {
      final Venue p = placingAt(c, area, placePoints);
      if (p == null) { canPlace = false; break; }
      if (KeyInput.wasTyped('e')) {
        for (int n = Venue.ALL_FACES.length, face = p.facing(); n-- > 0;) {
          p.setFacing(++face);
          if (SiteUtils.isViableEntrance(p, p.mainEntrance())) break;
        }
      }
      if (! p.canPlace(reasons)) { canPlace = false; break; }
    }
    
    final String
      POINT_MESSAGE = "(Enter to place, Esc to cancel, E to change entrance)",
      LINE_MESSAGE  = "(Drag to place line, Esc to cancel, Enter to place)"  ,
      AREA_MESSAGE  = "(Drag to select area, Esc to cancel, Enter to place)" ,
      FAIL_MESSAGE  = "(ILLEGAL PLACEMENT- REASON NOT LOGGED- LIKELY A BUG)" ;
    String message = null;
    switch (mode) {
      case MODE_POINT : message = POINT_MESSAGE; break;
      case MODE_LINE  : message = LINE_MESSAGE ; break;
      case MODE_AREA  : message = AREA_MESSAGE ; break;
    }
    
    final String failMessage = reasons.failReasons().first();
    if (! canPlace) message = failMessage == null ? FAIL_MESSAGE : failMessage;
    BaseUI.setPopupMessage(message);
    return canPlace;
  }
  
  
  private void performPlacement(Box2D area, Batch <Coord> placePoints) {
    final Batch <Venue> placed = new Batch <Venue> ();
    
    if (I.logEvents()) {
      I.say("\nPLACING "+placeType.name+" IN AREA: "+area);
      I.say("  Placement points are:");
      for (Coord c : placePoints) I.say("    "+c);
    }
    
    for (Coord c : placePoints) {
      final Venue p = placingAt(c, area, placePoints);
      p.doPlacement(false);
      placed.add(p);
      if (I.logEvents()) I.say("  Facing: "+p.facing());
    }
    UI.endCurrentTask();
    
    if (placeType.isFixture() || placeType.isPublic()) {
      final PlacingTask next = new PlacingTask(UI, placeType);
      UI.beginTask(next);
    }
  }
  
  
  public void cancelTask() {
    if (UI.currentTask() == this) UI.endCurrentTask();
  }
  
  
  
  /**  Rendering/preview and debug methods-
    */
  private void renderPlacement(
    Box2D area, Batch <Coord> placePoints, boolean canPlace
  ) {
    if (SiteUtils.showPockets) return;
    for (Coord c : placePoints) {
      final Venue p = placingAt(c, area, placePoints);
      if (p != null && p.origin() != null) {
        p.previewPlacement(canPlace, UI.rendering);
      }
    }
  }
  
  
  public ImageAsset cursorImage() {
    return null;
  }
  
  
  
  /**  Various public utility methods-
    */
  public String toString() {
    return "Placing "+placeType.name;
  }
  
  
  public static boolean isBeingPlaced(Target e) {
    final PlacingTask task = currentPlacement();
    if (task != null) for (Venue v : task.placeItems.values()) {
      if (e == v) return true;
    }
    return false;
  }
  
  
  public static boolean isBeingPlaced(Blueprint b) {
    final PlacingTask task = currentPlacement();
    if (task != null) for (Venue v : task.placeItems.values()) {
      if (v.blueprint == b) return true;
    }
    return false;
  }
  
  
  public static Blueprint currentPlaceType() {
    final PlacingTask task = currentPlacement();
    return task == null ? null : task.placeType;
  }
  
  
  private static PlacingTask currentPlacement() {
    final BaseUI UI = BaseUI.current();
    if (UI == null || ! (UI.currentTask() instanceof PlacingTask)) return null;
    return (PlacingTask) UI.currentTask();
  }
  
  
  public static void performPlacingTask(Blueprint type) {
    final BaseUI UI = BaseUI.current();
    if (UI != null) UI.beginTask(new PlacingTask(UI, type));
  }
  
  
  public static void performPlacements(
    Series <? extends Venue> placed, Box2D area, Base base
  ) {
    //  TODO:  UNIFY WITH THE METHODS ABOVE FOR HIGHER FIDELITY/CONCISION
    final Batch <Coord> coords = new Batch();
    
    for (Venue v : placed) {
      if (area == null) area = new Box2D().setTo(v.footprint());
      else area.include(v.footprint());
      final Tile at = v.origin();
      coords.add(new Coord(at.x, at.y));
    }
    
    for (Venue s : placed) {
      s.setupWith(s.origin(), area, coords.toArray(Coord.class));
      s.doPlacement(false);
    }
  }
}















