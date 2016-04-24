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

package org.waveprotocol.wave.model.operation.wave;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * Checker for if a document mutation is worthy of updating a blip
 * timestamp and authorship.
 *
 * This class shouldn't be there.  Is worthy or not should be a property of the ops,
 * there shouldn't be an O(N) algorithm to work this out.
 *
 * @author zdwang@google.com (David Wang)
 */
public class WorthyChangeChecker {

  private WorthyChangeChecker() {}

  // Copied from Blips.
  @VisibleForTesting
  public static final String THREAD_INLINE_ANCHOR_TAGNAME = "reply";

  public interface ReusableWorthyChangeChecker {
    boolean isWorthy(DocOp op);
  }

  private static final ReusableWorthyChangeChecker INSTANCE = new ReusableWorthyChangeChecker() {
    @Override
    public boolean isWorthy(DocOp op) {
      try {
        op.apply(CHECKER);
      } catch (True t) {
        return true;
      }
      return false;
    }
  };

  // Exception thrown in order to terminate the visitor pattern early when an
  // op is known to be worthy.
  private static class True extends RuntimeException {
    True(String message) {
      super(message);
    }
    @Override
    public Throwable fillInStackTrace() {
      return this;  // don't fill in stack trace, for efficiency
    }
  }

  private static final True TRUE =
      new True("Preallocated exception with a meaningless stack trace");

  private static final DocOpCursor CHECKER = new DocOpCursor() {
    @Override
    public void retain(int itemCount) {
    }

    @Override
    public void characters(String characters) {
      throw TRUE;
    }

    @Override
    public void elementStart(String type, Attributes attributes) {
      if (!isAnchor(type)) {
        throw TRUE;
      }
    }

    @Override
    public void elementEnd() {
    }

    @Override
    public void deleteCharacters(String chars) {
      throw TRUE;
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      if (!isAnchor(type)) {
        throw TRUE;
      }
    }

    @Override
    public void deleteElementEnd() {
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw TRUE;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw TRUE;
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      for (int i = 0; i < map.changeSize(); i++) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        if (!ValueUtils.equal(oldValue, newValue) &&
            !key.startsWith(AnnotationConstants.USER_PREFIX) &&
            !key.startsWith(AnnotationConstants.SPELLY_PREFIX) &&
            !key.startsWith(AnnotationConstants.LINK_PREFIX) &&
            !key.startsWith(AnnotationConstants.ROSY_PREFIX) &&
            !key.startsWith(AnnotationConstants.LANGUAGE_PREFIX)) {
          throw TRUE;
        }
      }
    }
  };

  private static boolean isAnchor(String tag) {
    return tag.equals(THREAD_INLINE_ANCHOR_TAGNAME);
  }

  public static boolean isWorthy(DocOp op) {
    return create().isWorthy(op);
  }

  public static ReusableWorthyChangeChecker create() {
    return INSTANCE;
  }

  /**
   * Tests whether a document id identifies a document for which modifications
   * are possibly worthy. Operations applied to <em>un</em>worthy documents are
   * always unworthy. (e.g., any edit on an unworthy document will not be
   * treated as an indexing event).
   *
   * @param docId document identifier
   * @return false if operations on the document identified by {@code docId}
   *         should always be unworthy
   */
  public static boolean isBlipIdWorthy(String docId) {
    // NOTE(anorth): These constants are duplicated from internal
    // models. Keep them in sync.
    return !(docId.startsWith("attach") || docId.equals("mini") || docId.startsWith("tr+"));
  }
}
