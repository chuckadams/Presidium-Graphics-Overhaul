/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.graphics.common.*;




//  TODO:
//  Hot and Wet seasons on a yearly scale (60 days), plus Terraforming effects
//  over several years.  A single dark/light cycle lasts for 5/10 days or so.
//  (The game should include the ability to 'skip forward' over one or more
//   years, so you see the effects accelerated.)

public class Planet {
  
  
  
  final public static Colour
    MORNING_LIGHT  = new Colour(1.00f, 0.95f, 0.70f),
    DAY_LIGHT      = new Colour(1.00f, 1.00f, 1.00f),
    EVENING_LIGHT  = new Colour(0.70f, 0.95f, 1.00f),
    NIGHT_LIGHT    = new Colour(0.45f, 0.35f, 0.55f),
    ALL_LIGHTS[]   = { MORNING_LIGHT, DAY_LIGHT, EVENING_LIGHT, NIGHT_LIGHT };
  final static float
    fade = (1 / 3f) / 2,
    HF = fade / 2, midday = 0.25f,
    morning_start = 1.0f - HF,
    morning_end   = 0.0f + HF,
    evening_start = 0.5f - HF,
    evening_end   = 0.5f + HF;
  
  //
  //  TODO:  Make use of these...
  float dayPeriod, gravity, insolation, atmosphere;
  
  
  
  /**  Interpolating daylight values-
    */
  private static float worldTime(Stage world) {
    final float time = world.currentTime() / Stage.STANDARD_DAY_LENGTH;
    return (time + 1 - midday) % 1;
  }
  
  
  public static float dayValue(Stage world) {
    final float time = worldTime(world);
    
    if (time <= morning_end  ) return (time + HF) / fade;
    if (time <= evening_start) return 1;
    if (time <= evening_end  ) return (evening_end - time) / fade;
    if (time <= morning_start) return 0;
    else                       return (time - morning_start) / fade;
  }
  
  
  public static Colour lightValue(Stage world) {
    final float dayValue = dayValue(world);
    if (dayValue == 1) return DAY_LIGHT;
    if (dayValue == 0) return NIGHT_LIGHT;
    
    final Colour a, b;
    final float w;
    if (dayValue > 0.5f) {
      a = DAY_LIGHT;
      b = isMorning(world) ? MORNING_LIGHT : EVENING_LIGHT;
      w = (dayValue - 0.5f) * 2;
    }
    else {
      a = isMorning(world) ? MORNING_LIGHT : EVENING_LIGHT;
      b = NIGHT_LIGHT;
      w = dayValue * 2;
    }
    
    final Colour blend = new Colour().set(
      (a.r * w) + (b.r * (1 - w)),
      (a.g * w) + (b.g * (1 - w)),
      (a.b * w) + (b.b * (1 - w)),
      1
    );
    return blend;
  }
  
  
  public static boolean isMorning(Stage world) {
    final float time = worldTime(world);
    return time > morning_start || time <= midday;
  }
  
  
  public static boolean isEvening(Stage world) {
    final float time = worldTime(world);
    return time > midday && time <= evening_end;
  }
  
  
  public static boolean isNight(Stage world) {
    final float time = worldTime(world);
    return time > evening_end && time <= morning_start;
  }
  
}








