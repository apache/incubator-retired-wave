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

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileManagerImpl;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.client.common.util.CountdownLatch;
import org.waveprotocol.wave.client.common.util.DateUtils;
import org.waveprotocol.wave.client.concurrencycontrol.LiveChannelBinder;
import org.waveprotocol.wave.client.concurrencycontrol.MuxConnector;
import org.waveprotocol.wave.client.concurrencycontrol.WaveletOperationalizer;
import org.waveprotocol.wave.client.doodad.DoodadInstallers;
import org.waveprotocol.wave.client.doodad.attachment.AttachmentManagerProvider;
import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail;
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailWrapper;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler;
import org.waveprotocol.wave.client.doodad.title.TitleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.gadget.Gadget;
import org.waveprotocol.wave.client.render.ReductionBasedRenderer;
import org.waveprotocol.wave.client.render.RenderingRules;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.state.BlipReadStateMonitor;
import org.waveprotocol.wave.client.state.BlipReadStateMonitorImpl;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitorImpl;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.LazyContentDocument;
import org.waveprotocol.wave.client.wave.LocalSupplementedWave;
import org.waveprotocol.wave.client.wave.LocalSupplementedWaveImpl;
import org.waveprotocol.wave.client.wave.RegistriesHolder;
import org.waveprotocol.wave.client.wave.SimpleDiffDoc;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.client.wavepanel.impl.diff.DiffController;
import org.waveprotocol.wave.client.wavepanel.impl.reader.Reader;
import org.waveprotocol.wave.client.wavepanel.render.BlipPager;
import org.waveprotocol.wave.client.wavepanel.render.DocumentRegistries;
import org.waveprotocol.wave.client.wavepanel.render.FullDomRenderer;
import org.waveprotocol.wave.client.wavepanel.render.FullDomRenderer.DocRefRenderer;
import org.waveprotocol.wave.client.wavepanel.render.HtmlDomRenderer;
import org.waveprotocol.wave.client.wavepanel.render.InlineAnchorLiveRenderer;
import org.waveprotocol.wave.client.wavepanel.render.LiveConversationViewRenderer;
import org.waveprotocol.wave.client.wavepanel.render.PagingHandlerProxy;
import org.waveprotocol.wave.client.wavepanel.render.ReplyManager;
import org.waveprotocol.wave.client.wavepanel.render.ShallowBlipRenderer;
import org.waveprotocol.wave.client.wavepanel.render.UndercurrentShallowBlipRenderer;
import org.waveprotocol.wave.client.wavepanel.view.ModelIdMapper;
import org.waveprotocol.wave.client.wavepanel.view.ModelIdMapperImpl;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProviderImpl;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.DomRenderer;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ViewFactories;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ViewFactory;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.WavePanelResourceLoader;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl.LoggerContext;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelFactory;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelImpl;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListenerFactory;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.indexed.IndexedDocumentImpl;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.supplement.LiveSupplementedWaveImpl;
import org.waveprotocol.wave.model.supplement.ObservablePrimitiveSupplement;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.CollectiveScheduler;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Scheduler;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletFactory;

import java.util.Collections;
import java.util.Map;

/**
 * The second stage of client code.
 * <p>
 * This stage builds the wave model in memory, and also opens the channel to
 * make it live. Rendering code that operates on the model is also established
 * in this stage.
 *
 */
public interface StageTwo {

  /** @return the (live) conversations in the wave. */
  ObservableConversationView getConversations();

  /** @return the core wave. */
  ObservableWaveView getWave();

  /** @return the signed-in user's (live) supplementary data in the wave. */
  LocalSupplementedWave getSupplement();

  /** @return live blip read/unread information. */
  BlipReadStateMonitor getReadMonitor();

  /** @return the registry of document objects used for conversational blips. */
  WaveDocuments<? extends InteractiveDocument> getDocumentRegistry();

  /** @return the provider of view objects given model objects. */
  ModelAsViewProvider getModelAsViewProvider();

  /** @return the profile manager. */
  ProfileManager getProfileManager();

  /** @return the id generator. */
  IdGenerator getIdGenerator();

  /** @return the communication channel connector. */
  MuxConnector getConnector();

  /**
   * @return the blip content renderer, which needs to be flushed by anything
   *         requiring synchronous rendering.
   */
  BlipQueueRenderer getBlipQueue();

  /** @return controller of diff state. */
  DiffController getDiffController();

  /** @return the signed in user. */
  ParticipantId getSignedInUser();

  /** @return a unique string identifying this session. */
  String getSessionId();

  /** @return stage one. */
  StageOne getStageOne();

  /** @return Reader. */
  Reader getReader();

  /**
   * Default implementation of the stage two configuration. Each component is
   * defined by a factory method, any of which may be overridden in order to
   * stub out some dependencies. Circular dependencies are not detected.
   *
   */
  public static abstract class DefaultProvider extends AsyncHolder.Impl<StageTwo>
      implements StageTwo {
    // Asynchronously constructed and external dependencies
    protected final StageOne stageOne;
    private WaveViewData waveData;

    //
    // Synchronously constructed dependencies.
    //

    // Client stuff.

    private String sessionId;
    private ParticipantId signedInuser;
    private CollectiveScheduler rpcScheduler;

    // Wave stack.

    private IdGenerator idGenerator;
    private WaveDocuments<LazyContentDocument> documentRegistry;
    private WaveletOperationalizer wavelets;
    private WaveViewImpl<OpBasedWavelet> wave;
    private MuxConnector connector;

    // Model objects

    private ProfileManager profileManager;
    private ObservableConversationView conversations;
    private LocalSupplementedWave supplement;
    private BlipReadStateMonitor readMonitor;

    // State Monitors
    private ThreadReadStateMonitor threadReadStateMonitor;

    // Rendering objects.

    private ViewIdMapper viewIdMapper;
    private ShallowBlipRenderer blipDetailer;
    private DomRenderer renderer;
    private BlipQueueRenderer queueRenderer;
    private ModelAsViewProvider modelAsView;
    private DiffController diffController;
    private Reader reader;

    private final UnsavedDataListener unsavedDataListener;

    public DefaultProvider(StageOne stageOne, UnsavedDataListener unsavedDataListener) {
      this.stageOne = stageOne;
      this.unsavedDataListener = unsavedDataListener;
    }

    /**
     * Creates the second stage.
     */
    @Override
    protected void create(final Accessor<StageTwo> whenReady) {
      onStageInit();

      final CountdownLatch synchronizer = CountdownLatch.create(2, new Command() {
        @Override
        public void execute() {
          install();
          onStageLoaded();
          whenReady.use(DefaultProvider.this);
        }
      });

      fetchWave(new Accessor<WaveViewData>() {
        @Override
        public void use(WaveViewData x) {
          waveData = x;
          synchronizer.tick();
        }
      });

      // Defer everything else, to let the RPC go out.
      SchedulerInstance.getMediumPriorityTimer().scheduleDelayed(new Task() {
        @Override
        public void execute() {
          installStatics();
          synchronizer.tick();
        }
      }, 20);
    }

    /** Notifies this provider that the stage is about to be loaded. */
    protected void onStageInit() {
    }

    /** Notifies this provider that the stage has been loaded. */
    protected void onStageLoaded() {
    }

    @Override
    public final StageOne getStageOne() {
      return stageOne;
    }

    @Override
    public final String getSessionId() {
      return sessionId == null ? sessionId = createSessionId() : sessionId;
    }

    protected final ViewIdMapper getViewIdMapper() {
      return viewIdMapper == null ? viewIdMapper = createViewIdMapper() : viewIdMapper;
    }

    protected final ShallowBlipRenderer getBlipDetailer() {
      return blipDetailer == null ? blipDetailer = createBlipDetailer() : blipDetailer;
    }

    protected final DomRenderer getRenderer() {
      return renderer == null ? renderer = createRenderer() : renderer;
    }

    protected final ThreadReadStateMonitor getThreadReadStateMonitor() {
      return threadReadStateMonitor == null ? threadReadStateMonitor =
        createThreadReadStateMonitor() : threadReadStateMonitor;
    }

    @Override
    public final BlipQueueRenderer getBlipQueue() {
      return queueRenderer == null ? queueRenderer = createBlipQueueRenderer() : queueRenderer;
    }

    @Override
    public final ModelAsViewProvider getModelAsViewProvider() {
      return modelAsView == null ? modelAsView = createModelAsViewProvider() : modelAsView;
    }

    @Override
    public final ParticipantId getSignedInUser() {
      return signedInuser == null ? signedInuser = createSignedInUser() : signedInuser;
    }

    @Override
    public final IdGenerator getIdGenerator() {
      return idGenerator == null ? idGenerator = createIdGenerator() : idGenerator;
    }

    /** @return the scheduler to use for RPCs. */
    protected final CollectiveScheduler getRpcScheduler() {
      return rpcScheduler == null ? rpcScheduler = createRpcScheduler() : rpcScheduler;
    }

    @Override
    public final ProfileManager getProfileManager() {
      return profileManager == null ? profileManager = createProfileManager() : profileManager;
    }

    @Override
    public final MuxConnector getConnector() {
      return connector == null ? connector = createConnector() : connector;
    }

    @Override
    public final WaveViewImpl<OpBasedWavelet> getWave() {
      return wave == null ? wave = createWave() : wave;
    }

    protected final WaveletOperationalizer getWavelets() {
      return wavelets == null ? wavelets = createWavelets() : wavelets;
    }

    @Override
    public final ObservableConversationView getConversations() {
      return conversations == null ? conversations = createConversations() : conversations;
    }

    @Override
    public final LocalSupplementedWave getSupplement() {
      return supplement == null ? supplement = createSupplement() : supplement;
    }

    @Override
    public final BlipReadStateMonitor getReadMonitor() {
      return readMonitor == null ? readMonitor = createReadMonitor() : readMonitor;
    }

    @Override
    public final WaveDocuments<LazyContentDocument> getDocumentRegistry() {
      return documentRegistry == null
          ? documentRegistry = createDocumentRegistry() : documentRegistry;
    }

    protected final WaveViewData getWaveData() {
      Preconditions.checkState(waveData != null, "wave not ready");
      return waveData;
    }

    @Override
    public final DiffController getDiffController() {
      return diffController == null ? diffController = createDiffController() : diffController;
    }

    @Override
    public final Reader getReader() {
      return reader;
    }

    /** @return the id mangler for model objects. Subclasses may override. */
    protected ModelIdMapper createModelIdMapper() {
      return ModelIdMapperImpl.create(getConversations(), "UC");
    }

    /** @return the id mangler for view objects. Subclasses may override. */
    protected ViewIdMapper createViewIdMapper() {
      return new ViewIdMapper(createModelIdMapper());
    }

    /** @return the id of the signed-in user. Subclassses may override. */
    protected abstract ParticipantId createSignedInUser();

    /** @return the unique id for this client session. */
    protected abstract String createSessionId();

    /** @return the id generator for model object. Subclasses may override. */
    protected IdGenerator createIdGenerator() {
      final String seed = getSessionId();
      // Replace with session.
      return new IdGeneratorImpl(getSignedInUser().getDomain(), new Seed() {
        @Override
        public String get() {
          return seed;
        }
      });
    }

    /** @return the scheduler to use for RPCs. Subclasses may override. */
    protected CollectiveScheduler createRpcScheduler() {
      // Use a scheduler that runs closely-timed tasks at the same time.
      return new OptimalGroupingScheduler(SchedulerInstance.getLowPriorityTimer());
    }

    protected WaveletOperationalizer createWavelets() {
      return WaveletOperationalizer.create(getWaveData().getWaveId(), getSignedInUser());
    }

    protected WaveViewImpl<OpBasedWavelet> createWave() {
      WaveViewData snapshot = getWaveData();
      // The operationalizer makes the wavelets function via operation control.
      // The hookup with concurrency-control and remote operation streams occurs
      // later in createUpgrader().
      final WaveletOperationalizer operationalizer = getWavelets();
      WaveletFactory<OpBasedWavelet> waveletFactory = new WaveletFactory<OpBasedWavelet>() {
        @Override
        public OpBasedWavelet create(WaveId waveId, WaveletId id, ParticipantId creator) {
          long now = System.currentTimeMillis();
          ObservableWaveletData data = new WaveletDataImpl(id,
              creator,
              now,
              0L,
              HashedVersion.unsigned(0),
              now,
              waveId,
              getDocumentRegistry());
          return operationalizer.operationalize(data);
        }
      };
      WaveViewImpl<OpBasedWavelet> wave =
          WaveViewImpl.create(waveletFactory, snapshot.getWaveId(), getIdGenerator(),
              getSignedInUser(), WaveletConfigurator.ADD_CREATOR);

      // Populate the initial state.
      for (ObservableWaveletData waveletData : snapshot.getWavelets()) {
        wave.addWavelet(operationalizer.operationalize(waveletData));
      }
      return wave;
    }

    /** @return the conversations in the wave. Subclasses may override. */
    protected ObservableConversationView createConversations() {
      return WaveBasedConversationView.create(getWave(), getIdGenerator());
    }

    /** @return the user supplement of the wave. Subclasses may override. */
    protected LocalSupplementedWave createSupplement() {
      Wavelet udw = getWave().getUserData();
      if (udw == null) {
         udw = getWave().createUserData();
      }
      ObservablePrimitiveSupplement state = WaveletBasedSupplement.create(udw);
      ObservableSupplementedWave live = new LiveSupplementedWaveImpl(
          state, getWave(), getSignedInUser(), DefaultFollow.ALWAYS, getConversations());
      return LocalSupplementedWaveImpl.create(getWave(), live);
    }

    /** @return a supplement to the supplement, to get exact read/unread counts. */
    protected BlipReadStateMonitor createReadMonitor() {
      return BlipReadStateMonitorImpl.create(
          getWave().getWaveId(), getSupplement(), getConversations());
    }

    /** @return the registry of documents in the wave. Subclasses may override. */
    protected WaveDocuments<LazyContentDocument> createDocumentRegistry() {
      IndexedDocumentImpl.performValidation = false;

      DocumentFactory<?> dataDocFactory =
          ObservablePluggableMutableDocument.createFactory(createSchemas());
      DocumentFactory<LazyContentDocument> blipDocFactory =
          new DocumentFactory<LazyContentDocument>() {
            private final Registries registries = RegistriesHolder.get();

            @Override
            public LazyContentDocument create(
                WaveletId waveletId, String docId, DocInitialization content) {
              // TODO(piotrkaleta,hearnden): hook up real diff state.
              SimpleDiffDoc noDiff = SimpleDiffDoc.create(content, null);
              return LazyContentDocument.create(registries, noDiff);
            }
          };

      return WaveDocuments.create(blipDocFactory, dataDocFactory);
    }

    protected abstract SchemaProvider createSchemas();

    /** @return the RPC interface for wave communication. */
    protected abstract WaveViewService createWaveViewService();

    /** @return upgrader for activating stacklets. Subclasses may override. */
    protected MuxConnector createConnector() {
      LoggerBundle logger = LoggerBundle.NOP_IMPL;
      LoggerContext loggers = new LoggerContext(logger, logger, logger, logger);

      IdURIEncoderDecoder uriCodec = new IdURIEncoderDecoder(new ClientPercentEncoderDecoder());
      HashedVersionFactory hashFactory = new HashedVersionZeroFactoryImpl(uriCodec);

      Scheduler scheduler = new FuzzingBackOffScheduler.Builder(getRpcScheduler())
          .setInitialBackOffMs(ClientFlags.get().initialRpcBackoffMs())
          .setMaxBackOffMs(ClientFlags.get().maxRpcBackoffMs())
          .setRandomisationFactor(0.5)
          .build();

      ViewChannelFactory viewFactory = ViewChannelImpl.factory(createWaveViewService(), logger);
      UnsavedDataListenerFactory unsyncedListeners = new UnsavedDataListenerFactory() {

        private final UnsavedDataListener listener = unsavedDataListener;

        @Override
        public UnsavedDataListener create(WaveletId waveletId) {
          return listener;
        }

        @Override
        public void destroy(WaveletId waveletId) {
        }
      };

      WaveletId udwId = getIdGenerator().newUserDataWaveletId(getSignedInUser().getAddress());
      final IdFilter filter = IdFilter.of(Collections.singleton(udwId),
          Collections.singleton(IdConstants.CONVERSATION_WAVELET_PREFIX));

      WaveletDataImpl.Factory snapshotFactory =
          WaveletDataImpl.Factory.create(getDocumentRegistry());
      final OperationChannelMultiplexer mux =
          new OperationChannelMultiplexerImpl(getWave().getWaveId(),
              viewFactory,
              snapshotFactory,
              loggers,
              unsyncedListeners,
              scheduler,
              hashFactory);

      final WaveViewImpl<OpBasedWavelet> wave = getWave();

      return new MuxConnector() {
        @Override
        public void connect(Command onOpened) {
          LiveChannelBinder.openAndBind(getWavelets(),
              wave,
              getDocumentRegistry(),
              mux,
              filter,
              onOpened);
        }

        @Override
        public void close() {
          mux.close();
        }
      };
    }

    /** @return the manager of user identities. Subclasses may override. */
    protected ProfileManager createProfileManager() {
      return new ProfileManagerImpl();
    }

    /** @return the renderer of intrinsic blip state. Subclasses may override. */
    protected ShallowBlipRenderer createBlipDetailer() {
      return new UndercurrentShallowBlipRenderer(getProfileManager(), getSupplement(), DateUtils.getInstance());
    }

    /** @return the thread state monitor. Subclasses may override. */
    protected ThreadReadStateMonitor createThreadReadStateMonitor() {
      return ThreadReadStateMonitorImpl.create(getSupplement(), getConversations());
    }

    /** @return the renderer of intrinsic blip state. Subclasses may override. */
    protected BlipQueueRenderer createBlipQueueRenderer() {
      DomAsViewProvider domAsView = stageOne.getDomAsViewProvider();
      ReplyManager replyManager = new ReplyManager(getModelAsViewProvider());

      // Add all doodads here.
      DocumentRegistries doodads = installDoodads(DocumentRegistries.builder()) // \u2620
          .use(InlineAnchorLiveRenderer.installer(getViewIdMapper(), replyManager, domAsView))
          .use(Gadget.install(getProfileManager(), getSupplement(), getSignedInUser()))
          .build();

      LiveConversationViewRenderer live =
          LiveConversationViewRenderer.create(SchedulerInstance.getLowPriorityTimer(),
              getConversations(), getModelAsViewProvider(), getBlipDetailer(), replyManager,
              getThreadReadStateMonitor(), getProfileManager(), getSupplement());
      live.init();

      BlipPager pager = BlipPager.create(
          getDocumentRegistry(), doodads, domAsView, getModelAsViewProvider(), getBlipDetailer(),
          stageOne.getWavePanel().getGwtPanel());

      // Collect various components required for paging blips in/out.
      PagingHandlerProxy pagingHandler = PagingHandlerProxy.create( // \u2620
          // Enables and disables the document rendering, as well blip metadata.
          pager,
          // Registers and deregisters profile listeners for name changes.
          live);

      return BlipQueueRenderer.create(pagingHandler);
    }

    protected ViewFactory createViewFactories() {
      return ViewFactories.FIXED;
    }

    protected DomRenderer createRenderer() {
      final BlipQueueRenderer pager = getBlipQueue();
      DocRefRenderer docRenderer = new DocRefRenderer() {
        @Override
        public UiBuilder render(
            ConversationBlip blip, IdentityMap<ConversationThread, UiBuilder> replies) {
          // Documents are rendered blank, and filled in later when
          // they get paged in.
          pager.add(blip);
          return DocRefRenderer.EMPTY.render(blip, replies);
        }
      };

      RenderingRules<UiBuilder> rules = new FullDomRenderer(
          getBlipDetailer(), docRenderer, getProfileManager(),
          getViewIdMapper(), createViewFactories(), getThreadReadStateMonitor());
      return new HtmlDomRenderer(ReductionBasedRenderer.of(rules, getConversations()));
    }

    protected DiffController createDiffController() {
      return DiffController.create(
          getConversations(), getSupplement(), getDocumentRegistry(), getModelAsViewProvider());
    }

    /**
     * Fetches and builds the core wave state.
     *
     * @param whenReady command to execute when the wave is built
     */
    protected abstract void fetchWave(final Accessor<WaveViewData> whenReady);

    /**
     * Installs parts of stage two that have no dependencies.
     * <p>
     * Subclasses may override this to change the set of installed features.
     */
    protected void installStatics() {
      WavePanelResourceLoader.loadCss();
    }

    protected DocumentRegistries.Builder installDoodads(DocumentRegistries.Builder doodads) {
      return doodads.use(new DoodadInstallers.GlobalInstaller() {
        @Override
        public void install(Registries r) {
          DiffAnnotationHandler.register(r.getAnnotationHandlerRegistry(), r.getPaintRegistry());
          DiffDeleteRenderer.register(r.getElementHandlerRegistry());
          StyleAnnotationHandler.register(r);
          TitleAnnotationHandler.register(r);
          LinkAnnotationHandler.register(r, createLinkAttributeAugmenter());
          SelectionAnnotationHandler.register(r, getSessionId(), getProfileManager());
          ImageThumbnail.register(r.getElementHandlerRegistry(), AttachmentManagerProvider.get(),
              new ImageThumbnail.ThumbnailActionHandler() {

                @Override
                public boolean onClick(ImageThumbnailWrapper thumbnail) {
                  return false;
                }
              });
        }
      });
    }

    protected LinkAttributeAugmenter createLinkAttributeAugmenter() {
      return new LinkAttributeAugmenter() {
        @Override
        public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
            Map<String, String> current) {
          return current;
        }
      };
    }

    protected ModelAsViewProvider createModelAsViewProvider() {
      return new ModelAsViewProviderImpl(getViewIdMapper(), stageOne.getDomAsViewProvider());
    }

    /**
     * Installs parts of stage two that have dependencies.
     * <p>
     * This method is only called once all asynchronously loaded components of
     * stage two are ready.
     * <p>
     * Subclasses may override this to change the set of installed features.
     */
    protected void install() {
      // Install diff control before rendering, because logical diff state may
      // need to be adjusted due to arbitrary UI policies.
      getDiffController().install();

      // Install rendering capabilities, then render if necessary.
      stageOne.getDomAsViewProvider().setRenderer(getRenderer());
      ensureRendered();

      // Install eager UI features
      installFeatures();

      // Activate liveness.
      getConnector().connect(null);
    }

    /**
     * Ensures that the wave is rendered.
     * <p>
     * Subclasses may override (e.g., to use server-side rendering).
     */
    protected void ensureRendered() {
      // Default behaviour is to render the whole wave.
      Element e = getRenderer().render(getConversations());
      stageOne.getWavePanel().init(e);
    }

    /**
     * Installs the eager features of this stage.
     */
    protected void installFeatures() {
      // Eagerly install some features.
      reader = Reader.install(getSupplement(), stageOne.getFocusFrame(), getModelAsViewProvider(),
          getDocumentRegistry());
    }
  }
}
