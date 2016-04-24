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

package org.waveprotocol.wave.client.editor.sugg;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;

/**
 * Resources for suggestion manager
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface SuggestionResources extends ClientBundle {
  /** Interface defining CSS stylenames */
  public interface Css extends CssResource {
    /** style for programmatically simulating 'hovered' suggestions */
    String hover();
    /** style for suggestions in the popup */
    String sugg();
    /** style for the original value in the popup */
    String orig();
    /** style for the separator */
    String sep();
    /** style for commands */
    String cmd();
    /** style for spelly when selected */
    String selected();
  }

  /** CSS */
  @Source("Suggestion.css")
  public Css css();

  @Source("icon_undo.png")
  DataResource undoIcon();

  @Source("icon_undo_hover.png")
  DataResource hoverUndoIcon();
}
