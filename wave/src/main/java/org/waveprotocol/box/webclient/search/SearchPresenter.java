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

package org.waveprotocol.box.webclient.search;

import com.google.gwt.core.client.GWT;
import org.waveprotocol.box.webclient.search.Search.State;
import org.waveprotocol.box.webclient.search.i18n.SearchPresenterMessages;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.widget.toolbar.GroupingToolbar;
import org.waveprotocol.wave.client.widget.toolbar.ToolbarButtonViewBuilder;
import org.waveprotocol.wave.client.widget.toolbar.ToolbarView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Presents a search model into a search view.
 * <p>
 * This class invokes rendering, and controls the lifecycle of digest views. It
 * also handles all UI gesture events sourced from views in the search panel.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SearchPresenter
    implements Search.Listener, SearchPanelView.Listener, SearchView.Listener, ProfileListener {

  /**
   * Handles wave actions.
   */
  public interface WaveActionHandler {
    /** Handles the wave creation action. */
    void onCreateWave();

    /** Handles a wave selection action. */
    void onWaveSelected(WaveId id);
  }

  private static final SearchPresenterMessages messages = GWT.create(SearchPresenterMessages.class);

  /** How often to repeat the search query. */
  private final static int POLLING_INTERVAL_MS = 15000; // 15s
  private final static String DEFAULT_SEARCH = "in:inbox";
  private final static int DEFAULT_PAGE_SIZE = 20;

  // External references
  private final TimerService scheduler;
  private final Search search;
  private final SearchPanelView searchUi;
  private final WaveActionHandler actionHandler;

  // Internal state
  private final IdentityMap<DigestView, Digest> digestUis = CollectionUtils.createIdentityMap();
  private final IncrementalTask searchUpdater = new IncrementalTask() {
    @Override
    public boolean execute() {
      doSearch();
      return true;
    }
  };

  private final Task renderer = new Task() {
    @Override
    public void execute() {
      if (search.getState() == State.READY) {
        render();
      } else {
        // Try again later.
        scheduler.schedule(this);
      }
    }
  };

  /** Current search query. */
  private String queryText = DEFAULT_SEARCH;
  /** Number of results to query for. */
  private int querySize = DEFAULT_PAGE_SIZE;
  /** Current selected digest. */
  private DigestView selected;

  /** The dispatcher of profiles events. */
  SourcesEvents<ProfileListener> profiles;
  private boolean isRenderingInProgress = false;

  SearchPresenter(TimerService scheduler, Search search, SearchPanelView searchUi,
      WaveActionHandler actionHandler, SourcesEvents<ProfileListener> profiles) {
    this.search = search;
    this.searchUi = searchUi;
    this.scheduler = scheduler;
    this.actionHandler = actionHandler;
    this.profiles = profiles;
  }

  /**
   * Creates a search presenter.
   *
   * @param model model to present
   * @param view view to render into
   * @param actionHandler handler for actions
   * @param profileEventsDispatcher the dispatcher of profile events.
   */
  public static SearchPresenter create(
      Search model, SearchPanelView view, WaveActionHandler actionHandler,
      SourcesEvents<ProfileListener> profileEventsDispatcher) {
    SearchPresenter presenter = new SearchPresenter(
        SchedulerInstance.getHighPriorityTimer(), model, view, actionHandler,
        profileEventsDispatcher);
    presenter.init();
    return presenter;
  }

  /**
   * Performs initial presentation, and attaches listeners to live objects.
   */
  private void init() {
    initToolbarMenu();
    initSearchBox();
    render();
    search.addListener(this);
    profiles.addListener(this);
    searchUi.init(this);
    searchUi.getSearch().init(this);

    // Fire a polling search.
    scheduler.scheduleRepeating(searchUpdater, 0, POLLING_INTERVAL_MS);
  }

  /**
   * Releases resources and detaches listeners.
   */
  public void destroy() {
    scheduler.cancel(searchUpdater);
    scheduler.cancel(renderer);
    searchUi.getSearch().reset();
    searchUi.reset();
    search.removeListener(this);
    profiles.removeListener(this);
  }

  /**
   * Adds custom buttons to the toolbar.
   */
  private void initToolbarMenu() {
    GroupingToolbar.View toolbarUi = searchUi.getToolbar();
    ToolbarView group = toolbarUi.addGroup();
    new ToolbarButtonViewBuilder().setText(messages.newWave()).applyTo(
        group.addClickButton(), new ToolbarClickButton.Listener() {
          @Override
          public void onClicked() {
            actionHandler.onCreateWave();

            // HACK(hearnden): To mimic live search, fire a search poll
            // reasonably soon (500ms) after creating a wave. This will be unnecessary
            // with a real live search implementation. The delay is to give
            // enough time for the wave state to propagate to the server.
            int delay = 500;
            scheduler.scheduleRepeating(searchUpdater, delay, POLLING_INTERVAL_MS);
          }
        });
    // Fake group with empty button - to force the separator be displayed.
    group = toolbarUi.addGroup();
    new ToolbarButtonViewBuilder().setText("").applyTo(group.addClickButton(), null);
  }

  /**
   * Initializes the search box.
   */
  private void initSearchBox() {
    searchUi.getSearch().setQuery(queryText);
  }

  /**
   * Executes the current search.
   */
  private void doSearch() {
    search.find(queryText, querySize);
  }

  /**
   * Renders the current state of the search result into the panel.
   */
  private void render() {
    renderTitle();
    renderDigests();
    renderShowMore();
  }

  /**
   * Renders the paging information into the title bar.
   */
  private void renderTitle() {
    int resultEnd = querySize;
    String totalStr;
    if (search.getTotal() != Search.UNKNOWN_SIZE) {
      resultEnd = Math.min(resultEnd, search.getTotal());
      totalStr = messages.of(search.getTotal());
    } else {
      totalStr = messages.ofUnknown();
    }
    searchUi.setTitleText(queryText + " (0-" + resultEnd + " of " + totalStr + ")");
  }

  private void renderDigests() {
    isRenderingInProgress = true;
    // Preserve selection on re-rendering.
    WaveId toSelect = selected != null ? digestUis.get(selected).getWaveId() : null;
    searchUi.clearDigests();
    digestUis.clear();
    setSelected(null);
    for (int i = 0, size = search.getMinimumTotal(); i < size; i++) {
      Digest digest = search.getDigest(i);
      if (digest == null) {
        continue;
      }
      DigestView digestUi = searchUi.insertBefore(null, digest);
      digestUis.put(digestUi, digest);
      if (digest.getWaveId().equals(toSelect)) {
        setSelected(digestUi);
      }
    }
    isRenderingInProgress = false;
  }

  private void renderShowMore() {
    searchUi.setShowMoreVisible(
        search.getTotal() == Search.UNKNOWN_SIZE || querySize < search.getTotal());
  }

  //
  // UI gesture events.
  //

  private void setSelected(DigestView digestUi) {
    if (selected != null) {
      selected.deselect();
    }
    selected = digestUi;
    if (selected != null) {
      selected.select();
    }
  }

  /**
   * Invokes the wave-select action on the currently selected digest.
   */
  private void openSelected() {
    actionHandler.onWaveSelected(digestUis.get(selected).getWaveId());
  }

  @Override
  public void onClicked(DigestView digestUi) {
    setSelected(digestUi);
    openSelected();
  }

  @Override
  public void onQueryEntered() {
    queryText = searchUi.getSearch().getQuery();
    querySize = DEFAULT_PAGE_SIZE;
    searchUi.setTitleText(messages.searching());
    doSearch();
  }

  @Override
  public void onShowMoreClicked() {
    querySize += DEFAULT_PAGE_SIZE;
    doSearch();
  }

  //
  // Search events. For now, dumbly re-render the whole list.
  //

  @Override
  public void onStateChanged() {
    //
    // If the state switches to searching, then do nothing. A manual title-bar
    // update is performed in onQueryEntered(), and the title-bar should not be
    // updated when a polling search fires.
    //
    // If the state switches to ready, then just update the title. Do not
    // necessarily re-render, since that is only necessary if a change occurred,
    // which would have fired one of the other methods below.
    //
    if (search.getState() == State.READY) {
      renderTitle();
    }
  }

  @Override
  public void onDigestAdded(int index, Digest digest) {
    renderLater();
  }

  @Override
  public void onDigestRemoved(int index, Digest digest) {
    renderLater();
  }

  /**
   * Find the DigestView that contains a certain digest
   *
   * @param digest the digest the DigestView should contain.
   * @return the DigestView containing the digest. {@null} if the digest is
   *            not found.
   */
  private DigestView findDigestView(Digest digest) {
    DigestView digestUi = searchUi.getFirst();
    while(digestUi != null) {
      if (digestUis.get(digestUi).equals(digest)) {
        return digestUi;
      }
      digestUi = searchUi.getNext(digestUi);
    }
    return null;
  }

  /**
   * Insert a digest before amongst the currently shown digests
   *
   * @param insertRef the DigestView to insert the new digest before. The new digest
   *                    is inserted last if insertRef is {@null}.
   * @param digest the digest to insert.
   * @return the newly inserted DigestView.
   */
  private DigestView insertDigest(DigestView insertRef, Digest digest) {
    DigestView newDigestUi = null;
    if (insertRef != null) {
      newDigestUi = searchUi.insertBefore(insertRef, digest);
      digestUis.put(newDigestUi, digest);
    } else {
      insertRef = searchUi.getLast();
      newDigestUi = searchUi.insertAfter(insertRef, digest);
      digestUis.put(newDigestUi, digest);
    }
    return newDigestUi;
  }

  @Override
  public void onDigestReady(int index, Digest digest) {
    if (isRenderingInProgress) {
      return;
    }

    setSelected(null);
    DigestView digestToRemove = findDigestView(digest);
    if (digestToRemove == null) {
      return;
    }
    DigestView insertRef = searchUi.getNext(digestToRemove);
    digestToRemove.remove();
    DigestView newDigestUi = insertDigest(insertRef, digest);
    setSelected(newDigestUi);
  }

  @Override
  public void onTotalChanged(int total) {
    renderLater();
  }

  private void renderLater() {
    if (!scheduler.isScheduled(renderer)) {
      scheduler.schedule(renderer);
    }
  }

  @Override
  public void onProfileUpdated(Profile profile) {
    // NOTE: Search panel will be re-rendered once for every profile that comes
    // back to the client. If this causes an efficiency problem then have the
    // SearchPanelRenderer to be the profile listener, rather than
    // SearchPresenter, and make it stateful. Have it remember which digests
    // have used which profiles in their renderings.
    renderLater();
  }
}
