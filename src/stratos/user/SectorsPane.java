/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.I;
import stratos.graphics.charts.*;



//
//  TODO:  Make SelectionOptions available for Sectors you select.
//
//  TODO:  Then you can start declaring offworld missions.  (The factory-
//         methods for each class just have to recognise sectors as viable
//         targets- and you'll need to script basic methods for evaluating the
//         outcome of each.)

//  TODO:  Finally, JoinMission will have to allow applicants to (A) exit off-
//         map, or (B), board a ship in order to conduct remote missions.  And
//         FactionAI will, conversely, need to implement generateApplicants
//         before sending missions of their own from remote sectors.


public class SectorsPane extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    PLANET_ICON     = ImageAsset.fromImage(
      SectorsPane.class, "media/GUI/Panels/planet_tab.png"
    ),
    PLANET_ICON_LIT = Button.CROSSHAIRS_LIT;
  
  final static String IMG_DIR = ChartUtils.LOAD_PATH;
  final static ImageAsset
    LEFT_BUTTON_IMG = ImageAsset.fromImage(
      SectorsPane.class , IMG_DIR+"button_left.png"
    ),
    RIGHT_BUTTON_IMG = ImageAsset.fromImage(
      SectorsPane.class, IMG_DIR+"button_right.png"
    ),
    BACKING_TEX = ImageAsset.fromImage(
      StarsPane.class, IMG_DIR+"stars_backing.png"
    ),
    BORDER_TEX = ImageAsset.fromImage(
      SectorsPane.class, IMG_DIR+"planet_frame.png"
    );
  
  
  /**  Interface presented-
    */
  public static Button createButton(final BaseUI baseUI) {
    final SectorsPane sectorsPane = baseUI.sectorsPane();
    
    return new SelectionPane.PaneButton(
      sectorsPane, baseUI,
      SECTORS_BUTTON_ID, PLANET_ICON, PLANET_ICON_LIT, "Sectors"
    ) {
      protected void whenClicked() {
        if (! baseUI.sectorsShown()) {
          baseUI.showSectorsPane();
          sectorsPane.onShow();
        }
        else {
          baseUI.hideSectorsPane();
          sectorsPane.onHide();
        }
      }
    };
  }
  
  
  
  /**  Data fields and construction-
    */
  final PlanetDisplay display;
  final Image backdrop, border;
  final UIGroup displayArea;
  final Button left, right;
  
  private Selectable before;
  private Sector focus;
  
  
  public SectorsPane(HUD UI) {
    super(UI);
    setWidgetID(SECTORS_PANE_ID);
    
    this.alignHorizontal(0.5f, CHARTS_WIDE + CHART_INFO_WIDE, 0);
    this.alignVertical  (0.5f, CHARTS_WIDE                  , 0);
    
    display = ChartUtils.createPlanetDisplay(
      ChartUtils.LOAD_PATH, ChartUtils.PLANET_LOAD_FILE
    );
    
    final UIGroup leftSide = new UIGroup(UI);
    leftSide.alignLeft    (0   , CHARTS_WIDE   );
    leftSide.alignVertical(0.5f, CHARTS_WIDE, 0);
    leftSide.stretch = false;
    leftSide.attachTo(this);
    
    backdrop = new Image(UI, BACKING_TEX);
    backdrop.alignHorizontal(20, 20);
    backdrop.alignVertical  (20, 20);
    backdrop.blocksSelect = true;
    backdrop.attachTo(leftSide);
    
    displayArea = new UIGroup(UI) {
      public void render(WidgetsPass pass) {
        ChartUtils.renderPlanet(display, this, pass);
        super.render(pass);
      }
    };
    displayArea.alignHorizontal(25, 25);
    displayArea.alignVertical  (25, 25);
    displayArea.stretch = false;
    displayArea.attachTo(leftSide);
    
    border = new Image(UI, BORDER_TEX);
    border.alignHorizontal(20, 20);
    border.alignVertical  (20, 20);
    border.attachTo(leftSide);
    
    left = new Button(
      UI, null,
      LEFT_BUTTON_IMG  .asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate left"
    ) {
      protected void whenPressed() { incRotation( 15, true); }
    };
    left.alignLeft  (0, 55);
    left.alignBottom(0, 55);
    left.attachTo(leftSide);
    
    right = new Button(
      UI, null,
      RIGHT_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate right"
    ) {
      protected void whenPressed() { incRotation(-15, true); }
    };
    right.alignLeft  (55, 55);
    right.alignBottom(0 , 55);
    right.attachTo(leftSide);
  }
  
  
  
  /**  Navigation and feedback-
    */
  //  TODO:  Control elevation as well.  Include a zoom function!
  private void incRotation(float degrees, boolean inFrame) {
    display.spinAtRate(degrees * 2, 0);
  }
  
  
  protected void onShow() {
    before = Selection.currentSelection();
    
    final Sector zooms;
    if (BaseUI.current() == null) zooms = Verse.DEFAULT_START_LOCATION;
    else zooms = BaseUI.currentPlayed().world.offworld.stageLocation();
    
    if (zooms != null) zooms.whenClicked(null);
    else Selection.pushSelection(null, null);
  }
  
  
  protected void onHide() {
    final BaseUI UI = BaseUI.current();
    
    if (before != null) {
      Selection.pushSelection(before, null);
    }
    else if (UI != null) {
      UI.clearInfoPane();
      UI.clearOptionsList();
    }
  }
  
  
  
  /**  Main rendering methods-
    */
  protected void updateState() {
    
    final DisplaySector DS = display.selectedAt(UI.mousePos());
    final Sector hovered = DS == null ? null :
      Sector.sectorNamed(DS.label)
    ;
    if (UI.mouseClicked()) {
      focus = hovered;
      if (focus != null) focus.whenClicked(null);
      else Selection.pushSelection(null, null);
    }
    
    super.updateState();
  }
}








