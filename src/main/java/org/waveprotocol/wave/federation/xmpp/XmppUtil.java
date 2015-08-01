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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;

import org.dom4j.Element;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Common utility code for XMPP packet generation and parsing.
 */
public class XmppUtil {
  private static final AtomicLong idSequenceNo = new AtomicLong(0);
  private static final Random random = new SecureRandom();

  public static final IdURIEncoderDecoder waveletNameCodec =
      new IdURIEncoderDecoder(new JavaUrlCodec());

  // If non-null, this fake unique ID will be returned from generateUniqueId()
  // rather than a random base64 string.
  @VisibleForTesting
  public static String fakeUniqueId = null;

  // Alternately, and better, this callable will be called each time an ID is needed, if non-null.
  @VisibleForTesting
  static Callable<String> fakeIdGenerator = null;

  private XmppUtil() {
  }

  /**
   * Helper method to translate from the XMPP package (1.4 without generics) to
   * type-safe element lists.
   */
  @SuppressWarnings({"cast", "unchecked", "rawtypes"})
  public static List<Element> toSafeElementList(List elements) {
    return (List<Element>) elements;
  }

  /**
   * Checked exception thrown by signer conversion code.
   */
  public static class UnknownSignerType extends Exception {
    public UnknownSignerType(String algorithm) {
      super(algorithm);
    }

    public UnknownSignerType(String algorithm, Throwable stacked) {
      super(algorithm, stacked);
    }
  }

  /**
   * Convert the signer information to XML and place the result within the
   * passed Element. This method should never fail.
   */
  public static void protocolSignerInfoToXml(ProtocolSignerInfo signerInfo, Element parent) {
    Element signature = parent.addElement("signature", XmppNamespace.NAMESPACE_WAVE_SERVER);
    signature.addAttribute("domain", signerInfo.getDomain());
    ProtocolSignerInfo.HashAlgorithm hashValue = signerInfo.getHashAlgorithm();

    signature.addAttribute("algorithm", hashValue.name());
    for (ByteString cert : signerInfo.getCertificateList()) {
      signature.addElement("certificate").addCDATA(Base64Util.encode(cert));
    }
  }

  /**
   * Convert the given Element to a signer information XML element.
   *
   * @throws UnknownSignerType when the given hash algorithm is not understood
   */
  public static ProtocolSignerInfo xmlToProtocolSignerInfo(Element signature)
      throws UnknownSignerType {
    ProtocolSignerInfo.HashAlgorithm hash;
    String algorithm = signature.attributeValue("algorithm").toUpperCase();
    try {
      hash = ProtocolSignerInfo.HashAlgorithm.valueOf(algorithm);
    } catch (IllegalArgumentException e) {
      throw new UnknownSignerType(algorithm, e);
    }

    ProtocolSignerInfo.Builder builder = ProtocolSignerInfo.newBuilder();
    builder.setHashAlgorithm(hash);
    builder.setDomain(signature.attributeValue("domain"));
    for (Element certElement : toSafeElementList(signature.elements("certificate"))) {
      builder.addCertificate(Base64Util.decode(certElement.getText()));
    }
    return builder.build();
  }

  /**
   * Convenience method to create a response {@link Message} instance based on
   * the passed request. Simply returns a new message instance with the same ID,
   * but with inverse to/from addresses.
   *
   * @param request the request message
   * @return the new response message
   */
  public static Message createResponseMessage(Message request) {
    Message response = new Message();
    response.setID(request.getID());
    response.setTo(request.getFrom());
    response.setFrom(request.getTo());
    return response;
  }

  /**
   * Convenience method to create a response {@link Packet} implementation from
   * the given source packet. This will return either an {@link IQ} or
   * {@link Message} depending on the passed type.
   *
   * @param request the request message
   * @return the new response message
   */
  public static Packet createResponsePacket(Packet request) {
    if (request instanceof Message) {
      return createResponseMessage((Message) request);
    } else if (request instanceof IQ) {
      return IQ.createResultIQ((IQ) request);
    } else {
      throw new IllegalArgumentException("Can't respond to unsupported packet type: "
          + request.getClass());
    }
  }

  /**
   * Generate a unique string identifier for use in stanzas.
   *
   * @return unique string identifier
   */
  public static String generateUniqueId() {
    if (fakeIdGenerator != null) {
      try {
        return fakeIdGenerator.call();
      } catch (Exception e) {
        // This is used in tests only.
        throw new RuntimeException(e);
      }
    }
    // TODO(arb): deprecate this.
    if (fakeUniqueId != null) {
      return fakeUniqueId;
    }

    // Generate a base64 ID based on raw bytes.
    byte[] bytes = ByteBuffer.allocate(16)
        .putLong(random.nextLong()).putLong(idSequenceNo.incrementAndGet()).array();
    return Base64Util.encode(bytes);
  }
}
