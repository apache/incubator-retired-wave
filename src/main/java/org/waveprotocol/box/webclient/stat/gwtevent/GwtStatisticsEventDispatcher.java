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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stats event dispatcher that uses the GWT global stats function to dispatch
 * events.
 */
public class GwtStatisticsEventDispatcher implements StatisticsEventDispatcher {

  //@Override
  public boolean enabled() {
    return RemoteServiceProxy.isStatsAvailable();
  }

  //@Override
  public StatisticsEvent newEvent(String system, String group, double millis, String type) {
    Event event = new Event(GWT.getModuleName(), system, group, millis);
    if (type != null) {
      setExtraParameter(event, "type", type);
    }
    return event;
  }

  //@Override
  public void setExtraParameter(StatisticsEvent event, String name, String value) {
    ((Event) event).set(name, value);
  }

  //@Override
  public void setExtraParameter(StatisticsEvent event, String name, JavaScriptObject value) {
    ((Event) event).set(name, value);
  }

  //@Override
  public void dispatch(StatisticsEvent event) {
    dispatch0(GwtStatisticsEvent.fromEvent(event));
  }

  private native void dispatch0(JavaScriptObject event) /*-{
    $stats && $stats(event);
  }-*/;

  private static class Event implements StatisticsEvent {
    private String module;
    private String system;
    private String group;
    private double millis;
    private Map<String, Object> params;

    public Event(String module, String system, String group, double millis) {
      this.module = module;
      this.system = system;
      this.group = group;
      this.millis = millis;
      this.params = new HashMap<String, Object>();
    }

    //@Override
    public String getModuleName() {
      return module;
    }

    //@Override
    public String getSubSystem() {
      return system;
    }

    //@Override
    public String getEventGroupKey() {
      return group;
    }

    //@Override
    public double getMillis() {
      return millis;
    }

    //@Override
    public Iterator<String> getExtraParameterNames() {
      return params.keySet().iterator();
    }

    //@Override
    public Object getExtraParameter(String name) {
      return params.get(name);
    }

    protected void set(String name, Object value) {
      params.put(name, value);
    }
  }
}
