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

package org.waveprotocol.box.server.util;

import net.oauth.OAuth;
import net.oauth.OAuthProblemException;
import net.oauth.http.HttpMessage;

/**
 * {@link OAuth} utility methods.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OAuthUtil {

  /**
   * Returns a {@link OAuthProblemException} with the given problem and the
   * correct status code. The problem should come from {@link OAuth.Problems}.
   */
  public static OAuthProblemException newOAuthProblemException(String problem) {
    OAuthProblemException exception = new OAuthProblemException(problem);
    exception.setParameter(HttpMessage.STATUS_CODE, OAuth.Problems.TO_HTTP_CODE.get(problem));
    return exception;
  }
}
