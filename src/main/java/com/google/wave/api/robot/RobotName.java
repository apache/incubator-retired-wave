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

package com.google.wave.api.robot;

import java.util.regex.Pattern;

/**
 * Simple class representing a (parsed) address of a robot.
 * The general form of these addresses is: id[+proxyfor][#version]@domain
 * where id is the basic identifier of the robot, proxyfor the id on
 * that is proxied for and version the appengine version that should be
 * used.
 *
 */
public final class RobotName {
  /**
   * Builds {@link RobotName}s.
   */
  public static class Builder {
    private final String id;
    private final String domain;
    private String proxyFor;
    private String version;

    public Builder(String id, String domain) {
      this.id = id;
      this.domain = domain;
      this.proxyFor = "";
      this.version = "";
    }

    public Builder withProxyFor(String proxyFor) {
      this.proxyFor = proxyFor;
      return this;
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public RobotName build() {
      return new RobotName(id, domain, proxyFor, version);
    }
  }

  /**
   * Regular expression for robot participant id. TLD is between 2 and 6
   * characters long to match the ascii IANA top level domains as of Sept 2010.
   */
  // TODO(user): Make this stricter.
  private static final Pattern ROBOT_ID_REGEX =
      Pattern.compile("^[a-z0-9._%+#-]+?@[a-z0-9.-]+\\.[a-z]{2,6}$", Pattern.CASE_INSENSITIVE);

  /**
   * Checks if the given address looks like a well-formed robot id.
   *
   * @param address the address to check.
   * @return {@code true} if the given address is a robot id.
   */
  public static boolean isWellFormedAddress(String address) {
    return address != null ? ROBOT_ID_REGEX.matcher(address).matches() : false;
  }

  /**
   * Construct a RobotName from an address. The address must be well-formed as
   * described. @see RobotName
   *
   * @param address the address to parse.
   * @return robot name instance, or {@code null} if the address is not a robot
   *     address.
   */
  public static RobotName fromAddress(String address) {
    if (!isWellFormedAddress(address)) {
      return null;
    }

    int index = address.indexOf('@');
    String id = address.substring(0, index);
    String domain = address.substring(index + 1);
    index = id.indexOf('#');
    String version = "";
    if (index >= 0) {
      version = id.substring(index + 1);
      id = address.substring(0, index);
    }
    index = id.indexOf('+');
    String proxyFor = "";
    if (index >= 0) {
      proxyFor = id.substring(index + 1);
      id = id.substring(0, index);
    }
    return new RobotName(id, domain, proxyFor, version);
  }

  private final String id;
  private final String domain;
  private String proxyFor;
  private String version;

  public RobotName(String id, String domain) {
    this.id = id;
    this.domain = domain;
    this.proxyFor = "";
    this.version = "";
  }

  private RobotName(String id, String domain, String proxyFor, String version) {
    this.id = id;
    this.domain = domain;
    this.proxyFor = proxyFor;
    this.version = version;
  }

  public boolean hasProxyFor() {
    return !proxyFor.isEmpty();
  }

  public boolean hasVersion() {
    return !version.isEmpty();
  }

  public String getId() {
    return id;
  }

  public String getDomain() {
    return domain;
  }

  public String getProxyFor() {
    return proxyFor;
  }

  public void setProxyFor(String proxyFor) {
    this.proxyFor = proxyFor;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Converts the robot name to the participant id form, including proxy and
   * version information, for example, {@code foo+proxy#1@appspot.com}.
   *
   * @return the robot participant address.
   */
  public String toParticipantAddress() {
    return toAddress(true, true);
  }

  /**
   * Converts the robot name to the email address form, excluding proxy and
   * version information, for example, {@code foo@appspot.com}.
   *
   * @return the robot participant address.
   */
  public String toEmailAddress() {
    return toAddress(false, false);
  }

  /**
   * Converts the robot name to the email address form, including version but
   * excluding proxy information, for example, {@code foo#1@appspot.com}.
   *
   * @return the robot participant address.
   */
  public String toEmailAddressWithVersion() {
    return toAddress(false, true);
  }

  /**
   * Converts the robot name to address form (e.g. foo@appspot.com).
   *
   * @param includeVersion whether to include the version or not.
   * @return the robot address.
   */
  @Deprecated
  public String toAddress(boolean includeVersion) {
    return toAddress(false, includeVersion);
  }

  /**
   * Converts the robot name to address form (e.g. foo+proxy#1@appspot.com).
   *
   * @param includeProxyFor whether to include the proxy id or not.
   * @param includeVersion whether to include the version or not.
   * @return the robot address.
   */
  private String toAddress(boolean includeProxyFor, boolean includeVersion) {
    StringBuilder address = new StringBuilder(id);
    if (includeProxyFor && hasProxyFor()) {
      address.append('+').append(proxyFor);
    }
    if (includeVersion && hasVersion()) {
      address.append('#').append(version);
    }
    address.append('@').append(domain);
    return address.toString();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) { return false; }
    if (other == this) { return true; }
    if (other instanceof RobotName) {
      RobotName o = (RobotName) other;
      return id.equals(o.id) && domain.equals(o.domain) && proxyFor.equals(o.proxyFor)
          && version.equals(o.version);
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode();
    result = prime * result + domain.hashCode();
    result = prime * result + proxyFor.hashCode();
    result = prime * result + version.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return toParticipantAddress();
  }
}
