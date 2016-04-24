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

package org.waveprotocol.wave.client.editor.testing;

import org.waveprotocol.wave.client.editor.RestrictedRange;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor.TypingSink;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.util.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A mocked typing sink providing assertion methods.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class MockTypingSink implements TypingSink {
  static class Op { }
  static class Ins extends Op {
    Point<ContentNode> start;
    String text;
    private Ins(Point<ContentNode> start, String text) {
      this.start = start;
      this.text = text;
    }
  }
  static class Del extends Op {
    Point<ContentNode> start;
    int deleteSize;
    private Del(Point<ContentNode> start, int deleteSize) {
      this.start = start;
      this.deleteSize = deleteSize;
    }
  }

  final List<Op> expectedOps = new ArrayList<Op>();
  final Set<ContentTextNode> affectedNodes = new HashSet<ContentTextNode>();
  boolean finished = true;

  public void expectDelete(Point<ContentNode> start, int deleteSize) {
    expectedOps.add(new Del(start, deleteSize));
  }

  public void expectInsert(Point<ContentNode> start, String text) {
    expectedOps.add(new Ins(start, text));
  }

  public void expectFinished() {
    TestCase.assertTrue(finished && expectedOps.isEmpty());
  }

  @Override
  public void typingReplace(Point<ContentNode> start, int length, String text,
      RestrictedRange<ContentNode> range) {

    if (length > 0) {
      Del delOp = (Del)expectedOps.remove(0);
      TestCase.assertEquals(delOp.start, start);
      TestCase.assertEquals(delOp.deleteSize, length);
    }

    if (text.length() > 0) {
      Ins insOp = (Ins)expectedOps.remove(0);
      TestCase.assertEquals(insOp.start, start);
      TestCase.assertEquals(insOp.text, text);
    }

    // TODO(danilatos): Test the range is correct
    finished = true;
  }

  @Override
  public void aboutToFlush() { }
}
