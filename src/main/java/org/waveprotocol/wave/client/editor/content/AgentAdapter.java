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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.NodeMutationHandler;
import org.waveprotocol.wave.client.editor.NodeMutationHandlerImpl;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler;
import org.waveprotocol.wave.client.editor.gwt.HasGwtWidget;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Map;

/**
 * AgentAdapter is composed of NodeEventHandler, NodeMutationHandler and
 * Renderer.
 *
 * This extends ContentElement for backwards compatibility, but calls are
 * delegated to the appropriate handlers.
 *
 * NOTE(user): This adapter is only temporary.
 *
 */
public class AgentAdapter extends ContentElement implements
                                                 MutatingNode<ContentNode, ContentElement>,
                                                 HasGwtWidget {
  public interface Resources extends ClientBundle {
    @Source("Default.css")
    Css css();
  }

  public interface Css extends CssResource {
    /** Unknown element */
    String unknown();
  }

  /** The singleton instance of our CSS resources. */
  public static final Css css =
      GWT.isClient() ? GWT.<Resources>create(Resources.class).css() : null;

  /**
   * Register schema + inject stylesheet
   */
  static {
    // For unit testing using mockito all Gwt.Create() returns mocks.
    // The mock for Resources.class returns null css by default.
    if (css != null) {
      StyleInjector.inject(css.getText(), true);
    }
  }

  /**
   * Stub mutation handler
   */
  static final NodeMutationHandlerImpl<ContentNode, ContentElement>
      defaultMutationHandler = new NodeMutationHandlerImpl<ContentNode, ContentElement>();

  static final NodeEventHandler defaultEventHandler = NodeEventHandlerImpl.get();

  /**
   * By default, elements are rendered by using their impl nodelet, and the
   * attachment point for child elements' impl nodelets is also the impl nodelet
   */
  static final Renderer defaultRenderer = new Renderer() {
  // TODO(danilatos): fix tests so that this works - many assume the impl nodelet has
  // the same tag name as the content element, this would change that.
    @Override
    public Element createDomImpl(Renderable element) {
      Element unknown = Document.get().createDivElement();
      unknown.setClassName(css.unknown());
      unknown.setInnerText("<" + element.getTagName() + ">");
      DomHelper.setContentEditable(unknown, false, false);
      DomHelper.makeUnselectable(unknown);
      return unknown;
    }
  };

  /**
   * This has the same behaviour as emptyRenderer, but is used differently.
   * noRenderer means we are not currently rendering this element.
   * emptyRenderer means we are in a rendering context but we choose not to render.
   */
  static final Renderer noRenderer = new Renderer() {
    @Override
    public Element createDomImpl(Renderable element) {
      return null;
    }
  };


  static final Renderer emptyRenderer = new Renderer() {
    @Override
    public Element createDomImpl(Renderable element) {
      return null;
    }
  };

  NodeEventHandler nodeEventHandler = defaultEventHandler;
  private NodeMutationHandler<ContentNode, ContentElement> nodeMutationHandler =
      defaultMutationHandler;
  private Renderer renderer = noRenderer;
  private ElementHandlerRegistry registry;
  private final ContentElement element = this;

  public AgentAdapter(String tagName, Map<String, String> attributes,
      ExtendedClientDocumentContext bundle, ElementHandlerRegistry registry) {
    this(tagName, attributes, bundle, registry, true);
  }

  /**
   * @param bundle
   * @param registry The handler registry associated with the document
   *   the element belongs to
   */
  public AgentAdapter(String tagName, Map<String, String> attributes,
      ExtendedClientDocumentContext bundle, ElementHandlerRegistry registry, boolean isRendered) {
    super(tagName, bundle, true);

    setRegistry(registry);
    init(attributes);
  }

  // NOTE(danilatos): The registry member variable is only needed in one place
  // where some code gets the "nice html renderer" from an element. Delete this
  // once that's gone.
  public void setRegistry(ElementHandlerRegistry registry) {
    this.registry = registry;
  }

  public ElementHandlerRegistry getRegistry() {
    return registry;
  }

  Renderer getRenderer() {
    return renderer;
  }

  /**
   * See {@link #noRenderer} This should not be used for hidden elements - it
   * should be used only when we turn off rendering altogether. For hidden
   * elements, use {@link #emptyRenderer}
   */
  void clearRenderer() {
    this.setRenderer(noRenderer);
  }

  void setRenderer(Renderer renderer) {
    Preconditions.checkNotNull(renderer, "Null renderer");
    if (this.renderer == renderer) {
      return;
    }

    this.renderer = renderer;

    Element oldNodelet = getImplNodelet();

    // This is a tad messy, because the createDomImpl method might set the container nodelet
    // itself, before we get the impl nodelet.
    setContainerNodelet(null); // clear any old container nodelet just in case
    setImplNodelets(renderer.createDomImpl(this), getContainerNodelet()); // reset both nodelets
    assert getImplNodelet() == null || NodeManager.getBackReference(getImplNodelet()) == this;

    if (oldNodelet != null) {
      oldNodelet.removeFromParent();
    } else {
    }
  }



  /**  Public for testing purposes ONLY */
  @VisibleForTesting public void debugSetNodeMutationHandler(
      NodeMutationHandler<ContentNode, ContentElement> handler) {
    setNodeMutationHandler(handler);
  }


  void setNodeMutationHandler(NodeMutationHandler<ContentNode, ContentElement> handler) {
    assert isContentAttached() || getMutableDoc().getDocumentElement() == this;

    handler = ValueUtils.valueOrDefault(handler, defaultMutationHandler);
    if (this.nodeMutationHandler == handler) {
      return;
    }

    this.nodeMutationHandler.onDeactivated(this);
    this.nodeMutationHandler = handler;
    this.nodeMutationHandler.onActivationStart(this);
  }

  /**
   * Called when {@link NodeMutationHandler#onActivatedSubtree(Object)} should be
   * called. See javadoc for that method for details.
   */
  void triggerChildrenReady() {
    this.nodeMutationHandler.onActivatedSubtree(this);
  }

  void setNodeEventHandler(NodeEventHandler newHandler) {
    assert isContentAttached() || getMutableDoc().getDocumentElement() == this;

    newHandler = ValueUtils.valueOrDefault(newHandler, defaultEventHandler);
    if (this.nodeEventHandler == newHandler) {
      return;
    }
    this.nodeEventHandler.onDeactivated(this);
    this.nodeEventHandler = newHandler;
    this.nodeEventHandler.onActivated(this);
  }

  // Special behaviour for these two, hence not with the mechanically generated ones

  @Override
  public final void onAddedToParent(ContentElement oldParent) {
    nodeMutationHandler.onAddedToParent(element, oldParent);
  }

  @Override
  public final void onRemovedFromParent(ContentElement newParent) {
    nodeMutationHandler.onRemovedFromParent(element, newParent);

    if (newParent == null) {
      this.nodeEventHandler.onDeactivated(this);
      this.nodeMutationHandler.onDeactivated(this);
      this.renderer = null;
      this.nodeMutationHandler = null;
      this.nodeEventHandler = null;
    }
  }

  //// NodeMutationHandler delegators
  @Override
  protected final void onRepair() {
    // Call activated, so the handler can essentially treat the node from
    // scratch again.
    nodeMutationHandler.onActivationStart(this);
  }

  @Override
  public final void onDescendantsMutated() {
    nodeMutationHandler.onDescendantsMutated(element);
  }

  @Override
  public final void onEmptied() {
    nodeMutationHandler.onEmptied(element);
  }

  @Override
  public final void onAttributeModified(String name, String oldValue, String newValue) {
    nodeMutationHandler.onAttributeModified(element, name, oldValue, newValue);
  }

  @Override
  public final void onChildAdded(ContentNode child) {
    nodeMutationHandler.onChildAdded(element, child);
  }

  @Override
  public final void onChildRemoved(ContentNode child) {
    nodeMutationHandler.onChildRemoved(element, child);
  }

  ////

  // TODO(danilatos): Get rid of this hack, which is a temporary measure to get
  // gwt event hookups to work for doodads that are rendered by gwt widgets.
  public void setLogicalParent(LogicalPanel parent) {
    if (renderer instanceof GwtRenderingMutationHandler) {
      ((GwtRenderingMutationHandler) renderer).setLogicalPanel(this, parent);
    }
  }
}
