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

package org.waveprotocol.wave.util.escapers.jvm;

import org.waveprotocol.wave.model.id.URIEncoderDecoder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Uses standard java URLEncoder and URLDecoder to percent encode.
 *
 *
 */
public class JavaUrlCodec implements URIEncoderDecoder.PercentEncoderDecoder {
  @Override
  public String decode(String encodedValue) throws URIEncoderDecoder.EncodingException {
    try {
      // The URL decoder will replace + with space so percent escape it.
      encodedValue = encodedValue.replace("+", "%2B");

      String ret = URLDecoder.decode(encodedValue, "UTF-8");
      if (ret.indexOf(0xFFFD) != -1) {
        throw new URIEncoderDecoder.EncodingException(
            "Unable to decode value " + encodedValue + " it contains invalid UTF-8");
      }
      return ret;
    } catch (UnsupportedEncodingException e) {
      throw new URIEncoderDecoder.EncodingException(e);
    }
  }

  @Override
  public String encode(String decodedValue) throws URIEncoderDecoder.EncodingException {
    try {
      return URLEncoder.encode(decodedValue, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new URIEncoderDecoder.EncodingException(e);
    }
  }
}
