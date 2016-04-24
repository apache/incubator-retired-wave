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

package org.waveprotocol.box.server.waveserver;

import java.io.IOException;

/**
 * Wraps an {@link IOException} in a {@link RuntimeException}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
public class RuntimeIOException extends RuntimeException {
  private final IOException cause;

  public RuntimeIOException(IOException cause) {
    super(cause);
    this.cause = cause;
  }

  public IOException getIOException() {
    return cause;
  }
}
