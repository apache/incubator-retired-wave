/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.waveprotocol.wave.client;

import com.google.gwt.core.client.GWT;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.doodad.selection.SelectionExtractor;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.wave.DocumentRegistry;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.impl.edit.Actions;
import org.waveprotocol.wave.client.wavepanel.impl.edit.ActionsImpl;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditController;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.impl.edit.KeepFocusInView;
import org.waveprotocol.wave.client.wavepanel.impl.edit.ParticipantController;
import org.waveprotocol.wave.client.wavepanel.impl.edit.i18n.ParticipantMessages;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.impl.indicator.ReplyIndicatorController;
import org.waveprotocol.wave.client.wavepanel.impl.menu.MenuController;
import org.waveprotocol.wave.client.wavepanel.impl.menu.i18n.MenuMessages;
import org.waveprotocol.wave.client.wavepanel.impl.title.WaveTitleHandler;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.ToolbarSwitcher;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.ViewToolbar;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * The third stage of client code.
 * <p>
 * This stage includes editing capabilities.
 *
 */
public interface StageThree {

  StageTwo getStageTwo();

  Actions getEditActions();

  EditToolbar getEditToolbar();

  EditSession getEditSession();

  ViewToolbar getViewToolbar();

  /**
   * Default implementation of the stage three configuration. Each component is
   * defined by a factory method, any of which may be overridden in order to
   * stub out some dependencies. Circular dependencies are not detected.
   *
   */
  public class DefaultProvider extends AsyncHolder.Impl<StageThree> implements StageThree {
    // External dependencies
    protected final StageTwo stageTwo;

    //
    // Synchronously constructed dependencies.
    //

    private Actions actions;
    private EditSession edit;
    private EditToolbar editToolbar;
    private ViewToolbar viewToolbar;

    public DefaultProvider(StageTwo stageTwo) {
      this.stageTwo = stageTwo;
    }

    /**
     * Creates the second stage.
     */
    @Override
    protected void create(final Accessor<StageThree> whenReady) {
      onStageInit();
      if (ClientFlags.get().enableUndercurrentEditing()) {
        install();
      }
      onStageLoaded();
      whenReady.use(this);
    }

    /** Notifies this provider that the stage is about to be loaded. */
    protected void onStageInit() {
    }

    /** Notifies this provider that the stage has been loaded. */
    protected void onStageLoaded() {
    }

    @Override
    public final StageTwo getStageTwo() {
      return stageTwo;
    }

    @Override
    public final Actions getEditActions() {
      return actions == null ? actions = createEditActions() : actions;
    }

    @Override
    public final EditSession getEditSession() {
      return edit == null ? edit = createEditSession() : edit;
    }

    @Override
    public final EditToolbar getEditToolbar() {
      return editToolbar == null ? editToolbar = createEditToolbar() : editToolbar;
    }

    @Override
    public final ViewToolbar getViewToolbar() {
      return viewToolbar == null ? viewToolbar = createViewToolbar() : viewToolbar;
    }

    protected Actions createEditActions() {
      StageOne stageOne = stageTwo.getStageOne();
      WavePanelImpl panel = stageOne.getWavePanel();
      FocusFramePresenter focus = stageOne.getFocusFrame();
      ModelAsViewProvider views = stageTwo.getModelAsViewProvider();
      WaveDocuments<? extends InteractiveDocument> docs = stageTwo.getDocumentRegistry();
      BlipQueueRenderer blipQueue = stageTwo.getBlipQueue();
      EditSession edit = getEditSession();
      return ActionsImpl.create(views, docs, blipQueue, focus, edit);
    }

    protected EditSession createEditSession() {
      StageOne stageOne = stageTwo.getStageOne();
      WavePanelImpl panel = stageOne.getWavePanel();
      FocusFramePresenter focus = stageOne.getFocusFrame();
      ModelAsViewProvider views = stageTwo.getModelAsViewProvider();
      DocumentRegistry<? extends InteractiveDocument> documents = stageTwo.getDocumentRegistry();
      String address = stageTwo.getSignedInUser().getAddress();
      TimerService clock = SchedulerInstance.getLowPriorityTimer();
      String sessionId = stageTwo.getSessionId();

      SelectionExtractor selectionExtractor = new SelectionExtractor(clock, address, sessionId);
      return EditSession.install(views, documents, selectionExtractor, focus, panel);
    }

    protected EditToolbar createEditToolbar() {
      return EditToolbar.create(getStageTwo().getSignedInUser(), stageTwo.getIdGenerator(),
          stageTwo.getWave().getWaveId());
    }

    protected ViewToolbar createViewToolbar() {
      ModelAsViewProvider views = stageTwo.getModelAsViewProvider();
      ConversationView wave = stageTwo.getConversations();
      return ViewToolbar.create(stageTwo.getStageOne().getFocusFrame(), views, wave,
          stageTwo.getReader());
    }

    protected String getLocalDomain() {
      return null;
    }

    /**
     * Installs parts of stage three that have dependencies.
     * <p>
     * This method is only called once all asynchronously loaded components of
     * stage three are ready.
     * <p>
     * Subclasses may override this to change the set of installed features.
     */
    protected void install() {
      EditorStaticDeps.setPopupProvider(PopupFactory.getProvider());
      EditorStaticDeps.setPopupChromeProvider(PopupChromeFactory.getProvider());

      // Eagerly install some features.
      WavePanelImpl panel = stageTwo.getStageOne().getWavePanel();
      FocusFramePresenter focus = stageTwo.getStageOne().getFocusFrame();
      ParticipantId user = stageTwo.getSignedInUser();
      ModelAsViewProvider models = stageTwo.getModelAsViewProvider();
      ProfileManager profiles = stageTwo.getProfileManager();

      MenuMessages menuMessages = GWT.create(MenuMessages.class);
      ParticipantMessages participantMessages = GWT.create(ParticipantMessages.class);

      Actions actions = getEditActions();
      EditSession edit = getEditSession();
      MenuController.install(actions, panel, menuMessages);
      ToolbarSwitcher.install(stageTwo.getStageOne().getWavePanel(), getEditSession(),
          getViewToolbar(), getEditToolbar());
      WaveTitleHandler.install(edit, models);
      ReplyIndicatorController.install(actions, edit, panel);
      EditController.install(focus, actions, panel);
      ParticipantController.install(panel, models, profiles, getLocalDomain(), user, participantMessages);
      KeepFocusInView.install(edit, panel);
      stageTwo.getDiffController().upgrade(edit);
    }
  }
}
