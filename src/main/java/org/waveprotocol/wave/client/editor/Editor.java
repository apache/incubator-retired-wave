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

package org.waveprotocol.wave.client.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.KeySignalListener;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.PainterRegistryImpl;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.RegistriesImpl;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.AnnotationRegistryImpl;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

/**
 * Rich Text Editor interface class. Implementation in {@link EditorImpl}
 *
 * An editor is given a document to edit. The document may be added or removed from the
 * editor as desired.
 *
 * The purpose of the editor is to abstract away the difficulties of dealing
 * with a browser when editing a document of some arbitrary semantic model,
 * rendered into HTML. The editor translates low level browser events and
 * behaviour into high level events for custom content handlers to deal with in
 * a desired manner.
 *
 * Cleaner separation of these concerns is a matter of ongoing improvement
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Editor extends EditorContext {

  /**
   * Base handler registry for all editors
   */
  public static final ElementHandlerRegistry ROOT_HANDLER_REGISTRY = ElementHandlerRegistry.ROOT;

  /**
   * Base annotation handler registry
   */
  public static final AnnotationRegistryImpl ROOT_ANNOTATION_REGISTRY = AnnotationRegistryImpl.ROOT;

  /**
   * Painter registry
   */
  public static final PainterRegistry ROOT_PAINT_REGISTRY = new PainterRegistryImpl(
          AnnotationPaint.SPREAD_FULL_TAGNAME, AnnotationPaint.BOUNDARY_FULL_TAGNAME,
          new AnnotationPainter(
              GWT.isClient() ? SchedulerInstance.getMediumPriorityTimer() : null));

  /**
   * Base registry set
   */
  public static final Registries ROOT_REGISTRIES =
      new RegistriesImpl(ROOT_HANDLER_REGISTRY,
          ROOT_ANNOTATION_REGISTRY, ROOT_PAINT_REGISTRY);

  /**
   * Declare a set of strings corresponding to tags that should
   * have special tabbing.
   */
  public static StringSet TAB_TARGETS = CollectionUtils.newStringSet();

  Widget getWidget();

  /**
   * Initialises the editor (in display mode)
   *
   * @param registries The registries needed for rendering/handling elements and
   *        annotations.
   */
  void init(Registries registries, KeyBindingRegistry keys, EditorSettings settings);

  /**
   * Clears internal state, and makes this editor ready to be re-used. If this
   * editor is never to be re-used, use the stronger {@link #cleanup()} instead.
   *
   * Opposite of init() Called when an editor is not needed for display or
   * editing, but might be re-purposed later by a call to init().
   *
   * TODO(hearnden/danilators): Consider a presenter/view separation for the
   * editor, to reduce the amount of state that needs to be reset here.
   */
  void reset();

  /**
   * Give the editor a sink to send outgoing operations to
   * @param sink
   */
  void setOutputSink(SilentOperationSink<DocOp> sink);

  /**
   * Makes this editor forget any previously registered output sink.
   */
  void clearOutputSink();

  /**
   * @return true if the editor currently contains a document
   */
  boolean hasDocument();

  /**
   * @return the document element after the decorator
   */
  Element getDocumentHtmlElement();

  /**
   * Gets the document state as a tree-structured document.
   * The returned document represents (only) the result of the operation
   * history; it does not include transparent decorations or other transient
   * state.
   *
   * @return the document.
   */
  ContentView getPersistentDocument();

  //TODO(danilatos): Rename these to setDocument(), getDocument(), etc.
  /**
   * Sets the content in the editor from a doc initialization
   *
   * @param op An operation describing the document
   */
  void setContent(final DocInitialization op, DocumentSchema schema);

  /**
   * Place an existing document into the editor
   *
   * @param doc An existing document
   */
  void setContent(ContentDocument doc);

  /**
   * @return The editor's document as an operation to apply to something else.
   *   Useful for serialisation, copying, etc.
   */
  DocInitialization getDocumentInitialization();

  /**
   * @return the current document being edited, or null if none
   */
  ContentDocument getContent();

  /**
   * Removes the editor's document
   *
   * @return the old document. It is still in render mode.
   */
  ContentDocument removeContent();

  /**
   * Removes the editor's document and return it in an unredered state.
   *
   * This is faster than calling {@link #removeContent()} and then
   * turning off rendering as separate steps.
   *
   * @return the old document.
   */
  ContentDocument removeContentAndUnrender();

  /**
   * Starts or stops editing
   * @param editing True is Editor should edit,
   *        false if it should display
   */
  void setEditing(boolean editing);

  /**
   * Must be called when an editor is no longer used anymore, to prevent memory
   * leaks
   */
  void cleanup();

  /**
   * Adds a listener for key signal events.
   * @param listener
   */
  void addKeySignalListener(KeySignalListener listener);

  /**
   * Removes a key signal event listener.
   * @param listener
   */
  void removeKeySignalListener(KeySignalListener listener);

  /**
   * Synchronously flushes any update events
   */
  void flushUpdates();

  /**
   * Show/hide the debug dialog
   */
  void debugToggleDebugDialog();

  /**
   * Synchronously flushes any annotation painting that may have been deferred.
   */
  void flushAnnotationPainting();

  /**
   * Runs any save selection tasks immediately rather than wait for them to be
   * triggered asynchronously.
   */
  void flushSaveSelection();
}
