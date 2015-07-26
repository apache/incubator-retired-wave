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

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.List;

/**
 * A safe container for building xml strings This is a Mutable class, it uses a
 * StringBuilder internally for performance
 *
 * This class provides an interface, and some handy constructor methods. The
 * implementation is in {@link XmlStringBuilderDoc}. That class also extends
 * this interface to provide methods to convert nodes to their string
 * representation using a document view. The need for that capability is rarer,
 * and hence the split into two classes, so users don't have to suffer the
 * verbosity of generics if they are just building a string. The static
 * constructors here will return the appropriate type depending on whether a
 * document view is given in the constructor, and so we also gain type safety
 * (it's impossible to call "appendNode" on a builder that was not constructed
 * with a document view.)
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class XmlStringBuilder {

  /**
   * Serializes the inner XML of a document.
   *
   * @param doc  document
   * @return the XML representation of the inner contents of {@code doc}.
   */
  public static <N> XmlStringBuilderDoc<N, ?, ?> innerXml(ReadableDocument<N, ?, ?> doc) {
    //
    // NOTE(user):
    // This two-step unravelling of re-quantifying the type-parameters into type variables
    // is only required in order to coax javac's dumb generic-inference to realise that the call is
    // type-safe.  Eclipse's compiler is smarter, and does not require this extra work.
    //
    return innerXml1(doc);
  }

  private static <N, E extends N, T extends N> XmlStringBuilderDoc<N, E, T>
      innerXml1(ReadableDocument<N, E, T> doc) {
    return createChildren(doc, doc.getDocumentElement());
  }

  /**
   * Serializes the inner XML of a document with constraints.
   *
   * @param doc document
   * @return the XML representation of the inner contents of {@code doc}.
   */
  public static <N> XmlStringBuilderDoc<N, ?, ?> innerXml(ReadableDocument<N, ?, ?> doc,
      PermittedCharacters permittedChars) {
    return innerXmlHelper(doc, permittedChars);
  }

  private static <N, E extends N, T extends N> XmlStringBuilderDoc<N, E, T> innerXmlHelper(
      ReadableDocument<N, E, T> doc, PermittedCharacters permittedChars) {
    return XmlStringBuilderDoc.createEmptyWithCharConstraints(doc, permittedChars)
        .appendChildXmlFragment(doc.getDocumentElement());
  }

  /**
   * Serializes the outer XML of a document.
   *
   * @param doc  a document
   * @return the XML representation of {@code doc}.
   */
  public static <N> XmlStringBuilderDoc<N, ?, ?> outerXml(ReadableDocument<N, ?, ?> doc) {
    //
    // NOTE(user):
    // This two-step unravelling of re-quantifying the type-parameters into type variables
    // is only required in order to coax javac's dumb generic-inference to realise that the call is
    // type-safe.  Eclipse's compiler is smarter, and does not require this extra work.
    //
    return outerXml1(doc);
  }

  private static <N, E extends N, T extends N> XmlStringBuilderDoc<N, E, T> outerXml1(
      ReadableDocument<N, E, T> doc) {
    return createNode(doc, doc.getDocumentElement());
  }

  /**
   * Constructs empty xml
   */
  public static XmlStringBuilder createEmpty() {
    return XmlStringBuilderDoc.createEmpty(null);
  }

  /**
   * Same as {@link #createEmpty()}, but with constraints on characters.
   * @param permittedChars
   */
  public static XmlStringBuilder createEmptyWithCharConstraints(
      PermittedCharacters permittedChars) {
    return XmlStringBuilderDoc.createEmptyWithCharConstraints(null, permittedChars);
  }
  /**
   * Constructs a builder from a string containing a bit of xml
   * @param xmlContent the xml content
   * @return new {@link XmlStringBuilder} with the xml as content
   */
  public static XmlStringBuilder createFromXmlString(String xmlContent) {
    return XmlStringBuilder.innerXml(DocProviders.POJO.parse(xmlContent));
  }

  /**
   * Same as {@link #createFromXmlString(String)}, but with constraints on characters.
   * @param xmlContent
   */
  public static XmlStringBuilder createFromXmlStringWithContraints(String xmlContent,
      PermittedCharacters permittedChars) {
    return XmlStringBuilder.innerXml(DocProviders.POJO.parse(xmlContent), permittedChars);
  }

  /**
   * @param text Will be adjusted if it contains invalid characters
   * @return new {@link XmlStringBuilder} with text content
   */
  // TODO(danilatos): Rename, because now it might modify text as well
  public static XmlStringBuilder createText(String text) {
    return createEmpty().appendText(text);
  }

  /**
   * Constructs empty xml
   */
  public static <N, E extends N, T extends N> XmlStringBuilderDoc<N,E,T>
      createEmpty(ReadableDocument<N, E, T> view) {
    return XmlStringBuilderDoc.createEmpty(view);
  }

  /**
   * @param node Must be either an Element or a Text
   * @return The xml string representation of the given node (i.e. like outerHTML)
   */
  public static <N, E extends N, T extends N> XmlStringBuilderDoc<N,E,T>
      createNode(ReadableDocument<N, E, T> view, N node) {
    return createEmpty(view).appendNode(node);
  }

  /**
   * @param element
   * @return The xml string representation of the node's children (i.e. like innerHTML)
   */
  public static <N, E extends N, T extends N> XmlStringBuilderDoc<N,E,T>
      createChildren(ReadableDocument<N, E, T> view, E element) {
    return createEmpty(view).appendChildXmlFragment(element);
  }

  /**
   * @param text
   * @return new {@link XmlStringBuilder} with text content
   */
  public static <N, E extends N, T extends N> XmlStringBuilderDoc<N,E,T>
      createText(ReadableDocument<N, E, T> view, String text) {
    return createEmpty(view).appendText(text);
  }

  /**
   * TODO(danilatos): Consider removing this method from here...
   * @return self for convenience
   */
  public XmlStringBuilder appendBr() {
    return append(createEmpty().wrap("br"));
  }

  /**
   * Empty the string
   */
  public abstract void clear();

  /**
   * @return the "item length"
   */
  public abstract int getLength();

  /**
   * Appends an XmlString onto this one
   *
   * @param xml
   * @return this for convenience
   */
  public abstract XmlStringBuilder append(XmlStringBuilder xml);

  /**
   * Wraps self in the given tag, with length adjusted to 1
   *
   * @param tagName
   * @return this for convenience
   */
  public abstract XmlStringBuilder wrap(String tagName);

  /**
   * Wraps the string in the given tag with attributes, length adjusted to 1
   *
   * @param tagName
   * @param attribs A vararg list of strings of even length, every odd one is an attrib name,
   *   every even one is an attrib value
   * @return this for convenience
   */
  public abstract XmlStringBuilder wrap(String tagName, String... attribs);

  /**
   * Same as {@link #appendText(String)} with {@link PermittedCharacters#BLIP_TEXT} as
   * the default permitted characters.
   */
  // TODO(danilatos): Rename, because now it might modify text as well
  public abstract XmlStringBuilder appendText(String text);

  /**
   * Adds the text to the end of the xml. The text may be adjusted based on the
   * permitted characters restriction - invalid characters will be ommitted or
   * replaced with whitespace.
   *
   * @param text
   * @param permittedChars
   * @return self for convenience
   */
  public abstract XmlStringBuilder appendText(String text, PermittedCharacters permittedChars);

  /**
   * This method should be used in preference to toString()
   *
   * TODO(danilatos): Make toString() return a prefixed version of this method.
   *
   * @return The XML string as a String
   */
  public abstract String getXmlString();

  /**
   * Same {@link XmlStringBuilder#wrap(String, String...)} but with a map rather than a list.
   * @param tagName
   * @param attribs
   */
  public final XmlStringBuilder wrap(String tagName, StringMap<String> attribs) {
    final List<String> attribList = new ArrayList<String>();
    attribs.each(new ProcV<String>() {
      @Override
      public void apply(String key, String value) {
        Preconditions.checkNotNull(key, "key should not be null be null");
        Preconditions.checkNotNull(value, "value should not be null be null");
        attribList.add(key);
        attribList.add(value);
      }});
    return wrap(tagName, attribList.toArray(new String[attribs.countEntries() * 2]));
  }
}
