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

package org.waveprotocol.wave.client.editor.testing;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Utility that decorates editors by adding the ability to get/set content using types
 *   other than DocInitialisation ops.
 *
 * TODO(patcoleman): figure out which class(/es) should be natively supported inside Editor
 * the interface, and which should be decorated within this class.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class ContentSerialisationUtil {
  /** Static utility, hence private constructor. */
  private ContentSerialisationUtil() {}

  /// String support

  /** Gets the editor's persistent document as a String. */
  public static String getContentString(Editor editor) {
    return XmlStringBuilder.innerXml(editor.getPersistentDocument()).toString();
  }

  /**
   * Sets the editor's content to a given string - the accepted format currently is XML,
   *   note to be careful that attribute values are surrounded by ", not '.
   */
  public static void setContentString(Editor editor, String content) {
    editor.setContent(DocProviders.POJO.parse(content).asOperation(),
        ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
  }

  /// Other classes to come...
}
