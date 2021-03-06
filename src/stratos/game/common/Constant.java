/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;



public abstract class Constant extends Index.Entry implements
  Text.Clickable, Session.Saveable, Accountable
{
  private static boolean verbose = false;
  
  final static Table <String, Object> allConstants = new Table();
  
  final public String name;
  
  
  protected Constant(Index index, String key, String name) {
    super(index, key);
    this.name = name;
    final String asString = toString();
    if (validAsString(asString)) allConstants.put(asString, this);
  }
  
  
  protected void setAsUniqueTo(Class belongs) {
    allConstants.put(OPEN_CHAR+belongs.getName()+SHUT_CHAR, this);
  }
  
  
  
  /**  Quick and dirty methods for creating cheap static constants-
    */
  final static Index <Storage> STORE_INDEX = new Index();
  
  public static class Storage extends Index.Entry implements Session.Saveable {
    
    final Object constant;
    
    Storage(String ID, Object constant) {
      super(STORE_INDEX, ID);
      this.constant = constant;
    }
    
    public static Storage loadConstant(Session s) throws Exception {
      return STORE_INDEX.loadEntry(s.input());
    }
    
    public void saveState(Session s) throws Exception {
      STORE_INDEX.saveEntry(this, s.output());
    }
    
    public Object stored() {
      return constant;
    }
    
    public String toString() {
      return constant.toString();
    }
  }
  
  
  public static Storage[] storeConstants(
    Class baseClass, String prefixID, Object... constants
  ) {
    final Storage stored[] = new Storage[constants.length];
    for (int i = 0; i < constants.length; i++) {
      constants[i] = new Storage(prefixID+"_"+i, constants[i]);
    }
    return stored;
  }

  
  
  /**  Accountable interface-
    */
  public Base base() {
    return null;
  }
  
  
  
  /**  Hyperlink support methods-
    */
  final static String OPEN_CAP  = "{", SHUT_CAP  = "}";
  final static char   OPEN_CHAR = '{', SHUT_CHAR = '}';
  
  
  public void describeHelp(Description d, Selectable prior) {
    d.append(name);
  }
  
  
  private boolean validAsString(String asString) {
    if (asString == null) I.complain(
      "\nA HELP-ITEM'S toString() VALUE MUST NOT BE NULL: "+name
    );
    //
    //  This is needed for reference-substitution within help-systems, so we
    //  just ensure that subclasses don't override toString() with something
    //  illegible-
    else if (asString.startsWith(OPEN_CAP) && asString.endsWith(SHUT_CAP)) {
      return true;
    }
    else I.complain(
      "\nA HELP-ITEM'S toString() VALUE MUST BE ENCLOSED BY "+
      "'"+OPEN_CAP+"' AND '"+SHUT_CAP+"', VALUE WAS: "+asString
    );
    return false;
  }
  
  
  public String toString() {
    return OPEN_CAP+name+SHUT_CAP;
  }
  
  
  public String fullName() {
    return name;
  }
  
  
  public void whenClicked(Object context) {
    //
    //  By default, we simply create a fresh selection pane and allow
    //  subclasses to fill in a more detailed description.
    final HUD UI = PlayLoop.currentUI();
    if (UI == null) return;
    //
    //  (We also include a reference to the subject of the last opened pane (if
    //  any), which can be contextually useful.)
    final Selectable prior = Selection.selectionFromContext(context);
    final SelectionPane help = new SelectionPane(UI) {
      protected void updateText(
        Text headerText, Text detailText, Text listingText
      ) {
        super.updateText(headerText, detailText, listingText);
        headerText.setText(fullName());
        describeHelp(detailText, prior);
      }
    };
    Selection.pushSelectionPane(help, context);
  }
  
  
  protected void substituteReferences(String helpString, Description d) {
    final boolean report = verbose && I.used60Frames;
    StringBuffer scanned = null;
    boolean inItem = false;
    //
    //  We also look for occurrences of class names, but simplify this through
    //  a little pre-substitution-
    helpString = helpString.replaceAll("class ", OPEN_CAP);
    final char chars[] = helpString.toCharArray();
    //
    //  In essence, we search for any terms 'capped' between opening/closing
    //  brackets, and try to substitute links to their matching Constant.
    for (int i = 0; i < chars.length;) {
      if (scanned == null) scanned = new StringBuffer();
      final char c = chars[i];
      final boolean ends = ++i == chars.length;
      
      if (c == OPEN_CHAR && ! inItem) {
        d.append(scanned);
        scanned = null;
        inItem = true;
      }
      else if ((c == SHUT_CHAR || c == ' ' || c == '.') && inItem) {
        final String key = OPEN_CHAR+scanned.toString()+SHUT_CHAR;
        if (report) I.say("\n  Found key: "+key);
        
        final Object item = allConstants.get(key);
        if (item == null) {
          if (report) I.say("  No matches found!");
          scanned.append(c);
          continue;
        }
        
        d.append(item);
        if (c != SHUT_CHAR) d.append(""+c);
        scanned = null;
        inItem = false;
      }
      else {
        scanned.append(c);
        if (ends) d.append(scanned);
      }
    }
  }
}










