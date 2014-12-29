

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.Boarding;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.math.*;



public class MapsPanel extends UIGroup {
  

  final BaseUI UI;
  final Stage world;
  private Base base;
  
  private float lastTime = -1;
  final Minimap minimap;
  final int RGBA[][];
  
  
  public MapsPanel(BaseUI UI, Stage world, Base base) {
    super(UI);
    this.UI = UI;
    this.world = world;
    this.base = base;
    minimap = new Minimap();
    RGBA = new int[world.size][world.size];
  }
  

  public void setBase(Base base) {
    this.base = base;
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    final UINode kid = super.selectionAt(mousePos);
    if (kid != null) return kid;
    if (! bounds.contains(mousePos.x, mousePos.y)) return null;
    final Coord c = minimap.getMapPosition(UI.mousePos(), bounds, world.size);
    final Tile t = world.tileAt(c.x, c.y);
    return (t == null) ? null : this;
  }
  
  
  protected void whenClicked() {
    final Coord c = minimap.getMapPosition(UI.mousePos(), bounds, world.size);
    if (c == null) return;
    UI.tracking.lockOn(world.tileAt(c.x, c.y));
  }
  
  
  public void render(WidgetsPass batch2d) {
    //  TODO:  Calling begin/end here is a bit of a hack.  Fix?
    batch2d.end();
    
    //  TODO:  Try and have this fade in gradually...
    final float time = Rendering.activeTime();
    if (((int) lastTime) != ((int) time)) {
      final int WS = world.size;
      for (Coord c : Visit.grid(0, 0, WS, WS, 1)) {
        RGBA[c.x][c.y] = colourFor(c);
      }
      minimap.updateTexture(WS, RGBA);
      cleanupTemps();
    }
    lastTime = time;
    
    minimap.updateGeometry(bounds);
    minimap.renderWith(base.intelMap.fogOver());
    batch2d.begin();
    super.render(batch2d);
  }
  
  
  private Colour avg;
  private Tile near[];
  
  
  private void cleanupTemps() {
    avg = null;
    near = null;
  }
  
  
  private int colourFor(Coord c) {
    if (avg == null) { avg = new Colour(); near = new Tile[8]; }
    
    final Tile t = world.tileAt(c.x, c.y);
    avg.set(t.minimapTone());
    
    final Base border = baseWithBorder(t);
    if (border != null) {
      final Colour badge = border.colour;
      avg.r = (1 + badge.r) / 2;
      avg.g = (1 + badge.g) / 2;
      avg.b = (1 + badge.b) / 2;
    }
    return avg.getRGBA();
  }
  
  
  private Base baseWithBorder(Tile t) {
    if (t.blocked()) return null;
    final Base owns = world.claims.baseClaiming(t);
    if (owns == null) return null;
    for (Boarding b : t.canBoard()) {
      if (b == null || b.boardableType() != Boarding.BOARDABLE_TILE) continue;
      final Base borders = world.claims.baseClaiming((Tile) b);
      if (borders != owns) return owns;
    }
    return null;
  }
}









