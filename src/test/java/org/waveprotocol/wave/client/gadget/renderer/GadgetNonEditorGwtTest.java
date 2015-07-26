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

package org.waveprotocol.wave.client.gadget.renderer;

import static org.waveprotocol.wave.model.gadget.GadgetConstants.AUTHOR_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.CATEGORY_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.KEY_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.PREFS_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.STATE_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.TITLE_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.URL_ATTRIBUTE;

import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window.Location;

import org.waveprotocol.wave.client.gadget.GadgetXmlUtil;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Tests Gadget class functionality that can be tested outside of the Editor framework.
 *
 *
 * TODO: Reorganize tests after the client refactoring is complete.
 */

public class GadgetNonEditorGwtTest extends GWTTestCase {

  private static final String VIEW_NAME = "canvas";

  private static class FakeLocale implements Locale {
    @Override
    public String getCountry() {
      return "OZ";
    }

    @Override
    public String getLanguage() {
      return "wizard";
    }
  }

  private static final String GADGET_SERVER = "-opensocial.googleusercontent.com";
  private static final String LOGIN_NAME = "johnny_addgadget@rentacoder.com";

  /**
   * Tests the construct XML function which is used to insert gadgets into wave
   * XML representation.
   */
  public void testConstructXml() {
    String xmlSource = "http://test.com/gadget.xml";
    XmlStringBuilder builder = GadgetXmlUtil.constructXml(xmlSource, "", LOGIN_NAME);
    String xml = builder.toString();
    String expectedValue = "<" + TAGNAME +
        " " + URL_ATTRIBUTE + "=\"" + xmlSource + "\"" +
        " " + TITLE_ATTRIBUTE + "=\"\"" +
        " " + PREFS_ATTRIBUTE + "=\"\"" +
        " " + STATE_ATTRIBUTE + "=\"\"" +
        " " + AUTHOR_ATTRIBUTE + "=\"johnny_addgadget@rentacoder.com\">" +
        "</" + TAGNAME + ">";
    assertEquals(expectedValue, xml);
    String[] categories = {"chess", "game"};
    builder = GadgetXmlUtil.constructXml(xmlSource, "my pref", categories, LOGIN_NAME);
    xml = builder.toString();
    expectedValue = "<" + TAGNAME +
        " " + URL_ATTRIBUTE + "=\"" + xmlSource + "\"" +
        " " + TITLE_ATTRIBUTE + "=\"\"" +
        " " + PREFS_ATTRIBUTE + "=\"my pref\"" +
        " " + STATE_ATTRIBUTE + "=\"\"" +
        " " + AUTHOR_ATTRIBUTE + "=\"johnny_addgadget@rentacoder.com\">" +
        "<" + CATEGORY_TAGNAME +
        " " + KEY_ATTRIBUTE + "=\"chess\"" + ">" +
        "</" + CATEGORY_TAGNAME + ">" +
        "<" + CATEGORY_TAGNAME +
        " " + KEY_ATTRIBUTE + "=\"game\"" + ">" +
        "</" + CATEGORY_TAGNAME + ">" +
        "</" + TAGNAME + ">";
    assertEquals(expectedValue, xml);
  }

  /**
   * Returns a test gadget metadata object.
   *
   * @param xmlSource gadget xml
   * @return the metadata object
   */
  public GadgetMetadata getTestMetadata(String xmlSource) {
    String iFrameUrl =
        "//0" + GADGET_SERVER + "/gadgets/ifr?url=http://test.com/gadget.xml&view=canvas";
    JSONObject canvasViewData = new JSONObject();
    JSONObject viewData = new JSONObject();
    viewData.put(VIEW_NAME, canvasViewData);
    JSONObject jsonData = new JSONObject();
    jsonData.put("iframeUrl", new JSONString(iFrameUrl));
    jsonData.put("views", viewData);
    jsonData.put("url", new JSONString(xmlSource));
    jsonData.put("userPrefs", new JSONObject());
    return new GadgetMetadata(jsonData);
  }

  /**
   * Tests the method that removes redundant parts from iframe URLs returned in
   * gadget metadata by GGS.
   */
  public void testCleanUrl() {
    String url = "//0-a-wave-opensocial.googleusercontent.com/gadgets/ifr?v=f5&container=wave" +
        "&view=default&debug=0&lang=en&url=http%3A%2F%2Fwave-api.appspot.com%2Fpublic%2Fgadgets" +
        "%2Ftest%2Fgadget.xml&country=ALL&st=SECURITY&libs=core%3Adynamic-height%3Awave" +
        "&mid=31415926#rpctoken=4815162342&st=WRONGPLACE&up_test=done&test=see&up_again=not";
    String cleanUrl = "//0-a-wave-opensocial.googleusercontent.com/gadgets/ifr?v=f5" +
    "&container=wave&view=default&url=http%3A%2F%2Fwave-api.appspot.com%2Fpublic" +
    "%2Fgadgets%2Ftest%2Fgadget.xml&libs=core%3Adynamic-height%3Awave#test=see";
    assertEquals(cleanUrl, GadgetWidget.cleanUrl(url));
  }

  /**
   * Tests the helper method that extracts and verifies the iframe URL host.
   */
  public void testGetIframeHost() {
    assertEquals("", GadgetWidget.getIframeHost("http://www.google.com/hey!"));
    assertEquals("//www.google.com/", GadgetWidget.getIframeHost("//www.google.com/hey!"));
    assertEquals("//googleusercontent.com/", GadgetWidget.getIframeHost("//googleusercontent.com/hey!"));
    assertEquals("//!!.googleusercontent.com/", GadgetWidget.getIframeHost("//!!.googleusercontent.com/hey!"));
    assertEquals("//123-yesgoogleusercontent.com/", GadgetWidget.getIframeHost("//123-yesgoogleusercontent.com/hey!"));
    assertEquals("//123-yes.googleusercontent.com/",
        GadgetWidget.getIframeHost("//123-yes.googleusercontent.com/hey!"));
  }

  /**
   * Tests the IFrame URI generator of Gadget class.
   */
  public void testIframeUri() {
    String xmlSource = "http://test.com/gadget.xml";
    String href = "http://" + Location.getHost();
    String hrefEscaped = href.replace("?", "%3F");
    if (hrefEscaped.endsWith("/")) {
      hrefEscaped = hrefEscaped.substring(0, hrefEscaped.length() - 1);
    }
    int clientInstanceId = 1234;
    GadgetUserPrefs userPrefs = GadgetUserPrefs.create();
    userPrefs.put("pref1", "value1");
    userPrefs.put("pref2", "value2");
    GadgetMetadata metadata = getTestMetadata(xmlSource);
    WaveId waveId = WaveId.of("wave.google.com", "123");
    WaveletId waveletId = WaveletId.of("wave.google.com", "conv+root");
    WaveletName name = WaveletName.of(waveId, waveletId);
    String securityToken = "SECURITY";
    GadgetWidget gadget = GadgetWidget.createForTesting(
        clientInstanceId, userPrefs, name, securityToken, new FakeLocale());
    int gadgetInstanceId = -12345;
    String url = gadget.buildIframeUrl(gadgetInstanceId, metadata.getIframeUrl(VIEW_NAME));
    String expectedValue =
        "//0" + GADGET_SERVER + "/gadgets"
            + "/ifr?url=http://test.com/gadget.xml&view=canvas&nocache=1&mid=" + gadgetInstanceId
            + "&lang=wizard&country=OZ&parent=" + hrefEscaped + "&wave=1&waveId="
            + URL.encodeQueryString(ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId))
            + "#rpctoken=" + gadget.getRpcToken() + "&st=" + securityToken
            + "&up_pref1=value1&up_pref2=value2";
    assertEquals(expectedValue, url);
  }

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.gadget.tests";
  }

}
