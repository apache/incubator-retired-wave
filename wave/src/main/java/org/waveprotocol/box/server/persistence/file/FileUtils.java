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

package org.waveprotocol.box.server.persistence.file;

import com.google.common.base.Preconditions;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.util.logging.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * Utility methods for file stores.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FileUtils {
  private static final String SEPARATOR = "_";

  /**
   * Converts an arbitrary string into a format that can be stored safely on the filesystem.
   *
   * @param str the string to encode
   * @return the encoded string
   */
  public static String toFilenameFriendlyString(String str) {
    byte[] bytes;
    try {
      bytes = str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      // This should never happen.
      throw new IllegalStateException("UTF-8 not supported", e);
    }

    return new String(Hex.encodeHex(bytes));
  }

  /**
   * Decodes a string that was encoded using toFilenameFriendlyString.
   *
   * @param encoded the encoded string
   * @return the decoded string
   * @throws DecoderException the string's encoding is invalid
   */
  public static String fromFilenameFriendlyString(String encoded) throws DecoderException {
    byte[] bytes = Hex.decodeHex(encoded.toCharArray());
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // This should never happen.
      throw new IllegalStateException("UTF-8 not supported", e);
    }
  }

  /** Decode a path segment pair. Throws IllegalArgumentException if the encoding is invalid */
  private static Pair<String, String> decodePathSegmentPair(String pathSegment) {
    String[] components = pathSegment.split(SEPARATOR);
    Preconditions.checkArgument(components.length == 2, "WaveId path name invalid");
    try {
      return new Pair<String, String>(fromFilenameFriendlyString(components[0]),
          fromFilenameFriendlyString(components[1]));
    } catch (DecoderException e) {
      throw new IllegalArgumentException("Wave path component encoding invalid");
    }
  }

  /**
   * Creates a filename-friendly pathname for the given waveId.
   *
   * The format is DOMAIN + '_' + ID where both the domain and the id are encoded
   * to a pathname friendly format.
   *
   * @param waveId the waveId to encode
   * @return a path segment which corresponds to the waveId
   */
  public static String waveIdToPathSegment(WaveId waveId) {
    String domain = toFilenameFriendlyString(waveId.getDomain());
    String id = toFilenameFriendlyString(waveId.getId());
    return domain + SEPARATOR + id;
  }

  /**
   * Converts a path segment created using waveIdToPathSegment back to a wave id
   *
   * @param pathSegment
   * @return the decoded WaveId
   * @throws IllegalArgumentException the encoding on the path segment is invalid
   */
  public static WaveId waveIdFromPathSegment(String pathSegment) {
    Pair<String, String> segments = decodePathSegmentPair(pathSegment);
    return WaveId.of(segments.first, segments.second);
  }

  /**
   * Creates a filename-friendly path segment for a waveId.
   *
   * The format is "domain_id", encoded in a pathname friendly format.
   * @param waveletId
   * @return the decoded WaveletId
   */
  public static String waveletIdToPathSegment(WaveletId waveletId) {
    String domain = toFilenameFriendlyString(waveletId.getDomain());
    String id = toFilenameFriendlyString(waveletId.getId());
    return domain + SEPARATOR + id;
  }

  /**
   * Converts a path segment created using waveIdToPathSegment back to a wave id.
   *
   * @param pathSegment
   * @return the decoded waveletId
   * @throws IllegalArgumentException the encoding on the path segment is invalid
   */
  public static WaveletId waveletIdFromPathSegment(String pathSegment) {
    Pair<String, String> segments = decodePathSegmentPair(pathSegment);
    return WaveletId.of(segments.first, segments.second);
  }

  /**
   * Creates a filename-friendly path segment for a wavelet name.
   *
   * @return the filename-friendly path segment representing the wavelet
   */
  public static String waveletNameToPathSegment(WaveletName waveletName) {
    return waveIdToPathSegment(waveletName.waveId)
        + File.separatorChar
        + waveletIdToPathSegment(waveletName.waveletId);
  }

  /**
   * Get a file for random binary access. If the file doesn't exist, it will be created.
   *
   * Calls to write() will not flush automatically. Call file.getChannel().force(true) to force
   * writes to flush to disk.
   *
   * @param fileRef the file to open
   * @return an opened RandomAccessFile wrapping the requested file
   * @throws IOException an error occurred opening or creating the file
   */
  public static RandomAccessFile getOrCreateFile(File fileRef) throws IOException {
    if (!fileRef.exists()) {
      fileRef.getParentFile().mkdirs();
      fileRef.createNewFile();
    }

    RandomAccessFile file;
    try {
      file = new RandomAccessFile(fileRef, "rw");
    } catch (FileNotFoundException e) {
      // This should never happen.
      throw new IllegalStateException("Java said the file exists, but it can't open it", e);
    }

    return file;
  }

  /** Create and return a new temporary directory */
  public static File createTemporaryDirectory() throws IOException {
    // We want a temporary directory. createTempFile will make a file with a
    // good temporary path. Its a bit nasty, but we'll create the file, then
    // delete it and create a directory with the same name.

    File dir = File.createTempFile("fedoneattachments", null);

    if (!dir.delete() || !dir.mkdir()) {
      throw new IOException("Could not make temporary directory for attachment store: "
          + dir);
    }

    return dir.getAbsoluteFile();
  }

  /**
   * Close the closeable and log, but ignore, any exception thrown.
   */
  public static void closeAndIgnoreException(Closeable closeable, File file, Log LOG) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        // This should never happen in practice. But just in case... log it.
        LOG.warning("Failed to close file: " + file.getAbsolutePath(), e);
      }
    }
  }

  /**
   * Create dir if it doesn't exist, and perform checks to make sure that the dir's contents are
   * listable, that files can be created in the dir, and that files in the dir are readable.
   */
  public static void performDirectoryChecks(String dir, final String extension, String dirType,
      Log LOG) throws PersistenceException {
    File baseDir = createDirIfNotExists(dir, dirType);

    // Make sure we can read files by trying to read one of the files.
    File[] files = baseDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(extension);
      }
    });

    if (files == null) {
      throw new PersistenceException(String.format(
          "Configured %s directory (%s) does not appear to be readable!", dirType, dir));
    }

    /*
     * If file list isn't empty, try opening the first file in the list to make sure it
     * is readable. If the first file is readable, then it is likely that the rest will
     * be readable as well.
     */
    if (files.length > 0) {
      try {
        FileInputStream file = new FileInputStream(files[0]);
        file.read();
      } catch (IOException e) {
        throw new PersistenceException(
            String.format(
              "Failed to read '%s' in configured %s directory '%s'. "
              + "The directory's contents do not appear to be readable.",
              dirType, files[0].getName(), dir),
            e);
      }
    }

    // Make sure the dir is writable.
    try {
      File tmp = File.createTempFile("tempInitialization", ".temp", baseDir);
      FileOutputStream stream = new FileOutputStream(tmp);
      stream.write(new byte[]{'H','e','l','l','o'});
      stream.close();
      tmp.delete();
    } catch (IOException e) {
      throw new PersistenceException(String.format(
          "Configured %s directory (%s) does not appear to be writable!", dirType, dir), e);
    }
  }

  /**
   * Creates a directory if it doesn't exist.
   *
   * @param dir the directory location.
   * @param dirType the directory type description (only useful for logs).
   * @return the File directory (it's created if doesn't exist).
   * @throws PersistenceException if the directory doesn't exist and cannot be
   *         created or when the path described by <code>dir</code> is not a
   *         directory.
   */
  public static File createDirIfNotExists(String dir, String dirType) throws PersistenceException {
    File baseDir = new File(dir);

    // Make sure the dir exists.
    if (!baseDir.exists()) {
      // It doesn't so try and create it.
      if (!baseDir.mkdirs()) {
        throw new PersistenceException(String.format(
            "Configured %s directory (%s) doesn't exist and could not be created!", dirType, dir));
      }
    }

    // Make sure the dir is a directory.
    if (!baseDir.isDirectory()) {
      throw new PersistenceException(String.format(
          "Configured %s path (%s) isn't a directory!", dirType, dir));
    }
    return baseDir;
  }

  /**
   * Opens a file and read the content to a string.
   *
   * @param fileName the file to read.
   * @return a string with the content of the file.
   * @throws IOException if the method failed to open the file for reading.
   */
  public static String getStringFromFile(String fileName) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    File file = new File(fileName);
    FileReader fileReader = new FileReader(file);
    BufferedReader bufferedReader = new BufferedReader(fileReader);

    String line;
    while ((line = bufferedReader.readLine()) != null) {
      stringBuilder.append(line);
    }
    fileReader.close();

    return stringBuilder.toString();
  }

  public static boolean isDirExistsAndNonEmpty(String dir) {
    File baseDir = new File(dir);
    if (!(baseDir.exists() && baseDir.isDirectory()) || baseDir.list().length == 0) {
      return false;
    }
    return true;
  }
}
