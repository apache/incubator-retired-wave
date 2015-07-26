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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Arrays;
import java.util.EnumMap;

/**
 * DOM encodings for view types.
 *
 */
public final class TypeCodes {

  private final static EnumMap<Type, String> CODES = new EnumMap<Type, String>(Type.class);
  private final static StringMap<Type> TYPES = CollectionUtils.createStringMap();

  static {
    //
    // These type codes could really be any string that is unique within the
    // space of codes, but in order to retain some pretense of debugability, the
    // codes are letters that closely match the names of the view types.
    //
    // To avoid the need for quotation, the alphabet for these codes is is
    // restricted by the alphabets of:
    // 1. JSO property names,
    // 2. attribute value strings, and
    // 3. Uniqueness of characters across supported browsers (e.g.,
    // ...uppercase/lowercase distinguishability).
    // To keep this property, the alphabet for code values is restricted to
    // lower case alphanumeric characters only.
    //
    TYPES.put("b", Type.BLIP);
    TYPES.put("m", Type.META);
    TYPES.put("d", Type.ANCHOR);
    TYPES.put("t", Type.INLINE_THREAD);
    TYPES.put("ci", Type.CONTINUATION_INDICATOR);
    TYPES.put("r", Type.ROOT_THREAD);
    TYPES.put("rb", Type.REPLY_BOX);
    TYPES.put("n", Type.INLINE_CONVERSATION);
    TYPES.put("c", Type.ROOT_CONVERSATION);
    TYPES.put("w", Type.WAVE);
    TYPES.put("g", Type.TOGGLE);
    TYPES.put("s", Type.PARTICIPANTS);
    TYPES.put("p", Type.PARTICIPANT);
    TYPES.put("i", Type.MENU_ITEM);
    TYPES.put("a", Type.ADD_PARTICIPANT);
    TYPES.put("npw", Type.NEW_WAVE_WITH_PARTICIPANTS);

    TYPES.each(new ProcV<Type>() {
      @Override
      public void apply(String code, Type type) {
        CODES.put(type, code);
      }
    });

    assert TYPES.countEntries() == CODES.size() : "not all codes are unique";
    assert CODES.keySet().containsAll(Arrays.asList(Type.values())) : "not all types are coded";
  }

  // Utility class.
  private TypeCodes() {
  }

  public static String kind(Type view) {
    return CODES.get(view);
  }

  public static Type type(String code) {
    return TYPES.get(code);
  }
}
