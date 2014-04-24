/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets ;
import stratos.graphics.common.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;



public class Bordering extends UINode {
  
  
  /**  
    */
  final Texture borderTex;
  public int
    left = 10, right = 10,
    bottom = 10, top = 10;
  public float
    leftU = 0.33f, rightU = 0.66f,
    bottomV = 0.33f, topV = 0.66f;
  
  
  public Bordering(HUD UI, ImageAsset tex) {
    super(UI);
    this.borderTex = tex.asTexture();
  }
  
  
  protected void render(SpriteBatch batch2D) {
    renderBorder(
      batch2D, bounds,
      left, right, top, bottom,
      leftU, rightU, bottomV, topV,
      borderTex
    );
  }
  
  
  
  /**  Public implementation for the convenience of other widgets-
    */
  final static float
    coordX[] = new float[4],
    coordY[] = new float[4],
    coordU[] = new float[4],
    coordV[] = new float[4];
  
  
  public static void renderBorder(
    SpriteBatch batch2D, Box2D area,
    int left, int right, int bottom, int top,
    float LU, float RU, float BV, float TV,
    Texture borderTex
  ) {
    coordX[0] = 0;
    coordX[1] = left;
    coordX[2] = area.xdim() - right;
    coordX[3] = area.xdim();
    
    coordY[0] = 0;
    coordY[1] = bottom;
    coordY[2] = area.ydim() - top;
    coordY[3] = area.ydim();
    
    coordU[0] = 0;
    coordU[1] = LU;
    coordU[2] = RU;
    coordU[3] = 1;
    
    coordV[0] = 0;
    coordV[1] = BV;
    coordV[2] = TV;
    coordV[3] = 1;
    
    for (int i = 4; i-- > 0;) {
      coordX[i] += area.xpos();
      coordY[i] = area.ymax() - coordY[i];
    }
    
    batch2D.setColor(1, 1, 1, 1);
    for (int x = 3 ; x-- > 0 ;) for (int y = 3 ; y-- > 0 ;) {
      batch2D.draw(
        borderTex,
        coordX[x],
        coordY[y],
        coordX[x + 1] - coordX[x],
        coordY[y + 1] - coordY[y],
        coordU[x],
        coordV[y],
        coordU[x + 1],
        coordV[y + 1]
      );
    }
  }
}







