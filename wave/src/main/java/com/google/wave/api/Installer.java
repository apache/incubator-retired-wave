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

/**
 * Installers are widgets that let Wave users install extensions on
 * their account. Installers are identified by the URL that points to
 * their manifest specification.
 *
 * @author pamelafox@google.com (Pamela Fox)
 */
public class Installer extends Element {

  /**
   * Constructs an empty installer.
   */
  public Installer() {
    super(ElementType.INSTALLER);
    setManifest("");
  }

  /**
   * Constructs an installer for the specified manifest url.
   *
   * @param url the url of the manifest XML.
   */
  public Installer(String manifest) {
    super(ElementType.INSTALLER);
    setManifest(manifest);
  }

  /**
   * Returns the URL of the manifest XML.
   *
   * @return the URL for the manifest XML.
   */
  public String getManifest() {
    return getProperty("manifest");
  }

  /**
   * Changes the URL for the manifest to the given url.
   * This will cause the installer puzzle piece to be re-loaded.
   *
   * @param manifest the new manifest url.
   */
  public void setManifest(String manifest) {
    setProperty("manifest", manifest);
  }
}
