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


package org.waveprotocol.wave.client.scroll;


/**
 * Defines UI scrolling actions on a scroll panel.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class ScrollController {
  private final ScrollPanel<?> scroller;

  /**
   * Creates a scroll controller.
   */
  ScrollController(ScrollPanel<?> scroller) {
    this.scroller = scroller;
  }

  void pageUp() {
    Extent viewport = scroller.getViewport();
    double target = Math.max(0, viewport.getStart() - viewport.getSize());
    scroller.moveTo(target);
  }

  void pageDown() {
    Extent viewport = scroller.getViewport();
    double target = Math.min(scroller.getContent().getEnd(), viewport.getEnd());
    scroller.moveTo(target);
  }

  void home() {
    scroller.moveTo(0);
  }

  void end() {
    Extent content = scroller.getContent();
    Extent viewport = scroller.getViewport();
    scroller.moveTo(content.getEnd() - viewport.getSize());
  }
}
