/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import com.badlogic.gdx.math.*;



public class BorderedLabel extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    BLACK_BAR = ImageAsset.fromImage(
      BorderedLabel.class, "media/GUI/black_bar.png"
    );
  
  
  final Bordering bordering;
  final public Text text;
  
  private boolean doFade = false;
  private String oldMessage = null;
  private float fadeIn = 0, fadeOut = 0;
  
  
  public BorderedLabel(BaseUI UI) {
    super(UI);
    this.bordering = new Bordering(UI, BLACK_BAR);
    bordering.setInsets(20, 20, 10, 10);
    bordering.setUV(0.33f, 0.33f, 0.5f, 0.5f);
    bordering.alignAcross(0, 1);
    bordering.alignDown  (0, 1);
    bordering.attachTo(this);
    
    this.text = new Text(UI, INFO_FONT);
    text.alignAcross(0, 1);
    text.alignDown  (0, 1);
    text.attachTo(this);
  }
  
  
  public void setMessage(String message, boolean doFade, float across) {
    text.setText(message);
    this.doFade = doFade;
    this.fadeOut = 1;
    
    if (oldMessage == null || ! oldMessage.equals(message)) {
      oldMessage = message;
      this.fadeIn = doFade ? 0 : 1;
    }
    
    text.setToPreferredSize(UI.xdim());
    final int pw = (int) text.preferredSize().xdim();
    text.alignHorizontal(across, pw, (int) (pw * (0.5f - across)));
    final int ph = (int) text.preferredSize().ydim();
    text.alignVertical  (0.5f  , ph, 0);
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if (doFade) return null;
    else return super.selectionAt(mousePos);
  }
  
  
  protected void updateState() {
    bordering.alignToMatch(text, 10, 2);
    text     .relAlpha = Nums.min(fadeOut, fadeIn);
    bordering.relAlpha = Nums.min(fadeOut, fadeIn);
    
    if (doFade) {
      fadeOut = Nums.clamp(fadeOut - DEFAULT_FADE_INC, 0, 1);
      fadeIn  = Nums.clamp(fadeIn  + DEFAULT_FADE_INC, 0, 1);
      if (fadeOut == 0) text.setText("");
    }
    super.updateState();
  }
}










