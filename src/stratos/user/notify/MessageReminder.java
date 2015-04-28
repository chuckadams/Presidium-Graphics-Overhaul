/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;



public class MessageReminder extends ReminderListing.Entry {
  
  
  final static ImageAsset
    COMM_IMAGE = ImageAsset.fromImage(
      MessageReminder.class, "media/GUI/Panels/comms_alert.png"
    );
  
  final MessagePane message;
  final BorderedLabel label;
  private boolean opened = false;
  
  
  MessageReminder(
    final BaseUI baseUI, Object refers, final MessagePane message
  ) {
    super(baseUI, refers, 60, 40);
    this.message = message;
    
    final MessageReminder m = this;
    
    final Button button = new Button(
      baseUI, COMM_IMAGE.asTexture(), message.title
    ) {
      protected void whenClicked() {
        baseUI.setInfoPanels(message, null);
        I.say(message.title+" WAS OPENED! "+m+", opened? "+opened);
        opened = true;
      }
      
      protected void render(WidgetsPass pass) {
        super.render(pass);
        if (opened) return;
        float flashRate = (Rendering.activeTime() % 2) / 2;
        flashRate *= (1 - flashRate) * 2;
        super.renderTex(highlit, flashRate * absAlpha, pass);
      }
    };
    button.stretch = false;
    button.alignToFill();
    button.attachTo(this);
    
    label = new BorderedLabel(baseUI);
    label.alignLeft(0, 0);
    label.alignBottom(-DEFAULT_MARGIN, 0);
    label.text.scale = SMALL_FONT_SIZE;
    label.setMessage(message.title, false, 0);
    label.attachTo(this);
  }
  
  
  protected void setLabel(String message) {
    label.setMessage(message, false, 0);
  }
  
  
  protected void setOpened(boolean opened) {
    this.opened = opened;
  }
  
  
  protected boolean opened() {
    return opened;
  }
}





