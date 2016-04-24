/*
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

package com.google.wave.api.impl;

/**
 * Attachment data.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class RawAttachmentData {
  private final String fileName;
  private final String creator;
  private final byte[] data;

  /**
   * Exported raw attachment data.
   *
   * @param fileName attachment file name.
   * @param creator attachment creator.
   * @param data attachment data.
   */
  public RawAttachmentData(String fileName, String creator, byte[] data) {
    this.fileName = fileName;
    this.creator = creator;
    this.data = data;
  }

  public String getFileName() {
    return fileName;
  }

  public String getCreator() {
    return creator;
  }

  public byte[] getData() {
    return data;
  }
}
