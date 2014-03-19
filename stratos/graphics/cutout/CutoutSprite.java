

package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;




public class CutoutSprite extends Sprite {
  
  
  final CutoutModel model ;
  
  
  public CutoutSprite(CutoutModel model) {
    this.model = model ;
  }
  
  
  public ModelAsset model() {
    return model ;
  }
  
  
  public void setAnimation(String animName, float progress) {}
  
  
  public void readyFor(Rendering rendering) {
    rendering.cutoutsPass.register(this);
  }
}



