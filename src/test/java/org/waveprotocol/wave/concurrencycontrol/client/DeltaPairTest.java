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

package org.waveprotocol.wave.concurrencycontrol.client;


import junit.framework.TestCase;

import org.waveprotocol.wave.concurrencycontrol.common.DeltaPair;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * Test we transform a delta pair correctly.
 *
 * @author zdwang@google.com (David Wang)
 */

public class DeltaPairTest extends TestCase {
  private static final DeltaTestUtil CLIENT_UTIL = new DeltaTestUtil("client@example.com");
  private static final DeltaTestUtil SERVER_UTIL = new DeltaTestUtil("server@example.com");

  /**
   * Test multiple server and client ops
   * @throws TransformException
   */
  public void testMultipleClientServerOps() throws TransformException {
    // Client insert ".A.B"
    List<WaveletOperation> client = CollectionUtils.newArrayList();
    client.add(CLIENT_UTIL.insert(1, "A", 1, null));
    client.add(CLIENT_UTIL.insert(3, "B", 0, null));

    // Server insert ".2.1"
    List<WaveletOperation> server = CollectionUtils.newArrayList();
    server.add(SERVER_UTIL.insert(2, "1", 0, null));
    server.add(SERVER_UTIL.insert(1, "2", 2, null));

    DeltaPair pair = new DeltaPair(client, server);
    pair = pair.transform();
    // Expect the transformation of the inserts are correct. If client and server
    // have the same insert point, client op is transformed to the left of the server op.

    // Expect client inserts ".A..B."
    assertEquals(2, pair.getClient().size());
    checkInsert(pair.getClient().get(0), 1, "A", 3);
    checkInsert(pair.getClient().get(1), 4, "B", 1);

    // Expect server inserts "..2..1"
    assertEquals(2, pair.getServer().size());
    checkInsert(pair.getServer().get(0), 4, "1", 0);
    checkInsert(pair.getServer().get(1), 2, "2", 3);
  }

  private void checkInsert(WaveletOperation operation, int location, String content,
      int remaining) {
    if (operation instanceof WaveletBlipOperation) {
      WaveletBlipOperation waveOp = (WaveletBlipOperation) operation;
      if (waveOp.getBlipOp() instanceof BlipContentOperation) {
        BlipContentOperation blipOp = (BlipContentOperation) waveOp.getBlipOp();
        DocOpBuilder builder = new DocOpBuilder();
        builder.retain(location).characters(content);
        if (remaining > 0) {
            builder.retain(remaining);
        }
        assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(builder.build(), blipOp.getContentOp()));
        return;
      }
    }
    fail("Did not get an insertion operation.");
  }

  /**
   * Simple test for deltas that have the same operations and the same author.
   * @throws TransformException
   */
  public void testIsSame() throws TransformException {
    // Client insert ".A.B"
    List<WaveletOperation> client = CollectionUtils.newArrayList();
    client.add(CLIENT_UTIL.insert(1, "A", 1, null));
    client.add(CLIENT_UTIL.insert(3, "B", 0, null));

    // Server insert ".A.B
    HashedVersion resultingVersion = HashedVersion.of(1L, new byte[] {1, 2, 3, 4});
    List<WaveletOperation> server = CollectionUtils.newArrayList();
    // Use CLIENT_UTIL to get the same author info.
    server.add(CLIENT_UTIL.insert(1, "A", 1, null));
    server.add(CLIENT_UTIL.insert(3, "B", 0, resultingVersion));

    // Deltas with same ops are the same, other info should be ignored
    assertTrue(DeltaPair.areSame(client, server));

    // Transforming the ops should result in only version update server ops
    DeltaPair pair = new DeltaPair(client, server);
    pair = pair.transform();
    assertEquals(0, pair.getClient().size());
    assertEquals(2, pair.getServer().size());
    checkVersionUpdate(pair.getServer().get(0), 1, null);
    checkVersionUpdate(pair.getServer().get(1), 1, resultingVersion);
  }

  private void checkVersionUpdate(WaveletOperation operation, long versionIncrement,
      HashedVersion distinctVersion) {
    assertTrue(operation instanceof VersionUpdateOp);
    VersionUpdateOp vop = (VersionUpdateOp) operation;
    assertEquals(versionIncrement, vop.getContext().getVersionIncrement());
    assertEquals(distinctVersion, vop.getContext().getHashedVersion());
  }
}
