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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import java.util.Iterator;

/**
 * Java "Overlay" object of the event objects fired by the GWT stats system.
 */
public class GwtStatisticsEvent extends JavaScriptObject /*implements StatisticsEvent*/ {
  protected GwtStatisticsEvent() {
  }

  public final native String getModuleName() /*-{
    return this.moduleName == null ? null : "" + this.moduleName;
  }-*/;

  public final native String getSubSystem() /*-{
    return this.subSystem == null ? null : "" + this.subSystem;
  }-*/;

  public final native String getEventGroupKey() /*-{
    return this.evtGroup == null ? null : "" + this.evtGroup;
  }-*/;

  public final native double getMillis() /*-{
    return this.millis == null ? 0 : this.millis;
  }-*/;

  private final native JsArrayString getExtraParameterNames0() /*-{
    if (!this.extraParameters) {
      var a = new Array();
      for (name in this) {
        if (name != "moduleName" && name != "subSystem" && name != "evtGroup" && name != "millis") {
          a.push(name);
        }
      }
      this.extraParameters = a;
    }
    return this.extraParameters
  }-*/;

  public final native Object getExtraParameter(String name) /*-{
    var r = this[name], t = typeof(r);
    if (t == "number") {
      r = @java.lang.Double::new(D)(r);
    } else if (t == "boolean") {
      r = @java.lang.Boolean::new(Z)(r);
    }
    return r;
  }-*/;

  public final StatisticsEvent asEvent() {
    return new StatisticsEvent() {
      //@Override
      public String getModuleName() {
        return GwtStatisticsEvent.this.getModuleName();
      }
      //@Override
      public String getSubSystem() {
        return GwtStatisticsEvent.this.getSubSystem();
      }
      //@Override
      public String getEventGroupKey() {
        return GwtStatisticsEvent.this.getEventGroupKey();
      }
      //@Override
      public double getMillis() {
        return GwtStatisticsEvent.this.getMillis();
      }
      //@Override
      public Iterator<String> getExtraParameterNames() {
        final JsArrayString names = getExtraParameterNames0();
        return new Iterator<String>() {
          private int idx = 0;

          //@Override
          public boolean hasNext() {
            return idx < names.length();
          }

          //@Override
          public String next() {
            return names.get(idx++);
          }

          //@Override
          public void remove() {
            throw new RuntimeException("parameter names are read-only");
          }
        };
      }
      //@Override
      public Object getExtraParameter(String name) {
        return GwtStatisticsEvent.this.getExtraParameter(name);
      }
    };
  }

  public static GwtStatisticsEvent fromEvent(StatisticsEvent event) {
    GwtStatisticsEvent result = fromEvent0(event.getModuleName(), event.getSubSystem(),
        event.getEventGroupKey(), event.getMillis());
    for (Iterator<String> it = event.getExtraParameterNames(); it.hasNext(); ) {
      String name = it.next();
      set(result, name, event.getExtraParameter(name));
    }
    return result;
  }

  private static native GwtStatisticsEvent fromEvent0(String moduleName, String subSystem,
      String eventGroup, double millis) /*-{
    var result = {
      moduleName: moduleName,
      subSystem: subSystem,
      evtGroup: eventGroup,
      millis: millis,
    };
    return result;
  }-*/;

  private static native void set(GwtStatisticsEvent event, String name, Object value) /*-{
    event[name] = value;
  }-*/;
}
