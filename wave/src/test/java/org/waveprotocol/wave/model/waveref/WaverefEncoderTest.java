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

package org.waveprotocol.wave.model.waveref;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.waveref.WaverefEncoder.PercentEncoderDecoder;

/**
 * Unit tests for {@link WaverefEncoder}
 *
 * @author meade@google.com <Edwina Mead>
 */
public class WaverefEncoderTest extends TestCase {

  private static final String decodeDummyString = "decodedDummyString";
  private static final String pathEncodeDummyString = "pathEncodedDummyString";
  private static final String queryEncodeDummyString = "queryEncodedDummyString";

  /**
   * Inject dummy functions in place of the injectable PercentEncoderDecoder for
   * simplicity/minimising dependencies.
   */
  private static final WaverefEncoder dummyEncoder =
      new WaverefEncoder(new PercentEncoderDecoder() {
        @Override
        public String decode(String encodedValue) {
          return decodeDummyString;
        }

        @Override
        public String pathEncode(String decodedValue) {
          return pathEncodeDummyString;
        }

        @Override
        public String queryEncode(String decodedValue) {
          return queryEncodeDummyString;
        }
      });

  /**
   * An encoder that does no percent escaping.
   */
  private static final WaverefEncoder waverefEncoder = new WaverefEncoder(new PercentEncoderDecoder() {

    @Override
    public String decode(String encodedValue) {
      return encodedValue;
    }

    @Override
    public String pathEncode(String decodedValue) {
      return decodedValue;
    }

    @Override
    public String queryEncode(String decodedValue) {
      return decodedValue;
    }
  });

  /**
   * Tests that the injected pathEncode function gets called when
   * encodeToUriPathSegment is called with a string input.
   */
  public void testPathEncodeGetsCalledWithStringInput() {
    assertEquals(pathEncodeDummyString, dummyEncoder.encodeToUriPathSegment("testString"));
  }

  /**
   * Tests that the injected queryEncode function gets called when
   * encodeToUriQueryString is called with a string input.
   */
  public void testQueryEncodeGetsCalledWithStringInput() {
    assertEquals(queryEncodeDummyString, dummyEncoder.encodeToUriQueryString("testString"));
  }

  /**
   * Tests that the injected pathEncode function gets called when
   * encodeToUriPathSegment is called with a waveRef input, and checks that the
   * resulting path has the correct format of
   * waveDomain/waveId[/~/waveletId/[documentId]]
   */
  public void testPathEncodeGetsCalledWithWaveRefInput() {
    WaveId waveId = WaveId.of("www.example.com", "abcdEFGH");
    WaveletId waveletId = WaveletId.of("www.example.com", "conv+root");
    String documentId = "b+1234";

    WaveRef waveIdOnly = WaveRef.of(waveId);
    WaveRef waveIdPlusWaveletId = WaveRef.of(waveId, waveletId);
    WaveRef fullySpecified = WaveRef.of(waveId, waveletId, documentId);

    String expectedResultWaveIdOnly = pathEncodeDummyString + "/" + pathEncodeDummyString;
    assertEquals(expectedResultWaveIdOnly,
        dummyEncoder.encodeToUriPathSegment(waveIdOnly));

    String expectedResultWithWaveletId =
      expectedResultWaveIdOnly + "/~/" + pathEncodeDummyString;
    assertEquals(expectedResultWithWaveletId,
        dummyEncoder.encodeToUriPathSegment(waveIdPlusWaveletId));

    String expectedResultFullySpecified =
      expectedResultWithWaveletId + "/" + pathEncodeDummyString;
    assertEquals(expectedResultFullySpecified,
        dummyEncoder.encodeToUriPathSegment(fullySpecified));
  }

  /**
   * Tests that the injected queryEncode function gets called when
   * encodeToUriPathSegment is called with a waveRef input, and checks that the
   * resulting path has the correct format of
   * waveDomain/waveId[/~/waveletId/[documentId]]
   */
  public void testQueryEncodeGetsCalledWithWaveRefInput() {
    WaveId waveId = WaveId.of("www.example.com", "abcdEFGH");
    WaveletId waveletId = WaveletId.of("www.example.com", "conv+root");
    String documentId = "b+1234";

    WaveRef waveIdOnly = WaveRef.of(waveId);
    WaveRef waveIdPlusWaveletId = WaveRef.of(waveId, waveletId);
    WaveRef fullySpecified = WaveRef.of(waveId, waveletId, documentId);

    String expectedResultWaveIdOnly = queryEncodeDummyString + "/" + queryEncodeDummyString;
    assertEquals(expectedResultWaveIdOnly,
        dummyEncoder.encodeToUriQueryString(waveIdOnly));

    String expectedResultWithWaveletId = expectedResultWaveIdOnly + "/~/" + queryEncodeDummyString;
    assertEquals(expectedResultWithWaveletId,
        dummyEncoder.encodeToUriQueryString(waveIdPlusWaveletId));

    String expectedResultFullySpecified =
      expectedResultWithWaveletId + "/" + queryEncodeDummyString;
    assertEquals(expectedResultFullySpecified,
        dummyEncoder.encodeToUriQueryString(fullySpecified));
  }

  public void testDecodeCalledWithStringInput() throws InvalidWaveRefException {
    assertEquals(decodeDummyString, dummyEncoder.decode("testString"));
  }

  public void testDomainlessWaveRefIsInvalid() {
    assertInvalidWaveRef("/foo/~/conv+root/xyz");
  }

  public void testIdlessWaveRefIsInvalid() {
    assertInvalidWaveRef("foocorp.com//~/conv+root/xyz");
  }

  public void testDomainlessWaveletWaveRefIsInvalid() {
    assertInvalidWaveRef("foocorp.com/w+abc123//conv+root/xyz");
  }

  public void testIdlessWaveletWaveRefIsInvalid() {
    assertInvalidWaveRef("foocorp.com/w+abc123/~//xyz");
  }

  public void testDeserialisingValidWaveRefs() throws InvalidWaveRefException {
    WaveRef ref = waverefEncoder.decodeWaveRefFromPath("example.com/w+abc123/~/conv+root/b+983247");
    WaveRef expected = WaveRef.of(WaveId.of("example.com", "w+abc123"),
        WaveletId.of("example.com", "conv+root"),
    "b+983247");
    assertEquals(expected, ref);
  }

  /**
   * Helper for testing that decoding an invalid waveref results in an exception.
   */
  private void assertInvalidWaveRef(String refStr) {
    try {
      waverefEncoder.decodeWaveRefFromPath(refStr);
    } catch (InvalidWaveRefException ex) {
      // This is expected
    }
  }
}
