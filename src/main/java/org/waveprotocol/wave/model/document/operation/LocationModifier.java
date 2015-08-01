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

package org.waveprotocol.wave.model.document.operation;


/**
 * Do not rely on this. This is just a temporary location modifier being used
 * for transitional purposes. The proper way will be to use annotations.
 *
 */
public class LocationModifier implements DocOpCursor {

  private int mutatingLocation;
  private int scanPoint = 0;

  /**
   * @param location The location to transform
   */
  public LocationModifier(int location) {
    this.mutatingLocation = location;
  }

  @Override
  public void retain(int itemCount) {
    scanPoint += itemCount;
  }

  @Override
  public void characters(String characters) {
    insert(characters.length());
  }

  @Override
  public void elementStart(String type, Attributes attributes) {
    insert(1);
  }

  @Override
  public void elementEnd() {
    insert(1);
  }

  @Override
  public void deleteCharacters(String chars) {
    delete(chars.length());
  }

  @Override
  public void deleteElementStart(String type, Attributes attributes) {
    delete(1);
  }

  @Override
  public void deleteElementEnd() {
    delete(1);
  }

  private void insert(int size) {
    if (scanPoint < mutatingLocation) {
      mutatingLocation += size;
      scanPoint += size;
    }
  }

  private void delete(int size) {
    if (scanPoint <= mutatingLocation) {
      mutatingLocation = Math.max(scanPoint, mutatingLocation - size);
    }
  }

  @Override
  public void replaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
    retain(1);
  }

  @Override
  public void updateAttributes(AttributesUpdate attributesUpdate) {
    retain(1);
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap m) {}

  /**
   * @param op
   * @param location
   * @return location transformed against mutation
   */
  public static int transformLocation(DocOp op, int location) {
    LocationModifier modifier = new LocationModifier(location);
    op.apply(modifier);
    return modifier.mutatingLocation;
  }

}
