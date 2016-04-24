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

package org.waveprotocol.wave.model.document.operation.impl;

import java.util.List;
import java.util.Map;

import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap;

public class AnnotationMapImpl
    extends ImmutableStateMap<AnnotationMapImpl, AnnotationsUpdate>
    implements AnnotationMap {

  /**
   * An empty T map.
   */
  public static final AnnotationMapImpl EMPTY_MAP = new AnnotationMapImpl();

  public AnnotationMapImpl() {
    super();
  }
  public AnnotationMapImpl(Map<String, String> map) {
    super(map);
  }

  AnnotationMapImpl(List<Attribute> attributes) {
    super(attributes);
  }

  @Override
  protected AnnotationMapImpl createFromList(List<Attribute> attributes) {
    return new AnnotationMapImpl(attributes);
  }

}
