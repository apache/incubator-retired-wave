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
package org.waveprotocol.box.webclient.stat.gwtevent;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implementation of the {@link StatisticsEventSystem} that dispatches all the
 * GWT events.
 */
public class GwtStatisticsEventSystem implements StatisticsEventSystem {
  private Listeners listeners;
  private boolean enabled;

  public GwtStatisticsEventSystem() {
    this.listeners = new Listeners();
    this.enabled = false;
  }

  public void enable(boolean replay) {
    if (!enabled) {
      enable0(listeners);
      enabled = true;
    }
    if (replay) {
      replay(listeners);
    }
  }

  private native void enable0(StatisticsEventListener listener) /*-{
    var old = $wnd.__stats_listener;
    if (!old) {
      old = function() {};
    }
    $wnd.__stats_listener = function(event) {
      old(event);
      @org.waveprotocol.box.webclient.stat.gwtevent.GwtStatisticsEventSystem::onEvent(Lorg/waveprotocol/box/webclient/stat/gwtevent/GwtStatisticsEvent;Lorg/waveprotocol/box/webclient/stat/gwtevent/StatisticsEventListener;)
        (event, listener);
    };
  }-*/;

  protected static void onEvent(GwtStatisticsEvent event, StatisticsEventListener listener) {
    listener.onStatisticsEvent(event.asEvent());
  }

  //@Override
  public void addListener(StatisticsEventListener listener, boolean replay) {
    if (replay) {
      replay(listener);
    }
    listeners.add(listener);
  }

  private native void replay(StatisticsEventListener listener) /*-{
    var stats = $wnd.__stats;
    if (stats) {
      for (var i in stats) {
        @org.waveprotocol.box.webclient.stat.gwtevent.GwtStatisticsEventSystem::onEvent(Lorg/waveprotocol/box/webclient/stat/gwtevent/GwtStatisticsEvent;Lorg/waveprotocol/box/webclient/stat/gwtevent/StatisticsEventListener;)
          (stats[i], listener);
      }
    }
  }-*/;

  //@Override
  public void removeListener(StatisticsEventListener listener) {
    listeners.remove(listener);
  }


  //@Override
  public native void clearEventHistory() /*-{
    $wnd.__stats.length = 0;
  }-*/;

  //@Override
  public Iterator<StatisticsEvent> pastEvents() {
    return new Iterator<StatisticsEvent>() {
      private int idx = 0;

      //@Override
      public void remove() {
        throw new RuntimeException("Illegal operation. Event history is read-only.");
      }

      //@Override
      public boolean hasNext() {
        return hasNext0(idx);
      }

      private native boolean hasNext0(int idx) /*-{
        var stats = $wnd.__stats;
        return stats && (idx < stats.length);
      }-*/;

      //@Override
      public StatisticsEvent next() {
        return next0(idx++).asEvent();
      }

      private native GwtStatisticsEvent next0(int idx) /*-{
        var stats = $wnd.__stats;
        return (stats && (idx < stats.length)) ? stats[idx] : null;
      }-*/;
    };
  }

  private static class Listeners
      extends ArrayList<StatisticsEventListener> implements StatisticsEventListener {

    public Listeners() {
    }

    //@Override
    public void onStatisticsEvent(StatisticsEvent event) {
      for (StatisticsEventListener l : this) {
        l.onStatisticsEvent(event);
      }
    }
  }
}
