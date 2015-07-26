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

package org.waveprotocol.wave.client.widget.common;

interface ButtonStyle {

  /**
   * Used on all {@link Button} widgets
   */
  String button = "button";

  /**
   * Button that has a text label, rather than an image.
   */
  String labeledButton = "lb";

  /**
   * Element's background should come from x-repeat sprite
   */
  String xRepeat = "xr";

  /**
   * Element's background should come from y-repeat sprite
   */
  String yRepeat = "yr";

  /**
   * Mouse is over an element
   */
  String mouseOver = "mouse";

  /**
   * Mouse is not over an element
   */
  String mouseOut = "cat";

  /**
   * Mouse is down on an element
   */
  String mouseDown = "down";

  /**
   * Mouse is up over an element
   */
  String mouseUp = "up";

  /**
   * Widget is enabled
   */
  String enabled = "enabled";

  /**
   * Widget is disabled
   */
  String disabled = "disabled";

  /**
   * Widget is on
   */
  String on = "on";

  /**
   * Widget is off
   */
  String off = "off";

  /**
   * Left-most element in frame or other composite
   */
  String left = "l";

  /**
   * Center element in frame or other composite
   */
  String center = "c";

  /**
   * Right-most element in frame or other composite
   */
  String right = "r";
}
