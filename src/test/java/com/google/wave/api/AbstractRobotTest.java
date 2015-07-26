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

package com.google.wave.api;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcConstant.RequestProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.WaveService.HttpFetcher;
import com.google.wave.api.WaveService.HttpResponse;
import com.google.wave.api.event.BlipContributorsChangedEvent;
import com.google.wave.api.event.BlipSubmittedEvent;
import com.google.wave.api.event.DocumentChangedEvent;
import com.google.wave.api.event.EventType;
import com.google.wave.api.event.WaveletTagsChangedEvent;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.GsonFactory;
import com.google.wave.api.impl.WaveletData;

import junit.framework.TestCase;

import net.oauth.http.HttpMessage;

import org.mockito.Matchers;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Test cases for {@link AbstractRobot}.
 */
public class AbstractRobotTest extends TestCase {

  private static final WaveId WAVE_1 = WaveId.of("example.com", "wave1");
  private static final WaveletId WAVELET_1 = WaveletId.of("example.com", "wavelet1");
  private static final String PROFILE_PATH = "/basepath/_wave/robot/profile";
  private static final String CAPABILITIES_XML_PATH = "/basepath/_wave/capabilities.xml";
  private static final String JSONRPC_PATH = "/basepath/_wave/robot/jsonrpc";
  private static final String VERIFY_TOKEN_PATH = "/basepath/_wave/verify_token";

  private class MockRobot extends AbstractRobot {

    public MockRobot() {
      super();
    }

    // This method provided to enable mock on HttpFetcher
    public  List<JsonRpcResponse> submit(Wavelet wavelet, String rpcServerUrl, WaveService service)
    throws IOException {
      return service.submit(wavelet, rpcServerUrl);
    }

    @Override
    protected String getRobotName() {
      return "Foo";
    }

    @Override
    protected String getRobotProfilePageUrl() {
      return "http://foo.com";
    }

    @Override
    protected String getRobotAvatarUrl() {
      return "http://foo.com/foo.png";
    }

    @Capability(contexts = {Context.PARENT, Context.SELF, Context.CHILDREN}, filter=".*")
    @Override
    public void onBlipSubmitted(BlipSubmittedEvent e) {
      calledEvents.add(e.getType());
    }

    @Override
    public void onDocumentChanged(DocumentChangedEvent e) {
      calledEvents.add(e.getType());
    }
  }

  private static class MockWriter extends PrintWriter {

    private String string;

    public MockWriter() {
      super(new StringWriter());
    }

    @Override
    public void write(String string) {
      this.string = string;
    }

    public String getString() {
      return string;
    }
  }

  private static <K, V> Map<K, V> anyMapOf(Class<K> keyClass, Class<V> valueClass) {
    return Matchers.<Map<K, V>>any();
  }

  private final List<EventType> calledEvents = new ArrayList<EventType>();

  public void testSubmit() throws Exception {
    HttpFetcher fetcher = mock(HttpFetcher.class);
    when(fetcher.execute(any(HttpMessage.class), anyMapOf(String.class, Object.class)))
        .thenReturn(new HttpResponse("POST", new URL("http://foo.google.com"), 0,
            new ByteArrayInputStream("[{\"id\":\"op1\",\"data\":{}}]".getBytes())));


    MockRobot robot = new MockRobot();
    robot.setupOAuth("consumerKey", "consumerSecret", "http://gmodules.com/api/rpc");

    WaveService service = new WaveService(fetcher, robot.computeHash());
    service.setupOAuth("consumerKey", "consumerSecret", "http://gmodules.com/api/rpc");

    OperationQueue opQueue = new OperationQueue();
    opQueue.appendOperation(OperationType.ROBOT_NOTIFY,
        Parameter.of(ParamsProperty.CAPABILITIES_HASH, "123"));
    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getOperationQueue()).thenReturn(opQueue);

    assertEquals(1, opQueue.getPendingOperations().size());
    robot.submit(wavelet, "http://gmodules.com/api/rpc", service);
    assertEquals(0, opQueue.getPendingOperations().size());
    verify(fetcher, times(1)).execute(any(HttpMessage.class), anyMapOf(String.class, Object.class));
  }

  public void testServiceCapabilitiesRequest() throws Exception {
    AbstractRobot robot = new MockRobot() {
      @Override
      public void onBlipContributorsChanged(BlipContributorsChangedEvent e) {
        calledEvents.add(e.getType());
      }
    };
    MockWriter writer = new MockWriter();
    robot.doGet(makeMockRequest(CAPABILITIES_XML_PATH), makeMockResponse(writer));
    String capabilitiesXml = writer.getString();

    String expectedCapabilityTag =
        "<w:capability name=\"BLIP_SUBMITTED\" context=\"PARENT,SELF,CHILDREN\" filter=\".*\"/>\n";
    assertTrue(capabilitiesXml.contains(expectedCapabilityTag));

    expectedCapabilityTag =
        "<w:capability name=\"DOCUMENT_CHANGED\"/>\n";
    assertTrue(capabilitiesXml.contains(expectedCapabilityTag));

    expectedCapabilityTag =
        "<w:capability name=\"BLIP_CONTRIBUTORS_CHANGED\"/>\n";
    assertTrue(capabilitiesXml.contains(expectedCapabilityTag));

    expectedCapabilityTag =
        "<w:capability name=\"WAVELET_SELF_ADDED\"/>\n";
    assertFalse(capabilitiesXml.contains(expectedCapabilityTag));

    expectedCapabilityTag =
        "<w:capability name=\"WAVELET_SELF_ADDED\" context=\"ROOT,PARENT,CHILDREN\"/>\n";
    assertFalse(capabilitiesXml.contains(expectedCapabilityTag));
  }

  public void testServiceProfileRequest() throws Exception {
    AbstractRobot robot = new MockRobot();
    MockWriter writer = new MockWriter();
    robot.doGet(makeMockRequest(PROFILE_PATH), makeMockResponse(writer));
    String profileJson = writer.getString();

    String expectedProfileJson =
        "{\"address\":\"\",\"name\":\"Foo\",\"imageUrl\":\"http://foo.com/foo.png\"," +
        "\"profileUrl\":\"http://foo.com\"}";
    assertEquals(expectedProfileJson, profileJson);
  }

  public void testServiceVerificationTokenRequest() throws Exception {
    AbstractRobot robot = new MockRobot();
    robot.setupVerificationToken("vertoken", "sectoken");

    MockWriter writer = new MockWriter();
    robot.doGet(makeMockRequest(VERIFY_TOKEN_PATH, "st", "sectoken"),
        makeMockResponse(writer));
    assertEquals("vertoken", writer.getString());

    HttpServletResponse response = makeMockResponse(new MockWriter());
    robot.doGet(makeMockRequest(VERIFY_TOKEN_PATH), response);
    verify(response).setStatus(HttpURLConnection.HTTP_UNAUTHORIZED);
  }

  public void refactor_testServiceEventMessageBundleRequest() throws Exception {
    final List<EventType> calledEvents = new ArrayList<EventType>();
    AbstractRobot robot = new AbstractRobot() {
      @Override
      protected String getRobotName() {
        return "Foo";
      }

      @Override
      public String getRobotProfilePageUrl() {
        return "http://code.google.com/apis/wave/";
      }

      @Override
      public void onBlipSubmitted(BlipSubmittedEvent e) {
        calledEvents.add(e.getType());
      }

      @Override
      public void onDocumentChanged(DocumentChangedEvent e) {
        calledEvents.add(e.getType());
      }

      @Override
      public void onWaveletTagsChanged(WaveletTagsChangedEvent e) {
        calledEvents.add(e.getType());
      }

      @Override
      protected String computeHash() {
        return "hash1";
      }
    };

    WaveletData waveletData = new WaveletData("google.com!wave1", "google.com!conv+root", "blip1",
        null);
    waveletData.addParticipant("foo@google.com");
    BlipSubmittedEvent event1 = new BlipSubmittedEvent(null, null, "foo@test.com", 1l, "blip1");
    DocumentChangedEvent event2 = new DocumentChangedEvent(null, null, "foo@test.com", 1l, "blip1");
    WaveletTagsChangedEvent event3 = new WaveletTagsChangedEvent(null, null, "foo@test.com", 1l,
        "blip1");

    EventMessageBundle bundle = new EventMessageBundle("Foo", "http://gmodules.com/api/rpc");
    bundle.addEvent(event1);
    bundle.addEvent(event2);
    bundle.addEvent(event3);
    bundle.setWaveletData(waveletData);
    String json = new GsonFactory().create().toJson(bundle);

    MockWriter mockWriter = new MockWriter();
    robot.doPost(
        makeMockRequest(JSONRPC_PATH, new BufferedReader(new StringReader(json))),
        makeMockResponse(mockWriter));

    assertEquals(3, calledEvents.size());
    assertEquals(EventType.BLIP_SUBMITTED, calledEvents.get(0));
    assertEquals(EventType.DOCUMENT_CHANGED, calledEvents.get(1));
    assertEquals(EventType.WAVELET_TAGS_CHANGED, calledEvents.get(2));

    // Assert that the outgoing operation bundle contains robot.notify() op.
    JsonParser jsonParser = new JsonParser();
    JsonArray ops = jsonParser.parse(mockWriter.getString()).getAsJsonArray();
    assertEquals(1, ops.size());

    JsonObject op = ops.get(0).getAsJsonObject();
    assertEquals(OperationType.ROBOT_NOTIFY.method(),
        op.get(RequestProperty.METHOD.key()).getAsString());

    JsonObject params = op.get(RequestProperty.PARAMS.key()).getAsJsonObject();
    assertEquals("0.22", params.get(ParamsProperty.PROTOCOL_VERSION.key()).getAsString());
    assertEquals("hash1", params.get(ParamsProperty.CAPABILITIES_HASH.key()).getAsString());
  }

  public void testBlindWavelet() throws Exception {
    AbstractRobot robot = new MockRobot();
    Wavelet blindWavelet = robot.blindWavelet(WAVE_1, WAVELET_1);
    assertEquals(0, blindWavelet.getOperationQueue().getPendingOperations().size());
    blindWavelet.getParticipants().add("foo@test.com");
    blindWavelet.reply("\n");
    assertEquals(2, blindWavelet.getOperationQueue().getPendingOperations().size());
    assertEquals(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX.method(),
        blindWavelet.getOperationQueue().getPendingOperations().get(0).getMethod());
    assertEquals(OperationType.WAVELET_APPEND_BLIP.method(),
        blindWavelet.getOperationQueue().getPendingOperations().get(1).getMethod());
  }

  public void testProxiedBlindWavelet() throws Exception {
    AbstractRobot robot = new MockRobot();
    Wavelet blindWavelet = robot.blindWavelet(WAVE_1, WAVELET_1, "proxyid");
    assertEquals(0, blindWavelet.getOperationQueue().getPendingOperations().size());
    blindWavelet.reply("\n");

    List<OperationRequest> ops = blindWavelet.getOperationQueue().getPendingOperations();
    assertEquals(1, ops.size());
    assertEquals(OperationType.WAVELET_APPEND_BLIP.method(), ops.get(0).getMethod());
    assertEquals("proxyid", ops.get(0).getParameter(ParamsProperty.PROXYING_FOR));

    // Assert that proxy id should be valid.
    try {
      robot.blindWavelet(WAVE_1, WAVELET_1, "foo@bar.com");
      fail("Should have failed since proxy id is not valid.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
  
  public void testInitRobot() throws Exception {
    AbstractRobot robot = new MockRobot();
    AbstractRobot spyRobot = spy(robot);
    spyRobot.initRobot();
    verify(spyRobot).computeCapabilityMap();
    verify(spyRobot).computeHash();
  }

  private HttpServletRequest makeMockRequest(String path, BufferedReader reader)
      throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getReader()).thenReturn(reader);
    return request;
  }

  private HttpServletRequest makeMockRequest(String path, String parameterKey,
      String parameterValue) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getParameter(parameterKey)).thenReturn(parameterValue);
    return request;
  }

  private HttpServletRequest makeMockRequest(String path) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(path);
    return request;
  }

  private HttpServletResponse makeMockResponse(MockWriter writer) throws IOException {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(writer);
    return response;
  }
}
