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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.http.client.URL;

import org.waveprotocol.wave.model.id.URIEncoderDecoder;

/**
 * Uses GWT {@link URL} to percent encode.
 */
public final class ClientPercentEncoderDecoder implements URIEncoderDecoder.PercentEncoderDecoder {
  @Override
  public String decode(String encodedValue) throws URIEncoderDecoder.EncodingException {
    String ret = URL.decodePathSegment(encodedValue);
    if (ret.indexOf(0xFFFD) != -1) {
      throw new URIEncoderDecoder.EncodingException("Unable to decode value " + encodedValue
          + " it contains invalid UTF-8");
    }
    return ret;
  }

  @Override
  public String encode(String decodedValue) {
    return URL.encode(decodedValue);
  }
}
