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

package org.waveprotocol.box.server.persistence.mongodb;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.wave.api.Context;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import org.bson.types.BasicBSONList;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.attachment.AttachmentProto;
import org.waveprotocol.box.attachment.proto.AttachmentMetadataProtoImpl;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.SignerInfoStore;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <b>CertPathStore:</b><br/>
 * <i>Collection(signerInfo):</i>
 * <ul>
 * <li>_id : signerId byte array.</li>
 * <li>protoBuff : byte array representing the protobuff message of a
 * {@link ProtocolSignerInfo}.</li>
 * </ul>
 * <p>
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author josephg@gmail.com (Joseph Gentle)
 *
 */
public final class MongoDbStore implements SignerInfoStore, AttachmentStore, AccountStore {

  private static final String ACCOUNT_COLLECTION = "account";
  private static final String ACCOUNT_HUMAN_DATA_FIELD = "human";
  private static final String ACCOUNT_ROBOT_DATA_FIELD = "robot";

  private static final String HUMAN_PASSWORD_FIELD = "passwordDigest";

  private static final String PASSWORD_DIGEST_FIELD = "digest";
  private static final String PASSWORD_SALT_FIELD = "salt";

  private static final String ROBOT_URL_FIELD = "url";
  private static final String ROBOT_SECRET_FIELD = "secret";
  private static final String ROBOT_CAPABILITIES_FIELD = "capabilities";
  private static final String ROBOT_VERIFIED_FIELD = "verified";

  private static final String CAPABILITIES_VERSION_FIELD = "version";
  private static final String CAPABILITIES_HASH_FIELD = "capabilitiesHash";
  private static final String CAPABILITIES_CAPABILITIES_FIELD = "capabilities";
  private static final String CAPABILITY_CONTEXTS_FIELD = "contexts";
  private static final String CAPABILITY_FILTER_FIELD = "filter";

  private static final Logger LOG = Logger.getLogger(MongoDbStore.class.getName());

  private final DB database;
  private final GridFS attachmentGrid;
  private final GridFS thumbnailGrid;
  private final GridFS metadataGrid;

  MongoDbStore(DB database) {
    this.database = database;
    attachmentGrid = new GridFS(database, "attachments");
    thumbnailGrid = new GridFS(database, "thumbnails");
    metadataGrid = new GridFS(database, "metadata");
  }

  @Override
  public void initializeSignerInfoStore() throws PersistenceException {
    // Nothing to initialize
  }

  @Override
  public SignerInfo getSignerInfo(byte[] signerId) {
    DBObject query = getDBObjectForSignerId(signerId);
    DBCollection signerInfoCollection = getSignerInfoCollection();
    DBObject signerInfoDBObject = signerInfoCollection.findOne(query);

    // Sub-class contract specifies return null when not found
    SignerInfo signerInfo = null;

    if (signerInfoDBObject != null) {
      byte[] protobuff = (byte[]) signerInfoDBObject.get("protoBuff");
      try {
        signerInfo = new SignerInfo(ProtocolSignerInfo.parseFrom(protobuff));
      } catch (InvalidProtocolBufferException e) {
        LOG.log(Level.SEVERE, "Couldn't parse the protobuff stored in MongoDB: " + protobuff, e);
      } catch (SignatureException e) {
        LOG.log(Level.SEVERE, "Couldn't parse the certificate chain or domain properly", e);
      }
    }
    return signerInfo;
  }

  @Override
  public void putSignerInfo(ProtocolSignerInfo protocolSignerInfo) throws SignatureException {
    SignerInfo signerInfo = new SignerInfo(protocolSignerInfo);
    byte[] signerId = signerInfo.getSignerId();

    // Not using a modifier here because rebuilding the object is not a lot of
    // work. Doing implicit upsert by using save with a DBOBject that has an _id
    // set.
    DBObject signerInfoDBObject = getDBObjectForSignerId(signerId);
    signerInfoDBObject.put("protoBuff", protocolSignerInfo.toByteArray());
    getSignerInfoCollection().save(signerInfoDBObject);
  }

  /**
   * Returns an instance of {@link DBCollection} for storing SignerInfo.
   */
  private DBCollection getSignerInfoCollection() {
    return database.getCollection("signerInfo");
  }

  /**
   * Returns a {@link DBObject} which contains the key-value pair used to
   * signify the signerId.
   *
   * @param signerId the signerId value to set.
   * @return a new {@link DBObject} with the (_id,signerId) entry.
   */
  private DBObject getDBObjectForSignerId(byte[] signerId) {
    DBObject query = new BasicDBObject();
    query.put("_id", signerId);
    return query;
  }

  // *********** Attachments.

  @Override
  public AttachmentData getAttachment(AttachmentId attachmentId) {

    final GridFSDBFile attachment = attachmentGrid.findOne(attachmentId.serialise());
    return fileToAttachmentData(attachment);
  }

  @Override
  public void storeAttachment(AttachmentId attachmentId, InputStream data)
      throws IOException {
    saveFile(attachmentGrid.createFile(data, attachmentId.serialise()));
  }

  @Override
  public void deleteAttachment(AttachmentId attachmentId) {
    attachmentGrid.remove(attachmentId.serialise());
    thumbnailGrid.remove(attachmentId.serialise());
    metadataGrid.remove(attachmentId.serialise());
  }


  @Override
  public AttachmentMetadata getMetadata(AttachmentId attachmentId) throws IOException {
    final GridFSDBFile metadata = metadataGrid.findOne(attachmentId.serialise());

    if (metadata == null) {
      return null;
    }
    AttachmentProto.AttachmentMetadata protoMetadata =
        AttachmentProto.AttachmentMetadata.parseFrom(metadata.getInputStream());
    return new AttachmentMetadataProtoImpl(protoMetadata);
  }

  @Override
  public AttachmentData getThumbnail(AttachmentId attachmentId) throws IOException {
    final GridFSDBFile thumbnail = thumbnailGrid.findOne(attachmentId.serialise());
    return fileToAttachmentData(thumbnail);
  }

  @Override
  public void storeMetadata(AttachmentId attachmentId, AttachmentMetadata metaData)
      throws IOException {
    AttachmentMetadataProtoImpl proto = new AttachmentMetadataProtoImpl(metaData);
    byte[] bytes = proto.getPB().toByteArray();
    GridFSInputFile file =
        metadataGrid.createFile(new ByteArrayInputStream(bytes), attachmentId.serialise());
    saveFile(file);
  }

  @Override
  public void storeThumbnail(AttachmentId attachmentId, InputStream dataData) throws IOException {
    saveFile(thumbnailGrid.createFile(dataData, attachmentId.serialise()));
  }

  private void saveFile(GridFSInputFile file) throws IOException {
    try {
      file.save();
    } catch (MongoException e) {
      // Unfortunately, file.save() wraps any IOException thrown in a
      // 'MongoException'. Since the interface explicitly throws IOExceptions,
      // we unwrap any IOExceptions thrown.
      Throwable innerException = e.getCause();
      if (innerException instanceof IOException) {
        throw (IOException) innerException;
      } else {
        throw e;
      }
    };
  }

  private AttachmentData fileToAttachmentData(final GridFSDBFile attachmant) {
    if (attachmant == null) {
      return null;
    } else {
      return new AttachmentData() {

        @Override
        public InputStream getInputStream() throws IOException {
          return attachmant.getInputStream();
        }

        @Override
        public long getSize() {
          return attachmant.getLength();
        }
      };
    }
  }

  // ******** AccountStore

  @Override
  public void initializeAccountStore() throws PersistenceException {
    // TODO: Sanity checks not handled by MongoDBProvider???
  }

  @Override
  public AccountData getAccount(ParticipantId id) {
    DBObject query = getDBObjectForParticipant(id);
    DBObject result = getAccountCollection().findOne(query);

    if (result == null) {
      return null;
    }

    DBObject human = (DBObject) result.get(ACCOUNT_HUMAN_DATA_FIELD);
    if (human != null) {
      return objectToHuman(id, human);
    }

    DBObject robot = (DBObject) result.get(ACCOUNT_ROBOT_DATA_FIELD);
    if (robot != null) {
      return objectToRobot(id, robot);
    }

    throw new IllegalStateException("DB object contains neither a human nor a robot");
  }

  @Override
  public void putAccount(AccountData account) {
    DBObject object = getDBObjectForParticipant(account.getId());

    if (account.isHuman()) {
      object.put(ACCOUNT_HUMAN_DATA_FIELD, humanToObject(account.asHuman()));
    } else if (account.isRobot()) {
      object.put(ACCOUNT_ROBOT_DATA_FIELD, robotToObject(account.asRobot()));
    } else {
      throw new IllegalStateException("Account is neither a human nor a robot");
    }

    getAccountCollection().save(object);
  }

  @Override
  public void removeAccount(ParticipantId id) {
    DBObject object = getDBObjectForParticipant(id);
    getAccountCollection().remove(object);
  }

  private DBObject getDBObjectForParticipant(ParticipantId id) {
    DBObject query = new BasicDBObject();
    query.put("_id", id.getAddress());
    return query;
  }

  private DBCollection getAccountCollection() {
    return database.getCollection(ACCOUNT_COLLECTION);
  }

  // ****** HumanAccountData serialization

  private DBObject humanToObject(HumanAccountData account) {
    DBObject object = new BasicDBObject();

    PasswordDigest digest = account.getPasswordDigest();
    if (digest != null) {
      DBObject digestObj = new BasicDBObject();
      digestObj.put(PASSWORD_SALT_FIELD, digest.getSalt());
      digestObj.put(PASSWORD_DIGEST_FIELD, digest.getDigest());

      object.put(HUMAN_PASSWORD_FIELD, digestObj);
    }

    return object;
  }

  private HumanAccountData objectToHuman(ParticipantId id, DBObject object) {
    PasswordDigest passwordDigest = null;

    DBObject digestObj = (DBObject) object.get(HUMAN_PASSWORD_FIELD);
    if (digestObj != null) {
      byte[] salt = (byte[]) digestObj.get(PASSWORD_SALT_FIELD);
      byte[] digest = (byte[]) digestObj.get(PASSWORD_DIGEST_FIELD);
      passwordDigest = PasswordDigest.from(salt, digest);
    }

    return new HumanAccountDataImpl(id, passwordDigest);
  }

  // ****** RobotAccountData serialization

  private DBObject robotToObject(RobotAccountData account) {
    return new BasicDBObject()
        .append(ROBOT_URL_FIELD, account.getUrl())
        .append(ROBOT_SECRET_FIELD, account.getConsumerSecret())
        .append(ROBOT_CAPABILITIES_FIELD, capabilitiesToObject(account.getCapabilities()))
        .append(ROBOT_VERIFIED_FIELD, account.isVerified());
  }

  private DBObject capabilitiesToObject(RobotCapabilities capabilities) {
    if (capabilities == null) {
      return null;
    }

    BasicDBObject capabilitiesObj = new BasicDBObject();
    for (Capability capability : capabilities.getCapabilitiesMap().values()) {
      BasicBSONList contexts = new BasicBSONList();
      for (Context c : capability.getContexts()) {
        contexts.add(c.name());
      }
      capabilitiesObj.put(capability.getEventType().name(),
          new BasicDBObject()
              .append(CAPABILITY_CONTEXTS_FIELD, contexts)
              .append(CAPABILITY_FILTER_FIELD, capability.getFilter()));
    }

    BasicDBObject object =
        new BasicDBObject()
            .append(CAPABILITIES_CAPABILITIES_FIELD, capabilitiesObj)
            .append(CAPABILITIES_HASH_FIELD, capabilities.getCapabilitiesHash())
            .append(CAPABILITIES_VERSION_FIELD, capabilities.getProtocolVersion().name());

    return object;
  }

  private AccountData objectToRobot(ParticipantId id, DBObject robot) {
    String url = (String) robot.get(ROBOT_URL_FIELD);
    String secret = (String) robot.get(ROBOT_SECRET_FIELD);
    RobotCapabilities capabilities =
        objectToCapabilities((DBObject) robot.get(ROBOT_CAPABILITIES_FIELD));
    boolean verified = (Boolean) robot.get(ROBOT_VERIFIED_FIELD);
    return new RobotAccountDataImpl(id, url, secret, capabilities, verified);
  }

  @SuppressWarnings("unchecked")
  private RobotCapabilities objectToCapabilities(DBObject object) {
    if (object == null) {
      return null;
    }

    Map<String, Object> capabilitiesObj =
	(Map<String, Object>) object.get(CAPABILITIES_CAPABILITIES_FIELD);
    Map<EventType, Capability> capabilities = CollectionUtils.newHashMap();

    for (Entry<String, Object> capability : capabilitiesObj.entrySet()) {
      EventType eventType = EventType.valueOf(capability.getKey());
      List<Context> contexts = CollectionUtils.newArrayList();
      DBObject capabilityObj = (DBObject) capability.getValue();
      DBObject contextsObj = (DBObject) capabilityObj.get(CAPABILITY_CONTEXTS_FIELD);
      for (String contextId : contextsObj.keySet()) {
        contexts.add(Context.valueOf((String) contextsObj.get(contextId)));
      }
      String filter = (String) capabilityObj.get(CAPABILITY_FILTER_FIELD);

      capabilities.put(eventType, new Capability(eventType, contexts, filter));
    }

    String capabilitiesHash = (String) object.get(CAPABILITIES_HASH_FIELD);
    ProtocolVersion version =
        ProtocolVersion.valueOf((String) object.get(CAPABILITIES_VERSION_FIELD));

    return new RobotCapabilities(capabilities, capabilitiesHash, version);
  }
}
