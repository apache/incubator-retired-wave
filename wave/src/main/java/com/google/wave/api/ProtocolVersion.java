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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An enumeration that represents the robot API wire protocol versions.
 *
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public enum ProtocolVersion {
  V1("0.1"),
  V2("0.2"),
  V2_1("0.21"),
  V2_2("0.22");

  /** The default protocol version. */
  public static final ProtocolVersion DEFAULT = ProtocolVersion.V2_2;

  /** Logger. */
  private static final Logger LOG = Logger.getLogger(ProtocolVersion.class.getName());

  /** Reverse mapping from version string to {@link ProtocolVersion} enum. */
  private static final Map<String, ProtocolVersion> REVERSE_LOOKUP_MAP =
      new HashMap<String, ProtocolVersion>(ProtocolVersion.values().length);

  static {
    for (ProtocolVersion protocolVersion : ProtocolVersion.values()) {
      String versionString = protocolVersion.versionString;
      if (REVERSE_LOOKUP_MAP.containsKey(versionString)) {
        LOG.warning("There are more than one ProtocolVersions that have the same version string " +
            versionString);
      }
      REVERSE_LOOKUP_MAP.put(versionString, protocolVersion);
    }
  }

  /** The version string, for example, 0.1. */
  private final String versionString;

  /**
   * Constructor.
   *
   * @param versionString the version string, for example, 0.1.
   */
  private ProtocolVersion(String versionString) {
    this.versionString = versionString;
  }

  /**
   * @return the version string.
   */
  public String getVersionString() {
    return versionString;
  }

  /**
   * @param other the other {@link ProtocolVersion} to compare to.
   * @return {@code true} if {@code this} version is less than the {@code other}
   *     version.
   */
  public boolean isLessThan(ProtocolVersion other) {
    return versionString.compareTo(other.versionString) < 0;
  }

  /**
   * @param other the other {@link ProtocolVersion} to compare to.
   * @return {@code true} if {@code this} version is less than or equal to the
   *     {@code other} version.
   */
  public boolean isLessThanOrEqual(ProtocolVersion other) {
    return versionString.compareTo(other.versionString) <= 0;
  }

  /**
   * @param other the other {@link ProtocolVersion} to compare to.
   * @return {@code true} if {@code this} version is greater than or equal to
   *     the {@code other} version.
   */
  public boolean isGreaterThanOrEqual(ProtocolVersion other) {
    return versionString.compareTo(other.versionString) >= 0;
  }

  /**
   * @param other the other {@link ProtocolVersion} to compare to.
   * @return {@code true} if {@code this} version is greater than the
   *     {@code other} version.
   */
  public boolean isGreaterThan(ProtocolVersion other) {
    return versionString.compareTo(other.versionString) > 0;
  }

  /**
   * Returns a {@link ProtocolVersion} that represents the given version string,
   * or {@link ProtocolVersion#DEFAULT} if the version string is invalid.
   *
   * @param versionString the version string.
   * @return a {@link ProtocolVersion}.
   */
  public static ProtocolVersion fromVersionString(String versionString) {
    ProtocolVersion protocolVersion = REVERSE_LOOKUP_MAP.get(versionString);
    if (protocolVersion == null) {
      return DEFAULT;
    }
    return protocolVersion;
  }
}
