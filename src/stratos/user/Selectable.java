/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public interface Selectable extends Text.Clickable, Session.Saveable {
  
  String fullName();
  Composite portrait(BaseUI UI);
  String helpInfo();
  String objectCategory();
  Constant infoSubject();
  
  SelectionPane    configSelectPane   (SelectionPane    pane, BaseUI UI);
  SelectionOptions configSelectOptions(SelectionOptions info, BaseUI UI);
  
  Target selectionLocksOn();
  void renderSelection(Rendering rendering, boolean hovered);
}