/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.wip;
import stratos.game.actors.Choice;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;



public abstract class School extends Venue {
  
  
  
  protected School(Blueprint blueprint, Base base) {
    super(blueprint, base);
  }
  
  
  public School(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  //
  //  All Schools have a few basic functions in common.  They are, first of all,
  //  places of basic education and spiritual instruction, in a manner similar
  //  to the monasteries of the orient and medieval period.
  
  //  Secondly, they are places of psyonic training for advanced adepts, and as
  //  such can be called upon to supplement military or diplomatic missions.
  
  
  protected void addServices(Choice choice, Actor forActor) {
    
    //
    //  TODO:  Offer training under supervision in the skill and Techniques
    //  appropriate to the school of thought in question.
    
    //  Basic training is available to the public, but advanced training only
    //  to members of the school OR to the Sovereign.
    
    //  Premonition-boosts for Logicians, plus general science research.
    //  Artilect taming and construction for Tek Priestesses.
    //  Charity and thought-sync sessions for the Collective.
    //  Ship-construction and accompaniment for Navigators.
    //  Animal-taming and body-modification for Jil Baru.
    //  Body-armour upgrades and nanite-industry for KOTSF.
    
    super.addServices(choice, forActor);
  }
  
  
  
}




    //  TODO:  Move this to the Pseer School.
    /*
    //
    //  TODO:  Consider replacing this a boost to Shields generation?  Make
    //  Fusion confinement dependant on that?
    QUALIA_WAVEFORM_INTERFACE = new Upgrade(
      "Qualia Waveform Interface",
      "Allows reactor output to contribute slightly towards regeneration of "+
      "psi points and range of psyon abilities.",
      250,
      null, 1, FEEDBACK_MONITORS,
      Reactor.class, ALL_UPGRADES
    ),
    //*/








