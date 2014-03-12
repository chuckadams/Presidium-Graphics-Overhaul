


package src.graphics.terrain;
import src.graphics.common.*;
import static src.graphics.common.GL.*;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;



public class TerrainPass {
  
  
  final Rendering rendering;
  final ShaderProgram shader;
  private Batch <TerrainChunk> chunks = new Batch <TerrainChunk> ();
  private FogOverlay fogApplied = null;
  
  
  public TerrainPass(Rendering rendering) {
    this.rendering = rendering;
    this.shader = new ShaderProgram(
      Gdx.files.internal("shaders/terrain.vert"),
      Gdx.files.internal("shaders/terrain.frag")
    );
    if (! shader.isCompiled()) {
      throw new GdxRuntimeException("\n"+shader.getLog()) ;
    }
  }
  
  
  public void dispose() {
    shader.dispose();
  }
  
  
  protected void register(TerrainChunk chunk) {
    chunks.add(chunk);
  }
  
  
  protected void applyFog(FogOverlay fog) {
    fogApplied = fog;
  }
  
  
  public void performPass() {
    if (chunks.size() == 0) return;
    shader.begin();
    shader.setUniformMatrix("u_camera", rendering.camera().combined);
    shader.setUniformi("u_texture", 0);
    //shader.setUniformi("u_animTex", 1);
    
    final float lightSum[] = rendering.lighting.lightSum();
    shader.setUniform4fv("u_lighting", lightSum, 0, 4);
    
    final TerrainSet set = chunks.first().belongs;
    if (fogApplied != null) {
      fogApplied.applyToShader(shader);
      shader.setUniformi("u_fogFlag", GL_TRUE);
    }
    else shader.setUniformi("u_fogFlag", GL_FALSE);
    
    //  TODO:  What about customised terrain splats?  ...If the ID is -1,
    //  render them last, but in order of presentation.
    for (LayerType type : set.layers) type.renderChunks(shader, chunks);
    shader.end();
    
    clearAll();
  }
  
  
  public void clearAll() {
    chunks.clear();
    fogApplied = null;
  }
}



//  TODO:  ...Do I need this?
/*
if (chunk.belongs != set) I.complain(
  "ALL RENDERED CHUNKS MUST BELONG TO SAME TERRAIN SET!"
) ;
//*/