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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreDataSerializer;
import org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta;
import org.waveprotocol.box.server.waveserver.AppliedDeltaUtil;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.util.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * A flat file based implementation of DeltasAccess. This class provides a storage backend for the
 * deltas in a single wavelet.
 *
 * The file starts with a header. The header contains the version of the file protocol. After the
 * version, the file contains a sequence of delta records. Each record contains a header followed
 * by a WaveletDeltaRecord.
 *
 * A particular FileDeltaCollection instance assumes that it's <em>the only one</em> reading and
 * writing a particular wavelet. The methods are <em>not</em> multithread-safe.
 *
 * See this document for design specifics:
 * https://sites.google.com/a/waveprotocol.org/wave-protocol/protocol/design-proposals/wave-store-design-for-wave-in-a-box
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FileDeltaCollection implements DeltasAccess {
  public static final String DELTAS_FILE_SUFFIX = ".deltas";
  public static final String INDEX_FILE_SUFFIX = ".index";

  private static final byte[] FILE_MAGIC_BYTES = new byte[]{'W', 'A', 'V', 'E'};
  private static final int FILE_PROTOCOL_VERSION = 1;
  private static final int FILE_HEADER_LENGTH = 8;

  private static final int DELTA_PROTOCOL_VERSION = 1;

  private static final Log LOG = Log.get(FileDeltaCollection.class);

  private final WaveletName waveletName;
  private final RandomAccessFile file;
  private final DeltaIndex index;

  private HashedVersion endVersion;
  private boolean isOpen;

  /**
   * A single record in the delta file.
   */
  private class DeltaHeader {
    /** Length in bytes of the header */
    public static final int HEADER_LENGTH = 12;

    /** The protocol version of the remaining fields. For now, must be 1. */
    public final int protoVersion;

    /** The length of the applied delta segment, in bytes. */
    public final int appliedDeltaLength;
    public final int transformedDeltaLength;

    public DeltaHeader(int protoVersion, int appliedDeltaLength, int transformedDeltaLength) {
      this.protoVersion = protoVersion;
      this.appliedDeltaLength = appliedDeltaLength;
      this.transformedDeltaLength = transformedDeltaLength;
    }

    public void checkVersion() throws IOException {
      if (protoVersion != DELTA_PROTOCOL_VERSION) {
        throw new IOException("Invalid delta header");
      }
    }
  }

  /**
   * Opens a file delta collection.
   *
   * @param waveletName name of the wavelet to open
   * @param basePath base path of files
   * @return an open collection
   * @throws IOException
   */
  public static FileDeltaCollection open(WaveletName waveletName, String basePath)
      throws IOException {
    Preconditions.checkNotNull(waveletName, "null wavelet name");

    RandomAccessFile deltaFile = FileUtils.getOrCreateFile(deltasFile(basePath, waveletName));
    setOrCheckFileHeader(deltaFile);
    DeltaIndex index = new DeltaIndex(indexFile(basePath, waveletName));

    FileDeltaCollection collection = new FileDeltaCollection(waveletName, deltaFile, index);

    index.openForCollection(collection);
    collection.initializeEndVersionAndTruncateTrailingJunk();
    return collection;
  }

  /**
   * Delete the delta files from disk.
   *
   * @throws PersistenceException
   */
  public static void delete(WaveletName waveletName, String basePath) throws PersistenceException {
    String error = "";
    File deltas = deltasFile(basePath, waveletName);

    if (deltas.exists()) {
      if (!deltas.delete()) {
        error += "Could not delete deltas file: " + deltas.getAbsolutePath() + ". ";
      }
    }

    File index = indexFile(basePath, waveletName);
    if (index.exists()) {
      if (!index.delete()) {
        error += "Could not delete index file: " + index.getAbsolutePath();
      }
    }
    if (!error.isEmpty()) {
      throw new PersistenceException(error);
    }
  }

  /**
   * Create a new file delta collection for the given wavelet.
   *
   * @param waveletName name of the wavelet
   * @param deltaFile the file of deltas
   * @param index index into deltas
   */
  public FileDeltaCollection(WaveletName waveletName, RandomAccessFile deltaFile,
      DeltaIndex index) {
    this.waveletName = waveletName;
    this.file = deltaFile;
    this.index = index;
    this.isOpen = true;
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  @Override
  public HashedVersion getEndVersion() {
    return endVersion;
  }

  @Override
  public WaveletDeltaRecord getDelta(long version) throws IOException {
    checkIsOpen();
    return seekToRecord(version) ? readRecord() : null;
  }

  @Override
  public WaveletDeltaRecord getDeltaByEndVersion(long version) throws IOException {
    checkIsOpen();
    return seekToEndRecord(version) ? readRecord() : null;
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version)
      throws IOException {
    checkIsOpen();
    return seekToRecord(version) ? readAppliedDeltaFromRecord() : null;
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(long version) throws IOException {
    checkIsOpen();
    return seekToRecord(version) ? readTransformedDeltaFromRecord() : null;
  }

  @Override
  public HashedVersion getAppliedAtVersion(long version) throws IOException {
    checkIsOpen();
    ByteStringMessage<ProtocolAppliedWaveletDelta> applied = getAppliedDelta(version);

    return (applied != null) ? AppliedDeltaUtil.getHashedVersionAppliedAt(applied) : null;
  }

  @Override
  public HashedVersion getResultingVersion(long version) throws IOException {
    checkIsOpen();
    TransformedWaveletDelta transformed = getTransformedDelta(version);

    return (transformed != null) ? transformed.getResultingVersion() : null;
  }

  @Override
  public void close() throws IOException {
    file.close();
    index.close();
    endVersion = null;
    isOpen = false;
  }

  @Override
  public void append(Collection<WaveletDeltaRecord> deltas) throws PersistenceException {
    checkIsOpen();
    try {
      file.seek(file.length());

      WaveletDeltaRecord lastDelta = null;
      for (WaveletDeltaRecord delta : deltas) {
        index.addDelta(delta.getTransformedDelta().getAppliedAtVersion(),
            delta.getTransformedDelta().size(),
            file.getFilePointer());
        writeDelta(delta);
        lastDelta = delta;
      }

      // fsync() before returning.
      file.getChannel().force(true);
      endVersion = lastDelta.getTransformedDelta().getResultingVersion();
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public boolean isEmpty() {
    checkIsOpen();
    return index.length() == 0;
  }

  /**
   * Creates a new iterator to move over the positions of the deltas in the file.
   *
   * Each pair returned is ((version, numOperations), offset).
   * @throws IOException
   */
  Iterable<Pair<Pair<Long,Integer>, Long>> getOffsetsIterator() throws IOException {
    checkIsOpen();

    return new Iterable<Pair<Pair<Long, Integer>, Long>>() {
      @Override
      public Iterator<Pair<Pair<Long, Integer>, Long>> iterator() {
        return new Iterator<Pair<Pair<Long, Integer>, Long>>() {
          Pair<Pair<Long, Integer>, Long> nextRecord;
          long nextPosition = FILE_HEADER_LENGTH;

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Pair<Pair<Long, Integer>, Long> next() {
            Pair<Pair<Long, Integer>, Long> record = nextRecord;
            nextRecord = null;
            return record;
          }

          @Override
          public boolean hasNext() {
            // We're using hasNext to prime the next call to next(). This works because in practice
            // any call to next() is preceeded by at least one call to hasNext().
            // We need to actually read the record here because hasNext() should return false
            // if there's any incomplete data at the end of the file.
            try {
              if (file.length() <= nextPosition) {
                // End of file.
                return false;
              }
            } catch (IOException e) {
              throw new RuntimeException("Could not get file position", e);
            }

            if (nextRecord == null) {
              // Read the next record
              try {
                file.seek(nextPosition);
                TransformedWaveletDelta transformed = readTransformedDeltaFromRecord();
                nextRecord = Pair.of(Pair.of(transformed.getAppliedAtVersion(),
                        transformed.size()), nextPosition);
                nextPosition = file.getFilePointer();
              } catch (IOException e) {
                // The next entry is invalid. There was probably a write error / crash.
                LOG.severe("Error reading delta file for " + waveletName + " starting at " +
                    nextPosition, e);
                return false;
              }
            }

            return true;
          }
        };
      }
    };
  }

  @VisibleForTesting
  static final File deltasFile(String basePath, WaveletName waveletName) {
    String waveletPathPrefix = FileUtils.waveletNameToPathSegment(waveletName);
    return new File(basePath, waveletPathPrefix + DELTAS_FILE_SUFFIX);
  }

  @VisibleForTesting
  static final File indexFile(String basePath, WaveletName waveletName) {
    String waveletPathPrefix = FileUtils.waveletNameToPathSegment(waveletName);
    return new File(basePath, waveletPathPrefix + INDEX_FILE_SUFFIX);
  }

  /**
   * Checks that a file has a valid deltas header, adding the header if the
   * file is shorter than the header.
   */
  private static void setOrCheckFileHeader(RandomAccessFile file) throws IOException {
    Preconditions.checkNotNull(file);
    file.seek(0);

    if (file.length() < FILE_HEADER_LENGTH) {
      // The file is new. Insert a header.
      file.write(FILE_MAGIC_BYTES);
      file.writeInt(FILE_PROTOCOL_VERSION);
    } else {
      byte[] magic = new byte[4];
      file.readFully(magic);
      if (!Arrays.equals(FILE_MAGIC_BYTES, magic)) {
        throw new IOException("Delta file magic bytes are incorrect");
      }

      int version = file.readInt();
      if (version != FILE_PROTOCOL_VERSION) {
        throw new IOException(String.format("File protocol version mismatch - expected %d got %d",
            FILE_PROTOCOL_VERSION, version));
      }
    }
  }

  private void checkIsOpen() {
    Preconditions.checkState(isOpen, "Delta collection closed");
  }

  /**
   * Seek to the start of a delta record. Returns false if the record doesn't exist.
   */
  private boolean seekToRecord(long version) throws IOException {
    Preconditions.checkArgument(version >= 0, "Version can't be negative");
    long offset = index.getOffsetForVersion(version);
    return seekTo(offset);
  }

  /**
   * Seek to the start of a delta record given its end version.
   * Returns false if the record doesn't exist.
   */
  private boolean seekToEndRecord(long version) throws IOException {
    Preconditions.checkArgument(version >= 0, "Version can't be negative");
    long offset = index.getOffsetForEndVersion(version);
    return seekTo(offset);
  }

  private boolean seekTo(long offset) throws IOException {
    if (offset == DeltaIndex.NO_RECORD_FOR_VERSION) {
      // There's no record for the specified version.
      return false;
    } else {
      file.seek(offset);
      return true;
    }
  }

  /**
   * Read a record and return it.
   */
  private WaveletDeltaRecord readRecord() throws IOException {
    DeltaHeader header = readDeltaHeader();

    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        readAppliedDelta(header.appliedDeltaLength);
    TransformedWaveletDelta transformedDelta = readTransformedWaveletDelta(
        header.transformedDeltaLength);

    return new WaveletDeltaRecord(AppliedDeltaUtil.getHashedVersionAppliedAt(appliedDelta),
        appliedDelta, transformedDelta);
  }

  /**
   * Reads a record, and only parses & returns the applied data field.
   */
  private ByteStringMessage<ProtocolAppliedWaveletDelta> readAppliedDeltaFromRecord()
      throws IOException {
    DeltaHeader header = readDeltaHeader();

    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        readAppliedDelta(header.appliedDeltaLength);
    file.skipBytes(header.transformedDeltaLength);

    return appliedDelta;
  }

  /**
   * Reads a record, and only parses & returns the transformed data field.
   */
  private TransformedWaveletDelta readTransformedDeltaFromRecord() throws IOException {
    DeltaHeader header = readDeltaHeader();

    file.skipBytes(header.appliedDeltaLength);
    TransformedWaveletDelta transformedDelta = readTransformedWaveletDelta(
        header.transformedDeltaLength);

    return transformedDelta;
  }


  // *** Low level data reading methods

  /** Read a header from the file. Does not move the file pointer before reading. */
  private DeltaHeader readDeltaHeader() throws IOException {
    int version = file.readInt();
    if (version != DELTA_PROTOCOL_VERSION) {
      throw new IOException("Delta header invalid");
    }
    int appliedDeltaLength = file.readInt();
    int transformedDeltaLength = file.readInt();
    DeltaHeader deltaHeader = new DeltaHeader(version, appliedDeltaLength, transformedDeltaLength);
    deltaHeader.checkVersion();
    // Verify the file size.
    long remaining = file.length() - file.getFilePointer();
    long missing = (appliedDeltaLength + transformedDeltaLength) - remaining;
    if (missing > 0) {
      throw new IOException("File is corrupted, missing " + missing + " bytes");
    }
    return deltaHeader;
  }

  /**
   * Write a header to the current location in the file
   */
  private void writeDeltaHeader(DeltaHeader header) throws IOException {
    file.writeInt(header.protoVersion);
    file.writeInt(header.appliedDeltaLength);
    file.writeInt(header.transformedDeltaLength);
  }

  /**
   * Read the applied delta at the current file position. After method call,
   * file position is directly after applied delta field.
   */
  private ByteStringMessage<ProtocolAppliedWaveletDelta> readAppliedDelta(int length)
      throws IOException {
    if (length == 0) {
      return null;
    }

    byte[] bytes = new byte[length];
    file.readFully(bytes);
    try {
      return ByteStringMessage.parseProtocolAppliedWaveletDelta(ByteString.copyFrom(bytes));
    } catch (InvalidProtocolBufferException e) {
      throw new IOException(e);
    }
  }

  /**
   * Write an applied delta to the current position in the file. Returns number of bytes written.
   */
  private int writeAppliedDelta(ByteStringMessage<ProtocolAppliedWaveletDelta> delta)
      throws IOException {
    if (delta != null) {
      byte[] bytes = delta.getByteArray();
      file.write(bytes);
      return bytes.length;
    } else {
      return 0;
    }
  }

  /**
   * Read a {@link TransformedWaveletDelta} from the current location in the file.
   */
  private TransformedWaveletDelta readTransformedWaveletDelta(int transformedDeltaLength)
      throws IOException {
    if(transformedDeltaLength < 0) {
      throw new IOException("Invalid delta length");
    }

    byte[] bytes = new byte[transformedDeltaLength];
    file.readFully(bytes);
    ProtoTransformedWaveletDelta delta;
    try {
      delta = ProtoTransformedWaveletDelta.parseFrom(bytes);
    } catch (InvalidProtocolBufferException e) {
      throw new IOException(e);
    }
    return ProtoDeltaStoreDataSerializer.deserialize(delta);
  }

  /**
   * Write a {@link TransformedWaveletDelta} to the file at the current location.
   * @return length of written data
   */
  private int writeTransformedWaveletDelta(TransformedWaveletDelta delta) throws IOException {
    long startingPosition = file.getFilePointer();
    ProtoTransformedWaveletDelta protoDelta = ProtoDeltaStoreDataSerializer.serialize(delta);
    OutputStream stream = Channels.newOutputStream(file.getChannel());
    protoDelta.writeTo(stream);
    return (int) (file.getFilePointer() - startingPosition);
  }

  /**
   * Read a delta to the file. Does not move the file pointer before writing. Returns number of
   * bytes written.
   */
  private long writeDelta(WaveletDeltaRecord delta) throws IOException {
    // We'll write zeros in place of the header and come back & write it at the end.
    long headerPointer = file.getFilePointer();
    file.write(new byte[DeltaHeader.HEADER_LENGTH]);

    int appliedLength = writeAppliedDelta(delta.getAppliedDelta());
    int transformedLength = writeTransformedWaveletDelta(delta.getTransformedDelta());

    long endPointer = file.getFilePointer();
    file.seek(headerPointer);
    writeDeltaHeader(new DeltaHeader(DELTA_PROTOCOL_VERSION, appliedLength, transformedLength));
    file.seek(endPointer);

    return endPointer - headerPointer;
  }

  /**
   * Reads the last complete record in the deltas file and truncates any trailing junk.
   */
  private void initializeEndVersionAndTruncateTrailingJunk() throws IOException {
    long numRecords = index.length();
    if (numRecords >= 1) {
      endVersion = getDeltaByEndVersion(numRecords).getResultingVersion();
    } else {
      endVersion = null;
    }
    // The file's position should be at the end. Truncate any
    // trailing junk such as from a partially completed write.
    file.setLength(file.getFilePointer());
  }
}
