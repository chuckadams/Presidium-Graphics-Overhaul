/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import stratos.graphics.widgets.Text.Clickable;


//
//  Alright.  How do I introduce these more gradually?

//  *  Recruitment.
//  *  Buildings.
//  *  Research.
//  *  Mission Types.


public class TutorialScript {
  
  final static String
    SCRIPT_FILE = "media/Help/TutorialScript.xml";
  final static String
    EVENT_WELCOME         = "Welcome"                    ,
    EVENT_SECURITY_DONE   = "Security Objective Complete",
    EVENT_CONTACT_DONE    = "Contact Objective Complete" ,
    EVENT_ECONOMY_DONE    = "Economy Objective Complete" ,
    EVENT_CONGRATULATIONS = "Tutorial Complete!"         ;
  
  final TutorialScenario tutorial;
  final Table <String, Object> allTopics = new Table <String, Object> ();
  
  
  protected TutorialScript(TutorialScenario tutorial) {
    this.tutorial = tutorial;
    final XML xml = XML.load(SCRIPT_FILE);
    
    for (XML topicNode : xml.allChildrenMatching("topic")) {
      final String titleKey = topicNode.value("name");
      allTopics.put(titleKey, topicNode);
      
      I.say("\nFOUND BASE TOPIC: "+titleKey);
    }
  }
  
  
  protected DialoguePane messageFor(String title, final BaseUI UI) {
    
    final Object cached = allTopics.get(title);
    if (cached instanceof DialoguePane) return (DialoguePane) cached;
    if (cached == null) return null;
    
    final XML topicNode = (XML) cached;
    final String content = topicNode.child("content").content();
    final Batch <Clickable> links = new Batch <Clickable> ();
    
    for (XML linkNode : topicNode.allChildrenMatching("link")) {
      final String linkKey  = linkNode.value("name");
      final String linkName = linkNode.content();
      
      links.add(new Clickable() {
        
        public String fullName() {
          return linkName;
        }
        
        public void whenClicked() {
          final DialoguePane message = messageFor(linkKey, UI);
          if (message != null) UI.setInfoPanels(message, null);
          else I.say("\nNO TOPIC MATCHING: "+linkKey);
        }
      });
    }
    
    final DialoguePane panel = new DialoguePane(
      UI, null, title, content, null, links
    );
    allTopics.put(title, panel);
    return panel;
  }
  
}




/*
protected DialoguePane messageFor(
  String title, CommsPane comms, boolean useCache
) {
  if (useCache && comms.hasMessage(title)) return comms.messageWith(title);
  
  if (title.equals(EVENT_WELCOME)) {
    return comms.addMessage(
      tutorial, EVENT_WELCOME, null,
      "Hello, and welcome to Stratos: The Sci-Fi Settlement Sim!  This "+
      "tutorial will walk you through the essentials of setting up a "+
      "settlement, exploring the surrounds, dealing with potential threats, "+
      "and establishing a sound economy.",
      linkFor("Proceed", TOPIC_OBJECTIVES)
    );
  }
  
  if (title.equals(TOPIC_OBJECTIVES)) {
    return comms.addMessage(
      tutorial, TOPIC_OBJECTIVES, null,
      "Stratos is a game where you have three basic objectives: firstly, "+
      "survival, secondly, prosperity, and thirdly, a legacy.  To survive, "+
      "you must deal with local threats (a security concern).  To prosper, "+
      "you must establish housing, trade and industry (an economic "+
      "concern.)  Legacy will come later, once you complete these two "+
      "objectives.",
      linkFor("Tell me about the security objective.", TOPIC_SECURITY  ),
      //linkFor("Tell me about the contact objective." , TOPIC_CONTACT   ),
      linkFor("Tell me about the economic objective.", TOPIC_ECONOMY   ),
      linkFor("Wait a second.  How do I navigate?"   , TOPIC_NAVIGATION)
    );
  }
  
  if (title.equals(TOPIC_NAVIGATION)) {
    return comms.addMessage(
      tutorial, TOPIC_NAVIGATION, null,
      "To move your viewpoint, either click on a selectable object (such as "+
      "a structure, person, or point of terrain,) or click on the minimap. "+
      "You can also use the WASD keys.  Clicking also displays information "+
      "about most objects.\n"+
      "From left to right at the top of the screen, you can see the Game "+
      "Options button, Finance report, Personnel tab, Construction tab, "+
      "and the Communication tab (where you can find messages like this "+
      "one.",
      linkFor("Okay, that helps.", TOPIC_OBJECTIVES)
    );
  }
  
  if (title.equals(TOPIC_SECURITY)) {
    return comms.addMessage(
      tutorial, TOPIC_SECURITY, null,
      "Somewhere on this map, there is an ancient ruin, inhabited by "+
      "artilect guardians who may come to threaten your settlement.  You "+
      "will need to find this site and destroy it, by first exploring the "+
      "map to find the site, and then declaring a strike mission.",
      linkFor("How do I deal with enemies?"          , TOPIC_EXPLAIN_EXPAND),
      linkFor("How do I protect my subjects?"        , TOPIC_EXPLAIN_DEFEND),
      //linkFor("Tell me about the contact objective." , TOPIC_CONTACT       ),
      linkFor("Tell me about the economic objective.", TOPIC_ECONOMY       )
    );
  }
  
  if (title.equals(TOPIC_EXPLAIN_EXPAND)) {
    return comms.addMessage(
      tutorial, TOPIC_EXPLAIN_EXPAND, null,
      "To deal with threats to your base, you will need to find them first. "+
      "Click on a piece of hidden terrain, then on the green icon, to begin "+
      "a Reconnaissance mission.  Leave it public for now, and increase the "+
      "payment to attract applicants.\n"+
      "Once you find a threat, select it, and then click on the red icon to "+
      "begin a Strike mission.  (You may wish to build a Trooper Lodge or "+
      "two, just to get numbers on your side.)  With luck, your soldiers "+
      "will polish off the interlopers in short order.",
      linkFor("How do I protect my subjects?"     , TOPIC_EXPLAIN_DEFEND),
      linkFor("Go back to the security objective.", TOPIC_SECURITY      )
    );
  }
  
  if (title.equals(TOPIC_EXPLAIN_DEFEND)) {
    return comms.addMessage(
      tutorial, TOPIC_EXPLAIN_DEFEND, null,
      "If you fear attacks on vulnerable portions of your settlement, you "+
      "can also declare temporary Security missions (the orange icon) for "+
      "up to 48 hours on selected persons or structures.  In the case of "+
      "buildings, repairs will be carried out by qualified applicants, "+
      "while unconscious citizens in dangerous areas can be recovered and "+
      "treated using the same method.",
      linkFor("How do I deal with enemies?"       , TOPIC_EXPLAIN_EXPAND),
      linkFor("Go back to the security objective.", TOPIC_SECURITY      )
    );
  }
  
  /*
  if (title.equals(TOPIC_CONTACT)) {
    return comms.addMessage(
      tutorial, TOPIC_CONTACT, null,
      "Somewhere on this map, there is a camp of native tribal humans, who "+
      "may present either a threat or an asset, depending on how they are "+
      "handled.  Either declare a contact mission to bring them within the "+
      "fold of your settlement, or drive them out with a strike team.",
      linkFor("How do I make first contact?"         , TOPIC_EXPLAIN_CONTACT),
      linkFor("What are the outcomes of contact?"    , TOPIC_EXPLAIN_DIPLO  ),
      linkFor("Tell me about the security objective.", TOPIC_SECURITY       ),
      linkFor("Tell me about the economic objective.", TOPIC_ECONOMY        )
    );
  }
  
  if (title.equals(TOPIC_EXPLAIN_CONTACT)) {
    return comms.addMessage(
      tutorial, TOPIC_EXPLAIN_CONTACT, null,
      "Once you have explored the environs of your base and located the "+
      "native camp, you have a couple of options for handling them.  Here "+
      "we cover the diplomatic option- selecting the camp and declaring a "+
      "Contact mission (click the blue icon.)\n"+
      "Before you determine the payment offered, you may wish to change the "+
      "mission type from public to 'screened' or 'covert'- diplomacy is a "+
      "delicate matter, and you may want some finer control over who "+
      "applies for the job.",
      linkFor("What are the outcomes of contact?", TOPIC_EXPLAIN_DIPLO),
      linkFor("Go back to the contact objective.", TOPIC_CONTACT      )
    );
  }
  
  if (title.equals(TOPIC_EXPLAIN_DIPLO)) {
    return comms.addMessage(
      tutorial, TOPIC_EXPLAIN_DIPLO, null,
      "Contact objectives can be changed, to either 'demand submission', "+
      "'request audience' or 'offer friendship'.  The first option will "+
      "simply intimidate the native(s) into joining your base, which is "+
      "the surest method in the short term but risks later defection and "+
      "malcontent.  The second will request their presence at the Bastion, "+
      "where they can be interviewed, and the third merely offers gifts and "+
      "good will.  Given time, sufficiently respected natives might join "+
      "your base spontaneously.\n"+
      "Any of your own subjects may be summoned for interview at will, so "+
      "they can be recruited for secret missions, or just asked for advice "+
      "and opinions.",
      linkFor("How do I make first contact?"     , TOPIC_EXPLAIN_CONTACT),
      linkFor("Go back to the contact objective.", TOPIC_CONTACT        )
    );
  }
  //*/
  /*
  if (title.equals(TOPIC_ECONOMY)) {
    return comms.addMessage(
      tutorial, TOPIC_ECONOMY, null,
      "In order for your settlement to provide a viable power base for "+
      "later expansion, you will need to establish exports and gather tax "+
      "from your citizens.  Try to get your citizens' average housing level "+
      "up to pyon grade or better.",
      linkFor("How do I get money and supplies?"     , TOPIC_EXPLAIN_SUPPLY),
      linkFor("How do I improve my housing?"         , TOPIC_EXPLAIN_INDUST),
      linkFor("Tell me about the security objective.", TOPIC_SECURITY      )
      //linkFor("Tell me about the contact objective." , TOPIC_CONTACT       )
    );
  }
  
  if (title.equals(TOPIC_EXPLAIN_SUPPLY)) {
    return comms.addMessage(
      tutorial, TOPIC_EXPLAIN_SUPPLY, null,
      "To secure a solid early cash flow, a fair strategy is to build a "+
      "Supply Depot, followed by either a Nursery (on fertile terrain) or "+
      "an Excavation Site (on rocky areas.)  The Supply Depot will allow "+
      "you to reserve goods for export offworld, and perhaps import a few "+
      "necessities.\n"+
      "The other structures, meanwhile, take advantage of local resources "+
      "to produce food and minerals for sale or local consumption.",
      linkFor("How do I improve my housing?"      , TOPIC_EXPLAIN_INDUST),
      linkFor("Go back to the economic objective.", TOPIC_ECONOMY       )
    );
  }
  
  if (title.equals(TOPIC_EXPLAIN_INDUST)) {
    return comms.addMessage(
      tutorial, TOPIC_EXPLAIN_INDUST, null,
      "In order to improve your housing- and export finished goods- you may "+
      "wish to place an Engineer Station or Fabricator to take metal ore "+
      "or carbons and process them into parts and plastics, along with "+
      "weapons or outfits for your citizens.  A skilled worker with enough "+
      "raw materials can make perhaps 5 units per day, but you may wish to "+
      "install upgrades (like Assembly Line or Polymer Loom) to speed the "+
      "process.\n"+
      "Holdings should display the goods and services they require to "+
      "upgrade in their status description, and will do so after being "+
      "satisfied for a day or so.",
      linkFor("How do I get money and supplies?"  , TOPIC_EXPLAIN_SUPPLY),
      linkFor("Go back to the economic objective.", TOPIC_ECONOMY       )
    );
  }
  
  //  Mention artificer and fabricator.  Upgrades to increase production.
  //  Mention supply depot and botanical station, for trade and food.
  
  if (title.equals(EVENT_SECURITY_DONE)) {
    return comms.addMessage(
      tutorial, EVENT_SECURITY_DONE, null,
      "Congratulations!  Each of the ruins has now been destroyed."
    );
  }
  
  /*
  if (title.equals(EVENT_CONTACT_DONE)) {
    return comms.addMessage(
      tutorial, EVENT_CONTACT_DONE, null,
      "Congratulations!  The native camps no longer pose a threat to your "+
      "base."
    );
  }
  //*/
  
  /*
  if (title.equals(EVENT_ECONOMY_DONE)) {
    return comms.addMessage(
      tutorial, EVENT_ECONOMY_DONE, null,
      "Congratulations!  Your future economic prospects are bright."
    );
  }
  
  if (title.equals(EVENT_CONGRATULATIONS)) {
    return comms.addMessage(
      tutorial, EVENT_CONGRATULATIONS, null,
      "This tutorial is now complete.  Feel free to explore the mechanics "+
      "of construction and mission-settings some more, but when you are "+
      "ready, select 'Complete Tutorial' to advance to a new mission.\n\n"+
      "Complete Tutorial <UNDER CONSTRUCTION!>"
    );
  }
  
  return null;
}
//*/
