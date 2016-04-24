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

package org.waveprotocol.wave.model.account;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdSerializer;

import java.util.Set;

/**
 * Indexability, backed by data stored in a document.
 *
 */
public class DocumentBasedIndexability implements ObservableMutableIndexability,
    ObservableBasicMap.Listener<ParticipantId, IndexDecision> {
  public static final String INDEX_TAG = "index";
  public static final String ADDRESS_ATTR = "address";
  public static final String VALUE_ATTR = "i";

  private final ObservableBasicMap<ParticipantId, IndexDecision> data;

  private final CopyOnWriteSet<ObservableIndexability.Listener> listeners =
    CopyOnWriteSet.create();

  DocumentBasedIndexability(ObservableBasicMap<ParticipantId, IndexDecision> map) {
    data = map;
  }

  /**
   * Creates a Permissions view on top of the document.
   */
  public static DocumentBasedIndexability create(final DocEventRouter doc) {
    DocumentBasedBasicMap<Doc.E, ParticipantId, IndexDecision> map =
      DocumentBasedBasicMap.create(doc,
        doc.getDocument().getDocumentElement(),
        ParticipantIdSerializer.INSTANCE,
        new Serializer.EnumSerializer<IndexDecision>(IndexDecision.class),
        INDEX_TAG, ADDRESS_ATTR, VALUE_ATTR);
    DocumentBasedIndexability indexability = new DocumentBasedIndexability(map);
    map.addListener(indexability);
    return indexability;
  }

  @Override
  public void setIndexability(ParticipantId participant, IndexDecision indexability) {
    Preconditions.checkNotNull(participant, "Null participant");
    IndexDecision current = getIndexability(participant);
    if (indexability == null) {
      if (current != null) {
        data.remove(participant);
      }
    } else {
      data.put(participant, indexability);
    }
  }

  @Override
  public IndexDecision getIndexability(ParticipantId participant) {
    return data.get(participant);
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public void onEntrySet(ParticipantId key, IndexDecision oldValue, IndexDecision newValue) {
    for (ObservableIndexability.Listener l : listeners) {
      l.onChanged(key, newValue);
    }
  }

  @Override
  public Set<ParticipantId> getIndexDecisions() {
    return data.keySet();
  }
}
