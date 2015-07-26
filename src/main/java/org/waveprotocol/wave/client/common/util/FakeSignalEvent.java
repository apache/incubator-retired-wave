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

import com.google.gwt.dom.client.Element;

import java.util.EnumSet;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public class FakeSignalEvent extends SignalEventImpl {

  public static SignalEventFactory<FakeSignalEvent> FACTORY =
    new SignalEventFactory<FakeSignalEvent>() {
      @Override public FakeSignalEvent create() {
        return new FakeSignalEvent();
      }
    };

  private static class FakeNativeEvent implements NativeEvent {
    private final boolean altKey, ctrlKey, metaKey, shiftKey;
    private final String type;
    private final int mouseButton;

    boolean defaultPrevented = false;
    boolean propagationStopped = false;

    public FakeNativeEvent(String type,
        int mouseButton, EnumSet<KeyModifier> modifiers) {
      this.type = type;
      this.mouseButton = mouseButton;
      this.altKey = modifiers != null && modifiers.contains(KeyModifier.ALT);
      this.ctrlKey = modifiers != null && modifiers.contains(KeyModifier.CTRL);
      this.metaKey = modifiers != null && modifiers.contains(KeyModifier.META);
      this.shiftKey = modifiers != null && modifiers.contains(KeyModifier.SHIFT);
    }

    @Override
    public boolean getAltKey() {
      return altKey;
    }

    @Override
    public int getButton() {
      return mouseButton;
    }

    @Override
    public boolean getCtrlKey() {
      return ctrlKey;
    }

    @Override
    public boolean getMetaKey() {
      return metaKey;
    }

    @Override
    public boolean getShiftKey() {
      return shiftKey;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public void preventDefault() {
      defaultPrevented = true;
    }

    @Override
    public void stopPropagation() {
      propagationStopped = true;
    }
  }

  public static FakeSignalEvent createKeyPress(
      KeySignalType type, int keyCode, EnumSet<KeyModifier> modifiers) {
    return createKeyPress(FACTORY, type, keyCode, modifiers);
  }

  public static <T extends FakeSignalEvent> T createEvent(
      SignalEventFactory<T> factory, String type) {
    return createInner(factory.create(), new FakeNativeEvent(type, 0, null), null);
  }

  public static <T extends FakeSignalEvent> T createKeyPress(SignalEventFactory<T> factory,
      KeySignalType type, int keyCode, EnumSet<KeyModifier> modifiers) {
    SignalKeyLogic.Result keyLogic = new SignalKeyLogic.Result();
    keyLogic.keyCode = keyCode;
    keyLogic.type = type;
    return createInner(factory.create(), new FakeNativeEvent("keydown", 0, modifiers), keyLogic);
  }

  public static <T extends FakeSignalEvent> T createClick(SignalEventFactory<T> factory,
      EnumSet<KeyModifier> modifiers) {
    return createInner(factory.create(), new FakeNativeEvent("click", 0, modifiers), null);
  }

  public boolean defaultPrevented() {
    return ((FakeNativeEvent) nativeEvent).defaultPrevented;
  }

  public boolean propagationStopped() {
    return ((FakeNativeEvent) nativeEvent).propagationStopped;
  }

  @Override public Element getTarget() {
    return null;
  }
}
