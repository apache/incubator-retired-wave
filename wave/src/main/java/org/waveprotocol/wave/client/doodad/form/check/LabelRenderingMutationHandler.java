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


package org.waveprotocol.wave.client.doodad.form.check;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;

/**
 * Rendering mutation handler for labels.
 *
 */
public final class LabelRenderingMutationHandler extends RenderingMutationHandler {
  public interface Resources extends ClientBundle {
    @Source("Label.css")
    Css css();
  }

  public interface Css extends CssResource {
    String label();
  }

  /** The singleton instance of our CSS resources. */
  private static final Css css = GWT.<Resources>create(Resources.class).css();

  static {
    // For unit testing using mockito all Gwt.Create() returns mocks.
    // The mock for Resources.class returns null css by default.
    if (css != null) {
      StyleInjector.inject(css.getText(), true);
    }
  }

  private static RenderingMutationHandler instance;

  /**
   */
  public static RenderingMutationHandler getInstance() {
    if (instance == null) {
      instance = new LabelRenderingMutationHandler();
    }
    return instance;
  }

  @Override
  public Element createDomImpl(Renderable element) {
    Element label = Document.get().createLabelElement();
    label.setClassName(css.label());
    return element.setAutoAppendContainer(label);
  }

  @Override
  public void onActivationStart(ContentElement element) {
    fanoutAttrs(element);
  }

  @Override
  public void onAttributeModified(ContentElement element, String name, String oldValue,
      String newValue) {
    if (Label.FOR.equals(name)) {
      getImpl(element).setHtmlFor(element.getEditorUniqueString() + newValue);
    }
  }

  /**
   * @return impl nodelet as {@link LabelElement}
   */
  private LabelElement getImpl(ContentElement element) {
    return LabelElement.as(element.getImplNodelet());
  }
}
