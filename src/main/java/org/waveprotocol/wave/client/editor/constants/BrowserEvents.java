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

package org.waveprotocol.wave.client.editor.constants;

/**
 * List of all events.
 * Events we don't care about handling in the editor are commented out.
 *
 * Add more events if they become known, even if we don't need to handle them.
 * Uncomment in order to handle an event.
 *
 * Only harmless events should be commented - harmful ones that we don't know
 * how to properly handle need to be cancelled.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class BrowserEvents {

  public static final String DOMCharacterDataModified = "DOMCharacterDataModified";
  public static final String DOMNodeInserted = "DOMNodeInserted";

  /** IME composition commencement event */
  public static final String COMPOSITIONSTART = "compositionstart";

  /** IME composition completion event */
  public static final String COMPOSITIONEND = "compositionend";

  /** DOM level 3 composition update event */
  public static final String COMPOSITIONUPDATE = "compositionupdate";

  /** Firefox composition update event */
  public static final String TEXT = "text";

  /** Poorly supported DOM3 event */
  public static final String TEXTINPUT = "textInput";

  /**
   * Array of events the editor listens for
   */
  public static final String[] HANDLED_EVENTS = new String[] {

  //// Category: mouse",
      "click",
      "dblclick",
      "mousedown", // need to flush on mouse down, for typex assumptions
      //"mouseup",
      //"mouseover",
      //"mousemove",
      //"mouseout",
      //"mousewheel",
      "contextmenu",
      //"selectstart",

  //// Category: key",
      "keypress",
      "keydown",
      "keyup",

  //// Category: input
      COMPOSITIONSTART,  // IME events
      COMPOSITIONEND,    // IME events
      COMPOSITIONUPDATE, // IME events
      TEXT,              // IME events
      TEXTINPUT,         // In supported browsers, fired both for IME and non-IME input


  //// Category: mutation",
      //TODO(danilatos): Omit these for IE
      "DOMSubtreeModified",
      DOMNodeInserted,
      "DOMNodeRemoved",
      "DOMNodeRemovedFromDocument",
      "DOMNodeInsertedIntoDocument",
      "DOMAttrModified",
      DOMCharacterDataModified,
      "DOMElementNameChanged",
      "DOMAttributeNameChanged",

      "DOMMouseScroll",

  //// Category: focus",
//      "focus",
//      "blur",
//      "beforeeditfocus",


  //// Category: dragdrop",
// TODO(danilatos): Handle drop events and protect the DOM
//      "drag",
//      "dragstart",
//      "dragenter",
//      "dragover",
//      "dragleave",
//      "dragend",
//      "drop",

  //// Category: frame/object",
//      "load",
//      "unload",
//      "abort",
//      "error",
//      "resize",
//      "scroll",
//      "beforeunload",
//      "stop",

  //// Category: form",
//      "select",
      "change",
      "submit",
      "reset",

  //// Category: ui",
      "domfocusin",
      "domfocusout",
      "domactivate",

  //// Category: clipboard",
      "cut",
      "copy",
      "paste",
      "beforecut",
      "beforecopy",
      "beforepaste",

  //// Category: data binding",
      "afterupdate",
      "beforeupdate",
      "cellchange",
      "dataavailable",
      "datasetchanged",
      "datasetcomplete",
      "errorupdate",
      "rowenter",
      "rowexit",
      "rowsdelete",
      "rowinserted",

  //// Category: misc",
      //"help",

      //"start",  //marquee
      //"finish", //marquee
      //"bounce", //marquee

      //"beforeprint",
      //"afterprint",

      //"propertychange",
      //"filterchange",
      //"readystatechange",
      //"losecapture"
  };


  private BrowserEvents() {}
}
