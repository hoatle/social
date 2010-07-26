/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.composer;


import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.social.webui.space.UISpaceActivitiesDisplay;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormTextAreaInput;

/**
 *
 * @author    zun
 * @since 	  Apr 6, 2010
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/social/webui/composer/UIComposer.gtmpl",
  events = {
    @EventConfig(listeners = UIComposer.PostMessageActionListener.class),
    @EventConfig(listeners = UIComposer.ActivateActionListener.class)
  }
)
public class UIComposer extends UIForm {

  public enum PostContext {
    SPACE,
    PEOPLE
  }

  private PostContext postContext;
  private UIFormTextAreaInput messageInput;
  private UIActivityComposerContainer composerContainer;
  private UIActivityComposerManager activityComposerManager;
  private List<UIActivityComposer> activityComposers;
  /**
   * Constructor
   * @throws Exception
   */
  public UIComposer() throws Exception {
    //add textbox for inputting message
    messageInput = new UIFormTextAreaInput("composerInput", "composerInput", null);
    addUIFormInput(messageInput);

    //add composer container
    composerContainer = addChild(UIActivityComposerContainer.class, null, null);

    //load UIActivityComposerManager via PortalContainer
    activityComposerManager = (UIActivityComposerManager) PortalContainer.getInstance().getComponentInstanceOfType(UIActivityComposerManager.class);
    if(!activityComposerManager.isInitialized()){
      initActivityComposerManager();
    }

    activityComposers = activityComposerManager.getAllComposers();
  }

  private void initActivityComposerManager() throws Exception {
    UIExtensionManager uiExtensionManager = (UIExtensionManager) PortalContainer.getInstance().getComponentInstanceOfType(UIExtensionManager.class);
    final List<UIExtension> extensionList = uiExtensionManager.getUIExtensions(UIActivityComposer.class.getName());

    for (int i = 0, j = extensionList.size(); i < j; i++) {
      final UIExtension composerExtension = extensionList.get(i);
      if(composerExtension.getName().equals(UIActivityComposerManager.DEFAULT_ACTIVITY_COMPOSER)){
        UIActivityComposer uiDefaultComposer = (UIActivityComposer) uiExtensionManager.addUIExtension(composerExtension, null, composerContainer);
        composerContainer.removeChildById(uiDefaultComposer.getId());
        uiDefaultComposer.setRendered(false);
        activityComposerManager.setDefaultActivityComposer(uiDefaultComposer);
      } else{
        UIActivityComposer uiActivityComposer = (UIActivityComposer) uiExtensionManager.addUIExtension(composerExtension, null, composerContainer);
        uiActivityComposer.setRendered(false);
        activityComposerManager.registerActivityComposer(uiActivityComposer);
      }
    }

    activityComposerManager.setInitialized();
  }

  public void setActivityDisplay(UISpaceActivitiesDisplay uiDisplaySpaceActivities) {
    activityComposerManager.setActivityDisplay(uiDisplaySpaceActivities);
  }

  public void setDefaultActivityComposer(){
    activityComposerManager.setDefaultActivityComposer();
  }

  public UIActivityComposerContainer getComposerContainer() {
    return composerContainer;
  }

  public List<UIActivityComposer> getActivityComposers() {

    return activityComposers;
  }

  public String getMessage() {
    return getChild(UIFormTextAreaInput.class).getValue();
  }

  public PostContext getPostContext() {
    return postContext;
  }

  public void setPostContext(PostContext postContext) {
    this.postContext = postContext;
  }

  public String getActivateEvent(String activityComposerId){
    String activityComposerFormId;

    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    if (context instanceof PortletRequestContext)
    {
      activityComposerFormId = ((PortletRequestContext)context).getWindowId() + "#" + activityComposerId;
    } else{
      activityComposerFormId = activityComposerId;
    }

    StringBuilder b = new StringBuilder();
    b.append("javascript:eXo.webui.UIForm.submitForm('").append(activityComposerFormId).append("','");
    b.append("Activate").append("',true)");
    return b.toString();
  }

  public static class PostMessageActionListener extends EventListener<UIComposer> {
    @Override
    public void execute(Event<UIComposer> event) throws Exception {
      //get current context
      UIComposer uiComposer = event.getSource();
      final PostContext postContext = uiComposer.getPostContext();

      //get current activity composer
      UIActivityComposerManager activityComposerManager = (UIActivityComposerManager) PortalContainer.getInstance().getComponentInstanceOfType(UIActivityComposerManager.class);
      final UIActivityComposer activityComposer = activityComposerManager.getCurrentActivityComposer();

      //get posted message
      String message = uiComposer.getMessage().trim();
      String defaultInput = event.getRequestContext().getApplicationResourceBundle().getString(uiComposer.getId()+".Default_Input_Write_Something");
      if (message.equals(defaultInput)) {
        message = "";
      }

      //post activity via the current activity composer
      WebuiRequestContext requestContext = event.getRequestContext();
      activityComposer.postActivity(postContext, uiComposer, requestContext, message);
    }
  }

  public static class ActivateActionListener extends EventListener<UIActivityComposer> {
    @Override
    public void execute(Event<UIActivityComposer> event) throws Exception {
      final UIComposer composer = event.getSource().getAncestorOfType(UIComposer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(composer);
    }
  }
}