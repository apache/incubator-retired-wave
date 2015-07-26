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

/**
 * A value object for an RGB triple.
 *
 * @author hearnden@google.com (David Hearnden)
 */
final class RgbColor {
  static final RgbColor BLACK = new RgbColor(0, 0, 0);
  static final RgbColor WHITE = new RgbColor(255, 255, 255);

  final int red;
  final int green;
  final int blue;

  RgbColor(int red, int green, int blue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  /** @return the CSS color expression for this color. */
  public String getCssColor() {
    return "rgb(" + red + "," + green + "," + blue + ")";
  }

  @Override
  public int hashCode() {
    // Bitshifting is significantly faster on both Chrome and Firefox.
    return (red << 16) | (green << 8) | blue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof RgbColor)) {
      return false;
    } else {
      RgbColor color = (RgbColor) o;
      return red == color.red && green == color.green && blue == color.blue;
    }
  }
}
