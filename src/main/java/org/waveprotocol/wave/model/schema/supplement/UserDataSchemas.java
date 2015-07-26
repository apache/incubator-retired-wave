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

package org.waveprotocol.wave.model.schema.supplement;

import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.AbstractXmlSchemaConstraints;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.SchemaUtils;
import org.waveprotocol.wave.model.supplement.DocumentBasedAbuseStore;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Schemas for the supplement model.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class UserDataSchemas implements SchemaProvider {

  private final StringMap<DocumentSchema> schemas = CollectionUtils.createStringMap();

  public UserDataSchemas() {
    schemas.put(WaveletBasedSupplement.READSTATE_DOCUMENT, UDW_READ);
    schemas.put(WaveletBasedSupplement.ARCHIVING_DOCUMENT, UDW_ARCHIVE);
    schemas.put(WaveletBasedSupplement.FOLDERS_DOCUMENT, UDW_FOLDER);
    schemas.put(WaveletBasedSupplement.MUTED_DOCUMENT, UDW_MUTE);
    schemas.put(WaveletBasedSupplement.PRESENTATION_DOCUMENT, UDW_PRESENTATION);
    schemas.put(WaveletBasedSupplement.CLEARED_DOCUMENT, UDW_CLEARED);
    schemas.put(WaveletBasedSupplement.SEEN_DOCUMENT, UDW_SEEN);
    schemas.put(WaveletBasedSupplement.GADGETS_DOCUMENT, UDW_GADGET);
    schemas.put(WaveletBasedSupplement.ABUSE_DOCUMENT, UDW_ABUSE);
  }

  @Override
  public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
    if (IdUtil.isUserDataWavelet(waveletId) && schemas.containsKey(documentId)) {
      return schemas.get(documentId);
    } else {
      return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    }
  }

  /**
   * User Data Wavelet folder tagging document schema constraints.
   */
  public static final DocumentSchema UDW_FOLDER = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.FOLDER_TAG);
      addAttrs(WaveletBasedSupplement.FOLDER_TAG, WaveletBasedSupplement.ID_ATTR);
    }

    @Override
    public boolean permitsAttribute(String tag, String attr, String value) {
      // Values of ID attribute are integers.
      if (WaveletBasedSupplement.ID_ATTR.equals(attr)) {
        return SchemaUtils.isValidInteger(value, Integer.MIN_VALUE);
      } else {
        return super.permitsAttribute(tag, attr, value);
      }
    }
  };

  /**
   * User Data Wavelet archive status document schema constraints.
   */
  public static final DocumentSchema UDW_ARCHIVE = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.ARCHIVE_TAG);
      addAttrs(WaveletBasedSupplement.ARCHIVE_TAG, WaveletBasedSupplement.ID_ATTR,
          WaveletBasedSupplement.VERSION_ATTR);
    }

    @Override
    public boolean permitsAttribute(String tag, String attr, String value) {
      // Values of version attributes are integers; can include 0.
      if (WaveletBasedSupplement.VERSION_ATTR.equals(attr)) {
        return SchemaUtils.isNonNegativeInteger(value);
      } else {
        return super.permitsAttribute(tag, attr, value);
      }
    }
  };

  /**
   * User Data Wavelet muted status document schema constraints.
   */
  public static final DocumentSchema UDW_MUTE = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.MUTED_TAG);
      addAttrWithValues(WaveletBasedSupplement.MUTED_TAG, WaveletBasedSupplement.MUTED_ATTR,
          SchemaUtils.BOOLEAN_VALUES);
    }
  };

  /**
   * User Data Wavelet cleared status document schema constraints.
   */
  public static final DocumentSchema UDW_CLEARED = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.CLEARED_TAG);
      addAttrWithValues(WaveletBasedSupplement.CLEARED_TAG, WaveletBasedSupplement.CLEARED_ATTR,
          SchemaUtils.BOOLEAN_VALUES);
    }
  };

  /**
   * User Data presentation (thread state) document schema constraints.
   */
  public static final DocumentSchema UDW_PRESENTATION = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.CONVERSATION_TAG);

      addChildren(WaveletBasedSupplement.CONVERSATION_TAG, WaveletBasedSupplement.THREAD_TAG);

      addAttrs(WaveletBasedSupplement.CONVERSATION_TAG, WaveletBasedSupplement.ID_ATTR);
      addAttrs(WaveletBasedSupplement.THREAD_TAG, WaveletBasedSupplement.ID_ATTR);
      addAttrs(WaveletBasedSupplement.THREAD_TAG, WaveletBasedSupplement.STATE_ATTR);
    }
  };

  /**
   * User Data Wavelet read/unread versions document schema constraints.
   */
  public static final DocumentSchema UDW_READ = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.WAVELET_TAG);

      addChildren(WaveletBasedSupplement.WAVELET_TAG, WaveletBasedSupplement.BLIP_READ_TAG);
      addChildren(WaveletBasedSupplement.WAVELET_TAG, WaveletBasedSupplement.PARTICIPANTS_READ_TAG);
      addChildren(WaveletBasedSupplement.WAVELET_TAG, WaveletBasedSupplement.WAVELET_READ_TAG);
      addChildren(WaveletBasedSupplement.WAVELET_TAG, WaveletBasedSupplement.TAGS_READ_TAG);

      addAttrs(WaveletBasedSupplement.WAVELET_TAG, WaveletBasedSupplement.ID_ATTR);
      addAttrs(WaveletBasedSupplement.BLIP_READ_TAG, WaveletBasedSupplement.ID_ATTR);

      addAttrs(WaveletBasedSupplement.BLIP_READ_TAG, WaveletBasedSupplement.VERSION_ATTR);
      addAttrs(WaveletBasedSupplement.PARTICIPANTS_READ_TAG, WaveletBasedSupplement.VERSION_ATTR);
      addAttrs(WaveletBasedSupplement.WAVELET_READ_TAG, WaveletBasedSupplement.VERSION_ATTR);
      addAttrs(WaveletBasedSupplement.TAGS_READ_TAG, WaveletBasedSupplement.VERSION_ATTR);
    }

    @Override
    public boolean permitsAttribute(String tag, String attr, String value) {
      // Values of version attributes are integers; can include 0.
      if (WaveletBasedSupplement.VERSION_ATTR.equals(attr)) {
        return SchemaUtils.isNonNegativeInteger(value);
      } else {
        return super.permitsAttribute(tag, attr, value);
      }
    }
  };

  /**
   * User Data Wavelet seen state document schema constraints.
   */
  public static final DocumentSchema UDW_SEEN = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.SEEN_VERSION_TAG);
      addAttrs(WaveletBasedSupplement.SEEN_VERSION_TAG, WaveletBasedSupplement.ID_ATTR,
          WaveletBasedSupplement.SIGNATURE_ATTR);
      addChildren(null, WaveletBasedSupplement.NOTIFIED_VERSION_TAG);
      addAttrs(WaveletBasedSupplement.NOTIFIED_VERSION_TAG, WaveletBasedSupplement.ID_ATTR,
          WaveletBasedSupplement.VERSION_ATTR);
      // Note(user): Deprecated
      addChildren(null, WaveletBasedSupplement.NOTIFICATION_TAG);
      addAttrs(WaveletBasedSupplement.NOTIFICATION_TAG,
          WaveletBasedSupplement.PENDING_NOTIFICATION_ATTR);
    }
  };


  /**
   * Gadget private (per user) state in User Data Wavelets.
   */
  public static final DocumentSchema UDW_GADGET = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, WaveletBasedSupplement.GADGET_TAG);
      addAttrs(WaveletBasedSupplement.GADGET_TAG, WaveletBasedSupplement.ID_ATTR,
          WaveletBasedSupplement.PERMISSIONS_ATTR);

      addChildren(WaveletBasedSupplement.GADGET_TAG, WaveletBasedSupplement.STATE_TAG);
      addAttrs(WaveletBasedSupplement.STATE_TAG, WaveletBasedSupplement.NAME_ATTR,
          WaveletBasedSupplement.VALUE_ATTR);
    }
  };

  /**
   * User Data Wavelet constraints on the abuse document.
   */
  public static final DocumentSchema UDW_ABUSE = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, DocumentBasedAbuseStore.WANTED_EVAL_TAG);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.WAVELET_ID_ATTR);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.CERTAINTY_ATTR);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.TIMESTAMP_ATTR);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.AGENT_ATTR);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.COMMENT_ATTR);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.IGNORED_ATTR);
      addAttrs(DocumentBasedAbuseStore.WANTED_EVAL_TAG, DocumentBasedAbuseStore.ADDER_ATTR);
      addAttrWithValues(DocumentBasedAbuseStore.WANTED_EVAL_TAG,
          DocumentBasedAbuseStore.WANTED_ATTR, SchemaUtils.BOOLEAN_VALUES);
    }

    @Override
    public boolean permitsAttribute(String tag, String attr, String value) {
      // Schema should already be enforcing that tag is WANTED_EVAL_TAG, so it
      // is assumed here.
      if (DocumentBasedAbuseStore.CERTAINTY_ATTR.equals(attr)) {
        return SchemaUtils.isDouble(value);
      } else if (DocumentBasedAbuseStore.TIMESTAMP_ATTR.equals(attr)) {
        return SchemaUtils.isLong(value);
      } else if (DocumentBasedAbuseStore.WAVELET_ID_ATTR.equals(attr)) {
        return SchemaUtils.isWaveletId(value);
      } else {
        return super.permitsAttribute(tag, attr, value);
      }
    }
  };

}
