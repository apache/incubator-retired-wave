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

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;
import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap.AttributeUpdate;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.Utf16Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility for scrubbing user-sensitive information out of operations.
 *
 * It is not intended to make reverse-engineering the original content totally
 * impossible, given enough time and effort. It is meant to prevent
 * casual/accidental viewing of user data in logs, while still preserving
 * valuable debugging information, such as the type of characters in a string
 * (so we can tell, for example, if the error is related to CJK input or not).
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class DocOpScrub {

  /**
   * An implementation of {@link AnnotationBoundaryMap} that tries its best to
   * be well formed (by sorting its input data) but does not perform any strong
   * validity check.
   */
  public static class UncheckedAnnotationBoundary implements AnnotationBoundaryMap {
    static class Triplet implements Comparable<Triplet> {
      private final String key, oldVal, newVal;

      private Triplet(String key, String oldVal, String newVal) {
        Preconditions.checkNotNull(key, "triplet key");
        this.key = key;
        this.oldVal = oldVal;
        this.newVal = newVal;
      }

      @Override
      public int compareTo(Triplet o) {
        return key.compareTo(o.key);
      }
    }

    private final String[] ends;
    private final Triplet[] changes;

    public UncheckedAnnotationBoundary(String[] triplets, String[] ends) {
      Preconditions.checkArgument(triplets.length % 3 == 0, "triplets.length not a multiple of 3");

      this.ends = copy(ends);
      Arrays.sort(this.ends);

      this.changes = new Triplet[triplets.length / 3];
      for (int i = 0; i < triplets.length; i += 3) {
        this.changes[i / 3] = new Triplet(triplets[i], triplets[i + 1], triplets[i + 2]);
      }
      Arrays.sort(this.changes);
    }

    /**
     * Copies an array. GWT does not seem to support Arrays.copyOf()
     */
    private String[] copy(String[] input) {
      String[] ret = new String[input.length];
      for (int i = 0; i < input.length; i++) {
        ret[i] = input[i];
      }
      return ret;
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
      return changes[i].newVal;
    }

    @Override
    public String getOldValue(int i) {
      return changes[i].oldVal;
    }
  }

  private static boolean shouldScrubByDefault = true;

  /**
   * Sets whether {@link #maybeScrub(DocOp)} should scrub.
   */
  public static void setShouldScrubByDefault(boolean shouldScrub) {
    shouldScrubByDefault = shouldScrub;
  }

  /**
   * Whether operations should be scrubbed by default. This should be true for
   * non-debug environments to protect privacy.
   *
   * See {@link #setShouldScrubByDefault(boolean)}.
   */
  public static boolean shouldScrubByDefault() {
    return shouldScrubByDefault;
  }

  /**
   * Number of leading characters to avoid scrubbing in cases where we do
   * not scrub the entire string
   */
  static final int CHARS_TO_LEAVE = 3;

  static final char PSI = 0x3c8, JIA = 0x42f, ARMENIAN = 0x554, WO = 0x6211;

  public interface StringScrubber {
    String scrub(String input);
  }

  public static class ScrubCache {
    private final StringScrubber format;
    private final StringMap<String> alreadyScrubbed = CollectionUtils.createStringMap();
    private final StringSet scrubbings = CollectionUtils.createStringSet();
    private int uniqueSuffix = 0;

    public ScrubCache(StringScrubber format) {
      this.format = format;
    }

    String scrubUniquely(String input) {
      if (alreadyScrubbed.containsKey(input)) {
        return alreadyScrubbed.get(input);
      }

      String bareScrubbed = format.scrub(input);
      String scrubbed = bareScrubbed;
      while (scrubbings.contains(scrubbed)) {
        scrubbed = bareScrubbed + '_' + (++uniqueSuffix);
      }

      alreadyScrubbed.put(input, scrubbed);
      scrubbings.add(scrubbed);

      return scrubbed;
    }
  }

  private static final StringScrubber attrNameScrubber = new StringScrubber() {
    @Override public String scrub(String input) {
      return scrubMostString(input);
    }
  };

  private static final StringScrubber annotationKeyScrubber = new StringScrubber() {
    @Override public String scrub(String input) {
      return scrubMostAnnotationKey(input);
    }
  };

  /**
   * Scrubs the given operation. Ill-formed input is permitted but may lead to
   * ill-formed output. Invalid characters will be clearly noted.
   */
  public static DocOp scrub(final DocOp op) {
    try {
      DocOpBuffer b = new DocOpBuffer();
      op.apply(createScrubber(b));
      return b.finishUnchecked();
    } catch (RuntimeException e) {
      // This should not really happen unless perhaps the input operation has some
      // diabolically broken implementation of apply ofr of attribute or annotation datastructures.
      return new DocOpBuilder().characters("Scrub exploded: " + e).build();
    }
  }

  /**
   * Same as {@link #scrub(DocOp)} but deals with {@link DocInitialization}s
   */
  public static DocInitialization scrub(final DocInitialization op) {
    return DocOpUtil.asInitialization(scrub((DocOp) op));
  }

  /**
   * Maybe scrub the given operation.
   *
   * See {@link #shouldScrubByDefault()} and {@link #scrub(DocOp)}
   */
  public static DocOp maybeScrub(final DocOp op) {
    return shouldScrubByDefault ? scrub(op) : op;
  }

  /**
   * Maybe scrub the given initialization.
   *
   * See {@link #shouldScrubByDefault()} and {@link #scrub(DocInitialization)}
   */
  public static DocInitialization maybeScrub(final DocInitialization op) {
    return shouldScrubByDefault ? scrub(op) : op;
  }

  public static DocOpCursor createScrubber(final DocOpCursor target) {
    final ScrubCache attrNames = new ScrubCache(attrNameScrubber);
    final ScrubCache annotationNames = new ScrubCache(annotationKeyScrubber);

    return new DocOpCursor() {
      @Override
      public void deleteCharacters(String chars) {
        target.deleteCharacters(scrubString(chars));
      }

      @Override
      public void deleteElementEnd() {
        target.deleteElementEnd();
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
        target.deleteElementStart(type, scrubAttributes(attrs, attrNames));
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        target.replaceAttributes(
            scrubAttributes(oldAttrs, attrNames),
            scrubAttributes(newAttrs, attrNames));
      }

      @Override
      public void retain(int itemCount) {
        target.retain(itemCount);
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        target.updateAttributes(scrubAttributesUpdate(attrUpdate, attrNames));
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
        target.annotationBoundary(scrubAnnotationBoundary(map, annotationNames));
      }

      @Override
      public void characters(String chars) {
        target.characters(scrubString(chars));
      }

      @Override
      public void elementEnd() {
        target.elementEnd();
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
        target.elementStart(type, scrubAttributes(attrs, attrNames));
      }
    };
  }

  public static AnnotationBoundaryMap scrubAnnotationBoundary(AnnotationBoundaryMap unscrubbed,
      ScrubCache nameScrubber) {
    String[] ends = new String[unscrubbed.endSize()];
    String[] changeTriplets = new String[unscrubbed.changeSize() * 3];

    for (int i = 0; i < unscrubbed.endSize(); i++) {
      ends[i] = nameScrubber.scrubUniquely(unscrubbed.getEndKey(i));
    }
    for (int i = 0; i < unscrubbed.changeSize(); i++) {
      changeTriplets[i * 3] = nameScrubber.scrubUniquely(unscrubbed.getChangeKey(i));
      changeTriplets[i * 3 + 1] = scrubMostString(unscrubbed.getOldValue(i));
      changeTriplets[i * 3 + 2] = scrubMostString(unscrubbed.getNewValue(i));
    }
    return new UncheckedAnnotationBoundary(changeTriplets, ends);
  }

  public static Attributes scrubAttributes(Attributes unscrubbed, ScrubCache nameScrubber) {
    List<Attribute> list = new ArrayList<Attribute>();
    for (Map.Entry<String, String> entry : unscrubbed.entrySet()) {
      list.add(new Attribute(
          nameScrubber.scrubUniquely(entry.getKey()),
          scrubMostString(entry.getValue())));
    }
    return AttributesImpl.fromUnsortedAttributesUnchecked(list);
  }

  public static AttributesUpdate scrubAttributesUpdate(AttributesUpdate unscrubbed,
      ScrubCache nameScrubber) {
    List<AttributeUpdate> list = new ArrayList<AttributeUpdate>();
    for (int i = 0; i < unscrubbed.changeSize(); i++) {
      list.add(new AttributeUpdate(
          nameScrubber.scrubUniquely(unscrubbed.getChangeKey(i)),
          scrubMostString(unscrubbed.getOldValue(i)),
          scrubMostString(unscrubbed.getNewValue(i))));
    }
    return AttributesUpdateImpl.fromUnsortedUpdatesUnchecked(list);
  }

  /**
   * Scrubs most of an annotation key. Uses {@link #scrubMostString(String)} on
   * each slash (/) separated component of the key, preserving the original
   * slashes.
   *
   * @param unscrubbed key
   * @return mostly scrubbed key
   */
  public static String scrubMostAnnotationKey(String unscrubbed) {
    // pass -1 to split to prevent dropping trailing empty strings
    String[] parts = unscrubbed.split("/", -1);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      parts[i] = scrubMostString(parts[i]);
    }
    return CollectionUtils.join('/', parts);
  }

  /**
   * Scrubs most of a string based on a heuristic that attempts to clean out
   * user sensitive data while still retaining some amount of information that
   * would be useful for debugging.
   *
   * If the string looks a bit like an email address, then everything but the @
   * symbol is scrubbed. Otherwise, everything but the first few characters is
   * scrubbed. In any case, invalid characters are always scrubbed for
   * readability. The motivation in the case of email addresses is both to make
   * it clear that the piece of data is an email address, and to be a little
   * stronger about removing the identifiable information
   *
   * NOTE(danilatos): Consider also always scrubbing confusing-to-print
   * characters such as RTL or ligature forming characters?
   *
   * @param unscrubbed unscrubbed string. may be null, in which case null is
   *        returned.
   * @return mostly scrubbed string
   */
  public static String scrubMostString(String unscrubbed) {
    if (unscrubbed == null) {
      return null;
    }

    int index = unscrubbed.indexOf('@');
    if (index != -1 && unscrubbed.lastIndexOf('@') == index) {
      // If it looks vaguely like an email address (contains a single '@'),
      // then scrub everything except for the '@' symbol.
      // pass 2 to split to prevent dropping of trailing empty strings
      String[] parts = unscrubbed.split("@", 2);
      return scrubString(parts[0]) + '@' + scrubString(parts[1]);
    } else if (unscrubbed.length() >= CHARS_TO_LEAVE){
      // Otherwise scrub everything but the first few characters,
      // which we leave to aid debugging.
      return
          scrubString(unscrubbed.substring(0, CHARS_TO_LEAVE), false) +
          scrubString(unscrubbed.substring(CHARS_TO_LEAVE), true);
    } else {
      return unscrubbed;
    }
  }

  public static String scrubString(String unscrubbed) {
    return scrubString(unscrubbed, true);
  }

  static String scrubString(String unscrubbed, boolean scrubValidChars) {
    char[] chars = new char[unscrubbed.length()];
    for (int i = 0; i < unscrubbed.length(); i++) {
      chars[i] = scrubChar(unscrubbed.charAt(i), scrubValidChars);
    }
    return new String(chars);
  }

  static char scrubChar(char c, boolean scrubValid) {

    assert Utf16Util.isCodePoint(c) : "isCodePoint() should always be true for char";

    if (Utf16Util.isSurrogate(c)) {
      // High surrogate comes first. Matching pairs should appear as "<>"
      if (Utf16Util.isHighSurrogate(c)) {
        return '<';
      } else {
        assert Utf16Util.isLowSurrogate(c);
        return '>';
      }
    }

    switch (Utf16Util.isCodePointGoodForBlip(c)) {
    case OK:
      return scrubValid ? scrubValidChar(c) : c;
    case BIDI:
      return '|';
    case CONTROL:
      return '^';
    case NONCHARACTER:
      return '!';
    default:
      // Other invalid characters
      return '#';
    }
  }

  private static char scrubValidChar(char c) {
    assert c >= 0x20;

    // Reference: http://www.alanwood.net/unicode/fontsbyrange.html#u0180
    // The ranges are approximate, some encompass more than they describe.
    if (c <= 0x7f) { // Basic Latin
      return 'a';
    } else if (c <= 0xff) { // Latin-1 Supplement
      return 'b';
    } else if (c <= 0x17f) { // Latin Extended-A
      return 'c';
    } else if (c <= 0x24f) { // Latin Extended-B
      return 'd';
    } else if (0x2c60 <= c && c <= 0x2c7f) { // Latin Extended-C
      return 'e';
    } else if (0xa720 <= c && c <= 0xa7ff) { // Latin Extended-D
      return 'f';
    } else if (c <= 0x2AF) { // IPA Extensions
      return 'I';
    } else if (c <= 0x2FF) { // Spacing modifier letters
      return 'S';
    } else if (c <= 0x36f) { // Combining diacritical marks
      return ':';
    } else if (c <= 0x3FF) { // Greek and coptic
      return PSI;
    } else if (c <= 0x52F) { // Cyrillic & Cyrillic supplement
      return JIA;
    } else if (c <= 0x58f) { // Armenian
      return ARMENIAN;
    } else if (c <= 0x5ff) { // Hebrew
      return 'H';
    } else if (c <= 0x6ff) { // Arabic
      return 'A';
    } else if (0x900 <= c && c <= 0x97f) { // Devangari
      return 'D';
    } else if (0xe00 <= c && c <= 0xe7f) { // Thai
      return 'T';
    } else if (0x1100 <= c && c <= 0x11ff) { // Some Hangul Jamo
      return 'K';
    } else if (0x20a0 <= c && c <= 0x2bff) { // Some symbol type things
      return '%';
    } else if (0x2e80 <= c && c <= 0x2eff ||
               0x3000 <= c && c <= 0x303f ||
               0x3200 <= c && c <= 0x9fff) { // Some CJK
      return WO;
    } else if (0x3040 <= c && c <= 0x30ff) { // Some Japanese
      return 'J';
    } else { // TODO others
      return '?';
    }
  }

  private DocOpScrub() {}
}
