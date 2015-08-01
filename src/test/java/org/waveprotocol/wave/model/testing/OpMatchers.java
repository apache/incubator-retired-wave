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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matchers for CWM operations. Many of these are for use in JMock
 * tests as replacements for the deprecated and broken
 * {@link org.jmock.Expectations#a(Class)} and non-typesafe alternative
 * {@link org.hamcrest.Matchers#instanceOf(Class)}.
 *
 */
public class OpMatchers {
  /**
   * Alternative to Matchers.a(AddParticipant.class) since JMock/Hamcrest's
   * implementation is deprecated due to being broken in Java 5 and 6.
   */
  public static Matcher<WaveletOperation> addParticipantOperation() {
    return new BaseMatcher<WaveletOperation>() {
      @Override
      public boolean matches(Object obj) {
        return obj instanceof AddParticipant;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(" instanceof AddParticipant");
      }
    };
  }

  /** Creates a matcher for operations created by the given author. */
  public static Matcher<WaveletOperation> opBy(final String author) {
    return new TypeSafeMatcher<WaveletOperation>() {
      @Override
      public boolean matchesSafely(WaveletOperation op) {
        return author.equals(op.getContext().getCreator().getAddress());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(" op created by " + author);
      }
    };
  }

  /**
   * Alternative to Matchers.a(WaveletBlipOperation.class) since
   * JMock/Hamcrest's implementation is deprecated due to being broken in Java 5
   * and 6.
   */
  public static Matcher<WaveletOperation> waveletBlipOperation() {
    return new BaseMatcher<WaveletOperation>() {
      @Override
      public boolean matches(Object obj) {
        return obj instanceof WaveletBlipOperation;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(" instanceof WaveletBlipOperation");
      }
    };
  }

  /** Uninstantiable. */
  private OpMatchers() {
  }
}
