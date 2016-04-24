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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;

/**
 * An abstract base class for DocInitializations that implements the methods
 * that take DocOpCursor in terms of corresponding methods that take
 * DocInitializationCursor.
 */
public abstract class AbstractDocInitialization implements DocInitialization {

  @Override
  public void apply(DocOpCursor c) {
    apply((DocInitializationCursor) c);
  }

  @Override
  public void applyComponent(int i, DocOpCursor c) {
    applyComponent(i, (DocInitializationCursor) c);
  }

}
