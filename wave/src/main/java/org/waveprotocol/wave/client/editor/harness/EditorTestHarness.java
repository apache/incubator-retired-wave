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

package org.waveprotocol.wave.client.editor.harness;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;

// TODO(danilatos/schuck): Delete this and use DefaultHarness directly
// (just need to update all the webdriver tests, go links, etc).
public class EditorTestHarness implements EntryPoint {
  /**
   * {@inheritDoc}
   */
  public void onModuleLoad() {
    while (true) {
      try {
        new DefaultTestHarness().onModuleLoad();
        break;
      } catch (RuntimeException e) {
        if (!Window.confirm("Exception on module load, try again?")) {
          throw e;
        }
      } catch (Error e) {
        if (!Window.confirm("Error on module load, try again?")) {
          throw e;
        }
      }
    }
  }
}
