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
package org.waveprotocol.box.stat;

import com.google.gwt.core.shared.GWT;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.waveprotocol.box.stat.Statistic.Entry;

import org.waveprotocol.wave.model.util.Pair;

/**
 * Renderer for statistic.
 *
 * @author David Byttow
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
class StatRenderer {

  /**
   * Represents a profiled request.
   */
  static class ProfiledRequest {
    String uri;
    ExecutionNode node;

    public ProfiledRequest(String uri, ExecutionNode node) {
      this.uri = uri;
      this.node = node;
    }
  }

  private boolean showModule;

  StatRenderer() {
    this.showModule = GWT.isClient();
  }

  String renderHtml(List<Pair<String, Measurement>> measurements, List<ExecutionNode> nodes) {
    StringBuilder builder = new StringBuilder();
    return builder.append("<div style=\"font-size: 0.9em; padding:4px\">")
        .append(renderTitle("Global", 3))
        .append(renderGlobalStats(measurements))
        .append("<br/>")
        .append(renderTitle("Requests", 3))
        .append(renderRequestStats(nodes))
        .append("</div>")
        .toString();
  }

  String renderHtml(Collection<Entry> stats) {
    StringBuilder builder = new StringBuilder();
    for (Statistic.Entry entry : stats) {
      builder.append("<b>")
          .append(entry.getName())
          .append(":</b> ")
          .append(entry.getValue())
          .append("<br/>");
    }
    return builder.toString();
  }

  String renderTitle(String title, int level) {
    StringBuilder builder = new StringBuilder();
    return builder.append("<h")
        .append(level)
        .append(" style='margin-top: 5px; margin-bottom: 0px'>")
        .append(title)
        .append("</h")
        .append(level)
        .append(">")
        .toString();
  }

  String renderRequestStats(List<ExecutionNode> nodes) {
    StringBuilder builder = new StringBuilder();

    beginStatTable(builder, showModule);
    for (int i = nodes.size() - 1; i >= 0; --i) {
      ExecutionNode node = nodes.get(i);
      builder.append(renderNode(node, 0));
    }
    endStatTable(builder);

    return builder.toString();
  }

  String renderNode(ExecutionNode node, int spaces) {
    StringBuilder builder = new StringBuilder();
    Measurement measurement = node.getMeasurement();
    if (showModule) {
      builder.append("<tr style=\"")
          .append(getMeasurementStyle(measurement))
          .append("\"><td><pre style=\"display:inline\">")
          .append(getSpaces(spaces))
          .append("</pre>")
          .append(node.getName() + "</td>")
          .append(" <td>")
          .append(node.getModule() + "</td>")
          .append(" ")
          .append(node.getMeasurement())
          .append("</tr>");
    } else {
      builder.append("<tr style=\"")
          .append(getMeasurementStyle(measurement))
          .append("\"><td><pre style=\"display:inline\">")
          .append(getSpaces(spaces))
          .append("</pre>")
          .append(node.getName() + "</td>")
          .append(" ")
          .append(node.getMeasurement())
          .append("</tr>");
    }
    for (ExecutionNode child : node.getChildren()) {
      builder.append(renderNode(child, spaces + 2));
    }
    return builder.toString();
  }

  private String renderGlobalStats(List<Pair<String, Measurement>> measurements) {
    StringBuilder builder = new StringBuilder();

    Collections.sort(measurements, new Comparator<Pair<String, Measurement>>() {

      @Override
      public int compare(Pair<String, Measurement> p1, Pair<String, Measurement> p2) {
        return -Integer.compare(p1.second.getTotal(), p2.second.getTotal());
      }
    });

    beginStatTable(builder, false);
    for (Pair<String, Measurement> entry : measurements) {
      builder.append("<tr style=\"")
          .append(getMeasurementStyle(entry.second))
          .append("\">")
          .append("<td>" + entry.first + "</td>")
          .append(" ")
          .append(entry.second)
          .append("</tr>");
    }
    endStatTable(builder);

    return builder.toString();
  }

  private static void beginStatTable(StringBuilder builder, boolean showModule) {
    builder.append("<table>");
    builder.append("<tr style=\"font-style: oblique\">");
    builder.append("<td>Name</td>");
    if (showModule) {
      builder.append("<td>Module</td>");
    }
    builder.append("<td>Count</td><td>Average</td><td>Low</td><td>Hight</td><td>Total</td>");
    builder.append("</tr>");
  }

  private static void endStatTable(StringBuilder builder) {
    builder.append("</table>");
  }

  private static String getSpaces(int spaces) {
    char[] array = new char[spaces];
    Arrays.fill(array, ' ');
    return new String(array);
  }

  private static String getMeasurementStyle(Measurement m) {
    if (m.getThreshold() == 0) {
      return "";
    }
    if (m.getAverage() >= m.getThreshold()) {
      return "color: red;";
    } else if (m.getHigh() > m.getThreshold() * 0.5) {
      return "color: orange;";
    }
    return "";
  }
}
