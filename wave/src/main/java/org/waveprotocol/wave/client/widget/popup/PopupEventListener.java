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

package org.waveprotocol.wave.client.widget.popup;

/**
 * Interface to be implemented by classes wanting notification of popup events.
 *
 */
public interface PopupEventListener {
  /** Called after the popup becomes visible */
  void onShow(PopupEventSourcer source);

  /** Called after the popup becomes invisible */
  void onHide(PopupEventSourcer source);

  /**
   * Convenience blank implementation to be used for subclassing.
   */
  public static class PopupEventListenerAdapter implements PopupEventListener {
    @Override
    public void onHide(PopupEventSourcer source) {
    }

    @Override
    public void onShow(PopupEventSourcer source) {
    }
  }
}
