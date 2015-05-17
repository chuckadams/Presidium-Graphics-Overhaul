/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  Adapt to work in an abbreviated horizontal format?  (More of a long-
//         term project.)

//  TODO:  Create proper, separately-instanced sub-panes for the different
//         categories...


public class SelectionPane extends UIGroup implements UIConstants {
  
  
  /**  Constants, fields and setup methods-
    */
  final public static ImageAsset
    BORDER_TEX = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Panel.png"
    ),
    SCROLL_TEX = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/scroll_grab.gif"
    );
  final public static int
    MARGIN_SIZE    = 10 ,
    HEADER_HIGH    = 35 ,
    CORE_INFO_HIGH = 160,
    PORTRAIT_SIZE  = 80 ;
  
  
  final protected BaseUI UI;
  final protected Selectable selected;
  
  final Bordering border;
  final UIGroup
    innerRegion;
  final Text
    headerText ,
    detailText ,
    listingText;
  final Scrollbar scrollbar;

  final   UINode    portraitFrame;
  private Composite portrait     ;
  
  final   String categories[];
  final   float  catScrolls[];
  private int    categoryID  ;
  

  public SelectionPane(
    final BaseUI UI, Selectable selected,
    final Composite portrait,
    boolean hasListing, String... categories
  ) {
    this(UI, selected, portrait != null, hasListing, 0, categories);
    this.portrait = portrait;
  }
  
  
  public SelectionPane(
    final BaseUI UI, Selectable selected,
    boolean hasPortrait, boolean hasListing, int topPadding,
    String... categories
  ) {
    super(UI);
    this.UI = UI;
    this.alignVertical(0, PANEL_TABS_HIGH);
    this.alignRight   (0, INFO_PANEL_WIDE);

    final int
      TP = topPadding,
      TM = 40, BM = 40,  //top and bottom margins
      LM = 40, RM = 40;  //left and right margins
    int down = hasPortrait ? (PORTRAIT_SIZE + MARGIN_SIZE) : 0;
    down += HEADER_HIGH + TP;
    
    this.border = new Bordering(UI, BORDER_TEX);
    border.left   = LM;
    border.right  = RM;
    border.bottom = BM;
    border.top    = TM;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    this.innerRegion = new UIGroup(UI);
    innerRegion.alignHorizontal(-15, -15);
    innerRegion.alignVertical  (-15, -15);
    innerRegion.attachTo(border.inside);
    
    headerText = new Text(UI, BaseUI.INFO_FONT);
    headerText.alignTop   (TP, HEADER_HIGH);
    headerText.alignAcross(0 , 1          );
    headerText.scale = BIG_FONT_SIZE;
    headerText.attachTo(innerRegion);
    
    
    if (hasPortrait) {
      portraitFrame = new UINode(UI) {
        protected void render(WidgetsPass batch2d) {
          if (portrait == null) return;
          portrait.drawTo(batch2d, bounds, absAlpha);
        }
      };
      portraitFrame.alignTop (HEADER_HIGH + TP, PORTRAIT_SIZE);
      portraitFrame.alignLeft(0               , PORTRAIT_SIZE);
      portraitFrame.attachTo(innerRegion);
    }
    else {
      this.portrait      = null;
      this.portraitFrame = null;
    }
    
    
    detailText = new Text(UI, BaseUI.INFO_FONT) {
      protected void whenLinkClicked(Clickable link) {
        super.whenLinkClicked(link);
        ((BaseUI) UI).beginPanelFade();
      }
    };
    detailText.scale = SMALL_FONT_SIZE;
    detailText.attachTo(innerRegion);
    
    if (hasListing) {
      detailText.alignHorizontal(0, 0);
      detailText.alignTop(down, CORE_INFO_HIGH);
      
      listingText = new Text(UI, BaseUI.INFO_FONT) {
        protected void whenLinkClicked(Clickable link) {
          super.whenLinkClicked(link);
          ((BaseUI) UI).beginPanelFade();
        }
      };
      listingText.alignVertical  (0, CORE_INFO_HIGH + down);
      listingText.alignHorizontal(0, 0                    );
      listingText.scale = SMALL_FONT_SIZE;
      listingText.attachTo(innerRegion);
      scrollbar = listingText.makeScrollBar(SCROLL_TEX);
      scrollbar.alignToMatch(listingText);
    }
    else {
      listingText = null;
      detailText.alignHorizontal(0, 0   );
      detailText.alignVertical  (0, down);
      scrollbar = detailText .makeScrollBar(SCROLL_TEX);
      scrollbar.alignToMatch(detailText);
    }
    
    scrollbar.alignRight(0 - SCROLLBAR_WIDE, SCROLLBAR_WIDE);
    scrollbar.attachTo(innerRegion);
    
    this.selected = selected;
    this.categories = categories;
    categoryID = defaultCategory();
    this.catScrolls = new float[categories == null ? 0 :categories.length];
  }
  
  
  public void assignPortrait(Composite portrait) {
    this.portrait = portrait;
  }
  
  
  protected Vec2D screenTrackPosition() {
    Vec2D middle = UI.trueBounds().centre();
    middle.x -= INFO_PANEL_WIDE / 2;
    //middle.y -= INFO_PANEL_HIGH / 2;
    return middle;
  }
  
  
  public Text header() {
    return headerText;
  }
  
  
  public Text detail() {
    return detailText;
  }
  
  
  public Text listing() {
    return listingText;
  }
  
  
  
  /**  Handling category-selection:
    */
  final static Class SELECT_TYPES[] = {
    Venue.class,
    Actor.class,
    Vehicle.class,
    Object.class
  };
  private static Table <Class, String> defaults = new Table();
  
  
  private int defaultCategory() {
    if (selected == null) return 0;
    final String match = defaults.get(selectType());
    if (match == null) return 0;
    for (String s : categories) if (s.equals(match)) {
      return Visit.indexOf(s, categories);
    }
    return 0;
  }
  
  
  private Class selectType() {
    if (selected == null) return null;
    for (Class c : SELECT_TYPES) {
      if (selected.getClass().isAssignableFrom(c)) return c;
    }
    return null;
  }
  
  
  public int categoryID() {
    return categoryID;
  }
  
  
  public String category() {
    if (categories.length == 0) return null;
    return categories[Nums.clamp(categoryID, categories.length)];
  }
  

  private void setCategory(int catID) {
    UI.beginPanelFade();
    
    catScrolls[categoryID] = 1 - scrollbar.scrollPos();
    this.categoryID = catID;
    scrollbar.setScrollPos(1 - catScrolls[categoryID]);
    
    if (selected != null) defaults.put(selectType(), categories[catID]);
  }
  
  
  
  /**  Display and updates-
    */
  protected void updateState() {
    updateText(UI, headerText, detailText, listingText);
    if (selected != null) selected.configPanel(this, UI);
    super.updateState();
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    if (selected != null) {
      headerText.setText(selected.fullName());
      headerText.append("\n");
    }
    else headerText.setText("");
    
    if (categories != null) {
      for (int i = 0; i < categories.length; i++) {
        final int index = i;
        final boolean CC = categoryID == i;
        headerText.append(new Text.Clickable() {
          public String fullName() { return ""+categories[index]+" "; }
          public void whenClicked() { setCategory(index); }
        }, CC ? Colour.GREEN : Text.LINK_COLOUR);
      }
    }
    
    detailText.setText("");
    if (listingText != null) listingText.setText("");
  }
}





