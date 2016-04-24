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

package com.google.wave.api.data;

import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.wave.api.Attachment;
import com.google.wave.api.ElementType;
import com.google.wave.api.Range;
import com.google.wave.api.Restriction;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.DocumentImpl;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for the DocumentHitIterator related classes.
 *
 */

public class DocumentHitIteratorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Blips.init();
  }

  public void testSingleshot() {
    Range range = new Range(0, 10);
    DocumentHitIterator it = new DocumentHitIterator.Singleshot(range);
    Range first = it.next();
    assertEquals(range, first);
    assertNull(it.next());
  }

  public void testTextMatcherWithoutShift() {
    ApiView apiView = createApiViewFromXml("1 1 1");
    DocumentHitIterator it = new DocumentHitIterator.TextMatcher(apiView, "1", -1);
    for (int i = 0; i < 3; i++) {
      Range r = it.next();
      assertEquals(i * 2 + 1, r.getStart());
      assertEquals(i * 2 + 2, r.getEnd());
    }
    assertNull(it.next());
  }

  public void testTextMatcherDeletingTheFirstChar() {
    ApiView apiView = createApiViewFromXml("1 1 1");
    DocumentHitIterator it = new DocumentHitIterator.TextMatcher(apiView, "1", -1);
    for (int i = 0; i < 3; i++) {
      Range r = it.next();
      assertEquals(i + 1, r.getStart());
      apiView.delete(1, 2);
      it.shift(1, -1);
    }
    assertNull(it.next());
  }

  /**
   * Pretend we're shifting in the manner of INSERT_AFTER
   */
  public void testTextMatcherShiftInsertAfter() {
    ApiView apiView = createApiViewFromXml("1 1 1");
    DocumentHitIterator it = new DocumentHitIterator.TextMatcher(apiView, "1", -1);
    for (int i = 0; i < 3; i++) {
      Range r = it.next();
      assertEquals(i * 3 + 1, r.getStart());
      apiView.insert(r.getEnd(), "1");
      it.shift(r.getEnd(), 1);
    }
    assertNull(it.next());
  }

  public void testBasicElementMatcher() {
    String xml = "text <gadget></gadget> <something/> <gadget/> hello";
    ApiView apiView = createApiViewFromXml(xml);
    DocumentHitIterator it = new DocumentHitIterator.ElementMatcher(
        apiView, ElementType.GADGET, Collections.<String, String>emptyMap(), -1);
    List<Integer> hits = Lists.newArrayList();
    assertEquals(xml.indexOf("<gadget>") + 1, it.next().getStart());
    assertEquals(10, it.next().getStart()); // Elements get counted as 1
    assertNull(it.next());
   }

  public void testElementMatcherWithRestrictions() {
    String xml = "text <image attachment=\"nR_0YFT75\" style=\"full\">"
        + "<caption>good times</caption></image> hello";
    ApiView apiView = createApiViewFromXml(xml);
    Map<String, String> restrictions = Maps.newHashMap();
    assertEquals(1,
        countHits(new DocumentHitIterator.ElementMatcher(
            apiView, ElementType.ATTACHMENT, Collections.<String, String>emptyMap(), -1)));

    Restriction restriction = Attachment.restrictByAttachmentId("nR_0YFT75");
    assertEquals(1,
        countHits(new DocumentHitIterator.ElementMatcher(apiView, ElementType.ATTACHMENT,
            ImmutableMap.of(restriction.getKey(), restriction.getValue()), -1)));
    restriction = Attachment.restrictByAttachmentId("nR_0YFT76");
    assertEquals(0,
        countHits(new DocumentHitIterator.ElementMatcher(apiView, ElementType.ATTACHMENT,
            ImmutableMap.of(restriction.getKey(), restriction.getValue()), -1)));
  }

  private int countHits(DocumentHitIterator it) {
    int res = 0;
    while (it.next() != null) {
      res++;
    }
    return res;
  }

  private static ApiView createApiViewFromXml(String xml) {
    IndexedDocument<Node, Element, Text> indexedDoc =
        DocProviders.POJO.parse(LineContainers.debugContainerWrap(xml));
    Document doc = new DocumentImpl(createSequencer(indexedDoc), indexedDoc);
    return new ApiView(doc, mock(Wavelet.class));
  }

  /**
   * Creates and returns a sequencer which applies incoming ops to the given
   * document
   */
  private static OperationSequencer<Nindo> createSequencer(
      final IndexedDocument<Node, Element, Text> document) {
    return new OperationSequencer<Nindo>() {
      @Override
      public void begin() {
      }

      @Override
      public void end() {
      }

      @Override
      public void consume(Nindo op) {
        try {
          document.consumeAndReturnInvertible(op);
        } catch (OperationException oe) {
          throw new OperationRuntimeException("sequencer consume failed.", oe);
        }
      }
    };
  }
}
