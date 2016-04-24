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

package org.waveprotocol.wave.model.util;


/**
 * Some Unicode-related tools based on Unicode 5.1.0.
 *
 * Chapter 16 ( http://www.unicode.org/versions/Unicode5.0.0/ch16.pdf )
 * defines terminology like surrogates, noncharacters, control codes, etc.
 */
public final class Utf16Util {

  private Utf16Util() {}

  /**
   * Unicode character 'REPLACEMENT CHARACTER'.
   */
  public static final char REPLACEMENT_CHARACTER = 0xFFFD;

  /**
   * The visitor interface used by traverseUtf16String.
   */
  public interface CodePointHandler<T> {
    T codePoint(int cp);
    T unpairedSurrogate(char c);
    T endOfString();
  }

  // java.lang.Character also has some of these, but not everything we want.  So
  // we just have our own code for everything to reduce unnecessary indirections
  // that obscure the actual values of the numbers and their relations.

  public static boolean isCodePoint(int c) {
    return 0 <= c && c <= 0x10ffff;
  }

  public static boolean isSurrogate(char c) {
    return 0xd800 <= c && c <= 0xdfff;
  }

  public static boolean isLowSurrogate(char c) {
    return 0xdc00 <= c && c <= 0xdfff;
  }

  public static boolean isHighSurrogate(char c) {
    return 0xd800 <= c && c <= 0xdbff;
  }

  public static boolean isSurrogate(int c) {
    if (!isCodePoint(c)) {
      Preconditions.illegalArgument("Not a code point: 0x" + Integer.toHexString(c));
    }
    return 0xd800 <= c && c <= 0xdfff;
  }

  public static boolean isSupplementaryCodePoint(int c) {
    if (!isCodePoint(c)) {
      Preconditions.illegalArgument("Not a code point: 0x" + Integer.toHexString(c));
    }
    return c >= 0x10000;
  }

  /**
   * Traverses the given UTF-16 string from left to right, decoding surrogates
   * into code points, and calls the handler for each code point and unmatched
   * surrogate.
   *
   * The return values of the handler's methods determine whether
   * to continue traversal and the return value of traverseUtf16String.
   *
   * Traversal continues as long as the handler returns null.  If handler
   * returns a non-null value, traversal immediately terminates, and
   * traverseUtf16String returns the value the hander returned.
   * If the end of the string is reached, traverseUtf16String calls
   * handler.endOfString() and returns its value.
   */
  public static <T> T traverseUtf16String(String s, CodePointHandler<T> handler) {
    Preconditions.checkNotNull(s, "Null string");
    nextCodeUnit:
    for (int i = 0; i < s.length(); i++) {
      int cp;
      char c = s.charAt(i);
      if (isSurrogate(c)) {
        if (isLowSurrogate(c)) {
          // unexpected trailing (low) surrogate
          T v = handler.unpairedSurrogate(c);
          if (v != null) {
            return v;
          }
          continue nextCodeUnit;
        }
        // leading (high) surrogate
        i++;
        if (i >= s.length()) {
          T v = handler.unpairedSurrogate(c);
          if (v != null) {
            return v;
          }
          break nextCodeUnit;
        }
        char c2 = s.charAt(i);
        if (isLowSurrogate(c2)) {
          // low surrogate as expected
          cp = Character.toCodePoint(c, c2);
        } else {
          // either not a surrogate, or a high surrogate
          T v = handler.unpairedSurrogate(c);
          if (v != null) {
            return v;
          }
          i--;
          continue nextCodeUnit;
        }
      } else {
        cp = c;
      }
      T v = handler.codePoint(cp);
      if (v != null) {
        return v;
      }
    }
    return handler.endOfString();
  }

  /**
   * Returns the index of the first surrogate character in the given string,
   * or -1 if there aren't any.
   *
   * Does not check whether surrogates in s are paired correctly.
   */
  public static int firstSurrogate(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (isSurrogate(s.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the index of the first non-surrogate character in the given string,
   * or -1 if all characters in s are surrogates.
   *
   * Does not check whether surrogates in s are paired correctly.
   */
  public static int firstNonSurrogate(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!isSurrogate(s.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private static final CodePointHandler<Boolean> UNPAIRED_SURROGATES =
    new CodePointHandler<Boolean>() {
      @Override
      public Boolean codePoint(int cp) {
        return null;
      }

      @Override
      public Boolean unpairedSurrogate(char c) {
        return true;
      }

      @Override
      public Boolean endOfString() {
        return false;
      }};


  // Deprecated because I think this check is too weak to be useful.  We should
  // always also check for noncharacter codepoints etc.
  @Deprecated
  public static boolean containsUnpairedSurrogates(String s) {
    return traverseUtf16String(s, UNPAIRED_SURROGATES);
  }

  /**
   * @param c
   * @return true if the code point is valid, false if it is a non-character
   */
  public static boolean isCodePointValid(int c) {
    if (!isCodePoint(c)) {
      Preconditions.illegalArgument("Not a code point: 0x" + Integer.toHexString(c));
    }
    if (isSurrogate(c)) {
      Preconditions.illegalArgument("Code point is a surrogate: 0x" + Integer.toHexString(c));
    }

    // noncharacters
    {
      int d = c & 0xFFFF;
      if (d == 0xFFFE || d == 0xFFFF) { return false; }
    }
    if (0xFDD0 <= c && c <= 0xFDEF) { return false; }
    return true;
  }

  public enum BlipCodePointResult {
    /** Character OK for blip text. All others are not OK. */
    OK,
    /** Control characters */
    CONTROL,
    /** Deprecated format characters */
    DEPRECATED,
    /** Bidi markers. This restriction may be lifted in the future. */
    BIDI,
    /** Tag characters */
    TAG,
    /** Non-characters */
    NONCHARACTER
  }

  /**
   * Returns whether the given code point is acceptable for blip content.
   *
   * This definition is based on RFC5198 (section 2 in particular)
   * and a few internal discussions.
   *
   * It may turn out to be overly restrictive, but relaxing it in the
   * future is easy.
   */
  public static BlipCodePointResult isCodePointGoodForBlip(int c) {
    if (!isCodePoint(c)) {
      Preconditions.illegalArgument("Not a code point: 0x" + Integer.toHexString(c));
    }
    if (isSurrogate(c)) {
      Preconditions.illegalArgument("Code point is a surrogate: 0x" + Integer.toHexString(c));
    }

    if (!isCodePointValid(c)) { return BlipCodePointResult.NONCHARACTER; }
    // control codes
    if (0 <= c && c <= 0x1f || 0x7f <= c && c <= 0x9f) { return BlipCodePointResult.CONTROL; }
    // private use
    // we permit these, they can be used for things like emoji
    //if (0xE000 <= c && c <= 0xF8FF) { return false; }
    //if (0xF0000 <= c && c <= 0xFFFFD) { return false; }
    //if (0x100000 <= c && c <= 0x10FFFD) { return false; }
    // deprecated format characters
    if (0x206A <= c && c <= 0x206F) { return BlipCodePointResult.DEPRECATED; }
    // TODO: investigate whether we can lift some of these restrictions
    // bidi markers
    if (c == 0x200E || c == 0x200F) { return BlipCodePointResult.BIDI; }
    if (0x202A <= c && c <= 0x202E) { return BlipCodePointResult.BIDI; }
    // tag characters, strongly discouraged
    if (0xE0000 <= c && c <= 0xE007F) { return BlipCodePointResult.TAG; }
    return BlipCodePointResult.OK;
  }

  /**
   * Returns whether the given code point is acceptable for data document
   * content.
   *
   * For now, it allows any valid Unicode.
   */
  public static boolean isCodePointGoodForDataDocument(int c) {
    if (!isCodePoint(c)) {
      Preconditions.illegalArgument("Not a code point: 0x" + Integer.toHexString(c));
    }
    if (isSurrogate(c)) {
      Preconditions.illegalArgument("Code point is a surrogate: 0x" + Integer.toHexString(c));
    }

    if (!isCodePointValid(c)) { return false; }
    return true;
  }

  /**
   * Returns whether a given code point is a NameStartChar according to XML
   * and valid Unicode.
   *
   * See http://www.w3.org/TR/xml/#NT-NameStartChar and isCodePointValid().
   */
  public static boolean isXmlNameStartChar(int c) {
    // There are some obvious ways to speed this up, but let's not bother until
    // profiles show that it matters.

    // NameStartChar ::= ":" | [A-Z] | "_" | [a-z]
    return c == ':' || ('A' <= c && c <= 'Z') || c == '_' | ('a' <= c && c <= 'z')
        //             | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF]
        || (0xC0 <= c && c <= 0xD6) || (0xD8 <= c && c <= 0xF6) || (0xF8 <= c && c <= 0x2FF)
        //             | [#x370-#x37D] | [#x37F-#x1FFF]
        || (0x370 <= c && c <= 0x37D) || (0x37F <= c && c <= 0x1FFF)
        //             | [#x200C-#x200D] | [#x2070-#x218F]
        || (0x200C <= c && c <= 0x200D) || (0x2070 <= c && c <= 0x218F)
        //             | [#x2C00-#x2FEF] | [#x3001-#xD7FF]
        || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF)
        //             | [#xF900-#xFDCF] | [#xFDF0-#xFFFD]
        || (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD)
        //             | [#x10000-#xEFFFF]
        || ((0x10000 <= c && c <= 0xEFFFF) && isCodePointValid(c));
  }

  /**
   * Returns whether a given code point is a NameChar according to XML
   * and valid Unicode.
   *
   * See http://www.w3.org/TR/xml/#NT-NameChar and isCodePointValid().
   */
  public static boolean isXmlNameChar(int c) {
    // There are some obvious ways to speed this up, but let's not bother until
    // profiles show that it matters.

    if (!isCodePointValid(c)) { return false; }

    // NameChar ::= NameStartChar | "-" | "." | [0-9]
    return isXmlNameStartChar(c) || c == '-' || c == '.' || ('0' <= c && c <= '9')
        //          | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
        || c == 0xB7 || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040);
  }


  /**
   * Returns whether a given string is a Name according to XML and valid
   * Unicode.
   *
   * See http://www.w3.org/TR/xml/#NT-Name and isValidUtf16().
   */
  public static boolean isXmlName(String s) {
    // Name ::= NameStartChar (NameChar)*
    Preconditions.checkNotNull(s, "Null XML name string");
    if (s.isEmpty()) {
      return false;
    }
    return traverseUtf16String(s, new CodePointHandler<Boolean>() {
      boolean first = true;
      @Override
      public Boolean codePoint(int cp) {
        if (first) {
          if (!isXmlNameStartChar(cp)) {
            return false;
          }
          first = false;
        } else {
          if (!isXmlNameChar(cp)) {
            return false;
          }
        }
        return null;
      }

      @Override
      public Boolean unpairedSurrogate(char c) {
        return false;
      }

      @Override
      public Boolean endOfString() {
        return true;
      }});
  }

  private static final CodePointHandler<Boolean> VALID_UTF16 =
    new CodePointHandler<Boolean>() {
      @Override
      public Boolean codePoint(int cp) {
        if (!isCodePointValid(cp)) {
          return false;
        }
        return null;
      }

      @Override
      public Boolean unpairedSurrogate(char c) {
        return false;
      }

      @Override
      public Boolean endOfString() {
        return true;
      }};

  public static boolean isValidUtf16(String s) {
    return traverseUtf16String(s, VALID_UTF16);
  }

  private static final CodePointHandler<Boolean> GOOD_UTF16_FOR_BLIP =
    new CodePointHandler<Boolean>() {
      @Override
      public Boolean codePoint(int cp) {
        if (isCodePointGoodForBlip(cp) != BlipCodePointResult.OK) {
          return false;
        }
        return null;
      }

      @Override
      public Boolean unpairedSurrogate(char c) {
        return false;
      }

      @Override
      public Boolean endOfString() {
        return true;
      }};

  public static boolean isGoodUtf16ForBlip(String s) {
    return traverseUtf16String(s, GOOD_UTF16_FOR_BLIP);
  }

  private static final CodePointHandler<Boolean> GOOD_UTF16_FOR_DATA_DOCUMENT =
    new CodePointHandler<Boolean>() {
      @Override
      public Boolean codePoint(int cp) {
        if (!isCodePointGoodForDataDocument(cp)) {
          return false;
        }
        return null;
      }

      @Override
      public Boolean unpairedSurrogate(char c) {
        return false;
      }

      @Override
      public Boolean endOfString() {
        return true;
      }};

  public static boolean isGoodUtf16ForDataDocument(String s) {
    return traverseUtf16String(s, GOOD_UTF16_FOR_DATA_DOCUMENT);
  }

}
