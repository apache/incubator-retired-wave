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

import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.InvalidIdException;

import java.text.DecimalFormat;

/**
 * File and directory names for Export/Import utilities.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class FileNames {
  private static final String FILE_NUMBER_PATTERN="000000";

  private final String exportDir;

  public FileNames(String exportDir) {
    this.exportDir = exportDir;
  }

  public static String getWaveDirName(WaveId waveId) {
    return FileUtils.waveIdToPathSegment(waveId);
  }

  public static String getWaveletDirName(WaveletId waveletId) {
    return FileUtils.waveletIdToPathSegment(waveletId);
  }

  public static String getSnapshotFileName() {
    return "snapshot.json";
  }

  public static String getDeltasFileName(int part) {
    return "deltas." + new DecimalFormat(FILE_NUMBER_PATTERN).format(part) + ".json";
  }

  public static String getAttachmentsDirName() {
    return "attachments";
  }

  public static String getAttachmentFileName(AttachmentId attachmentId) {
    return attachmentId.serialise() + ".json";
  }

  public String getExportDir() {
    return exportDir;
  }

  public String getWaveDirPath(WaveId waveId) {
    return exportDir + "/" + getWaveDirName(waveId);
  }

  public String getWaveletDirPath(WaveId waveId, WaveletId waveletId) {
    return getWaveDirPath(waveId) + "/" + getWaveletDirName(waveletId);
  }

  public String getSnapshotFilePath(WaveId waveId, WaveletId waveletId) {
    return getWaveletDirPath(waveId, waveletId) + "/" + getSnapshotFileName();
  }

  public String getDeltasFilePath(WaveId waveId, WaveletId waveletId, int part) {
    return getWaveletDirPath(waveId, waveletId) + "/" + getDeltasFileName(part);
  }

  public String getAttachmentsDirPath(WaveId waveId, WaveletId waveletId) {
    return getWaveletDirPath(waveId, waveletId) + "/" + getAttachmentsDirName();
  }

  public String getAttachmentFilePath(WaveId waveId, WaveletId waveletId, AttachmentId attachmentId) {
    return getAttachmentsDirPath(waveId, waveletId) + "/" + getAttachmentFileName(attachmentId);
  }

  public static WaveId getWaveIdFromFileName(String name) {
     return FileUtils.waveIdFromPathSegment(name);
  }

  public static WaveletId getWaveletIdFromFileName(String name) {
     return FileUtils.waveletIdFromPathSegment(name);
  }

  public static AttachmentId getAttachmentIdFromFileName(String name) throws InvalidIdException {
     return AttachmentId.deserialise(name.substring(0, name.length()-5));
  }
}
