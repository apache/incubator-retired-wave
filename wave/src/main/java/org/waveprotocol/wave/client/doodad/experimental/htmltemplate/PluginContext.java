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

package org.waveprotocol.wave.client.doodad.experimental.htmltemplate;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PluginContext {
  private static final List<String> EMPTY_STRING_LIST =
      Collections.unmodifiableList(Arrays.asList(new String[0]));

  // The members of this enumeration are named like the JavaScript event type
  // strings they represent, e.g., EventType.dataChanged => "dataChanged". This
  // makes EventType.valueOf() and .toString() work smoothly for us.
  private enum EventType {
    activated,
    deactivated,
    dataChanged,
    partAdded,
    partRemoved;
  }

  private static class EventListeners {
    private final Map<EventType, Set<JavaScriptObject>> listeners =
        new HashMap<EventType, Set<JavaScriptObject>>();
    private final JavaScriptObject caja;

    public EventListeners(JavaScriptObject caja) {
      this.caja = caja;
      for (EventType t : EventType.values()) {
        listeners.put(t, new HashSet<JavaScriptObject>());
      }
    }

    public void fire(EventType type, List<String> args) {
      assert(args.size() % 2 == 0);
      Set<JavaScriptObject> typeListeners = listeners.get(type);
      if (typeListeners.isEmpty()) { return; }
      JavaScriptObject event = makeEvent(type.toString(), args, caja);
      for (JavaScriptObject l : typeListeners) { invokeEventListener(l, event); }
    }

    public void add(EventType type, JavaScriptObject listener) {
      listeners.get(type).add(listener);
    }

    public void remove(EventType type, JavaScriptObject listener) {
      listeners.get(type).remove(listener);
    }

    public void add(String typeName, JavaScriptObject listener) {
      EventType type = EventType.valueOf(typeName);
      if (type == null) { return; }
      add(type, listener);
    }

    public void remove(String typeName, JavaScriptObject listener) {
      EventType type = EventType.valueOf(typeName);
      if (type == null) { return; }
      remove(type, listener);
    }
  }

  private final Map<String, JavaScriptObject> tamePartRenderings = new HashMap<String, JavaScriptObject>();
  private final ContentElement htmlTemplateElement;
  private final PartIdFactory partIdFactory;
  private final JavaScriptObject caja;
  private final EventListeners listeners;
  private final JavaScriptObject waveJSO;
  private JavaScriptObject tameDomNodeMaker;

  public PluginContext(
      ContentElement htmlTemplateElement, PartIdFactory partIdFactory, JavaScriptObject caja) {
    this.htmlTemplateElement = htmlTemplateElement;
    this.partIdFactory = partIdFactory;
    this.caja = caja;
    this.listeners = new EventListeners(caja);
    this.waveJSO = makeWaveJSOInterface(this, caja);
  }

  public JavaScriptObject getJSOInterface() {
    return waveJSO;
  }

  public List<String> getDataNames() {
    List<String> result = new ArrayList<String>();
    for (ContentNode n = htmlTemplateElement.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (HtmlTemplate.isNameValuePairElement(n)) {
        result.add(n.asElement().getAttribute(HtmlTemplate.NAMEVALUEPAIR_NAME_ATTR));
      }
    }
    return result;
  }

  public JavaScriptObject getDataNamesJSO() {
    return stringListToJSO(getDataNames(), caja);
  }

  public String getData(String name) {
    Preconditions.checkNotNull(name, "Name of data values may not be null");
    ContentElement nameValuePair = doFindNameValuePair(name);
    if (nameValuePair == null) {
      return null;
    }
    return nameValuePair.getAttribute(HtmlTemplate.NAMEVALUEPAIR_VALUE_ATTR);
  }

  public void putData(String name, String value) {
    Preconditions.checkNotNull(name, "Name of data values may not be null");
    if (value == getData(name) || (value != null && value.equals(getData(name)))) { return; }
    ContentElement nameValuePair = doFindNameValuePair(name);
    if (value == null && nameValuePair != null) {
      htmlTemplateElement.getMutableDoc().deleteNode(nameValuePair);
    } else {
      if (nameValuePair == null) {
        doAddNameValuePair(name, value);
      } else {
        htmlTemplateElement.getMutableDoc().setElementAttribute(
            nameValuePair, HtmlTemplate.NAMEVALUEPAIR_VALUE_ATTR, value);
      }
    }
  }

  public List<String> getPartIdentifiers() {
    List<String> result = new ArrayList<String>();
    for (ContentNode n = htmlTemplateElement.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (HtmlTemplate.isPartElement(n)) {
        result.add(n.asElement().getAttribute(HtmlTemplate.PART_ID_ATTR));
      }
    }
    return result;
  }

  public JavaScriptObject getPartIdentifiersJSO() {
    return stringListToJSO(getPartIdentifiers(), caja);
  }

  public Element getPartRendering(String id) {
    ContentElement part = doFindPart(id);
    if (part == null) { return null; }
    return part.getImplNodelet();
  }

  public JavaScriptObject getPartRenderingJSO(String id) {
    Preconditions.checkNotNull(id, "Identifier of part rendering may not be null");

    if (tamePartRenderings.containsKey(id)) {
      return tamePartRenderings.get(id);
    }

    Element feralRendering = getPartRendering(id);
    if (feralRendering == null) { return null; }

    if (tameDomNodeMaker == null) { return null; }
    JavaScriptObject tameRendering = makeTamePartRendering(feralRendering, tameDomNodeMaker);

    tamePartRenderings.put(id, tameRendering);
    return tameRendering;
  }

  public String addPart() {
    String id = partIdFactory.getNextPartId();
    doAddPart(id);
    return id;
  }

  public void removePart(String id) {
    Preconditions.checkNotNull(id, "Identifier of part rendering may not be null");
    ContentElement part = doFindPart(id);
    if (part != null) {
      htmlTemplateElement.getMutableDoc().deleteNode(part);
    }
  }

  public void addEventListener(String typeName, JavaScriptObject listener) {
    listeners.add(typeName, listener);
  }

  public void removeEventListener(String typeName, JavaScriptObject listener) {
    listeners.remove(typeName, listener);
  }

  public void setTameDomNodeMaker(JavaScriptObject tameDomNodeMaker) {
    this.tameDomNodeMaker = tameDomNodeMaker;
  }

  public void onHtmlTemplateChildAdded(ContentNode child) {
    if (HtmlTemplate.isPartElement(child)) {
      DomHelper.setContentEditable(child.asElement().getImplNodelet(), true, true);
      firePartAdded(child.asElement().getAttribute(HtmlTemplate.PART_ID_ATTR));
    }
  }

  public void onHtmlTemplateChildRemoved(ContentNode child) {
    if (HtmlTemplate.isPartElement(child)) {
      firePartRemoved(child.asElement().getAttribute(HtmlTemplate.PART_ID_ATTR));
    }
  }

  public void onActivated() {
    fireActivated();
  }

  public void onDeactivated() {
    fireDeactivated();
  }

  public void onNameValuePairAdded(ContentElement nameValuePair) {
    fireDataChanged(
        nameValuePair.getAttribute(HtmlTemplate.NAMEVALUEPAIR_NAME_ATTR),
        null,
        nameValuePair.getAttribute(HtmlTemplate.NAMEVALUEPAIR_VALUE_ATTR));
  }

  public void onNameValuePairRemoved(ContentElement nameValuePair) {
    fireDataChanged(
        nameValuePair.getAttribute(HtmlTemplate.NAMEVALUEPAIR_NAME_ATTR),
        nameValuePair.getAttribute(HtmlTemplate.NAMEVALUEPAIR_VALUE_ATTR),
        null);
  }

  public void onNameValuePairAttributeModified(ContentElement nameValuePair, String name,
      String oldValue, String newValue) {
    fireDataChanged(
        nameValuePair.getAttribute(HtmlTemplate.NAMEVALUEPAIR_NAME_ATTR),
        oldValue,
        newValue);
  }

  private void fireActivated() {
    doSetPartsEditable();
    listeners.fire(EventType.activated, EMPTY_STRING_LIST);
  }

  private void fireDeactivated() {
    listeners.fire(EventType.deactivated, EMPTY_STRING_LIST);
  }

  private void firePartAdded(String id) {
    List<String> data = Arrays.<String>asList("id", id);
    listeners.fire(EventType.partAdded, data);
  }

  private void firePartRemoved(String id) {
    tamePartRenderings.remove(id);
    List<String> data = Arrays.<String>asList("id", id);
    listeners.fire(EventType.partRemoved, data);
  }

  private void fireDataChanged(String name, String oldValue, String newValue) {
    List<String> data = Arrays.<String>asList(
        "name", name,
        "oldValue", oldValue,
        "newValue", newValue);
    listeners.fire(EventType.dataChanged, data);
  }

  private ContentElement doFindPart(String id) {
    for (ContentNode n = htmlTemplateElement.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (HtmlTemplate.isPartElement(n)
          && id.equals(n.asElement().getAttribute(HtmlTemplate.PART_ID_ATTR))) {
        return n.asElement();
      }
    }
    return null;
  }

  private void doAddPart(String id) {
    htmlTemplateElement.getMutableDoc().insertXml(
        Point.<ContentNode>end(htmlTemplateElement),
        XmlStringBuilder.createEmpty()
            .append(
                XmlStringBuilder.createEmpty()
                    .wrap(HtmlTemplate.LINE_TAG))
            .append(
                XmlStringBuilder.createEmpty()
                    .appendText("Hello to all the world"))
            .wrap(HtmlTemplate.PART_TAG,
                HtmlTemplate.PART_ID_ATTR, id));
  }

  private ContentElement doFindNameValuePair(String name) {
    for (ContentNode n = htmlTemplateElement.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (HtmlTemplate.isNameValuePairElement(n)
          && name.equals(n.asElement().getAttribute(HtmlTemplate.NAMEVALUEPAIR_NAME_ATTR))) {
        return n.asElement();
      }
    }
    return null;
  }

  private void doAddNameValuePair(String name, String value) {
    htmlTemplateElement.getMutableDoc().insertXml(
        Point.<ContentNode>end(htmlTemplateElement),
        XmlStringBuilder.createEmpty()
            .wrap(HtmlTemplate.NAMEVALUEPAIR_TAG,
                HtmlTemplate.NAMEVALUEPAIR_NAME_ATTR, name,
                HtmlTemplate.NAMEVALUEPAIR_VALUE_ATTR, value));
  }

  private void doSetPartsEditable() {
    for (String id : getPartIdentifiers()) {
      DomHelper.setContentEditable(getPartRendering(id), true, true);
    }
  }

  private static native JavaScriptObject makeTamePartRendering(
      Element partRendering, JavaScriptObject tameDomNodeMaker) /*-{
    return tameDomNodeMaker(partRendering, true);
  }-*/;

  private static native void invokeEventListener(
      JavaScriptObject listener, JavaScriptObject event) /*-{
    listener.call((void 0), event);
  }-*/;

  private static native JavaScriptObject makeEvent(
      String type, List<String> args, JavaScriptObject caja) /*-{
    var result = { type: type };
    if (args) {
      for (var i = 0; i < args.@java.util.List::size()(); ) {
        result[args.@java.util.List::get(I)(i++)] = args.@java.util.List::get(I)(i++);
      }
    }
    return caja.tameFrozenRecord(result);
  }-*/;

  private static native JavaScriptObject stringListToJSO(List<String> l, JavaScriptObject caja) /*-{
    var result = [];
    for (var i = 0; i < l.@java.util.List::size()(); i++) {
      result.push(l.@java.util.List::get(I)(i));
    }
    return caja.tameFrozenArray(result);
  }-*/;

  private static native JavaScriptObject makeWaveJSOInterface(
      PluginContext ctx, JavaScriptObject caja) /*-{
    return caja.tameFrozenRecord({
      getDataNames: caja.tameFrozenFunc(function() {
        return ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::getDataNamesJSO()
            ();
      }),
      getData: caja.tameFrozenFunc(function(name) {
        return ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::getData(Ljava/lang/String;)
            (String(name));
      }),
      putData: caja.tameFrozenFunc(function(name, value) {
        ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::putData(Ljava/lang/String;Ljava/lang/String;)
            (String(name), String(value));
      }),
      getPartIdentifiers: caja.tameFrozenFunc(function() {
        return ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::getPartIdentifiersJSO()
            ();
      }),
      getPartRendering: caja.tameFrozenFunc(function(id) {
        return ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::getPartRenderingJSO(Ljava/lang/String;)
            (String(id));
      }),
      addPart: caja.tameFrozenFunc(function() {
        return ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::addPart()
            ();
      }),
      removePart: caja.tameFrozenFunc(function(id) {
        ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::removePart(Ljava/lang/String;)
            (String(id));
      }),
      addEventListener: caja.tameFrozenFunc(function(type, l) {
        ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::addEventListener(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
            (String(type), l);
      }),
      removeEventListener: caja.tameFrozenFunc(function(type, l) {
        ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::removeEventListener(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
            (String(type), l);
      }),
      // TODO(ihab): Not a security risk, but think of a better way to expose this setter
      setTameDomNodeMaker: caja.tameFrozenFunc(function(tameDomNodeMaker) {
        ctx.
            @org.waveprotocol.wave.client.doodad.experimental.htmltemplate.PluginContext::setTameDomNodeMaker(Lcom/google/gwt/core/client/JavaScriptObject;)
            (tameDomNodeMaker);
      })
    });
  }-*/;
}

