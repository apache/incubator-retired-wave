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

package org.waveprotocol.wave.client.clipboard;

import com.google.gwt.http.client.URL;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.RangedAnnotationImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialization/deserialization for annotations.
 *
 * Annotation format (There is no guarantee this will stay):
 *
 * Each annotation contains start, end, url escaped key, url escaped value
 * separated by "," The annotations are separated by ":"
 *
 * Example : "23,39,fontWeight,bold:59,64,fontStyle,italics"
 *
 * TODO(user): Move this to a common package, as this seems generally useful.
 * But first, remove the dependency on gwt.
 *
 */
public class AnnotationSerializer  {
  /**
   * Deserializes the string into a list of ranged annotations.
   *
   * @param serialized
   */
  public static List<RangedAnnotation<String>> deserialize(String serialized) {
    String[] split = serialized.split(OUTER_SEPARATOR);
    List<RangedAnnotation<String>> annotations = new ArrayList<RangedAnnotation<String>>();
    for (String s : split) {
      if (!s.isEmpty()) {
        String innerSplit[] = s.split(INNER_SEPARATOR);
        if (innerSplit.length == 4) {
          annotations.add(new RangedAnnotationImpl<String>(unescape(innerSplit[2]),
              unescape(innerSplit[3]), Integer.parseInt(innerSplit[0]), Integer
                  .parseInt(innerSplit[1])));
        }
      }
    }
    return annotations;
  }

  /** Utility class */
  private AnnotationSerializer() {}

  /**
   * Serializes a list of rangedAnnotations
   *
   * @param rangedAnnotations
   */
  public static String serializeAnnotation(Iterable<RangedAnnotation<String>> rangedAnnotations) {
    Builder entries = new Builder();
    for (RangedAnnotation<String> ann : rangedAnnotations) {
      if (ann.value() != null) {
        entries.pushEntry(ann.start(), ann.end(), ann.key(), ann.value());
      }
    }
    return entries.toString();
  }


  // TODO(user): Consider some other encoding format. This is ok, but is gwt
  // dependent.
  private static final String INNER_SEPARATOR = ",";
  private static final String OUTER_SEPARATOR = ":";

  private static String escape(String s) {
    return URL.encodeComponent(s);
  }

  private static String unescape(String s) {
    return URL.decodeComponent(s);
  }

  private static class Builder {
    StringBuilder builder = new StringBuilder();

    public void pushEntry(int start, int end, String key, String value) {
      builder
        .append(start).append(INNER_SEPARATOR)
        .append(end).append(INNER_SEPARATOR)
        .append(escape(key)).append(INNER_SEPARATOR)
        .append(escape(value)).append(OUTER_SEPARATOR);
    }

    @Override
    public String toString() {
      return builder.toString();
    }
  }
}
