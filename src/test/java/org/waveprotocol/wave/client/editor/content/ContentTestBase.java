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

package org.waveprotocol.wave.client.editor.content;

/**
 * Convenience base class for content node DOM tests
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class ContentTestBase extends EditorGwtTestCase {
  ContentRawDocument c = null;

  protected void assertStructure(ContentElement e,
      ContentElement parent, ContentNode prev, ContentNode next,
      ContentNode first, ContentNode last) {

    assertStructure(e, parent, prev, next);
    assertSame(first, c.getFirstChild(e));
    assertSame(last, c.getLastChild(e));

    assertSame(first == null ? null : first.getImplNodelet(), e.getImplNodelet().getFirstChild());
    assertSame(last == null ? null : last.getImplNodelet(), e.getImplNodelet().getLastChild());
  }

  protected void assertStructure(ContentNode n,
      ContentElement parent, ContentNode prev, ContentNode next) {

    assertSame(parent, c.getParentElement(n));
    assertSame(prev, c.getPreviousSibling(n));
    assertSame(next, c.getNextSibling(n));

    // TODO(danilatos): Factor this out, and use filtered view...
    assertSame(parent == null ? null
        : parent.getImplNodelet(), n.getImplNodelet().getParentElement());
    assertSame(prev == null ? null
        : prev.getImplNodelet(), n.getImplNodelet().getPreviousSibling());
    assertSame(next == null ? null
        : next.getImplNodelet(), n.getImplNodelet().getNextSibling());
  }
}
