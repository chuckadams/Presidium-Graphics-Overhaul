/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.content.civic.*;
import stratos.game.wild.*;
import stratos.graphics.sfx.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;


/*
  Solo Run         (stealth 5.)
  First Shot       (stealth 10, marksmanship 5.)
  
  Tracer Slug      (marksmanship 10, stealth 5, sniper kit/training.)
  Alias            (masquerade 10, camo suit/training.)
  Overload         (inscription 5, jak terminal/training.)
  
  Slow Burn     & Fast Toxin     (IV Punks)
  Nip/Tuck      & Gene Roulette  (Ladder Snakes)
  Cyberkinetics & AI Assist      (Jak Blacks)
//*/

//  Slow burn.  Fast Toxin.  
//  Tracer Bead.  Overload.
//  Solo run.  First shot.  (general abilities.)
//
//  Alias and Nip/Tuck are going to require undercover ops, which I'm not
//  ready for yet.


public class RunnerTechniques {
  
  
  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = RunnerTechniques.class;
  
  final static PlaneFX.Model
    OVERLOAD_BURST_FX = PlaneFX.animatedModel(
      "overload_burst_fx", BASE_CLASS,
      FX_DIR+"overload_burst.png", 4, 2, 8, 1, 1.33f
    ),
    CYBER_HACK_FX = PlaneFX.imageModel(
      "toxin_burst_fx", BASE_CLASS,
      FX_DIR+"hack_flash_symbol.png",
      0.8f, 0.2f, 0.1f, false, true
    ),
    TOXIN_BURST_FX = PlaneFX.imageModel(
      "toxin_burst_fx", BASE_CLASS,
      FX_DIR+"toxin_burst.png",
      0.66f, 0, 0.33f, true, false
    );
  final public static ShotFX.Model
    SNIPER_TRACE_FX = new ShotFX.Model(
      "sniper_trace_fx", BASE_CLASS,
      FX_DIR+"sniper_trace.png",
      -1, 0.1f, 0.05f, 0.75f, false, true
    );
  
  final static int
    OVERLOAD_DAMAGE  = 10,
    
    SNIPER_HIT_MIN   = 5,
    SNIPER_HIT_MAX   = 15,
    
    BURN_STAT_BONUS  = 5,
    BURN_SKILL_BONUS = 2,
    BURN_CHARGES     = 4,
    
    TOXIN_DURATION   = Stage.STANDARD_HOUR_LENGTH / 2,
    TOXIN_RESIST_DC  = 10,
    TOXIN_DAMAGE     = 10,
    TOXIN_CHARGES    = 4 ;
  
  
  //  TODO:  Allow hiding inside enemy structures this way?
  //  TODO:  Sniper kit should improve night vision.
  
  
  final public static Technique OVERLOAD = new Technique(
    "Overload", UI_DIR+"sniper_kit.png",
    "Helps to bypass security systems and disable robotic opponents.",
    BASE_CLASS, "overload",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    NO_FATIGUE          ,
    MAJOR_CONCENTRATION ,
    IS_FOCUS_TARGETING | IS_TRAINED_ONLY, null, 0,
    Action.LOOK, Action.RANGED
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      return (current instanceof Combat && subject instanceof Artilect);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (success) {
        final Artilect zaps = (Artilect) subject;
        zaps.health.takeInjury(OVERLOAD_DAMAGE, true);
        
        ActionFX.applyBurstFX(CYBER_HACK_FX    , using, 0   , 1.0f);
        ActionFX.applyBurstFX(CYBER_HACK_FX    , zaps , 0   , 1.0f);
        ActionFX.applyBurstFX(OVERLOAD_BURST_FX, zaps , 0.5f, 0.5f);
      }
    }
  };
  
  
  final public static Technique SNIPER_KIT = new Technique(
    "Sniper Kit", UI_DIR+"sniper_kit.png",
    "Deals "+SNIPER_HIT_MIN+"-"+SNIPER_HIT_MAX+" damage to surprised or "+
    "stationary targets.",
    BASE_CLASS, "sniper kit",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    NO_FATIGUE          ,
    MAJOR_CONCENTRATION ,
    IS_FOCUS_TARGETING | IS_TRAINED_ONLY, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (current instanceof Combat && subject instanceof Actor) {
        final Actor mark = (Actor) subject;
        if (mark.senses.isEmergency() && mark.isMoving()) return false;
        return true;
      }
      return false;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final Actor mark = (Actor) subject;
      if (mark.senses.isEmergency() && mark.isMoving()) return false;
      final boolean hits = Combat.performGeneralStrike(
        actor, mark, Combat.OBJECT_DESTROY, actor.currentAction()
      );
      
      ActionFX.applyBurstFX(
        CommonTechniques.AIM_FX_MODEL, actor, 1.5f, 1
      );
      ActionFX.applyShotFX(
        SNIPER_TRACE_FX, actor, mark, hits, 1, actor.world()
      );
      
      return hits && super.checkActionSuccess(actor, mark);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      final Actor mark = (Actor) subject;
      if (success) {
        float damage = 0;
        damage += (SNIPER_HIT_MAX - SNIPER_HIT_MIN) * Rand.num();
        damage += SNIPER_HIT_MIN;
        mark.health.takeInjury(damage, true);
      }
    }
  };
  
  
  
  final public static Technique FAST_TOXIN = new Technique(
    "Fast Toxin", UI_DIR+"fast_toxin.png",
    "A fast-acting poison suitable for application to melee or kinetic "+
    "weaponry. Deals "+TOXIN_DAMAGE+" damage over "+TOXIN_DURATION+" seconds.",
    BASE_CLASS, "fast_toxin",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_FOCUS_TARGETING | IS_GAINED_FROM_ITEM, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (! actor.gear.hasDeviceProperty(Devices.KINETIC)) {
        return false;
      }
      if (current instanceof Combat && subject instanceof Actor) {
        final Actor struck = (Actor) subject;
        if (struck.traits.hasTrait(asCondition)) return false;
        if (! struck.health.organic()          ) return false;
        return true;
      }
      return false;
    }
    
    public Traded itemNeeded() {
      return FAST_TOXIN_ITEM;
    }
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final boolean hits = Combat.performGeneralStrike(
        actor, (Actor) subject, Combat.OBJECT_DESTROY, actor.currentAction()
      );
      return hits && super.checkActionSuccess(actor, subject);
    }
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      ActionFX.applyBurstFX(TOXIN_BURST_FX, subject, 0.5f, 0.5f);
      final Actor struck = (Actor) subject;
      using.gear.bumpItem(FAST_TOXIN_ITEM, -1f / TOXIN_CHARGES);
      struck.traits.incLevel(asCondition, 1);
    }
    
    protected float conditionDuration() {
      return TOXIN_DURATION;
    }
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      float toxLevel = affected.traits.traitLevel(asCondition);
      float damage = TOXIN_DAMAGE / conditionDuration();
      float resist = affected.skills.test(IMMUNE, TOXIN_RESIST_DC, 1, 2, null);
      
      if (resist > 0           ) damage *= 0.5f;
      if (resist > toxLevel * 3) affected.traits.remove(asCondition);
      affected.health.takeInjury(damage, true);
    }
  };
  
  
  final public static UsedItemType FAST_TOXIN_ITEM = new UsedItemType(
    BASE_CLASS, "Fast Toxin", null,
    40, FAST_TOXIN.description,
    FAST_TOXIN, RunnerMarket.class,
    1, REAGENTS, MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
  ) {
    
    public float useRating(Actor actor) {
      if (! PlanUtils.isArmed(actor)) return -1;
      if (! actor.gear.hasDeviceProperty(Devices.KINETIC)) return -1;
      return 1.5f - actor.traits.relativeLevel(ETHICAL);
    }
    
    public int normalCarry(Actor actor) {
      return 1;
    }
  };
  
  
  final public static Technique SLOW_BURN = new Technique(
    "Slow Burn", UI_DIR+"slow_burn.png",
    "Slows the user's perception of time, allowing for faster reactions and "+
    "effective concentration.",
    BASE_CLASS, "slow_burn",
    MEDIUM_POWER        ,
    REAL_HELP           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_SELF_TARGETING | IS_GAINED_FROM_ITEM, null, 0,
    Action.STRIKE_BIG, Action.NORMAL
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (current instanceof Combat && subject == actor) {
        return ! actor.traits.hasTrait(asCondition);
      }
      return false;
    }
    
    public Traded itemNeeded() {
      return SLOW_BURN_ITEM;
    }
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      using.gear.bumpItem(SLOW_BURN_ITEM, -1f / BURN_CHARGES);
      using.traits.setLevel(asCondition, 1);
    }
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      affected.traits.incBonus(MOTOR       , BURN_STAT_BONUS );
      affected.traits.incBonus(COGNITION   , BURN_STAT_BONUS );
      affected.traits.incBonus(HAND_TO_HAND, BURN_SKILL_BONUS);
      affected.traits.incBonus(MARKSMANSHIP, BURN_SKILL_BONUS);
    }
  };
  
  
  final public static UsedItemType SLOW_BURN_ITEM = new UsedItemType(
    BASE_CLASS, "Slow Burn", null,
    40, SLOW_BURN.description,
    SLOW_BURN, RunnerMarket.class,
    1, REAGENTS, MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
  ) {
    
    public float useRating(Actor actor) {
      if (! PlanUtils.isArmed(actor)) return -1;
      return 1.5f - actor.traits.relativeLevel(ETHICAL);
    }
    
    public int normalCarry(Actor actor) {
      return 1;
    }
  };
  
  
  final public static Technique RUNNER_TECHNIQUES[] = {
    OVERLOAD, SNIPER_KIT, FAST_TOXIN, SLOW_BURN
  };
}




//  TODO:  Restore some of these:

/*
final static Traded
  NEURAL_IMPLANT = new Traded(
    RunnerMarket.class, "Neural Implant", null, Economy.FORM_USED_ITEM, 100,
    "Neural implants boost cognitive ability and may allow hacking of "+
    "simple drones and cybrids."
  ),
  KINETIC_IMPLANT = new Traded(
    RunnerMarket.class, "Kinetic Implant", null, Economy.FORM_USED_ITEM, 120,
    "Kinetic implants boost strength and dexterity, together with a degree "+
    "of natural armour."
  ),
  SIMSTIMS = new Traded(
    RunnerMarket.class, "Simstims", null, Economy.FORM_SPECIAL, 45,
    "Simstims provide voyeuristic virtual entertainment to the masses."
  ),
  
  FAST_TOXIN = new Traded(
    RunnerMarket.class, "Fast Toxin", null, Economy.FORM_USED_ITEM, 85,
    "A fast-acting poison suitable for application to melee or kinetic "+
    "weaponry."
  ),
  SLOW_BURN = new Traded(
    RunnerMarket.class, "Slow Burn", null, Economy.FORM_USED_ITEM, 55,
    "An addictive narcotic that greatly accelerates reaction times and "+
    "perception."
  ),
  
  //  Disguise, beauty, and cognitive/sensitive/physical DNA treatments are
  //  reserved for the Hudzin Baru.
  
  //  TODO:  Maybe these should be special abilities for the runner class?
  //  Yes.  Work that out.
  
  SNIPER_KIT = new Traded(
    RunnerMarket.class, "Sniper Kit", null, Economy.FORM_USED_ITEM, 90,
    "Allows ranged attacks at far greater distances, particularly if the "+
    "target is surprised."
  ),
  STICKY_BOMB = new Traded(
    RunnerMarket.class, "Sticky Bomb", null, Economy.FORM_USED_ITEM, 35,
    "Deals heavy damage to vehicles and buildings, if attached at point-"+
    "blank range."
  ),
  NIGHT_OPTICS = new Traded(
    RunnerMarket.class, "Night Optics", null, Economy.FORM_USED_ITEM, 25,
    "Allows extended sight range in space or nocturnal conditions."
  ),
  GHOST_CAMO = new Traded(
    RunnerMarket.class, "Ghost Camo", null, Economy.FORM_USED_ITEM, 40,
    "Improves stealth and cover in daytime or outdoor environments."
  );
//*/


    
    //  TODO:  Restore later?
    /*
    RUNNER_SILVERFISH = new Background(
      Backgrounds.class,
      "Runner (Silverfish)", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, MASQUERADE,
      LEARNING, HAND_TO_HAND,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, METICULOUS,
      BLASTER, STEALTH_SUIT
    ),
    JACK_ARTIST = new Background(
      Backgrounds.class,
      "Jack Artist", "", "artificer_skin.gif", "artificer_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, INSCRIPTION, SIMULACRA,
      PRACTICED, ASSEMBLY, ACCOUNTING,
      LEARNING, SUASION, MASQUERADE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, METICULOUS,
      OVERALLS
    ),
    ASSASSIN = null,
    //  TODO:  Assassin.
    
    RUNNER_IV_PUNKS = new Background(
      Backgrounds.class,
      "Runner (IV Punks)", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, HAND_TO_HAND, SURVEILLANCE, BATTLE_TACTICS,
      LEARNING, COMMAND, MASQUERADE, ANATOMY,
      OFTEN, ACQUISITIVE, SOMETIMES, DEFENSIVE, DISHONEST,
      BLASTER, STEALTH_SUIT
    ),
    STREET_COOK = new Background(
      Backgrounds.class,
      "Street Cook", "", "physician_skin.gif", "physician_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, CHEMISTRY, PHARMACY, PRACTICED, FORENSICS,
      LEARNING, ANATOMY, COUNSEL, TRUTH_SENSE,
      OVERALLS
    ),
    BRUISER = null,
    //  TODO:  Bruiser.
    
    RUNNER_HUDZENA = new Background(
      Backgrounds.class,
      "Runner (Hudzeena)", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MASQUERADE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, SURVEILLANCE, SUASION,
      LEARNING, NATIVE_TABOO, XENOZOOLOGY,
      OFTEN, ACQUISITIVE, DISHONEST, SOMETIMES, NERVOUS, NATURALIST,
      BLASTER, STEALTH_SUIT
    ),
    FACE_FIXER = new Background(
      Backgrounds.class,
      "Face Fixer", "", "physician_skin.gif", "physician_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, GENE_CULTURE, ANATOMY, PRACTICED, SUASION,
      LEARNING, GRAPHIC_DESIGN, HANDICRAFTS,
      OVERALLS
    ),
    ANONYMOUS = null,
    //  TODO:  Anonymous.
    //*/


