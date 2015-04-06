

package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Condensor extends Venue {
  
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Condensor.class, "media/Buildings/aesthete/PLAZA.png", 3, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Condensor.class, "media/GUI/Buttons/arcology_button.gif"
  );
  
  final static VenueProfile PROFILE = new VenueProfile(
    Condensor.class, "condensor", "Condensor",
    3, 2, Venue.Type.TYPE_FIXTURE,
    SolarBank.PROFILE, Owner.TIER_FACILITY
  );
  
  
  
  public Condensor(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      85 ,  // integrity
      1  ,  // armour
      200,  // build cost
      0  ,  // max upgrades
      Structure.TYPE_FIXTURE
    );
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
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return new Traded[] { ATMO, WATER };
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    structure.assignOutputs(
      Item.withAmount(ATMO , 10),
      Item.withAmount(WATER, 5 )
    );
    structure.setAmbienceVal(5);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "condensor");
  }
  
  
  public String helpInfo() {
    return
      "The Condensor provides water and life support to the surrounding "+
      "settlement.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_AESTHETIC;
  }
}








