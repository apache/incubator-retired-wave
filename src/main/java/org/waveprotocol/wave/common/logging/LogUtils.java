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

package org.waveprotocol.wave.common.logging;

/**
 * Static utility methods for logging.
 *
 */
public final class LogUtils {
  // TODO(user): Why do we need to limit the stack frames in the logs?
  // If we don't, remove this and the parameter on LogUtils.printStackTrace
  private static final int MAX_STACK_FRAME = 12;

  /**
   * Utility class, not constructable.
   */
  private LogUtils() {}

  /**
   * Stringifies and XML escapes the passed object, and wraps the result in a
   * span.
   *
   * @return escaped string representation, wrapped in an html span
   */
  public static String printObjectAsHtml(Object o) {
    return "<span class='object'>" + xmlEscape(String.valueOf(o)) + "</span>";
  }

  /**
   * Converts an object into a string.
   *
   * If it's an array (coming from the var args log message),
   * make a comma separated list of the array's components. Otherwise,
   * toString() the object.
   *
   */
  public static String stringifyLogObject(Object o) {
    if (o instanceof Object[]) {
      StringBuilder builder = new StringBuilder();
      Object[] objects = (Object[]) o;
      for (Object object : objects) {
        builder.append(object.toString());
      }
      return builder.toString();
    } else {
      return o.toString();
    }
  }

  /**
   * Formats a subset of a throwable's stack trace as HTML.
   *
   * @param t throwable to print
   * @return t's stack trace as HTML string
   */
  public static String printStackTraceAsHtml(Throwable t) {
    StackTraceElement[] stes = t.getStackTrace();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < stes.length && i <= MAX_STACK_FRAME; i++) {
      sb.append("&nbsp;&nbsp;&nbsp;&nbsp;")
          .append(stes[i].getClassName())
          .append(":")
          .append(stes[i].getLineNumber())
          .append(": ")
          .append(stes[i].getMethodName())
          .append("<br/>");
    }
    if (MAX_STACK_FRAME < stes.length - 1) {
      sb.append("&nbsp;&nbsp;&nbsp;&nbsp;...<br/>");
    }
    return sb.toString();
  }

  /**
   * Poor man's common.base.StringUtil.xmlEscape
   *
   * @param xml
   * @return XML-escaped string
   */
  public static String xmlEscape(String xml) {
    return xml.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").
               replaceAll("\"", "&quot;").replaceAll("\'", "&#39;");
  }
}
