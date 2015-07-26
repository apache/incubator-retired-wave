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

package org.waveprotocol.wave.client.editor;

import org.waveprotocol.wave.model.document.indexed.AnnotationSetListener;
import org.waveprotocol.wave.model.document.indexed.AnnotationTree;

/**
 * A specialization of AnnotationTree for the Editor.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class EditorAnnotationTree extends AnnotationTree<Object> {

  protected static final Object ONE_OBJECT = new Object();
  protected static final Object ANOTHER_OBJECT = new Object();

  public EditorAnnotationTree(AnnotationSetListener<Object> listener) {
    super(ONE_OBJECT, ANOTHER_OBJECT, listener);
  }
}
