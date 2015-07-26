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

package org.waveprotocol.wave.client.doodad.link;

import junit.framework.TestCase;

import org.waveprotocol.wave.client.doodad.link.Link.InvalidLinkException;
import org.waveprotocol.wave.util.escapers.GwtWaverefEncoder;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class LinkTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    GwtWaverefEncoder.INSTANCE = JavaWaverefEncoder.INSTANCE;
  }

  public void testNormalizeRejectsJunk() {
    String[] invalid = new String[] {
        "asdf", "foo://asdf", "wave://asdf", "waveid://asdf",
        "wave://example.com!abcd"
    };
    for (String s : invalid) {
      checkNormalizeRejects(s);
    }
  }

  public void testNormalizeIsGenerousWithWaveIdOrNoPrefix() throws InvalidLinkException {
    checkNormalize("wave://example.com/abcd", "example.com!abcd");
    checkNormalize("wave://example.com/abcd", "example.com/abcd");
    // waveid: can support wave serialized wave ids and wave refs
    checkNormalize("wave://example.com/abcd", "waveid://example.com/abcd");
    checkNormalize("wave://example.com/abcd", "waveid://example.com!abcd");
  }

  public void testNormalizeWaveRefs() throws InvalidLinkException {
    checkNormalize("wave://example.com/abcd");
    checkNormalize("wave://example.com/abcd/~/bar");
    checkNormalizeRejects("wave://example.com!abcd");
  }

  public void testNormalizeAcceptsValidWebSchemes() throws InvalidLinkException {
    checkNormalize("http://example.com/abcd");
    checkNormalize("https://example.com/abcd");
    checkNormalize("ftp://example.com/abcd");
    checkNormalize("mailto://example.com/abcd");
    checkNormalize("#");
    checkNormalize("#example.com/w+N0tgD7rctjA");
    checkNormalize("#fragment");
    checkNormalize("#fragment/?+-:@!$&'()*+,;=");
    checkNormalize("?param=1&param2");
    checkNormalize("?param=1&param2#");
    checkNormalize("?param=1&param2#fragment/?+-:@!$&'()*+,;=");
    checkNormalizeRejects("#fragment<>");
    checkNormalizeRejects("?param=1&param2<>");
  }

  public void testConvertUri() {
    assertNull(Link.toHrefFromUri("asdf"));
    assertNull(Link.toHrefFromUri("wave://asdf"));
    assertNull(Link.toHrefFromUri("foo://asdf"));

    assertEquals("http://example.com/abcd", Link.toHrefFromUri("http://example.com/abcd"));
    assertEquals("#example.com/abcd", Link.toHrefFromUri("wave://example.com/abcd"));
    assertEquals("#", Link.toHrefFromUri("#"));
    assertEquals("#fragment", Link.toHrefFromUri("#fragment"));
    assertEquals("#fragment/?+-:@!$&'()*+,;=", Link.toHrefFromUri("#fragment/?+-:@!$&'()*+,;="));
    assertEquals("?param=1&param2", Link.toHrefFromUri("?param=1&param2"));
    assertEquals("?param=1&param2#", Link.toHrefFromUri("?param=1&param2#"));
    assertEquals("?param=1&param2#fragment/?+-:@!$&'()*+,;=", Link.toHrefFromUri("?param=1&param2#fragment/?+-:@!$&'()*+,;="));
  }


  void checkNormalize(String inputOutput) throws InvalidLinkException {
    checkNormalize(inputOutput, inputOutput);
  }

  void checkNormalize(String expected, String input) throws InvalidLinkException {
    assertEquals(expected, Link.normalizeLink(input));
    // Check the invariant that passing in the output should always return the same thing.
    assertEquals(expected, Link.normalizeLink(expected));
  }

  void checkNormalizeRejects(String input) {
    try {
      Link.normalizeLink(input);
      fail("'" + input + "' should not be considered a valid link");
    } catch (InvalidLinkException e) {
      // ok
    }
  }
}
