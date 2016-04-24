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
package org.waveprotocol.box.expimp;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.waveprotocol.wave.communication.gson.GsonException;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.box.common.comms.proto.WaveletSnapshotProtoImpl;

import com.google.wave.api.WaveService;
import com.google.wave.api.impl.GsonFactory;
import com.google.wave.api.impl.RawAttachmentData;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.wave.api.impl.RawDeltasListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

/**
 * Export waves from Wiab to files.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class WaveExport {
  private static final IdURIEncoderDecoder URI_CODEC = new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);

  private static final Gson gson = new GsonFactory().create();
  private static final JsonParser jsonParser = new JsonParser();

  private final String serverUrl;
  private final WaveService api;
  private final FileNames fileNames;
  private String search = "";
  private String consumerKey;
  private String consumerSecret;
  private List<WaveId> includeList;
  private List<WaveId> excludeList;
  private String rpcServerUrl;

  public WaveExport(String serverUrl, String exportDir) {
    this.serverUrl = serverUrl;
    fileNames = new FileNames(exportDir);
    api = new WaveService();
    api.setFetchFimeout(WaveService.FETCH_INFINITE_TIMEOUT);
  }

  public static void usageError() {
    Console.println("Use: WaveExport <server URL> <export directory>\n"
        + "   [-consumer_key     Robot consumer key]\n"
        + "   [-consumer_secret  Robot consumer secret]\n"
        + "   [-search           Search query]\n"
        + "   [-include          Include waves list]\n"
        + "   [-include_file     Include waves list file]\n"
        + "   [-exclude          Exclude waves list]");
    System.exit(1);
  }

  public static void main(String[] args) throws IOException, Base64DecoderException {
    if (args.length < 2) {
      usageError();
    }
    WaveExport export = new WaveExport(args[0], args[1]);
    List<WaveId> includeList = new LinkedList<WaveId>();
    List<WaveId> excludeList = new LinkedList<WaveId>();
    for (int i = 2; i < args.length;) {
      if (args[i].equals("-search")) {
        export.setSearch(args[++i]);
        i++;
      } else if (args[i].equals("-consumer_key")) {
        export.setConsumerKey(args[++i]);
        i++;
      } else if (args[i].equals("-consumer_secret")) {
        export.setConsumerSecret(args[++i]);
        i++;
      } else if (args[i].equals("-include")) {
        for (i = i + 1; i < args.length && args[i].charAt(0) != '-'; i++) {
          includeList.add(WaveId.deserialise(args[i]));
        }
      } else if (args[i].equals("-include_file")) {
        BufferedReader in = new BufferedReader(new FileReader(args[++i]));
        for (;;) {
          String line = in.readLine();
          if (line == null)
            break;
          includeList.add(WaveId.deserialise(line));
        }
        in.close();
        i++;
      } else if (args[i].equals("-exclude")) {
        for (i = i + 1; i < args.length && args[i].charAt(0) != '-'; i++) {
          excludeList.add(WaveId.deserialise(args[i]));
        }
      } else {
        usageError();
      }
    }
    if (!includeList.isEmpty()) {
      export.setIncludeList(includeList);
    }
    if (!excludeList.isEmpty()) {
      export.setExcludeList(excludeList);
    }

    export.authorization();
    export.exportWavesToFiles();
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public void setConsumerKey(String consumerKey) {
    this.consumerKey = consumerKey;
  }

  public void setConsumerSecret(String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  public void setExcludeList(List<WaveId> excludeList) {
    this.excludeList = excludeList;
  }

  public void setIncludeList(List<WaveId> includeList) {
    this.includeList = includeList;
  }

  /**
   * Performs authorization.
   */
  public void authorization() throws IOException, Base64DecoderException {
    OAuth oauth = new OAuth(serverUrl);
    if (consumerKey != null && consumerSecret != null) {
      rpcServerUrl = oauth.twoLeggedOAuth(api, consumerKey, consumerSecret);
    } else {
      rpcServerUrl = oauth.threeLeggedOAuth(api);
    }
  }

  /**
   * Exports waves to files.
   */
  private void exportWavesToFiles() throws IOException {
    Console.println();
    List<WaveId> waves = includeList;
    if (waves == null || waves.isEmpty()) {
      waves = getAllWavesList();
    }
    int exportedCount = 0;
    int failedCount = 0;
    for (int i = 0; i < waves.size(); i++) {
      boolean errorOccured = false;
      WaveId waveId = waves.get(i);
      try {
        if (excludeList != null && excludeList.contains(waveId)) {
          Console.println("Skipping wave " + waveId.serialise() + "...");
          continue;
        }
        Console.println("Exporting wave " + waveId.serialise()
            + " (" + (i + 1) + " of " + waves.size() + ") ...");
        new File(fileNames.getWaveDirPath(waveId)).mkdir();
        for (WaveletId waveletId : api.retrieveWaveletIds(waveId, rpcServerUrl)) {
          WaveletSnapshotProtoImpl snapshot = exportSnapshot(waveId, waveletId);
          Set<AttachmentId> attachmentIds = new HashSet<AttachmentId>();
          try {
            exportDeltas(waveId, waveletId, snapshot.getVersion().getPB(), attachmentIds);
          } catch (IOException ex) {
            errorOccured = true;
            Console.error("Export of deltas error.", ex);
          }
          if (!attachmentIds.isEmpty()) {
            new File(fileNames.getAttachmentsDirPath(waveId, waveletId)).mkdir();
            for (AttachmentId attachmentId : attachmentIds) {
              try {
                exportAttachment(waveId, waveletId, attachmentId);
              } catch (IOException ex) {
                errorOccured = true;
                Console.error("Uploading of attachment error.", ex);
              }
            }
          }
        }
      } catch (Exception ex) {
        errorOccured = true;
        Console.error("Exporting of " + waveId.serialise() + " error.", ex);
      }
      if (errorOccured) {
        failedCount++;
      } else {
        exportedCount++;
      }
    }
    Console.println();
    Console.println("Successfully exported " + exportedCount + " waves.");
    Console.println("Failed for " + failedCount + " waves.");
  }

  private WaveletSnapshotProtoImpl exportSnapshot(WaveId waveId, WaveletId waveletId)
      throws IOException {
    new File(fileNames.getWaveletDirPath(waveId, waveletId)).mkdir();
    Console.println("  wavelet " + waveletId.serialise());
    Console.print("    getting snapshot ...");
    String snapshotJSon = api.exportRawSnapshot(waveId, waveletId, rpcServerUrl);
    writeSnapshotToFile(waveId, waveletId, snapshotJSon);
    WaveletSnapshotProtoImpl snapshot = new WaveletSnapshotProtoImpl();
    try {
      snapshot.fromGson(jsonParser.parse(snapshotJSon), gson, null);
    } catch (GsonException ex) {
      throw new IOException(ex);
    }
    Console.println(" Ok, version " + (long)snapshot.getVersion().getVersion());
    return snapshot;
  }

  private void exportDeltas(WaveId waveId, WaveletId waveletId, ProtocolHashedVersion lastVersion,
      Set<AttachmentId> attachmentIds) throws IOException {
    HashedVersion zeroVersion = HASH_FACTORY.createVersionZero(WaveletName.of(waveId, waveletId));
    ProtocolHashedVersion version = CoreWaveletOperationSerializer.serialize(zeroVersion);
    for (int fetchNum = 0; version.getVersion() != lastVersion.getVersion(); fetchNum++) {
      Console.print("    getting deltas from version " + version.getVersion() + " ...");
      final List<byte[]> deltas = Lists.newArrayList();
      final AtomicReference<byte[]> targetVersion = new AtomicReference<byte[]>();
      api.exportRawDeltas(waveId, waveletId,
          version.toByteArray(), lastVersion.toByteArray(), rpcServerUrl, new RawDeltasListener() {
        @Override
        public void onSuccess(List<byte[]> rawDeltas, byte[] rawTargetVersion) {
          deltas.addAll(rawDeltas);
          targetVersion.set(rawTargetVersion);
        }

        @Override
        public void onFailire(String message) {
          Console.error(search);
        }
      });
      if (deltas.isEmpty()) {
        Console.println(" empty response");
        continue;
      }
      version = ProtocolHashedVersion.parseFrom(targetVersion.get());
      Console.println(" Ok, got to version " + version.getVersion());
      writeDeltasToFile(waveId, waveletId, deltas, fetchNum);
      for (byte[] delta : deltas) {
        attachmentIds.addAll(DeltaParser.getAttachemntIds(ProtocolWaveletDelta.parseFrom(delta)));
      }
    }
  }

  private void exportAttachment(WaveId waveId, WaveletId waveletId, AttachmentId attachmentId) throws IOException {
    Console.print("    getting attachment " + attachmentId.getId() + " ...");
    RawAttachmentData attachment = api.exportAttachment(attachmentId, rpcServerUrl);
    writeAttachmentToFile(waveId, waveletId, attachmentId, attachment);
    Console.println(" Ok");
  }

  private List<WaveId> getAllWavesList() throws IOException {
    List<WaveId> allList = new LinkedList<WaveId>();
    SearchResult result = api.search(search, 0, Integer.MAX_VALUE, rpcServerUrl);
    for (Digest digest : result.getDigests()) {
      allList.add(WaveId.deserialise(digest.getWaveId()));
    }
    return allList;
  }

  private void writeSnapshotToFile(WaveId waveId, WaveletId waveletId, String snapshot) throws IOException {
    String fileName = fileNames.getSnapshotFilePath(waveId, waveletId);
    writeFile(fileName, snapshot);
  }

  private void writeDeltasToFile(WaveId waveId, WaveletId waveletId, List<byte[]> deltas,
      int fetchNum) throws IOException {
    String fileName = fileNames.getDeltasFilePath(waveId, waveletId, fetchNum);
    String gsonStr = gson.toJson(deltas);
    writeFile(fileName, gsonStr);
  }

  private void writeAttachmentToFile(WaveId waveId, WaveletId waveletId, AttachmentId attachmentId,
      RawAttachmentData attachment) throws IOException {
    String fileName = fileNames.getAttachmentFilePath(waveId, waveletId, attachmentId);
    String gsonStr = gson.toJson(attachment);
    writeFile(fileName, gsonStr);
  }

  static private void writeFile(String name, String data) throws IOException {
    FileWriter w = new FileWriter(new File(name));
    w.write(data);
    w.close();
  }
}
