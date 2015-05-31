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

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;

/**
 * A utility class containing convenient methods for creating and checking blip
 * document content.
 *
 */
public final class ModelTestUtils {

  private ModelTestUtils() {
  }

  /**
   * Creates a document with the given content.
   *
   * @param contentText The content that the document should have.
   * @return The document with the given content.
   */
  public static DocInitialization createContent(String contentText) {
    if (contentText.isEmpty()) {
      return (new DocInitializationBuilder())
          .elementStart("body", new AttributesImpl())
          .elementStart("line", new AttributesImpl())
          .elementEnd()
          .elementEnd()
          .build();
    } else {
      return new DocInitializationBuilder()
          .elementStart("body", new AttributesImpl())
          .elementStart("line", new AttributesImpl())
          .elementEnd()
          .characters(contentText)
          .elementEnd()
          .build();
    }
  }

}
