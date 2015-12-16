/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.Image;
import stratos.user.*;
import stratos.util.*;



public class Faction extends Constant {
  
  
  /**  Public constant declarations-
    */
  final static Index <Faction> INDEX = new Index();
  final static String HOUSES_DIR = "media/Charts/houses/";
  
  final public static Faction
    
    FACTION_SUHAIL    = new Faction(
      "House Suhail" , HOUSES_DIR+"house_suhail.png" , Colour.DARK_MAGENTA,
      "",
        "Begin game with native vassals"+
      "\n2 fewer starting colonists",
      false
    ),
    FACTION_PROCYON   = new Faction(
      "House Procyon", HOUSES_DIR+"house_procyon.png", Colour.LITE_GREY,
      "",
        "2 extra starting colonists"+
      "\n40% penalty to tech research",
      false
    ),
    FACTION_ALTAIR    = new Faction(
      "House Altair" , HOUSES_DIR+"house_altair.png" , Colour.LITE_BLUE,
      "",
        "Extra starting advisor"+
      "\nAdvisor loyalty increased"+
      "\nStarting funds -20%",
      false
    ),
    FACTION_TAYGETA   = new Faction(
      "House Taygeta", HOUSES_DIR+"house_taygeta.png", Colour.CYAN,
      "",
        "Starting funds +30%"+
      "\nTribute +40%",
      false
    ),
    FACTION_CIVILISED = new Faction(
      "Civilised", null, Colour.WHITE     ,
      "", "",
      false
    ),
    CIVIL_FACTIONS[] = Faction.INDEX.soFar(Faction.class),
    
    FACTION_NATIVES   = new Faction(
      "Natives"  , null, Colour.LITE_YELLOW,
      "", "",
      true
    ),
    FACTION_WILDLIFE  = new Faction(
      "Wildlife" , null, Colour.LITE_GREEN ,
      "", "",
      true
    ),
    FACTION_VERMIN    = new Faction(
      "Vermin"   , null, Colour.LITE_BROWN ,
      "", "",
      true
    ),
    FACTION_ARTILECTS = new Faction(
      "Artilects", null, Colour.LITE_RED   ,
      "", "",
      true
    ),
    PRIMAL_FACTIONS[] = Faction.INDEX.soFar(Faction.class);
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private Faction parent;
  private VerseLocation startSite;
  private boolean primal;
  
  final String description, startInfo;
  final ImageAsset crestImage;
  final Colour bannerColour;
  
  
  protected Faction(
    String name, String crestPath, Colour banner,
    String description, String startInfo,
    boolean primal
  ) {
    super(INDEX, name, name);
    this.primal = primal;
    
    this.description = description;
    this.startInfo   = startInfo  ;
    this.bannerColour = new Colour(banner == null ? Colour.WHITE : banner);
    this.crestImage = crestPath == null ?
      Image.SOLID_WHITE : ImageAsset.fromImage(Verse.class, crestPath)
    ;
  }
  
  
  protected Faction(String name, String crestPath, Faction parent) {
    this(name, crestPath, parent.bannerColour, "", "", parent.primal);
    this.parent = parent;
  }
  
  
  protected void bindToStartSite(VerseLocation site) {
    //
    //  Must be called separately to avoid initialisation loop...
    if (startSite == null) return;
    this.startSite = site;
  }
  
  
  public static Faction loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Political setup information-
    */
  public VerseLocation startSite() {
    return startSite;
  }
  
  
  public Faction parent() {
    return parent;
  }
  
  
  public boolean primal() {
    return primal;
  }
  
  
  public void configStartingExpedition(Expedition e) {
    //
    //  TODO:  Move these into method-overrides for the individual factions, or
    //  include in a general argument-list!
    
    if (this == FACTION_SUHAIL) {
      e.seMigrantLimits(
        Expedition.DEFAULT_MAX_ADVISORS,
        Expedition.DEFAULT_MAX_COLONISTS - 2
      );
      e.setFunding(
        (int) (Expedition.DEFAULT_FUNDING * 1.0f),
        (int) (Expedition.DEFAULT_TRIBUTE * 1.0f)
      );
    }
    if (this == FACTION_ALTAIR) {
      e.seMigrantLimits(
        Expedition.DEFAULT_MAX_ADVISORS + 1,
        Expedition.DEFAULT_MAX_COLONISTS
      );
      e.setFunding(
        (int) (Expedition.DEFAULT_FUNDING * 0.8f),
        (int) (Expedition.DEFAULT_TRIBUTE * 1.0f)
      );
    }
    if (this == FACTION_PROCYON) {
      e.seMigrantLimits(
        Expedition.DEFAULT_MAX_ADVISORS,
        Expedition.DEFAULT_MAX_COLONISTS + 2
      );
      e.setFunding(
        (int) (Expedition.DEFAULT_FUNDING * 1.0f),
        (int) (Expedition.DEFAULT_TRIBUTE * 1.0f)
      );
    }
    if (this == FACTION_TAYGETA) {
      e.seMigrantLimits(
        Expedition.DEFAULT_MAX_ADVISORS,
        Expedition.DEFAULT_MAX_COLONISTS
      );
      e.setFunding(
        (int) (Expedition.DEFAULT_FUNDING * 1.3f),
        (int) (Expedition.DEFAULT_TRIBUTE * 1.4f)
      );
    }
    
    return;
  }
  
  
  public void applyEffectsOnLanding(Expedition e, Base base, Stage world) {
    //
    //  TODO:  Move these into method-overrides for the individual factions, or
    //  include in a general argument-list!
    if (this == FACTION_SUHAIL) {
      //
      //  In essence, we find the closest native-controlled province and assign
      //  that to be a vassal of House Suhail-
      final VerseLocation landing = world.offworld.stageLocation();
      final Pick <Demographic> pick = new Pick();
      
      for (VerseLocation l : Verse.ALL_DIAPSOR_SECTORS) {
        if (l.startingOwner == FACTION_NATIVES && l != landing) {
          final Demographic d = Verse.demographicFor(l, world.offworld);
          pick.compare(d, Verse.travelDistance(landing, l));
        }
      }
      if (pick.empty()) return;
      
      final Demographic nativeVassal = pick.result();
      final Expedition founding = new Expedition();
      founding.configFrom(
        landing, nativeVassal.location, this,
        Expedition.TITLE_KNIGHTED, 0, 0, new Batch()
      );
      nativeVassal.setClaimant(this, founding);
    }
  }
  
  
  public float techResearchMultiple() {
    //
    //  TODO:  Move these into method-overrides for the individual factions, or
    //  include in a general argument-list!
    if (this == FACTION_PROCYON) return 0.6f;
    
    return 1.0f;
  }
  
  
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeHelp(Description d, Selectable prior) {
    d.append(description);
  }
  
  
  public Colour bannerColour() {
    return bannerColour;
  }
  
}







