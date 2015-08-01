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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.scroll.ScrollBuilder;
import org.waveprotocol.wave.client.scroll.SmartScroller;
import org.waveprotocol.wave.client.wavepanel.event.FocusManager;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.impl.collapse.CollapseBuilder;
import org.waveprotocol.wave.client.wavepanel.impl.collapse.CollapsePresenter;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFrameBuilder;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.CssProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.FullStructure;
import org.waveprotocol.wave.client.wavepanel.view.dom.UpgradeableDomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.WavePanelResourceLoader;

/**
 * The first stage of Undercurrent code.
 * <p>
 * This exposes minimal features required for basic reading interactions.
 *
 * @see StageZero
 */
public interface StageOne {

  /** @return the wave panel. */
  WavePanelImpl getWavePanel();

  /** @return the focus feature. */
  FocusFramePresenter getFocusFrame();

  /** @return the collapse feature. */
  CollapsePresenter getCollapser();

  /** @return the provider of view objects from DOM elements. */
  UpgradeableDomAsViewProvider getDomAsViewProvider();

  /**
   * Default implementation of the stage one configuration. Each component is
   * defined by a factory method, any of which may be overridden in order to
   * stub out some dependencies. Circular dependencies are not detected.
   *
   */
  public static class DefaultProvider extends AsyncHolder.Impl<StageOne> implements StageOne {
    private WavePanelImpl wavePanel;
    private FocusFramePresenter focus;
    private CollapsePresenter collapser;
    private UpgradeableDomAsViewProvider views;

    public DefaultProvider(StageZero previous) {
      // Nothing in stage one depends on anything in stage zero currently, but
      // the dependency is wired up so that it is simple to add such
      // dependencies should they be necessary in the future.
    }

    @Override
    protected final void create(Accessor<StageOne> whenReady) {
      onStageInit();
      install();
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
    public final WavePanelImpl getWavePanel() {
      return wavePanel == null ? wavePanel = createWavePanel() : wavePanel;
    }

    @Override
    public final FocusFramePresenter getFocusFrame() {
      return focus == null ? focus = createFocusPresenter() : focus;
    }

    @Override
    public final CollapsePresenter getCollapser() {
      return collapser == null ? collapser = createCollapsePresenter() : collapser;
    }

    @Override
    public UpgradeableDomAsViewProvider getDomAsViewProvider() {
      return views == null ? views = createViewProvider() : views;
    }

    /** @return the container of the wave panel. Subclasses may override. */
    protected Element createWaveHolder() {
      Element panel = Document.get().getElementById("initialHtml");
      if (panel == null) {
        throw new RuntimeException("Page is malformed: no wave frame.");
      }
      return panel;
    }

    /**
     * @return the wave panel's (optional) logical parent. Subclasses may
     *         override.
     */
    protected LogicalPanel createWaveContainer() {
      return null;
    }

    /** @return the interpreter of DOM elements as semantic views. */
    protected UpgradeableDomAsViewProvider createViewProvider() {
      return new FullStructure(createCssProvider());
    }

    /** @return the wave panel. Subclasses may override. */
    protected WavePanelImpl createWavePanel() {
      return WavePanelImpl.create(
          getDomAsViewProvider(), createWaveHolder(), createWaveContainer());
    }

    /** @return the focus feature. Subclasses may override. */
    protected FocusFramePresenter createFocusPresenter() {
      SmartScroller<? super BlipView> scroller = ScrollBuilder.install(getWavePanel());
      return FocusFrameBuilder.createAndInstallIn(getWavePanel(), scroller);
    }

    /** @return the collapse feature. Subclasses may override. */
    protected CollapsePresenter createCollapsePresenter() {
      return CollapseBuilder.createAndInstallIn(getWavePanel());
    }
    
    /** @return the source of CSS rules to apply in views. */
    protected CssProvider createCssProvider() {
      return WavePanelResourceLoader.createCssProvider();
    }

    /**
     * Installs parts of stage one that have dependencies.
     * <p>
     * This method is only called once all asynchronously loaded components of
     * stage one are ready.
     * <p>
     * Subclasses may override this to change the set of installed features.
     */
    protected void install() {
      // Eagerly install some features.
      getFocusFrame();
      getCollapser();

      // Install wave panel into focus framework.
      FocusManager focus = FocusManager.getRoot();
      focus.add(getWavePanel());
      focus.select(getWavePanel());
    }
  }
}
