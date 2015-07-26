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

package org.waveprotocol.box.server.persistence.file;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.util.Pair;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * An index for quickly accessing deltas. The index is an array of longs, one for each version.
 *
 * The index must return the offset of a delta applied at a version, and of a delta leading to
 * a version.
 *
 * Internal format:
 *
 * Let's assume that operations are 10 bytes long. Deltas are separated by |.
 * <pre>
 * Deltas:    |  0  1  2 |  3 |  4  5 |
 *    offset  0          30   40      60
 *
 * Index:        0 -1 -1   30   40 -41
 * </pre>
 * The file contains a negative value for any version for which there is not a delta. This will
 * happen whenever the previous delta contains multiple ops. This negative value is ~offset of
 * the delta containing the op, so that finding the delta leading to a version is easy: just read
 * the previous index entry.
 *
 * @author josephg@google.com (Joseph Gentle)
 */
public class DeltaIndex {
  /** Returned from methods when there is no record for a specified version. */
  public static final int NO_RECORD_FOR_VERSION = -1;

  private static final int RECORD_LENGTH = 8;
  private final File fileRef;
  private RandomAccessFile file;

  public DeltaIndex(File indexFile) {
    this.fileRef = indexFile;
  }

  /**
   * Open the index.
   *
   * @param baseCollection the collection which the index indexes.
   * @throws IOException
   */
  public void openForCollection(FileDeltaCollection baseCollection) throws IOException {
    if (!fileRef.exists()) {
      fileRef.mkdirs();
      rebuildIndexFromDeltas(baseCollection);
    } else {
      // TODO(josephg): For now, we just rebuild the index anyway.
      rebuildIndexFromDeltas(baseCollection);
    }
  }

  private void checkOpen() {
    Preconditions.checkState(file != null, "Index file not open");
  }

  /**
   * Rebuild the index based on a delta collection. This will wipe the index file.
   *
   * @param collection
   * @throws IOException
   */
  public void rebuildIndexFromDeltas(FileDeltaCollection collection) throws IOException {
    if (file != null) {
      file.close();
    }

    if (fileRef.exists()) {
      fileRef.delete();
    }

    file = FileUtils.getOrCreateFile(fileRef);

    for (Pair<Pair<Long, Integer>, Long> pair : collection.getOffsetsIterator()) {
      addDelta(pair.first.first, pair.first.second, pair.second);
    }
  }

  /**
   * Get the delta file offset for the specified version.
   *
   * @param version
   * @return the offset on success, NO_RECORD_FOR_VERSION if there's no record.
   * @throws IOException
   */
  public long getOffsetForVersion(long version) throws IOException {
    if (!seekToPosition(version)) {
      return NO_RECORD_FOR_VERSION;
    }
    long offset = file.readLong();
    return offset < 0 ? NO_RECORD_FOR_VERSION : offset;
  }

  /**
   * Get the delta file offset for the specified end version.
   *
   * @param version
   * @return the offset on success, NO_RECORD_FOR_VERSION if there's no record.
   * @throws IOException
   */
  public long getOffsetForEndVersion(long version) throws IOException {
    if (!seekToPosition(version - 1)) {
      return NO_RECORD_FOR_VERSION;
    }
    long offset = file.readLong();
    try {
      if (file.readLong() < 0) {
        // user tried to read something which isn't an end version
        return NO_RECORD_FOR_VERSION;
      }
    } catch (EOFException e) {
      // it's ok to hit the end of the file, for the last end version
    }
    return offset < 0 ? ~offset : offset;
  }

  /**
   * Seeks to the corresponding version, if it is valid.
   *
   * @param version version to seek to.
   * @return true iff the position is valid
   * @throws IOException
   */
  private boolean seekToPosition(long version) throws IOException {
    if (version < 0) {
      return false;
    }
    checkOpen();

    long position = version * RECORD_LENGTH;
    if (position >= file.length()) {
      return false;
    }

    file.seek(position);
    return true;
  }

  /**
   * Indexes a new delta.
   *
   * @param version the version at which the delta is applied
   * @param numOperations number of operations in the delta
   * @param offset offset at which the delta is stored
   */
  public void addDelta(long version, int numOperations, long offset)
      throws IOException {
    checkOpen();

    long position = version * RECORD_LENGTH;
    // We're expected to append the new delta
    long fileLength = file.length();
    Preconditions.checkState(position == fileLength,
        "position = %d, file=%d", position, fileLength);
    file.seek(position);
    file.writeLong(offset);
    // fill in the additional positions with the 1-complement of the offset,
    for (int i = 1; i < numOperations; i++) {
      file.writeLong(~offset);
    }
  }

  /**
   * @return number of records in the index
   */
  public long length() {
    checkOpen();

    long fileLength;
    try {
      fileLength = file.length();
    } catch (IOException e) {
      // This shouldn't happen in practice.
      throw new RuntimeException("IO error reading index file length", e);
    }
    return fileLength / RECORD_LENGTH;
  }

  public void close() throws IOException {
    if (file != null) {
      file.close();
      file = null;
    }
  }
}
