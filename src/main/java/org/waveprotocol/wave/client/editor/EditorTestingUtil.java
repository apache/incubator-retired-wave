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

package org.waveprotocol.wave.client.editor;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.extract.Repairer;
import org.waveprotocol.wave.model.document.util.LineContainers;

/**
 * Utility that exposes various package-private internals of an Editor for testing purposes.
 * NOTE(patcoleman): These methods should *only* need be called from test/debugging code.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
@VisibleForTesting
public class EditorTestingUtil {
  /** Util class, private constructor. */
  private EditorTestingUtil() {}

  /** Flushes an editor synchronously. */
  public static void forceFlush(Editor editor) {
    if (editor instanceof EditorImpl) {
      ((EditorImpl) editor).flushSynchronous();
    }
  }

  /** Performs various health checks on the editor's current state. */
  public static void checkHealth(Editor editor) {
    if (editor instanceof EditorImpl) {
      ((EditorImpl) editor).debugCheckHealth();
    }
  }

  /** Sees whether the editor is in a consistent state. */
  public static boolean isConsistent(Editor editor) {
    if (editor instanceof EditorImpl) {
      return ((EditorImpl) editor).isConsistent();
    } else {
      return true;
    }
  }

  public static void setupTestEnvironment() {
    Repairer.debugRepairIsFatal = true;
    LineContainers.setTopLevelContainerTagname("body");
    LineRendering.registerContainer("body", Editor.ROOT_HANDLER_REGISTRY);
  }
}
