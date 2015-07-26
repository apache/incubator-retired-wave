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

package org.waveprotocol.wave.client.doodad.selection;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * Resources for use by the UserName doodad renderer
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface CaretMarkerResources extends ClientBundle {
  /** Css */
  interface Css extends CssResource {
    /** inner span for our rounded user label trick */
    String inner();
    /** outer span */
    String outer();
    /** container for IME composition state */
    String compositionState();
  }

  /** Css resource */
  @Source("CaretMarker.css")
  public Css css();
}
