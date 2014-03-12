


package src.graphics.cutout ;
import src.graphics.common.*;
import static src.graphics.common.GL.*;
import static src.graphics.cutout.CutoutModel.*;
import src.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.glutils.*;



public class CutoutsPass {
	
  
  final static int
    MAX_SPRITES   = 1000,
    COMPILE_LIMIT = MAX_SPRITES * SIZE ;
  
  private static Vector3 temp = new Vector3(), temp2 = new Vector3();
  
  
  final Rendering rendering;
  final Batch <CutoutSprite> inPass = new Batch <CutoutSprite> ();
  
  private Mesh compiled ;
  private float vertComp[] ;
  private short compIndex[] ;
  
  private int total = 0 ;
  private Texture lastTex = null ;
  private ShaderProgram shading ;
  
  
  
  public CutoutsPass(Rendering rendering) {
    this.rendering = rendering;
    
    compiled = new Mesh(
      Mesh.VertexDataType.VertexArray,
      false,
      MAX_SPRITES * 4, MAX_SPRITES * 6,
      VertexAttribute.Position(),
      VertexAttribute.Color(),
      VertexAttribute.TexCoords(0)
    );
    vertComp = new float[COMPILE_LIMIT];
    compIndex = new short[MAX_SPRITES * 6];
    
    for (int i = 0; i < compIndex.length ; i++) {
      compIndex[i] = (short) (((i / 6) * 4) + VERT_INDICES[i % 6]);
    }
    compiled.setIndices(compIndex) ;
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/cutouts.vert"),
      Gdx.files.internal("shaders/cutouts.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
    }
  }
  
  
  public void dispose() {
    compiled.dispose();
    shading.dispose();
  }
  
  
  
  /**  Rendering methods-
    */
  protected void register(CutoutSprite sprite) {
    inPass.add(sprite);
  }
  
  
  public void performPass() {
    //I.say("Sprites to render: "+inPass.size());
    for (CutoutSprite s : inPass) {
      compileSprite(s, rendering.camera());
    }
    compileAndRender(rendering.camera());
    clearAll();
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
  
  
  private void compileSprite(CutoutSprite s, Camera camera) {
    if (s.model.texture != lastTex || total >= COMPILE_LIMIT) {
      compileAndRender(camera);
    }
    
    for (int off = 0 ; off < SIZE ; off += VERTEX_SIZE) {
      final int offset = total + off;
      temp.set(
        s.model.vertices[X0 + off],
        s.model.vertices[Y0 + off],
        s.model.vertices[Z0 + off]
      );
      temp.scl(s.scale);
      rendering.view.worldToGL(s.position, temp2);
      temp.add(temp2);
      vertComp[X0 + offset] = temp.x;
      vertComp[Y0 + offset] = temp.y;
      vertComp[Z0 + offset] = temp.z;
      
      final Colour fog = Colour.greyscale(s.fog);
      final float colourBits;
      if (s.colour == null) colourBits = fog.bitValue;
      else if (! s.colour.blank()) colourBits = s.colour.bitValue;
      else colourBits = Colour.combineAlphaBits(fog, s.colour);
      
      vertComp[C0 + offset] = colourBits;
      vertComp[U0 + offset] = s.model.vertices[U0 + off];
      vertComp[V0 + offset] = s.model.vertices[V0 + off];
    }
    
    total += SIZE;
    lastTex = s.model.texture;
  }
  
  
  private void compileAndRender(Camera camera) {
    if (total == 0 || lastTex == null) return ;
    
    //I.say("  compiled: "+(total / SIZE)+", texture: "+lastTex.hashCode()) ;
    //I.say("  total floats: "+total) ;
    compiled.setVertices(vertComp, 0, total) ;
    
    shading.begin();
    shading.setUniformMatrix("u_camera", camera.combined);
    shading.setUniformi("u_texture", 0);
    
    final float lightSum[] = rendering.lighting.lightSum();
    shading.setUniform4fv("u_lighting", lightSum, 0, 4);
    
    lastTex.bind(0);
    compiled.render(shading, GL10.GL_TRIANGLES, 0, (total * 6) / SIZE);
    shading.end();

    total = 0 ;
  }
}




