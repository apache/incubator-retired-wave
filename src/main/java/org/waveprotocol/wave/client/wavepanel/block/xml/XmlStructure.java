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


package org.waveprotocol.wave.client.wavepanel.block.xml;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.block.BlockStructure;

/**
 * Exposes an XML block dom as a view structure.
 *
 */
public final class XmlStructure implements BlockStructure {

  /**
   * JSO access to a DOM node.
   */
  public final static class XmlNode extends JavaScriptObject implements BlockStructure.Node {

    //
    // Schema: <x id="X..." k="n"></x>
    //
    // Block ids are quoted by prepending "X", so as not to clash with HTML
    // views.
    // "n" is the ordinal of the NodeType enum.
    //

    protected XmlNode() {
    }

    public static XmlNode of(JavaScriptObject jso) {
      return (XmlNode) jso;
    }

    private static String quoteId(String id) {
      return "X" + id;
    }

    private static String unquoteId(String value) {
      return value.substring(1);
    }

    @Override
    public String getId() {
      return unquoteId(getAttribute("id"));
    }

    @Override
    public NodeType getType() {
      int type = getTypeInt();
      if (type < 0) {
        String id = getAttribute("id");
        String kind = getAttribute("k");
        throw new RuntimeException("bad kind: " + kind + ", on id: " + id);
      }
      return NodeType.values()[type];
    }

    private native String getAttribute(String name) /*-{
      return this.getAttribute(name);
    }-*/;

    // For some reason, parseInt fails come back as doubles (NaN), so the JSNI
    // has to handle the error case in order to return ints safely.
    private native int getTypeInt() /*-{
      var k = parseInt(this.getAttribute("k"));
      return k >= 0 ? k : -1;
    }-*/;


    @Override
    public native XmlNode getFirstChild() /*-{
      return this.firstChild;
    }-*/;


    @Override
    public native XmlNode getLastChild() /*-{
      return this.lastChild;
    }-*/;


    @Override
    public native XmlNode getNextSibling() /*-{
      return this.nextSibling;
    }-*/;


    @Override
    public native XmlNode getParent() /*-{
      return this.parentNode;
    }-*/;


    @Override
    public native XmlNode getPreviousSibling() /*-{
      return this.previousSibling;
    }-*/;
  }

  private final XmlNode root;

  private XmlStructure(XmlNode root) {
    this.root = root;
  }

  public static XmlStructure create(String id) {
    return new XmlStructure(XmlNode.of(Document.get().getElementById(XmlNode.quoteId(id))));
  }

  @Override
  public XmlNode getRoot() {
    return root;
  }

  @Override
  public XmlNode getNode(String id) {
    return XmlNode.of(Document.get().getElementById(XmlNode.quoteId(id)));
  }

  /**
   * Detaches the XML tree from the DOM. Use with caution.
   * {@link #getNode(String)} will not work after this.
   */
  public void detach() {
    if (root != null) {
      Element.as(root).removeFromParent();
    }
  }
}
