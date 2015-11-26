/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.graphics.common.*;
import stratos.game.verse.*;
import stratos.user.*;
import stratos.util.*;
import stratos.util.Description.*;
import static stratos.user.ChartUtils.*;



//  TODO:  Just use a Text for now.  Don't bother with interior listings, or
//         headers and footers.


public class NewGamePane extends MenuPane {
  
  final static int
    STAGE_PICK_SECTORS   = 0,
    STAGE_CONFIG_LEADER  = 1,
    STAGE_PICK_HOUSEHOLD = 2,
    STAGE_PICK_COLONISTS = 3;
  
  private int stage = STAGE_PICK_SECTORS;
  
  
  private UINode forwardButton;
  
  private VerseLocation homeworld;
  private VerseLocation landing;
  
  private Actor leader;
  private List <Actor> advisors  = new List();
  private List <Actor> colonists = new List();
  
  
  
  public NewGamePane(HUD UI) {
    super(UI, MainScreen.MENU_NEW_GAME_SITE);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    //
    //  Pick a homeworld first.
    listing.add(createTextItem("Homeworld:", 1.2f, null));
    
    final VerseLocation homeworlds[] = Verse.ALL_PLANETS;
    for (final VerseLocation homeworld : homeworlds) {
      listing.add(createTextButton("  "+homeworld.name, 1, new Link() {
        public void whenClicked() { selectHomeworld(homeworld); }
      }));
    }
    listing.add(createTextItem(
      "Your homeworld will determine the initial colonists and finance "+
      "available to your settlement, along with technical expertise and "+
      "trade revenue.", 0.75f, Colour.LITE_GREY
    ));
    //
    //  Then pick a sector.
    listing.add(createTextItem("Landing Site:", 1.2f, null));

    final VerseLocation landings[] = Verse.ALL_DIAPSOR_SECTORS;
    for (final VerseLocation landing : landings) {
      listing.add(createTextButton("  "+landing.name, 1, new Link() {
        public void whenClicked() { selectLanding(landing); }
      }));
    }
    listing.add(createTextItem(
      "Your landing site will determine the type of resources initially "+
      "available to your settlement, along with local species and other "+
      "threats.", 0.75f, Colour.LITE_GREY
    ));
    
    forwardButton = createTextButton("Continue", 1, new Link() {
      public void whenClicked() {
        
      }
    });
    listing.add(forwardButton);
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainScreen.current();
    
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = false;
    
    forwardButton.hidden = landing == null || homeworld == null;
    super.updateState();
  }
  
  
  private void selectHomeworld(VerseLocation homeworld) {
    final MainScreen screen = MainScreen.current();
    screen.worldsDisplay.setSelection(homeworld);
    this.homeworld = homeworld;
  }
  
  
  private void selectLanding(VerseLocation landing) {
    final MainScreen screen = MainScreen.current();
    screen.display.setSelection(landing.name, true);
    this.landing = landing;
  }
}



  /*
  public static class Config {
    //  TODO:  Just pick House, Province, Options.  And a few perks.
    
    public Background house ;
    public Background gender;
    public List <Trait    > chosenTraits = new List <Trait    > ();
    public List <Skill    > chosenSkills = new List <Skill    > ();
    public List <Technique> chosenTechs  = new List <Technique> ();
    
    public List  <Background> advisors = new List  <Background> ();
    public Tally <Background> crew     = new Tally <Background> ();
    public Tally <Blueprint > built    = new Tally <Blueprint > ();
    public VerseLocation demesne;
    public int siteLevel, fundsLevel, titleLevel;
  }
  
  final Config config;
  //*/
  
  
  
  