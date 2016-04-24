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

public $if(m.inner)$static$endif$ final class $m.javaType$ {

  /** Factory to pass to {@link #build()}. */
  public interface Factory {
    $m.name$ create();
  }

  $m.nestedMessages: {nested|$class(m=nested)$}$

  $m.fields: {f|$fieldDeclaration(f=f)$}$

  public $m.javaType$() {
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  /** Builds a {@link $m.name$} using this builder and a factory. */
  public $m.name$ build(Factory factory) {
    $m.name$ message = factory.create();
    $m.fields: {f|$buildField(f=f, target="message")$}$
    return message;
  }
}
