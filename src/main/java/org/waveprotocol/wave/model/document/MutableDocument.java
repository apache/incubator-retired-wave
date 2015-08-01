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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.Map;

/**
 * Defines a set of methods to perform changes to a Document. The changes will generate
 * operations.
 *
 * Extends ReadableDocument to allow traversal
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface MutableDocument<N, E extends N, T extends N>
    extends ReadableWDocument<N, E, T>, MutableAnnotationSet.Persistent {

  /**
   * (For-now-)Empty interface representing a document contributor
   */
  public interface Contributor { }

  /**
   * A procedure to be run with a mutable document.
   *
   * The method, not the interface, is parameterised.
   */
  public interface Action {
    /**
     * Runs the action with the given document.
     *
     * The document may only be assumed to be valid only during the method's
     * execution.
     */
    <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc);
  }

  /**
   * A procedure to be run with which returns a value.
   *
   * @see Action
   */
  public interface Method<V> {
    /**
     * Runs the method with the given document.
     *
     * The document may only be assumed to be valid only during the method's
     * execution.
     */
    <N, E extends N, T extends N> V exec(MutableDocument<N, E, T> doc);
  }

  /**
   * Performs an action with this document.
   */
  public void with(Action actionToRunWithDocument);

  /**
   * Performs a method with this document.
   */
  public <V> V with(Method<V> methodToRunWithDocument);

  /**
   * Delete a node
   *
   * @param element
   */
  public void deleteNode(E element);

  /**
   * Empty an element
   *
   * @param element
   */
  public void emptyElement(E element);

  /**
   * Insert text (no xml) at a location.
   *
   * @param location
   * @param text
   */
  public void insertText(int location, String text);

  /**
   * Insert text (no xml) at a point
   *
   * @param point
   * @param text
   */
  public void insertText(Point<N> point, String text);

  /**
   * Appends xml markup to end of document.
   *
   * @param xml
   * @return the first inserted element, or null if text is
   *   at that point.
   */
  public E appendXml(XmlStringBuilder xml);

  /**
   * Inserts xml markup at point.
   *
   * @param point
   * @param xml
   * @return the first inserted element, or null if text is
   *   at that point.
   */
  public E insertXml(Point<N> point, XmlStringBuilder xml);

  /**
   * Creates an element at a location.
   *
   * @param point point at which to insert the new element
   * @param tag
   * @param attributes
   * @return created element, inserted at given point
   */
  public E createElement(Point<N> point, String tag, Map<String,String> attributes);

  /**
   * Creates an element as the last child of another element.
   *
   * @param parent   parent element
   * @param tag
   * @param attributes
   * @return created element
   */
  public E createChildElement(E parent, String tag, Map<String,String> attributes);

  /**
   * Delete everything between two locations
   *
   * @param start
   * @param end
   * @return A new range representing the resulting start and end
   */
  public Range deleteRange(int start, int end);

  /**
   * Delete everything between two points
   *
   * @param start
   * @param end
   * @return A new range representing the resulting start and end
   */
  public PointRange<N> deleteRange(Point<N> start, Point<N> end);

  /**
   * Moves a run of siblings to a new point in the document.
   * @param location Where to move to
   * @param from Starting node
   * @param to Ending node or null, sibling of from.
   */
  public void moveSiblings(Point<N> location, N from, N toExcl);

  /**
   * Set an attribute on an element. A value of null will clear the attribute.
   *
   * @param element
   * @param name
   * @param value
   */
  public void setElementAttribute(E element, String name, String value);

  /**
   * Replaces the attributes set on an element with the specified set. If the
   * element has attributes with keys not in the given attributes, then they are
   * removed from the element.
   *
   * @param element
   * @param attrs
   */
  public void setElementAttributes(E element, Attributes attrs);

  /**
   * Updates only the given attributes to new values for an element.
   * Does not modify attributes whose keys are not in the given set of
   * attributes. An attribute value of null will clear that attribute.
   *
   * @param element
   * @param attrs
   */
  public void updateElementAttributes(E element, Map<String, String> attrs);

  //
  // Operation concerns.
  //

  /**
   * Applies document operations directly.
   *
   * @param op The document operation to apply.
   */
  public void hackConsume(Nindo op);

  /**
   * Factory for creating MutableDocuments.
   *
   * @param <D>  Type of MutableDocument to create
   */
  interface Factory<D extends MutableDocument<?,?,?>> extends DocumentFactory<D> {
  }

  /**
   * Provider for MutableDocuments.
   *
   * @param <D>  Type of MutableDocument to provide.
   */
  interface Provider<D extends MutableDocument<?,?,?>>
      extends ReadableDocument.Provider<D>, Factory<D> {
  }
}
