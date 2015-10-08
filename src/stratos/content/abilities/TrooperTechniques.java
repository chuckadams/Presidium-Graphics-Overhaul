/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.sfx.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.content.civic.TrooperLodge;



//  TODO:  Implement these?  (And consider adding one or two extra SFX for the
//  frag launcher?  Limit the ammo?)

//  Platform Gunner.
//  Golem Armour Use.


public class TrooperTechniques {

  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = TrooperTechniques.class;
  
  final public static PlaneFX.Model
    ZAP_FX_MODEL = PlaneFX.imageModel(
      "zap_fx", BASE_CLASS,
      FX_DIR+"electro_zap.png",
      0.3f, 0, 0.1f, true, false
    ),
    HARMONICS_FX_MODEL = PlaneFX.imageModel(
      "harmonics_fx", BASE_CLASS,
      FX_DIR+"shield_harmonics.png",
      0.45f, 0, 0.2f, true, false
    ),
    FRAG_BURST_MODEL = PlaneFX.imageModel(
      "frag_burst_fx", BASE_CLASS,
      FX_DIR+"frag_burst.png",
      1.0f, 0, 1.0f, true, true
    );
  final public static ShotFX.Model
    FRAG_MISSILE_MODEL = new ShotFX.Model(
      "frag_missile_fx", BASE_CLASS,
      FX_DIR+"frag_missile.png",
      0.05f, 0.1f, 0.15f, 0.75f, false, false
    );
  
  final static float
    HARMONICS_RANGE   = 2.0f,
    HARMONICS_BOOST   = 1.0f,
    FRAG_BURST_RANGE  = 2.0f,
    FRAG_DAMAGE_BONUS = 4.0f;
  
  
  
  final public static Technique ELECTROCUTE = new Technique(
    "Electrocute", UI_DIR+"electrocute.png",
    "Deals bonus shock damage during close combat, even in defence.",
    BASE_CLASS, "electrocute",
    MINOR_POWER        ,
    REAL_HARM          ,
    NO_FATIGUE         ,
    MINOR_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, HAND_TO_HAND, 5,
    HAND_TO_HAND
  ) {
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return
        hasGear(learns, Devices.HALBERD_GUN) &&
        hasUpgrade(learns.mind.work(), TrooperLodge.MELEE_TRAINING, 1) &&
        super.canBeLearnt(learns, trained);
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return Rand.num() * 5;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (success && subject instanceof Actor) {
        ActionFX.applyBurstFX(ZAP_FX_MODEL, subject, 0.5f, 1);
        
        final float damage = using.gear.totalDamage() * Rand.num() / 2;
        final Actor struck = (Actor) subject;
        struck.health.takeFatigue(damage);
        struck.health.takeInjury(damage, false);
      }
    }
  };
  
  
  final public static Technique SHIELD_HARMONICS = new Technique(
    "Shield Harmonics", UI_DIR+"shield_harmonics.png",
    "Provides additional shield regeneration to nearby allies.",
    BASE_CLASS, "shield_harmonics",
    MEDIUM_POWER       ,
    REAL_HELP          ,
    NO_FATIGUE         ,
    MINOR_CONCENTRATION,
    IS_INDEPENDANT_ACTION | IS_TRAINED_ONLY, null, -1,
    Action.FIRE, Action.QUICK
  ) {
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return
        hasGear(learns, Devices.HALBERD_GUN) &&
        hasUpgrade(learns.mind.work(), TrooperLodge.MARKSMAN_TRAINING, 1) &&
        super.canBeLearnt(learns, trained);
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (! (current instanceof Combat)) return false;
      if (actor.traits.hasTrait(asCondition)) return false;
      return true;
    }
    
    
    public float basePriority(Actor actor, Target subject, float harmWanted) {
      return super.basePriority(actor, actor, Technique.REAL_HELP);
    }


    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected Action createActionFor(Plan parent, Actor actor, Target subject) {
      final Action a = super.createActionFor(parent, actor, actor);
      //
      //  If possible, we move next to a nearby ally so as to maximise the
      //  effect:
      final Pick <Actor> pick = new Pick(0);
      for (Actor ally : PlanUtils.subjectsInRange(actor, HARMONICS_RANGE)) {
        if (actor.base() != ally.base() || actor == ally) continue;
        final float rating = 1 / 1f + Spacing.distance(actor, ally);
        pick.compare(ally, rating);
      }
      if (! pick.empty()) {
        a.setMoveTarget(pick.result().origin());
      }
      return a;
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      ActionFX.applyBurstFX(HARMONICS_FX_MODEL, affected, 0.5f, 1);
      affected.gear.boostShields(HARMONICS_BOOST, true);
      
      for (Actor ally : PlanUtils.subjectsInRange(affected, HARMONICS_RANGE)) {
        if (affected.base() != ally.base() || affected == ally) continue;
        ally.gear.boostShields(HARMONICS_BOOST / 2, true);
        ActionFX.applyBurstFX(HARMONICS_FX_MODEL, ally, 0.5f, 1);
      }
    }
  };
  
  
  final public static Technique FRAG_LAUNCHER = new Technique(
    "Frag Launcher", UI_DIR+"frag_launcher.png",
    "Launches a devastating rocket attack at long-to-medium range.",
    BASE_CLASS, "frag_launcher",
    MAJOR_POWER        ,
    EXTREME_HARM       ,
    MINOR_FATIGUE      ,
    MAJOR_CONCENTRATION,
    IS_INDEPENDANT_ACTION | IS_TRAINED_ONLY, MARKSMANSHIP, 15,
    Action.FIRE, Action.QUICK | Action.RANGED
  ) {
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return
        hasGear(learns, Devices.HALBERD_GUN) &&
        hasUpgrade(learns.mind.work(), TrooperLodge.MARKSMAN_TRAINING, 1) &&
        super.canBeLearnt(learns, trained);
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (passive || action == null || ! (action.subject() instanceof Actor)) {
        return false;
      }
      return current instanceof Combat;
    }
    
    
    public float basePriority(Actor actor, Target subject, float harmWanted) {
      //
      //  TODO:  You need some generalised methods for handling AoE effects-
      //  both impacts and evaluation.
      final float priority = super.basePriority(actor, subject, harmWanted);
      if (priority <= 0) return 0;
      
      for (Actor a : PlanUtils.subjectsInRange(subject, FRAG_BURST_RANGE)) {
        if (PlanUtils.combatPriority(
          actor, a, 0, 1, false, Plan.EXTREME_HARM
        ) <= 0) return -1;
      }
      return priority;
    }


    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      
      //  TODO:  Could this be unified with similar methods in the Combat
      //  class?
      
      final Stage world = using.world();
      ActionFX.applyShotFX(
        FRAG_MISSILE_MODEL, using, subject, success, 1.5f, world
      );
      ActionFX.applyBurstFX(
        FRAG_BURST_MODEL, subject.position(null), 1.5f, world
      );
      
      for (Actor a : PlanUtils.subjectsInRange(subject, FRAG_BURST_RANGE)) {
        float damage = (Rand.num() + 0.5f) * using.gear.totalDamage() / 1.5f;
        damage += FRAG_DAMAGE_BONUS;
        if (a != subject) damage /= 2;
        
        damage = Nums.max(0, a.gear.afterShields(damage, true));
        damage -= a.gear.totalArmour() * Rand.num();
        
        if (damage > 0) {
          a.health.takeInjury(damage, true);
          a.enterStateKO(Action.FALL);
          float damageLevel = damage / a.gear.baseArmour();
          Item.checkForBreakdown(a, a.gear.outfitEquipped(), damageLevel, 1);
        }
      }
    }
  };
  
  
  final public static Technique POWER_ARMOUR_USE = new Technique(
    "Power Armour Use", UI_DIR+"power_armour_use.png",
    "Permits the use of "+Outfits.POWER_ARMOUR+".",
    BASE_CLASS      , "power_armour_use",
    MAJOR_POWER     ,
    NO_HARM         ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_GEAR_PROFICIENCY | IS_TRAINED_ONLY, HAND_TO_HAND, 15,
    Outfits.POWER_ARMOUR
  ) {};
  
}













