/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.graphics.*;

import java.awt.Dimension;
import java.awt.Toolkit;



public final class PlayLoop {
  
  
  /**  Fields and constant definitions-
    */
  private static boolean
    verbose = false;
  
  final public static Class DEFAULT_INIT_CLASSES[] = {
    stratos.game.actors.Backgrounds.class,
    stratos.game.wild.Species.class
  };
  final public static String DEFAULT_INIT_PACKAGE = "stratos";
  
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 60,
    
    DEFAULT_WIDTH  = 1200,
    DEFAULT_HEIGHT = 720,
    DEFAULT_HERTZ  = 60,
    
    MIN_SLEEP    = 10,
    SLEEP_MARGIN = 2;
  
  
  private static String initPackage  = DEFAULT_INIT_PACKAGE;
  private static Class[] initClasses = DEFAULT_INIT_CLASSES;
  
  private static Rendering rendering;
  private static Playable playing;
  private static Playable prepared;
  private static Thread gdxThread;
  private static boolean loopChanged = false;
  
  private static long lastFrame, lastUpdate;
  private static float frameTime;
  private static long numStateUpdates = 0, numFrameUpdates = 0;
  private static float gameSpeed = 1.0f;
  
  private static boolean
    initDone   = false,
    shouldLoop = false,
    paused     = false,
    background = false,
    noInput    = false;
  
  
  final static String
    TITLE_IMG_PATH     = "media/GUI/title_image.png",
    BLANK_IMG_PATH     = "media/GUI/blank_back.png",
    PROG_FILL_IMG_PATH = "media/GUI/prog_fill.png",
    PROG_BACK_IMG_PATH = "media/GUI/prog_back.png";
  
  private static LoadingScreen loadScreen;
  
  
  
  /**  Returns the components of the current game state-
    */
  public static HUD currentUI() {
    return playing.UI();
  }
  
  public static Rendering rendering() {
    return rendering;
  }
  
  public static Playable played() {
    return playing;
  }
  
  public static boolean onMainThread() {
    return Thread.currentThread() == gdxThread;
  }
  
  public static boolean mainThreadBegun() {
    return gdxThread != null;
  }
  
  
  
  
  /**  The big static setup, run and exit methods-
    */
  private static LwjglApplicationConfiguration getConfig() {
    final Dimension SS = Toolkit.getDefaultToolkit().getScreenSize();
    final boolean report = true;
    
    final LwjglApplicationConfiguration
      config = new LwjglApplicationConfiguration()
    ;
    config.title = "Stratos";
    config.width  = Nums.min(DEFAULT_WIDTH , SS.width  - 100);
    config.height = Nums.min(DEFAULT_HEIGHT, SS.height - 100);
    config.foregroundFPS = DEFAULT_HERTZ;
    config.backgroundFPS = DEFAULT_HERTZ;
    config.resizable  = false;
    config.fullscreen = false;
    
    if (report) {
      I.say("\nSetting up screen configuration...");
      I.say("  Default width/height: "+DEFAULT_WIDTH+"/"+DEFAULT_HEIGHT);
      I.say("  Screen  width/height: "+SS    .width +"/"+SS    .height );
      I.say("  Window  width/height: "+config.width +"/"+config.height );
      I.say("");
    }
    return config;
  }
  
  
  public static void setupAndLoop(Playable scenario) {
    setupAndLoop(scenario, DEFAULT_INIT_PACKAGE, DEFAULT_INIT_CLASSES);
  }
  
  
  public static void setupAndLoop(
    Playable scenario, String initPackage, Class... initClasses
  ) {
    
    PlayLoop.initPackage = initPackage;
    PlayLoop.initClasses = initClasses;
    
    PlayLoop.loopChanged     = true;
    PlayLoop.prepared        = scenario;
    PlayLoop.numStateUpdates = 0;
    PlayLoop.numFrameUpdates = 0;
    PlayLoop.gameSpeed       = 1.0f;
    
    if (verbose) {
      I.say("ASSIGNED NEW PLAYABLE: "+scenario);
      I.reportStackTrace();
    }
    
    if (! initDone) {
      initDone = true;
      
      new LwjglApplication(new ApplicationListener() {
        public void create() {
          //
          //  NOTE:  We perform some extra diagnostic printouts here, since the
          //  GL context wasn't obtainable earlier:
          /*
          I.say(
            "Please send me this info"+
            "\n--- GL INFO -----------"+
            "\n   GL_VENDOR: "+glGetString(GL_VENDOR)+
            "\n GL_RENDERER: "+glGetString(GL_RENDERER)+
            "\n  GL_VERSION: "+glGetString(GL_VERSION)+
            "\nGLSL_VERSION: "+glGetString(GL_SHADING_LANGUAGE_VERSION)+
            "\n-----------------------\n"
          );
          //*/
          shouldLoop = true;
          initLoop();
        }
        
        public void resize(int width, int height) {
        }
        
        public void dispose() {
          disposeLoop();
        }
        
        public void pause() {
          background = true;
        }
        
        public void resume() {
          background = false;
        }
        
        public void render() {
          gdxThread = Thread.currentThread();
          if (! shouldLoop) {
            if (verbose) I.say("should not be looping...");
            return;
          }
          
          final boolean okay = advanceLoop();
          
          if (! okay) {
            if (verbose) I.say("Loop does not want to advance!");
            exitLoop();
          }
        }
      }, getConfig());
    }
  }
  
  
  public static void sessionStateWipe() {
    I.talkAbout = null;
    playing     = null;
    Assets.disposeSessionAssets();
    
    if (rendering != null) rendering.clearAll();
  }
  
  
  public static void exitLoop() {
    if (verbose) I.say("EXITING PLAY LOOP");
    shouldLoop = false;
    Gdx.app.exit();
  }
  
  
  private static void initLoop() {
    rendering = new Rendering();
    
    loadScreen = new LoadingScreen(
      rendering,
      TITLE_IMG_PATH,
      BLANK_IMG_PATH,
      PROG_FILL_IMG_PATH,
      PROG_BACK_IMG_PATH
    );
    
    Assets.compileAssetList(
      initPackage, initClasses
    );
    for (String name : Assets.classesToLoad()) {
      Session.checkSaveable(name);
    }
  }
  
  
  private static void disposeLoop() {
    rendering.dispose();
    Assets.disposeGameAssets();
  }
  
  
  private static boolean advanceLoop() {
    final long time = timeMS(), frameGap = time - lastFrame, updateGap;
    final int FRAME_INTERVAL  = 1000 / FRAMES_PER_SECOND;
    final int UPDATE_INTERVAL = (int) (
      1000 / (UPDATES_PER_SECOND * gameSpeed)
    );
    final boolean freeze = paused || background;
    
    if (freeze || (time - lastUpdate) > UPDATE_INTERVAL * 10) {
      lastUpdate = time;
      updateGap = 0;
    }
    else {
      updateGap = time - lastUpdate;
      frameTime = (updateGap - 0) * 1.0f / UPDATE_INTERVAL;
      frameTime = Nums.clamp(frameTime, 0, 1);
    }
    
    if (playing != prepared) {
      if (playing != null && playing.wipeAssetsOnExit()) {
        PlayLoop.sessionStateWipe();
      }
      playing = prepared;
    }
    loopChanged = false;
    float worldTime = (numStateUpdates + frameTime) / UPDATES_PER_SECOND;
    rendering.updateViews(worldTime, frameTime);
    
    if (verbose) {
      I.say("\nAdvancing play loop, time: "+time);
      I.say("  Last frame/last update: "+lastFrame+"/"+lastUpdate);
      I.say("  Frame/update gap: "+frameGap+"/"+updateGap);
      I.say("  FRAME/UPDATE INTERVAL: "+FRAME_INTERVAL+"/"+UPDATE_INTERVAL);
    }
    
    if (Assets.loadProgress() < 1) {
      if (verbose) {
        I.say("  Loading assets!");
        I.say("  Loading progress: "+Assets.loadProgress());
      }
      
      loadScreen.update("Loading Assets", Assets.loadProgress());
      Assets.advanceAssetLoading(FRAME_INTERVAL - (SLEEP_MARGIN * 2));
      
      rendering.renderDisplay(FRAMES_PER_SECOND);
      rendering.renderUI(loadScreen);
      return true;
    }
    
    if (loopChanged) {
      if (verbose) I.say("  Loop changed!  Will return");
      return true;
    }
    if (playing != null && playing.loadProgress() < 1) {
      if (verbose) {
        I.say("  Loading simulation: "+playing);
        I.say("  Is loading?         "+playing.isLoading());
        I.say("  Loading progress:   "+playing.loadProgress());
      }
      
      if (! playing.isLoading()) {
        if (verbose) I.say("  Beginning simulation setup...");
        playing.beginGameSetup();
      }
      loadScreen.update("Loading Simulation", playing.loadProgress());
      
      rendering.renderDisplay(FRAMES_PER_SECOND);
      rendering.renderUI(loadScreen);
      lastUpdate = lastFrame = time;
      return true;
    }
    
    //  TODO:  I'm updating graphics as fast as possible for the moment, since
    //  I get occasional flicker problems otherwise.  Still seems wasteful,
    //  mind...
    if (loopChanged) {
      if (verbose) I.say("  Loop changed!  Will return");
      return true;
    }
    if (frameGap >= FRAME_INTERVAL || true) {
      if (verbose) I.say("  Rendering stratos.graphics.");
      
      if (playing != null) {
        playing.renderVisuals(rendering);
      }
      final HUD UI = playing.UI();
      if (UI != null) {
        UI.updateInput();
        UI.renderWorldFX();
      }
      rendering.renderDisplay(FRAMES_PER_SECOND);
      rendering.renderUI(UI);
      KeyInput.updateInputs();
      lastFrame = time;
      numFrameUpdates++;
      
      I.used60Frames = numFrameUpdates % 60 == 0;
    }
    
    //  Now we essentially 'pretend' that updates were occurring once every
    //  UPDATE_INTERVAL milliseconds:
    if (playing != null) {
      final int numUpdates = Nums.min(
        (int) (updateGap / UPDATE_INTERVAL),
        (1 + (FRAME_INTERVAL / UPDATE_INTERVAL))
      );
      if (playing.shouldExitLoop()) {
        if (verbose) I.say("  Exiting loop!  Will return");
        return false;
      }
      
      if (verbose) I.say("  No. of updates: "+numUpdates);
      if (! freeze) for (int n = numUpdates; n-- > 0;) {
        
        if (loopChanged) {
          if (verbose) I.say("  Loop changed!  Will return");
          return true;
        }
        if (verbose) I.say("  Updating simulation.");
        playing.updateGameState();
        numStateUpdates++;
        lastUpdate += UPDATE_INTERVAL;
      }
    }
    return true;
  }
  
  
  private static long timeMS() {
    return java.lang.System.nanoTime() / 1000000;
  }
  
  
  
  /**  Pausing the loop, exiting the loop, and setting simulation speed and
    *  frame rate.
    */
  public static float frameTime() {
    return frameTime;
  }
  
  
  public static long frameUpdates() {
    return numFrameUpdates;
  }
  
  
  public static long stateUpdates() {
    return numStateUpdates;
  }
  
  
  public static boolean isFrameIncrement(int unit) {
    return (numFrameUpdates % unit) == 0;
  }
  
  
  public static boolean paused() {
    return paused;
  }
  
  
  public static float gameSpeed() {
    return gameSpeed;
  }
  
  
  public static void setGameSpeed(float mult) {
    gameSpeed = Nums.max(0, mult);
  }
  
  
  public static void setPaused(boolean p) {
    paused = p;
  }
  
  
  public static void setNoInput(boolean n) {
    noInput = n;
  }
}




