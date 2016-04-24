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

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.El;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an integrity-preserving interface for a document that stores tags
 * as follows:
 *
 * <tag>First tag name</tag>
 * <tag>Second tags name</tag>
 * ...
 *
 *
 * @param <N>
 * @param <E>
 * @param <T>
 *
 * TODO(anorth,user) implement in terms of DocumentBasedElementList to handle
 * all the XML manipulation.
 */
public class TagsDocument<N, E extends N, T extends N> {
  /**
   * A listener for changes to a TagsDocument.
   */
  public interface Listener {
    void onAdd(String tagName);
    void onRemove(int tagPosition);
  }

  /** The physical document. */
  private final MutableDocument<N, E, T> doc;

  /**
   * The list of tags in the document (used to determine where tags are
   * deleted from).
   */
  private final List<E> tagElements = new ArrayList<E>();

  private final List<Listener> listeners;

  public TagsDocument(MutableDocument<N, E, T> tagsDocument) {
    this.doc = tagsDocument;
    this.listeners = new ArrayList<Listener>();
  }

  /**
   * Add a listener for changes to the document.
   * @param listener A listener interested in TagsDocument changes.
   */
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  /**
   * Adds a tag to the document.
   *
   * @param tagName The name of the tag.
   * @param i The index at which to insert the tag.
   */
  public void addTag(String tagName, int i) {
    E node = getNthTagNode(i);
    El<N> point = Point.<N>inElement(doc.getDocumentElement(), node);
    XmlStringBuilder xml = getXmlFor(tagName);
    doc.insertXml(point, xml);
  }

  /**
   * @param index The index of the tag we are interested in.
   * @return The element that contains the desired tag.
   */
  private E getNthTagNode(int index) {
    N node = doc.getFirstChild(doc.getDocumentElement());
    for (int i = 0; i < index; i++) {
      if (node == null) {
        return null;
      }
      node = doc.getNextSibling(node);
    }
    return doc.asElement(node);
  }

  /**
   * Adds a tag to the document.
   *
   * @param tagName The name of the tag to add.
   */
  public void addTag(String tagName) {
    if (tagName == null) {
      return;
    }
    tagElements.add(doc.appendXml(getXmlFor(tagName)));
  }

  private XmlStringBuilder getXmlFor(String tag) {
    return XmlStringBuilder.createText(tag).wrap("tag");
  }

  /**
   * Deletes a tag from the document by index.
   *
   * @param index The index of the tag to delete.
   */
  public void deleteTag(int index) {
    E nthTagNode = getNthTagNode(index);
    doc.deleteNode(nthTagNode);
  }

  /**
   * Deletes a tag from the document by name.
   *
   * @param tagName the name of the tag to delete.
   */
  public void deleteTag(String tagName) {
    N node = doc.getFirstChild(doc.getDocumentElement());
    while (node != null) {
      E element = doc.asElement(node);
      if (element != null) {
        T textNode = doc.asText(doc.getFirstChild(node));
        if (textNode != null && doc.getData(textNode).equals(tagName)) {
          doc.deleteNode(element);
          return;
        }
      }
      node = doc.getNextSibling(node);
    }
  }


  /**
   * Reads the document and sets up the initial state. If a node without text
   * is found, it is deleted and reported as a non-fatal error.
   */
  public void processInitialState() {
    List<N> emptyNodes = new ArrayList<N>();
    for (N node = doc.getFirstChild(doc.getDocumentElement());
         node != null;
         node = doc.getNextSibling(node)) {
      T textNode = doc.asText(doc.getFirstChild(node));
      if (textNode == null) {
        emptyNodes.add(node);
      } else {
        notifyAddTag(doc.getData(textNode));
        tagElements.add(doc.asElement(node));
      }
    }

    for (N emptyNode : emptyNodes) {
      doc.deleteNode(doc.asElement(emptyNode));
    }
  }

  private void notifyAddTag(String tagName) {
    for (Listener listener : listeners) {
      listener.onAdd(tagName);
    }
  }

  private void notifyRemoveTag(int tagPosition) {
    for (Listener listener : listeners) {
      listener.onRemove(tagPosition);
    }
  }

  /**
   * Called when an element has been inserted into the document.
   *
   * @param e The inserted element.
   */
  public void handleInsertedElement(E e) {
    tagElements.add(e);
    String addedTagText = doc.getData(doc.asText(doc.getFirstChild(e)));
    notifyAddTag(addedTagText);
  }

  /**
   * Called when an element has been removed from the document.
   *
   * @param e The removed element.
   */
  public void handleDeletedElement(E e) {
    int i = tagElements.indexOf(e);
    if (i == -1) {
      return;
    }
    tagElements.remove(i);
    notifyRemoveTag(i);
  }
}
