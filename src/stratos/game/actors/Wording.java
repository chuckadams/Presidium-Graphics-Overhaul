/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.base.*;
import stratos.game.common.Actor;
import stratos.game.plans.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;


//
//  TODO:  SPLIT THIS INTO DIFFERENT CLASSES

public class Wording {
  
  
  
  /**  These methods generate names for new actors.
    */
  //
  //  TODO:  DERIVE NAMES FROM HOMEWORLD OF ORIGIN IF POSSIBLE
  final static String
    //
    //  Natives only have first names, but might use son/daughter of X as a
    //  title, or a conspicuous trait.
    NATIVE_MN[] = {
      "Duor", "Huno", "Umun", "Tunto", "Parab", "Sxumo", "Zhaka", "Hoka"
    },
    NATIVE_FN[] = {
      "Khasi", "Mari", "Tesza", "Borab", "Hana", "Kaeli", "Hlara", "Ote"
    },
    //
    //  Pyons have first and second names as standard.
    PYON_MN[] = {
      "Haber", "Danyl", "Jeme", "Marec", "Hoeb", "Ombar", "Tober", "Alav",
      "Dann", "Gereg", "Sony", "Terev", "Olvar", "Man", "Halan", "Yohn"
    },
    PYON_FN[] = {
      "Besa", "Linn", "Mina", "Abi", "Nana", "Dova", "Saba", "Kela", "Aryl",
      "Vina", "Nena", "Lanu", "Mai", "Nevi", "Mona", "Ambi", "Kayt", "Tesa",
    },
    PYON_LN[] = {
      "Foyle", "Orphy", "Samsun", "Ulga", "Yimon", "Timan", "Jo", "Yonson"
    },
    //
    //  Citizens have first and second names as standard.
    CITIZEN_MN[] = {
      "Sarles", "Mortan", "Daneel", "Trevize", "Tedrick", "Arnalt", "Bictor"
    },
    CITIZEN_FN[] = {
      "Becca", "Norema", "Catrin", "Xinia", "Max", "Sovia", "Unize", "Vonda"
    },
    CITIZEN_LN[] = {
      "Vasov", "Olvaw", "Mallo", "Palev", "Unterhaussen", "Valiz", "Ryod",
      "Obar", "Tiev", "Hanem", "Tsolo", "Matson", "Prestein", "Valter"
    },
    //
    //  Highborn always have family/house names, depending on their planet of
    //  origin, and may have additional titles.
    HIGHBORN_MN[] = {
      "Calib", "Vladmar", "Ledo", "Cado", "Alexander", "Xerxes", "Poul",
      "Altan"
    },
    HIGHBORN_FN[] = {
      "Meina", "Mnestra", "Aria", "Belise", "Ylande", "Vana", "Portia", "Vi",
      "Lysandre"
    },
    HIGHBORN_TN[] = {
      "Prime", "Secundus", "Tertius", "Minor",
      "Alpha", "Beta", "Gamma", "Major"
    },
    //
    //  TODO:  HOUSE NAMES ***MUST*** BE CUSTOMISED BY HOMEWORLD.
    HIGHBORN_HN[] = {
      "Rigel", "Ursa", "Alyph", "Rana", "Maia", "Fomalhaut", "Aldebaran",
      "Regulus", "Suhail", "Antares", "Paleides", "Algol", "Orion",
      "Deneb", "Ares",
    };
  
  
  public static String[] namesFor(Human actor) {
    final Batch <String> names = new Batch <String> ();
    final Background birth = actor.career().birth();
    final boolean female = actor.traits.female();
    
    if (birth == Backgrounds.BORN_NATIVE) {
      final String pick[] = female ? NATIVE_FN : NATIVE_MN;
      names.add((String) Rand.pickFrom(pick));
      if (Rand.yes()) {
        final String parents[] = female ? NATIVE_FN : NATIVE_MN;
        final String title = female ? "daughter of " : "son of ";
        names.add(title+Rand.pickFrom(parents));
      }
      else {
        final Batch <Trait> traits = Rand.yes() ?
          actor.traits.physique() : actor.traits.personality();
        final Trait desc = (Trait) Rand.pickFrom(traits);
        if (desc != null) names.add("the "+actor.traits.description(desc));
      }
    }
    
    if (birth == Backgrounds.BORN_PYON || birth == Backgrounds.BORN_DREGS) {
      final String pick[] = female ? PYON_FN : PYON_MN;
      names.add((String) Rand.pickFrom(pick));
      final String LN[] = PYON_LN;
      names.add((String) Rand.pickFrom(LN));
    }
    
    if (birth == Backgrounds.BORN_FREE || birth == Backgrounds.BORN_GELDER) {
      final String pick[] = female ? CITIZEN_FN : CITIZEN_MN;
      names.add((String) Rand.pickFrom(pick));
      names.add((String) Rand.pickFrom(CITIZEN_LN));
    }
    
    if (birth == Backgrounds.BORN_HIGH) {
      final String pick[] = female ? HIGHBORN_FN : HIGHBORN_MN;
      names.add((String) Rand.pickFrom(pick));
      names.add((String) Rand.pickFrom(HIGHBORN_HN));
      if (Rand.yes()) names.add((String) Rand.pickFrom(HIGHBORN_TN));
      names.add("of "+actor.career().homeworld());
    }
    
    return names.toArray(String.class);
  }
  
  
  
  /**  These methods return actors' voiceline responses to a variety of
    *  situations.
    */
  final public static String
    
    VOICE_RECRUIT  = "Ready for action.",
    VOICE_DISMISS  = "I quit!",
    
    VOICE_COMBAT   = "Watch out!",
    VOICE_EXPLORE  = "To boldy go...",
    VOICE_ASSIST   = "Hang in there!",
    VOICE_RETREAT  = "Fall back!",
    VOICE_REPAIR   = "I can fix that up.",
    
    VOICE_GREETING = "Hello!",
    VOICE_GOODBYE  = "Goodbye...",
    VOICE_RELAX    = "Time to relax.",
    VOICE_LEVEL    = "I'm getting better at this!",
    VOICE_ITEM     = "A fine piece of kit!",
    
    VOICE_AGREE    = "Sure.",
    VOICE_MAYBE    = "...Maybe.",
    VOICE_REFUSE   = "No way!";
  
  
  public static String response(Actor actor, Pledge pledgeMade) {
    return null;
  }
  
  
  //
  //  Base this off voice-keys instead.
  public static String phraseFor(Actor actor, Behaviour begun) {
    //
    //  This should be customised by vocation.
    
    if (begun instanceof Combat) {
      
    }
    
    return null;
  }
}



/*
final static String NAME_SUFFIXES[] = {
  "Alpha",
  "Beta",
  "Gamma",
  "Delta",
  "Epsilon",
  "Zeta",
  "Eta",
  "Theta",
  "Iota",
  "Kappa",
  "Lambda",
  "Mu",
  "Nu",
  "Xi",
  "Omicron",
  "Pi",
  "Rho",
  "Sigma",
  "Tau",
  "Upsilon",
  "Phi",
  "Chi",
  "Psy",
  "Omega"
};
//*/













