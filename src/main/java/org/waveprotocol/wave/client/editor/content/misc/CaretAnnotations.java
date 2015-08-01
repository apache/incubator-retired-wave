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

package org.waveprotocol.wave.client.editor.content.misc;

import org.waveprotocol.wave.model.document.operation.Nindo;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Manages the annotations that are to be applied to content
 * inserted at the user's caret, which may override annotations that cover
 * a range containing the current caret.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class CaretAnnotations {
  // TODO(patcoleman) - optimise to StringMap when testable

  /** Stores the annotations as an optimised mapping of strings to strings. */
  Map<String, String> annotations = new HashMap<String, String>();

  /**
   * Stores something which can assist in finding current styles to be
   * applied on the caret.
   */
  AnnotationResolver resolver = null;

  /** Defines the contract for an annotation resolution strategy. */
  public interface AnnotationResolver {
    /** Retrieves the annotation for a particular key. */
    String getAnnotation(String key);
  }

  /** Retrieves all the annotations for the current caret. */
  public Set<String> getAnnotationKeys() {
    return annotations.keySet();
  }

  /** Retrieves the annotation for a particular key. */
  public String getAnnotation(String key) {
    if (hasAnnotation(key)) {
      return annotations.get(key);
    } else if (resolver != null) {
      return resolver.getAnnotation(key); // delegate to try to work it out
    } else {
      return null; // unknown, so set as unstyled
    }
  }

  /** Annotates the caret with a particular key/value pair. */
  public void setAnnotation(String key, String value) {
    annotations.put(key, value);
  }

  /** Check whether the caret has a particular annotation */
  public boolean hasAnnotation(String key) {
    return annotations.containsKey(key);
  }

  /** Removes an annotation from the caret (note that this is not the same as setting it to null) */
  public void removeAnnotation(String key) {
    annotations.remove(key);
  }

  /** Clears all annotations on the caret. */
  public void clear() {
    annotations.clear();
  }

  /** Attaches a strategy to resolve unknown annotation styles. */
  public void setAnnotationResolver(AnnotationResolver newResolver) {
    resolver = newResolver;
  }

  /**
   * Generate the mutation arguments for the annotations to be applied.
   * @param b The object building the mutation.
   */
  public void buildAnnotationStarts(Nindo.Builder b) {
    // add the annotations to the builder
    for (Entry<String, String> entry : annotations.entrySet()) {
      b.startAnnotation(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Close off the mutation arguments for the annotations to be applied.
   * @param b The object building the mutation.
   * @param clear True iff the annotations are to be cleared afterwards.
   */
  public void buildAnnotationEnds(Nindo.Builder b, boolean clear) {
    // finish all we applied earlier, and possibly clear
    for (String key : annotations.keySet()) {
      b.endAnnotation(key);
    }
    if (clear) {
      clear();
    }
  }

  /**
   * Convenience check to see whether a given key/value annotation has
   * been applied to the caret.
   * @param key Key of the annotation.
   * @param value Value to check if the key has been set to.
   * @return True iff the annotation is set to the value for the current caret.
   */
  public boolean isAnnotated(String key, String value) {
    // compare the two, handling nulls:
    String current = getAnnotation(key);
    return current == null ? value == null : current.equals(value);
  }
}
