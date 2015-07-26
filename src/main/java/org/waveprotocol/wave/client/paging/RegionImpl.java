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


package org.waveprotocol.wave.client.paging;

/**
 * Region implementation.
 *
 */
public final class RegionImpl implements Region {
  private double start;
  private double end;

  private RegionImpl(double start, double end) {
    set(start, end);
  }

  public static RegionImpl at(double start, double end) {
    return new RegionImpl(start, end);
  }

  public static RegionImpl at(Region content) {
    return new RegionImpl(content.getStart(), content.getEnd());
  }

  @Override
  public double getStart() {
    return start;
  }

  @Override
  public double getEnd() {
    return end;
  }

  public double getSize() {
    return end - start;
  }

  public void moveStart(double distance) {
    start += distance;
  }

  public void moveEnd(double distance) {
    end += distance;
  }

  public RegionImpl moveBy(double distance) {
    moveStart(distance);
    moveEnd(distance);
    return this;
  }

  public RegionImpl set(double start, double end) {
    if (end < start) {
      throw new IllegalArgumentException("start: " + start + ", end: " + end);
    }

    this.start = start;
    this.end = end;
    return this;
  }

  public RegionImpl scale(double scale) {
    double oldHeight = end - start;
    double mid = (start + end) / 2;
    double newHeight = oldHeight * scale;

    this.start = mid - newHeight / 2;
    this.end = mid + newHeight / 2;
    return this;
  }

  public RegionImpl set(Region region) {
    return set(region.getStart(), region.getEnd());
  }

  @Override
  public String toString() {
    return "{start: " + start + "; end: " + end + "}";
  }
}
