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

package org.waveprotocol.wave.model.conversation.testing;

import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.Collection;

/**
 * A fake conversation view. The view is fully functioning but not attached to
 * any communication channels.
 *
 * @author anorth@google.com (Alex North)
 */
public final class FakeConversationView implements ObservableConversationView {

  private final static SchemaProvider DEFAULT_SCHEMAS = new ConversationSchemas();

  public final static class Builder {
    private SchemaProvider schemas;
    private IdGenerator idGenerator;
    private WaveId waveId;
    private ParticipantId viewer;
    private DocumentFactory<?> docFactory;

    private Builder() {
    }

    public Builder with(DocumentFactory<?> docFactory) {
      this.docFactory = docFactory;
      return this;
    }

    public Builder with(SchemaProvider schemas) {
      this.schemas = schemas;
      return this;
    }

    public Builder with(IdGenerator idGenerator) {
      this.idGenerator = idGenerator;
      return this;
    }

    public Builder with(WaveId wid) {
      this.waveId = wid;
      return this;
    }

    public Builder with(ParticipantId viewer) {
      this.viewer = viewer;
      return this;
    }

    public FakeConversationView build() {
      if (schemas == null) {
        schemas = DEFAULT_SCHEMAS;
      }
      if (idGenerator == null) {
        idGenerator = FakeIdGenerator.create();
      }
      if (waveId == null) {
        waveId = idGenerator.newWaveId();
      }

      FakeWaveView waveView = FakeWaveView.builder(schemas) // \u2620
          .with(docFactory) // \u2620
          .with(idGenerator) // \u2620
          .with(waveId) // \u2620
          .with(viewer) // \u2620
          .build();

      return new FakeConversationView(WaveBasedConversationView.create(waveView, idGenerator));
    }
  }

  /** Creates a new conversation view builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** The backing conversation view. */
  private final WaveBasedConversationView view;

  private FakeConversationView(WaveBasedConversationView view) {
    this.view = view;
  }

  @Override
  public String getId() {
    return view.getId();
  }

  @Override
  public WaveletBasedConversation createConversation() {
    return view.createConversation();
  }

  @Override
  public WaveletBasedConversation createRoot() {
    return view.createRoot();
  }

  @Override
  public WaveletBasedConversation getConversation(String conversationId) {
    return view.getConversation(conversationId);
  }

  @Override
  public Collection<? extends WaveletBasedConversation> getConversations() {
    return view.getConversations();
  }

  @Override
  public WaveletBasedConversation getRoot() {
    return view.getRoot();
  }

  @Override
  public void addListener(Listener listener) {
    view.addListener(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    view.removeListener(listener);
  }

  public ObservableWaveView getWaveView() {
    return view.getWaveView();
  }
}
