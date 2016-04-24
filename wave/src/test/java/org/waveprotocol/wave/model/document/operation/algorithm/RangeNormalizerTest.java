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

package org.waveprotocol.wave.model.document.operation.algorithm;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;

public class RangeNormalizerTest extends TestCase {

  public void testMultipleRetainNormalization() {
    RangeNormalizer<DocOp> normalizer =
        new RangeNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.characters("a");
    normalizer.retain(1);
    normalizer.retain(1);
    normalizer.retain(1);
    normalizer.characters("b");
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .characters("a")
        .retain(3)
        .characters("b")
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testEmptyRetainNormalization() {
    RangeNormalizer<DocOp> normalizer =
        new RangeNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.characters("a");
    normalizer.retain(0);
    normalizer.characters("b");
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .characters("ab")
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testMultipleCharactersNormalization() {
    RangeNormalizer<DocOp> normalizer =
        new RangeNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.characters("a");
    normalizer.characters("b");
    normalizer.characters("c");
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .characters("abc")
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testEmptyCharactersNormalization() {
    RangeNormalizer<DocOp> normalizer =
        new RangeNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.characters("");
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(2)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testMultipleDeleteCharactersNormalization() {
    RangeNormalizer<DocOp> normalizer =
        new RangeNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.deleteCharacters("a");
    normalizer.deleteCharacters("b");
    normalizer.deleteCharacters("c");
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .deleteCharacters("abc")
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testEmptyDeleteCharactersNormalization() {
    RangeNormalizer<DocOp> normalizer =
        new RangeNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.deleteCharacters("");
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(2)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

}
