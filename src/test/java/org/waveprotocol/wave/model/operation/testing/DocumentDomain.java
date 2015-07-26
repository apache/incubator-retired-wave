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
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.Domain;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

public class DocumentDomain implements Domain<BootstrapDocument, DocOp> {

  @Override
  public BootstrapDocument initialState() {
    return new BootstrapDocument();
  }

  @Override
  public void apply(DocOp op, BootstrapDocument state) throws OperationException {
    state.consume(op);
  }

  @Override
  public DocOp compose(DocOp f, DocOp g) throws OperationException {
    return Composer.compose(g, f);
  }

  @Override
  public OperationPair<DocOp> transform(DocOp clientOp, DocOp serverOp)
      throws TransformException {
    return Transformer.transform(clientOp, serverOp);
  }

  @Override
  public DocOp invert(DocOp operation) {
    EvaluatingDocOpCursor<DocOp> inverter =
        new DocOpInverter<DocOp>(new DocOpBuffer());
    operation.apply(inverter);
    return inverter.finish();
  }

  @Override
  public DocOp asOperation(BootstrapDocument state) {
    EvaluatingDocOpCursor<DocOp> builder = new DocOpBuffer();
    state.asOperation().apply(builder);
    return builder.finish();
  }

  @Override
  public boolean equivalent(BootstrapDocument state1, BootstrapDocument state2) {
    return state1.toString().equals(state2.toString());
  }

}
