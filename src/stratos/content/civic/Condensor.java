/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



public class Condensor extends Venue {
  
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Condensor.class, "condensor_model",
    "media/Buildings/aesthete/condensor.png", 2, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Condensor.class, "condensor_button",
    "media/GUI/Buttons/condensor_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Condensor.class, "condensor",
    "Condensor", Target.TYPE_ECOLOGIST, ICON,
    "The Condensor provides "+WATER+" and "+ATMO+" to the surrounding "+
    "settlement.",
    2, 1, Structure.IS_FIXTURE, Owner.TIER_FACILITY, 85, 1,
    ATMO, WATER
  );
  
  final public static Upgrade LEVELS[] = BLUEPRINT.createVenueLevels(
    Upgrade.SINGLE_LEVEL, BotanicalStation.LEVELS[0],
    new Object[] { 10, ASSEMBLY, 0, CHEMISTRY },
    250
  );
  
  
  
  public Condensor(Base base) {
    super(BLUEPRINT, base);
    attachModel(MODEL);
  }
  
  
  public Condensor(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic functions-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (structure.intact()) {
      final Tile at = world.tileAt(this);
      
      float waterOut = 0;
      waterOut += world.terrain().fertilitySample(at) / 1f;
      waterOut += world.terrain().habitatSample(at, Habitat.SHALLOWS);
      waterOut += world.terrain().habitatSample(at, Habitat.OCEAN   );
      waterOut *= 10;
      
      stocks.forceDemand(POWER, 4, 0       );
      stocks.forceDemand(ATMO , 0, 5       );
      stocks.forceDemand(WATER, 0, waterOut);
      structure.setAmbienceVal(5);
    }
    else {
      stocks.clearDemands();
      structure.setAmbienceVal(0);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configStandardPanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    String info = super.helpInfo();
    if (stocks.production(WATER) < 5) info =
      "The terrain around this Condensor is very dry, which will limit "+WATER+
      " output.";
    return info;
  }
}









