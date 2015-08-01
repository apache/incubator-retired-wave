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

package org.waveprotocol.wave.model.adt.docbased;


import org.waveprotocol.wave.model.adt.ObservableBasicSetTestBase;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;

import java.util.Collections;

/**
 * Functional tests for the {@link DocumentBasedBasicSet} class.
 *
 */

public class ObservableBasicSetWithDocumentBasedBasicSetTest extends ObservableBasicSetTestBase {
  private static final String ENTRY_TAG = "folder";
  private static final String VALUE_ATTR = "value";

  /** Creates an empty ObservableBasicSetImpl to work with. */
  @Override
  protected void createEmptyMap() {
    ObservablePluggableMutableDocument doc = BasicFactories.observableDocumentProvider().create(
        "data", Collections.<String, String> emptyMap());

    set = DocumentBasedBasicSet.create(
        DefaultDocumentEventRouter.create(doc), doc.getDocumentElement(), Serializer.STRING,
        ENTRY_TAG, VALUE_ATTR);
  }
}
