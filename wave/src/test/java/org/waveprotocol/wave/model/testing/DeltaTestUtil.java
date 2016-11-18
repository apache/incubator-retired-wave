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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * A bunch of utility functions to make testing easier.
 *
 * @author zdwang@google.com (David Wang)
 */
public class DeltaTestUtil {
  private static final WaveletOperationContext DUMMY = new WaveletOperationContext(null, 0L, 0L);

  private final ParticipantId author;
  private final Random random = new Random(42);

  /**
   * Creates a {@link DeltaTestUtil} with which operations authored by the given
   * author can readily be made.
   */
  public DeltaTestUtil(String author) {
    this(new ParticipantId(author));
  }

  /**
   * Creates a {@link DeltaTestUtil} with which operations authored by the given
   * author can readily be made.
   */
  public DeltaTestUtil(ParticipantId author) {
    this.author = author;
  }

  public ParticipantId getAuthor() {
    return author;
  }

  /**
   * Creates an XmlDelete with the given data.
   */
  public WaveletOperation delete(int posStart, String characters, int remaining) {
    DocOp op = new DocOpBuilder()
        .retain(posStart)
        .deleteCharacters(characters)
        .retain(remaining)
        .build();
    BlipContentOperation blipOp = new BlipContentOperation(
        new WaveletOperationContext(author, 0L, 1), op);
    WaveletBlipOperation waveOp = new WaveletBlipOperation("blip id", blipOp);
    return waveOp;
  }

  /**
   * Wrap an op with a delta.
   */
  public TransformedWaveletDelta delta(long targetVersion, WaveletOperation op) {
    return TransformedWaveletDelta.cloneOperations(author,
        HashedVersion.unsigned(targetVersion + 1), 0L, Arrays.asList(op));
  }

  /**
   * Create a delta with a single NoOp operation.
   *
   * @param initialVersion The version before the operation.
   */
  public TransformedWaveletDelta noOpDelta(long initialVersion) {
    return makeTransformedDelta(0L, HashedVersion.unsigned(initialVersion + 1), 1);
  }

  /** Create a NoOp operation. */
  public NoOp noOp() {
    return new NoOp(new WaveletOperationContext(author, 0L, 1L));
  }

  /** Create an AddParticipant operation. */
  public AddParticipant addParticipant(ParticipantId participant) {
    return new AddParticipant(new WaveletOperationContext(author, 0L, 1L), participant);
  }

  /** Creates a RemoveParticipant operation. */
  public RemoveParticipant removeParticipant(ParticipantId participant) {
    return new RemoveParticipant(new WaveletOperationContext(author, 0L, 1L), participant);
  }

  /**
   * A docop that is empty. i.e. does nothing to the document. The document must
   * also be empty, otherwise the operation is invalid.
   */
  public WaveletOperation noOpDocOp(String blipId) {
    WaveletOperationContext context = new WaveletOperationContext(author, 0L, 1L);
    BlipContentOperation blipOp = new BlipContentOperation(context, (new DocOpBuilder()).build());
    return new WaveletBlipOperation(blipId, blipOp);
  }

  /**
   * Creates an XmlInsert with the given data.
   */
  public WaveletOperation insert(int pos, String text, int remaining,
      HashedVersion resultingVersion) {
    DocOpBuilder builder = new DocOpBuilder();
    builder.retain(pos).characters(text);
    if (remaining > 0) {
      builder.retain(remaining);
    }
    BlipContentOperation blipOp = new BlipContentOperation(
        new WaveletOperationContext(author, 0L, 1, resultingVersion), builder.build());
    WaveletBlipOperation waveOp = new WaveletBlipOperation("blip id", blipOp);
    return waveOp;
  }

  /**
   * Builds a random client delta.
   */
  public WaveletDelta makeDelta(HashedVersion targetVersion, long timestamp, int numOps) {
    List<WaveletOperation> ops = CollectionUtils.newArrayList();
    WaveletOperationContext context =
        new WaveletOperationContext(author, Constants.NO_TIMESTAMP, 1);
    for (int i = 0; i < numOps; ++i) {
      ops.add(randomOp(context));
    }
    return new WaveletDelta(author, targetVersion, ops);
  }

  /**
   * Builds a no-op client delta.
   */
  public WaveletDelta makeNoOpDelta(HashedVersion targetVersion, long timestamp, int numOps) {
    List<WaveletOperation> ops = CollectionUtils.newArrayList();
    WaveletOperationContext context =
        new WaveletOperationContext(author, Constants.NO_TIMESTAMP, 1);
    for (int i = 0; i < numOps; ++i) {
      ops.add(new NoOp(context));
    }
    return new WaveletDelta(author, targetVersion, ops);
  }

  /**
   * Builds a random transformed delta.
   */
  public TransformedWaveletDelta makeTransformedDelta(long applicationTimestamp,
      HashedVersion resultingVersion, int numOps) {
    List<WaveletOperation> ops = CollectionUtils.newArrayList();
    for (int i = 0; i < numOps; ++i) {
      ops.add(randomOp(DUMMY));
    }
    return TransformedWaveletDelta.cloneOperations(author, resultingVersion, applicationTimestamp,
        ops);
  }

  /**
   * Creates a random op. The result is unlikely to be applicable to any
   * wavelet, but is generated such that we are fairly certain that it will be
   * unique so we can identify it when it completes a round-trip.
   */
  private WaveletOperation randomOp(WaveletOperationContext context) {
    DocOp blipOp = new DocOpBuilder()
        .retain(Math.abs(random.nextInt()) / 2 + 1)
        .characters("createRndOp#" + random.nextInt())
        .build();
    return new WaveletBlipOperation("createRndId#" + random.nextInt(),
        new BlipContentOperation(context, blipOp));
  }
}
