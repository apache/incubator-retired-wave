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

package org.waveprotocol.box.server.persistence.migration;

import com.google.common.collect.ImmutableSet;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * An utility class to copy all deltas between storages. Already existing Waves
 * in the target store wont be changed.
 *
 * It is NOT an incremental process.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class DeltaMigrator {

  private static final Log LOG = Log.get(DeltaMigrator.class);

  protected DeltaStore sourceStore = null;
  protected DeltaStore targetStore = null;


  public DeltaMigrator(DeltaStore sourceStore, DeltaStore targetStore) {
    this.sourceStore = sourceStore;
    this.targetStore = targetStore;
  }



  public void run() {


    LOG.info("Starting Wave migration from " + sourceStore.getClass().getSimpleName() + " to "
        + targetStore.getClass().getSimpleName());

    long startTime = System.currentTimeMillis();


    try {

      ExceptionalIterator<WaveId, PersistenceException> srcItr = sourceStore.getWaveIdIterator();

      // Waves
      while (srcItr.hasNext()) {

        WaveId waveId = srcItr.next();

        ImmutableSet<WaveletId> waveletIds = sourceStore.lookup(waveId);

        if (!targetStore.lookup(waveId).isEmpty()) {
          LOG.info("Skipping Wave because it's found in target store : " + waveId.toString());
          continue;
        }

        LOG.info("Migrating Wave : " + waveId.toString() + " with " + waveletIds.size()
            + " wavelets");

        int waveletsTotal = waveletIds.size();
        int waveletsCount = 0;

        // Wavelets
        for (WaveletId waveletId : waveletIds) {

          waveletsCount++;

          LOG.info("Migrating wavelet " + waveletsCount + "/" + waveletsTotal + " : "
              + waveletId.toString());

          DeltasAccess sourceDeltas = sourceStore.open(WaveletName.of(waveId, waveletId));
          DeltasAccess targetDeltas = targetStore.open(WaveletName.of(waveId, waveletId));

          // Get all deltas from last version to initial version (0): reverse
          // order
          int deltasCount = 0;

          ArrayList<WaveletDeltaRecord> deltas = new ArrayList<WaveletDeltaRecord>();
          HashedVersion deltaResultingVersion = sourceDeltas.getEndVersion();

          // Deltas
          while (deltaResultingVersion != null && deltaResultingVersion.getVersion() != 0) {
            deltasCount++;
            WaveletDeltaRecord deltaRecord =
                sourceDeltas.getDeltaByEndVersion(deltaResultingVersion.getVersion());
            deltas.add(deltaRecord);
            // get the previous delta, this is the appliedAt
            deltaResultingVersion = deltaRecord.getAppliedAtVersion();
          }
          LOG.info("Appending " + deltasCount + "deltas to target");
          targetDeltas.append(deltas);
        }
      } // While Waves

      long endTime = System.currentTimeMillis();

      LOG.info("Migration completed. Total time = " + (endTime - startTime) + "ms");

    } catch (PersistenceException e) {

      throw new RuntimeException(e);

    } catch (IOException e) {

      throw new RuntimeException(e);

    }

  }

}
