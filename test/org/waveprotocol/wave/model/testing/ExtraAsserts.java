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

import org.waveprotocol.wave.model.wave.data.BlipData;

import junit.framework.Assert;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Extra assertions that are useful for tests involving the model.
 *
 */
public final class ExtraAsserts {

  /**
   * Asserts that the structure of the document and the builder are the same.
   */
  public static <N, E extends N, T extends N> void assertStructureEquivalent(
      XmlStringBuilder expected, MutableDocument<N, E, T> doc) {
    String expectedStr = expected.getXmlString();
    if (!DocCompare.equivalent(DocCompare.STRUCTURE, expectedStr, doc)) {
      String docStr = doc.toXmlString();
      String message = "Expected [" + expectedStr + "], found [" + docStr + "]";
      Assert.fail(message);
    }
  }

  /**
   * Asserts that the structure of the two documents are the same.
   */
  public static <N1, N2> void assertStructureEquivalent(ReadableWDocument<N1, ?, ?> doc1,
      ReadableWDocument<N2, ?, ?> doc2) {
    if (!DocCompare.equivalent(DocCompare.STRUCTURE, doc1, doc2)) {
      String doc1Str = doc1.toXmlString();
      String doc2Str = doc2.toXmlString();
      String message = "Expected [" + doc1Str + "] found [" + doc2Str + "]";
      Assert.fail(message);
    }
  }

  /**
   * Asserts that the content, both structure and annotations, of the document
   * and the builder are the same.
   */
  public static <N, E extends N, T extends N> void assertEqual(
      XmlStringBuilder expected, MutableDocument<N, E, T> doc) {
    String expectedStr = expected.getXmlString();
    if (!DocCompare.equivalent(DocCompare.ALL, expectedStr, doc)) {
      String docStr = doc.toXmlString();
      String message = "Expected [" + expectedStr + "], found [" + docStr + "]";
      Assert.fail(message);
    }
  }

  // Static utility class
  private ExtraAsserts() { }

  /**
   * Checks the content of a document and asserts it matches the given expected
   * content.
   *
   * @param expectedContent The expected content.
   * @param root The content to check.
   */
  public static void checkContent(String expectedContent, BlipData root) {
    Assert.assertEquals(expectedContent, DocOpUtil.toXmlString(root.getContent().asOperation()));
  }

}
