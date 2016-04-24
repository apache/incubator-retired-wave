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

import static org.waveprotocol.wave.model.document.util.DocOpScrub.CHARS_TO_LEAVE;
import static org.waveprotocol.wave.model.document.util.DocOpScrub.JIA;
import static org.waveprotocol.wave.model.document.util.DocOpScrub.PSI;
import static org.waveprotocol.wave.model.document.util.DocOpScrub.WO;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.util.DocOpScrub.UncheckedAnnotationBoundary;
import org.waveprotocol.wave.model.operation.OpComparators;


/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class DocOpScrubTest extends TestCase {

  static {
    DocOpScrub.setShouldScrubByDefault(false);
  }

  // Sprinkling from a few random character ranges
  static final String VALID = " abcDEF^#!(_+90" +
      "\u03e0\u042e\u6202\u05f3\u0610\u0710\u3045\u00ee\u0175\u0a00";
  static final String VALID_SCRUBBED = "aaaaaaaaaaaaaaa" + PSI + JIA + WO + "HA?Jbc?";

  static final String INVALID = "\u200e\u200f\u202a\u202e\u0000\u001f\u007f\u009f\u206a\u206f" +
      "\ud800\udbff\udc00\udfff\uffff\ufffe\ufdd0\ufdef";
  static final String INVALID_SCRUBBED = "||||^^^^##<<>>!!!!";

  static final String BOTH = VALID + INVALID;
  static final String BOTH_SCRUBBED = VALID_SCRUBBED + INVALID_SCRUBBED;

  static final String VALID_PARTIAL = leaveInitial(VALID, VALID_SCRUBBED);
  static final String INVALID_PARTIAL = INVALID_SCRUBBED; // Always scrub invalid characters

  static final Attributes ATTRS = AttributesImpl.fromUnsortedPairsUnchecked(
      VALID, VALID + "x",
      INVALID, INVALID + "y",
      "w", "a", // test robust against duplicate keys (ill-formed op proto)
      "w", "a",
      "xxxa", "a", // test multiple similar keys to ensure they are unique-ified
      "xxxb", "b",
      "xxxc", "c",
      "joe.bloe@example.com", "john.smith@foobar.net",
      "@", "@x",
      "a@b", "c@d",
      "a@@@b@@@c", "d@@e",
      "b", "bb",
      "", "");

  static final Attributes ATTRS_SCRUBBED = AttributesImpl.fromUnsortedPairsUnchecked(
      VALID_PARTIAL, VALID_PARTIAL +"a",
      INVALID_PARTIAL, INVALID_PARTIAL + "a",
      "w", "a",
      "w", "a",
      "xxxa", "a", // test multiple similar keys to ensure they are unique-ified
      "xxxa_1", "b",
      "xxxa_2", "c",
      "aaaaaaaa@aaaaaaaaaaa", "aaaaaaaaaa@aaaaaaaaaa",
      "@", "@a",
      "a@a", "a@a",
      "a@@aaaaaa", "d@@a",
      "b", "bb",
      "", "");

  static final AttributesUpdate ATTRSUP = AttributesUpdateImpl.fromUnsortedTripletsUnchecked(
      VALID, VALID + "w", VALID + "x",
      INVALID, INVALID + "y", INVALID + "z",
      "w", "a", "b", // test robust against duplicate keys (ill-formed op proto)
      "w", "a", "b",
      "xxxa", "a", "aa", // test multiple similar keys to ensure they are unique-ified
      "xxxb", "b", "bb",
      "xxxc", "c", "cc",
      "joe.bloe@example.com", "john.smith@foobar.net", "joe.shmoe@fubar.org",
      "a", null, "x@",
      "@", "@x", null,
      "a@b", "c@d", "e@f",
      "a@@@b@@@c", "d@@e", "@@",
      "b", "bb", "ccc",
      "", "", "");

  static final AttributesUpdate ATTRSUP_SCRUBBED =
    AttributesUpdateImpl.fromUnsortedTripletsUnchecked(
      VALID_PARTIAL, VALID_PARTIAL + "a", VALID_PARTIAL + "a",
      INVALID_PARTIAL, INVALID_PARTIAL + "a", INVALID_PARTIAL + "a",
      "w", "a", "b", // test duplicate keys (ill-formed op) to ensure they are unique-ified
      "w", "a", "b",
      "xxxa", "a", "aa", // test multiple similar keys to ensure they are unique-ified
      "xxxa_1", "b", "bb",
      "xxxa_2", "c", "cc",
      "aaaaaaaa@aaaaaaaaaaa", "aaaaaaaaaa@aaaaaaaaaa", "aaaaaaaaa@aaaaaaaaa",
      "a", null, "a@",
      "@", "@a", null,
      "a@a", "a@a", "a@a",
      "a@@aaaaaa", "d@@a", "@@",
      "b", "bb", "ccc",
      "", "", "");

  static final AnnotationBoundaryMap ANNO = new UncheckedAnnotationBoundary(new String[]{
        "x", "y", VALID,
        "y", null, INVALID,
        "", "", null,
        "/qwerty", "/qwerty/", "//qwert/y",
        VALID + "/" + INVALID + "/c@@b//c@b/johno@example.com", "x", "y",
        "w", "a", "b", // test robust against duplicate keys (ill-formed op proto)
        "w", "a", "b",
        "xxxa", "a", "aa", // test multiple similar keys to ensure they are unique-ified
        "xxxb", "b", "bb",
        "xxxc", "c", "cc",
      }, new String[] {
        "x", // test robust against non-empty intersection of change & end keys
        "ww", // test duplicate keys (ill-formed op) to ensure they are unique-ified
        "ww",
        "yyya", // test multiple similar keys to ensure they are unique-ified
        "yyyb",
        "yyyc",
        VALID + "/" + INVALID + "/c@@b/c@b/johno@example.com/"
      });

  static final AnnotationBoundaryMap ANNO_SCRUBBED = new UncheckedAnnotationBoundary(new String[]{
        "x", "y", VALID_PARTIAL,
        "y", null, INVALID_PARTIAL,
        "", "", null,
        "/qweaaa", "/qwaaaaa", "//qaaaaaa", // slashes only meaningful for keys
        VALID_PARTIAL + "/" + INVALID_PARTIAL + "/c@@a//a@a/aaaaa@aaaaaaaaaaa", "x", "y",
        "w", "a", "b", // test duplicate keys (ill-formed op) to ensure they are unique-ified
        "w", "a", "b",
        "xxxa", "a", "aa", // test multiple similar keys to ensure they are unique-ified
        "xxxa_3", "b", "bb",
        "xxxa_4", "c", "cc",
      }, new String[] {
        "x",
        "ww",
        "ww",
        "yyya",
        "yyya_1",
        "yyya_2",
        VALID_PARTIAL + "/" + INVALID_PARTIAL + "/c@@a/a@a/aaaaa@aaaaaaaaaaa/"
      });

  public void testScrubs() {
    DocOpBuilder b1 = new DocOpBuilder();
    DocOpBuilder b2 = new DocOpBuilder();

    b1.characters(VALID);
    b2.characters(VALID_SCRUBBED);

    b1.deleteCharacters(VALID);
    b2.deleteCharacters(VALID_SCRUBBED);

    b1.characters(INVALID);
    b2.characters(INVALID_SCRUBBED);

    b1.deleteCharacters(INVALID);
    b2.deleteCharacters(INVALID_SCRUBBED);

    b1.elementStart("abc", ATTRS);
    b2.elementStart("abc", ATTRS_SCRUBBED);

    b1.elementEnd();
    b2.elementEnd();

    b1.deleteElementStart("abc", ATTRS);
    b2.deleteElementStart("abc", ATTRS_SCRUBBED);

    b1.deleteElementEnd();
    b2.deleteElementEnd();

    b1.retain(5);
    b2.retain(5);

    b1.replaceAttributes(ATTRS, ATTRS);
    b2.replaceAttributes(ATTRS_SCRUBBED, ATTRS_SCRUBBED);

    b1.updateAttributes(ATTRSUP);
    b2.updateAttributes(ATTRSUP_SCRUBBED);

    b1.annotationBoundary(ANNO);
    b2.annotationBoundary(ANNO_SCRUBBED);

    DocOp o1 = DocOpScrub.scrub(b1.buildUnchecked());
    DocOp o2 = b2.buildUnchecked();
    assertTrue("\n" + o1 + "\n but expected \n" + o2,
        OpComparators.SYNTACTIC_IDENTITY.equal(o1, o2));
  }

  static String leaveInitial(String original, String scrubbed) {
    return original.substring(0, DocOpScrub.CHARS_TO_LEAVE) + scrubbed.substring(CHARS_TO_LEAVE);
  }
}
