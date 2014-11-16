

package stratos.graphics.solids;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import stratos.graphics.common.*;
import stratos.start.Assets;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;

import org.apache.commons.math3.util.FastMath;



public class SolidSprite extends Sprite {
  
  
  final static float ANIM_INTRO_TIME = 0.2f;
  private static boolean verbose = true;
  
  
  final public SolidModel model;
  final Matrix4 transform = new Matrix4();
  final Matrix4 boneTransforms[];
  final Material materials[];
  private int hideMask = 0;
  
  private static class AnimState {
    Animation current;
    float time, incept;
  }
  final Stack <AnimState> animStates = new Stack <AnimState> ();
  
  private static Vector3 tempV = new Vector3();
  private static Matrix4 tempM = new Matrix4();
  
  
  
  protected SolidSprite(final SolidModel model) {
    this.model = model;
    if (! model.compiled) I.complain("MODEL MUST BE COMPILED FIRST!");
    
    this.boneTransforms = new Matrix4[model.allNodes.length];
    for (int i = boneTransforms.length; i-- > 0;) {
      boneTransforms[i] = new Matrix4();
    }
    
    this.materials = new Material[model.allMaterials.length];
    for (int i = materials.length; i-- > 0;) {
      materials[i] = model.allMaterials[i];
    }
    
    this.setAnimation(AnimNames.FULL_RANGE, 0, true);
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    
    final float AT = Rendering.activeTime();
    out.write(animStates.size());
    for (AnimState state : animStates) {
      out.writeInt  (model.indexFor(state.current));
      out.writeFloat(state.time  );
      out.writeFloat(AT - state.incept);
    }
  }
  
  
  protected void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    
    final float AT = Rendering.activeTime();
    for (int n = in.read(); n-- > 0;) {
      final AnimState state = new AnimState();
      state.current = model.gdxModel.animations.get(in.readInt());
      state.time    = in.readFloat();
      state.incept  = AT - in.readFloat();
      animStates.add(state);
    }
  }
  
  
  public void readyFor(Rendering rendering) {
    
    //  Set up the translation matrix based on game-world position and facing-
    Viewport.worldToGL(position, tempV);
    transform.setToTranslation(tempV);
    transform.scl(tempV.set(scale, scale, scale));
    
    final float radians = (float) FastMath.toRadians(
      model.rotateOffset - rotation
    );
    transform.rot(Vector3.Y, radians);
    
    model.animControl.begin(this);
    if (animStates.size() > 0) {
      //  If we're currently being animated, then we need to loop over each
      //  animation state and blend them together, while culling any that have
      //  expired-
      final float time = Rendering.activeTime();
      AnimState validFrom = animStates.getFirst();
      for (AnimState state : animStates) {
        float alpha = (time - state.incept) / ANIM_INTRO_TIME;
        if (alpha >= 1) { validFrom = state; alpha = 1; }
        model.animControl.apply(state.current, state.time, alpha);
      }
      while (animStates.getFirst() != validFrom) animStates.removeFirst();
    }
    model.animControl.end();
    
    //  The nodes here are ordered so as to guarantee that parents are always
    //  visited before children, allowing a single pass-
    for (int i = 0; i < model.allNodes.length; i++) {
      final Node node = model.allNodes[i];
      if (node.parent == null) {
        boneTransforms[i].setToTranslation(node.translation);
        boneTransforms[i].scl(node.scale);
        continue;
      }
      final Matrix4 parentTransform = boneFor(node.parent);
      tempM.set(parentTransform).mul(boneTransforms[i]);
      boneTransforms[i].set(tempM);
    }
    
    rendering.solidsPass.register(this);
  }
  
  
  
  /**  Rendering and animation-
   */
  static class Part {
    SolidSprite belongs;
    
    Texture texture, overlays[];
    Colour colour;
    
    Mesh mesh;
    Matrix4 meshBones[];
    int meshType, meshIndex, meshVerts;
  }
  
  
  protected void addPartsTo(Series <Part> allParts) {
    
    final Colour c = new Colour();
    if (this.colour == null) c.set(Colour.WHITE);
    else c.set(this.colour);
    c.r *= fog;
    c.g *= fog;
    c.b *= fog;
    
    for (int i = 0; i < model.allParts.length; i++) {
      final NodePart part = model.allParts[i];
      if ((hideMask & (1 << i)) != 0) continue;
      
      final int numBones = part.invBoneBindTransforms.size;
      //  TODO:  Use an object pool for these, if possible?
      final Matrix4 boneSet[] = new Matrix4[numBones];
      for (int b = 0; b < numBones; b++) {
        final Node node = part.invBoneBindTransforms.keys[b];
        final Matrix4 offset = part.invBoneBindTransforms.values[b];
        boneSet[b] = new Matrix4(boneFor(node)).mul(offset);
      }
      
      final int matIndex = model.indexFor(part.material);
      final Material material = materials[matIndex];

      final TextureAttribute t;
      t = (TextureAttribute) material.get(TextureAttribute.Diffuse);
      final OverlayAttribute a;
      a = (OverlayAttribute) material.get(OverlayAttribute.Overlay);
      
      final Part p = new Part();
      p.belongs = this;
      p.texture = t == null ? null : t.textureDescription.texture;
      p.overlays = a == null ? null : a.textures;
      p.colour = c;
      
      p.mesh = part.meshPart.mesh;
      p.meshBones = boneSet;
      p.meshType = part.meshPart.primitiveType;
      p.meshIndex = part.meshPart.indexOffset;
      p.meshVerts = part.meshPart.numVertices;
      allParts.add(p);
    }
  }
  
  
  protected Matrix4 boneFor(Node node) {
    final int index = model.indexFor(node);
    return boneTransforms[index];
  }
  
  
  public void setAnimation(String id, float progress, boolean loop) {
    Animation match = model.gdxModel.getAnimation(id);
    if (match == null) return;
    
    AnimState topState = animStates.getLast();
    final boolean newState =
      (animStates.size() == 0) ||
      (topState.current != match);
    
    if (newState) {
      topState = new AnimState();
      topState.current = match;
      topState.incept = Rendering.activeTime();
      animStates.addLast(topState);
    }
    if (loop) topState.time = progress * match.duration;
    else {
      final float minTime = progress * match.duration;
      if (minTime > topState.time) topState.time = minTime;
    }
  }
  
  
  
  /**  Customising appearance (toggling parts, adding skins)-
    */
  public void setOverlaySkins(String partName, Texture... skins) {
    final NodePart match = model.partWithName(partName);
    if (match == null) return;
    final Material base = match.material;
    final Material overlay = new Material(base);
    overlay.set(new OverlayAttribute(skins));
    this.materials[model.indexFor(base)] = overlay;
  }
  
  
  public Vec3D attachPoint(String function, Vec3D v) {
    if (v == null) v = new Vec3D();
    if (animStates.size() == 0) return v.setTo(position);
    
    final Integer nodeIndex = model.indexFor(function);
    if (nodeIndex == null) {
      return new Vec3D(position);
    }
    
    tempV.set(0, 0, 0);
    tempV.mul(boneTransforms[nodeIndex]);
    tempV.mul(transform);
    return Viewport.GLToWorld(tempV, v);
  }
  
  
  
  /**  Showing and hiding model parts-
    */
  private void hideMask(NodePart p, boolean is) {
    final int index = model.indexFor(p);
    if (is) hideMask |= 1 << index;
    else hideMask &= ~ (1 << index);
  }
  
  
  public void hideParts(String... partIDs) {
    for (String id : partIDs) {
      togglePart(id, false);
    }
  }
  
  
  public void showOnly(String partID) {
    final Node root = model.allNodes[0];
    boolean match = false;
    for (NodePart np : root.parts) {
      if (np.meshPart.id.equals(partID)) {
        hideMask(np, false);
        match = true;
      }
      else hideMask(np, true);
    }
    if (! match) I.say("  WARNING:  No matching model part: "+partID);
  }
  
  
  public void togglePart(String partID, boolean visible) {
    final Node root = model.allNodes[0];
    for (NodePart np : root.parts) {
      if (np.meshPart.id.equals(partID)) {
        hideMask(np, ! visible);
        return;
      }
    }
    I.say("  WARNING:  No matching model part: "+partID);
  }
}



