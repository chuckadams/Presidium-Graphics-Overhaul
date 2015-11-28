

package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.PowersPane.PowerButton;
import stratos.util.*;

import com.badlogic.gdx.math.Vector2;



public class SelectionOptions extends UIGroup implements UIConstants {
  
  
  final static int
    OB_SIZE   = 40,
    OB_MARGIN = 2;
  //  need health bar, title, status fx,
  
  final BaseUI BUI;
  final Target subject;
  
  
  public static SelectionOptions configOptions(
    Target subject, SelectionOptions info, HUD UI
  ) {
    if (info != null) return info;
    if (UI != BaseUI.current()) return null;
    return new SelectionOptions((BaseUI) UI, subject);
  }
  
  
  private SelectionOptions(BaseUI UI, Target subject) {
    super(UI);
    this.BUI = UI;
    this.subject = subject;
    this.relAlpha = 0;
    setup();
  }
  
  
  private class OptionButton extends Button {
    final Mission mission;
    
    OptionButton(BaseUI UI, String ID, ImageAsset img, String info, Mission m) {
      super(
        UI, ID, img.asTexture(), Mission.MISSION_ICON_LIT.asTexture(), info
      );
      this.mission = m;
    }
    
    protected void whenClicked() {
      final Base base = BUI.played();
      //
      //  If an existing mission matches this one, just select that instead.
      final Mission match = base.matchingMission(mission);
      if (match != null) {
        Selection.pushSelection(match, null);
        return;
      }
      //
      //  Otherwise, create a new mission for the target.
      base.tactics.addMission(mission);
      Selection.pushSelection(mission, null);
    }
  }
  
  
  private void setup() {
    final BaseUI BUI   = (BaseUI) UI;
    final Base   base  = BUI.played();
    final Actor  ruler = base.ruler();
    final List <UINode> options = new List <UINode> ();
    
    if (
      subject instanceof Actor ||
      subject instanceof Venue
    ) {
      options.add(new OptionButton(
        BUI, STRIKE_BUTTON_ID, Mission.STRIKE_ICON,
        "Destroy or capture subject",
        new MissionStrike(base, (Element) subject)
      ));
    }
    if (
      subject instanceof Actor ||
      subject instanceof Venue ||
      subject instanceof Item.Dropped
    ) {
      options.add(new OptionButton(
        BUI, SECURITY_BUTTON_ID, Mission.SECURITY_ICON,
        "Secure or protect subject",
        new MissionSecurity(base, (Element) subject)
      ));
    }
    if (
      Summons.canSummon(subject, base)
    ) {
      final MissionContact contactMission = new MissionContact(
        base, (Actor) subject
      );
      options.add(new OptionButton(
        BUI, CONTACT_BUTTON_ID, Mission.CONTACT_ICON,
        "Contact or negotiate with subject",
        contactMission
      ) {
        protected void whenClicked() {
          if (subject.base() == base) contactMission.setupAsSummons();
          super.whenClicked();
        }
        
        protected String info() {
          if (subject.base() != base) return super.info();
          return "Summon this citizen for discussion.";
        }
      });
    }
    if (
      subject instanceof Tile && Exploring.canExplore(base, subject)
    ) {
      options.add(new OptionButton(
        BUI, RECON_BUTTON_ID, Mission.RECON_ICON,
        "Explore an area or follow subject",
        new MissionRecon(base, (Tile) subject)
      ));
    }
    
    if (ruler != null) for (Power power : ruler.skills.knownPowers()) {
      if (power.icon == null || ! power.appliesTo(ruler, subject)) continue;
      
      final UIGroup option = new UIGroup(BUI);
      options.add(option);
      final Button button;
      button = new PowersPane.PowerButton(BUI, power, subject, option);
      button.alignAcross(0, 1);
      button.alignDown  (0, 1);
      button.attachTo(option);
      //
      //  We also attach a label listing the psy cost of the power:
      final int cost = power.costFor(ruler, subject);
      final BorderedLabel costLabel = new BorderedLabel(BUI);
      costLabel.attachTo(option);
      costLabel.alignHorizontal(0.5f, 0, 0);
      costLabel.alignTop(0, 0);
      costLabel.text.scale = SMALL_FONT_SIZE;
      costLabel.setMessage("Psy "+cost, false, 0.5f);
    }
    
    int sumWide = options.size() * (OB_SIZE + OB_MARGIN), across = 0;
    for (UINode option : options) {
      option.alignToArea(across - (sumWide / 2), 0, OB_SIZE, OB_SIZE);
      option.attachTo(this);
      across += OB_SIZE + OB_MARGIN;
    }
  }
  
  
  protected void updateState() {
    
    this.alignBottom(0, 0);
    this.alignHorizontal(0.5f, 0, 0);

    if (fadeout) {
      this.relAlpha -= DEFAULT_FADE_INC;
      if (relAlpha <= 0) detach();
    }
    else {
      this.relAlpha += DEFAULT_FADE_INC;
      if (relAlpha > 1) relAlpha = 1;
    }
    
    super.updateState();
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if (fadeout || relAlpha < 1) return null;
    return super.selectionAt(mousePos);
  }
}








