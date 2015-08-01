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

package org.waveprotocol.wave.client.common.util;


public interface WaveRefConstants {

  public static final String WAVE_SCHEME = "wave";

  /** The temporary format for wavelinks */
  public static final String WAVE_URI_PREFIX = WAVE_SCHEME + "://";


  @Deprecated
  public static final String WAVE_SCHEME_OLD = "waveid";

  /**
   * The temporary format for wavelinks
   * @deprecated  Use WAVE_URI_PREFIX.
   */
  @Deprecated
  public static final String WAVE_URI_PREFIX_OLD = WAVE_SCHEME_OLD + "://";

  /** The tag in a permalink */
  public static final String PERMALINK_WAVEREF_TAG = "waveref/";
}
