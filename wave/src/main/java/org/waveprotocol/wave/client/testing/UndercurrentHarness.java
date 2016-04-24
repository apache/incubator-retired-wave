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


package org.waveprotocol.wave.client.testing;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.StageOne;
import org.waveprotocol.wave.client.StageThree;
import org.waveprotocol.wave.client.StageTwo;
import org.waveprotocol.wave.client.StageZero;
import org.waveprotocol.wave.client.Stages;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.concurrencycontrol.MuxConnector;
import org.waveprotocol.wave.client.doodad.DoodadInstallers;
import org.waveprotocol.wave.client.doodad.attachment.AttachmentManagerProvider;
import org.waveprotocol.wave.client.doodad.attachment.testing.FakeAttachmentsManager;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.util.NullTypedSource;
import org.waveprotocol.wave.client.util.OverridingTypedSource;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.color.ComplexColorPicker;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.color.SampleCustomColorPicker;
import org.waveprotocol.wave.common.bootstrap.FlagConstants;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletFactory;

/**
 * Kicks off some initial actions for development purposes.
 *
 */
public class UndercurrentHarness implements EntryPoint {

  private UndercurrentHarness() {
  }

  private static boolean loaded;

  /**
   * Runs the harness script.
   */
  @Override
  public void onModuleLoad() {
    AttachmentManagerProvider.init(new FakeAttachmentsManager());
    if (loaded) {
      return;
    }
    loaded = true;

    final Timeline timeline = new Timeline();
    new Stages() {

      @Override
      protected AsyncHolder<StageZero> createStageZeroLoader() {
        return new StageZero.DefaultProvider() {

          @Override
          protected void onStageInit() {
            timeline.add("stage0_start");
          }

          @Override
          protected void onStageLoaded() {
            timeline.add("stage0_end");
          }

          @Override
          protected UncaughtExceptionHandler createUncaughtExceptionHandler() {
            return GWT.getUncaughtExceptionHandler();
          }
        };
      }

      @Override
      protected AsyncHolder<StageOne> createStageOneLoader(StageZero zero) {
        return new StageOne.DefaultProvider(zero) {
          @Override
          protected void onStageInit() {
            timeline.add("stage1_start");
          }

          @Override
          protected void onStageLoaded() {
            timeline.add("stage1_end");
          }

          @Override
          protected Element createWaveHolder() {
            return Document.get().getElementById("initialHtml");
          }
        };
      }

      @Override
      protected AsyncHolder<StageTwo> createStageTwoLoader(StageOne one) {
        return new StageTwo.DefaultProvider(one, null) {

          @Override
          protected void onStageInit() {
            timeline.add("stage2_start");
          }

          @Override
          protected void onStageLoaded() {
            timeline.add("stage2_end");
          }

          @Override
          protected void fetchWave(Accessor<WaveViewData> whenReady) {
            timeline.add("fakewave_start");
            WaveViewData fake = WaveFactory.create(getDocumentRegistry());
            timeline.add("fakewave_end");
            whenReady.use(fake);
          }

          @Override
          protected ParticipantId createSignedInUser() {
            return ParticipantId.ofUnsafe("nobody@example.com");
          }

          @Override
          protected String createSessionId() {
            return "session";
          }

          @Override
          protected MuxConnector createConnector() {
            return new MuxConnector() {
              @Override
              public void connect(Command whenOpened) {
                if (whenOpened != null) {
                  whenOpened.execute();
                }
              }

              @Override
              public void close() {
                // Ignore
              }
            };
          }

          @Override
          protected WaveViewService createWaveViewService() {
            // The vacuous MuxConnector should avoid the need for a
            // communication layer.
            throw new UnsupportedOperationException();
          }

          @Override
          protected SchemaProvider createSchemas() {
            return new ConversationSchemas();
          }
        };
      }

      @Override
      protected AsyncHolder<StageThree> createStageThreeLoader(StageTwo two) {
        ClientFlags.resetWithSourceForTesting(OverridingTypedSource.of(new NullTypedSource())
            .withBoolean(FlagConstants.ENABLE_UNDERCURRENT_EDITING, true)
            .build());

        // Only for test additional color pickers
        new SampleCustomColorPicker(ComplexColorPicker.getInstance());

        return new StageThree.DefaultProvider(two) {
          @Override
          protected void onStageInit() {
            timeline.add("stage3_start");
          }

          @Override
          protected void onStageLoaded() {
            timeline.add("stage3_end");
          }
        };
      }
    }.load(new Command() {
      @Override
      public void execute() {
        showInfo(timeline);
      }
    });
  }

  /**
   * Populates the info box. Continuously reports which element has browser
   * focus, and reports timing information for the stage loading.
   *
   * @param timeline timeline to report
   */
  private static void showInfo(Timeline timeline) {
    Element timeBox = Document.get().getElementById("timeline");
    timeline.dump(timeBox);

    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
      private final Element activeBox = Document.get().getElementById("active");

      @Override
      public boolean execute() {
        Element e = getActiveElement();
        String text = (e != null ? e.getTagName() + " id:" + e.getId() : "none");
        activeBox.setInnerText(text);
        return true;
      }

      private native Element getActiveElement() /*-{
        return $doc.activeElement;
      }-*/;
    }, 1000);
  }

  /**
   * Creates a sample wave with a conversation in it.
   */
  private final static class WaveFactory {

    /**
     * Creates a sample wave.
     *
     * @param docFactory factory/registry for documents in the wave
     * @return the wave state of the sample wave.
     */
    public static WaveViewDataImpl create(DocumentFactory<?> docFactory) {
      // Create a sample wave.
      WaveViewData sampleData = createSampleWave();

      // Now build one that has the same setup state as that required by
      // undercurrent (complex issue with the per-document output sinks).
      WaveViewDataImpl newData = WaveViewDataImpl.create(sampleData.getWaveId());
      WaveletDataImpl.Factory copier = WaveletDataImpl.Factory.create(docFactory);
      for (ReadableWaveletData src : sampleData.getWavelets()) {
        WaveletDataImpl copied = copier.create(src);
        for (ParticipantId p : src.getParticipants()) {
          copied.addParticipant(p);
        }
        copied.setVersion(copied.getVersion());
        copied.setHashedVersion(src.getHashedVersion());
        copied.setLastModifiedTime(src.getLastModifiedTime());
        newData.addWavelet(copied);
      }
      return newData;
    }

    /** @return a sample wave with a conversation in it. */
    private static WaveViewData createSampleWave() {
      final ParticipantId sampleAuthor = ParticipantId.ofUnsafe("nobody@example.com");
      IdGenerator gen = FakeIdGenerator.create();
      final WaveViewDataImpl waveData = WaveViewDataImpl.create(gen.newWaveId());
      final DocumentFactory<?> docFactory = BasicFactories.fakeDocumentFactory();
      final ObservableWaveletData.Factory<?> waveletDataFactory =
          new ObservableWaveletData.Factory<WaveletDataImpl>() {
            private final ObservableWaveletData.Factory<WaveletDataImpl> inner =
                WaveletDataImpl.Factory.create(docFactory);

            @Override
            public WaveletDataImpl create(ReadableWaveletData data) {
              WaveletDataImpl wavelet = inner.create(data);
              waveData.addWavelet(wavelet);
              return wavelet;
            }
          };
      WaveletFactory<OpBasedWavelet> waveletFactory = BasicFactories
            .opBasedWaveletFactoryBuilder()
            .with(waveletDataFactory)
            .with(sampleAuthor)
            .build();

      WaveViewImpl<?> wave = WaveViewImpl.create(
          waveletFactory, waveData.getWaveId(), gen, sampleAuthor, WaveletConfigurator.ADD_CREATOR);

      // Build a conversation in that wave.
      ConversationView v = WaveBasedConversationView.create(wave, gen);
      Conversation c = v.createRoot();
      ConversationThread root = c.getRootThread();
      sampleReply(root.appendBlip());
      write(root.appendBlip());
      write(root.appendBlip());
      write(root.appendBlip());

      return waveData;
    }

    private static void write(ConversationBlip blip) {
      org.waveprotocol.wave.model.document.Document d = blip.getContent();
      d.emptyElement(d.getDocumentElement());
      d.appendXml(XmlStringBuilder.createFromXmlString("<body><line></line>Hello World</body>"));
    }

    private static void sampleReply(ConversationBlip blip) {
      write(blip);
      ConversationThread thread = blip.addReplyThread(8);
      write(thread.appendBlip());
    }

    private static void biggerSampleReply(ConversationBlip blip) {
      write(blip);
      ConversationThread thread = blip.addReplyThread();
      sampleReply(thread.appendBlip());
      sampleReply(thread.appendBlip());
      write(thread.appendBlip());
    }
  }

  private static class Timeline {
    private final StringMap<Integer> events = CollectionUtils.createStringMap();
    private final Duration duration = new Duration();

    void add(String name) {
      events.put(name, duration.elapsedMillis());
    }

    void dump(Element timeBox) {
      final SafeHtmlBuilder timeHtml = new SafeHtmlBuilder();
      timeHtml.appendHtmlConstant("<table cellpadding='0' cellspacing='0'>");
      events.each(new ProcV<Integer>() {
        @Override
        public void apply(String key, Integer value) {
          timeHtml.appendHtmlConstant("<tr><td>");
          timeHtml.appendEscaped(key);
          timeHtml.appendHtmlConstant(":</td><td>");
          timeHtml.appendEscaped("" + value);
          timeHtml.appendHtmlConstant("</td></tr>");
        }
      });
      timeHtml.appendHtmlConstant("</table>");
      timeBox.setInnerHTML(timeHtml.toSafeHtml().asString());

    }
  }
}
