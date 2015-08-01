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

package org.waveprotocol.wave.model.document.operation.automaton;

import org.waveprotocol.wave.model.util.Utf16Util;
import org.waveprotocol.wave.model.util.Utf16Util.BlipCodePointResult;
import org.waveprotocol.wave.model.util.Utf16Util.CodePointHandler;

import java.util.Collections;
import java.util.List;

/**
 * Allows queries on what the XML schema allows.
 */
public interface DocumentSchema {

  /**
   * Defines constraints on characters
   */
  public enum PermittedCharacters {
    /** No characters permitted in this context */
    NONE {
      @Override
      public String coerceString(String string) {
        throw new IllegalArgumentException("Text not permitted at all, can't convert");
      }
    },

    /** Only "blip text" characters permitted */
    BLIP_TEXT {
      @Override
      public String coerceString(String string) {
        final StringBuilder result = new StringBuilder();
        Utf16Util.traverseUtf16String(string, new CodePointHandler<Void>() {
          @Override
          public Void codePoint(int cp) {
            if (cp == '\t') {
              result.append("    ");
            } else if (cp == '\n' || cp == '\r') {
              result.append(' ');
            } else if (Utf16Util.isSupplementaryCodePoint(cp)) {
              // NOTE: This will need updating when we support supplementary code points.
              result.append(Utf16Util.REPLACEMENT_CHARACTER);
            } else if (Utf16Util.isCodePointGoodForBlip(cp) == BlipCodePointResult.OK) {
              assert 0 <= cp && cp <= 0xFFFF : "Not handling supplementary code points (yet)";
              result.append((char) cp);
            } else {
              result.append(Utf16Util.REPLACEMENT_CHARACTER);
            }
            return null;
          }

          @Override
          public Void unpairedSurrogate(char c) {
            result.append(Utf16Util.REPLACEMENT_CHARACTER);
            return null;
          }

          @Override
          public Void endOfString() {
            return null;
          }
        });

        // TODO: Efficiency?!? :(
        return result.toString();
      }
    },

    /** Anything (Though well formedness requires valid unicode excluding surrogates)  */
    ANY {
      @Override
      public String coerceString(String string) {
        final StringBuilder result = new StringBuilder();
        Utf16Util.traverseUtf16String(string, new CodePointHandler<Void>() {
          @Override
          public Void codePoint(int cp) {
            if (Utf16Util.isSupplementaryCodePoint(cp)) {
              // NOTE: This will need updating when we support supplementary code points.
              result.append(Utf16Util.REPLACEMENT_CHARACTER);
            } else if (Utf16Util.isCodePointValid(cp)) {
              assert 0 <= cp && cp <= 0xFFFF;
              result.append((char) cp);
            } else {
              result.append(Utf16Util.REPLACEMENT_CHARACTER);
            }
            return null;
          }

          @Override
          public Void unpairedSurrogate(char c) {
            result.append(Utf16Util.REPLACEMENT_CHARACTER);
            return null;
          }

          @Override
          public Void endOfString() {
            return null;
          }
        });

        // TODO: Efficiency?!? :(
        return result.toString();
      }
    };

    /**
     * Converts any string into a well formed string (with respect to the characters
     * operation component) and also satisfying the particular schema constraint for
     * the associated enum value.
     *
     * @param string Does not have to be well formed
     * @return well-formed and valid
     */
    public abstract String coerceString(String string);
  }

  /**
   * True iff childType elements are permitted to occur as children of parentTypeOrNull,
   * or at the top level if parentTypeOrNull is null.
   *
   * @param parentTypeOrNull an XML name or null.
   * @param childType an XML name.
   */
  boolean permitsChild(String parentTypeOrNull, String childType);

  /**
   * What type of text is permitted within elements of type typeOrNull, or
   * at the top level if typeOrNull is null.
   *
   * @param typeOrNull an XML name.
   */
  PermittedCharacters permittedCharacters(String typeOrNull);

  /**
   * True iff elements of type type permit an attribute named attributeName.
   *
   * @param type an XML name.
   * @param attributeName an XML name.
   */
  boolean permitsAttribute(String type, String attributeName);

  /**
   * True iff elements of type type permit an attribute named attributeName
   * with the value attributeValue.
   *
   * @param type an XML name.
   * @param attributeName an XML name.
   * @param attributeValue a string.
   */
  boolean permitsAttribute(String type, String attributeName, String attributeValue);

  /**
   * Returns a list of tag names of elements that must always occur in the given
   * order at the start of the given type.
   *
   * @param typeOrNull
   * @return list of required initial elements (may be empty)
   */
  List<String> getRequiredInitialChildren(String typeOrNull);

  /**
   * A schema that permits anything
   */
  public static final DocumentSchema NO_SCHEMA_CONSTRAINTS =
      new DocumentSchema() {
        @Override
        public boolean permitsAttribute(String type, String attributeName) {
          return true;
        }

        @Override
        public boolean permitsAttribute(String type, String attributeName, String attributeValue) {
          return true;
        }

        @Override
        public boolean permitsChild(String parentType, String childType) {
          return true;
        }

        @Override
        public PermittedCharacters permittedCharacters(String type) {
          return PermittedCharacters.ANY;
        }

        @Override
        public List<String> getRequiredInitialChildren(String typeOrNull) {
          return Collections.emptyList();
        }
      };

}
