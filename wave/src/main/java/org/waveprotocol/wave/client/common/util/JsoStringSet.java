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

package org.waveprotocol.wave.client.common.util;

import static org.waveprotocol.wave.client.common.util.JsoStringMap.escape;
import static org.waveprotocol.wave.client.common.util.JsoStringMap.unescape;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import java.util.Set;

/**
 * An implementation of StringSet based on JavaScript objects.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class JsoStringSet implements StringSet {

  @VisibleForTesting
  public final JsoView backend;

  private static final Object AN_OBJECT = new Object();

  /**
   * Exposed for JsoStringMap
   * @param backend
   */
  JsoStringSet(JsoView backend) {
    this.backend = backend;
  }

  public static JsoStringSet create() {
    return new JsoStringSet(JsoStringMap.createBackend());
  }

  @Override
  public void add(String s) {
    backend.setObject(escape(s), AN_OBJECT);
  }

  @Override
  public void clear() {
    backend.clear();
  }

  @Override
  public boolean contains(String s) {
    return backend.containsKey(escape(s));
  }

  @Override
  public void each(final Proc callback) {
    backend.each(new ProcV<Object>() {
      @Override
      public void apply(String key, Object item) {
        callback.apply(unescape(key));
      }
    });
  }

  @Override
  public void filter(final StringPredicate filter) {
    backend.each(new ProcV<Object>() {
      @Override
      public void apply(String key, Object item) {
        if (filter.apply(unescape(key))) {
          // entry stays
        } else {
          backend.remove(key);
        }
      }
    });
  }

  @Override
  public boolean isEmpty() {
    return backend.isEmpty();
  }

  @Override
  public String someElement() {
    return backend.firstKey();
  }

  @Override
  public void remove(String s) {
    backend.remove(escape(s));
  }

  private static class False extends RuntimeException {
    private False() {
      super("Preallocated exception without a meaningful stacktrace");
    }
    @Override
    public Throwable fillInStackTrace() {
      // don't fill in the stack trace, which is slow (especially on client)
      return this;
    }
  }
  private static final False FALSE = new False();

  @Override
  public boolean isSubsetOf(final Set<String> set) {
    try {
      each(new Proc() {
        @Override
        public void apply(String element) {
          if (!set.contains(element)) {
            throw FALSE;
          }
        }
      });
      return true;
    } catch (False e) {
      assert e == FALSE;
      return false;
    }
  }

  @Override
  public void addAll(ReadableStringSet set) {
    set.each(new Proc() {
      public void apply(String element) {
        add(element);
      }
    });
  }

  @Override
  public void removeAll(ReadableStringSet set) {
    set.each(new Proc() {
      public void apply(String element) {
        remove(element);
      }
    });
  }

  @Override
  public boolean isSubsetOf(final ReadableStringSet other) {
    try {
      each(new Proc() {
        @Override
        public void apply(String element) {
          if (!other.contains(element)) {
            throw FALSE;
          }
        }
      });
    } catch (RuntimeException e) {
      if (e != FALSE) {
        throw e;
      }
      return false;
    }
    return true;
  }

  @Override
  public int countEntries() {
    return backend.countEntries();
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder("{");
    each(new Proc() {
      @Override
      public void apply(String element) {
        if (b.length() > 1) {
          b.append(",");
        }
        try {
          b.append("'" + element.replaceAll("\\\\", "\\\\").replaceAll("'", "\\'") + "'");
        } catch (RuntimeException e) {
          b.append("REGEX DEATH - " + element);
        }
      }
    });
    b.append("}");
    return b.toString();
  }
}
