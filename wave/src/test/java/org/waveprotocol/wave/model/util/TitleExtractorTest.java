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

package org.waveprotocol.wave.model.util;

import junit.framework.TestCase;

/**
 */

@SuppressWarnings("deprecation") // the class under test is deprecated
public class TitleExtractorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testExtractSimpleTitle() throws Exception {
    String blip =  "<blip><p _t=\"title\">Time for another trip...</p><p></p><p>I'm sure you all " +
      "remember the rafting trip from last time: http://picasaweb.google.com/gdalesandre </p>" +
      "<p></p><p></p></blip>";


    String title = TitleExtractor.extractTitle(blip);
    assertEquals("Time for another trip...", title);
    assertFalse(TitleExtractor.isOnlyTitle(blip));
  }

  public void testOnlyTitle() throws Exception {
    String blip =  "<blip><p _t=\"title\">Time for another trip?!?</p><p></p></p><p></p></blip>";

    String title = TitleExtractor.extractTitle(blip);
    assertEquals("Time for another trip?!?", title);
    assertTrue(TitleExtractor.isOnlyTitle(blip));
  }

  public void testExtractEvilTitle() throws Exception {
    String blip =  "<blip><p _t=\"title\">Time for another trip!!!</p><p></p><p>I'm sure you all " +
      "remember the rafting trip from last time: http://picasaweb.google.com/gdalesandre </p>" +
      "<p></p>" +
      "<p><gadget prefs=\"\" state=\"%7B%22maybe%22%3A%22jochen%40google.com%2Cthorogood" +
      "%40google.com%2Cwhitelaw%40google.com%22%2C%22no%22%3A%22duff%40google.com%2Cgregd" +
      "%40google.com%22%2C%22yes%22%3A%22douwe%40google.com%2Cahaberlach%40google.com%22%2C" +
      "%220%22%3A%22%7B%22%2C%221%22%3A%22%5C%22%22%2C%222%22%3A%22y%22%2C%223%22%3A%22e%22" +
      "%2C%224%22%3A%22s%22%2C%225%22%3A%22%5C%22%22%2C%226%22%3A%22%3A%22%2C%227%22%3A%22" +
      "%5C%22%22%2C%228%22%3A%22u%22%2C%229%22%3A%22s%22%2C%2210%22%3A%22e%22%2C%2211%22%3A" +
      "%22r%22%2C%2212%22%3A%22i%22%2C%2213%22%3A%22d%22%2C%2214%22%3A%22%5C%22%22%2C%2215%" +
      "22%3A%22%2C%22%2C%2216%22%3A%22%5C%22%22%2C%2217%22%3A%22n%22%2C%2218%22%3A%22o%22%2C" +
      "%2219%22%3A%22%5C%22%22%2C%2220%22%3A%22%3A%22%2C%2221%22%3A%22n%22%2C%2222%22%3A%22u" +
      "%22%2C%2223%22%3A%22l%22%2C%2224%22%3A%22l%22%2C%2225%22%3A%22%2C%22%2C%2226%22%3A%22%5C" +
      "%22%22%2C%2227%22%3A%22m%22%2C%2228%22%3A%22a%22%2C%2229%22%3A%22y%22%2C%2230%22%3A%22b" +
      "%22%2C%2231%22%3A%22e%22%2C%2232%22%3A%22%5C%22%22%2C%2233%22%3A%22%3A%22%2C%2234%22%3A" +
      "%22n%22%2C%2235%22%3A%22u%22%2C%2236%22%3A%22l%22%2C%2237%22%3A%22l%22%2C%2238%22%3A%22" +
      "%7D%22%2C%22typeId%24%22%3A%222%22%7D\" title=\"\" " +
      "url=\"http://hosting.gmodules.com/ig/gadgets/file/103849234114306421973/whoscoming.xml\">" +
      "</gadget></p></blip>";


    String title = TitleExtractor.extractTitle(blip);
    assertEquals("Time for another trip!!!", title);
    assertFalse(TitleExtractor.isOnlyTitle(blip));
  }
}
