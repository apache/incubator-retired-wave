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

import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;

import java.util.Iterator;

/**
 * Transformer of GWT statistic.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class GwtStatisticsHandler implements StatisticsEventListener {
  static private String GWT_PREFIX = "GWT.";

  static private String PARAM_TYPE = "type";
  static private String TYPE_BEGIN = "begin";
  static private String TYPE_END = "end";

  private Timer requestTimer;
  private String currentGroup;
  private long previousCallTimestamp;

  @Override
  public void onStatisticsEvent(StatisticsEvent event) {
    String group = event.getEventGroupKey();
    String type = (String)event.getExtraParameter(PARAM_TYPE);
    if (TYPE_BEGIN.equals(type)) {
      enterGroup(group, (long)event.getMillis());
    } else {
      if (TYPE_END.equals(type)) {
        leaveGroup((long)event.getMillis());
      } else {
        if (!group.equals(currentGroup)) {
          leaveGroup((long)event.getMillis());
          enterGroup(group, (long)event.getMillis());
        }
        Timing.record(GWT_PREFIX + type, (int)((long)event.getMillis()-previousCallTimestamp));
      }
    }
    previousCallTimestamp = (long)event.getMillis();
  }

  private void enterGroup(String group, long time) {
    if (Timing.isEnabled()) {
      Timing.enterScope();
      requestTimer = Timing.startRequest(GWT_PREFIX + group);
      requestTimer.start(time);
      currentGroup = group;
    }
  }

  private void leaveGroup(long time) {
    if (Timing.isEnabled() && currentGroup != null) {
      Timing.stop(requestTimer, time);
      requestTimer = null;
      currentGroup = null;
      Timing.exitScope();
    }
  }

  private static String getExtraParameters(StatisticsEvent event) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    Iterator<String> names = event.getExtraParameterNames();
    if (names.hasNext()) {
      String n = names.next();
      sb.append(n).append(" = ").append(event.getExtraParameter(n));
      while (names.hasNext()) {
        sb.append(", ").append(n = names.next()).append(" = ").append(event.getExtraParameter(n));
      }
    }
    sb.append("}");
    return sb.toString();
  }
}
