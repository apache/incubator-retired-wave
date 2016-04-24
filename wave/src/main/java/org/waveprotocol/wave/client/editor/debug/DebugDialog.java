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

package org.waveprotocol.wave.client.editor.debug;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.util.EditorDocFormatter;

import org.waveprotocol.wave.common.logging.InMemoryLogSink;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.common.logging.LogUtils;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.model.document.RangedAnnotation;

import java.util.List;

/**
 * Debug pop up for editor.
 *
 */
final class DebugDialog extends Composite {
  /**
   * Text area for display the impl dom.
   */
  private final TextArea dom = new TextArea();
  /**
   * Text area for displaying the local content xml
   */
  private final TextArea localXml = new TextArea();
  /**
   * Text area for displaying the persistent document
   */
  private final TextArea persistenDocumentContent = new TextArea();

  /**
   * Text area for the annotations
   */
  private final TextArea annotationContent = new TextArea();

  private final EditorImpl editorImpl;

  private final FlowPanel mainPanel = new FlowPanel();

  private final DebugOptions debugOptions;

  /**
   * Enum for the things that can be selected.
   */
  private enum Selection {
    LOCAL_XML("localXml"),
    DOM("dom"),
    PERSISTENT_DOCUMENT("persistentDocument"),
    ANNOTATIONS("annotations"),
    LOG("log"),
    OPTIONS("options");

    /** Display name of this enum value */
    private final String displayName;

    /** Constructor */
    Selection(String displayName){
      this.displayName = displayName;
    }

    @Override
    public String toString(){
      return displayName;
    }

  }

  /**
   * Tracks the content that was previously selected.
   */
  private Selection previousSelection;

  /**
   * Radio button group for selecting which panel to display (content/editor log)
   */
  private static int radioId = 0;
  private final String groupName = "__debug_dialog_selector_" + radioId++;

  // TODO(user): Pull radio button group into its own class.
  private final RadioButton domButton =
      new RadioButton(groupName, Selection.DOM.toString());
  private final RadioButton localXmlButton =
      new RadioButton(groupName, Selection.LOCAL_XML.toString());
  private final RadioButton persistenDocumentButton =
      new RadioButton(groupName, Selection.PERSISTENT_DOCUMENT.toString());
  private final RadioButton annotationButton =
      new RadioButton(groupName, Selection.ANNOTATIONS.toString());
  private final RadioButton logButton =
      new RadioButton(groupName, Selection.LOG.toString());
  private final RadioButton optionsButton =
      new RadioButton(groupName, Selection.OPTIONS.toString());

  private void setChecked(RadioButton button) {
    button.setValue(true);
    if (button == domButton) {
      setNewSelection(Selection.DOM);
    } else if (button == localXmlButton) {
      setNewSelection(Selection.LOCAL_XML);
    } else if (button == persistenDocumentButton){
      setNewSelection(Selection.PERSISTENT_DOCUMENT);
    } else if (button == annotationButton){
      setNewSelection(Selection.ANNOTATIONS);
    } else if (button == logButton){
      setNewSelection(Selection.LOG);
    } else if (button == optionsButton) {
      setNewSelection(Selection.OPTIONS);
    }
  }

  private final ClickHandler radioButtonHandler = new ClickHandler() {
    public void onClick(ClickEvent e) {
      DebugDialog.this.setChecked((RadioButton) e.getSource());
    }
  };

  private static class LogPanel {
    private final InMemoryLogSink log;
    private final DivElement logContainer = Document.get().createDivElement();
    private DivElement lastElement;

    LogPanel(InMemoryLogSink log) {
      this.log = log;
      logContainer.getStyle().setWidth(800, Unit.PX);
      logContainer.getStyle().setHeight(500, Unit.PX);
      logContainer.getStyle().setOverflow(Overflow.SCROLL);
    }

    public void attachTo(Element el) {
      el.appendChild(logContainer);
    }

    public void detach() {
      logContainer.removeFromParent();
    }

    private void appendLogLine(String line) {
      DivElement element = Document.get().createDivElement();
      element.setInnerHTML(line);
      element.getStyle().setProperty("borderBottomStyle", "dotted");
      element.getStyle().setBorderWidth(1, Unit.PX);
      element.getStyle().setBorderColor("#c0c0c0");
      logContainer.appendChild(element);
      lastElement = element;
    }

    public void fillContent() {
      logContainer.setInnerHTML(null);
      List<String> strs = log.showAll();

      for (String s : strs) {
        appendLogLine(s);
      }

      // scroll to the bottom if there's anything to see:
      if (lastElement != null) {
        lastElement.scrollIntoView();
      }
    }

    private final LogSink domSink = new LogSink() {
      @Override
      public void log(Level level, String msg) {
        appendLogLine(msg);
        lastElement.scrollIntoView();
      }

      @Override
      public void lazyLog(Level level, Object... messages) {
        appendLogLine(LogUtils.stringifyLogObject(messages));
        lastElement.scrollIntoView();
      }
    };

    /**
     * If true, keeps the log panel up to date with events from the logger. If
     * false, stop listening to updates.
     *
     * @param enable
     */
    public void showUpdates(boolean enable) {
      if (enable) {
        fillContent();

        log.addLogSink_DO_NOT_USE(domSink);
      } else {
        log.removeLogSink(domSink);
      }
    }
  }

  private final LogPanel logPanel = new LogPanel(DomLogger.logbuffer);

  /**
   * Initialisation code for the TextAreas
   *
   * @param area text area to initialise
   */
  private void initTextArea(TextArea area) {
    area.setReadOnly(true);
    area.setVisibleLines(40);
    area.setCharacterWidth(80);
  }

  /**
   * @param editorImpl
   */
  public DebugDialog(EditorImpl editorImpl) {
    // TODO(user): move inline styles to css + use declarative ui
    this.editorImpl = editorImpl;
    debugOptions = new DebugOptions(editorImpl);
    initWidget(mainPanel);
    mainPanel.add(domButton);
    mainPanel.add(localXmlButton);
    mainPanel.add(persistenDocumentButton);
    mainPanel.add(annotationButton);
    mainPanel.add(logButton);
    mainPanel.add(optionsButton);
    mainPanel.getElement().insertBefore(Document.get().createBRElement(), null);

    previousSelection = Selection.PERSISTENT_DOCUMENT;
    domButton.addClickHandler(radioButtonHandler);
    localXmlButton.addClickHandler(radioButtonHandler);
    persistenDocumentButton.addClickHandler(radioButtonHandler);
    annotationButton.addClickHandler(radioButtonHandler);
    logButton.addClickHandler(radioButtonHandler);
    optionsButton.addClickHandler(radioButtonHandler);
    initTextArea(dom);
    initTextArea(localXml);
    initTextArea(persistenDocumentContent);
    initTextArea(annotationContent);

    // Start with persistent selected.
    setChecked(persistenDocumentButton);
  }

  /**
   * Builds a string representation of the annotations in the current document.
   *
   * @return formatted string of all the annotations in the document
   */
  private String getAnnotations() {
    CMutableDocument doc = editorImpl.mutable();
    int end = doc.size();
    // Grab a cursor over the whole document for our known keys
    Iterable<RangedAnnotation<Object>> rangedAnnotations =
        editorImpl.getContent().getLocalAnnotations().rangedAnnotations(0, end, null);
    StringBuilder retval = new StringBuilder();
    for (RangedAnnotation<Object> ann : rangedAnnotations) {
      if (ann.value() != null) {
        retval.append("(" + ann.start() + "," + ann.end() + ") : " + ann.key() + "=" + ann.value()
            + "\n");
      }
    }

    return retval.toString();
  }

  EditorUpdateEvent.EditorUpdateListener updateListener = new EditorUpdateListener() {
    @Override
    public void onUpdate(EditorUpdateEvent dummy) {
      switch (previousSelection) {
        case DOM:
          dom.setText(EditorDocFormatter.formatImplDomString(editorImpl));
          break;
        case LOCAL_XML:
          localXml.setText(EditorDocFormatter.formatContentDomString(editorImpl));
          break;
        case PERSISTENT_DOCUMENT:
          persistenDocumentContent.setText(
              EditorDocFormatter.formatPersistentDomString(editorImpl));
          break;
        case ANNOTATIONS:
          annotationContent.setText(getAnnotations());
          break;
        case LOG:
          break;
      }
    }
  };

  /**
   * onShow, update to show the editor's content.
   */
  public void onShow() {
    switch (previousSelection) {
      case DOM:
      case LOCAL_XML:
      case PERSISTENT_DOCUMENT:
      case ANNOTATIONS:
        editorImpl.addUpdateListener(updateListener);
        updateListener.onUpdate(null);
        logPanel.showUpdates(false);
        break;
      case LOG:
        logPanel.showUpdates(true);
        editorImpl.removeUpdateListener(updateListener);
        break;
    }
  }

  /**
   * Removes the previously selected content, and displays the new content.
   *
   * @param selection what to display now
   */
  private void setNewSelection(Selection selection) {
    switch (previousSelection) {
      case DOM:
        mainPanel.remove(dom);
        break;

      case LOCAL_XML:
        mainPanel.remove(localXml);
        break;

      case PERSISTENT_DOCUMENT:
        mainPanel.remove(persistenDocumentContent);
        break;

      case ANNOTATIONS:
        mainPanel.remove(annotationContent);
        break;

      case LOG:
        logPanel.detach();
        break;

      case OPTIONS:
        mainPanel.remove(debugOptions.getWidget());
        break;
    }

    switch (selection) {
      case DOM:
        mainPanel.add(dom);
        break;

      case LOCAL_XML:
        mainPanel.add(localXml);
        break;

      case PERSISTENT_DOCUMENT:
        mainPanel.add(persistenDocumentContent);
        break;

      case ANNOTATIONS:
        mainPanel.add(annotationContent);
        break;

      case LOG:
        logPanel.attachTo(mainPanel.getElement());
        break;

      case OPTIONS:
        debugOptions.refresh();
        mainPanel.add(debugOptions.getWidget());
        break;
    }

    previousSelection = selection;

    onShow();
  }

  public void onHide() {
    editorImpl.removeUpdateListener(updateListener);
    logPanel.showUpdates(false);
  }
}
