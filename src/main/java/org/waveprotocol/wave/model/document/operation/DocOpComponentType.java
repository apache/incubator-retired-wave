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
 * A superset of the DocInitializationComponentType enum that adds
 * update and deletion components.
 *
 *
 */
public class DocOpComponentType {

  public static final DocInitializationComponentType ANNOTATION_BOUNDARY =
      new DocInitializationComponentType("annotation boundary");
  public static final DocInitializationComponentType CHARACTERS =
      new DocInitializationComponentType("characters");
  public static final DocInitializationComponentType ELEMENT_START =
      new DocInitializationComponentType("element start");
  public static final DocInitializationComponentType ELEMENT_END =
      new DocInitializationComponentType("element end");

  public static final DocOpComponentType RETAIN =
      new DocOpComponentType("retain");
  public static final DocOpComponentType DELETE_CHARACTERS =
      new DocOpComponentType("delete characters");
  public static final DocOpComponentType DELETE_ELEMENT_START =
      new DocOpComponentType("delete element start");
  public static final DocOpComponentType DELETE_ELEMENT_END =
      new DocOpComponentType("delete element end");
  public static final DocOpComponentType REPLACE_ATTRIBUTES =
      new DocOpComponentType("replace attributes");
  public static final DocOpComponentType UPDATE_ATTRIBUTES =
      new DocOpComponentType("update attributes");

  // TODO: add ordinals
  protected final String name;

  // package-visible for DocInitializationComponentType
  DocOpComponentType(String name) {
    this.name = name;
  }

  @Override
  public final String toString() {
    return name;
  }

}
