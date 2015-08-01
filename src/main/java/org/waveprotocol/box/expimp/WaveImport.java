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

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.wave.api.WaveService;
import com.google.wave.api.impl.RawAttachmentData;
import com.google.wave.api.impl.GsonFactory;

import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Import waves from files to Wiab.
 *
 * @author (akaplanov@gmail.com) (Andrew Kaplanov)
 */
public final class WaveImport {

  private enum ImportWaveletState { NOTHING_DONE, WAVELET_CREATED, WAVELET_UPDATED };

  private static final Gson gson = new GsonFactory().create();

  private final String serverUrl;
  private final WaveService api;
  private final FileNames fileNames;

  private String consumerKey;
  private String consumerSecret;
  private String rpcServerUrl;
  private String waveDomain;

  static public void usageError() {
    Console.println("Use: WaveImport <server URL> <export directory>\n"
      + "   [-consumer_key    Robot consumer key]\n"
      + "   [-consumer_secret Robot consumer secret]\n"
      + "   [-wave_domain     Target wave domain]");
    System.exit(1);
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      usageError();
    }
    WaveImport imp = new WaveImport(args[0], args[1]);
    for (int i = 2; i < args.length;) {
      if (args[i].equals("-consumer_key")) {
        imp.setConsumerKey(args[++i]);
        i++;
      } else if (args[i].equals("-consumer_secret")) {
        imp.setConsumerSecret(args[++i]);
        i++;
      } else if (args[i].equals("-wave_domain")) {
        imp.setWaveDomain(args[++i]);
        i++;
      } else {
        usageError();
      }
    }
    imp.authorization();
    imp.importWavesFromFiles();
  }

  private WaveImport(String serverUrl, String exportDir) {
    this.serverUrl = serverUrl;
    fileNames = new FileNames(exportDir);
    api = new WaveService();
    api.setFetchFimeout(WaveService.FETCH_INFINITE_TIMEOUT);
  }

  public void setConsumerKey(String consumerKey) {
    this.consumerKey = consumerKey;
  }

  public void setConsumerSecret(String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  public void setWaveDomain(String waveDomain) {
    this.waveDomain = waveDomain;
  }

  /**
   * Performs authorization.
   */
  public void authorization() throws IOException {
    OAuth oauth = new OAuth(serverUrl);
    if (consumerKey != null && consumerSecret != null) {
      rpcServerUrl = oauth.twoLeggedOAuth(api, consumerKey, consumerSecret);
    } else {
      rpcServerUrl = oauth.threeLeggedOAuth(api);
    }
  }

  /**
   * Imports waves from files.
   */
  public void importWavesFromFiles() {
    Console.println();
    File expDir = new File(fileNames.getExportDir());
    if (!expDir.exists()) {
      Console.println("Directory " + fileNames.getExportDir() + " is not exists.");
      System.exit(1);
    }
    File[] waveDirs = expDir.listFiles();
    Arrays.sort(waveDirs, new Comparator<File>() {

      @Override
      public int compare(File f1, File f2) {
        return f1.getName().compareTo(f2.getName());
      }
    });
    int createdCount = 0;
    int updatedCount = 0;
    int skippedCount = 0;
    int failedCount = 0;
    for (int i = 0; i < waveDirs.length; i++) {
      File waveDir = waveDirs[i];
      WaveId sourceWaveId = FileNames.getWaveIdFromFileName(waveDir.getName());
      WaveId targetWaveId = DomainConverter.convertWaveId(sourceWaveId, waveDomain);
      Console.println("Importing wave " + targetWaveId.serialise()
          + " (" + (i + 1) + " of " + waveDirs.length + ") ...");
      boolean waveCreated = false;
      boolean waveUpdated = false;
      File[] waveletDirs = waveDir.listFiles();
      for (File waveletDir : waveletDirs) {
        WaveletId sourceWaveletId = FileNames.getWaveletIdFromFileName(waveletDir.getName());
        try {
          ImportWaveletState state = importWavelet(sourceWaveId, sourceWaveletId);
          if (state == ImportWaveletState.WAVELET_CREATED && !IdUtil.isUserDataWavelet(sourceWaveletId)) {
            waveCreated = true;
          } else if (state != ImportWaveletState.NOTHING_DONE) {
            waveUpdated = true;
          }
        } catch (IOException ex) {
          failedCount++;
          Console.error("Importing wavelet error", ex);
        }
      }
      if (waveCreated) {
        createdCount++;
      } else if (waveUpdated) {
        updatedCount++;
      } else {
        skippedCount++;
      }
    }
    Console.println();
    Console.println("Created " + createdCount + " waves.");
    Console.println("Updated " + updatedCount + " waves.");
    Console.println("Skipped " + skippedCount + " waves.");
    Console.println("Failed for " + failedCount + " waves.");
  }

  private ImportWaveletState importWavelet(WaveId sourceWaveId, WaveletId sourceWaveletId)
      throws IOException {
    ImportWaveletState state = ImportWaveletState.NOTHING_DONE;
    WaveId targetWaveId = DomainConverter.convertWaveId(sourceWaveId, waveDomain);
    WaveletId targetWaveletId;
    try {
      targetWaveletId = DomainConverter.convertWaveletId(sourceWaveletId, waveDomain);
    } catch (InvalidParticipantAddress ex) {
      throw new IOException(ex);
    }
    Set<AttachmentId> attachmentIds = new HashSet<AttachmentId>();
    Console.println("  wavelet " + targetWaveletId.serialise());
    for (int part=0 ; ; part++) {
      File deltasFile = new File(fileNames.getDeltasFilePath(sourceWaveId, sourceWaveletId, part));
      if (!deltasFile.exists()) {
        break;
      }
      ImportWaveletState st = importDeltas(deltasFile, targetWaveId, targetWaveletId, attachmentIds);
      if (state == ImportWaveletState.NOTHING_DONE) {
        state = st;
      }
    }
    importAttachments(sourceWaveId, sourceWaveletId, targetWaveId, targetWaveletId, attachmentIds);
    return state;
  }

  private ImportWaveletState importDeltas(File deltasFile, WaveId targetWaveId, WaveletId targetWaveletId,
      Set<AttachmentId> attachmentIds) throws IOException {
    ImportWaveletState state = ImportWaveletState.NOTHING_DONE;
    @SuppressWarnings("unchecked")
    List<byte[]> sourceHistory = gson.fromJson(readFile(deltasFile), GsonFactory.RAW_DELTAS_TYPE);
    List<byte[]> targetHistory;
    List<ProtocolWaveletDelta> deltas = DeltaParser.parseDeltas(sourceHistory);
    List<ProtocolWaveletDelta> targetDeltas;
    if (waveDomain != null) {
      try {
        targetDeltas = DomainConverter.convertDeltas(deltas, waveDomain);
      } catch (InvalidParticipantAddress ex) {
        throw new IOException(ex);
      }
      targetHistory = new LinkedList<byte[]>();
      for (ProtocolWaveletDelta delta : targetDeltas) {
        targetHistory.add(delta.toByteArray());
      }
    } else {
      targetDeltas = deltas;
      targetHistory = sourceHistory;
    }
    if (!targetDeltas.isEmpty()) {
      long fromVersion = targetDeltas.get(0).getHashedVersion().getVersion();
      ProtocolWaveletDelta lastDelta = targetDeltas.get(targetDeltas.size()-1);
      long toVersion = lastDelta.getHashedVersion().getVersion()+lastDelta.getOperationCount();
      Console.print("    send deltas " + fromVersion + "-" + toVersion + " ...");
      long importedFromVersion = api.importRawDeltas(targetWaveId, targetWaveletId, targetHistory, rpcServerUrl);
      if (fromVersion == importedFromVersion) {
        Console.println(" imported");
      } else if (importedFromVersion == -1) {
        Console.println(" skipped");
      } else {
        Console.println(" imported from version " + importedFromVersion);
      }
      if (fromVersion == importedFromVersion) {
        if (fromVersion == 0) {
          state = ImportWaveletState.WAVELET_CREATED;
        } else {
          state = ImportWaveletState.WAVELET_UPDATED;
        }
      } else if (importedFromVersion != -1) {
        state = ImportWaveletState.WAVELET_UPDATED;
      }
      if (importedFromVersion != -1) {
        for (ProtocolWaveletDelta delta : targetDeltas) {
          if (delta.getHashedVersion().getVersion() >= importedFromVersion) {
            attachmentIds.addAll(DeltaParser.getAttachemntIds(delta));
          }
        }
      }
    }
    return state;
  }

  private void importAttachments(WaveId sourceWaveId, WaveletId sourceWaveletId,
      WaveId targetWaveId, WaveletId targetWaveletId, Set<AttachmentId> attachmentIds)
      throws FileNotFoundException, IOException {
    for (AttachmentId attachmentId : attachmentIds) {
      String attachmentFile = fileNames.getAttachmentFilePath(sourceWaveId, sourceWaveletId, attachmentId);
      RawAttachmentData attachmentData = gson.fromJson(readFile(new File(attachmentFile)), RawAttachmentData.class);
      Console.print("    importing attachment " + attachmentId.serialise() + " ...");
      api.importAttachment(targetWaveId, targetWaveletId, attachmentId, attachmentData, rpcServerUrl);
      Console.println(" Ok");
    }
  }

  private static String readFile(File file) throws FileNotFoundException, IOException {
    Reader reader = new FileReader(file);
    StringBuilder sb = new StringBuilder();
    char buf[] = new char[1024];
    for (;;) {
      int ret = reader.read(buf, 0, buf.length);
      if (ret == -1) {
        break;
      }
      sb.append(buf, 0, ret);
    }
    return sb.toString();
  }
}