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

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A more convenient builder than AnnotationBoundaryMapImpl.builder().
 *
 * @author ohler@google.com (Christian Ohler)
 */
// TODO(ohler): merge with AnnotationBoundaryMapImpl.Builder
public final class AnnotationBoundaryMapBuilder {

  private final StringSet endKeys = CollectionUtils.createStringSet();
  private final StringMap<Pair<String, String>> changes = CollectionUtils.createStringMap();

  public AnnotationBoundaryMapBuilder() {}

  /**
   * Adds an end of the given key to the boundary map under construction.  This
   * overrides any previous calls to change() with that key.
   */
  public AnnotationBoundaryMapBuilder end(String key) {
    changes.remove(key);
    endKeys.add(key);
    return this;
  }

  /**
   * Adds a change of the given key from oldValue to newValue to the boundary
   * map under construction.  This overrides any previous calls to change()
   * or end() with the same key.
   */
  public AnnotationBoundaryMapBuilder change(String key, String oldValue, String newValue) {
    endKeys.remove(key);
    changes.put(key, Pair.of(oldValue, newValue));
    return this;
  }

  private static class Triplet {
    final String key;
    final String oldValue;
    final String newValue;

    public Triplet(String key, String oldValue, String newValue) {
      assert key != null;
      this.key = key;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }
  }

  private static Comparator<Triplet> TRIPLET_COMPARATOR = new Comparator<Triplet>() {
    @Override
    public int compare(Triplet a, Triplet b) {
      return a.key.compareTo(b.key);
    }
  };

  private static class AnnotationBoundaryMapImpl implements AnnotationBoundaryMap {
    final String[] ends;
    final Triplet[] changes;

    public AnnotationBoundaryMapImpl(String[] ends, Triplet[] changes) {
      this.ends = ends;
      this.changes = changes;
    }

    @Override
    public int changeSize() {
      return changes.length;
    }

    @Override
    public int endSize() {
      return ends.length;
    }

    @Override
    public String getChangeKey(int i) {
      return changes[i].key;
    }

    @Override
    public String getEndKey(int i) {
      return ends[i];
    }

    @Override
    public String getNewValue(int i) {
      return changes[i].newValue;
    }

    @Override
    public String getOldValue(int i) {
      return changes[i].oldValue;
    }
  }

  /**
   * Returns an AnnotationBoundaryMap that corresponds to this builder's state.
   *
   * Behaviour is undefined if this builder is used after calling this method.
   */
  public AnnotationBoundaryMap build() {
    final String[] ends = new String[endKeys.countEntries()];
    final Triplet[] changes = new Triplet[this.changes.countEntries()];
    endKeys.each(new Proc() {
      int i = 0;
      @Override
      public void apply(String key) {
        ends[i] = key;
        i++;
      }});
    this.changes.each(new ProcV<Pair<String, String>> () {
      int i = 0;
      @Override
      public void apply(String key, Pair<String, String> value) {
        changes[i] = new Triplet(key, value.first, value.second);
        i++;
      }});
    Arrays.sort(ends);
    Arrays.sort(changes, TRIPLET_COMPARATOR);
    return new AnnotationBoundaryMapImpl(ends, changes);
  }

}
