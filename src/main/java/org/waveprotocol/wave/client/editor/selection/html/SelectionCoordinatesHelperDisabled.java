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
package org.waveprotocol.wave.client.editor.selection.html;

import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.model.util.IntRange;

/**
 * Selection X/Y Coordinate calculator for to use when obtaining offsets is disabled.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class SelectionCoordinatesHelperDisabled implements SelectionCoordinatesHelper {
  @Override
  public OffsetPosition getNearestElementPosition() {
    // unimplemented
    return null;
  }

  @Override
  public OffsetPosition getFocusPosition() {
    // unimplemented
    return null;
  }

  @Override
  public OffsetPosition getAnchorPosition() {
    // unimplemented
    return null;
  }

  @Override
  public IntRange getFocusBounds() {
    // unimplemented
    return null;
  }

}
