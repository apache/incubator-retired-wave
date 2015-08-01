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

package org.waveprotocol.wave.client.doodad.form;

import org.waveprotocol.wave.client.doodad.form.button.Button;
import org.waveprotocol.wave.client.doodad.form.check.CheckBox;
import org.waveprotocol.wave.client.doodad.form.check.Label;
import org.waveprotocol.wave.client.doodad.form.check.RadioButton;
import org.waveprotocol.wave.client.doodad.form.check.RadioGroup;
import org.waveprotocol.wave.client.doodad.form.events.ContentEvents;
import org.waveprotocol.wave.client.doodad.form.input.Input;
import org.waveprotocol.wave.client.doodad.form.input.Password;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;

/**
 * Common static stuff for form doodads incl. a method to register them all at
 * once.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class FormDoodads {

  private FormDoodads() {
  }

  /**
   * Register all form doodads on the given registry
   * @param registry
   */
  public static void register(ElementHandlerRegistry registry) {
    ContentEvents.register(registry);
    Button.register(registry);
    Input.register(registry);
    Password.register(registry);
    Label.register(registry);
    RadioButton.register(registry);
    RadioGroup.register(registry);
    CheckBox.register(registry);
  }
}
