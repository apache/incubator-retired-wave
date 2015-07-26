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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletDataListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Skeleton implementation of {@link WaveletData}.
 *
 * Implementations must implement the following. The underlying representation
 * of the participant list and expose it via the protected
 * {@link #getMutableParticipants()} method. The underlying mapping from doc ids
 * to documents. The creation of documents from {@link DocumentOperationSink}s
 * via the protected
 * {@link #internalCreateDocument(String, ParticipantId, Collection, DocumentOperationSink, long, long)
 * )} method.
 *
 * @param <B> The document data type.
 */
public abstract class AbstractWaveletData<B extends BlipData> implements ObservableWaveletData {

  /** Id of the wave to which this wavelet belongs. */
  private final WaveId waveId;

  /** The identifier of this wavelet. */
  private final WaveletId id;

  /** The creator of the wavelet. It does not need to be a participant of the wavelet. */
  private final ParticipantId creator;

  /** Version number. */
  private long version;

  /** Wavelet hashed server version. */
  private HashedVersion hashedVersion;

  /** Creation time. */
  private final long creationTime;

  /** Last-modified time. */
  private long lastModifiedTime;

  /** The factory used to create the content of a new document. */
  private final DocumentFactory<?> contentFactory;

  /** The manager for broadcasting listener events. */
  private final WaveletDataListenerManager listenerManager = new WaveletDataListenerManager();

  /**
   * Creates a new wavelet.
   *
   * @param id                id of the wavelet
   * @param creator           creator of the wavelet
   * @param creationTime      timestamp of wavelet creation
   * @param version           initial version of the wavelet
   * @param hashedVersion     initial distinct server version of the wavelet
   * @param lastModifiedTime  initial last-modified time for the wavelet
   * @param waveId            id of the wave containing the wavelet
   * @param contentFactory    factory for creating new documents
   */
  protected AbstractWaveletData(WaveletId id, ParticipantId creator, long creationTime,
      long version, HashedVersion hashedVersion, long lastModifiedTime, WaveId waveId,
      DocumentFactory<?> contentFactory) {
    Preconditions.checkNotNull(id, "id cannot be null");
    Preconditions.checkNotNull(waveId, "wave id cannot be null");

    this.id = id;
    this.creator = creator;
    this.creationTime = creationTime;
    this.version = version;
    this.hashedVersion = hashedVersion;
    this.lastModifiedTime = lastModifiedTime;
    this.waveId = waveId;
    this.contentFactory = contentFactory;
  }

  /**
   * Creates a copy of the given wavelet data, except its documents and
   * participants.
   *
   * @param data The ReadableWaveletData to copy the data from.
   * @param contentFactory Factory for creating new documents.
   */
  protected AbstractWaveletData(ReadableWaveletData data, DocumentFactory<?> contentFactory) {
    this.id = data.getWaveletId();
    this.creator = data.getCreator();
    this.creationTime = data.getCreationTime();
    this.version = data.getVersion();
    this.hashedVersion = data.getHashedVersion();
    this.lastModifiedTime = data.getLastModifiedTime();
    this.waveId = data.getWaveId();
    this.contentFactory = contentFactory;
  }

  /**
   * @return mutable, ordered set of participants.
   */
  protected abstract Set<ParticipantId> getMutableParticipants();

  /**
   * Creates a document in this wavelet (private factory method).
   *
   * @param docId new document id, which must be unique in this wavelet
   * @param author new document's author
   * @param contributors new document's contributors
   * @param contentSink new document's content
   * @param lastModifiedTime new document's last modified time
   * @param lastModifiedVersion new document's last modified version
   * @return a new document data
   */
  protected abstract B internalCreateDocument(String docId, ParticipantId author,
      Collection<ParticipantId> contributors, DocumentOperationSink contentSink,
      long lastModifiedTime, long lastModifiedVersion);

  @Override
  public abstract B getDocument(String documentId);

  @Override
  public Set<ParticipantId> getParticipants() {
    return Collections.unmodifiableSet(getMutableParticipants());
  }

  @Override
  public boolean addParticipant(ParticipantId p) {
    Set<ParticipantId> participants = getMutableParticipants();
    if (participants.contains(p)) {
      return false;
    }
    participants.add(p);
    getListenerManager().onParticipantAdded(this, p);
    return true;
  }

  @Override
  public boolean addParticipant(ParticipantId p, int position) {
    Set<ParticipantId> participants = getMutableParticipants();
    if (participants.contains(p)) {
      return false;
    }
    insert(participants, position, p);
    getListenerManager().onParticipantAdded(this, p);
    return true;
  }

  @Override
  public boolean removeParticipant(ParticipantId p) {
    Set<ParticipantId> participants = getMutableParticipants();
    if (!participants.remove(p)) {
      return false;
    }
    getListenerManager().onParticipantRemoved(this, p);
    return true;
  }

  @Override
  final public ParticipantId getCreator() {
    return creator;
  }

  @Override
  final public long getVersion() {
    return version;
  }

  @Override
  final public HashedVersion getHashedVersion() {
    return hashedVersion;
  }

  @Override
  final public long getCreationTime() {
    return creationTime;
  }

  @Override
  final public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  final public WaveId getWaveId() {
    return waveId;
  }

  @Override
  final public WaveletId getWaveletId() {
    return id;
  }

  @Override
  final public long setVersion(long newVersion) {
    if (version != newVersion) {
      long oldVersion = version;
      version = newVersion;
      listenerManager.onVersionChanged(this, oldVersion, newVersion);
      return oldVersion;
    } else {
      return version;
    }
  }

  @Override
  final public HashedVersion setHashedVersion(HashedVersion newHashedVersion) {
    if (!hashedVersion.equals(newHashedVersion)) {
      HashedVersion oldHashedVersion = hashedVersion;
      hashedVersion = newHashedVersion;
      listenerManager.onHashedVersionChanged(this, oldHashedVersion, newHashedVersion);
      return oldHashedVersion;
    } else {
      return hashedVersion;
    }
  }

  @Override
  final public long setLastModifiedTime(long newTime) {
    if (newTime == lastModifiedTime) {
      return newTime;
    }
    long oldTime = lastModifiedTime;
    lastModifiedTime = newTime;
    listenerManager.onLastModifiedTimeChanged(this, oldTime, newTime);
    return oldTime;
  }

  @Override
  public B createDocument(String docId, ParticipantId author,
      Collection<ParticipantId> contributors, DocInitialization content,
      long lastModifiedTime, long lastModifiedVersion) {
    B doc = internalCreateDocument(docId, author, contributors,
        contentFactory.create(id, docId, content), lastModifiedTime, lastModifiedVersion);
    getListenerManager().onBlipDataAdded(this, doc);
    return doc;
  }

  @Override
  final public void addListener(WaveletDataListener listener) {
    listenerManager.addListener(listener);
  }

  @Override
  final public void removeListener(WaveletDataListener listener) {
    listenerManager.removeListener(listener);
  }

  /**
   * Gets the listener manager for this wavelet data. Package-private for use by
   * BlipDataImpl.
   */
  protected WaveletDataListenerManager getListenerManager() {
    return listenerManager;
  }

  @Override
  final public String toString() {
    // Space before \n in case some logger swallows the newline.
    StringBuilder b = new StringBuilder("WaveletDataImpl: " + getWaveId() + "/" + getWaveletId()
        + " \n[version:" + getVersion() + "]"
        + " \n[creator: " + getCreator() + "]"
        + " \n[participants: " + getParticipants() + "]"
        + " \n[creation time: " + getCreationTime() + "]"
        + " \n[lastModifiedTime:" + getLastModifiedTime() + "]");
    b.append("\n[documents:");
    for (String docId : getDocumentIds()) {
      b.append(" \n  [" + docId + ": " + getDocument(docId) + "]");
    }
    b.append(" \n]");
    return b.toString();
  }

  /**
   * Inserts an element at a specified position in a collection.
   * It does so by iterating through the collection, removing the elements
   * after the specified position, adding the given element at the end,
   * and then adding the removed elements back.
   *
   * Note that this method does the right thing when collection is an
   * insertion-ordered LinkedHashSet. If the collection is a {@link List},
   * use {@link List#add(int, Object)} instead.
   *
   * @param collection A mutable, insertion-ordered collection.
   * @param position The position within the collection to insert the element
   * @param element The element to insert.
   * @throws IndexOutOfBoundsException if {@code position < 0} or
   *   {@code position > collection.size()}.
   */
  private static <T> void insert(Collection<T> collection, int position, T element) {
    Preconditions.checkPositionIndex(position, collection.size());
    if (position == collection.size()) {
      collection.add(element);
      return;
    }
    // We iterate through the collection to insert element at the required position.
    Iterator<T> iterator = collection.iterator();
    // First skip the first position participants.
    for (int i = 0; i < position; i++) {
      iterator.next();
    }
    // Then move the remainder aside.
    List<T> remainder = new ArrayList<T>(collection.size() - position);
    while (iterator.hasNext()) {
      remainder.add(iterator.next());
      iterator.remove();
    }
    Preconditions.checkState(collection.size() == position,
        "size %s != position %s", collection.size(), position);
    // Finally add p at the end and add the remainder back.
    collection.add(element);
    collection.addAll(remainder);
  }

  /**
   * Copies all documents from {@code source}.
   *
   * @param source to get the documents from.
   */
  protected void copyDocuments(ReadableWaveletData source) {
    for (String docId : source.getDocumentIds()) {
      ReadableBlipData docData = source.getDocument(docId);
      this.createDocument(docData.getId(), docData.getAuthor(), docData.getContributors(),
          docData.getContent().asOperation(), docData.getLastModifiedTime(),
          docData.getLastModifiedVersion());
    }
  }

  /**
   * Copies participants from {@code source}.
   *
   * @param source to get the participants from.
   */
  protected void copyParticipants(ReadableWaveletData source) {
    this.getMutableParticipants().addAll(source.getParticipants());
  }
}
