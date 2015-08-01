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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletIdSerializer;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A document based store for abuse data.
 *
 * Uses a DocumentBasedElementList, keeping elements as children of the root
 * element.
 *
 */
public final class DocumentBasedAbuseStore<N, E extends N> implements ObservableAbuseStore {

  /**
   * Converts WantedEvaluations to and from XML.
   */
  private static class ElementFactory<E> implements
      Factory<E, WantedEvaluation, WantedEvaluation> {

    @Override
    public WantedEvaluation adapt(DocumentEventRouter<? super E, E, ?> router,
        E element) {

      // We can do the conversion once at adapt() time because WantedEvaluations
      // are immutable. This also means we don't need a DocumentBased
      // implementation of WantedEvaluation.

      // TODO(user): work out what to do if wavelet id is missing.
      ObservableMutableDocument<? super E, E, ?> document = router.getDocument();
      WaveletId waveletId =
          WaveletIdSerializer.INSTANCE.fromString(document.getAttribute(element, WAVELET_ID_ATTR));
      boolean wanted =
          Serializer.BOOLEAN.fromString(document.getAttribute(element, WANTED_ATTR), false);
      double certainty =
          Serializer.DOUBLE.fromString(document.getAttribute(element, CERTAINTY_ATTR), 0.0);
      long timestamp =
          Serializer.LONG.fromString(document.getAttribute(element, TIMESTAMP_ATTR), 0L);
      boolean ignored =
          Serializer.BOOLEAN.fromString(document.getAttribute(element, IGNORED_ATTR), false);

      String agentIdentity = getNonNullAttribute(document, element, AGENT_ATTR);

      String comment = getNonNullAttribute(document, element, COMMENT_ATTR);

      String adderAddress = getNonNullAttribute(document, element, ADDER_ATTR);

      return new SimpleWantedEvaluation(waveletId, adderAddress, wanted, certainty, timestamp,
          agentIdentity, ignored, comment);
    }

    @Override
    public Initializer createInitializer(
        final WantedEvaluation wantedEvaluation) {
      return new Initializer() {
        @Override
        public void initialize(Map<String, String> target) {
          target.put(
              WAVELET_ID_ATTR,
              WaveletIdSerializer.INSTANCE.toString(wantedEvaluation.getWaveletId()));
          target.put(WANTED_ATTR, Serializer.BOOLEAN.toString(wantedEvaluation.isWanted()));
          target.put(CERTAINTY_ATTR, Serializer.DOUBLE.toString(wantedEvaluation.getCertainty()));
          target.put(TIMESTAMP_ATTR, Serializer.LONG.toString(wantedEvaluation.getTimestamp()));
          target.put(ADDER_ATTR, wantedEvaluation.getAdderAddress());
          target.put(AGENT_ATTR, wantedEvaluation.getAgentIdentity());
          target.put(COMMENT_ATTR, wantedEvaluation.getComment());
          target.put(IGNORED_ATTR, Serializer.BOOLEAN.toString(wantedEvaluation.isIgnored()));
        }
      };
    }

    /**
     * Convenience method for getting an attribute value, substituting an empty
     * string for a null attribute.
     *
     * @param document document to get from
     * @param element element to get from
     * @param attributeName name of attribute to get
     *
     * @return value of attribute, or empty string if the value is null
     */
    private String getNonNullAttribute(ObservableMutableDocument<? super E, E, ?> document,
        E element, String attributeName) {
      String attribute = document.getAttribute(element, attributeName);
      return attribute == null ? "" : attribute;
    }
  }

  public static final String AGENT_ATTR = "agent";
  public static final String CERTAINTY_ATTR = "certainty";
  public static final String COMMENT_ATTR = "comment";
  public static final String TIMESTAMP_ATTR = "timestamp";
  public static final String WANTED_ATTR = "wanted";
  public static final String WANTED_EVAL_TAG = "wanted";
  public static final String WAVELET_ID_ATTR = "wavelet_id";
  public static final String IGNORED_ATTR = "ignored";
  public static final String ADDER_ATTR = "adder";

  /**
   * Creates the document based store.
   *
   * If the document does not yet have an "evals" tag this method will first add
   * one as a child of the root.
   *
   * @param document the document on which to base the manifest
   */
  public static <N, E extends N> DocumentBasedAbuseStore<N, E> create(
      DocumentEventRouter<N, E, ?> router) {
    DocumentBasedElementList<E, WantedEvaluation, WantedEvaluation> list =
        DocumentBasedElementList.create(router,
            router.getDocument().getDocumentElement(), WANTED_EVAL_TAG,
            new ElementFactory<E>());
    return new DocumentBasedAbuseStore<N, E>(list);
  }

  /** XML backed list holding the Abuse Store data. */
  private final ObservableElementList<WantedEvaluation, WantedEvaluation> list;

  /** Contains all the listeners to this abuse store. */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /** Constructor. */
  private DocumentBasedAbuseStore(
      ObservableElementList<WantedEvaluation, WantedEvaluation> list) {
    this.list = list;

    this.list.addListener(new ObservableElementList.Listener<WantedEvaluation>() {
      @Override
      public void onValueAdded(WantedEvaluation entry) {
        triggerEvaluationAdded(entry);
      }

      @Override
      public void onValueRemoved(WantedEvaluation entry) {
        // This never happens, and, when it does, is safe to ignore.
      }
    });
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    // Could check for duplicates at this point. In actual use, however,
    // duplicates are likely to be rare, and it is unclear whether scanning the
    // list of duplicates would be a win.
    list.add(evaluation);
  }

  @Override
  public Set<WantedEvaluation> getWantedEvaluations() {
    Set<WantedEvaluation> result = new HashSet<WantedEvaluation>();
    for (WantedEvaluation each : list.getValues()) {
      result.add(each);
    }
    return result;
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  /** Helper method - send to all listeners */
  private void triggerEvaluationAdded(WantedEvaluation newEvaluation) {
    for (Listener l : listeners) {
      l.onEvaluationAdded(newEvaluation);
    }
  }
}
