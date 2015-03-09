/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.util.*;



//  TODO:  MERGE THIS WITH THE ACTOR'S MEMORY-SET?


public class Career implements Qualities {
  
  
  private static boolean
    verbose = false;
  
  final static int
    MIN_PERSONALITY           = 3,
    NUM_RANDOM_CAREER_SAMPLES = 3;
  
  
  private Actor subject;
  private Background gender;
  private Background vocation, birth, homeworld;
  private String fullName = null;
  
  
  public Career(
    Background vocation, Background birth,
    Background homeworld, Background gender
  ) {
    this.gender    = gender   ;
    this.vocation  = vocation ;
    this.birth     = birth    ;
    this.homeworld = homeworld;
  }
  
  
  public Career(Background root) {
    vocation = root;
  }
  
  
  public Career(Actor subject) {
    this.subject = subject;
  }
  
  
  public void loadState(Session s) throws Exception {
    subject   = (Actor) s.loadObject();
    gender    = (Background) s.loadObject();
    birth     = (Background) s.loadObject();
    homeworld = (Background) s.loadObject();
    vocation  = (Background) s.loadObject();
    fullName  = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(subject);
    s.saveObject(gender   );
    s.saveObject(birth    );
    s.saveObject(homeworld);
    s.saveObject(vocation );
    s.saveString(fullName);
  }
  
  
  public Background vocation() {
    return vocation;
  }
  
  
  public Background birth() {
    return birth;
  }
  
  
  public Background homeworld() {
    return homeworld;
  }
  
  
  public Background topBackground() {
    if (vocation  != null) return vocation ;
    if (homeworld != null) return homeworld;
    if (birth     != null) return birth    ;
    if (gender    != null) return gender   ;
    return null;
  }
  
  
  public String fullName() {
    return fullName;
  }
  
  
  
  /**  Fresh developments after recruitment...
    */
  public void recordVocation(Background b) {
    //
    //  TODO:  Add this to a list of vocations taken!
    vocation = b;
  }
  
  
  
  /**  Binds this career to a specific in-world actor and configures their
    *  physique, aptitudes and motivations:
    */
  public void applyCareer(Human actor, Base base) {
    if (verbose) I.say("\nGENERATING NEW CAREER");
    subject = actor;
    
    applyBackgrounds(actor, base);
    applySex(actor);
    setupAttributes(actor);
    fillPersonality(actor);
    
    //
    //  TODO:  Specify a few starter relationships here!  (And vary the base
    //  relation somewhat.)
    actor.relations.setRelation(base, 0.5f, 0);
    
    //  We top up basic attributes to match.
    actor.traits.initDNA(0);
    actor.health.setupHealth(
      Nums.clamp(Rand.avgNums(2), 0.26f, 0.94f),
      1, 0
    );
    
    //  For now, we apply gender at random, though this might be tweaked a bit
    //  later.  We also assign some random personality and/or physical traits.
    //
    //  Finally, specify name and (TODO:) a few other details of appearance.
    for (String name : Wording.namesFor(actor)) {
      if (fullName == null) fullName = name;
      else fullName+=" "+name;
    }
    ///I.say("Full name: "+fullName);
    //
    //  Along with current wealth and equipment-
    applyGear(vocation, actor);
    
    if (verbose) {
      I.say("  GENERATION COMPLETE: "+actor);
      I.say("  Personality:");
      for (Trait t : actor.traits.personality()) {
        final float level = actor.traits.traitLevel(t);
        I.say("    "+actor.traits.description(t)+" ("+level+")");
      }
    }
  }
  
  
  
  /**  Methods for generating an actor's background life-story:
    */
  private void applyBackgrounds(Human actor, Base base) {
    //
    //  If the target vocation is undetermined, we work forward at random from
    //  birth towards a final career stage:
    if (vocation == null) {
      pickBirthClass(actor, base);
      applyBackground(birth, actor);
      
      pickHomeworld(actor, base);
      applyBackground(homeworld, actor);
      applySystem((Sector) homeworld, actor);
      
      pickVocation(actor, base);
      applyBackground(vocation, actor);
    }
    //
    //  Alternatively, we work backwards from the target vocation to determine
    //  a probably system and social class of origin:
    else {
      applyBackground(vocation, actor);
      
      pickHomeworld(actor, base);
      applyBackground(homeworld, actor);
      applySystem((Sector) homeworld, actor);
      
      pickBirthClass(actor, base);
      applyBackground(birth, actor);
    }
  }
  
  
  private void pickBirthClass(Human actor, Base base) {
    if (birth != null) {
      return;
    }
    else if (base.isNative()) {
      birth = Backgrounds.BORN_NATIVE;
    }
    else {
      //  TODO:  What about noble birth?  What triggers that?
      final Batch <Float> weights = new Batch <Float> ();
      for (Background v : Backgrounds.OPEN_CLASSES) {
        weights.add(ratePromotion(v, actor, verbose));
      }
      birth = (Background) Rand.pickFrom(
        Backgrounds.OPEN_CLASSES, weights.toArray()
      );
    }
  }
  
  
  private void pickHomeworld(Human actor, Base base) {
    if (homeworld != null) {
      return;
    }
    else if (base.isNative()) {
      homeworld = base.world.offworld.worldSector();
    }
    else {
      //  TODO:  Include some weighting based off house relations!
      final Batch <Float> weights = new Batch <Float> ();
      for (Background v : Sectors.ALL_PLANETS) {
        weights.add(ratePromotion(v, actor, verbose));
      }
      homeworld = (Background) Rand.pickFrom(
        Sectors.ALL_PLANETS, weights.toArray()
      );
    }
  }
  
  
  private void pickVocation(Human actor, Base base) {
    final Pick <Background> pick = new Pick <Background> ();
    
    if (base.isNative()) {
      for (Background b : Backgrounds.NATIVE_CIRCLES) {
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num());
      }
    }
    else for (Background circle[] : base.commerce.homeworld().circles()) {
      final float weight = base.commerce.homeworld().weightFor(circle);
      for (Background b : circle) {
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num() * weight);
      }
      for (int n = NUM_RANDOM_CAREER_SAMPLES; n-- > 0;) {
        final Background b = (Background) Rand.pickFrom(
          Backgrounds.ALL_STANDARD_CIRCLES
        );
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num() / 2);
      }
    }
    this.vocation = pick.result();
  }
  
  
  private void applyBackground(Background v, Actor actor) {
    if (verbose) I.say("\nApplying vocation: "+v);
    
    for (Skill s : v.baseSkills.keySet()) {
      final int level = v.baseSkills.get(s);
      actor.traits.raiseLevel(s, level + (Rand.num() * 10) - 5);
      if (s.parent != null) actor.traits.raiseLevel(s.parent, level / 2);
    }
    
    for (Trait t : v.traitChances.keySet()) {
      float chance = v.traitChances.get(t);
      chance += Personality.traitChance(t, actor) / 2;
      actor.traits.incLevel(t, chance * Rand.avgNums(2) * 2);
      
      if (verbose) {
        I.say("  Chance for "+t+" is "+chance);
        final float level = actor.traits.traitLevel(t);
        I.say("  Level is now: "+level);
      }
    }
  }
  
  
  /**  Promotion-evaluation methods:
    */
  public static float ratePromotion(
    Background next, Actor actor, boolean report
  ) {
    float fitness = rateDefaultFitness(actor, next, report);
    float desire  = rateDefaultDesire (actor, next, report);
    return Nums.min(fitness, desire);
  }
  
  
  static float rateDefaultFitness(
    Actor actor, Background position, boolean report
  ) {
    float skillsRating = 0, sumSkills = 0;
    if (report) I.say("\nRating fitness of "+I.tagHash(actor)+" as "+position);
    //
    //  NOTE:  The numbers chosen here are quite sensitive, so please don't
    //  fiddle with them without some testing.
    for (Skill skill : position.baseSkills.keySet()) {
      final float
        jobLevel  = position.baseSkills.get(skill),
        haveLevel = actor.traits.traitLevel(skill),
        rating    = Nums.clamp((haveLevel + 10 - jobLevel) / 10, 0, 1.5f);
      sumSkills    += jobLevel;
      skillsRating += jobLevel * rating;
      if (report) {
        I.say("  "+skill+": "+rating+" (have "+haveLevel+"/"+jobLevel+")");
      }
    }
    if (sumSkills > 0) skillsRating = skillsRating /= sumSkills;
    else skillsRating = 1;
    
    if (report) I.say("  Overall rating: "+skillsRating);
    return skillsRating;
  }
  
  
  static float rateDefaultDesire(
    Actor actor, Background position, boolean report
  ) {
    float rating = 1.0f;
    if (report) I.say("\nRating desire by "+I.tagHash(actor)+" for "+position);
    //
    //  Citizens gravitate to jobs that suit their temperament, so we get a
    //  weighted average of those traits associated with the position, relative
    //  to how much the actor possesses them-
    if (position.traitChances.size() > 0) {
      float sumChances = 0, sumWeights = 0;
      for (Trait t : position.traitChances.keySet()) {
        final float posChance = position.traitChances.get(t);
        if (posChance == 0) continue;
        //
        //  NOTE: Personality traits are handled a little differently, since
        //  those can have opposites:
        final float ownChance = (t.type == Trait.PERSONALITY) ?
          Personality.traitChance(t, actor) :
          actor.traits.traitLevel(t)        ;
        
        if (report) I.say("  Chance due to "+t+" is "+ownChance+"/"+posChance);
        sumWeights += Nums.abs(posChance);
        sumChances += ownChance * (posChance > 0 ? 1 : -1);
      }
      
      if (sumWeights > 0) rating *= (1 + (sumChances / sumWeights)) / 2;
      if (report) {
        I.say("  Total trait chance: "+sumChances+"/"+sumWeights);
        I.say("  Subsequent rating:  "+rating);
      }
    }
    //
    //  Finally, we also favour transition to more prestigious vocations.  (In
    //  the case of actors already in the world, we skip this step, since we
    //  can use the finance-evaluation methods in FindWork.)
    if (
      actor instanceof Human && (! actor.inWorld()) &&
      position.standing != Backgrounds.NOT_A_CLASS
    ) {
      final Background prior = ((Human) actor).career().topBackground();
      int nextStanding = position.standing;
      if (report) {
        I.say("  Prior standing: "+prior.standing);
        I.say("  Next standing:  "+nextStanding  );
      }
      while (nextStanding < prior.standing) { rating /= 5; nextStanding++; }
      while (nextStanding > prior.standing) { rating *= 2; nextStanding--; }
    }
    if (report) I.say("  Overall rating: "+rating);
    return rating;
  }
  
  
  private void fillPersonality(Actor actor) {
    while (true) {
      final int numP = actor.traits.personality().size();
      if (numP >= MIN_PERSONALITY) break;
      final Trait t = (Trait) Rand.pickFrom(PERSONALITY_TRAITS);
      float chance = (Personality.traitChance(t, actor) / 2) + Rand.num();
      if (chance < 0 && chance > -0.5f) chance = -0.5f;
      if (chance > 0 && chance <  0.5f) chance =  0.5f;
      actor.traits.incLevel(t, chance);
    }
    
    actor.traits.incLevel(HANDSOME, Rand.rangeAvg(-2, 2, 2));
    actor.traits.incLevel(TALL    , Rand.rangeAvg(-2, 2, 2));
    actor.traits.incLevel(STOUT   , Rand.rangeAvg(-2, 2, 2));
  }
  
  
  
  /**  Methods for customising fundamental attributes, rather than life
    *  experience-
    */
  final static float
    STRAIGHT_CHANCE = 0.85f,
    BISEXUAL_CHANCE = 0.55f,
    TRANS_CHANCE    = 0.05f,
    
    RACE_CLIMATE_CHANCE  = 0.65f,
    HALF_CLIMATE_CHANCE  = 0.35f;
  
  
  private void setupAttributes(Actor actor) {
    float minPhys = 0, minSens = 0, minCogn = 0;
    for (Skill s : actor.traits.skillSet()) {
      final float level = actor.traits.traitLevel(s);
      actor.traits.raiseLevel(s.parent, level - Rand.index(5));
      if (s.form == FORM_COGNITIVE) minCogn = Nums.max(level, minCogn + 1);
      if (s.form == FORM_SENSITIVE) minSens = Nums.max(level, minSens + 1);
      if (s.form == FORM_PHYSICAL ) minPhys = Nums.max(level, minPhys + 1);
    }
    actor.traits.raiseLevel(MUSCULAR , (minPhys + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(IMMUNE   , (minPhys + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(MOTOR    , (minSens + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(PERCEPT  , (minSens + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(COGNITION, (minCogn + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(NERVE    , (minCogn + Rand.rollDice(3, 7)) / 2f);
  }
  
  
  private void applySex(Human actor) {
    
    if (gender == null) {
      final float
        rateM = ratePromotion(Backgrounds.BORN_MALE  , actor, verbose),
        rateF = ratePromotion(Backgrounds.BORN_FEMALE, actor, verbose);
      if (rateM * Rand.avgNums(2) > rateF * Rand.avgNums(2)) {
        gender = Backgrounds.BORN_MALE;
      }
      else gender = Backgrounds.BORN_FEMALE;
    }
    applyBackground(gender, actor);
    
    //  TODO:  Do some of these traits need to be rendered 'dormant' in younger
    //  citizens?
    
    float ST = Nums.clamp(Rand.rangeAvg(-1, 3, 2), 0, 3);
    if (Rand.num() < TRANS_CHANCE) ST = -1;
    final int GT = gender == Backgrounds.BORN_FEMALE ? 1 : -1;
    actor.traits.setLevel(GENDER_FEMALE,      GT);
    actor.traits.setLevel(GENDER_MALE  ,     -GT);
    actor.traits.setLevel(FEMININE     , ST * GT);
    actor.traits.setLevel(
      ORIENTATION,
       Rand.num() < STRAIGHT_CHANCE ? "Heterosexual" :
      (Rand.num() < BISEXUAL_CHANCE ? "Bisexual" : "Homosexual")
    );
  }
  
  
  //
  //  TODO:  Try incorporating these trait-FX into the rankings first.
  private void applySystem(Sector world, Actor actor) {
    //
    //  Assign skin texture (race) based on prevailing climate.  (Climate
    //  matching the parent homeworld is most likely, followed by races with
    //  similar skin tone- i.e, adjacent in the spectrum.)
    final boolean report = verbose;
    final Pick <Trait> racePick = new Pick <Trait> ();
    final int raceID = Visit.indexOf(world.climate, RACIAL_TRAITS);
    float sumChances = 0;
    if (report) {
      I.say("\nApplying effects of "+world);
      I.say("  Default climate:    "+world.climate+", ID: "+raceID);
    }
    
    for (int n = RACIAL_TRAITS.length; n-- > 0;) {
      float chance = 1;
      if (n                    == raceID) chance /= 1 - RACE_CLIMATE_CHANCE;
      if (Nums.abs(raceID - n) == 1     ) chance /= 1 - HALF_CLIMATE_CHANCE;
      if (report) {
        I.say("  Base chance for "+RACIAL_TRAITS[n]+" is: "+chance);
      }
      chance *= Rand.avgNums(2);
      sumChances += chance;
      racePick.compare(RACIAL_TRAITS[n], chance);
    }
    final Trait race = racePick.result();
    final float raceChance = racePick.bestRating() / sumChances;
    actor.traits.setLevel(race, (raceChance + 1) / 2);
    if (report) {
      I.say("  RACE PICKED: "+race+", CHANCE: "+raceChance);
      for (Trait t : RACIAL_TRAITS) {
        I.say("  Level of "+t+" is "+actor.traits.traitLevel(t));
      }
    }
    //  TODO:  Blend these a bit more, once you have the graphics in order?
    
    //
    //  Vary height/build based on gravity-
    //  TODO:  Have the citizen models actually reflect this.
    actor.traits.incLevel(TALL , Rand.num() * -1 * world.gravity);
    actor.traits.incLevel(STOUT, Rand.num() * 1 * world.gravity);
  }
  
  
  
  /**  And finally, some finishing touches for material and social assets-
    */
  public static void applyGear(Background v, Actor actor) {
    final int BQ = v.standing;
    
    for (Traded gear : v.gear) {
      if (gear instanceof DeviceType) {
        final int quality = Nums.clamp(BQ - 1 + Rand.index(3), 4);
        actor.gear.equipDevice(Item.withQuality(gear, quality));
      }
      else if (gear instanceof OutfitType) {
        final int quality = Nums.clamp(BQ - 1 + Rand.index(3), 4);
        actor.gear.equipOutfit(Item.withQuality(gear, quality));
      }
      else actor.gear.addItem(Item.withAmount(gear, 1 + Rand.index(3)));
    }
    
    final float cash = (50 + Rand.index(100)) * BQ / 2f;
    if (cash > 0) actor.gear.incCredits(cash);
    else actor.gear.incCredits(Rand.index(5));
    actor.gear.taxDone();
    
    actor.gear.boostShields(actor.gear.maxShields(), true);
  }
}





