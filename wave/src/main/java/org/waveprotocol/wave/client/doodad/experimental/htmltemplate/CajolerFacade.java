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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.HTML;

import org.waveprotocol.wave.client.doodad.experimental.htmltemplate.CajoleService.CajolerResponse;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Interface to the Caja machinery that prepares the surrounding native HTML
 * page and instantiates each HtmlTemplate doodad within a GWT HTML widget
 * supplied by the client.
 *
 * @author jasvir@google.com (Jasvir Nagra)
 * @author ihab@google.com (Ihab Awad)
 */
class CajolerFacade {
  interface Css extends CssResource {
    String outerHull();

    String innerHull();
  }

  interface Resources extends ClientBundle {
    @Source("secureStyles.css")
    Css secureStyles();

    @Source("supporting_js.jslib")
    TextResource supportingJs();

    @Source("taming.js")
    TextResource taming();
  }

  private static final Resources RESOURCES = GWT.create(Resources.class);

  static {
    StyleInjector.inject(RESOURCES.secureStyles().getText(), true);
  }

  // Enable to true once tested more thoroughly.
  private static final CajolerFacade instance = create();

  // Enable once a cache replacement policy is established.  Timeout?
  private static final boolean ENABLE_CACHE = false;

  private final CajoleService cajoleService;
  private final IFrameElement cajaFrame;
  private final StringMap<CajolerResponse> cache = CollectionUtils.createStringMap();

  public static CajolerFacade instance() {
    return instance;
  }

  private static CajolerFacade create() {
    IFrameElement cajaFrame = createCajaFrame();
    CajoleService service = new HttpCajoleService();
    return new CajolerFacade(service, cajaFrame);
  }

  private static IFrameElement createCajaFrame() {
    IFrameElement cajaFrame = Document.get().createIFrameElement();
    cajaFrame.setFrameBorder(0);
    cajaFrame.setAttribute("width", "0");
    cajaFrame.setAttribute("height", "0");
    Document.get().getBody().appendChild(cajaFrame);
    Document cajaFrameDoc = cajaFrame.getContentDocument();
    cajaFrameDoc.getBody().appendChild(
        cajaFrameDoc.createScriptElement(RESOURCES.supportingJs().getText()));
    cajaFrameDoc.getBody().appendChild(
        cajaFrameDoc.createScriptElement(RESOURCES.taming().getText()));
    return cajaFrame;
  }

  private CajolerFacade(CajoleService service, IFrameElement cajaFrame) {
    this.cajoleService = service;
    this.cajaFrame = cajaFrame;
  }

  private void appendToDocument(HTML target, PluginContext pluginContext, CajolerResponse response) {
    DivElement domitaVdocElement = Document.get().createDivElement();
    domitaVdocElement.setClassName("innerHull");

    target.getElement().setInnerHTML("");
    target.getElement().setClassName("outerHull");
    target.getElement().appendChild(domitaVdocElement);

    initializeDoodadEnvironment(
        cajaFrame, domitaVdocElement, pluginContext.getJSOInterface());

    // Render HTML
    domitaVdocElement.setInnerHTML(response.getHtml());

    // Inject JS
    Document cajaFrameDoc = cajaFrame.getContentDocument();
    cajaFrameDoc.getBody().appendChild(cajaFrameDoc.createScriptElement(response.getJs()));
  }

  private static native void initializeDoodadEnvironment(
      Element cajaFrame, Element domitaVdocElement, JavaScriptObject wave) /*-{
    cajaFrame.contentWindow.caja___.initialize(domitaVdocElement, wave);
  }-*/;

  private void handleSuccessfulResponse(
      String url, HTML target, PluginContext pluginContext, CajolerResponse cajoled) {
    if (ENABLE_CACHE) {
      cache.put(url, cajoled);
    }

    appendToDocument(target, pluginContext, cajoled);
  }

  /**
   * Given a GWT {@code HTML} widget, instantiate an HTML Template doodad within
   * that widget.
   *
   * @param target a GWT {@code HTML} widget. The contents of this widget will
   *        be cleared out and replaced with the dynamic content created by the
   *        HTML Template doodad.
   * @param pluginContext an object which will be exposed to the HTML Template
   *        doodad as a global window variable named {@code wave}. This
   *        represents the Wave API to the doodad's JavaScript.
   * @param url the URL of an HTML file that will be cajoled (via Caja) and
   *        instantiated as the contents of the supplied {@code HTML} widget.
   */
  // This should be @SuppressWarnings("deadCode"), to ignore the branch disabled
  // by the ENABLE_CACHE value, but Eclipse does not recognise "deadCode".
  @SuppressWarnings("all")
  public void instantiateDoodadInHTML(
      final HTML target, final PluginContext pluginContext, final String url) {
    if (ENABLE_CACHE && cache.containsKey(url)) {
      SchedulerInstance.getMediumPriorityTimer().schedule(new Task() {
        @Override
        public void execute() {
          // Use isAttached as a cheap approximation of doodad still being active.
          if (target.isAttached()) {
            appendToDocument(target, pluginContext, cache.get(url));
          }
        }
      });
    } else {
      cajoleService.cajole(url, new Callback<CajolerResponse>() {
        @Override
        public void onSuccess(CajolerResponse cajoled) {
          handleSuccessfulResponse(url, target, pluginContext, cajoled);
        }

        @Override
        public void onError(String message) {
          // log.
        }
      });
    }
  }

  public JavaScriptObject getTaming() {
    return doGetCajaObject(cajaFrame);
  }

  private static native JavaScriptObject doGetCajaObject(JavaScriptObject cajaFrame) /*-{
    return cajaFrame.contentWindow.caja___;
  }-*/;
}
