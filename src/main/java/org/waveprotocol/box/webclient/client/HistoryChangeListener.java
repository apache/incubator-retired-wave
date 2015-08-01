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

package org.waveprotocol.box.webclient.client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

import org.waveprotocol.wave.client.events.ClientEvents;
import org.waveprotocol.wave.client.events.Log;
import org.waveprotocol.wave.client.events.WaveSelectionEvent;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.GwtWaverefEncoder;

/**
 * The listener interface for receiving historyChange events from browser history.
 *
 * @author vjrj@ourproject.org
 *
 * @see {@link ValueChangeEvent}
 */
public class HistoryChangeListener {
  private static final Log LOG = Log.get(HistoryChangeListener.class);

  /**
   * Commonly we start to listen history changes when webclient starts calling this
   * method. If you are using wave client integrated with other different GWT application
   * and with a different History management, you can avoid to use this and just
   * call to the {@link WaveSelectionEvent} events (for example) or other uses.
   */
  public static void init() {
    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        String encodedToken = event.getValue();
        if (encodedToken == null || encodedToken.length() == 0) {
          return;
        }
        WaveRef waveRef;
        try {
          waveRef = GwtWaverefEncoder.decodeWaveRefFromPath(encodedToken);
        } catch (InvalidWaveRefException e) {
          LOG.info("History token contains invalid path: " + encodedToken);
          return;
        }
        LOG.info("Changing selected wave based on history event to " + waveRef.toString());
        ClientEvents.get().fireEvent(new WaveSelectionEvent(waveRef));
      }
    });
  }

  public HistoryChangeListener() {
  }
}
