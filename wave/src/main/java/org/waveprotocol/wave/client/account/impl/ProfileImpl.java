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

package org.waveprotocol.wave.client.account.impl;


import com.google.common.base.Joiner;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * A {@link Profile} which determines all properties from just a
 * {@link ParticipantId} given on construction.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class ProfileImpl implements Profile {

  private final AbstractProfileManager<? super ProfileImpl> manager;
  private final ParticipantId id;

  // Lazily loaded values
  private String firstName;
  private String fullName;
  private String imageUrl;

  public ProfileImpl(AbstractProfileManager<ProfileImpl> manager, ParticipantId id) {
    this.manager = manager;
    this.id = id;
  }

  @Override
  public ParticipantId getParticipantId() {
    return id;
  }

  @Override
  public String getAddress() {
    return id.getAddress();
  }

  @Override
  public String getFullName() {
    if (fullName == null) {
      buildNames();
    }
    return fullName;
  }

  @Override
  public String getFirstName() {
    if (firstName == null) {
      buildNames();
    }
    return firstName;
  }

  @Override
  public String getImageUrl() {
    if (imageUrl == null) {
      imageUrl = "static/images/unknown.jpg";
    }
    return imageUrl;
  }

  /**
   * Attempts to create the fragments of the participant's name from their
   * address, for example "john.smith@example.com" into ["John", "Smith"].
   */
  private void buildNames() {
    List<String> names = CollectionUtils.newArrayList();
    String nameWithoutDomain = id.getAddress().split("@")[0];
    if (nameWithoutDomain != null && !nameWithoutDomain.isEmpty()) {
      // Include empty names from fragment, so split with a -ve.
      for (String fragment : nameWithoutDomain.split("[._]", -1)) {
        if (!fragment.isEmpty()) {
          names.add(capitalize(fragment));
        }
      }
      // ParticipantId normalization implies names can not be empty.
      assert !names.isEmpty();
      firstName = names.get(0);
      fullName = Joiner.on(' ').join(names);
    } else {
      // Name can be empty in case of shared domain participant which has the the form:
      // @example.com.
      fullName = id.getAddress();
    }
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : (Character.toUpperCase(s.charAt(0))) + s.substring(1);
  }

  /**
   * Replaces this profile's fields.
   * <p>
   * Each non-null argument replaces this profile's corresponding field. Null
   * arguments have no effect (i.e., they do not clear the existing field).
   */
  public void update(String firstName, String fullName, String imageUrl) {
    this.firstName = firstName != null ? firstName : this.firstName;
    this.fullName = fullName != null ? fullName : this.fullName;
    this.imageUrl = imageUrl != null ? imageUrl : this.imageUrl;

    manager.fireOnUpdated(this);
  }

  @Override
  public String toString() {
    return "ProfileImpl [id=" + id + ", firstName=" + firstName + ", fullName=" + fullName
        + ", imageUrl=" + imageUrl + "]";
  }
}
