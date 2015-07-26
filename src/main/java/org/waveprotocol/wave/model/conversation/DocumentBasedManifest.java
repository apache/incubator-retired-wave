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

package org.waveprotocol.wave.model.conversation;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.adt.ObservableStructuredValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedStructuredValue;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.Map;

/**
 * Manifest backed by a mutable document.
 *
 * Example Manifest XML:
 *
 * <conversation sort="m" ...private reply anchor stuff>
 *  <blip id="b1"></blip>            <!-- the "root" blip -->
 *  <blip id="b2">
 *    <thread id="b2t1">             <!-- a reply to b2 -->
 *      <blip id="b2t1b1"></blip>
 *    </thread>
 *    <thread id="b2t2" inline="1">  <!-- an inline reply to b2 -->
 *      ...
 *    </thread>
 *   </blip...>
 * </conversation>
 *
 * @author anorth@google.com (Alex North)
 */
public final class DocumentBasedManifest implements ObservableManifest {

  public enum AnchorKey {
    WAVELET { @Override public String toString() { return "anchorWavelet"; } },
    BLIP { @Override public String toString() { return "anchorBlip"; } },
  }

  /** {@link Factory} for manifests. */
  static final Factory<Doc.E, DocumentBasedManifest, Void> FACTORY =
      new Factory<Doc.E, DocumentBasedManifest, Void>() {
        @Override
        public DocumentBasedManifest adapt(DocumentEventRouter<? super Doc.E, Doc.E, ?> router,
            E element) {
          return DocumentBasedManifest.createOnExisting(router, element);
        }

        @Override
        public Initializer createInitializer(Void initialState) {
          // NOTE(anorth): We could set the private reply anchor info here.
          return new Initializer() {
            public void initialize(Map<String, String> target) {
            }
          };
        }
      };

  /**
   * Checks whether a manifest could be created on a document.
   */
  public static boolean documentHasManifest(Document doc) {
    // True if the document has a top-level <MANIFEST_TOP_TAG> element.
    // The schema implies this is the only possible top element.
    Doc.E top = DocHelper.getFirstChildElement(doc, doc.getDocumentElement());
    return (top != null) && doc.getTagName(top).equals(MANIFEST_TOP_TAG);
  }

  /**
   * Deletes the toplevel conversation manifest in the given document.
   */
  public static void delete(Document manifestDoc) {
    Doc.E top = DocHelper.expectAndGetFirstTopLevelElement(manifestDoc, MANIFEST_TOP_TAG);
    manifestDoc.deleteNode(top);
  }

  /**
   * Initialises a manifest document such that
   * {@link #documentHasManifest(Document)} is true.
   *
   * @param doc document in which to initialise conversation structure
   * @precondition {@code !documentHasManifest(doc)}
   */
  static void initialiseDocumentManifest(Document doc) {
    // The schema implies that any other top-level elements must be
    // manifest roots, thus if the precondition holds then the document
    // must be empty.
    DocHelper.createFirstTopLevelElement(doc, MANIFEST_TOP_TAG);
  }

  /**
   * Creates a document-based manifest backed by a document with existing
   * conversation structure.
   *
   * @param router event router for the document on which to base the manifest
   * @precondition {@code documentHasManifest(doc)}
   */
  static <E> DocumentBasedManifest createOnExisting(
      DocumentEventRouter<? super E, E, ?> router, E top) {
    Preconditions.checkArgument(router.getDocument().getTagName(top).equals(MANIFEST_TOP_TAG),
        "Invalid manifest top tag %s", router.getDocument().getTagName(top));
    return new DocumentBasedManifest(
        DocumentBasedManifestThread.create(router, top),
        DocumentBasedStructuredValue.create(router, top, Serializer.STRING, AnchorKey.class));
  }

  /**
   * Creates a document-based manifest backed by an empty document.
   *
   * @param doc the document on which to base the manifest
   * @precondition {@code !documentHasManifest(doc)}
   */
  @VisibleForTesting
  public static <E> DocumentBasedManifest createNew(
      ObservableMutableDocument<? super E, E, ?> doc) {
    // If the precondition holds then the document must be empty.
    E top = DocHelper.createFirstTopLevelElement(doc, MANIFEST_TOP_TAG);
    DocumentEventRouter<? super E, E, ?> router = DefaultDocumentEventRouter.create(doc);
    return new DocumentBasedManifest(
        DocumentBasedManifestThread.create(router, top),
        DocumentBasedStructuredValue.create(router, top, Serializer.STRING, AnchorKey.class));
  }

  /** The root element of the conversation manifest. */
  public static final String MANIFEST_TOP_TAG = "conversation";

  /** The manifest anchor value. */
  private final ObservableStructuredValue<AnchorKey, String> anchor;

  /** The root thread. */
  private final ObservableManifestThread rootThread;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /**
   * Creates a document-based manifest. Package-private for testing.
   *
   * @param rootThread the root thread
   * @param anchor value representing the conversation anchor
   */
  DocumentBasedManifest(ObservableManifestThread rootThread,
      ObservableStructuredValue<AnchorKey, String> anchor) {
    ObservableStructuredValue.Listener<AnchorKey, String> anchorListener =
        new ObservableStructuredValue.Listener<AnchorKey, String>() {
          @Override
          public void onValuesChanged(Map<AnchorKey, ? extends String> oldValues,
              Map<AnchorKey, ? extends String> newValues) {
            triggerOnAnchorChanged(oldValues, newValues);
          }

          @Override
          public void onDeleted() {
            // TODO(anorth): remove onDeleted from this interface.
          }
        };

    this.rootThread = rootThread;
    this.anchor = anchor;
    this.anchor.addListener(anchorListener);
  }

  @Override
  public AnchorData getAnchor() {
    return new AnchorData(anchor.get(AnchorKey.WAVELET), anchor.get(AnchorKey.BLIP));
  }

  @Override
  public void setAnchor(AnchorData newAnchor) {
    anchor.set(CollectionUtils.immutableMap(AnchorKey.WAVELET, newAnchor.getConversationId(),
        AnchorKey.BLIP, newAnchor.getBlipId()));
  }

  @Override
  public ObservableManifestThread getRootThread() {
    return rootThread;
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void triggerOnAnchorChanged(Map<AnchorKey, ? extends String> oldValues,
      Map<AnchorKey, ? extends String> newValues) {
    // Get old values. If a component didn't change use its current value.
    String oldAnchorWavelet = (oldValues.containsKey(AnchorKey.WAVELET)) ?
        oldValues.get(AnchorKey.WAVELET) : anchor.get(AnchorKey.WAVELET);
    String oldAnchorBlipId = (oldValues.containsKey(AnchorKey.BLIP)) ?
        oldValues.get(AnchorKey.BLIP) : anchor.get(AnchorKey.BLIP);

    // Get new/current values from the underlying state. If a component didn't
    // change it will not be present in newValues.
    String newAnchorWavelet = anchor.get(AnchorKey.WAVELET);
    String newAnchorBlipId = anchor.get(AnchorKey.BLIP);

    AnchorData oldAnchor = new AnchorData(oldAnchorWavelet, oldAnchorBlipId);
    AnchorData newAnchor = new AnchorData(newAnchorWavelet, newAnchorBlipId);

    if (!oldAnchor.equals(newAnchor)) {
      for (Listener l : listeners) {
        l.onAnchorChanged(oldAnchor, newAnchor);
      }
    }
  }
}
