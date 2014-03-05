

package src.graphics.common;
import src.graphics.widgets.*;
import src.util.*;

import org.apache.commons.math3.util.FastMath;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public class Viewport {
  
  
  final public static float
    DEFAULT_SCALE = 40.0f,
    DEFAULT_ROTATE  = 45,
    DEFAULT_ELEVATE = 25 ;
  
  
  final OrthographicCamera camera;
  final public Vec3D lookedAt = new Vec3D();
  public float
    rotation  = DEFAULT_ROTATE,
    elevation = DEFAULT_ELEVATE,
    zoomLevel = 1.0f ;
  
  final private Vector3 temp = new Vector3();
  
  
  public Viewport() {
    camera = new OrthographicCamera();
    update();
  }
  
  
  public void update() {
    final float
      wide = Gdx.graphics.getWidth(),
      high = Gdx.graphics.getHeight();
    final float screenScale = screenScale();
    camera.setToOrtho(false, wide / screenScale, high / screenScale);
    
    final float
      ER  = (float) FastMath.toRadians(elevation),
      opp = (float) FastMath.sin(ER) * 100,
      adj = (float) FastMath.cos(ER) * 100;
    
    camera.position.set(adj, opp, 0);
    temp.set(0, 0, 0);
    camera.lookAt(temp);
    
    camera.rotateAround(temp, Vector3.Y, 180 + rotation);
    camera.near = 0.1f;
    camera.far = 200.1f;
    
    worldToGL(lookedAt, temp);
    camera.position.add(temp);
    camera.update();
  }
  
  
  private float screenScale() {
    return DEFAULT_SCALE * zoomLevel;
  }
  

  public boolean intersects(Vec3D point, float radius) {
    worldToGL(point, temp);
    return camera.frustum.sphereInFrustumWithoutNearFar(temp, radius);
  }
  
  
  public boolean mouseIntersects(Vec3D point, float radius, HUD UI) {
    final Vec3D
      p = new Vec3D().setTo(point),
      m = new Vec3D().set(UI.mouseX(), UI.mouseY(), 0);
    translateToScreen(p).z = 0;
    final float distance = p.distance(m) / screenScale();
    return distance <= radius;
  }
  
  
  public Vec3D translateToScreen(Vec3D point) {
    worldToGL(point, temp);
    camera.project(temp);
    point.x = temp.x;
    point.y = temp.y;// Gdx.graphics.getHeight() - temp.y;
    point.z = temp.z;
    return point;
  }
  
  
  public Vec3D translateFromScreen(Vec3D point) {
    //  Note:  We have to treat the y values differently from screen
    //  translation, thanks to how LibGDX implements these functions.
    temp.x = point.x;
    temp.y = Gdx.graphics.getHeight() - point.y;
    temp.z = point.z;
    camera.unproject(temp);
    GLToWorld(temp, point);
    return point;
  }
  
  
  public Vec3D direction() {
    return GLToWorld(camera.direction, new Vec3D());
  }
  
  
  public Vector3 worldToGL(Vec3D from, Vector3 to) {
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
  
  
  public Vec3D GLToWorld(Vector3 from, Vec3D to) {
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
}



