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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.List;
import java.util.Map;

/**
 * Handy delegating implementation.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class MutableDocumentProxy<N, E extends N, T extends N>
    implements MutableDocument<N, E, T> {

  public static class DocumentProxy extends MutableDocumentProxy<Doc.N, Doc.E, Doc.T>
      implements Document {

    public  DocumentProxy() {
      super();
    }

    public DocumentProxy(Document delegate, String noDelegateErrorMessage) {
      super(delegate, noDelegateErrorMessage);
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // Adapter
    public DocumentProxy(MutableDocument delegate, String noDelegateErrorMessage) {
      super(delegate, noDelegateErrorMessage);
      Preconditions.checkArgument(delegate.getDocumentElement() instanceof Doc.E,
          "Incompatibel delegate type - must be of the Doc.* variety of nodes");
    }
  }

  private final String noDelegateErrorMessage;

  private MutableDocument<N, E, T> delegate;

  /**
   * Constructor with a null delegate and default error message.
   */
  public MutableDocumentProxy() {
    this(null, "delegate document is not set");
  }

  /**
   * @param delegate initial delegate
   * @param noDelegateErrorMessage error message when the delegate is null
   */
  public MutableDocumentProxy(MutableDocument<N, E, T> delegate, String noDelegateErrorMessage) {
    this.delegate = delegate;
    this.noDelegateErrorMessage = noDelegateErrorMessage;
  }

  /**
   * Sets the delegate to a new object
   *
   * @param newDelegate
   */
  protected void setDelegate(MutableDocument<N, E, T> newDelegate) {
    this.delegate = newDelegate;
  }

  /**
   * @return true if this proxy currently has a delegate.
   */
  protected boolean hasDelegate() {
    return delegate != null;
  }

  /**
   * Retrieves the delegate for immediate use.
   * Fails if there is no delegate.
   *
   * @return the current delegate
   */
  protected MutableDocument<N, E, T> getDelegate() {
    if (!hasDelegate()) {
      throw new IllegalStateException("MutableDocumentProxy: " + noDelegateErrorMessage);
    }
    return delegate;
  }

  @Override
  public void with(Action actionToRunWithDocument) {
    getDelegate().with(actionToRunWithDocument);
  }

  @Override
  public <V> V with(Method<V> methodToRunWithDocument) {
    return getDelegate().with(methodToRunWithDocument);
  }

  // Mostly eclipse generated delegate methods below.

  @Override
  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    return getDelegate().annotationCursor(start, end, keys);
  }

  @Override
  public Iterable<AnnotationInterval<String>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    return getDelegate().annotationIntervals(start, end, keys);
  }

  @Override
  public E appendXml(XmlStringBuilder xml) {
    return getDelegate().appendXml(xml);
  }

  @Override
  public E asElement(N node) {
    return getDelegate().asElement(node);
  }

  @Override
  public T asText(N node) {
    return getDelegate().asText(node);
  }

  @Override
  public E createChildElement(E parent, String tag, Map<String, String> attributes) {
    return getDelegate().createChildElement(parent, tag, attributes);
  }

  @Override
  public E createElement(Point<N> point, String tag, Map<String, String> attributes) {
    return getDelegate().createElement(point, tag, attributes);
  }

  @Override
  public void deleteNode(E element) {
    getDelegate().deleteNode(element);
  }

  @Override
  public Range deleteRange(int start, int end) {
    return getDelegate().deleteRange(start, end);
  }

  @Override
  public PointRange<N> deleteRange(Point<N> start, Point<N> end) {
    return getDelegate().deleteRange(start, end);
  }

  @Override
  public void emptyElement(E element) {
    getDelegate().emptyElement(element);
  }

  @Override
  public int firstAnnotationChange(int start, int end, String key, String fromValue) {
    return getDelegate().firstAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public void forEachAnnotationAt(int location, ProcV<String> callback) {
    getDelegate().forEachAnnotationAt(location, callback);
  }

  @Override
  public String getAnnotation(int location, String key) {
    return getDelegate().getAnnotation(location, key);
  }

  @Override
  public String getAttribute(E element, String name) {
    return getDelegate().getAttribute(element, name);
  }

  @Override
  public Map<String, String> getAttributes(E element) {
    return getDelegate().getAttributes(element);
  }

  @Override
  public String getData(T textNode) {
    return getDelegate().getData(textNode);
  }

  @Override
  public E getDocumentElement() {
    return getDelegate().getDocumentElement();
  }

  @Override
  public N getFirstChild(N node) {
    return getDelegate().getFirstChild(node);
  }

  @Override
  public N getLastChild(N node) {
    return getDelegate().getLastChild(node);
  }

  @Override
  public int getLength(T textNode) {
    return getDelegate().getLength(textNode);
  }

  @Override
  public int getLocation(N node) {
    return getDelegate().getLocation(node);
  }

  @Override
  public int getLocation(Point<N> point) {
    return getDelegate().getLocation(point);
  }

  @Override
  public N getNextSibling(N node) {
    return getDelegate().getNextSibling(node);
  }

  @Override
  public short getNodeType(N node) {
    return getDelegate().getNodeType(node);
  }

  @Override
  public E getParentElement(N node) {
    return getDelegate().getParentElement(node);
  }

  @Override
  public N getPreviousSibling(N node) {
    return getDelegate().getPreviousSibling(node);
  }

  @Override
  public String getTagName(E element) {
    return getDelegate().getTagName(element);
  }

  @Override
  public void insertText(int location, String text) {
    getDelegate().insertText(location, text);
  }

  @Override
  public void insertText(Point<N> point, String text) {
    getDelegate().insertText(point, text);
  }

  @Override
  public E insertXml(Point<N> point, XmlStringBuilder xml) {
    return getDelegate().insertXml(point, xml);
  }

  @Override
  public boolean isSameNode(N node, N other) {
    return getDelegate().isSameNode(node, other);
  }

  @Override
  public int lastAnnotationChange(int start, int end, String key, String fromValue) {
    return getDelegate().lastAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public Point<N> locate(int location) {
    return getDelegate().locate(location);
  }

  @Override
  public void moveSiblings(Point<N> location, N from, N to) {
    getDelegate().moveSiblings(location, from, to);
  }

  @Override
  public Iterable<RangedAnnotation<String>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    return getDelegate().rangedAnnotations(start, end, keys);
  }

  @Override
  public void resetAnnotation(int start, int end, String key, String value) {
    getDelegate().resetAnnotation(start, end, key, value);
  }

  @Override
  @Deprecated
  public void resetAnnotationsInRange(int rangeStart, int rangeEnd, String key,
      List<org.waveprotocol.wave.model.document.MutableAnnotationSet.RangedValue<String>> values) {
    getDelegate().resetAnnotationsInRange(rangeStart, rangeEnd, key, values);
  }

  @Override
  public void setAnnotation(int start, int end, String key, String value) {
    getDelegate().setAnnotation(start, end, key, value);
  }

  @Override
  public void setElementAttribute(E element, String name, String value) {
    getDelegate().setElementAttribute(element, name, value);
  }

  @Override
  public void setElementAttributes(E element, Attributes attrs) {
    getDelegate().setElementAttributes(element, attrs);
  }

  @Override
  public int size() {
    return getDelegate().size();
  }

  @Override
  public void hackConsume(Nindo op) {
    getDelegate().hackConsume(op);
  }

  @Override
  public DocInitialization toInitialization() {
    return getDelegate().toInitialization();
  }

  @Override
  public void updateElementAttributes(E element, Map<String, String> attrs) {
    getDelegate().updateElementAttributes(element, attrs);
  }

  @Override
  public ReadableStringSet knownKeys() {
    return getDelegate().knownKeys();
  }

  @Override
  public String toXmlString() {
    return getDelegate().toXmlString();
  }

  @Override
  public String toDebugString() {
    return getDelegate().toDebugString();
  }

  @Override
  public String toString() {
    return "MutableDocumentProxy(" + getDelegate().toString() + ")";
  }
}
