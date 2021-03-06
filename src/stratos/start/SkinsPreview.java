/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.Input.Keys;

import java.io.*;



public class SkinsPreview extends VisualDebug {
  
  
  public static void main(String a[]) {
    PlayLoop.setupAndLoop(new SkinsPreview(), "stratos.graphics");
  }
  
  private static boolean
    verbose = true;
  
  final static Colour BACK_COLOUR = Colour.DARK_BLUE;
  final static PlaneFX.Model CENTRE_MARK_MODEL = PlaneFX.imageModel(
    "centre_mark_model", SkinsPreview.class,
    "media/GUI/selectCircle.png", 0.5f, 0, 0, false, true
  );
  
  final static int MAX_PATH_LEN = 100;
  final static char[] VALID_KEYS =
    "abcdefghijklmnopqrstuvwxyz._/01234567890"
  .toCharArray();
  
  final static String SETTINGS_PATH = "media/Tools/preview_settings.xml";
  
  final static int
    OPTION_ANIMS = 0,
    OPTION_COSTS = 1,
    NUM_OPTIONS  = 3;
  final static String OPTION_NAMES[] = {
    " (SHOW ANIMS) ", " (SHOW HUMAN COSTUME) "
  };
  
  private HUD    UI;
  private Text   modelPathEntry;
  private String lastValidPath = "";
  private String currentPath = null;

  private int optionType = OPTION_ANIMS;
  private XML        currentXML  ;
  private SolidModel currentModel;
  private String     currentAnim ;
  private boolean    shouldLoop  ;
  private boolean    partsHide[] ;
  private String     baseSkin    ;
  private String     costumeSkin ;
  
  private boolean showAsHuman;
  private String humanSkinPath, baseSkinName;
  private Batch <String> humanSkins, humanCostumes;
  
  private boolean showOrigin = true;
  private PlaneFX centerMark = null;
  

  private String basePartName() {
    for (final String partName : currentModel.partNames()) {
      final String skinName = currentModel.materialID(partName);
       if (skinName.equals(baseSkinName)) {
         return partName;
       }
    }
    return null;
  }

  protected void loadVisuals() {
    UI = new HUD(PlayLoop.rendering());
    //
    //  Get some default for general skins-
    final XML
      xml      = XML.load(SETTINGS_PATH),
      settings = xml.child("settings"),
      costumes = xml.child("humanCostume");
    String filePath = settings.value("defaultPath");
    String fileName = settings.value("defaultFile");
    this.currentPath = filePath+fileName;
    //
    //  Load in some defaults for human skins-
    this.showAsHuman   = settings.getBool("showAsHuman");
    this.humanSkinPath = costumes.value("path"    );
    this.baseSkinName  = costumes.value("basePart");
    this.humanSkins = new Batch <String> ();
    
    for (XML kid : costumes.child("basicSkins").allChildrenMatching("skin")) {
      final String name = kid.value("name"), path = humanSkinPath+name;
      if (Assets.exists(path)) humanSkins.add(name);
      else I.say("\n  Warning, no such skin file: '"+path+"'");
    }
    this.humanCostumes = new Batch <String> ();
    for (XML kid : costumes.child("costumeSkins").allChildrenMatching("skin")) {
      final String name = kid.value("name"), path = humanSkinPath+name;
      if (Assets.exists(path)) humanCostumes.add(name);
      else I.say("\n  Warning, no such skin file: '"+path+"'");
    }
    //
    //  Attach some basic UI items-
    modelPathEntry = new Text(UI, BaseUI.INFO_FONT);
    modelPathEntry.alignVertical  (0, 0);
    modelPathEntry.alignHorizontal(0, 0);
    modelPathEntry.attachTo(UI);
    //
    //  And some display FX-
    centerMark = (PlaneFX) CENTRE_MARK_MODEL.makeSprite();
    shouldLoop = true;
  }
  
  
  public HUD UI() {
    return UI;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    rendering.backColour = BACK_COLOUR;
    updatePath();
    updateModel();
    checkAssetsChange();
    setupText();
    
    centerMark.position.setTo(rendering.view.lookedAt);
    if (showOrigin) centerMark.readyFor(rendering);
    super.renderVisuals(rendering);
  }
  
  
  protected void onRendering(Sprite sprite, Rendering rendering) {
    final float duration = currentAnim == null || currentModel == null ? 0 :
      currentModel.defaultAnimDuration(currentAnim)
    ;
    final float time = Rendering.activeTime() / duration;
    sprite.setAnimation(currentAnim, time % 1, shouldLoop);
    
    if (! (sprite instanceof SolidSprite)) return;
    final SolidSprite solid = (SolidSprite) sprite;
    final String parts[] = currentModel.partNames();
    
    if (partsHide != null) for (int i = partsHide.length; i-- > 0;) {
      solid.togglePart(parts[i], ! partsHide[i]);
    }
    
    final Texture
      base = baseSkin == null ? null : ImageAsset.getTexture(
        humanSkinPath+baseSkin
      ),
      costume = costumeSkin == null ? null : ImageAsset.getTexture(
        humanSkinPath+costumeSkin
      );
    for (String part : parts) {
      final boolean isBase = part.equals(basePartName());
      
      if (isBase && base != null && costume != null) {
        solid.setOverlaySkins(part, base, costume);
      }
      else if (isBase && base != null) {
        solid.setOverlaySkins(part, base);
      }
      else if (!isBase && costume != null) {
        solid.setOverlaySkins(part, costume);
      }
      else {
        solid.clearOverlays(part);
      }
    }
  }
  
  
  
  /**  Helper method implementations-
    */
  private void updatePath() {
    if (inMoveMode()) return;
    //
    //  Firstly, filter the set of pressed keys for valid path-characters, and
    //  tack them on at the end of the path.
    for (char k : KeyInput.keysTyped()) {
      boolean valid = false;
      for (char c : VALID_KEYS) {
        if (c == k) valid = true;
        if (c == Character.toLowerCase(k)) valid = true;
      }
      if (currentPath.length() >= MAX_PATH_LEN || ! valid) continue;
      currentPath = currentPath+""+k;
    }
    //
    //  If backspace is typed, trim the path down again.
    if (KeyInput.wasTyped(Keys.DEL) && currentPath.length() > 0) {
      currentPath = currentPath.substring(0, currentPath.length() - 1);
    }
  }
  
  
  private void updateModel() {
    boolean report = verbose && I.used60Frames;
    //
    //  First of all, see if a valid file has been specified with this path-
    final File match = new File(currentPath);
    ///if (report) I.say("\nCurrent file path: "+currentPath);
    if (currentPath.equals(lastValidPath) || ! match.exists()) return;
    
    if (currentPath.endsWith(".ms3d")) {
      if (switchModel(match.getParent()+"/", match.getName(), null, null)) {
        lastValidPath = currentPath;
        currentXML = null;
      };
    }
    
    if (currentPath.endsWith(".xml")) {
      try {
        currentXML = XML.load(currentPath);
        lastValidPath = currentPath;
        switchToEntry(currentXML.allChildrenMatching("model")[0]);
      }
      catch(Exception e) { I.report(e); currentPath = ""; }
    }
  }
  
  
  private void setupText() {
    final Text t = modelPathEntry;
    t.setText("Enter File Path: "+currentPath);
    t.append("\n  Last Valid Path: "+lastValidPath);
    t.append(new Description.Link("\n  (clear path)") {
      public void whenClicked(Object context) { currentPath = ""; }
    });
    
    //
    //  If the current path refers to an xml entry, list the available files-
    if (currentXML != null) {
      t.append("\n\nFile entries:");
      
      for (final XML entry : currentXML.allChildrenMatching("model")) {
        t.append("\n  ");
        final String name = entry.value("name");
        t.append(new Description.Link(name) {
          public void whenClicked(Object context) { switchToEntry(entry); }
        });
      }
    }
    //
    //  List the various model-view options-
    t.append("\n\n");
    for (final String option : OPTION_NAMES) {
      final int index = Visit.indexOf(option, OPTION_NAMES);
      if (index == OPTION_COSTS && ! showAsHuman) continue;
      
      t.append(new Description.Link(option) {
        public void whenClicked(Object context) { optionType = index; }
      }, (index == optionType) ? Colour.GREEN : Text.LINK_COLOUR);
    }
    //
    //  If the current model has animations, list those too-
    if (currentModel == null) {
      t.append("\n\nNo model selected");
    }
    else if (optionType == OPTION_ANIMS) {
      t.append("\n  ");
      t.append(new Description.Link("(NONE)") {
        public void whenClicked(Object context) { currentAnim = null; }
      }, (currentAnim == null) ? Colour.GREEN : Text.LINK_COLOUR);
      
      for (final String animName : currentModel.animNames()) {
        t.append("\n  ");
        t.append(new Description.Link(animName) {
          public void whenClicked(Object context) { currentAnim = animName; }
        }, (currentAnim == animName) ? Colour.GREEN : Text.LINK_COLOUR);
      }
    }
    else if (optionType == OPTION_COSTS) {
      //
      //  If parts-hiding hasn't been set up already, then by default we hide
      //  anything except the base-group.
      final String parts[] = currentModel.partNames();
      if (partsHide == null) {
        partsHide = new boolean[parts.length];
        if (showAsHuman) for (int i = parts.length; i-- > 0;) {
          partsHide[i] = ! parts[i].equals(basePartName());
        }
      }
      t.append("\n\nModel parts:");
      //
      //  Display toggles for parts shown/hidden:
      for (final String part : parts) {
        final int index = Visit.indexOf(part, parts);
        final boolean hide = partsHide[index];
        
        t.append("\n  ");
        t.append(new Description.Link(part) {
          public void whenClicked(Object context) { partsHide[index] = ! hide; }
        }, (! partsHide[index]) ? Colour.GREEN : Text.LINK_COLOUR);

        String s = basePartName();
        final boolean isBase = part.equals(basePartName());
        final String skinName = currentModel.materialID(part);
        t.append("  ("+skinName+": "+(isBase ? "Base" : "Costume")+")");
      }
      //
      //  And display the array of options for base and costume skins-
      t.append("\n\nBase skins:");
      for (final String skin : humanSkins) {
        t.append("\n  ");
        final boolean picked = baseSkin == skin;
        t.append(new Description.Link(skin) {
          public void whenClicked(Object context) { baseSkin = picked ? null : skin; }
        }, picked ? Colour.GREEN : Text.LINK_COLOUR);
      }
      t.append("\n\nCostume skins:");
      for (final String skin : humanCostumes) {
        t.append("\n  ");
        final boolean picked = costumeSkin == skin;
        t.append(new Description.Link(skin) {
          public void whenClicked(Object context) { costumeSkin = picked ? null : skin; }
        }, picked ? Colour.GREEN : Text.LINK_COLOUR);
      }
    }
    //
    //  
    t.append("\n\nShould loop: ");
    t.append(new Description.Link(shouldLoop ? "TRUE" : "FALSE") {
      public void whenClicked(Object context) { shouldLoop = ! shouldLoop; }
    });
    t.append("\n\nMove mode: ");
    t.append(new Description.Link(inMoveMode() ? "TRUE" : "FALSE") {
      public void whenClicked(Object context) { toggleMoveMode(); }
    });
    t.append(new Description.Link(
      showOrigin ? " (hide origin)" : " (show origin)"
    ) {
      public void whenClicked(Object context) { showOrigin = ! showOrigin; }
    });
    
    t.append(
      "\n\nPress enter then use WASD to move model."+
      "\nUse arrow keys to zoom and change lighting.",
      Colour.LITE_GREY
    );
  }
  
  
  private boolean switchToEntry(XML entry) {
    final File match = new File(lastValidPath);
    final String
      path    = match.getParent()+"/",
      fileXML = match.getName();
    final String
      name = entry.value("name"),
      file = entry.value("file");
    return switchModel(path, file, fileXML, name);
  }
  
  
  private boolean switchModel(
    String path, String file, String fileXML, String nameXML
  ) {
    if (path == null || file == null) return false;
    final File match = new File(path+file);
    if (! match.exists()) return false;
    //
    //  Firstly, see if it's possible to load a new model from the given path
    //  and file strings.
    SolidModel newModel = null;
    try {
      final SolidModel model = MS3DModel.loadFrom(
        path, file, SkinsPreview.class, fileXML, nameXML
      );
      Assets.loadNow(model);
      if (model != null) newModel = model;
    }
    catch(Exception e) {
      I.say("ERROR LOADING "+path+file);
      I.report(e);
      currentPath = "";
      newModel = null;
    }
    //
    //  If the file loads successfully, then dispose of the old model and
    //  create a sprite based on the new.  Otherwise, return false.
    if (newModel != null) {
      Assets.disposeOf(currentModel);
      currentModel = newModel;
      partsHide = null;
      sprites.clear();
      sprites.add(currentModel.makeSprite());
      return true;
    }
    return false;
  }
  
  
  private void checkAssetsChange() {
    if (currentModel != null && Assets.checkForRefresh(currentModel, 500)) {
      partsHide = null;
      sprites.clear();
      sprites.add(currentModel.makeSprite());
    }
  }
}



