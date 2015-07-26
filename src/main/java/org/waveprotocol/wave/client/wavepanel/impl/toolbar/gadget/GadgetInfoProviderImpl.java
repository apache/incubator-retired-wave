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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.LoggerBundle;

import java.util.ArrayList;
import java.util.List;

public class GadgetInfoProviderImpl implements GadgetInfoProvider {
  private final static LoggerBundle LOG = new DomLogger("GadgetInfoProvider");
  public static final String GADGET_INFO_PATH = "/gadget/gadgetlist";

  private ArrayList<GadgetInfo> gadgetList;
  private Listener listener;
  private GadgetInfoParser parser;

  public GadgetInfoProviderImpl(GadgetInfoParser parser) {
    this.parser = parser;
    gadgetList = new ArrayList<GadgetInfo>();
  }

  @VisibleForTesting
  void addGadgetJson(String json) {
    gadgetList.addAll(parser.parseGadgetInfoJson(json));
  }

  private void notifyListener() {
    if (listener != null) {
      listener.onUpdate();
    }
  }

  /**
   * @see org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProviderInterface#getGadgetInfoList(String,
   *      String)
   */
  public List<GadgetInfo> getGadgetInfoList(String filter, String category) {
    List<GadgetInfo> filteredList = new ArrayList<GadgetInfo>();
    String lowerCaseFilter = filter.toLowerCase();
    for (GadgetInfo gadget : gadgetList) {
      String gadgetName = gadget.getName().toLowerCase();
      String gadgetDesc = gadget.getDescription().toLowerCase();
      if ((gadgetName.contains(lowerCaseFilter) || gadgetDesc.contains(lowerCaseFilter))
          && (category.equals(GadgetCategoryType.ALL.getType())
              || category.equals(gadget.getPrimaryCategory().getType()) || category.equals(gadget
              .getSecondaryCategory().getType()))) {
        filteredList.add(gadget);
      }
    }
    return filteredList;
  }

  /**
   * @see org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProviderInterface#setListener(org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProviderInterface.Listener)
   */
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * @see org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProviderInterface#startLoadingGadgetList()
   */
  public void startLoadingGadgetList() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, GADGET_INFO_PATH);
    try {
      builder.sendRequest(null, new RequestCallback() {
        public void onError(Request request, Throwable exception) {
          LOG.log(Level.ERROR, "Error fetching gadget info list from server: " + exception);
        }

        public void onResponseReceived(Request request, Response response) {
          if (200 == response.getStatusCode()) {
            String jsonResponse = response.getText();
            addGadgetJson(jsonResponse);
            notifyListener();
            return;
          } else {
            LOG.log(Level.ERROR, "Server responded with a HTTP error: " + response.getStatusCode());
          }
        }
      });
    } catch (RequestException e) {
      LOG.log(Level.ERROR, "Exception while sending HTTP request: " + e.toString());
    }
  }
}
