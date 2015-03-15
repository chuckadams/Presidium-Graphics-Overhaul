/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.graphics.common.Rendering;
import stratos.user.*;
import stratos.util.*;



/*  TUTORIAL TODO LIST:

*  Limit the powers available too- using upgrades or the household-panel?

*  Have tutorial-messages appear as message-items along the right-hand side.

*  Focus on the objectives that have an intuitive, emotional component- such as
   survival (strike/defend) and comfort (the economy.)  Making those mandatory
   isn't too bad.

*  Use a linear sequence with much more detail on specific, simple steps.  You
   want to cover-
     Building & Upgrades (space management.)
     Recruitment & Missions (indirect control, motivation.)
     Housing + Manufacture + Sales and Imports (having money.)

*  Limit the structure types available during the tutorial?
//*/



/*  BIG TODO LIST:

BUILDING AND UPGRADES-

  >>>>>>> DO THIS <<<<<<<
*  Explicit or improved placement for shield walls and arcologies.
     Fresh art for shield walls.  And give them out for free!
   
*  There needs to be a more precise 'reservation' system to allow for zoning,
   entrances and not-in-the-world-yet situations.

*  Upgrades need to be filled in and tested for:
     Stock Exchange
     Archives
     Runner Market
     Kommando Lodge
     Airfield
     Trooper Lodge (drilling)
     Cut out recruitment-extras for others.

*  Use service hatches to initiate heavier paving and utility-transmission?

*  Buildings need to have multiple levels.  (At least for the main/root-guild
   structures.)

*  Fresh art and level-depictions for all structures.

*  Ensure that spontaneous-buildings can 'migrate' to new sites if conditions
   change.  (Allow that for buildings in general?)

ENEMIES AND ENVIRONMENT-

  >>>>>>> DO THIS <<<<<<<
*  Need to restore animal nests and check their breeding behaviours.
   Need to ensure auto-distribution of predator and prey nests.

*  Need to write vermin behaviour (see Vermin class.)

*  Extra concept art for new creatures/vermin:
     Avrodil, Rem Leech, Mistaken, Desert Maw, Hirex Body.

CONTROL AND DIRECTION-

*  Introduce Call-to-Arms at the Trooper Lodge (with respect to a particular
   mission at a time.)

  >>>>>>> DO THIS <<<<<<<
*  Make Pledges more persistent and nuanced (see Proposal class.)
   Also, try to allow negotiation with buildings, in some sense.

*  Psy abilities need to be persistent.


INTERFACE AND DEBUGGING

*  Include an Advisors(?) and more detail Finances/Economy pane.

*  Include healthbars for psy-meter and target-descriptions?  Personal icon?

*  Try tracking actors only when they wander outside your field of view (or
   start that way.)  And include tracking options in the UI.

*  Read definitions, tutorial messages, etc. from .xml and allow for later
   translations.

*  Try to implement some global debugging-levels.
   
*  Delete all later saves if you restart a mission or revert to an earlier
   point in time.


CITIZEN BEHAVIOUR-

  >>>>>>> DO THIS <<<<<<<
*  Rework the migration system to that it's more gradual- colonists arrive from
   the homeworld on a scale of 1-2 months (10-20 days.)  Landing parties 'come
   ashore' every day or two, but those can't respond to job-demand as such.

*  Hide-and-seek needs to be more effective (both retreat and combat.)

*  Figure out entry-permissions for structures and guests, and/or sieges.

*  General internal clean-up of plan routines.  (Look in PlanUtils.)

  >>>>>>> DO THIS <<<<<<<
*  Proper evaluation of mood and memory-events.
//*/



public class DebugTutorial extends TutorialScenario {
  
  
  public static void main(String s[]) {
    PlayLoop.setupAndLoop(new DebugTutorial());
  }
  
  
  private DebugTutorial() {
    super("tutorial_quick");
  }
  
  
  public DebugTutorial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("tutorial_quick");
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = false;
    
    super.configureScenario(world, base, UI);
    UI.selection.pushSelection(base.ruler());
    
    I.say("\nLISTING BASE RELATIONS:");
    for (Base b : world().bases()) {
      I.say("  Relations for "+b+" with...");
      for (Base o : world().bases()) if (o != b) {
        I.say("    "+o+": "+b.relations.relationWith(o));
      }
    }
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
  }
}




