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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.operation.Nindo.NindoCursor;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * Convenience interface used by IndexedDocumentImpl and by tests.
 *
 * The modifying methods present a streaming interface, similar to
 * ModifiableDocument.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface RawAnnotationSet<V> extends ReadableAnnotationSet<V> {

  /**
   * @see NindoCursor#begin()
   */
  void begin();

  /**
   * @see NindoCursor#finish()
   */
  void finish();

  /**
   * Content has been inserted into the document being annotated
   *
   * @param insertSize
   */
  void insert(int insertSize);

  String getInherited(String key);

  /**
   * Content has been removed from the document being annotated
   *
   * @param deleteSize
   */
  void delete(int deleteSize);

  /**
   */
  void skip(int skipSize);

  /**
   * @see ModifiableDocument#startAnnotation(String, String)
   */
  void startAnnotation(String key, V value);

  /**
   * @see ModifiableDocument#endAnnotation(String)
   */
  void endAnnotation(String key);

  /**
   * @return a live updated set of the known keys
   */
  ReadableStringSet knownKeysLive();

  public abstract class AnnotationEvent {
    final int index;
    final String key;

    private AnnotationEvent(int index, String key) {
      this.index = index;
      this.key = key;
    }

    abstract String getChangeKey();
    abstract String getChangeOldValue();
    abstract String getEndKey();
  }

  public final class AnnotationStartEvent extends AnnotationEvent {
    final String value;
    AnnotationStartEvent(int index, String key, String value) {
      super(index, key);
//      assert !Annotations.isLocal(key);
      this.value = value;
    }

    @Override
    String getChangeKey() {
      return key;
    }

    @Override
    String getChangeOldValue() {
      return value;
    }

    @Override
    String getEndKey() {
      return null;
    }

    @Override
    public String toString() {
      return "AnnotationStart " + key + "=" + value + " @" + index;
    }
  }

  public final class AnnotationEndEvent extends AnnotationEvent {
    AnnotationEndEvent(int index, String key) {
      super(index, key);
    }


    @Override
    String getChangeKey() {
      return null;
    }

    @Override
    String getChangeOldValue() {
      return null;
    }

    @Override
    String getEndKey() {
      return key;
    }

    @Override
    public String toString() {
      return "AnnotationEndEvent " + key + " @" + index;
    }
  }

}
