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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.LocationModifier;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Couple of static helper methods for {@link Range}
 *
 */
public class RangeHelper {

  /**
   * @param range
   * @param modifier
   * @return (copy of) range transformed against modifier assuming
   *    range contains selection boundaries
   */
  public static FocusedRange applyModifier(FocusedRange range, DocOp modifier) {
    return applyModifier(modifier, range);
  }

  /**
   * @param range
   * @param modifier
   * @return (copy of) range transformed against modifier
   */
  public static FocusedRange applyModifier(DocOp modifier, FocusedRange range) {
    int anchor = LocationModifier.transformLocation(modifier, range.getAnchor());
    int focus  = LocationModifier.transformLocation(modifier, range.getFocus());
    return new FocusedRange(anchor, focus);
  }

}
