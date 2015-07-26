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

package org.waveprotocol.wave.client.editor.content.paragraph;

import static org.waveprotocol.wave.client.editor.content.paragraph.Line.DIRTY;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the logic for renumbering ordered list items. Tries to do the
 * minimum amount of work.
 *
 * When nodes are changed in a way that might possibly require renumbering,
 * users must call {@link #markDirty(ContentElement, String)} for new or updated
 * nodes and {@link #markRemoved(ContentElement)} for removed nodes. To actually
 * do the renumbering, call {@link #renumberAll()}.
 *
 * Guarantees at worst linear time (in the size of the document) but in most
 * cases should be linear in the number of nodes actually requiring renumbering.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class OrderedListRenumberer {

  /**
   * Keeps track of the current number at each level of indentation, for a mode
   * of traversal going forward through the document. For example, when going
   * forward, reducing the level of indentation resets the numbers of higher
   * levels of indentation.
   */
  static class LevelNumbers {
    final int[] numbers = new int[Paragraph.MAX_INDENT + 1];
    int currentLevel = 0;
    {
      for (int i = 0; i < numbers.length; i++) {
        numbers[i] = 1;
      }
    }

    LevelNumbers(int initialLevel, int initialNumber) {
      setLevel(initialLevel);
      setNumber(initialNumber);
    }

    void setLevel(int level) {
      for (int i = currentLevel + 1; i <= level; i++) {
        numbers[i] = 1;
      }
      currentLevel = level;
    }

    void setNumber(int number) {
      numbers[currentLevel] = number;
    }

    int getNumberAndIncrement() {
      assert numbers[currentLevel] != 0;
      return numbers[currentLevel]++;
    }

    @Override
    public String toString() {
      return "current=" + currentLevel + ", " + numbers.toString();
    }
  }

  /**
   * A wrapper around Line that contains cached information for sorting
   * purposes. (Some of these properties are expensive to compute, so we get a
   * significant speedup by calculating them up front).
   *
   * See {@link #sortedLines()} for details on sorting order.
   */
  static class ComparableLine implements Comparable<ComparableLine> {
    final Line line;
    final int docId;
    final int minIndent;
    final int location;

    private ComparableLine(Line line, int docId, int minIndent, int location) {
      this.line = line;
      this.docId = docId;
      this.minIndent = minIndent;
      this.location = location;
    }

    @Override
    public int compareTo(ComparableLine d2) {
      // Partition by documents first
      if (this.docId != d2.docId) {
        return this.docId - d2.docId;
      }

      // Then by indentation level
      if (this.minIndent != d2.minIndent) {
        return this.minIndent - d2.minIndent;
      }

      // Finally by document order
      return this.location - d2.location;
    }
  }

  /**
   * Renderer for when a renumbering occurs.
   */
  private final ParagraphHtmlRenderer htmlRenderer;

  /**
   * Map of element to minimum affected indentation level. The interpretation is
   * that something happened at the given element, at the 'importance' of the
   * stored level.
   */
  private final IdentityMap<ContentElement, Integer> dirtyElements =
      CollectionUtils.createIdentityMap();


  /** Used to avoid a short-circuit optimisation when testing */
  @VisibleForTesting boolean updateHtmlEvenWhenNullImplNodelet = false;

  OrderedListRenumberer(ParagraphHtmlRenderer htmlRenderer) {
    this.htmlRenderer = htmlRenderer;
  }

  /**
   * Marks the given element as having changed in a way that might affect
   * numbering in some way.
   *
   * @param oldIndent the indent attribute the element had before the change
   *        that necessitated calling this method.
   */
  public void markDirty(ContentElement paraElement, String oldIndent) {
    int indent = Paragraph.getIndent(oldIndent);
    markDirty(paraElement, indent);
  }

  private void markDirty(ContentElement paraElement, int indent) {
    assert paraElement.isContentAttached() : paraElement + " not attached!";
    if (!dirtyElements.has(paraElement) || dirtyElements.get(paraElement) > indent) {
      dirtyElements.put(paraElement, indent);
    }
  }

  /**
   * Marks the given line as having been removed.
   */
  public void markRemoved(ContentElement elem) {
    Line line = Line.fromParagraph(elem);

    if (line == null) {
      // Not in a line structure. See comment in sortedLines().
      // Unfortunately we need this check in two places.
      return;
    }

    Line next = line.next();
    if (next != null) {
      // Mark the next one dirty, but preserving the minimum indent between the
      // removed line and the next line.
      markDirty(next.getParagraph(),
          dirtyElements.has(line.getParagraph()) ? minIndent(line) : line.getIndent());
    }
  }

  /**
   * @return true if a renumbering is needed.
   */
  public boolean renumberNeeded() {
    return !dirtyElements.isEmpty();
  }

  /**
   * Renumber everything in one go.
   */
  public void renumberAll() {
    List<ComparableLine> lines = sortedLines();

    for (ComparableLine data : lines) {
      Line line = data.line;
      // a renumber might remove several elements from the dirtyElements map
      if (isDirty(line)) {
        renumber(line);
      }
    }

    assert checkDirtyElementsContainsOnlyObsoleteLines();
    dirtyElements.clear();
  }

  /**
   * Perform a renumbering in the vicinity of the given line. This might end up
   * renumbering many other lines that were marked as dirty.
   */
  private void renumber(final Line aroundLine) {
    // Short cut
    if (aroundLine.getParagraph().getImplNodelet() == null && !updateHtmlEvenWhenNullImplNodelet) {
      // bail if no impl nodelet, the node might be shelved
      markClean(aroundLine);
      return;
    }

    assert aroundLine.getCachedNumberValue() == DIRTY && isDirty(aroundLine);

    int minimumIndent = minIndent(aroundLine);
    Line startingLine = aroundLine;
    int startingNumber = 1;

    // Determine starting line and number by going backwards with the loop.
    // If we find a clean, numbered line at the same level, break early and
    // take that as the the starting line and number. Otherwise, break once
    // we hit a superior line, and take the one just after it as the starting
    // line, (and with a starting number of 1).
    Line line = aroundLine;
    while (line != null) {
      RelativeImportance importance =
          importance(minimumIndent, line.isDecimalListItem(), line.getIndent());

      if (importance == RelativeImportance.MATCH) {
        if (line.getCachedNumberValue() != DIRTY) {
          // Note, in this case, the startingLine will be renumbered
          // redundantly (but this makes the logic simpler).
          startingLine = line;
          startingNumber = line.getCachedNumberValue();
          break;
        }
      } else if (importance == RelativeImportance.SUPERIOR) {
          break;
      }

      startingLine = line;
      line = line.previous();
    }

    assert startingLine != null;

    // Do the actual renumbering
    renumberRange(startingLine, minimumIndent, startingNumber);

    assert !isDirty(aroundLine);
  }

  /**
   * Renumbers a contiguous range of lines, marking them as clean.
   *
   * (Split out as a separate inner method so it's easier to see what variables
   * are needed from the previous step).
   *
   * @param startingLine initial line to begin renumbering from.
   * @param minimumIndent minimum indent within which to renumber. Renumbering
   *        will stop when the decision relative to this indent is superior
   *        (exited into a more "important" section).
   * @param startingNumber initial number for the starting line (minimum 1).
   */
  private void renumberRange(Line startingLine, int minimumIndent, int startingNumber) {
    assert startingNumber >= 1;

    Line line = startingLine;
    int currentIndent = line.getIndent();
    LevelNumbers numbers = new LevelNumbers(currentIndent, startingNumber);

    assert minimumIndent == currentIndent || startingNumber == 1;

    while (line != null) {
      int prevIndent = currentIndent;
      currentIndent = line.getIndent();
      boolean isNumbered = line.isDecimalListItem();

      numbers.setLevel(currentIndent);

      if (importance(minimumIndent, isNumbered, currentIndent) == RelativeImportance.SUPERIOR &&
          line != startingLine) {
        // Break if we've reached a higher importance run.
        // Note the special case to avoid breaking for the first element, as
        // it being dirty might have affected subsequent lines that will need
        // to be renumbered.
        break;
      }

      if (isNumbered) {
        // Renumber numbered items
        int num = numbers.getNumberAndIncrement();
        if (line.getCachedNumberValue() != num) {
          line.setCachedNumberValue(num);
          htmlRenderer.updateListValue(line.getParagraph(), num);
        }
        // Expensive assert for debugging
        // assert debugHasCorrectNumber(line);
      } else {
        // Reset the count for anything non-numbered
        numbers.setNumber(1);
      }

      markClean(line);
      line = line.next();
    }
  }

  /**
   * Grab all the dirty lines, culling obsolete ones, and return them in a
   * special order.
   *
   * Order first by owning document, then by indentation order (least indented
   * to most) then by document traversal order. We have this fancy ordering so
   * that the renumbering logic can make assumptions that make the code simpler
   * and linear in complexity.
   */
  private List<ComparableLine> sortedLines() {
    final List<ComparableLine> list = new ArrayList<ComparableLine>();

    final int[] nextDocId = new int[1];
    final IdentityMap<LocationMapper<?>, Integer> docIds = CollectionUtils.createIdentityMap();

    // Add them all to the list in one go at the rendering stage, to avoid
    // potential issues of elements being re-ordered, removed, etc since the
    // time they were originally placed in the map. We're also going to
    // pre-compute the comparison information, as it's relatively expensive
    // and so better to do it once up-front for each line rather than log(n)
    // times during the sort.
    dirtyElements.each(new IdentityMap.ProcV<ContentElement, Integer>() {
      @Override public void apply(ContentElement paraElement, Integer oldIndent) {
        // If there's no line element, then treat it as a default case with no
        // numbering. So we don't do anything about it. Also ignore if the
        // element is no longer attached.
        assert paraElement != null;

        Line line = getLineIfRelevant(paraElement);
        if (line != null) {
          line.setCachedNumberValue(DIRTY);

          LocationMapper<ContentNode> mapper = paraElement.getLocationMapper();
          int docId;
          if (!docIds.has(mapper)) {
            docId = nextDocId[0]++;
            docIds.put(mapper, docId);
          } else {
            docId = docIds.get(mapper);
          }

          list.add(new ComparableLine(line, docId, minIndent(line),
            mapper.getLocation(line.getLineElement())));
        }
      }
    });

    Collections.sort(list);

    return list;
  }

  /**
   * @return the minimum indent the given line ever had since the last
   *         renumbering
   */
  private int minIndent(Line line) {
    assert isDirty(line);
    return Math.min(line.getIndent(), dirtyElements.get(line.getParagraph()));
  }

  private boolean isDirty(Line line) {
    return dirtyElements.has(line.getParagraph());
  }

  private void markClean(Line line) {
    dirtyElements.remove(line.getParagraph());
  }

  /**
   * See {@link #importance(int, boolean, int)} for details on use and each enum
   * instance for details on specifics.
   */
  static enum RelativeImportance {
    /** The element is part of a superior indentation sequence */
    SUPERIOR,
    /** The element is part of an inferior indentation sequence */
    INFERIOR,
    /** The element is a match */
    MATCH
  }

  /**
   * Relative to the {@code relativeToIndent}, decide the 'relative importance'
   * of a line based on its properties (passed as the remaining parameters).
   *
   * See {@link RelativeImportance} for details on each possibility.
   */
  static RelativeImportance importance(int relativeToIndent, boolean isNumberedItem, int indent) {

    if (indent == relativeToIndent && isNumberedItem) {
      return RelativeImportance.MATCH;
    }

    // NOTE(danilatos): Ideally we'd have this behaviour as well, but it requires
    // complicated & potentially expensive re-numbering rules
    //   if (!ParagraphBehaviour.isList(type) && line.paragraph.getFirstChild() == null) {
    //     // Skip over non-list, empty paragraphs. I.e. invisible paragraphs.
    //   } else if (indent <= startingIndent) {
    if (indent <= relativeToIndent) {
      return RelativeImportance.SUPERIOR;
    }

    return RelativeImportance.INFERIOR;
  }

  /**
   * Decide if a paragraph element is relevant, and if so, return the associated
   * line. Otherwise return null.
   *
   * This method defines the logic of whether or not to ignore a dirty element.
   */
  private Line getLineIfRelevant(ContentElement paraElement) {
    // Check that the element is still in the DOM, and that it has
    // an associated line. If it does not, we'll assume that in this case,
    // we're in some other thing that's using ParagraphRenderer but doesn't
    // have a line structure associated, such as a caption. (Instead of
    // checking and asserting that a paragraph must have an associated line).
    return paraElement.isContentAttached() ? Line.fromParagraph(paraElement) : null;
  }

  //////////////////////////////////
  //// Methods used for testing only

  boolean checkDirtyElementsContainsOnlyObsoleteLines() {
    dirtyElements.each(new IdentityMap.ProcV<ContentElement, Integer>() {
      @Override public void apply(ContentElement paraElement, Integer oldIndent) {
        if (getLineIfRelevant(paraElement) != null) {
          throw new AssertionError("Non-obsolete line found in dirtyElements after renumbering ");
        }
      }
    });

    // return a boolean so we can call this from an assert statement.
    return true;
  }

  @SuppressWarnings("unused") // Called from an expensive assertion that
  // is commented out by default for performance reasons. Generally useful
  // for debugging.
  private boolean debugHasCorrectNumber(Line line) {
    if (!line.isDecimalListItem()) {
      return true;
    }

    Line prev = debugNextNumberedItem(line, false, line.getIndent());
    boolean ret;
    if (prev == null) {
      ret = line.getCachedNumberValue() == 1;
    } else {
      if (prev.getCachedNumberValue() == DIRTY) {
        assert  prev.getCachedNumberValue() != DIRTY;
      }
      ret = line.getCachedNumberValue() == prev.getCachedNumberValue() + 1;
    }
    if (ret == false) {
      return false;
    } else {
      return true;
    }
  }

  private Line debugNextNumberedItem(
      Line startingLine, boolean forwards, int startingIndent) {

    Line line = forwards ? startingLine.next(): startingLine.previous();
    assert line != startingLine;

    while (line != null) {
      switch (importance(startingIndent, line.isDecimalListItem(), line.getIndent())) {
      case SUPERIOR:
        return null;
      case MATCH:
        return line;
      case INFERIOR:
        // continue
      }

      line = forwards ? line.next() : line.previous();
      assert line != startingLine;
    }

    return null;
  }

}
