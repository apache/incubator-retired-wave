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

package org.waveprotocol.wave.model.operation;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpComponentType;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Utilities for comparing operations.
 */
public class OpComparators {
  private OpComparators() {}

  // Proper equators would also provide hash codes.

  public interface OpEquator {
    boolean equalNullable(DocOp a, DocOp b);
    boolean equal(DocOp a, DocOp b);
  }

  public static final OpEquator SYNTACTIC_IDENTITY = new OpEquator() {
    @Override
    public boolean equal(DocOp a, DocOp b) {
      Preconditions.checkNotNull(a, "First argument is null");
      Preconditions.checkNotNull(b, "Second argument is null");
      return equalNullable(a, b);
    }

    @Override
    public boolean equalNullable(DocOp a, DocOp b) {
      if (a == null) {
        return b == null;
      }
      if (b == null) {
        return false;
      }

      if (a.size() != b.size()) {
        return false;
      }

      for (int i = 0; i < a.size(); ++i) {
        if (!equalComponent(a, b, i)) {
          return false;
        }
      }

      return true;
    }

    private boolean equalComponent(DocOp a, DocOp b, int i) {
      DocOpComponentType type = a.getType(i);
      if (type != b.getType(i)) {
        return false;
      }

      if (type == DocOpComponentType.ANNOTATION_BOUNDARY) {
        return equal(a.getAnnotationBoundary(i), b.getAnnotationBoundary(i));

      } else if (type == DocOpComponentType.CHARACTERS) {
        return equal(a.getCharactersString(i), b.getCharactersString(i));

      } else if (type == DocOpComponentType.ELEMENT_START) {
        return equal(a.getElementStartTag(i), b.getElementStartTag(i))
            && equal(a.getElementStartAttributes(i), b.getElementStartAttributes(i));

      } else if (type == DocOpComponentType.ELEMENT_END) {
        return true;  // ignored

      } else if (type == DocOpComponentType.RETAIN) {
        return a.getRetainItemCount(i) == b.getRetainItemCount(i);

      } else if (type == DocOpComponentType.DELETE_CHARACTERS) {
        return equal(a.getDeleteCharactersString(i), b.getDeleteCharactersString(i));

      } else if (type == DocOpComponentType.DELETE_ELEMENT_START) {
        return equal(a.getDeleteElementStartTag(i), b.getDeleteElementStartTag(i))
            && equal(a.getDeleteElementStartAttributes(i), b.getDeleteElementStartAttributes(i));

      } else if (type == DocOpComponentType.DELETE_ELEMENT_END) {
        return true;  // ignored

      } else if (type == DocOpComponentType.REPLACE_ATTRIBUTES) {
        return equal(a.getReplaceAttributesOldAttributes(i), b.getReplaceAttributesOldAttributes(i))
            && equal(a.getReplaceAttributesNewAttributes(i),
            b.getReplaceAttributesNewAttributes(i));

      } else if (type == DocOpComponentType.UPDATE_ATTRIBUTES) {
        return equal(a.getUpdateAttributesUpdate(i), b.getUpdateAttributesUpdate(i));

      } else {
        throw new IllegalArgumentException("unsupported DocOpComponentType: " + type);
      }
    }

  private boolean equal(AnnotationBoundaryMap a, AnnotationBoundaryMap b) {
      int changeSize = a.changeSize();
      if (changeSize != b.changeSize()) {
        return false;
      }

      for (int i = 0; i < changeSize; ++i) {
        if (!equal(a.getChangeKey(i), b.getChangeKey(i))) {
          return false;
        }
        if (!equalNullable(a.getOldValue(i), b.getOldValue(i))) {
          return false;
        }
        if (!equalNullable(a.getNewValue(i), b.getNewValue(i))) {
          return false;
        }
      }

      int endSize = a.endSize();
      if (endSize != b.endSize()) {
        return false;
      }

      for (int i = 0; i < endSize; ++i) {
        if (!equal(a.getEndKey(i), b.getEndKey(i))) {
          return false;
        }
      }

      return true;
    }

    private boolean equalNullable(String a, String b) {
      if (a == null) {
        return b == null;
      }

      if (b == null) {
        return false;
      }

      return equal(a, b);
    }

    private boolean equal(String a, String b) {
      return a.equals(Preconditions.checkNotNull(b, "b"));
    }

    private boolean equal(Map<String, String> a, Map<String, String> b) {
      return a.equals(Preconditions.checkNotNull(b, "b"));
    }

    private boolean equal(AttributesUpdate a, AttributesUpdate b) {
      int changeSize = a.changeSize();
      if (changeSize != b.changeSize()) {
        return false;
      }

      for (int i = 0; i < changeSize; ++i) {
        if (!equal(a.getChangeKey(i), b.getChangeKey(i))) {
          return false;
        }

        if (!equalNullable(a.getOldValue(i), b.getOldValue(i))) {
          return false;
        }

        if (!equalNullable(a.getNewValue(i), b.getNewValue(i))) {
          return false;
        }
      }

      return true;
    }

  };

  public static boolean equalDocuments(DocInitialization a, DocInitialization b) {
    return DocOpUtil.toXmlString(a).equals(DocOpUtil.toXmlString(b));
  }
}
