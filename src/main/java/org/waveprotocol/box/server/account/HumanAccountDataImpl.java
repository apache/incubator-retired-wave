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

package org.waveprotocol.box.server.account;

import com.google.common.base.Preconditions;

import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Human Account. Expected to be expanded when authentication is implemented.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author akaplanov@gmail.com (Andrew kaplanov)
 */
public final class HumanAccountDataImpl implements HumanAccountData {
  private final ParticipantId id;
  private final PasswordDigest passwordDigest;
  private String locale;

  /**
   * Creates an {@link HumanAccountData} for the given username, with no
   * password.
   *
   * This user will not be able to login using password-bsed authentication.
   *
   * @param id non-null participant id for this account.
   */
  public HumanAccountDataImpl(ParticipantId id) {
    this(id, null);
  }

  /**
   * Creates an {@link HumanAccountData} for the given participant.
   *
   * @param id non-null participant id for this account.
   * @param passwordDigest The user's password digest, or null if the user
   *        should not be authenticated using a password. This is typically
   *        obtained by calling {@code new PasswordDigest(password_chars);}
   */
  public HumanAccountDataImpl(ParticipantId id, PasswordDigest passwordDigest) {
    Preconditions.checkNotNull(id, "Id can not be null");

    this.id = id;
    this.passwordDigest = passwordDigest;
  }

  @Override
  public ParticipantId getId() {
    return id;
  }

  @Override
  public PasswordDigest getPasswordDigest() {
    return passwordDigest;
  }

  @Override
  public String getLocale() {
    return locale;
  }

  @Override
  public void setLocale(String locale) {
    this.locale = locale;
  }

  @Override
  public boolean isHuman() {
    return true;
  }

  @Override
  public HumanAccountData asHuman() {
    return this;
  }

  @Override
  public boolean isRobot() {
    return false;
  }

  @Override
  public RobotAccountData asRobot() {
    throw new UnsupportedOperationException("Can't turn a HumanAccount into a RobotAccount");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((passwordDigest == null) ? 0 : passwordDigest.hashCode());
    result = prime * result + ((locale == null) ? 0 : locale.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof HumanAccountDataImpl)) return false;
    HumanAccountDataImpl other = (HumanAccountDataImpl) obj;
    if (id == null) {
      if (other.id != null) return false;
    } else if (!id.equals(other.id)) return false;
    if (passwordDigest == null) {
      if (other.passwordDigest != null) return false;
    } else if (!passwordDigest.equals(other.passwordDigest)) return false;
    if (locale == null) {
      if (other.locale != null) return false;
    } else if (!locale.equals(other.locale)) return false;
    return true;
  }
}
