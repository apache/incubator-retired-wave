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

package org.waveprotocol.wave.federation.xmpp;

/**
 * Namespace definitions for the XMPP package.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
final class XmppNamespace {

  // Namespace definitions for packet types
  static final String NAMESPACE_XMPP_RECEIPTS = "urn:xmpp:receipts";
  static final String NAMESPACE_DISCO_INFO = "http://jabber.org/protocol/disco#info";
  static final String NAMESPACE_DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
  static final String NAMESPACE_PUBSUB = "http://jabber.org/protocol/pubsub";
  static final String NAMESPACE_PUBSUB_EVENT = "http://jabber.org/protocol/pubsub#event";
  static final String NAMESPACE_WAVE_SERVER = "http://waveprotocol.org/protocol/0.2/waveserver";

  /**
   * Uninstantiable class.
   */
  private XmppNamespace() {
  }

}
