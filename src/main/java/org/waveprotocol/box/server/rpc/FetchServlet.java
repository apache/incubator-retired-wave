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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.protobuf.MessageLite;

import org.waveprotocol.box.common.comms.WaveClientRpc.DocumentSnapshot;
import org.waveprotocol.box.common.comms.WaveClientRpc.WaveViewSnapshot;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.rpc.ProtoSerializer.SerializationException;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for static fetching of wave data. Typically, the servlet will be
 * hosted on /fetch/*. A document, a wavelet, or a whole wave can be specified
 * in the URL.
 *
 *  Valid request formats are: Fetch a wave: GET /fetch/wavedomain.com/waveid
 * Fetch a wavelet: GET /fetch/wavedomain.com/waveid/waveletdomain.com/waveletid
 * Fetch a document: GET
 * /fetch/wavedomain.com/waveid/waveletdomain.com/waveletid/b+abc123
 *
 *  The format of the returned information is the protobuf-JSON format used by
 * the websocket interface.
 */
@SuppressWarnings("serial")
@Singleton
public final class FetchServlet extends HttpServlet {
  private static final Log LOG = Log.get(FetchServlet.class);

  @Inject
  public FetchServlet(
      WaveletProvider waveletProvider, ProtoSerializer serializer, SessionManager sessionManager) {
    this.waveletProvider = waveletProvider;
    this.serializer = serializer;
    this.sessionManager = sessionManager;
  }

  private final ProtoSerializer serializer;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;

  /**
   * Create an http response to the fetch query. Main entrypoint for this class.
   */
  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
    ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));

    // This path will look like "/example.com/w+abc123/foo.com/conv+root
    // Strip off the leading '/'.
    String urlPath = req.getPathInfo().substring(1);

    // Extract the name of the wavelet from the URL
    WaveRef waveref;
    try {
      waveref = JavaWaverefEncoder.decodeWaveRefFromPath(urlPath);
    } catch (InvalidWaveRefException e) {
      // The URL contains an invalid waveref. There's no document at this path.
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    renderSnapshot(waveref, user, response);
  }

  private void serializeObjectToServlet(MessageLite message, HttpServletResponse dest)
      throws IOException {
    if (message == null) {
      // Snapshot is null. It would be nice to 404 here, but we can't let
      // clients guess valid wavelet ids that they're not authorized to access.
      dest.sendError(HttpServletResponse.SC_FORBIDDEN);
    } else {
      dest.setStatus(HttpServletResponse.SC_OK);
      dest.setContentType("application/json");

      // This is a hack to make sure the fetched data is fresh.
      // TODO(josephg): Change this so that browsers can cache wave snapshots. Probably need:
      // 'Cache-Control: must-revalidate, private' and an ETag with the wave[let]'s version.
      dest.setHeader("Cache-Control", "no-store");

      try {
        dest.getWriter().append(serializer.toJson(message).toString());
      } catch (SerializationException e) {
        throw new IOException(e);
      }
    }
  }

  /**
   * Render the requested waveref out to the HttpServletResponse dest.
   *
   * @param waveref The referenced wave. Could be a whole wave, a wavelet or
   *        just a document.
   * @param dest The servlet response to render the snapshot out to.
   * @throws IOException
   */
  private void renderSnapshot(WaveRef waveref, ParticipantId requester, HttpServletResponse dest)
      throws IOException {
    // TODO(josephg): Its currently impossible to fetch all wavelets inside a
    // wave that are visible to the user. Until this is fixed, if no wavelet is
    // specified we'll just return the conv+root.
    WaveletId waveletId = waveref.hasWaveletId() ? waveref.getWaveletId() : WaveletId.of(
        waveref.getWaveId().getDomain(), "conv+root");

    WaveletName waveletName = WaveletName.of(waveref.getWaveId(), waveletId);

    CommittedWaveletSnapshot committedSnapshot;
    try {
      if (!waveletProvider.checkAccessPermission(waveletName, requester)) {
        dest.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      LOG.info("Fetching snapshot of wavelet " + waveletName);
      committedSnapshot = waveletProvider.getSnapshot(waveletName);
    } catch (WaveServerException e) {
      throw new IOException(e);
    }
    if (committedSnapshot != null) {
      ReadableWaveletData snapshot = committedSnapshot.snapshot;
      if (waveref.hasDocumentId()) {
        // We have a wavelet id and document id. Find the document in the
        // snapshot and return it.
        DocumentSnapshot docSnapshot = null;
        for (String docId : snapshot.getDocumentIds()) {
          if (docId.equals(waveref.getDocumentId())) {
            docSnapshot = SnapshotSerializer.serializeDocument(snapshot.getDocument(docId));
            break;
          }
        }
        serializeObjectToServlet(docSnapshot, dest);
      } else if (waveref.hasWaveletId()) {
        // We have a wavelet id. Pull up the wavelet snapshot and return it.
        serializeObjectToServlet(SnapshotSerializer.serializeWavelet(snapshot,
            snapshot.getHashedVersion()), dest);
      } else {
        // Wrap the conv+root we fetched earlier in a WaveSnapshot object and
        // send it.
        WaveViewSnapshot waveSnapshot = WaveViewSnapshot.newBuilder()
            .setWaveId(ModernIdSerialiser.INSTANCE.serialiseWaveId(waveref.getWaveId()))
            .addWavelet(SnapshotSerializer.serializeWavelet(snapshot, snapshot.getHashedVersion()))
            .build();
        serializeObjectToServlet(waveSnapshot, dest);
      }
    } else {
      dest.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}
