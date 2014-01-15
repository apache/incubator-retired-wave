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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.wave.federation.Proto.ProtocolDocumentOperation;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A utility class to serialize/deserialize delta objects to/from MongoDB.
 * The implementation approach is analog to the provided at {@link CoreWaveletOperationSerializer}
 * and {@link ProtoDeltaStoreDataSerializer}
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDbDeltaStoreUtil {

  
  public static final String WAVELET_OP_WAVELET_BLIP_OPERATION = "WaveletBlipOperation";
  public static final String WAVELET_OP_REMOVE_PARTICIPANT = "RemoveParticipant";
  public static final String WAVELET_OP_ADD_PARTICIPANT = "AddParticipant";
  public static final String WAVELET_OP_NOOP = "NoOp";
  public static final String FIELD_BYTES = "bytes";
  public static final String FIELD_CONTENTOP = "contentop";
  public static final String FIELD_BLIPOP = "blipop";
  public static final String FIELD_BLIPID = "blipid";
  public static final String FIELD_PARTICIPANT = "participant";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_OPS = "ops";
  public static final String FIELD_APPLICATIONTIMESTAMP = "applicationtimestamp";
  public static final String FIELD_AUTHOR = "author";
  public static final String FIELD_ADDRESS = "address";
  public static final String FIELD_HISTORYHASH = "historyhash";
  public static final String FIELD_VERSION = "version";
  public static final String FIELD_TRANSFORMED_RESULTINGVERSION_VERSION = "transformed.resultingversion.version";
  public static final String FIELD_TRANSFORMED_APPLIEDATVERSION = "transformed.appliedatversion";
  public static final String FIELD_TRANSFORMED_RESULTINGVERSION = "transformed.resultingversion";
  public static final String FIELD_APPLIEDATVERSION = "appliedatversion";
  public static final String FIELD_RESULTINGVERSION = "resultingversion";
  public static final String FIELD_TRANSFORMED = "transformed";
  public static final String FIELD_APPLIED = "applied";
  public static final String FIELD_WAVELET_ID = "waveletid";
  public static final String FIELD_WAVE_ID = "waveid";  
  


  public static DBObject serialize(WaveletDeltaRecord waveletDelta, String waveId, String waveletId) {
    
    BasicDBObject _waveletDelta = new BasicDBObject();
 
    // 
    _waveletDelta.append(FIELD_WAVE_ID, waveId);
    _waveletDelta.append(FIELD_WAVELET_ID, waveletId);
    
    _waveletDelta.append(FIELD_APPLIEDATVERSION, serialize(waveletDelta.getAppliedAtVersion()));
    _waveletDelta.append(FIELD_APPLIED, waveletDelta.getAppliedDelta().getByteArray());
    _waveletDelta.append(FIELD_TRANSFORMED, serialize(waveletDelta.getTransformedDelta()));
    
    return _waveletDelta;
  }
  
  
  public static DBObject serialize(HashedVersion hashedVersion) {
    
    BasicDBObject _hashedVersion = new BasicDBObject();
    _hashedVersion.append(FIELD_VERSION, hashedVersion.getVersion());
    _hashedVersion.append(FIELD_HISTORYHASH, hashedVersion.getHistoryHash());
    
    return _hashedVersion;
    
  }
  
  
  public static DBObject serialize(ParticipantId participantId) {
    
    BasicDBObject _participantId = new BasicDBObject();
    _participantId.append(FIELD_ADDRESS, participantId.getAddress());
    
    return _participantId;
    
  }
  
  public static DBObject serialize(TransformedWaveletDelta transformedWaveletDelta) {
    
    BasicDBObject _transformedWaveletDelta = new BasicDBObject();
    _transformedWaveletDelta.append(FIELD_AUTHOR, serialize(transformedWaveletDelta.getAuthor()));
    _transformedWaveletDelta.append(FIELD_RESULTINGVERSION, serialize(transformedWaveletDelta.getResultingVersion()));
    _transformedWaveletDelta.append(FIELD_APPLICATIONTIMESTAMP, transformedWaveletDelta.getApplicationTimestamp());
    
    // Calculated value to provide DB implementation of MongoDBDeltaCollection.getDelta(long version)
    _transformedWaveletDelta.append(FIELD_APPLIEDATVERSION, transformedWaveletDelta.getAppliedAtVersion());
       
    BasicDBList _waveletOperations = new BasicDBList();
    
    for (WaveletOperation op: transformedWaveletDelta) {
      _waveletOperations.add(serialize(op));
    }
    
    _transformedWaveletDelta.append(FIELD_OPS, _waveletOperations);
     
    return _transformedWaveletDelta;

  }
  
  
  public static DBObject serialize(WaveletOperation waveletOp) {
    
    final BasicDBObject _op = new BasicDBObject();
      
      
    if (waveletOp instanceof NoOp) {
      _op.append(FIELD_TYPE, WAVELET_OP_NOOP);
   
    } else if (waveletOp instanceof AddParticipant) {
      _op.append(FIELD_TYPE, WAVELET_OP_ADD_PARTICIPANT);
      _op.append(FIELD_PARTICIPANT, serialize(((AddParticipant) waveletOp).getParticipantId()));
      
    } else if (waveletOp instanceof RemoveParticipant) {
      
      _op.append(FIELD_TYPE, WAVELET_OP_REMOVE_PARTICIPANT);
      _op.append(FIELD_PARTICIPANT, serialize(((RemoveParticipant) waveletOp).getParticipantId()));
      
    } else if (waveletOp instanceof WaveletBlipOperation) {
      
      final WaveletBlipOperation waveletBlipOp = (WaveletBlipOperation) waveletOp;
   
      _op.append(FIELD_TYPE, WAVELET_OP_WAVELET_BLIP_OPERATION);
      _op.append(FIELD_BLIPID, waveletBlipOp.getBlipId());

      if (waveletBlipOp.getBlipOp() instanceof BlipContentOperation) {
        
        _op.append(FIELD_BLIPOP, serialize((BlipContentOperation) waveletBlipOp.getBlipOp()));
        
      } else {
               
        throw new IllegalArgumentException("Unsupported blip operation: " + waveletBlipOp.getBlipOp());
      } 
      
    } else {
      
      throw new IllegalArgumentException("Unsupported wavelet operation: " + waveletOp);
    }
    
    return _op;
  }
  
  
  
  public static DBObject serialize(BlipContentOperation blipContentOp) {
    
    BasicDBObject _blipContentOp = new BasicDBObject();
    _blipContentOp.append(FIELD_CONTENTOP, serialize(blipContentOp.getContentOp()));
    return _blipContentOp;
  }
  
  
  public static DBObject serialize(DocOp docOp) {
    
    // This method relays on the provided CoreWaveletOperationSerializer, 
    // because of complexity of serializing DocOp's
    BasicDBObject _docOp = new BasicDBObject();
    _docOp.append(FIELD_BYTES, CoreWaveletOperationSerializer.serialize(docOp).toByteArray());
    return _docOp;
  }
  
  
  
  public static WaveletDeltaRecord deserializeWaveletDeltaRecord(DBObject dbObject) throws PersistenceException {
    
   try {
   
     return new WaveletDeltaRecord(
          deserializeHashedVersion((DBObject) dbObject.get(FIELD_APPLIEDATVERSION)), 
          ByteStringMessage.parseProtocolAppliedWaveletDelta(ByteString.copyFrom((byte[]) dbObject.get(FIELD_APPLIED))),
          deserializeTransformedWaveletDelta((DBObject) dbObject.get(FIELD_TRANSFORMED)));
  
   } catch (InvalidProtocolBufferException e) {
     
     throw new PersistenceException(e);
  }   

  }
  
  public static HashedVersion deserializeHashedVersion(DBObject dbObject) {
    
    return HashedVersion.of((Long) dbObject.get(FIELD_VERSION), (byte[]) dbObject.get(FIELD_HISTORYHASH));
  }
  
  
  public static ParticipantId deserializeParicipantId(DBObject dbObject) {
    
    return ParticipantId.ofUnsafe((String) dbObject.get(FIELD_ADDRESS));
  }
  
  
  public static TransformedWaveletDelta deserializeTransformedWaveletDelta(DBObject dbObject) throws PersistenceException {
    
    ParticipantId author = deserializeParicipantId((DBObject) dbObject.get(FIELD_AUTHOR));
    HashedVersion resultingVersion = deserializeHashedVersion((DBObject) dbObject.get(FIELD_RESULTINGVERSION)); 
    long applicationTimestamp = (Long) dbObject.get(FIELD_APPLICATIONTIMESTAMP);
    
    BasicDBList dbOps = (BasicDBList) dbObject.get(FIELD_OPS);
    ImmutableList.Builder<WaveletOperation> operations = ImmutableList.builder();
    
    int numOperations = dbOps.size();
    
    // Code analog to ProtoDeltaStoreDataSerializer.deserialize
    for (int i = 0; i < numOperations; i++) {
      
      WaveletOperationContext context;
      if (i == numOperations - 1) {
        context = new WaveletOperationContext(author, applicationTimestamp, 1, resultingVersion);
      } else {
        context = new WaveletOperationContext(author, applicationTimestamp, 1);
      }
      operations.add(deserializeWaveletOperation((DBObject) dbOps.get(i), context));
    }

    
    return new TransformedWaveletDelta(author, resultingVersion, applicationTimestamp, operations.build());
    
  }
  
  public static WaveletOperation deserializeWaveletOperation(DBObject dbObject, WaveletOperationContext context) throws PersistenceException {
    
    
    String type = (String) dbObject.get(FIELD_TYPE);
    
    if (type.equals(WAVELET_OP_NOOP)) {
      return new NoOp(context);
    } else if (type.equals(WAVELET_OP_ADD_PARTICIPANT)) {
      return new AddParticipant(context, deserializeParicipantId((DBObject) dbObject.get(FIELD_PARTICIPANT)));
    } else if (type.equals(WAVELET_OP_REMOVE_PARTICIPANT)) {
      return new RemoveParticipant(context, deserializeParicipantId((DBObject) dbObject.get(FIELD_PARTICIPANT)));
    } else if (type.equals(WAVELET_OP_WAVELET_BLIP_OPERATION)) {
      
      return new WaveletBlipOperation((String) dbObject.get(FIELD_BLIPID), 
                                       deserializeBlipContentOperation((DBObject) dbObject.get(FIELD_BLIPOP), context));
          
         
    } else {
      throw new IllegalArgumentException("Unsupported operation: " + type);
    }
    
  }


  public static BlipOperation deserializeBlipContentOperation(DBObject dbObject,
      WaveletOperationContext context) throws PersistenceException {

    return new BlipContentOperation(context, deserializeDocOp((DBObject) dbObject.get(FIELD_CONTENTOP)));
  }


  private static DocOp deserializeDocOp(DBObject dbObject) throws PersistenceException {
   
    
    try {
      
      return CoreWaveletOperationSerializer.deserialize(ProtocolDocumentOperation.parseFrom(((byte[]) dbObject.get(FIELD_BYTES))));
    
    } catch (InvalidProtocolBufferException e) {
       throw new PersistenceException(e);
    }
   
  }

  
}
