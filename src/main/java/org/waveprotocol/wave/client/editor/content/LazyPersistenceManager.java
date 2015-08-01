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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.client.editor.EditorStaticDeps;

import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DomOperationUtil;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.document.util.ReadableTreeWalker;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Bundle of documents to manage the lazy promotion of local nodes into the persistent view.
 *
 * To add this feature, the manager adapts the following objects:
 *   - a LocalDocument containing the entire local document DOM tree
 *   - a LocationMapper to assist in finding locations in the the persisted tree.
 *   - a ReadableDocumentView containing a view over the local document of just persisted nodes.
 *   - a Sink to send correction ops to.
 *
 * It allows nodes in the LocalDocument to be marked as lazily persistable, then when needed
 * (defined as a point inside being needed to be persisted) it creates an op that will persist
 * the required subtree DOM.
 *
 * Additionally, when the op is sent, a delegate is created that redirects node creation methods
 * to reuse the nodes that already exist in the local doc for the persisted version. This avoids
 * the need for the lazy persistence to affect the local document at all.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class LazyPersistenceManager {
  private final SilentOperationSink<DocOp> sink;
  private final LocalDocument<ContentNode, ContentElement, ContentTextNode> localDoc;
  private final LocationMapper<ContentNode> persistedLocations;
  private final ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> persistedView;

  /** Delegate out for createElement / createTextNode calls that may reuse old nodes. */
  private ReadableTreeWalker<ContentNode, ContentElement, ContentTextNode>
    nodeCreationDelegate = null;

  /**
   * Contains all nodes that are lazily persisted.
   * NOTE(patcoleman): Use IdentitySet when possible.
   */
  private final IdentityMap<ContentNode, ContentNode> lazilyPersistedNodes =
      CollectionUtils.createIdentityMap();

  /**
   * Creates by attaching helper members for manipulating the persistent filteredView.
   * @param outgoingSink Outgoing Sink for the fix-up persistence ops.
   * @param localDoc Document with all local and persisted nodes.
   * @param persistedLocations Indexed version of the persisted tree.
   * @param persistedView View over the persisted document, for filtering.
   * @param localCorrectionSink Internal sink for the fix-up persistence ops.
   */
  public LazyPersistenceManager(
      final SilentOperationSink<DocOp> outgoingSink,
      LocalDocument<ContentNode, ContentElement, ContentTextNode> localDoc,
      LocationMapper<ContentNode> persistedLocations,
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> persistedView,
      final ModifiableDocument localCorrectionSink) {
    // combine both sinks into one:
    this.sink = new SilentOperationSink<DocOp>() {
      @Override
      public void consume(DocOp op) {
        try {
          localCorrectionSink.consume(op);
          outgoingSink.consume(op);
        } catch (OperationException e) {
          EditorStaticDeps.logger.fatal().log(e);
        }
      }
    };
    this.localDoc = localDoc;
    this.persistedLocations = persistedLocations;
    this.persistedView = persistedView;
  }

  /** Mark a node to be persisted when it is next needed. */
  public void markAsLazyPersisted(ContentNode node) {
    // NOTE(patcoleman): not checked due to speed reasons, but this node should not have
    //   a descendant or ancestor also lazily persisted, or a persisted descendant.
    // i.e. it should be true that every lazy node is the root of a local subtree containing
    //   no other lazy nodes.
    lazilyPersistedNodes.put(node, node);
  }

  /** @return Whether this should be used to 'create' nodes by retrieving the local instances. */
  public boolean isCreationDelegate() {
    return nodeCreationDelegate != null;
  }

  /** Creates an element by instead sourcing already placed local nodes. */
  public ContentElement createElement(String tagName, Map<String, String> attributes,
      ContentElement parent, ContentNode nodeAfter) {
    Preconditions.checkState(isCreationDelegate(),
        "Lazy persistence delegated an element creation when not a delegate.");
    return nodeCreationDelegate.checkElement(tagName, attributes);
  }

  /** Creates a text node by instead sourcing already placed local nodes. */
  public ContentTextNode createTextNode(String data, ContentElement parent, ContentNode nodeAfter) {
    Preconditions.checkState(isCreationDelegate(),
        "Lazy persistence delegated a text node creationgwhen not a delegate.");
    return nodeCreationDelegate.checkTextNode(data);
  }

  /** Manage the lazy persistence of nodes, given a position that is required to be persisted. */
  public void updateLazyNodes(ContentNode at) {
    // Walk up transparent local nodes to find the lowest lazy node...
    for (; localDoc.isTransparent(at); at = localDoc.getParentElement(at)) {
      if (lazilyPersistedNodes.has(at)) {
        // ... and if found, persist.
        lazilyPersistedNodes.remove(at);
        this.actuallyPersist(at);
        break;
      }
      if (localDoc.isSameNode(at, localDoc.getDocumentElement())) {
        break;
      }
    }
  }

  /** Utility for the logic that goes into actually persisting a given node and its subtree. */
  private void actuallyPersist(ContentNode localNode) {
    // NOTE(patcoleman): assumes entire subtree is local - do not call with persisted children!
    if (persistedView.isSameNode(localNode, persistedView.getVisibleNode(localNode))) {
      throw new IllegalArgumentException("opaquePersist: element must not be persistent");
    }

    // when promoting to opaque, we find where it is and mark it as no longer transparent,
    // persisting that operation to the server

    // find length to the insertion point, and length after:
    int position = DocHelper.getFilteredLocation(persistedLocations,
        persistedView, Point.end(localNode));
    int remainder = persistedLocations.size() - position;

    // create op, wrapping the tree operations within retains.
    DocOpBuffer opBuffer = new DocOpBuffer();
    safeRetain(opBuffer, position);
    DomOperationUtil.buildDomInitializationFromSubtree(localDoc, localNode, opBuffer);
    safeRetain(opBuffer, remainder);
    DocOp op = opBuffer.finish();

    // fake out the node creation, and consume:
    nodeCreationDelegate =
      new ReadableTreeWalker<ContentNode, ContentElement, ContentTextNode>(localDoc, localNode);
    sink.consume(op);
    Preconditions.checkState(nodeCreationDelegate.checkComplete(),
        "Walk of tree did not match up...");
    nodeCreationDelegate = null;
  }

  // Utility - TODO(patcoleman): move somewhere common, if not already exists?
  private static void safeRetain(DocOpCursor cursor, int retain) {
    if (retain > 0) {
      cursor.retain(retain);
    }
  }
}
