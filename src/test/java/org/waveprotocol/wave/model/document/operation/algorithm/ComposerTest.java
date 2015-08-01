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

package org.waveprotocol.wave.model.document.operation.algorithm;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class ComposerTest extends TestCase {

  public void testDocumentLengthMismatch() {
    try {
      Composer.compose(new DocOpBuilder().build(), new DocOpBuilder().retain(1).build());
      fail();
    } catch (OperationException e) {
      // ok
    }
    try {
      Composer.compose(new DocOpBuilder().retain(1).build(), new DocOpBuilder().build());
      fail();
    } catch (OperationException e) {
      // ok
    }
  }

  public void testComposerChecking() throws OperationException {
    DocOp checked = new DocOpBuilder().build();
    DocOp unchecked = new DocOpBuilder().elementStart(".!$*.4!(,", Attributes.EMPTY_MAP)
        .elementEnd().buildUnchecked();

    try {
      Composer.compose(checked, unchecked);
      fail();
    } catch (IllegalStateException e) {
      // ok, fails checking.
    }

    // compose unchecked this time, nothing should be thrown.
    Composer.composeUnchecked(checked, unchecked);
  }
}
