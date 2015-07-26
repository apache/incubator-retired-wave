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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.Collection;

/**
 * Encodes a string sequence as a single string. As per the {@link Sequence}
 * interface, repeated strings are not supported.
 * <p>
 * The single string that holds the sequences is a comma-delimited list. There
 * are no restrictions on the strings that can be placed in this sequence -
 * strings are encoded and decoded appropriately. For example,
 *
 * <pre>
 * [&quot;foo&quot;, &quot;bar&quot;] --&gt; &quot;,foo,bar,&quot;
 * [&quot;foo,bar&quot;, &quot;foo&amp;baz&quot;] --&gt; &quot;,foo&amp;,bar,foo&amp;&amp;baz,&quot;
 * </pre>
 *
 * All random-access methods in this implementation are linear time, so a linear
 * number of accesses is quadratic in the worst case. This class expects a
 * sequential access pattern, and is implemented so that <em>m</em> sequential
 * queries on an <em>n</em>-sized sequence is only <em>O(n + m)</em>. This is
 * not true for mutations, however: a series of <em>m</em> mutations, sequential
 * or not, will be <em>O(nm)</em>. For a sequence implementation that provides
 * expected constant time complexity for all methods, see {@link LinkedSequence}.
 *
 */
//
// Unescaped version of the example above:
// ["foo", "bar"] --> ",foo,bar,"
// ["foo,bar", "baz&quux"] --> ",foo&cbar,baz&&quux,"
//
public final class StringSequence implements Sequence<String> {
  /** Codec to use for encoding / decoding values in the list. */
  private static final StringCodec CODEC = StringCodec.INSTANCE;

  /** String to demarcate items in the list. */
  private static final String DELIMITER = CODEC.free().substring(0, 1);

  /**
   * Embedded list. Never null, and is always is of the form: DELIMITER
   * (NONDELIMITER+ DELIMITER)*.
   */
  // Note: composite linear complexity for sequential writes (i.e., m sequential
  // writes is only O(n + m)) could be achieved by alternating between a
  // serialized string and a stringbuffer of pending sequential mutations. This
  // is not implemented in order to keep this implementation very lightweight.
  private String data;

  /**
   * Last index used in a reference search. This is used to optimize for
   * sequential access, so that m queries cost (n + m) rather than O(nm).
   */
  private int recentIndex;

  @VisibleForTesting
  StringSequence(String data) {
    this.data = data;
  }

  /** Creates an empty string sequence. */
  public static StringSequence create() {
    return new StringSequence(DELIMITER);
  }

  /** Creates a string sequence on a string from another {@code StringSequence}. */
  public static StringSequence create(String serializedSequence) {
    Preconditions.checkArgument(serializedSequence.startsWith(DELIMITER)
        && serializedSequence.endsWith(DELIMITER));
    return new StringSequence(serializedSequence);
  }

  /** Creates a string sequence with an initial state. */
  public static StringSequence of(Collection<String> xs) {
    StringBuilder data = new StringBuilder();
    data.append(DELIMITER);
    for (String x : xs) {
      data.append(CODEC.encode(x));
      data.append(DELIMITER);
    }
    return new StringSequence(data.toString());
  }

  // JS does not do automatic builderization of composite concatentaions.
  /** Concatenates strings. */
  private static String concat(String s1, String s2, String s3) {
    StringBuilder s = new StringBuilder();
    s.append(s1);
    s.append(s2);
    s.append(s3);
    return s.toString();
  }

  /** Concatenates strings. */
  private static String concat(String s1, String s2, String s3, String s4) {
    StringBuilder s = new StringBuilder();
    s.append(s1);
    s.append(s2);
    s.append(s3);
    s.append(s4);
    return s.toString();
  }

  @Override
  public boolean contains(String x) {
    return data.contains(concat(DELIMITER, CODEC.encode(x), DELIMITER));
  }

  /**
   * Finds a term in the data string. The search is initiated from the location
   * of the most recent hit.
   *
   * @param term (coded) term to search for
   * @param forwardFirst if true, the search is performed with a forward search
   *        then a backward search; if false, the search is performed with a
   *        backward search then a forward search;
   * @return the index after {@code term} for a forward search; the index of
   *         {@code} term for a backward search.
   * @throws IllegalArgumentException if {@code term} is not found.
   */
  private int find(String term, boolean forwardFirst) {
    int index;

    if (forwardFirst) {
      // Search forward, leaving cursor after the find.
      index = data.indexOf(term, recentIndex);
      if (index >= 0) {
        return recentIndex = index + term.length();
      }

      // Search backward, leaving cursor after the find.
      index = data.lastIndexOf(term, recentIndex);
      if (index >= 0) {
        return recentIndex = index + term.length();
      }
    } else {
      // Search backward, leaving cursor before the find.
      index = data.lastIndexOf(term, recentIndex);
      if (index >= 0) {
        return recentIndex = index;
      }

      // Search forward, leaving cursor before the find.
      index = data.indexOf(term, recentIndex);
      if (index >= 0) {
        return recentIndex = index;
      }
    }

    // Miss.
    throw new IllegalArgumentException("Item not found: "
        + CODEC.decode(term.substring(DELIMITER.length(), term.length() - DELIMITER.length())));
  }

  private int findForward(String term) {
    return find(term, true);
  }

  private int findBackward(String term) {
    return find(term, false);
  }

  @Override
  public String getFirst() {
    //
    // , f o o , ... , b a r ,
    // . ^ first
    //
    recentIndex = DELIMITER.length();
    return recentIndex == data.length() ? null // \u2620
        : CODEC.decode(data.substring(recentIndex, data.indexOf(DELIMITER, recentIndex)));
  }

  @Override
  public String getLast() {
    //
    // , f o o , ... , b a r ,
    // 0 1 . . . ... . . . . ^ last
    //
    recentIndex = data.length() - DELIMITER.length();
    return recentIndex == 0 ? null // \u2620
        : CODEC.decode(data.substring(data.lastIndexOf(DELIMITER, recentIndex - 1)
            + DELIMITER.length(), recentIndex));
  }

  @Override
  public String getNext(String x) {
    if (x == null) {
      return getFirst();
    }
    //
    // if x = "foobar", then
    // , ... , f o o b a r , b a z q u u x , ... ,
    // . . . ^ index . . . . ^ nextStart . ^ nextEnd
    //
    String coded = concat(DELIMITER, CODEC.encode(x), DELIMITER);
    int nextStart = findForward(coded);
    if (nextStart == data.length()) {
      return null;
    } else {
      int nextEnd = data.indexOf(DELIMITER, nextStart);
      return CODEC.decode(data.substring(nextStart, nextEnd));
    }
  }

  @Override
  public String getPrevious(String x) {
    if (x == null) {
      return getLast();
    }
    //
    // if x = "foobar", then
    // , ... , b a z q u u x , f o o b a r , ... ,
    // . . . . ^ prevStart . ^ index / prevEnd
    //
    String coded = concat(DELIMITER, CODEC.encode(x), DELIMITER);
    int prevEnd = findBackward(coded);
    if (prevEnd == 0) {
      return null;
    } else {
      int prevStart = data.lastIndexOf(DELIMITER, prevEnd - 1) + DELIMITER.length();
      return CODEC.decode(data.substring(prevStart, prevEnd));
    }
  }

  @Override
  public boolean isEmpty() {
    // Being shorter than DELIMITER is impossible, due to data's invariant.
    return data.length() == DELIMITER.length();
  }

  /**
   * Inserts a value before a reference item.
   *
   * @param ref reference value (or {@code null} for append)
   * @param x value to insert
   * @throws IllegalArgumentException if {@code x} is null, or {@code ref} is
   *         non-null and not in this sequence.
   */
  public void insertBefore(String ref, String x) {
    Preconditions.checkArgument(x != null, "null item");
    if (ref == null) {
      data += CODEC.encode(x) + DELIMITER;
    } else {
      // if ref = "foobar", then
      // , ... , b a z q u u x , f o o b a r , ... ,
      // . . . . . . . . . . . ^ refIndex
      String codedRef = concat(DELIMITER, CODEC.encode(ref), DELIMITER);
      int refIndex = findBackward(codedRef);
      data =
          concat(data.substring(0, refIndex), DELIMITER, CODEC.encode(x), data.substring(refIndex));
    }
  }

  /**
   * Inserts a value after a reference item.
   *
   * @param ref reference value (or {@code null} for prepend)
   * @param x value to insert
   * @throws IllegalArgumentException if {@code x} is null, or {@code ref} is
   *         non-null and not in this sequence.
   */
  public void insertAfter(String ref, String x) {
    Preconditions.checkArgument(x != null, "null item");
    if (ref == null) {
      data = DELIMITER + CODEC.encode(x) + data;
    } else {
      // if ref = "foobar", then
      // , ... , f o o b a r , b a z q u u x , ... ,
      // . . . ^refIndex . . . ^refIndex'
      String codedRef = concat(DELIMITER, CODEC.encode(ref), DELIMITER);
      int refIndex = findForward(codedRef);
      data =
          concat(data.substring(0, refIndex), CODEC.encode(x), DELIMITER, data.substring(refIndex));
    }
  }

  /**
   * Removes a value from this sequence.
   *
   * @param x
   * @throws IllegalArgumentException if {@code x} is null or not in this
   *         sequence.
   */
  public void remove(String x) {
    // if ref = "foobar", then
    // , ... , b a r , f o o , b a z , ... ,
    // . . . . . . . ^refIndex
    Preconditions.checkArgument(x != null, "null item");
    String coded = concat(DELIMITER, CODEC.encode(x), DELIMITER);
    int index = findBackward(coded);
    data = data.substring(0, index) + data.substring(index + coded.length() - DELIMITER.length());
    // Reminder that the trailing delimiter is there.
    assert data.startsWith(DELIMITER, index);
  }

  /**
   * Clears all entries from this sequence.
   */
  public void clear() {
    data = DELIMITER;
    recentIndex = 0;
  }

  /** @return the underyling data in which strings are embedded. */
  public String getRaw() {
    return data;
  }
}
