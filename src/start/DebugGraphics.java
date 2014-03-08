

package src.start;
import org.apache.commons.math3.util.FastMath;

import src.game.building.DeviceType;
import src.graphics.common.*;
import src.graphics.cutout.*;
import src.graphics.solids.*;
import src.graphics.sfx.*;
import src.graphics.widgets.*;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;



public class DebugGraphics {
  
  
  final static CutoutModel
    CM = CutoutModel.fromImage(
      "media/Buildings/military/bastion.png",
      DebugGraphics.class, 7, 5
    );
  final static MS3DModel
    SM = MS3DModel.loadFrom(
      "media/Actors/fauna/", "Micovore.ms3d",
      DebugGraphics.class, "FaunaModels.xml", "Micovore"
    );
  final static ShotFX.Model
    FM = new ShotFX.Model(
      "laser_beam_fx", DeviceType.class,
      "media/SFX/blast_beam.gif",
      0.05f, 0,
      0.5f, 3, true, true
    );
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new Playable() {
      
      private boolean loaded = false;
      List <Sprite> sprites = new List <Sprite> ();
      
      private boolean moused = false ;
      private float origX, origY, origR, origE ;
      
      
      public void beginGameSetup() {
        final Sprite SS = SM.makeSprite();
        sprites.add(SS);
        
        for (int i = 10 ; i-- > 0;) {
          final Sprite CS = CM.makeSprite();
          CS.position.set(i, -i, 0);
          CS.fog = (i + 1) / 10f;
          CS.colour = Colour.transparency(CS.fog);
          CS.scale = 0.5f;
          sprites.add(CS);
        }
        
        final TalkFX FX1 = new TalkFX() {
          int count = 0;
          public void update() {
            if (this.numPhrases() < 2) {
              addPhrase("Testing "+(count++), TalkFX.FROM_LEFT);
            }
            super.update();
          }
        };
        FX1.position.set(0, 0, 2);
        sprites.add(FX1);
        
        final ShieldFX FX2 = new ShieldFX() {
          public void update() {
            if (Rand.index(Rendering.FRAMES_PER_SECOND) <= 2) {
              final Vec3D point = new Vec3D();
              final float angle = (float) (Math.PI * 2 * Rand.num());
              point.x = 10 * (float) FastMath.sin(angle);
              point.y = 10 * (float) FastMath.cos(angle);
              this.attachBurstFromPoint(point, Rand.yes());
            }
            super.update();
          }
        };
        FX2.scale = 1.5f;
        FX2.position.set(-2, 2, 0);
        sprites.add(FX2);
        
        final ShotFX FX3 = new ShotFX(FM) {
          public void update() {
            super.update();
            if (Rand.index(Rendering.FRAMES_PER_SECOND) <= 1) {
              refreshShot();
            }
          }
        };
        FX3.position.set(0, 2, 0);
        FX3.origin.set(0, 1, 0);
        FX3.target.set(0, 4, 0);
        sprites.add(FX3);
        
        loaded = true;
      }
      
      
      public HUD UI() {
        return null;
      }
      
      
      public boolean isLoading() {
        return loaded;
      }
      
      
      public float loadProgress() {
        return loaded ? 1 : 0;
      }
      
      
      public boolean shouldExitLoop() {
        return false;
      }
      
      
      public void updateGameState() {
      }
      
      
      public void renderVisuals(Rendering rendering) {
        
        final Viewport port = rendering.view ;
        if (Gdx.input.isKeyPressed(Keys.UP)) {
          port.lookedAt.x-- ;
          port.lookedAt.y++ ;
        }
        if (Gdx.input.isKeyPressed(Keys.DOWN)) {
          port.lookedAt.x++ ;
          port.lookedAt.y-- ;
        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
          port.lookedAt.x++ ;
          port.lookedAt.y++ ;
        }
        if (Gdx.input.isKeyPressed(Keys.LEFT)) {
          port.lookedAt.x-- ;
          port.lookedAt.y-- ;
        }
        
        final int MX = Gdx.input.getX(), MY = Gdx.input.getY();
        if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
          if (! moused) {
            moused = true ;
            origX = MX ;
            origY = MY ;
            origR = port.rotation  ;
            origE = port.elevation ;
          }
          else {
            port.rotation  = origR + ((origX - MX) / 2);
            port.elevation = origE + ((MY - origY) / 2);
          }
        }
        else moused = false ;
        
        for (Sprite sprite : sprites) {
          sprite.update();
          sprite.registerFor(rendering);
          final float f = sprite.fog, a = f * (1 - f) * 4;
          sprite.colour = Colour.transparency(a);
          sprite.fog = (f + 0.01f) % 1;
          sprite.rotation += 90 / 60f;
          
          sprite.setAnimation(AnimNames.MOVE, sprite.fog);
        }
      }
    });
  }
}






