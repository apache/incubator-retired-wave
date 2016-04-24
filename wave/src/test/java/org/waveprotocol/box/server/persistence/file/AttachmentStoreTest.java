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

import com.typesafe.config.ConfigFactory;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentStoreTestBase;

import java.io.File;

/**
 * A wrapper for the tests in AttachmentStoreBase which uses a file based
 * attachment store.
 */
public class AttachmentStoreTest extends AttachmentStoreTestBase {
  private File path;

  @Override
  protected void setUp() throws Exception {
    path = FileUtils.createTemporaryDirectory();
    super.setUp();
  }

  @Override
  protected AttachmentStore newAttachmentStore() {
    return new FileAttachmentStore(
      ConfigFactory.parseString("core.attachment_store_directory : " + path.getAbsolutePath()));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    org.apache.commons.io.FileUtils.deleteDirectory(path);

    // On Linux, it will be impossible to delete the directory until all of the
    // input streams have been closed. This is annoying, but gives us a nice
    // check to make sure everyone is behaving themselves.
    assertFalse(path.exists());
  }
}
