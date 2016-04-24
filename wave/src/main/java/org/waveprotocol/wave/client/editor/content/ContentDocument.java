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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorAnnotationTree;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorImpl.MiniBundle;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeMutationHandler;
import org.waveprotocol.wave.client.editor.content.ClientDocumentContext.RenderingConcerns;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter.DiffHighlightTarget;
import org.waveprotocol.wave.client.editor.content.ExtendedClientDocumentContext.LowLevelEditingConcerns;
import org.waveprotocol.wave.client.editor.content.SelectionMaintainer.TextNodeChangeType;
import org.waveprotocol.wave.client.editor.content.misc.DisplayEditModeHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.DefaultParagraphHtmlRenderer;
import org.waveprotocol.wave.client.editor.content.paragraph.LineContainerParagraphiser;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException;
import org.waveprotocol.wave.client.editor.extract.RepairListener;
import org.waveprotocol.wave.client.editor.extract.Repairer;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor;
import org.waveprotocol.wave.client.editor.gwt.HasGwtWidget;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.editor.impl.HtmlViewImpl;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.impl.StrippingHtmlView;
import org.waveprotocol.wave.client.editor.impl.TransparencyUtil;
import org.waveprotocol.wave.client.editor.operation.EditorOperationSequencer;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.selection.content.ValidSelectionStrategy;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager;
import org.waveprotocol.wave.client.scheduler.FinalTaskRunner;
import org.waveprotocol.wave.client.scheduler.FinalTaskRunnerImpl;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.indexed.AnnotationSetListener;
import org.waveprotocol.wave.model.document.indexed.IndexedDocumentImpl;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.document.indexed.SimpleAnnotationSet;
import org.waveprotocol.wave.model.document.indexed.StubModifiableAnnotations;
import org.waveprotocol.wave.model.document.indexed.Validator;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.NindoSink;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocIterate;
import org.waveprotocol.wave.model.document.util.ElementManager;
import org.waveprotocol.wave.model.document.util.FilteredView;
import org.waveprotocol.wave.model.document.util.IdentityView;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.LocalAnnotationSetImpl;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.PersistentContent;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.El;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Editor specific extensions of {@link IndexedDocumentImpl} supporting
 * the management of the content-vs-html duality.
 *
 * TODO(danilatos): Document thoroughly.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author lars@google.com (Lars Rasmussen)
 */
public class ContentDocument {

  /**
   * Tag interface to denote mutation handlers that should not be disabled when
   * rendering is turned off.
   */
  public interface PermanentMutationHandler { }

  /**
   * If true, local ops will be validated before application. Set to false to
   * avoid this overhead, but at the rist of permitting invalid ops to be sent
   * out.
   */
  public static boolean validateLocalOps = true;

  /**
   * If true, expensive health checks will be performed if assertions are also
   * on.
   */
  public static boolean performExpensiveChecks = true;

  /**
   * Thrown before a locally generated operation is applied to the document.
   * This prevents its corruption and means that it is still usable and
   * consistent with the server, provided the invalid operation is not then
   * sent out into the op stream.
   */
  public abstract static class LocalOperationException extends RuntimeException {
    private final ViolationCollector violation;

    LocalOperationException(ViolationCollector violation) {
      super("" + violation);
      Preconditions.checkNotNull(violation, "Missing violation information");
      this.violation = violation;
    }

    public ViolationCollector getViolations() {
      return violation;
    }

    @Override
    public String toString() {
      return "LocalOperationException: " + violation;
    }
  }

  public static class SchemaViolatingLocalOperationException extends LocalOperationException {
    SchemaViolatingLocalOperationException(ViolationCollector violation) {
      super(violation);
      Preconditions.checkArgument(
          violation.getValidationResult().isInvalidSchema(),
          "Not a schema violation");
    }
  }

  public static class BadOpLocalOperationException extends LocalOperationException {
    BadOpLocalOperationException(ViolationCollector violation) {
      super(violation);
      Preconditions.checkArgument(
          !violation.getValidationResult().isInvalidSchema(),
          "Not a low level violation");
    }
  }

  /**
   * For easy switching of annotation set for debugging
   */
  public enum AnnotationImplFactory {
    /***/
    SIMPLE {
      @Override
      RawAnnotationSet<Object> create(AnnotationSetListener<Object> annotationListener) {
        return new SimpleAnnotationSet(annotationListener);
      }
    },
    /***/
    TREE {
      @Override
      RawAnnotationSet<Object> create(AnnotationSetListener<Object> annotationListener) {
        return new EditorAnnotationTree(annotationListener);
      }
    },
    /***/
    STUB {
      @Override
      RawAnnotationSet<Object> create(AnnotationSetListener<Object> annotationListener) {
        return new StubModifiableAnnotations<Object>();
      }
    };
    abstract RawAnnotationSet<Object> create(AnnotationSetListener<Object> annotationListener);
  }

  /**
   * For easy switching of annotation set for debugging
   */
  public static AnnotationImplFactory annotationFactory = AnnotationImplFactory.TREE;

  /**
   * Singleton listener
   */
  private SilentOperationSink<? super DocOp> outgoingOperationSink =
      SilentOperationSink.Void.get();

  /**
   * Wraps up the listener also with a call to repaint nodes.
   */
  private final SilentOperationSink<DocOp> outgoingRepaintSink =
      new SilentOperationSink<DocOp>() {
        @Override
        public void consume(DocOp op) {
          outgoingOperationSink.consume(op);
          flushNodeRepaint();
        }};

  /**
   * Sink for locally sourced operations, applying the operation to the document
   * and sending it out.
   */
  private class SourceNindoSink implements NindoSink.Silent {
    private final boolean saveSelection;

    private SourceNindoSink(boolean saveSelection) {
      this.saveSelection = saveSelection;
    }

    @Override
    public DocOp consumeAndReturnInvertible(Nindo op) {
      DocOp docOp = consumeLocal(op, saveSelection);
      outgoingOperationSink.consume(docOp);
      return docOp;
    }
  }

  /**
   * This sink applies the operations locally and to an outgoing sink. When
   * applying locally, it uses SelectionMaintainer to update the selection if
   * neccessary.
   */
  private final NindoSink.Silent nindoSink = new SourceNindoSink(true);

  /**
   * This version of the nindo sink does not save selection. It is intended for
   * use when the DOM is already modified.
   */
  private final NindoSink.Silent dontSaveSelectionNindoSink = new SourceNindoSink(false);

  // NOTE(danilatos): This will become nullable in the future
  private final RenderingConcerns renderingConcerns = new RenderingConcerns() {

    @Override
    public NodeManager getNodeManager() {
      return nodeManager;
    }

    @Override
    public ContentView getRenderedContentView() {
      return ContentDocument.this.getRenderedView();
    }

    @Override
    public FilteredHtml getFilteredHtmlView() {
      return ContentDocument.this.getFilteredHtmlView();
    }

    @Override
    public HtmlView getFullHtmlView() {
      return ContentDocument.this.getRawHtmlView();
    }

    @Override
    public Repairer getRepairer() {
      return repairer;
    }
  };

  private LowLevelEditingConcerns editingConcerns = LowLevelEditingConcerns.STUB;

  /** Used for getting a unique id per document */
  private static int counter = 1;

  /** Document's unique id */
  private final String documentUniqueString = "ed" + counter++ + "_";

  /**
   * Map from name attribute to content element
   */
  private final StringMap<ContentElement> nameMap = CollectionUtils.createStringMap();

  /** List of nodes that we need to repaint. */
  private final List<ContentNode> nodesToRepaint = new ArrayList<ContentNode>();

  private final ContentRawDocument.Factory factory = new ContentRawDocument.Factory() {
    @Override
    public ContentElement createElement(String tagName, Map<String, String> attributes) {
      AgentAdapter el = new AgentAdapter(tagName, attributes,
          context, registries.getElementHandlerRegistry());
      postCreation(el);
      return el;
    }

    @Override
    public void setupBehaviour(ContentElement element) {
      ContentDocument.this.setupBehaviour(element, null);
    }

    @Override
    public ContentTextNode createText(String text) {
      return new ContentTextNode(text, context);
    }
  };

  @SuppressWarnings({"unchecked", "fallthrough", "rawtypes"}) // NodeMutationHandler is generic
  private void setupBehaviour(ContentElement element, Level oldLevel) {
    AgentAdapter e = (AgentAdapter) element;

    ElementHandlerRegistry elementRegistry = registries.getElementHandlerRegistry();

    // bootstrapping for new nodes
    if (oldLevel == null) {
      NodeMutationHandler mutationHandler = elementRegistry.getMutationHandler(e);
      if (mutationHandler instanceof PermanentMutationHandler) {
        e.setNodeMutationHandler(mutationHandler);
      }
      oldLevel = Level.SHELVED;
    }

    boolean notRendered = (e.getRenderer() == AgentAdapter.noRenderer);
    assert notRendered == (oldLevel == Level.SHELVED) : "oldLevel: " + oldLevel + " notRendered:"+notRendered;
    boolean shouldBeRendered = level.isAtLeast(Level.RENDERED);

    // Increasing level
    switch (oldLevel) {
      case SHELVED: // -> RENDERED
        if (level.isAtMost(Level.SHELVED)) break;

        setupRenderer(e, true);
        if (e == fullRawSubstrate.getDocumentElement()) {
          initRootElementRendering(true);
        }
        e.setNodeMutationHandler(elementRegistry.getMutationHandler(e));

      case RENDERED: // -> INTERACTIVE
        if (level.isAtMost(Level.RENDERED)) break;

        e.setNodeEventHandler(elementRegistry.getEventHandler(e));
        maybeSetupGwtWidget(e);

      case INTERACTIVE: // -> EDITING
        if (level.isAtMost(Level.INTERACTIVE)) break;

        maybeSetupModeNotifications(e);
        break;
    }

    // Decreasing level
    switch (oldLevel) {
      case EDITING: // -> INTERACTIVE
        if (level.isAtLeast(Level.EDITING)) break;
        // No need to cleanup mode notifications

      case INTERACTIVE: // -> RENDERED
        if (level.isAtLeast(Level.INTERACTIVE)) break;

        maybeSetupGwtWidget(e);
        e.setNodeEventHandler(null);

      case RENDERED: // -> SHELVED
        if (level.isAtLeast(Level.RENDERED)) break;

        NodeMutationHandler mutationHandler = elementRegistry.getMutationHandler(e);
        if (mutationHandler instanceof PermanentMutationHandler) {
          e.setNodeMutationHandler(mutationHandler);
        } else {
          e.setNodeMutationHandler(null);
        }
        setupRenderer(e, false);

        if (e == fullRawSubstrate.getDocumentElement()) {
          initRootElementRendering(false);
        }
    }

    for (ContentNode n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof ContentElement) {
        setupBehaviour((AgentAdapter) n, oldLevel);
      } else {
        n.asText().setRendering(shouldBeRendered);
      }
    }

    if (notRendered && shouldBeRendered) {
      e.reInsertImpl();
    }

    // Another increasing-level piece of logic, similar to above.
    // if oldLevel < RENDERED && RENDERED <= level
    if (!oldLevel.isAtLeast(Level.RENDERED) && level.isAtLeast(Level.RENDERED)) {
      e.triggerChildrenReady();
    }

    assert checkHealthy(e, false);
  }


  // TODO(danilatos): Kill the postCreation methods

  @SuppressWarnings("unchecked")
  private void postCreation(ContentElement element) {
    maybeAddToNameMap(element);
  }

  /**
   * Adds element to nameMap if it has a name attribute
   * TODO(user): consider a friendly warning if detecting duplicate name here.
   * Would be useful for agent developers...
   *
   * @param element
   */
  private void maybeAddToNameMap(ContentElement element) {
    if (element.hasName()) {
      nameMap.put(element.getName(), element);
    }
  }

  /**
   * Sanity checks for an element
   * @param e
   * @return dummy boolean so the code can be run inside an assert statement
   */
  private boolean checkHealthy(ContentElement e, boolean recursive) {
    if (level.isAtLeast(Level.RENDERED)) {
      if (LineRendering.isLocalParagraph(e)) {
        if (e.getImplNodelet() == null) {
          throw new AssertionError("Local paragraphs have no impl nodelet?");
        }
        if (!e.getImplNodelet().getTagName().equalsIgnoreCase(
                DefaultParagraphHtmlRenderer.PARAGRAPH_IMPL_TAGNAME)
            && !e.getImplNodelet().getTagName().equalsIgnoreCase(
                DefaultParagraphHtmlRenderer.LIST_IMPL_TAGNAME)) {
          throw new AssertionError("Local paragraph impl nodelet is " +
              e.getImplNodelet().getTagName());
        }
        if (e.getContainerNodelet() == null) {
          throw new AssertionError("Local paragraphs have no container nodelet?");
        }
      }
      if (e.getContainerNodelet() != null) {
        for (ContentNode n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
          if (n.getImplNodelet() != null && !n.getImplNodelet().hasParentElement()) {
            throw new AssertionError("Unattached impl nodelet for " + n + " in "+ e);
          }
        }
      }
    }

    if (recursive) {
      for (ContentNode n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
        if (n.getImplNodelet() != null && !n.getImplNodelet().hasParentElement()) {
          if (n.isElement()) {
            checkHealthy(n.asElement(), true);
          }
        }
      }
    }

    return true;
  }

  /**
   * Provides a parent for the GWT Widget if so needed by the doodad
   * @param element
   */
  private void maybeSetupGwtWidget(ContentElement element) {
    if (element instanceof HasGwtWidget) {
      ((HasGwtWidget) element).setLogicalParent(logicalPanel);
    }
  }

  /**
   * Registers element for mode notifications if the it
   * implements HasDisplayEditModes
   *
   * @param element
   */
  private void maybeSetupModeNotifications(ContentElement element) {
    if (editorPackage != null && DisplayEditModeHandler.hasListener(element)) {
      DisplayEditModeHandler.onEditModeChange(element, editorPackage.inEditMode());
      editorPackage.getElementsWithDisplayModes().add(element);
    }
  }

  private void setupRenderer(AgentAdapter e, boolean isRendering) {
    if (isRendering) {
      Renderer renderer = registries.getElementHandlerRegistry().getRenderer(e);
      e.setRenderer(renderer != null ? renderer : AgentAdapter.defaultRenderer);
    } else {
      e.clearRenderer();
    }
  }

  // TODO(danilatos): Reconcile this with the old bundle, and have only one,
  // once the interfaces have stabilised.
  private final ExtendedClientDocumentContext context =
    new ExtendedClientDocumentContext() {

      FinalTaskRunner finalTaskRunner = new FinalTaskRunnerImpl() {
        @Override protected void begin() {
          beginDeferredMutation();
        }

        @Override protected void end() {
          endDeferredMutation();
        }
      };

      @Override
      public LocalDocument<ContentNode, ContentElement, ContentTextNode> annotatableContent() {
        return ContentDocument.this.getAnnotatableContent();
      }

      @Override
      public CMutableDocument document() {
        return ContentDocument.this.getMutableDoc();
      }

      @Override
      public MutableAnnotationSet.Local localAnnotations() {
        return ContentDocument.this.getLocalAnnotations();
      }

      @Override
      public LocationMapper<ContentNode> locationMapper() {
        return ContentDocument.this.getLocationMapper();
      }

      @Override
      public ContentView persistentView() {
        return ContentDocument.this.getPersistentView();
      }

      @Override
      public ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> hardView() {
        return persistentContentView.hardView();
      }

      @Override
      public TextNodeOrganiser<ContentTextNode> textNodeOrganiser() {
        return ContentDocument.this.indexedDoc;
      }

      @Override
      public ElementManager<ContentElement> elementManager() {
        return ContentElement.ELEMENT_MANAGER;
      }

      @Override
      public LowLevelEditingConcerns editing() {
        return editingConcerns;
      }

      @Override
      public String getDocumentId() {
        return documentUniqueString;
      }

      @Override
      public ContentElement getElementByName(String name) {
        return nameMap.get(name);
      }

      @Override
      public boolean isEditing() {
        return editorPackage.inEditMode();
      }

      @Override
      public RenderingConcerns rendering() {
        return renderingConcerns;
      }

      @Override
      public void scheduleFinally(Task task) {
        finalTaskRunner.scheduleFinally(task);
      }

      @Override
      public void beginDeferredMutation() {
        EditorStaticDeps.startIgnoreMutations();
        selectionMaintainer.saveSelection();
      }

      @Override
      public void endDeferredMutation() {
        selectionMaintainer.restoreSelection();
        EditorStaticDeps.endIgnoreMutations();
      }
    };

  /**
   * Presents a view that skips over any ContentNode that does not have a ImpleNodelet.
   */
  public static class RenderedContent
      extends FilteredView<ContentNode, ContentElement, ContentTextNode>
      implements ContentView {

    /***/
    public RenderedContent(ContentView rawView) {
      super(rawView);
    }

    @Override
    protected Skip getSkipLevel(ContentNode node) {
      return node.isRendered() ? Skip.NONE : Skip.SHALLOW;
    }
  }

  /**
   * Full view of the content
   *
   * Wrapper to guard the local mutation methods with selection preservation,
   * and for protection against potential naughty casting to RawDoc.
   */
  public class FullContent
      extends IdentityView<ContentNode, ContentElement, ContentTextNode>
      implements ContentView, LocalDocument<ContentNode, ContentElement, ContentTextNode> {

    FullContent(ReadableDocument<ContentNode, ContentElement, ContentTextNode> inner) {
      super(inner);
    }

    @Override
    public void transparentSetAttribute(ContentElement element, String name, String value) {
      persistentContentView.transparentSetAttribute(element, name, value);
    }

    @Override
    public ContentElement transparentCreate(String tagName,
        Map<String, String> attributes, ContentElement parent,
        ContentNode nodeAfter) {

      selectionMaintainer.saveSelection();
      try {
        return persistentContentView.transparentCreate(tagName, attributes, parent, nodeAfter);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public ContentTextNode transparentCreate(String text, ContentElement parent,
        ContentNode nodeAfter) {
      selectionMaintainer.saveSelection();
      try {
        return persistentContentView.transparentCreate(text, parent, nodeAfter);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public void transparentMove(ContentElement newParent, ContentNode fromIncl,
        ContentNode toExcl, ContentNode refChild) {
      selectionMaintainer.saveSelection();
      try {
        persistentContentView.transparentMove(newParent, fromIncl, toExcl, refChild);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public void transparentUnwrap(ContentElement element) {
      selectionMaintainer.saveSelection();
      try {
        persistentContentView.transparentUnwrap(element);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public void transparentDeepRemove(ContentNode node) {
      selectionMaintainer.saveSelection();
      try {
        persistentContentView.transparentDeepRemove(node);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public <T> T getProperty(Property<T> property, ContentElement element) {
      return persistentContentView.getProperty(property, element);
    }

    @Override
    public boolean isDestroyed(ContentElement element) {
      return persistentContentView.isDestroyed(element);
    }

    @Override
    public <T> void setProperty(Property<T> property, ContentElement element, T value) {
      persistentContentView.setProperty(property, element, value);
    }

    @Override
    public ContentNode transparentSlice(ContentNode splitAt) {
      return persistentContentView.transparentSlice(splitAt);
    }

    @Override
    public void onBeforeFilter(Point<ContentNode> at) {
      persistentContentView.onBeforeFilter(at);
    }

    @Override
    public String toString() {
      return "FullContentView: " + getDocumentElement().toString();
    }

    @Override
    public void markNodeForPersistence(ContentNode localNode, boolean lazy) {
      selectionMaintainer.saveSelection();
      try {
        persistentContentView.markNodeForPersistence(localNode, lazy);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public boolean isTransparent(ContentNode node) {
      return persistentContentView.isTransparent(node);
    }
  }

  /**
   * Factory for creating our substrate, by creating our full raw document and boxing it
   * in our persistent raw doc wrapper.
   */
  public class PersistentContentDoc extends PersistentContentView {
    /** NOTE(patcoleman): Not final for circular construction issues. Only set once. */
    private LazyPersistenceManager persistenceManager;

    private boolean isInsideFilter; /* Flag to avoid callbacks within the filter. */

    /** Constructor */
    public PersistentContentDoc(ContentRawDocument fullRawSubstrate) {
      super(fullRawSubstrate);
    }

    /** Set the persistence manager after construction - only can be called once. */
    void setPersistenceManager(LazyPersistenceManager persistenceManager) {
      Preconditions.checkArgument(persistenceManager != null,
          "Can't use a null persistence manager.");
      Preconditions.checkState(this.persistenceManager == null,
          "Can't set persistence manager twice.");
      this.persistenceManager = persistenceManager;
    }

    @Override
    protected void schedulePaint(final ContentNode node) {
      nodesToRepaint.add(node);
    }

    @Override
    public void markNodeForPersistence(ContentNode localNode, boolean lazy) {
      persistenceManager.markAsLazyPersisted(localNode);
      if (!lazy) {
        persistenceManager.updateLazyNodes(localNode);
      }
    }

    @Override
    public void onBeforeFilter(Point<ContentNode> at) {
      if (isConsistent && !isInsideFilter) {
        isInsideFilter = true;
        persistenceManager.updateLazyNodes(at.getContainer());
        isInsideFilter = false;
      }
    }

    @Override
    public ContentElement createElement(String tagName, Map<String, String> attributes,
        ContentElement parent, ContentNode nodeAfter) {
      // delegate when appropriate:
      if (persistenceManager.isCreationDelegate()) {
        ContentElement node = persistenceManager.createElement(
            tagName, attributes, parent, nodeAfter);
        schedulePaint(node);
        return node;
      }
      return super.createElement(tagName, attributes, parent, nodeAfter);
    }

    @Override
    public ContentTextNode createTextNode(String data, ContentElement parent,
        ContentNode nodeAfter) {
      // delegate when appropriate:
      if (persistenceManager.isCreationDelegate()) {
        ContentTextNode node = persistenceManager.createTextNode(data, parent, nodeAfter);
        schedulePaint(node);
        return node;
      }
      return super.createTextNode(data, parent, nodeAfter);
    }
  }

  /**
   * Presents a view that skips over any HTML node that does not have a back reference to a
   * ContentNode.
   */
  public class FilteredHtml extends FilteredView<Node, Element, Text>
      implements HtmlView, TransparentManager<Element> {
    private final List<Element> invadingElements = new ArrayList<Element>();

    FilteredHtml(ReadableDocument<Node, Element, Text> rawView) {
      super(rawView);
    }

    @Override
    protected Skip getSkipLevel(Node node) {
      // TODO(danilatos): Detect and repair new elements. Currently we just ignore them.
      if (DomHelper.isTextNode(node) || NodeManager.hasBackReference(node.<Element>cast())) {
        return Skip.NONE;
      } else {
        Element element = node.<Element>cast();

        Skip level = NodeManager.getTransparency(element);
        if (level == null) {
          if (!getDocumentElement().isOrHasChild(element)) {
            return Skip.INVALID;
          }
          register(element);
        }
        // For now, we treat unknown nodes as shallow as well.
        // TODO(danilatos): Either strip them or extract them
        return level == null ? Skip.SHALLOW : level;
      }
    }

    @Override
    protected Node getNextOrPrevNodeDepthFirst(ReadableDocument<Node, Element, Text> doc,
        Node start, Node stopAt, boolean enter, boolean rightwards) {

      if (!DomHelper.isTextNode(start) && start.<Element>cast().getPropertyBoolean(
          ContentElement.COMPLEX_IMPLEMENTATION_MARKER)) {

        // If the nodelet is marked as part of a complex implementation structure
        // (e.g. a part of an image thumbnail's implementation), then we find the
        // next html node by instead popping up into the filtered content view,
        // getting the next node from there, and popping back down into html land.
        // This should be both faster and more accurate than doing a depth first
        // search through the impl dom of an arbitrarily complex doodad.

        // Go upwards to find the backreference to the doodad wrapper
        Element e = start.cast();
        while (!NodeManager.hasBackReference(e)) {
          e = e.getParentElement();
        }

        // This must be true, otherwise we could get into an infinite loop
        assert (start == stopAt || !start.isOrHasChild(stopAt));

        // Try to get a wrapper for stopAt as well.
        // TODO(danilatos): How robust is this?
        // What are the chances that it would ever fail in practice anyway?
        ContentNode stopAtWrapper = null;
        for (int tries = 1; tries <= 3; tries++) {
          try {
            stopAtWrapper = nodeManager.findNodeWrapper(stopAt);
            break;
          } catch (InconsistencyException ex) {
            stopAt = stopAt.getParentElement();
          }
        }

        // Do a depth first next-node-find in the content view
        ContentNode next = DocHelper.getNextOrPrevNodeDepthFirst(renderedContentView,
            NodeManager.getBackReference(e),
            stopAtWrapper, enter, rightwards);

        // return the impl nodelet
        return next != null ? next.getImplNodelet() : null;
      } else {

        // In other cases, just do the default.
        return super.getNextOrPrevNodeDepthFirst(doc, start, stopAt, enter, rightwards);
      }
    }


    /**
     * {@inheritDoc}
     */
    public Element needToSplit(Element transparentNode) {
      Element e = transparentNode.cloneNode(false).cast();
      register(e);
      return e;
    }

    /**
     * Clear out any dodgy browser-inserted elements
     */
    public void clearInvadingElements() {
      TransparencyUtil.clear(invadingElements);
    }

    /**
     * Forget the existence of dodgy browser-inserted elements
     */
    public void forgetInvadingElements() {
      invadingElements.clear();
    }

    /**
     * @return list of dodgy browser-inserted elements
     */
    public List<Element> getInvadingElements() {
      return invadingElements;
    }

    /**
     * Register this element as an invading element (i.e. the browser put it there)
     * We'll note it and look after it as a transparent node, rather than just
     * removing it. Then some other smarter code can come and decide what to do with it.
     * @param element
     */
    private void register(Element element) {
      NodeManager.setTransparency(element, Skip.SHALLOW);
      NodeManager.setTransparentBackref(element, this);
      invadingElements.add(element);
    }
  }

  private boolean applyingToDocument = false;

  /**
   * Local annotation set, changes to which are not sent out, but it is implemented
   * in the same data structure as the persistent annotation set.
   */
  private class LocalAnnotationSet extends LocalAnnotationSetImpl
      implements DiffHighlightTarget {

    LocalAnnotationSet(RawAnnotationSet<Object> fullAnnotationSet) {
      super(fullAnnotationSet);
    }

    @Override
    public void startLocalAnnotation(String key, Object value) {
      checkLocalKey(key);
      fullAnnotationSet.startAnnotation(key, value);
    }

    @Override
    public void endLocalAnnotation(String key) {
      checkLocalKey(key);
      fullAnnotationSet.endAnnotation(key);
    }

    @Override
    public ContentNode getCurrentNode() {
      return indexedDoc.getCurrentNode();
    }

    @Override
    public void consume(DocOp op) throws OperationException {
      try {
        ContentDocument.this.consume(op, false, true);
      } catch (OperationRuntimeException e) {
        throw e.get();
      }
    }

    @Override
    public boolean isApplyingToDocument() {
      return applyingToDocument;
    }
  }

  private class ContentIndexedDoc
    extends IndexedDocumentImpl<ContentNode, ContentElement, ContentTextNode, Void> {

    ContentIndexedDoc(PersistentContentDoc doc, RawAnnotationSet<Object> annotations,
        DocumentSchema schema) {
      super(doc, annotations, schema);
    }

    @Override
    public ContentNode getCurrentNode() {
      // NOTE(danilatos): reimplemented to promote visibility to public.
      return super.getCurrentNode();
    }

    /**
     * This method currently also handle nodes not in the persistent view, in which
     * case it adjusts in a forwards direction.
     *
     * {@inheritDoc}
     */
    // TODO(danilatos): A nicer way than munging it here... somehow ensure
    // that we never get invalid points in the first place
    @Override
    public int getLocation(ContentNode node) {
      node = persistentContentView.getVisibleNodeFirst(node);
      return node == null ? size() : super.getLocation(node);
    }

    // TODO(mtsui/danilatos): clean this up later
    private final LocationMapper<ContentNode> indexedDocLocationMapper =
        new LocationMapper<ContentNode>() {
          @Override
          public int getLocation(ContentNode node) {
        return ContentIndexedDoc.super.getLocation(node);
      }

      @Override
      public int getLocation(Point<ContentNode> point) {
        return ContentIndexedDoc.super.getLocation(point);
      }

      @Override
      public Point<ContentNode> locate(int location) {
        return ContentIndexedDoc.super.locate(location);
      }

      @Override
      public int size() {
        return ContentIndexedDoc.super.size();
      }
    };

    /**
     * This method currently also handle nodes not in the persistent view, in which
     * case it adjusts in a forwards direction.
     *
     * {@inheritDoc}
     */
    @Override
    public int getLocation(Point<ContentNode> point) {
      return DocHelper.getFilteredLocation(
          indexedDocLocationMapper, persistentContentView, point);
    }

    @Override
    protected Void evaluate() {
      return super.evaluate();
    }

    @Override
    public ContentTextNode splitText(ContentTextNode textNode, int offset) {
      selectionMaintainer.saveSelection();
      try {
        return super.splitText(textNode, offset);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }

    @Override
    public ContentTextNode mergeText(ContentTextNode secondSibling) {
      selectionMaintainer.saveSelection();
      try {
        return super.mergeText(secondSibling);
      } finally {
        selectionMaintainer.restoreSelection();
      }
    }
  }

  private Element rootElement;

  private final RenderedContent renderedContentView;

  private final PersistentContentDoc persistentContentView;

  private final FullContent fullContentView;

  private final LazyPersistenceManager lazyPersistenceManager;

  private HtmlView rawHtmlView, strippingHtmlView;

  private FilteredHtml filteredHtmlView;

  private final LocalAnnotationSet localAnnotations;

  // Temporarily here during this stage of reshuffling
  private Repairer repairer;

  private NodeManager nodeManager;

  private final ContentIndexedDoc indexedDoc;

  /**
   * This object allows operations to be grouped together and handles selections
   * and suboperations. Operations it sequences are applied to the document and
   * also passed on to the given sink.
   */
  private final EditorOperationSequencer sequencer;

  private final CMutableDocument mutableContent;

  private MiniBundle editorPackage;

  private LogicalPanel logicalPanel;

  private final RawAnnotationSet<Object> fullAnnotationSet;

  private final SelectionMaintainer selectionMaintainer;

  private final FilteredView<ContentNode, ContentElement, ContentTextNode> selectionContent;

  private Registries registries;

  private final ContentRawDocument fullRawSubstrate;

  private final RepairListener repairListener = new RepairListener() {
    @Override
    public void onFullDocumentRevert(
        ReadableDocument<ContentNode, ContentElement, ContentTextNode> doc) {
      if (editorPackage != null) {
        editorPackage.getRepairListener().onFullDocumentRevert(doc);
      }
    }

    @Override
    public void onRangeRevert(El<ContentNode> start, El<ContentNode> end) {
      if (editorPackage != null) {
        editorPackage.getRepairListener().onRangeRevert(start, end);
      }
    }
  };


  /**
   * Constructs a content document with initial registry information, content and a schema.
   */
  public ContentDocument(Registries initialRegistries, DocInitialization initialState,
      DocumentSchema schema) {
    this(schema);
    setRegistries(initialRegistries);
    consume(initialState);
  }

  /**
   * Sets up all documents and handlers needed for the content document.
   */
  public ContentDocument(DocumentSchema documentSchema) {
    AnnotationSetListener<Object> annotationListener = new AnnotationSetListener<Object>() {
      public void onAnnotationChange(int start, int end, String key, Object newValue) {
        EditorStaticDeps.startIgnoreMutations();
        try {
          Iterator<AnnotationMutationHandler> handlers =
              registries.getAnnotationHandlerRegistry().getHandlers(key);
          while (handlers.hasNext()) {
            try {
              handlers.next().handleAnnotationChange(
                  getContext(), start, end, key, newValue);
            } catch (Exception e) {
              // Swallow exceptions from mutation handlers - we don't want to
              // corrupt the document by allowing an exception to escape here.
              EditorStaticDeps.logger.error().log("Exception from annotation change handler", e);
            }
          }
        } finally {
          EditorStaticDeps.endIgnoreMutations();
        }
      }
    };

      // {
      // NOTE(user): This check prevents paste/typing extraction from working
      // TODO(danilatos): Fix this and add it back.
//      @Override
//      public void begin() {
//        if (!isConsistent()) {
//          throw new IllegalStateException("Editor is in a transient state, you " +
//              "may not use the mutable document at this point (try deferring)");
//        }
//        super.begin();
//      }
//    };

    // TODO(danilatos): Parametrise this element, probably in a renderer for
    // the doc root element.

    ContentElement contentRoot = new AgentAdapter(
        ContentDocElement.DEFAULT_TAGNAME, Attributes.EMPTY_MAP,
        // HACK(danilatos): Circular dependency, use root handler registry for now.
        context, Editor.ROOT_HANDLER_REGISTRY);

    sequencer = new EditorOperationSequencer(nindoSink);
    fullAnnotationSet = annotationFactory.create(annotationListener);
    this.localAnnotations = new LocalAnnotationSet(fullAnnotationSet);
    fullRawSubstrate = new ContentRawDocument(contentRoot, factory);
    fullContentView = new FullContent(fullRawSubstrate);
    renderedContentView = new RenderedContent(fullRawSubstrate);

    // NOTE(patcoleman): these rely on eachother, hence the fourth step not being in construction.
    persistentContentView = new PersistentContentDoc(fullRawSubstrate);
    indexedDoc = new ContentIndexedDoc(persistentContentView, fullAnnotationSet, documentSchema);
    lazyPersistenceManager = new LazyPersistenceManager(
        outgoingRepaintSink, fullContentView, indexedDoc, persistentContentView, indexedDoc);
    persistentContentView.setPersistenceManager(lazyPersistenceManager);

    selectionMaintainer = new SelectionMaintainer(indexedDoc);
    mutableContent =  new CMutableDocument(sequencer, indexedDoc);
    selectionContent = ValidSelectionStrategy.buildSelectionFilter(
        persistentContentView, renderedContentView);
  }

  public void setOutgoingSink(SilentOperationSink<? super DocOp> outgoingOperationSink) {
    Preconditions.checkNotNull(outgoingOperationSink, "Outgoing operation sink cannot be null");
    Preconditions.checkState(this.outgoingOperationSink == SilentOperationSink.Void.get(),
        "Already has a sink");
    this.outgoingOperationSink = outgoingOperationSink;
  }

  /**
   * Replaces the existing outgoing sink with a new one.
   *
   * @param newSink new sink to use
   * @return previous sink
   */
  public SilentOperationSink<? super DocOp> replaceOutgoingSink(
      SilentOperationSink<? super DocOp> newSink) {
    Preconditions.checkState(outgoingOperationSink != null, "");
    SilentOperationSink<? super DocOp> oldSink = outgoingOperationSink;
    outgoingOperationSink = newSink;
    return oldSink;
  }

  /**
   * Synchronously flushes any annotation painting that may have been deferred.
   */
  public void flushAnnotationPainting() {
    AnnotationPainter.flush(context);
  }

  /**
   * Brings the editor into a consistent state, possibly asynchronously.
   *
   * @param resume command to run later once the editor has reached consistency,
   *        only if it is not consistent right now
   * @return true if the editor is already consistent, false otherwise. If this
   *         method returns true, {@code resume} will not be executed. If this
   *         method returns {@code false}, {@code resume} will be executed once
   *         the editor is consistent.
   */
  public boolean flush(Runnable resume) {
    return editorPackage != null ? editorPackage.flush(resume) : true;
  }

  public void setShelved() {
    Level oldLevel = adjustLevel(Level.SHELVED);
    setupBehaviour(fullRawSubstrate.getDocumentElement(), oldLevel);
  }

  /**
   * Transitions this document to/from a rendering state.
   */
  public void setRendering() {

    // setupBehaviour deactivates and re-activates rendering, does re-creation
    // of GWT widgets, clobbers event handlers etc. Skip it if possible.
    if (level == Level.RENDERED) {
      return;
    }
    Level oldLevel = adjustLevel(Level.RENDERED);
    setupBehaviour(fullRawSubstrate.getDocumentElement(), oldLevel);
  }

  /**
   * Downgrades the current level to interactive.
   *
   * @throws IllegalStateException if the current level is not at or above
   *         interactive.
   */
  public void setInteractive() {
    Preconditions.checkState(logicalPanel != null, "Don't have a logicalPanel");
    assert level.isAtLeast(Level.INTERACTIVE);

    adjustLevel(Level.INTERACTIVE);
    // No need to setupBehaviour, nothing to do if already interactive, or just
    // leaving edit mode.
  }

  public void setInteractive(LogicalPanel logicalPanel) {
    Preconditions.checkNotNull(logicalPanel, "Null logicalPanel");

    if (this.logicalPanel == logicalPanel) {
      this.setInteractive();
      return;
    }

    this.logicalPanel = logicalPanel;

    Level oldLevel = adjustLevel(Level.INTERACTIVE);
    setupBehaviour(fullRawSubstrate.getDocumentElement(), oldLevel);
  }

  /**
   * Puts the document into the new level. Does not traverse the document
   * to re-render elements and so forth.
   *
   * @return the old level for convenience
   */
  private Level adjustLevel(Level newLevel) {
    Level old = level;

    for (int i = level.ordinal() + 1; i <= newLevel.ordinal(); i++) {
      Level currentLevel = Level.values()[i];

      switch (currentLevel) {
        case RENDERED:
          if (!fullRawSubstrate.getAffectHtml()) {
            fullRawSubstrate.setAffectHtml();
          }
          break;
        case INTERACTIVE:
          assert logicalPanel != null;
          Preconditions.checkState(fullRawSubstrate.getAffectHtml(),
              "rendered or higher state should imply affectHtml");
          break;
        case EDITING:
          // Most is done in attachEditor()
          break;
        default:
          throw new AssertionError("Unknown level " + currentLevel);
      }
    }

    for (int i = level.ordinal() - 1; i >= newLevel.ordinal(); i--) {
      Level currentLevel = Level.values()[i];

      switch (currentLevel) {
        case INTERACTIVE:
          assert editorPackage != null;
          editingConcerns = LowLevelEditingConcerns.STUB;
          editorPackage = null;
          selectionMaintainer.detachEditor();
          break;
        case RENDERED:
          logicalPanel = null;
          break;
        case SHELVED:
          if (fullRawSubstrate.getAffectHtml()) {
            fullRawSubstrate.clearAffectHtml();
          }
          AnnotationPainter.clearDocPainter(context);
          break;
        default:
          throw new AssertionError("Unknown level " + currentLevel);
      }
    }

    level = newLevel;
    return old;
  }

  public void initRootElementRendering(boolean isRendering) {
    if ((rootElement != null) == isRendering) {
      return;
    }

    ContentElement root = fullRawSubstrate.getDocumentElement();

    if (isRendering) {
      this.rootElement = root.getImplNodelet();
      rawHtmlView = new HtmlViewImpl(rootElement);
      filteredHtmlView = new FilteredHtml(rawHtmlView);
      strippingHtmlView = new StrippingHtmlView(rootElement);
      repairer = new Repairer(persistentContentView, renderedContentView, strippingHtmlView,
          repairListener);
      nodeManager = new NodeManager(filteredHtmlView, renderedContentView, repairer);
      assert rootElement == fullRawSubstrate.getDocumentElement().getImplNodelet();
    } else {
      this.rootElement = null;
      rawHtmlView = null;
      filteredHtmlView = null;
      strippingHtmlView = null;
      repairer = null;
      nodeManager = null;
    }
  }

  public enum Level {
    /** Completely unrendered, no HTML manipulations (faster and smaller memory footprint) */
    SHELVED,
    /** Rendered */
    RENDERED,
    /** Event handlers attached */
    INTERACTIVE,
    /** Editor attached */
    EDITING;

    public boolean isAtLeast(Level other) {
      return this.compareTo(other) >= 0;
    }

    public boolean isAtMost(Level other) {
      return this.compareTo(other) <= 0;
    }
  }

  private Level level = Level.SHELVED;

  public Level getLevel() {
    return level;
  }

  /**
   * Attaches an editor to this document.
   *
   * Note that this method does not yet support attaching an editor if the
   * document already has a DOM built up.
   *
   * @param editorBundle
   */
  // TODO(danilatos): Ultimately, remove the document's explicit knowledge
  // of editors altogether.
  public void attachEditor(MiniBundle editorBundle, LogicalPanel panel) {
    Preconditions.checkNotNull(editorBundle, "editorBundle must not be null");
    Preconditions.checkState(level != Level.EDITING,
        "Cannot attach editor to a document already with an editor");

    if (panel == null) {
      Preconditions.checkState(this.logicalPanel != null,
          "Must either already have a logical panel, or one must be provided");
    } else {
      this.logicalPanel = panel;
    }

    Level oldLevel = adjustLevel(Level.EDITING);

    this.editorPackage = editorBundle;

    editingConcerns = new LowLevelEditingConcerns() {

      @Override
      public SelectionHelper getSelectionHelper() {
        // WARNING(danilatos): THIS SHOULD ALWAYS BE THE PASSIVE SELECTION HELPER
        // (As opposed to the aggressive one). Otherwise, lots of subtle bug-inducing
        // side effects could occur.
        return editorPackage.getPassiveSelectionHelper();
      }

      @Override
      public TypingExtractor getTypingExtractor() {
        return editorPackage.getTypingExtractor();
      }

      @Override
      public void textNodeletAffected(Text nodelet, int affectedAfterOffset, int insertionAmount,
          TextNodeChangeType changeType) {
        selectionMaintainer.textNodeletAffected(nodelet, affectedAfterOffset, insertionAmount,
            changeType);
      }

      @Override
      public SuggestionsManager getSuggestionsManager() {
        return editorPackage.getSuggestionsManager();
      }

      /** Returns true */
      @Override
      public boolean hasEditor() {
        return true;
      }

      @Override
      public EditorContext editorContext() {
        return editorPackage.getEditorContext();
      }
    };

    selectionMaintainer.attachEditor(editingConcerns);
    setupBehaviour(fullRawSubstrate.getDocumentElement(), oldLevel);
  }

  public void setRegistries(Registries registriesBundle) {
    registries = registriesBundle;

    if (registries != null) {
      AnnotationPainter.createAndSetDocPainter(getContext(), registries.getPaintRegistry());
    } else {
      AnnotationPainter.clearDocPainter(getContext());
    }
  }

  public Registries getRegistries() {
    return registries;
  }

  /**
   * @return The operation sequencer used by the mutable document
   */
  public EditorOperationSequencer getOpSequencer() {
    return sequencer;
  }

  /**
   * NOTE(danilatos): This method temporary.
   *
   * @return repairer
   */
  public Repairer getRepairer() {
    return repairer;
  }

  /**
   * @return node manager
   */
  public NodeManager getNodeManager() {
    return nodeManager;
  }

  /**
   * @return the persistent view of the document expressed as an operation
   */
  public DocInitialization asOperation() {
    return indexedDoc.asOperation();
  }

  /**
   * @return the document schema
   */
  public DocumentSchema getSchema() {
    return indexedDoc.getSchema();
  }

  /**
   * @return location mapper for the persistent view
   */
  public LocationMapper<ContentNode> getLocationMapper() {
    return indexedDoc;
  }

  /**
   * @return the document context bundle for this document
   */
  public ClientDocumentContext getContext() {
    return context;
  }

  /**
   * Mutable document of this content doc
   */
  public CMutableDocument getMutableDoc() {
    return mutableContent;
  }

  /**
   * @see RenderedContent
   */
  public ContentView getRenderedView() {
    return renderedContentView;
  }

  /**
   * @see PersistentContent
   */
  public ContentView getPersistentView() {
    return persistentContentView;
  }

  /**
   * @see FullContent
   */
  public ContentView getFullContentView() {
    return fullContentView;
  }

  /**
   * @see LocalDocument
   */
  public LocalDocument<ContentNode, ContentElement, ContentTextNode>
      getAnnotatableContent() {
    return fullContentView;
  }

  /**
   * @see HtmlViewImpl
   */
  public HtmlView getRawHtmlView() {
    return rawHtmlView;
  }

  /**
   * @see FilteredHtml
   */
  public FilteredHtml getFilteredHtmlView() {
    return filteredHtmlView;
  }

  /**
   * @see StrippingHtmlView
   */
  public HtmlView getStrippingHtmlView() {
    return strippingHtmlView;
  }

  /**
   * Get the view for valid selection placement.
   */
  public ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> getSelectionFilter() {
    return selectionContent;
  }

  /**
   * Get local annotations. Hack?
   */
  public MutableAnnotationSet.Local getLocalAnnotations() {
    return localAnnotations;
  }

  /**
   * Gets a validator to check schema.
   */
  public Validator getValidator() {
    return indexedDoc;
  }

  /**
   * "Sources" an operation, which means applying it locally and sending it out
   * on the wire.
   *
   * This variant does not affect the html, or move the selection.
   */
  public void sourceNindoWithoutModifyingHtml(Nindo nindo) {
    clearAffectHtml();
    try {
      // NOTE(user): SelectionMaintainer does not know how to save selection
      // when the DOM is already modified, so don't try to save and restore the
      // selection here.
      // Typing extractor already places the selection
      // correctly.
      dontSaveSelectionNindoSink.consumeAndReturnInvertible(nindo);
    } finally {
      setAffectHtml();
    }
  }

  /**
   * Applies the op locally and also sends it out on the wire.
   * @param nindo
   */
  public void sourceNindo(Nindo nindo) {
    nindoSink.consumeAndReturnInvertible(nindo);
  }

  //// Begin DocumentOperationSink impl ////

  /**
   * Applies an operation while disabling DOM mutation events
   *
   * Callers should ensure that if the editor is attached, it should be in a
   * consistent state, before calling this method.
   */
  public void consume(DocOp operation) {
    consume(operation, false, true);
    notifyListener(operation);
  }

  private boolean isConsistent = true;

  private DocOp consumeLocal(Nindo nindo, boolean saveSelection) {

    if (!isConsistent) {
      throw new IllegalStateException("Document is not in a consistent state - " +
          "must have died during a previous bad op");
    }

    // First, validate ops. This is especially important for ops we have sourced,
    // which could be wrong. This should catch most errors before corrupting the
    // document and the CC state (as the server should reject the invalid op as well).
    if (validateLocalOps) {
      try {
        indexedDoc.maybeThrowOperationExceptionFor(nindo);
      } catch (OperationException e1) {
        dealWithBadOp(true, nindo, e1, false);
      }
    }

    isConsistent = false;

    beginConsume(saveSelection);

    DocOp op = null;
    try {
      op = indexedDoc.consumeAndReturnInvertible(nindo, false);
    } catch (OperationException e) {
      dealWithBadOp(true, nindo, e, true);
    } catch (RuntimeException e) {
      dealWithBadOp(true, nindo, e, true);
    }

    endConsume(saveSelection, op);

    return op;
  }

  private void consume(DocOp operation, boolean isLocal, boolean saveSelection) {
    if (!isConsistent) {
      throw new IllegalStateException("Document is not in a consistent state - " +
          "must have died during a previous bad op");
    }

    // First, validate ops. This is especially important for ops we have sourced,
    // which could be wrong. This should catch most errors before corrupting the
    // document and the CC state (as the server should reject the invalid op as well).
    try {
      if (isLocal) {
        indexedDoc.maybeThrowOperationExceptionFor(operation);
      }
    } catch (OperationException e1) {
      dealWithBadOp(isLocal, operation, e1, false);
    }

    isConsistent = false;

    beginConsume(saveSelection);

    try {
      applyingToDocument = true;
      try {
        indexedDoc.consume(operation, false);
      } finally {
        applyingToDocument = false;
      }
    } catch (OperationException e) {
      dealWithBadOp(isLocal, operation, e, true);
    } catch (RuntimeException e) {
      dealWithBadOp(isLocal, operation, e, true);
    }

    endConsume(saveSelection, operation);
  }

  /**
   * Deals with a bad operation and throws the appropriate exception.
   *
   * Does not return.
   *
   * @param isLocal
   * @param operation either an OperationException or a RuntimeException.
   * @param e
   * @param probableCorruption
   */
  private void dealWithBadOp(
      boolean isLocal, Object operation, Exception e, boolean probableCorruption) {

    // Project the exception onto one of its two possible states.
    OperationException oe;
    RuntimeException re;
    if (e instanceof OperationException) {
      oe = (OperationException) e;
      re = null;
    } else {
      oe = null;
      re = (RuntimeException) e;
    }
    String msg =  (probableCorruption ? "DEATH: " : "")
        + "Invalid " + (isLocal ? "LOCAL" : "REMOTE") + " operation: " + operation
        + (oe != null && oe.hasViolationsInformation()
              ? " Violation: " + oe.getViolations().firstDescription()
              : " <No violation information!> ")
        + "Exception: " + e
        + " IndexedDoc: " + indexedDoc.toString();
    EditorStaticDeps.logger.error().logPlainText(msg);

    RuntimeException death;
    if (oe == null) {
      death = re;
    } else if (isLocal && !probableCorruption) {
      assert oe.hasViolationsInformation();
      if (oe.getViolations().getValidationResult().isInvalidSchema()) {
        death = new SchemaViolatingLocalOperationException(oe.getViolations());
      } else {
        death = new BadOpLocalOperationException(oe.getViolations());
      }
    } else {
      death = new OperationRuntimeException("Invalid for current document", oe);
    }

    // If the op failed, the document may be arbitrarily broken state, so there
    // is no reason to believe that a repair will work, let alone execute
    // without throwing its own exceptions. Nevertheless, since we're about to
    // kill the universe by throwing an unchecked exception anyway, it can't
    // hurt to try.
    if (repairer != null) {
      try {
        repairer.revert(fullContentView, fullContentView.getDocumentElement());
      } catch (RuntimeException ex) {
        // Ignore, since a system-killing exception is about to be thrown anyway.
      }
    }

    throw death;
  }

  private void beginConsume(boolean saveSelection) {

    if (selectionMaintainer.isNested()) {
      EditorStaticDeps.logger.error().log("Selection save/restore imbalance!");
      // Recover...
      selectionMaintainer.hackForceClearDepth();
    }

    if (saveSelection) {
      selectionMaintainer.saveSelection();
    } else {
      selectionMaintainer.startDontSaveSelection();
    }

    assert nodesToRepaint.isEmpty();
  }

  private void endConsume(boolean saveSelection, DocOp opApplied) {
    flushNodeRepaint();

    assert debugCheckHealthy();

    if (saveSelection) {
      selectionMaintainer.restoreSelection(opApplied);
    } else {
      selectionMaintainer.endDontSaveSelection();
    }

    isConsistent = true;
  }

  /**
   * Notifies the listener of an incoming op.
   * @param opApplied
   */
  private void notifyListener(DocOp opApplied) {
    if (editorPackage != null) {
      editorPackage.onIncomingOp(opApplied);
    }
  }

  private void flushNodeRepaint() {
    for (ContentNode n : nodesToRepaint) {
      repaintNode(n);
    }
    nodesToRepaint.clear();
  }

  public DiffHighlightTarget getDiffTarget() {
    return localAnnotations;
  }

  @Override
  public String toString() {
    return indexedDoc.toString();
  }

  /**
   * @see ContentRawDocument#clearAffectHtml()
   */
  private void clearAffectHtml() {
    persistentContentView.clearAffectHtml();
  }

  /**s
   * @see ContentRawDocument#clearAffectHtml()
   */
  private void setAffectHtml() {
    persistentContentView.setAffectHtml();
  }

  /** Check that the document is fine. For now, just check the line container is fine. */
  public boolean debugCheckHealthy() {
    if (performExpensiveChecks) {
      for (ContentElement element : DocIterate.deepElements(
          indexedDoc, indexedDoc.getDocumentElement(), null)) {
        if (LineContainers.isLineContainer(indexedDoc, element)) {
          return LineContainerParagraphiser.containerIsHealthyStrong(element);
        }
      }
    }

    return true;
  }

  public boolean debugCheckHealthy2() {
    checkHealthy(fullRawSubstrate.getDocumentElement(), true);
    return debugCheckHealthy();
  }

  private void repaintNode(ContentNode node) {
    if (node.isTextNode()) {
      ContentTextNode textNode = node.asText();
      if (textNode.getParentElement() != null) {
        int start = indexedDoc.getLocation(textNode);
        AnnotationPainter.maybeScheduleRepaint(context,
            start, start + textNode.getLength());
      }
    } else {
      ContentElement element = node.asElement();
      int start = indexedDoc.getLocation(Point.start(indexedDoc, element));
      int end = indexedDoc.getLocation(Point.<ContentNode>end(element));
      AnnotationPainter.maybeScheduleRepaint(context,
          start, end);
    }
  }

  public CMutableDocument createSequencedDocumentWrapper(OperationSequencer<Nindo> sequencer) {
    return new CMutableDocument(sequencer, indexedDoc);
  }

  // Try to get rid of these debug methods eventually

  /**
   * DO NOT USE EXCEPT FOR TESTING!!!
   */
  public ContentRawDocument debugGetRawDocument() {
    return fullRawSubstrate;
  }

  /**
   * DO NOT USE EXCEPT FOR TESTING!!!
   */
  public ExtendedClientDocumentContext debugGetContext() {
    return context;
  }
}
