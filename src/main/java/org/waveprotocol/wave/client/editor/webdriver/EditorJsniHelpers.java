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

package org.waveprotocol.wave.client.editor.webdriver;

/**
 * The purpose of this class is purely to have code that violates that 100 char
 * check in a separate file, because it gets really annoying every time you want
 * to change EditorImpl.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author lars@google.com
 */
public class EditorJsniHelpers {

  /**
   * Drops js functions on window for webdriver tests
   */
  public static native void nativeSetupWebDriverTestPins() /*-{
    $wnd['webdriverEditorGetContent'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetContent(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
    $wnd['webdriverEditorSetContent'] = function(editorDiv, content) {
      @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorSetContent(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;)
          (editorDiv, content);
    }
    $wnd['webdriverEditorGetStartSelection'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetStartSelection(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
    $wnd['webdriverEditorGetEndSelection'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetEndSelection(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
    $wnd['webdriverEditorSetSelection'] = function(editorDiv, start, end) {
      @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorSetSelection(Lcom/google/gwt/dom/client/Element;II)
          (editorDiv, start, end);
    }
    $wnd['webdriverEditorGetEndSelection'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetEndSelection(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
    $wnd['webdriverEditorGetDocDiv'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetDocDiv(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
    $wnd['webdriverEditorGetLocalDiffAnnotations'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetLocalDiffAnnotations(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
    $wnd['webdriverEditorGetLocalContent'] = function(editorDiv) {
      return (
          @org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil::webdriverEditorGetLocalContent(Lcom/google/gwt/dom/client/Element;)
              (editorDiv));
    }
  }-*/;
}
