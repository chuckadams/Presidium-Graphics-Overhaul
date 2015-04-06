


package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.wild.Species;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.user.*;
import stratos.util.*;



public class Suspensor extends Mobile implements Mount {
  

  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset SUSPENSOR_MODEL = MS3DModel.loadFrom(
    FILE_DIR, "Barge.ms3d", Suspensor.class,
    XML_FILE, "Suspensor"
  );
  
  
  final Actor followed;
  final Plan tracked;
  
  private Actor passenger = null;
  private Item  cargo     = null;
  
  
  
  public Suspensor(Actor followed, Plan tracked) {
    super();
    this.followed = followed;
    this.tracked  = tracked ;
    attachSprite(SUSPENSOR_MODEL.makeSprite());
  }
  
  
  public Suspensor(Session s) throws Exception {
    super(s);
    followed  = (Actor) s.loadObject();
    tracked   = (Plan ) s.loadObject();
    passenger = (Actor) s.loadObject();
    cargo     = Item.loadFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(followed );
    s.saveObject(tracked  );
    s.saveObject(passenger);
    Item.saveTo(s, cargo);
  }
  
  
  
  /**  Satisfying contract as Mount-
    */
  public Actor mounted() {
    return passenger;
  }
  
  
  public boolean setMounted(Actor mounted, boolean is) {
    if (is) {
      if (this.passenger != null) return false;
      else this.passenger = mounted;
    }
    else {
      if (this.passenger != mounted) return false;
      else this.passenger = null;
    }
    return true;
  }
  
  
  public Property storedAt() {
    return null;
  }
  
  
  public boolean allowsActivity(Plan activity) {
    return false;
  }
  
  
  public boolean actorVisible(Actor mounted) {
    return true;
  }
  
  
  public void configureSpriteFrom(
    Actor mounted, Action action, Sprite actorSprite
  ) {
    actorSprite.setAnimation(Action.FALL, 1, false);
  }
  
  
  
  /**  Performing regular updates-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
  }
  
  
  public void exitWorld() {
    if (passenger != null) passenger.bindToMount(null);
    super.exitWorld();
  }
  
  
  public static Actor carrying(Actor other) {
    if (! (other.currentMount() instanceof Suspensor)) return null;
    final Suspensor s = (Suspensor) other.currentMount();
    return s.followed;
  }
  
  
  protected void updateAsMobile() {
    final boolean report = true && (
      I.talkAbout == passenger || I.talkAbout == followed
    );
    super.updateAsMobile();
    //
    //  Firstly, check whether you even need to exist any more-
    if ((! followed.inWorld()) || (followed.matchFor(tracked) == null)) {
      if (report) {
        I.say("\nSuspensor exiting world!");
        I.say("  Actor followed:   "+followed);
        I.say("  In world?         "+followed.inWorld());
        I.say("  Activity tracked: "+tracked);
        I.say("  Activity valid?   "+(followed.matchFor(tracked) != null));
      }
      if (passenger != null) {
        final Vec3D ground = this.position(null);
        ground.z = world.terrain().trueHeight(ground.x, ground.y);
        passenger.setHeading(ground, this.rotation, false, world);
      }
      exitWorld();
      return;
    }
    //
    //  If so, update your position so as to follow behind the actor-
    final Vec3D FP = followed.position(null);
    final Vec2D FR = new Vec2D().setFromAngle(followed.rotation());
    final float idealDist = followed.radius() + this.radius();
    FR.scale(0 - idealDist);
    FP.x += FR.x;
    FP.y += FR.y;
    nextPosition.setTo(FP);
    nextPosition.z = aboveGroundHeight();
    nextRotation = followed.rotation();
    //
    //  And if you have a passenger, update their position.
    if (passenger != null) {
      if (followed.indoors()) {
        final Boarding toBoard = followed.aboard();
        goAboard(toBoard, world);
        passenger.goAboard(toBoard, world);
        passenger.setHeading(toBoard.position(null), 0, false, world);
      }
      else {
        final Vec3D raise = new Vec3D(nextPosition);
        raise.z += 0.15f * GameSettings.actorScale;
        passenger.setHeading(raise, nextRotation, false, world);
      }
    }
  }
  
  
  public boolean isMoving() {
    return followed.isMoving();
  }
  
  
  protected float aboveGroundHeight() { return 0.15f; }
  public float radius() { return 0.0f; }
  public Base base() { return followed.base(); }
  
  
  protected float spriteScale() {
    return super.spriteScale() * GameSettings.actorScale;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return;
    super.renderFor(rendering, base);
  }
  
  
  public void describeStatus(Description d) {
    if (passenger != null) {
      d.append("Carrying ");
      d.append(passenger);
    }
    else if (cargo != null) {
      d.append("Carrying "+cargo);
    }
    else d.append("Idling");
  }
}





