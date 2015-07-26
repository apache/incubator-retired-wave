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
package org.waveprotocol.box.server.persistence.lucene;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.box.server.waveserver.IndexException;

import java.io.File;
import java.io.IOException;

/**
 * File system based {@link IndexDirectory}.
 *
 * @author A. Kaplanov
 */
public class FSIndexDirectory implements IndexDirectory {

  private Directory directory;

  @Inject
  public FSIndexDirectory(Config config) {
    String directoryName = config.getString("core.index_directory");
    if (directory == null) {
      File file;
      try {
        file = FileUtils.createDirIfNotExists(directoryName, "");
      } catch (PersistenceException e) {
        throw new IndexException("Cannot create index directory " + directoryName, e);
      }
      try {
        directory = FSDirectory.open(file);
      } catch (IOException e) {
        throw new IndexException("Cannot open index directory " + directoryName, e);
      }
    }
  }

  @Override
  public Directory getDirectory() throws IndexException {
    return directory;
  }
}
