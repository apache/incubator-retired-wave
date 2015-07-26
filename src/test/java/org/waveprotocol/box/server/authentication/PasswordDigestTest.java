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

package org.waveprotocol.box.server.authentication;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class PasswordDigestTest extends TestCase {
  public void testPasswordValidatesItself() {
    PasswordDigest pwd = new PasswordDigest("internet".toCharArray());
    assertTrue(pwd.verify("internet".toCharArray()));
    assertFalse(pwd.verify("wrongpwd".toCharArray()));
  }

  public void testSerializeDeserialize() {
    PasswordDigest pwd = new PasswordDigest("tubes".toCharArray());

    byte[] digest = pwd.getDigest();
    byte[] salt = pwd.getSalt();
    PasswordDigest roundtripped = PasswordDigest.from(salt, digest);

    assertTrue(pwd.verify("tubes".toCharArray()));
    assertFalse(pwd.verify("wrongpwd".toCharArray()));
  }

  public void testSaltAtLeast10Bytes() {
    PasswordDigest pwd = new PasswordDigest("blogosphere".toCharArray());
    byte[] salt = pwd.getSalt();
    assertTrue(salt.length >= 10);
  }

  public void testReallyLongPasswordWorksRight() {
    char[] reallyLongPassword = new char[1024];

    for (int i = 0; i < reallyLongPassword.length; i++) {
      // We'll make a password filled with junk.
      reallyLongPassword[i] = (char) i;
    }

    PasswordDigest pwd = new PasswordDigest(reallyLongPassword);
    assertTrue(pwd.verify(reallyLongPassword));

    // Make a new password that misses the last character. It shouldn't work.
    char[] shorterPassword = Arrays.copyOf(reallyLongPassword, 1023);
    assertFalse(pwd.verify(shorterPassword));
  }

  public void testEditingExposedBytesDoesntChangeInternalState() {
    PasswordDigest pwd1 = new PasswordDigest("webernets".toCharArray());

    byte[] digest = pwd1.getDigest();
    byte[] salt = pwd1.getSalt();

    PasswordDigest pwd2 = PasswordDigest.from(salt, digest);

    // We'll mess with the digest and salt we got back and make sure both
    // passwords still verify normally.
    digest[digest.length / 2]++;
    salt[salt.length / 2]--;

    assertTrue(pwd1.verify("webernets".toCharArray()));
    assertTrue(pwd2.verify("webernets".toCharArray()));
  }
  
  public void testEmptyPasswordVerifiesCorrectly() {
    PasswordDigest pwd = new PasswordDigest(new char[0]);
    assertTrue(pwd.verify(new char[0]));
    assertFalse(pwd.verify(new char[1]));
  }
}
