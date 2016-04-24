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

package org.waveprotocol.box.server.rpc;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Message;

import junit.framework.TestCase;

import org.waveprotocol.box.common.comms.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.authentication.SessionManagerImpl;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.util.TestDataUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for the FetchServlet. The fetch servlet provides wavelet snapshots
 * from a waveletProvider.
 *
 * These tests make sure reasonable errors are generated for invalid URLs and
 * that the fetch results match what was sent.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FetchServletTest extends TestCase {
  private static final ProtoSerializer protoSerializer = new ProtoSerializer();
  private WaveletProviderStub waveletProvider;
  private FetchServlet servlet;

  @Override
  protected void setUp() throws Exception {
    waveletProvider = new WaveletProviderStub();
    AccountStore accountStore = new MemoryStore();
    accountStore.putAccount(new HumanAccountDataImpl(ParticipantId.ofUnsafe("fred@example.com")));
    org.eclipse.jetty.server.SessionManager jettySessionManager =
        mock(org.eclipse.jetty.server.SessionManager.class);
    SessionManager sessionManager = new SessionManagerImpl(accountStore, jettySessionManager);
    servlet = new FetchServlet(waveletProvider, protoSerializer, sessionManager);
  }

  public void testGetInvalidWaverefReturnsNotFound() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    when(request.getPathInfo()).thenReturn("/invalidwaveref");
    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  public void testDisallowedUserReturnsForbidden() throws Exception {
    waveletProvider.setAllowsAccess(false);

    WaveletData wavelet = waveletProvider.getHostedWavelet();
    WaveRef waveref = WaveRef.of(wavelet.getWaveId(), wavelet.getWaveletId());
    verifyServletReturnsForbiddenForWaveref(waveref);
  }

  public void testGetMissingDataReturnsForbidden() throws Exception {
    WaveletData wavelet = waveletProvider.getHostedWavelet();
    WaveId waveId = wavelet.getWaveId();
    WaveletId waveletId = wavelet.getWaveletId();

    WaveRef unknownWave = WaveRef.of(WaveId.of(waveId.getDomain(), waveId.getId() + "junk"));
    verifyServletReturnsForbiddenForWaveref(unknownWave);
    WaveRef unknownWavelet = WaveRef.of(waveId, WaveletId.of(waveletId.getDomain(), waveletId.getId() + "junk"));
    verifyServletReturnsForbiddenForWaveref(unknownWavelet);
    WaveRef unknownDocument = WaveRef.of(waveId, waveletId, "madeupdocid");
    verifyServletReturnsForbiddenForWaveref(unknownDocument);
  }

  /**
   * Round-trip a wavelet and make sure all the fields match.
   * We only check the fields that WaveletSnapshot serializes.
   * @throws Exception
   */
  public void testGetWavelet() throws Exception {
    WaveletData wavelet = waveletProvider.getHostedWavelet();

    WaveRef waveref = WaveRef.of(wavelet.getWaveId(), wavelet.getWaveletId());
    WaveletSnapshot snapshot = fetchWaverRefAndParse(waveref, WaveletSnapshot.class);
    WaveletData roundtripped = SnapshotSerializer.deserializeWavelet(snapshot, waveref.getWaveId());

    // We have just round-tripped wavelet through the servlet. wavelet and
    // roundtripped should be identical in all the fields that get serialized.
    TestDataUtil.checkSerializedWavelet(wavelet, roundtripped);

    // TODO(josephg): Enable this test when the persistence store is in place.
//    assertEquals(snapshot.getVersion(), waveletProvider.getCommittedVersion());
  }

  /**
   * The fetch servlet also exposes document snapshots through a longer url
   * (/fetch/domain/waveid/domain/waveletid/docid).
   *
   * @throws Exception
   */
  public void testGetDocument() throws Exception {
    WaveletData wavelet = waveletProvider.getHostedWavelet();
    for (String docId : wavelet.getDocumentIds()) {
      // We currently have no way to deserialize a document. Instead, we'll
      // serialize the expected document and compare with what we get from the
      // fetch servlet.
      StringWriter writer = new StringWriter();
      BlipData expectedDoc = wavelet.getDocument(docId);
      writer.append("" + protoSerializer.toJson(SnapshotSerializer.serializeDocument(expectedDoc)));
      String expectedResult = writer.toString();

      WaveRef waveref = WaveRef.of(wavelet.getWaveId(), wavelet.getWaveletId(), docId);
      String actualResult = fetchWaveRef(waveref);

      assertEquals(expectedResult, actualResult);
    }
  }

  // ** Helper methods

  /**
   * Fetch the given waveref from the servlet.
   */
  private void requestWaveRef(WaveRef waveref, HttpServletResponse response) throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getPathInfo()).thenReturn("/" + JavaWaverefEncoder.encodeToUriPathSegment(waveref));
    servlet.doGet(request, response);
  }

  private void verifyServletReturnsForbiddenForWaveref(WaveRef waveref) throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    requestWaveRef(waveref, response);
    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  private String fetchWaveRef(WaveRef waveref) throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter writer = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(writer));

    requestWaveRef(waveref, response);

    verify(response).getWriter();
    verify(response, never()).sendError(anyInt());

    return writer.toString();
  }

  private <T extends Message> T fetchWaverRefAndParse(WaveRef waveref, Class<T> klass) throws Exception {
    String message = fetchWaveRef(waveref);
    JsonElement json = new JsonParser().parse(message);
    return protoSerializer.fromJson(json, klass);
  }
}
