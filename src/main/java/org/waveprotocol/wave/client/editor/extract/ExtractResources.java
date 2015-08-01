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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ClientBundle;

/**
 * Resources for things in this package
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ExtractResources extends ClientBundle {
  /** Interface defining CSS stylenames */
  interface Css extends CssResource {
    /** Something which has just been repaired */
    String repaired();
    /** Something which is marked as a problem */
    String problem();
    /** Something that cannot be repaired */
    String dead();
  }

  /** CSS */
  @Source("Extract.css")
  public Css css();
}
