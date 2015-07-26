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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.Point.Tx;

/**
 * Class for locating text in a document.
 *
 */
public final class TextLocator {
  /**
   * Locates the index of a character matching some criteria.
   */
  private interface CharacterLocator {
    int indexOf(String data, int start, boolean forward);
  }

  /**
   * Checks if a character satisfies some criteria.
   */
  public interface CharacterPredicate {
    boolean apply(char c);
  }

  /**
   * Utility class.
   */
  private TextLocator() {}

  private static class CharacterLocatorImpl implements CharacterLocator {
    protected final String characters;

    public CharacterLocatorImpl(String characters) {
      this.characters = characters;
    }

    @Override
    public int indexOf(String data, int start, boolean forward) {
      return forward ? data.indexOf(characters, start) : data.lastIndexOf(characters, start);
    }
  }

  private static class PredicateBoundaryLocator implements CharacterLocator {
    private final CharacterPredicate pred;

    public PredicateBoundaryLocator(CharacterPredicate pred) {
      this.pred = pred;
    }

    private int findForwards(String data, int start) {
      for (int i = start; i < data.length(); i++) {
        if (pred.apply(data.charAt(i))) {
          return i;
        }
      }
      return -1;
    }

    private int findBackwards(String data, int start) {
      for (int i = start - 1; i >= 0 ; i--) {
        if (pred.apply(data.charAt(i))) {
          return i + 1;
        }
      }
      return -1;
    }

    @Override
    public int indexOf(String data, int start, boolean forward) {
      return forward ? findForwards(data, start) : findBackwards(data, start);
    }
  }

  /**
   * Matches "word" characters
   */
  private static final CharacterLocator wordCharactersBoundaryLocator =
      new PredicateBoundaryLocator(new CharacterPredicate() {

        @Override
        public boolean apply(char c) {
          return isWordCharacter(c);
        }
      });

  /**
   * Matches "non-word" characters
   */
  private static final CharacterLocator nonWordCharactersBoundaryLocator =
    new PredicateBoundaryLocator(new CharacterPredicate() {

      @Override
      public boolean apply(char c) {
        return !isWordCharacter(c);
      }
    });

  /**
   * Contains characters treated as whitespace for the purpose of finding an
   * appropriate insertion point when inserting a doodad.
   */
  private static final String INLINE_WHITESPACE = " \t";

  private static boolean isInlineWhitespace(char c) {
    return INLINE_WHITESPACE.indexOf(c) != -1;
  }

  /**
   * Predicate that matches inline whitespace characters.
   */
  public static final CharacterPredicate WHITESPACE_MATCHER = new CharacterPredicate() {
    @Override
    public boolean apply(char c) {
      return isInlineWhitespace(c);
    }
  };

  /**
   * Predicate that matches non-inline whitespace characters.
   */
  public static final CharacterPredicate NON_WHITESPACE_MATCHER = new CharacterPredicate() {
    @Override
    public boolean apply(char c) {
      return !isInlineWhitespace(c);
    }
  };

  /**
   * The intent of this function is to check for "word characters" i.e. letters
   * and digits and other characters that are considered part of a word such as
   * _.
   *
   * At the moment, we are approximating this by using
   * Character.isLetterOrDigit, but this doesn't work with unicode letters in
   * GWT.
   *
   * We can also consider regexp match with \w which should match with
   * "word characters", but in current browsers, these don't match with unicode
   * letters.
   */
  private static boolean isWordCharacter(char codePoint) {
    return Character.isLetterOrDigit(codePoint) || codePoint == '_';
  }

  private static <N, E extends N, T extends N> Tx<N> findCharacterInNode(
      ReadableDocument<N, E, T> doc, Tx<N> start, CharacterLocator locator, boolean forward) {
    assert start.getContainer() != null && doc.asText(start.getContainer()) != null;
    String data = doc.getData(doc.asText(start.getContainer()));

    int index = locator.indexOf(data, start.getTextOffset(), forward);
    if (index != -1) {
      return Point.inText(start.getContainer(), index);
    } else {
      return null;
    }
  }

  /**
   * Locates characters in contiguous of text nodes.
   *
   * Returns location when given locator matches with the data in a text node,
   * Returns null if not found.
   *
   * @param doc
   * @param start
   * @param locator
   * @param forward
   */
  private static <N, E extends N, T extends N> Tx<N> locateCharacters(
      ReadableDocument<N, E, T> doc, Tx<N> start, CharacterLocator locator, boolean forward) {
    Tx<N> current = start;
    N node = start.getContainer();
    Tx<N> found = null;

    while (true) {
      found = findCharacterInNode(doc, current.asTextPoint(), locator, forward);
      if (found != null) {
        return found;
      }

      node = forward ? doc.getNextSibling(node) : doc.getPreviousSibling(node);
      if (doc.asText(node) != null) {
        current =
            forward ? Point.inText(node, 0) : Point.inText(node, doc.getLength(doc.asText(node)));
      } else {
        return null;
      }
    }
  }

  /**
   * Find the next point in the neighbouring sequence of text nodes that matches
   * a set of characters.
   *
   * Returns the last point in the text node sequence if we cannot find a match.
   *
   * @param start  the point we want to start the search
   * @param characters  characters that we want to match
   * @param forward  if true search forwards, else search backwards.
   */
  public static <N, E extends N, T extends N> Tx<N> findCharacter(
      ReadableDocument<N, E, T> doc, Tx<N> start, String characters, boolean forward) {
    CharacterLocator locator = new CharacterLocatorImpl(characters);
    Tx<N> boundary;
    boundary = locateCharacters(doc, start, locator, forward);

    if (boundary == null) {
      boundary = lastPointInTextSequence(doc, start, forward);
    }
    return boundary;
  }

  /**
   * Find the next point in the neighbouring sequence of text node where
   * the character there matches some criteria.
   *
   * Returns the last point in the text node sequence if we cannot find a match.
   *
   * @param start  the point we want to start the search
   * @param pred  the criteria for the character we want to match
   * @param forward  if true search forwards, else search backwards.
   */
  public static <N, E extends N, T extends N> Tx<N> findCharacterBoundary(
      ReadableDocument<N, E, T> doc, Tx<N> start, CharacterPredicate pred, boolean forward) {
    CharacterLocator locator = new PredicateBoundaryLocator(pred);
    Tx<N> boundary;
    boundary = locateCharacters(doc, start, locator, forward);

    if (boundary == null) {
      boundary = lastPointInTextSequence(doc, start, forward);
    }
    return boundary;
  }

  /**
   * Gets the next word boundary
   *
   * NOTE(user): At the moment this only works in a contiguous sequence of text
   * nodes.
   *
   * @param start  the point to start the search
   * @param forward  if true, search forwards, else search backwards
   */
  public static <N, E extends N, T extends N> Tx<N> getWordBoundary(Point<N> start,
      ReadableDocument<N, E, T> doc, boolean forward) {
    Tx<N> startAsTx = start.asTextPoint();

    if (startAsTx == null) {
      return null;
    }

    Tx<N> firstWordCharacter =
        locateCharacters(doc, startAsTx, wordCharactersBoundaryLocator, forward);

    Tx<N> boundary = null;
    if (firstWordCharacter != null) {
      boundary =
          locateCharacters(doc, firstWordCharacter, nonWordCharactersBoundaryLocator, forward);
    }

    if (boundary == null) {
      boundary = lastPointInTextSequence(doc, startAsTx, forward);
    }

    return boundary;
  }

  /**
   * This method returns the last point in a sequence of text node in the given direction.
   *
   * We guarantee that the return value is non-null and inside a text node.
   *
   * @return the last point in the text sequence as a text point.
   */
  private static <N, E extends N, T extends N> Tx<N> lastPointInTextSequence(
      ReadableDocument<N, E, T> doc, Tx<N> start, boolean forward) {
    Tx<N> ret;
    if (forward) {
      T t = doc.asText(start.getCanonicalNode());
      T next = doc.asText(doc.getNextSibling(t));
      while (next != null) {
        t = next;
        next = doc.asText(doc.getNextSibling(t));
      }
      ret = Point.<N> inText(t, doc.getLength(t));
    } else {
      T t = doc.asText(start.getCanonicalNode());
      T prev = doc.asText(doc.getPreviousSibling(t));
      while (prev != null) {
        t = prev;
        prev = doc.asText(doc.getPreviousSibling(t));
      }
      ret = Point.<N> inText(t, 0);
    }
    return ret;
  }
}
