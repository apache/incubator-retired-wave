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

package org.waveprotocol.box.server.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Tests {@link CoreWaveletOperationSerializer}.
 *
 *
 */
public class WaveletOperationSerializerTest extends TestCase {

  // A context for ops that don't have one.
  private static final WaveletOperationContext OP_CONTEXT =
    new WaveletOperationContext(ParticipantId.ofUnsafe("test@example.com"), 0, 1);


  private static void assertDeepEquals(WaveletDelta a, WaveletDelta b) {
    assertEquals(a.getAuthor(), b.getAuthor());
    assertEquals(a.size(), b.size());
    Iterator<WaveletOperation> aItr = a.iterator();
    Iterator<WaveletOperation> bItr = b.iterator();
    while(aItr.hasNext()) {
      assertEquals(aItr.next(), bItr.next());
    }
  }

  /**
   * Assert that an operation is unchanged when serialised then deserialised.
   *
   * @param op operation to check
   */
  private static void assertReversible(WaveletOperation op) {
    // Test both (de)serialising a single operation...
    assertEquals(op, CoreWaveletOperationSerializer.deserialize(
        CoreWaveletOperationSerializer.serialize(op), OP_CONTEXT));

    List<WaveletOperation> ops = ImmutableList.of(op, op, op);
    ParticipantId author = new ParticipantId("kalman@google.com");
    HashedVersion hashedVersion = HashedVersion.unsigned(0);
    WaveletDelta delta = new WaveletDelta(author, hashedVersion, ops);
    ProtocolWaveletDelta serialized = CoreWaveletOperationSerializer.serialize(delta);
    WaveletDelta deserialized = CoreWaveletOperationSerializer.deserialize(serialized);
    assertEquals(hashedVersion.getVersion(), serialized.getHashedVersion().getVersion());
    assertTrue(Arrays.equals(hashedVersion.getHistoryHash(),
        serialized.getHashedVersion().getHistoryHash().toByteArray()));
    assertDeepEquals(delta, deserialized);
  }

  public void testNoOp() {
    assertReversible(new NoOp(OP_CONTEXT));
  }

  public void testAddParticipant() {
    assertReversible(new AddParticipant(OP_CONTEXT, new ParticipantId("kalman@google.com")));
  }

  public void testRemoveParticipant() {
    assertReversible(new RemoveParticipant(OP_CONTEXT, new ParticipantId("kalman@google.com")));
  }

  public void testEmptyDocumentMutation() {
    assertReversible(makeBlipOp("empty", new DocOpBuilder().build()));
  }

  public void testSingleCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");

    assertReversible(makeBlipOp("single", m.build()));
  }

  public void testManyCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.characters("world");
    m.characters("foo");
    m.characters("bar");

    assertReversible(makeBlipOp("many", m.build()));
  }

  public void testRetain() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.retain(5);
    m.characters("world");
    m.retain(10);
    m.characters("foo");
    m.retain(13);
    m.characters("bar");
    m.retain(16);

    assertReversible(makeBlipOp("retain", m.build()));
  }

  public void testDeleteCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.retain(1);
    m.deleteCharacters("ab");
    m.characters("world");
    m.retain(2);
    m.deleteCharacters("cd");

    assertReversible(makeBlipOp("deleteCharacters", m.build()));
  }

  public void testElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.elementStart("a", a);
    m.elementStart("b", b);
    m.elementStart("c", c);
    m.elementEnd();
    m.elementEnd();
    m.elementEnd();

    assertReversible(makeBlipOp("elements", m.build()));
  }

  public void testCharactersAndElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.elementStart("a", a);
    m.characters("hello");
    m.elementStart("b", b);
    m.characters("world");
    m.elementStart("c", c);
    m.elementEnd();
    m.characters("blah");
    m.elementEnd();
    m.elementEnd();

    assertReversible(makeBlipOp("charactersAndElements", m.build()));
  }

  public void testDeleteElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.deleteElementStart("a", a);
    m.deleteElementStart("b", b);
    m.deleteElementStart("c", c);
    m.deleteElementEnd();
    m.deleteElementEnd();
    m.deleteElementEnd();

    assertReversible(makeBlipOp("deleteElements", m.build()));
  }

  public void testDeleteCharactersAndElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.deleteElementStart("a", a);
    m.deleteCharacters("hello");
    m.deleteElementStart("b", b);
    m.deleteCharacters("world");
    m.deleteElementStart("c", c);
    m.deleteElementEnd();
    m.deleteCharacters("blah");
    m.deleteElementEnd();
    m.deleteElementEnd();

    assertReversible(makeBlipOp("deleteCharactersAndElements", m.build()));
  }

  public void testAnnotationBoundary() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    AnnotationBoundaryMap mapA = new AnnotationBoundaryMapImpl(
        new String[]{},new String[]{"a"},new String[]{null},new String[]{"b"});
    AnnotationBoundaryMap mapB = new AnnotationBoundaryMapImpl(
        new String[]{},new String[]{"a"},new String[]{"b"},new String[]{null});
    AnnotationBoundaryMap mapC = new AnnotationBoundaryMapImpl(
        new String[]{"a"},new String[]{},new String[]{},new String[]{});
    m.elementStart("a", a);
    m.annotationBoundary(mapA);
    m.characters("test");
    m.annotationBoundary(mapB);
    m.characters("text");
    m.annotationBoundary(mapC);
    m.elementEnd();

    assertReversible(makeBlipOp("annotationBoundary", m.build()));
  }

  public void testEmptyAnnotationBoundary() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    m.elementStart("a", a);
    m.annotationBoundary(AnnotationBoundaryMapImpl.EMPTY_MAP);
    m.characters("text");
    m.annotationBoundary(AnnotationBoundaryMapImpl.EMPTY_MAP);
    m.elementEnd();

    assertReversible(makeBlipOp("emptyAnnotationBoundary", m.build()));
  }

  public void testReplaceAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes oldA = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes newA = new AttributesImpl(ImmutableMap.of("a1", "3", "a2", "4"));

    m.retain(4);
    m.replaceAttributes(oldA, newA);
    m.retain(4);

    assertReversible(makeBlipOp("replaceAttributes", m.build()));
  }

  public void testEmptyReplaceAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    m.retain(4);
    m.replaceAttributes(AttributesImpl.EMPTY_MAP, AttributesImpl.EMPTY_MAP);
    m.retain(4);

    assertReversible(makeBlipOp("emptyReplaceAttributes", m.build()));
  }

  public void testUpdateAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    AttributesUpdate u = new AttributesUpdateImpl(new String[]{"a", null, "2", "b", "1", null});

    m.retain(4);
    m.updateAttributes(u);
    m.retain(4);

    assertReversible(makeBlipOp("updateAttributes", m.build()));
  }

  public void testEmptyUpdateAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    m.retain(4);
    m.updateAttributes(AttributesUpdateImpl.EMPTY_MAP);
    m.retain(4);

    assertReversible(makeBlipOp("emptyUpdateAttributes", m.build()));
  }

  private static WaveletBlipOperation makeBlipOp(String blipId, DocOp mutation) {
    return new WaveletBlipOperation(blipId, new BlipContentOperation(OP_CONTEXT, mutation));
  }
}
