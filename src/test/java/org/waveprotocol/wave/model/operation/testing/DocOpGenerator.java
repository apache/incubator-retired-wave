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

package org.waveprotocol.wave.model.operation.testing;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;

import java.util.Random;

public class DocOpGenerator implements RandomOpGenerator<BootstrapDocument, DocOp> {

  // TODO: replace the argument with RandomProvider to make this work in GWT
  @Override
  public DocOp randomOperation(BootstrapDocument state, final Random random) {
    RandomProvider randomProvider = new RandomProvider() {
      @Override
      public boolean nextBoolean() {
        return random.nextBoolean();
      }

      @Override
      public int nextInt(int upperBound) {
        return random.nextInt(upperBound);
      }
    };
    EvaluatingDocOpCursor<DocOp> builder = new DocOpBuffer();
    RandomDocOpGenerator.generate(randomProvider, new RandomDocOpGenerator.Parameters(),
        state).apply(builder);
    return builder.finish();
  }

}
