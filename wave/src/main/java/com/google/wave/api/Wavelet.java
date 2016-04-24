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

package com.google.wave.api;

import com.google.wave.api.Participants.Role;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A class that models a single wavelet instance.
 *
 * A single wavelet is composed of metadata, for example, id,creator, creation
 * time, last modified time, title. A wavelet is also composed of a set of
 * participants, a set of tags, a list of blips, and a data document, which
 * is a map of key-value pairs of data.
 */
public class Wavelet implements Serializable {

  /** Delimiter constants for robot participant id. */
  private static final char ROBOT_ID_PROXY_DELIMITER = '+';
  private static final char ROBOT_ID_VERSION_DELIMITER = '#';
  private static final char ROBOT_ID_DOMAIN_DELIMITER = '@';

  /** The id of the wave that owns this wavelet. */
  private transient WaveId waveId;

  /** The id of this wavelet. */
  private transient WaveletId waveletId;

  /** The serialized form of waveId **/
  private String serializedWaveId;

  /** The serialized form of waveletId **/
  private String serializedWaveletId;

  /** The participant id that created this wavelet. */
  private final String creator;

  /** This wavelet's creation time, in milliseconds. */
  private final long creationTime;

  /** This wavelet's last modified time, in milliseconds. */
  private final long lastModifiedTime;

  /** The id of the root blip of this wavelet. */
  private final String rootBlipId;

  /** The root thread of this wavelet. */
  private final BlipThread rootThread;

  /** The participants of this wavelet. */
  private final Participants participants;

  /** The data documents of this wavelet. */
  private final DataDocuments dataDocuments;

  /** The tags that this wavelet has. */
  private final Tags tags;

  /** The title of this wavelet. */
  private String title;

  /** The blips that are contained in this wavelet. */
  @NonJsonSerializable private final Map<String, Blip> blips;

  /** The conversation threads that are contained in this wavelet. */
  @NonJsonSerializable private final Map<String, BlipThread> threads;

  /** The operation queue to queue operation to the robot proxy. */
  @NonJsonSerializable private final OperationQueue operationQueue;

  /** The address of the current robot. */
  @NonJsonSerializable private String robotAddress;

  /**
   * Constructor.
   *
   * @param waveId the id of the wave that owns this wavelet.
   * @param waveletId the id of this wavelet.
   * @param creator the creator of this wavelet.
   * @param creationTime the creation time of this wavelet.
   * @param lastModifiedTime the last modified time of this wavelet.
   * @param title the title of this wavelet.
   * @param rootBlipId the root blip id of this wavelet.
   * @param rootThread the root thread of this wavelet.
   * @param participantRoles the roles for those participants
   * @param participants the participants of this wavelet.
   * @param dataDocuments the data documents of this wavelet.
   * @param tags the tags that this wavelet has.
   * @param blips the blips that are contained in this wavelet.
   * @param threads the conversation threads that are contained in this wavelet.
   * @param operationQueue the operation queue to queue operation to the robot
   *     proxy.
   */
  Wavelet(WaveId waveId, WaveletId waveletId, String creator, long creationTime,
      long lastModifiedTime, String title, String rootBlipId, BlipThread rootThread,
      Map<String, String> participantRoles, Set<String> participants,
      Map<String, String> dataDocuments, Set<String> tags, Map<String, Blip> blips,
      Map<String, BlipThread> threads, OperationQueue operationQueue) {
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.creator = creator;
    this.creationTime = creationTime;
    this.lastModifiedTime = lastModifiedTime;
    this.title = title;
    this.rootBlipId = rootBlipId;
    this.rootThread = rootThread;
    this.participants = new Participants(participants, participantRoles, this, operationQueue);
    this.dataDocuments = new DataDocuments(dataDocuments, this, operationQueue);
    this.tags = new Tags(tags, this, operationQueue);
    this.blips = blips;
    this.threads = threads;
    this.operationQueue = operationQueue;
  }

  /**
   * Constructor.
   *
   * @param waveId the id of the wave that owns this wavelet.
   * @param waveletId the id of this wavelet.
   * @param rootBlipId the root blip id of this wavelet.
   * @param rootThread the root thread of this wavelet.
   * @param participants the participants of this wavelet.
   * @param participantRoles the roles for those participants.
   * @param blips the blips that are contained in this wavelet.
   * @param threads the conversation threads that are contained in this wavelet.
   * @param operationQueue the operation queue to queue operation to the robot
   *     proxy.
   */
  Wavelet(WaveId waveId, WaveletId waveletId, String rootBlipId, BlipThread rootThread,
      Set<String> participants, Map<String, String> participantRoles, Map<String, Blip> blips,
      Map<String, BlipThread> threads, OperationQueue operationQueue) {
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.rootBlipId = rootBlipId;
    this.rootThread = rootThread;
    this.creator = null;
    this.creationTime = -1;
    this.lastModifiedTime = -1;
    this.title = null;
    this.participants = new Participants(participants, participantRoles, this, operationQueue);
    this.dataDocuments = new DataDocuments(new HashMap<String, String>(), this,
        operationQueue);
    this.tags = new Tags(Collections.<String>emptySet(), this, operationQueue);
    this.blips = blips;
    this.threads = threads;
    this.operationQueue = operationQueue;
  }

  /**
   * Shallow copy constructor.
   *
   * @param other the other {@link Wavelet} instance to copy.
   * @param operationQueue the operation queue for this new wavelet instance.
   */
  private Wavelet(Wavelet other, OperationQueue operationQueue) {
    this.waveId = other.waveId;
    this.waveletId = other.waveletId;
    this.creator = other.creator;
    this.creationTime = other.creationTime;
    this.lastModifiedTime = other.lastModifiedTime;
    this.title = other.title;
    this.rootBlipId = other.rootBlipId;
    this.rootThread = other.rootThread;
    this.participants = other.participants;
    this.dataDocuments = other.dataDocuments;
    this.tags = other.tags;
    this.blips = other.blips;
    this.threads = other.threads;
    this.robotAddress = other.robotAddress;
    this.operationQueue = operationQueue;
  }

  /**
   * Returns the wave id that owns this wavelet.
   *
   * @return the wave id.
   */
  public WaveId getWaveId() {
    return waveId;
  }

  /**
   * Returns the id of this wavelet.
   *
   * @return the wavelet id.
   */
  public WaveletId getWaveletId() {
    return waveletId;
  }

  /**
   * Returns the participant id of the creator of this wavelet.
   *
   * @return the creator of this wavelet.
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Returns the creation time of this wavelet.
   *
   * @return the wavelet's creation time.
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * Returns the last modified time of this wavelet.
   *
   * @return the wavelet's last modified time.
   */
  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  /**
   * Returns the data documents of this wavelet, which is a series of key-value
   * pairs of data.
   *
   * @return an instance of {@link DataDocuments}, which represents the data
   *     documents of this wavelet.
   */
  public DataDocuments getDataDocuments() {
    return dataDocuments;
  }

  /**
   * Returns a list of participants of this wavelet.
   *
   * @return an instance of {@link Participants}, which represents the
   *     participants of this wavelet.
   */
  public Participants getParticipants() {
    return participants;
  }

  /**
   * Returns a list of tags of this wavelet.
   *
   * @return an instance of {@link Tags}, which represents the tags that have
   *     been associated with this wavelet.
   */
  public Tags getTags() {
    return tags;
  }

  /**
   * Returns the title of this wavelet.
   *
   * @return the wavelet title.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the wavelet title.
   *
   * @param title the new title to be set.
   */
  public void setTitle(String title) {
    if (title.contains("\n")) {
      throw new IllegalArgumentException("Wavelet title should not contain a newline character. " +
          "Specified: " + title);
    }

    operationQueue.setTitleOfWavelet(this, title);
    this.title = title;

    // Adjust the content of the root blip, if it is available in the context.
    Blip rootBlip = getRootBlip();
    if (rootBlip != null) {
      String content = "\n";
      int indexOfSecondNewline = rootBlip.getContent().indexOf('\n', 1);
      if (indexOfSecondNewline != -1) {
        content = rootBlip.getContent().substring(indexOfSecondNewline);
      }
      rootBlip.setContent("\n" + title + content);
    }
  }

  /**
   * Returns the address of the robot that receives events or performs events
   * on this wavelet.
   *
   * @return the robot address.
   */
  public String getRobotAddress() {
    return robotAddress;
  }

  /**
   * Sets the address of the robot that receives events or performs events on
   * this wavelet.
   *
   * @param address the robot address.
   * @throw IllegalStateException if this method has been called before.
   */
  public void setRobotAddress(String address) {
    if (this.robotAddress != null) {
      throw new IllegalStateException("Robot address has been set previously to " +
          this.robotAddress);
    }
    this.robotAddress = address;
  }

  /**
   * Returns the id of the root blip of this wavelet.
   *
   * @return the id of the root blip.
   */
  public String getRootBlipId() {
    return rootBlipId;
  }

  /**
   * Returns the root blip of this wavelet.
   *
   * @return an instance of {@link Blip} that represents the root blip of this
   *     wavelet.
   */
  public Blip getRootBlip() {
    return blips.get(rootBlipId);
  }

  /**
   * @return an instance of {@link BlipThread} that represents the root thread of
   *     this wavelet.
   */
  public BlipThread getRootThread() {
    return rootThread;
  }

  /**
   * Returns a blip with the given id.
   *
   * @return an instance of {@link Blip} that has the given blip id.
   */
  public Blip getBlip(String blipId) {
    return blips.get(blipId);
  }

  /**
   * Returns all blips that are in this wavelet.
   *
   * @return a map of blips in this wavelet, that is keyed by blip id.
   */
  public Map<String, Blip> getBlips() {
    return blips;
  }

  /**
   * Returns a thread with the given id.
   *
   * @param threadId the thread id.
   * @return a thread that has the given thread id.
   */
  public BlipThread getThread(String threadId) {
    if (threadId == null || threadId.isEmpty()) {
      return getRootThread();
    }
    return threads.get(threadId);
  }

  /**
   * Returns all threads that are in this wavelet.
   *
   * @return a map of threads in this wavelet, that is keyed by thread id.
   */
  public Map<String, BlipThread> getThreads() {
    return threads;
  }

  /**
   * Adds a thread to this wavelet.
   *
   * @param thread the thread to add.
   */
  protected void addThread(BlipThread thread) {
    threads.put(thread.getId(), thread);
  }

  /**
   * Returns the operation queue that this wavelet uses to queue operation to
   * the robot proxy.
   *
   * @return an instance of {@link OperationQueue} that represents this
   *     wavelet's operation queue.
   */
  protected OperationQueue getOperationQueue() {
    return operationQueue;
  }

  /**
   * Returns the domain of this wavelet.
   *
   * @return the wavelet domain, which is encoded in the wave/wavelet id.
   */
  public String getDomain() {
    return waveId.getDomain();
  }

  /**
   * Returns a view of this wavelet that will proxy for the specified id.
   *
   * A shallow copy of the current wavelet is returned with the
   * {@code proxyingFor} field set. Any modifications made to this copy will be
   * done using the {@code proxyForId}, i.e. the
   * {@code robot+<proxyForId>@appspot.com} address will be used.
   *
   * If the wavelet was retrieved using the Active Robot API, that is
   * by {@code fetchWavelet}, then the address of the robot must be added to the
   * wavelet by calling {@code setRobotAddress} with the robot's address
   * before calling {@code proxy_for}.
   *
   * @param proxyForId the id to proxy. Please note that this parameter should
   *     be properly encoded to ensure that the resulting participant id is
   *     valid (see {@link Util#checkIsValidProxyForId(String)} for more
   *     details).
   * @return a shallow copy of this wavelet with the proxying information set.
   */
  public Wavelet proxyFor(String proxyForId) {
    Util.checkIsValidProxyForId(proxyForId);
    addProxyingParticipant(proxyForId);
    OperationQueue proxiedOperationQueue = operationQueue.proxyFor(proxyForId);
    return new Wavelet(this, proxiedOperationQueue);
  }

  /**
   * Submit this wavelet when the given {@code other} wavelet is submitted.
   *
   * Wavelets constructed outside of the event callback need to
   * be either explicitly submitted using {@code AbstractRobot.submit(Wavelet)}
   * or be associated with a different wavelet that will be submitted or is part
   * of the event callback.
   *
   * @param other the other wavelet whose operation queue will be joined with.
   */
  public void submitWith(Wavelet other) {
    operationQueue.submitWith(other.operationQueue);
  }

  /**
   * Replies to the conversation in this wavelet.
   *
   * @param initialContent the initial content of the reply.
   * @return an instance of {@link Blip} that represents a transient version of
   *     the reply.
   *
   * @throws IllegalArgumentException if {@code initialContent} does not start
   *     with a newline character.
   */
  public Blip reply(String initialContent) {
    if (initialContent == null || !initialContent.startsWith("\n")) {
      throw new IllegalArgumentException("Initial content should start with a newline character");
    }
    return operationQueue.appendBlipToWavelet(this, initialContent);
  }

  /**
   * Removes a blip from this wavelet.
   *
   * @param blip the blip to be removed.
   */
  public void delete(Blip blip) {
    delete(blip.getBlipId());
  }

  /**
   * Removes a blip from this wavelet.
   *
   * @param blipId the id of the blip to be removed.
   */
  public void delete(String blipId) {
    operationQueue.deleteBlip(this, blipId);
    Blip removed = blips.remove(blipId);

    if (removed != null) {
      // Remove the blip from the parent blip.
      Blip parentBlip = removed.getParentBlip();
      if (parentBlip != null) {
        parentBlip.deleteChildBlipId(blipId);
      }

      // Remove the blip from the containing thread.
      BlipThread thread = removed.getThread();
      if (thread != null) {
        thread.removeBlip(removed);
      }

      // If the containing thread is now empty, remove it from the parent blip
      // and from the wavelet.
      if (thread != null && parentBlip != null && thread.isEmpty()) {
        parentBlip.removeThread(thread);
        threads.remove(thread.getId());
      }
    }
  }

  /**
   * Ads a proxying participant to the wave.
   *
   * Proxying participants are of the form {@code robot+proxy@domain.com}. This
   * convenience method constructs this id and then calls
   * {@code wavelet.addParticipant()} operation.
   *
   * @param proxyForId the id to proxy.
   */
  private void addProxyingParticipant(String proxyForId) {
    if (robotAddress == null || robotAddress.isEmpty()) {
      throw new IllegalStateException("Need a robot address to add a proxying for participant.");
    }

    // Parse the id and the domain.
    int index = robotAddress.indexOf(ROBOT_ID_DOMAIN_DELIMITER);
    String newId = robotAddress.substring(0, index);
    String domain = robotAddress.substring(index + 1);

    // Parse the version.
    String version = null;
    index = newId.indexOf(ROBOT_ID_VERSION_DELIMITER);
    if (index != -1) {
      version = newId.substring(index + 1);
      newId = newId.substring(0, index);
    }

    // Remove the previous proxying id.
    index = newId.indexOf(ROBOT_ID_PROXY_DELIMITER);
    if (index != -1) {
      newId = newId.substring(0, index);
    }

    // Assemble the new id.
    newId += ROBOT_ID_PROXY_DELIMITER + proxyForId;
    if (version != null) {
      newId += ROBOT_ID_VERSION_DELIMITER + version;
    }
    newId += ROBOT_ID_DOMAIN_DELIMITER + domain;
    participants.add(newId);
  }

  /**
   * Serializes this {@link Wavelet} into a {@link WaveletData}.
   *
   * @return an instance of {@link WaveletData} that represents this wavelet.
   */
  public WaveletData serialize() {
    WaveletData waveletData = new WaveletData();

    // Add primitive properties.
    waveletData.setWaveId(ApiIdSerializer.instance().serialiseWaveId(waveId));
    waveletData.setWaveletId(ApiIdSerializer.instance().serialiseWaveletId(waveletId));
    waveletData.setCreator(creator);
    waveletData.setCreationTime(creationTime);
    waveletData.setLastModifiedTime(lastModifiedTime);
    waveletData.setRootBlipId(rootBlipId);
    waveletData.setRootThread(rootThread);
    waveletData.setTitle(title);

    // Add tags.
    List<String> tags = new ArrayList<String>();
    for (String tag : this.tags) {
      tags.add(tag);
    }
    waveletData.setTags(tags);

    // Add participants.
    List<String> participants = new ArrayList<String>();
    for (String participant : this.participants) {
      participants.add(participant);
      Role role = getParticipants().getParticipantRole(participant);
      waveletData.setParticipantRole(participant, role.name());
    }
    waveletData.setParticipants(participants);

    // Add data documents.
    Map<String, String> dataDocuments = new HashMap<String, String>();
    for (Entry<String, String> entry : this.dataDocuments) {
      dataDocuments.put(entry.getKey(), entry.getValue());
    }
    waveletData.setDataDocuments(dataDocuments);

    return waveletData;
  }

  /**
   * Deserializes the given {@link WaveletData} object into an instance of
   * {@link Wavelet}.
   *
   * @param operationQueue the operation queue.
   * @param blips the map of blips that are in this wavelet.
   * @param waveletData the wavelet data to be deserialized.
   * @return an instance of {@link Wavelet}.
   */
  public static Wavelet deserialize(OperationQueue operationQueue, Map<String, Blip> blips,
      Map<String, BlipThread> threads, WaveletData waveletData) {
    WaveId waveId;
    WaveletId waveletId;
    try {
      waveId = ApiIdSerializer.instance().deserialiseWaveId(waveletData.getWaveId());
      waveletId = ApiIdSerializer.instance().deserialiseWaveletId(waveletData.getWaveletId());
    } catch (InvalidIdException e) {
      throw new IllegalArgumentException(e);
    }
    String creator = waveletData.getCreator();
    long creationTime = waveletData.getCreationTime();
    long lastModifiedTime = waveletData.getLastModifiedTime();
    String rootBlipId = waveletData.getRootBlipId();

    BlipThread originalRootThread = waveletData.getRootThread();
    List<String> rootThreadBlipIds = originalRootThread == null ?
        new ArrayList<String>() :
        new ArrayList<String>(originalRootThread.getBlipIds());
    BlipThread rootThread = new BlipThread("", -1, rootThreadBlipIds, blips);

    String title = waveletData.getTitle();
    Set<String> participants = new LinkedHashSet<String>(waveletData.getParticipants());
    Set<String> tags = new LinkedHashSet<String>(waveletData.getTags());
    Map<String, String> dataDocuments = waveletData.getDataDocuments();
    Map<String, String> roles = waveletData.getParticipantRoles();

    return new Wavelet(waveId, waveletId, creator, creationTime, lastModifiedTime, title,
        rootBlipId, rootThread, roles, participants, dataDocuments, tags, blips, threads,
        operationQueue);
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    serializedWaveId = ApiIdSerializer.instance().serialiseWaveId(waveId);
    serializedWaveletId = ApiIdSerializer.instance().serialiseWaveletId(waveletId);
    out.defaultWriteObject();
  }

  private void readObject(ObjectInputStream in) throws IOException, InvalidIdException {
    try {
      in.defaultReadObject();
    } catch(ClassNotFoundException e) {
      // Fatal.
      throw new IOException("Incorrect serial versions" + e);
    }

    waveId = ApiIdSerializer.instance().deserialiseWaveId(serializedWaveId);
    waveletId = ApiIdSerializer.instance().deserialiseWaveletId(serializedWaveletId);
  }
}
