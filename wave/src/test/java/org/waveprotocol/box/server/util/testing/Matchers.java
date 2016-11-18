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

package org.waveprotocol.box.server.util.testing;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Collection;

/**
 * Additional matchers to go with JUnit4's assertThat and assumeThat.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
// TODO(Michael): Maybe move this class to the libraries repository/branch.
public class Matchers {
  /**
   * Nicer aliases for some of the methods in this class, which may conflict with methods in other
   * packages (potential conficts noted for each alias).
   */
  public static class Aliases {
    /**
     * Alias for "containsString". May conflict with "org.mockito.Mockito.contains".
     *
     * @param substring to look for.
     * @return a matcher for checking that a string contains the specified substring.
     */
    public static TypeSafeMatcher<String> contains(final String substring) {
      return containsString(substring);
    }

    /**
     * Alias for "matchesRegex". May conflict with "org.mockito.Mockito.matches".
     *
     * @param regularExpression to match against.
     * @return a matcher for checking that a string matches the specified regular expression.
     */
    public static TypeSafeMatcher<String> matches(final String regularExpression) {
      return matchesRegex(regularExpression);
    }
  }

  /**
   * A more user-friendly version of org.junit.matchers.JUnitMatchers.hasItem(T element). Allows a
   * more verbose failure than assertTrue(collection.contains(item)). The matcher produces
   * "Expected: a collection containing '...' got: '...'", whereas assertTrue produces merely
   * "AssertionFailedError".
   * Usage: static import, then assertThat(collection, contains(item)).
   *
   * @param item to look for.
   * @return a matcher for checking that a collection contains the specified item.
   */
  public static <T> TypeSafeMatcher<Collection<? super T>> contains(final T item) {
    return new TypeSafeMatcher<Collection<? super T>>() {
          @Override
          public boolean matchesSafely(Collection<? super T> collection) {
            return collection.contains(item);
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("a collection containing ").appendValue(item);
          }
        };
  }

  /**
   * Same as JUnitMatchers.containsString. Allows a more verbose failure than
   * assertTrue(str.contains(substring)).
   * Usage: static import, then assertThat(str, containsString(substring)).
   *
   * @param substring to look for.
   * @return a matcher for checking that a string contains the specified substring.
   */
  public static TypeSafeMatcher<String> containsString(final String substring) {
    return new TypeSafeMatcher<String>() {
          @Override
          public boolean matchesSafely(String str) {
            return str.contains(substring);
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("a string containing ").appendValue(substring);
          }
        };
  }

  /**
   * The negative version of "contains" for a collection. Allows a more verbose failure than
   * assertFalse(collection.contains(item)).
   * Usage: static import, then assertThat(collection, doesNotContain(item)).
   *
   * @param item to look for.
   * @return a matcher for checking that a collection does not contain the specified item.
   */
  public static <T> TypeSafeMatcher<Collection<? super T>> doesNotContain(final T item) {
    return new TypeSafeMatcher<Collection<? super T>>() {
          @Override
          public boolean matchesSafely(Collection<? super T> collection) {
            return !collection.contains(item);
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("a collection NOT containing ").appendValue(item);
          }
        };
  }

  /**
   * The negative version of "contains" for a string (or "containsString"). Allows a more verbose
   * failure than assertFalse(str.contains(substring)).
   * Usage: static import, then assertThat(str, doesNotContain(substring)).
   *
   * @param substring to look for.
   * @return a matcher for checking that a string contains the specified substring.
   */
  public static TypeSafeMatcher<String> doesNotContain(final String substring) {
    return new TypeSafeMatcher<String>() {
          @Override
          public boolean matchesSafely(String str) {
            return !str.contains(substring);
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("a string NOT containing ").appendValue(substring);
          }
        };
  }

  /**
   * Allows a more verbose failure than assertTrue(str.matches(regex)). The matcher produces
   * "Expected: a string matching regex '...' got: '...'", whereas assertTrue produces merely
   * "AssertionFailedError".
   * Usage: static import, then assertThat(str, matchesRegex(regex)).
   *
   * @param regularExpression to match against.
   * @return a matcher for checking that a string matches the specified regular expression.
   */
  public static TypeSafeMatcher<String> matchesRegex(final String regularExpression) {
    return new TypeSafeMatcher<String>() {
          @Override
          public boolean matchesSafely(String str) {
            return str.matches(regularExpression);
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("a string matching regex ").appendValue(regularExpression);
          }
        };
  }
}
