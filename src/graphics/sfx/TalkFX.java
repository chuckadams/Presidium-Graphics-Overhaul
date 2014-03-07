


package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;

import java.io.* ;
//import org.lwjgl.opengl.* ;




public class TalkFX extends SFX {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final static ModelAsset TALK_MODEL = new ModelAsset(
    "talk_fx_model", TalkFX.class
  ) {
    public boolean isLoaded() { return true; }
    protected void loadAsset() {}
    protected void disposeAsset() {}
    public Sprite makeSprite() { return new TalkFX() ; }
  } ;
  
  final static ImageAsset BUBBLE_TEX = ImageAsset.fromImage(
    "media/GUI/textBubble.png", TalkFX.class
  ) ;
  final static Alphabet FONT = Alphabet.loadAlphabet(
    "media/GUI/", "FontVerdana.xml"
  );
  
  final public static int
    MAX_LINES = 3,
    NOT_SPOKEN = 0,
    FROM_LEFT  = 1,
    FROM_RIGHT = 2 ;
  final static float
    //LINE_HIGH  = FONT.map[' '].height,
    //LINE_SPACE = LINE_HIGH + 10,
    FADE_RATE  = 1f / 25 ;
  
  public float fadeRate = 1.0f ;
  final float LINE_HIGH, LINE_SPACE;
  final Stack <Bubble> toShow  = new Stack <Bubble> ();
  final Stack <Bubble> showing = new Stack <Bubble> ();
  
  
  public TalkFX() {
    LINE_HIGH = FONT.letterFor(' ').height;
    LINE_SPACE = LINE_HIGH + 10;
  }
  
  
  public ModelAsset model() {
    return TALK_MODEL ;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    out.writeInt(toShow .size()) ;
    out.writeInt(showing.size()) ;
    final Batch <Bubble> all = new Batch <Bubble> () ;
    for (Bubble b : toShow ) all.add(b) ;
    for (Bubble b : showing) all.add(b) ;
    for (Bubble b : all) {
      LoadService.writeString(out, b.phrase) ;
      out.writeInt(b.type) ;
      out.writeFloat(b.width) ;
      out.writeFloat(b.xoff ) ;
      out.writeFloat(b.yoff ) ;
      out.writeFloat(b.alpha) ;
    }
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    final int numT = in.readInt(), numS = in.readInt() ;
    for (int n = 0 ; n < numT + numS ; n++) {
      final Bubble b = new Bubble() ;
      b.phrase = LoadService.readString(in) ;
      b.type = in.readInt() ;
      b.width = in.readFloat() ;
      b.xoff  = in.readFloat() ;
      b.yoff  = in.readFloat() ;
      b.alpha = in.readFloat() ;
      if (n < numT) toShow.add(b) ;
      else showing.add(b) ;
    }
  }
  
  
  
  /**  Updates and modifications-
    */
  static class Bubble {
    
    String phrase ;
    int type ;
    
    float width ;
    float xoff, yoff ;
    float alpha ;
  }
  
  
  public void addPhrase(String phrase) {
    addPhrase(phrase, NOT_SPOKEN) ;
  }
  
  
  public void addPhrase(String phrase, int bubbleType) {
    final Bubble b = new Bubble() ;
    b.phrase = phrase ;
    b.type = bubbleType ;
    toShow.add(b) ;
  }
  
  
  public void update() {
    //
    //  If there are bubbles awaiting display, see if you can move the existing
    //  bubbles up to make room.
    final Bubble first = showing.getFirst() ;
    final boolean
      shouldMove = toShow.size() > 0,
      canMove = showing.size() == 0 || first.alpha <= 1,
      isSpace = showing.size() == 0 || first.yoff >= LINE_SPACE ;
    if (shouldMove && canMove) {
      if (isSpace) {
        showBubble(toShow.removeFirst()) ;
      }
      else for (Bubble b : showing) {
        b.yoff += FADE_RATE * fadeRate * LINE_SPACE ;
      }
    }
    //
    //  In either case, gradually fate out existing bubbles-
    for (Bubble b : showing) {
      b.alpha -= FADE_RATE * fadeRate / MAX_LINES ;
      if (b.alpha <= 0) showing.remove(b) ;
    }
  }
  
  
  private void showBubble(Bubble b) {
    final float fontScale = 1;//LINE_HIGH / FONT.map[' '].height ;
    float width = 0 ;
    for (char c : b.phrase.toCharArray()) {
      Alphabet.Letter l = FONT.letterFor(c) ;
      if (l == null) l = FONT.letterFor(' ') ;
      width += l.width * fontScale ;
    }
    //
    //  You also need to either left or right justify, depending on the bubble
    //  type.
    b.width = width ;
    b.yoff = 5 ;
    if (b.type == NOT_SPOKEN) b.xoff = width / -2 ;
    if (b.type == FROM_LEFT ) b.xoff = width / -2 ;//20 - width ;
    if (b.type == FROM_RIGHT) b.xoff = width / -2 ;//-20 ;
    b.alpha = 1.5f ;
    showing.addFirst(b) ;
  }
  
  
  public int numPhrases() {
    return showing.size() + toShow.size() ;
  }
  
  
  /**  Rendering methods-
    */
  public void registerFor(Rendering rendering) {
    rendering.sfxPass.register(this);
  }
  
  
  protected void renderInPass(SFXPass pass) {
    if (showing.size() == 0) return;
    
    final Vec3D flatPoint = new Vec3D(position);
    pass.rendering.view.translateToScreen(flatPoint);
    final float fontScale = LINE_HIGH / FONT.letterFor(' ').height;
    
    for (Bubble bubble : showing) if (bubble.type != NOT_SPOKEN) {
      renderBubble(pass, bubble, flatPoint, bubble.type == FROM_RIGHT) ;
    }
    
    for (Bubble bubble : showing) {
      renderPhrase(pass, bubble, flatPoint, fontScale) ;
    }
  }
  
  
  //  TODO:  Consider unifying this code with the Border-display routines?
  
  private void renderBubble(
    SFXPass pass, Bubble bubble,
    Vec3D flatPoint, boolean fromRight
  ) {
    //
    //  Some of this could be moved to the constants section-
    final float
      x = flatPoint.x + bubble.xoff,
      y = flatPoint.y + bubble.yoff,
      //TW = BUBBLE_TEX.xdim(),
      //TH = BUBBLE_TEX.ydim(),
      //
      //  In the case of bubble from the right, we just flip U values-
      MIN_U = fromRight ? 1 : 0,
      MAX_U = fromRight ? 0 : 1,
      CAP_LU = fromRight ? 0.75f : 0.25f,
      CAP_RU = fromRight ? 0.25f : 0.75f,
      BOT_V = 0,
      TOP_V = 1,//BUBBLE_TEX.maxV(),
      
      //pad = 5,
      texHigh = (LINE_HIGH + 10) * 1.5f,
      minY = y - (5 + (texHigh / 3)),
      maxY = minY + texHigh,
      
      texWide = 128 * 40f / texHigh,  //True width/height for the texture.
      minX = x - 10,
      maxX = x + Math.max(bubble.width, 64) + 10,
      capXL = minX + (texWide * 0.25f),
      capXR = maxX - (texWide * 0.25f) ;
    //
    //  Render the three segments of the bubble-
    final Colour colour = Colour.transparency(bubble.alpha);
    pass.compileQuad(
      BUBBLE_TEX.asTexture(), colour,
      minX, minY, capXL - minX, maxY - minY,
      MIN_U, BOT_V, CAP_LU, TOP_V,
      0.5f, true
    );
    pass.compileQuad(
      BUBBLE_TEX.asTexture(), colour,
      capXL, minY, capXR - capXL, maxY - minY,
      CAP_LU, BOT_V, CAP_RU, TOP_V,
      0.5f, true
    );
    pass.compileQuad(
      BUBBLE_TEX.asTexture(), colour,
      capXR, minY, maxX - capXR, maxY - minY,
      CAP_RU, BOT_V, MAX_U, TOP_V,
      0.5f, true
    );
  }
  
  
  private void renderPhrase(
    SFXPass pass, Bubble bubble,
    Vec3D flatPoint, float fontScale
  ) {
    float
      scanW = 0,
      x = flatPoint.x + bubble.xoff,
      y = flatPoint.y + bubble.yoff ;

    final Colour colour = Colour.transparency(bubble.alpha);
    for (char c : bubble.phrase.toCharArray()) {
      Alphabet.Letter l = FONT.letterFor(c) ;
      if (l == null) l = FONT.letterFor(' ') ;
      
      pass.compileQuad(
        FONT.texture(), colour,
        x + scanW, y,
        l.width * fontScale, l.height * fontScale,
        l.umin, l.vmin, l.umax, l.vmax,
        0.5f, true
      );
      scanW += l.width * fontScale ;
    }
  }
}






