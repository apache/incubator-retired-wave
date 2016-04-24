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

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;

/**
 * A normalizer for mutation events that affect ranges.
 *
 * @param <T> the type of the value returned by the normalizer
 */
public final class RangeNormalizer<T> implements EvaluatingDocOpCursor<T> {

  private abstract class Cache {

    abstract void flush();

    public void skip(int distance) {
      flush();
      cache = skipCache;
      cache.skip(distance);
    }

    public void characters(String characters) {
      flush();
      cache = charactersCache;
      cache.characters(characters);
    }

    public void deleteCharacters(String characters) {
      flush();
      cache = deleteCharactersCache;
      cache.deleteCharacters(characters);
    }

  }

  private final Cache emptyCache = new Cache() {

    @Override
    public void flush() {}

  };

  private final Cache skipCache = new Cache() {

    int distance = 0;

    @Override
    public void flush() {
      target.retain(distance);
      distance = 0;
      cache = emptyCache;
    }

    @Override
    public void skip(int distance) {
      this.distance += distance;
    }

  };

  private final Cache charactersCache = new Cache() {

    StringBuilder characters = new StringBuilder();

    @Override
    public void flush() {
      target.characters(characters.toString());
      characters = new StringBuilder();
      cache = emptyCache;
    }

    @Override
    public void characters(String characters) {
      this.characters.append(characters);
    }

  };

  private final Cache deleteCharactersCache = new Cache() {

    StringBuilder characters = new StringBuilder();

    @Override
    public void flush() {
      target.deleteCharacters(characters.toString());
      characters = new StringBuilder();
      cache = emptyCache;
    }

    @Override
    public void deleteCharacters(String characters) {
      this.characters.append(characters);
    }

  };

  private final EvaluatingDocOpCursor<? extends T> target;
  private Cache cache = emptyCache;

  public RangeNormalizer(EvaluatingDocOpCursor<? extends T> target) {
    this.target = target;
  }

  @Override
  public T finish() {
    cache.flush();
    return target.finish();
  }

  @Override
  public void retain(int itemCount) {
    if (itemCount > 0) {
      cache.skip(itemCount);
    }
  }

  @Override
  public void characters(String chars) {
    if (!chars.isEmpty()) {
      cache.characters(chars);
    }
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    cache.flush();
    target.elementStart(type, attrs);
  }

  @Override
  public void elementEnd() {
    cache.flush();
    target.elementEnd();
  }

  @Override
  public void deleteCharacters(String chars) {
    if (!chars.isEmpty()) {
      cache.deleteCharacters(chars);
    }
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
    cache.flush();
    target.deleteElementStart(type, attrs);
  }

  @Override
  public void deleteElementEnd() {
    cache.flush();
    target.deleteElementEnd();
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    cache.flush();
    target.replaceAttributes(oldAttrs, newAttrs);
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
    cache.flush();
    target.updateAttributes(attrUpdate);
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap map) {
    cache.flush();
    target.annotationBoundary(map);
  }

}
