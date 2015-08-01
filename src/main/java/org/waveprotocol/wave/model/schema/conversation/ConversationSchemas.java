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

package org.waveprotocol.wave.model.schema.conversation;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletIdSerializer;
import org.waveprotocol.wave.model.schema.AbstractXmlSchemaConstraints;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.SchemaUtils;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Arrays;
import java.util.Collections;

/**
 * Hard coded conversation schema constraints.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class ConversationSchemas implements SchemaProvider {

  private final StringMap<DocumentSchema> schemas;

  @SuppressWarnings("deprecation")
  public ConversationSchemas() {
    schemas = CollectionUtils.createStringMap();
    schemas.put(IdConstants.MANIFEST_DOCUMENT_ID, MANIFEST_SCHEMA_CONSTRAINTS);
  }

  public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
    if (IdUtil.isConversationalId(waveletId)) {
      if (IdUtil.isBlipId(documentId)) {
        return BLIP_SCHEMA_CONSTRAINTS;
      } else if (schemas.containsKey(documentId)) {
        return schemas.get(documentId);
      }
    }
    return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
  }

  /**
   * Hard coded ("for now") conversation document schema constraints.
   */
  public static final DocumentSchema BLIP_SCHEMA_CONSTRAINTS = new DefaultDocumentSchema();

  public static class DefaultDocumentSchema extends AbstractXmlSchemaConstraints {
    {
      addChildren(null, "head");
      addChildren("head", "timestamp");
      addChildren("timestamp", "lmt");
      addAttrs("lmt", "t");

      addChildren(null, "body");

      lineContainer("body");

      addAttrWithValues("line", "t", "h1", "h2", "h3", "h4", "li");
      addAttrWithValues("line", "listyle", "decimal");
      addAttrWithValues("line", "a", "l", "r", "c", "j");
      addAttrWithValues("line", "d", "l", "r");
      // NOTE: for now, value constraints for indent implemented explicitly
      addAttrWithValues("line", "i");

      addChildren("image", "caption");
      addAttrWithValues("image", "attachment");
      addAttrWithValues("image", "style", "full");
      addChildren("image", "gadget");

      oneLiner("caption");
      addChildren("caption", "reply");
      oneLiner("label");
      oneLiner("input");

      addAttrs("reply", "id");

      containsFormElements("body");
      lineContainer("textarea");
      addAttrs("button", "name");
      addChildren("button", "caption", "events");
      addChildren("events", "click");
      addAttrs("click", "time", "clicker");
      addAttrs("check", "name", "submit", "value");
      addAttrs("radiogroup", "name", "submit", "value");
      addAttrs("password", "name", "submit", "value");
      addAttrs("textarea", "name", "submit", "value");
      addAttrs("input", "name", "submit");
      addAttrs("radio", "name", "group");
      addAttrs("click", "time", "clicker");
      addAttrs("label", "for");

      addChildren("gadget", "title", "thumbnail", "category", "state", "pref");
      // Some of these attributes might be obsolete and/or require stricter
      // validation
      addAttrs("gadget", "url", "title", "prefs", "state", "author", "height", "width", "id",
          "extension", "ifr", "snippet");
      for (String gadgetEl : new String[] {"category", "state", "pref"}) {
        addAttrs(gadgetEl, "name");
      }
      for (String gadgetEl : new String[] {"title", "thumbnail", "state", "pref"}) {
        addAttrs(gadgetEl, "value");
      }

      addChildren("profile", "profile-field", "gadget");
      addAttrs("profile-field", "name", "user-set");
      addAttrs("profile", "avatar-url");
      containsBlipText("profile-field");

      addChildren("mediasearch", "result", "customsearch");
      addAttrs("mediasearch", "page", "corpora", "query", "selected", "pending", "lang");
      addAttrs("result", "thumbnail", "thumbwidth", "thumbheight", "content", "url", "dispurl",
          "title", "snippet", "num", "type", "disphtml");
      addAttrs("customsearch", "name", "icon", "shortname", "resultrows", "resultcols",
          "addmethod");

      addChildren("body", "trustreq");
      containsBlipText("trustreq");
      addChildren("trustreq", "trwave");
      addAttrs("trustreq", "from", "numberOfWaves", "userAction");
      addAttrs("trwave", "messageCount", "lastModified");
      containsBlipText("trwave");

      addChildren("body", "blacklist");
      addAttrs("blacklist", "address", "contacts");

      addChildren("body", "invitation");
      addAttrs("invitation", "remaining", "title", "invitedString");
      addChildren("invitation", "invited");
      addAttrs("invited", "address");

      addAttrWithValues("eqn", "format", "tex");
      containsBlipText("eqn");

      addChildren("body", "settings");
      addAttrs("settings", "name");
      addChildren("settings", "bool-setting", "radio-setting", "text-setting", "listbox-setting");
      addAttrs("bool-setting", "id", "live-value", "saved-value");
      addAttrs("radio-setting", "id", "live-value", "saved-value");
      addAttrs("listbox-setting", "id", "live-value", "saved-value");
      addAttrs("text-setting", "id", "saved-value");
      oneLiner("text-setting");

      addChildren("body", "html");
      addChildren("html", "data");
      containsBlipText("data");

      addChildren("body", "experimental");
      addAttrs("experimental", "url");
      addChildren("experimental", "namevaluepair", "part");
      addAttrs("part", "id");
      lineContainer("part");
      containsFormElements("part");
      addAttrs("namevaluepair", "name");
      addAttrs("namevaluepair", "value");

      addChildren("body", "translation");
      addChildren("translation", "stanza");
      lineContainer("stanza");
      addAttrs("stanza", "lang", "users");

      addChildren("body", "extension_installer");
      // Can it contain form elements?
      // TODO(user): Remove img when I know it's safe.
      addAttrs("extension_installer", "manifest", "img", "installed");

      addChildren("body", "ext-settings");
      addAttrs("ext-settings", "manifest", "enabled");

      addChildren("body", "gadget-settings");
      addAttrs("gadget-settings", "url", "prefs");

      // NOTE: For now, schema constraints for height and width implemented
      // explicitly
      addAttrs("img", "alt", "height", "width", "src");

      addChildren("body", "quote");
      lineContainer("quote");
    }

    private void lineContainer(String element) {
      addChildren(element, "line", "image", "gadget", "eqn",
          "experimental", "mediasearch", "img", "reply", "profile");
      containsBlipText(element);
      addRequiredInitial(element, Collections.singletonList("line"));
    }

    private void oneLiner(String element) {
      containsBlipText(element);
      // Possibly allow other some elements, TBD
    }

    private void containsFormElements(String element) {
      addChildren(element, "button", "check", "input", "label", "password",
          "radiogroup", "radio", "textarea");
    }

    @Override
    public boolean permitsAttribute(String tag, String attr, String value) {
      // Some special cases
      if ("line".equals(tag) && "i".equals(attr)) {
        return SchemaUtils.isPositiveInteger(value);
      }

      if ("img".equals(tag) && ("width".equals(attr) || "height".equals(attr))) {
        return SchemaUtils.isValidInteger(value, 0);
      }

      if (Blips.LAST_MODIFICATION_TIME_TAGNAME.equals(tag)
          && "t".equals(attr)) {
        return SchemaUtils.isNonNegativeInteger(value);
      }

      if ("invitation".equals(tag) && "remaining".equals(attr)) {
        return SchemaUtils.isNonNegativeInteger(value);
      }

      return super.permitsAttribute(tag, attr, value);
    }
  }

  /**
   * Conversation manifest document schema constraints.
   */
  public static final DocumentSchema MANIFEST_SCHEMA_CONSTRAINTS =
      new AbstractXmlSchemaConstraints() {
        {
          addChildren(null, "conversation");

          // Value constraints for ids and offsets implemented explicitly.
          addAttrs("conversation", "anchorWavelet", "anchorManifestOffset", "anchorVersion",
              "anchorBlip", "anchorOffset", "sort");
          addChildren("conversation", "blip");

          // Value constraints for blip id implemented explicitly.
          addAttrs("blip", "id");
          addAttrWithValues("blip", "deleted", SchemaUtils.BOOLEAN_VALUES);
          addChildren("blip", "thread");
          addChildren("blip", "peer");

          addAttrs("thread", "id");
          addAttrWithValues("thread", "inline", SchemaUtils.BOOLEAN_VALUES);
          addChildren("thread", "blip");

          addAttrs("peer", "id");
        }

        @Override
        public boolean permitsAttribute(String tag, String attr, String value) {
          // Some special cases
          if ("conversation".equals(tag)) {
            if ("anchorWavelet".equals(attr)) {
              return IdUtil.isConversationalId(WaveletIdSerializer.INSTANCE.fromString(value));
            } else if (Arrays.asList("anchorManifestOffset", "anchorVersion", "anchorOffset")
                .contains(attr)) {
              return SchemaUtils.isNonNegativeInteger(value);
            } else if ("anchorBlip".equals(attr)) {
              return IdUtil.isBlipId(value);
            }
          }

          if ("blip".equals(tag) && "id".equals(attr)) {
            return IdUtil.isBlipId(value);
          }

          return super.permitsAttribute(tag, attr, value);
        }
      };
}
