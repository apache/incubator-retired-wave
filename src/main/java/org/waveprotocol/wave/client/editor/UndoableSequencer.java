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

import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.operation.OperationSequencer;

/**
 * A sequencer that guards all its operations as undoable
 *
 * @author danilatos@google.com (Daniel Danilatos);
 */
public class UndoableSequencer implements OperationSequencer<Nindo> {

  private final OperationSequencer<Nindo> sequencer;

  Responsibility.Manager responsibility;

  public UndoableSequencer(
      OperationSequencer<Nindo> chainedSequencer,
      Responsibility.Manager responsibility) {
    this.responsibility = responsibility;
    this.sequencer = chainedSequencer;
  }

  @Override
  public void begin() {
    responsibility.startDirectSequence();
    sequencer.begin();
  }

  @Override
  public void end() {
    sequencer.end();
    responsibility.endDirectSequence();
  }

  @Override
  public void consume(Nindo op) {
    sequencer.consume(op);
  }

}
