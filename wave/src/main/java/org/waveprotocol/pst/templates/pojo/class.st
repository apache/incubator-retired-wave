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

public $if(m.inner)$static$endif$ class $m.javaType$ implements $m.name$ {

  $m.nestedMessages: {nested|$class(m=nested)$}$

  $m.fields: {f|$fieldDeclaration(f=f)$}$

  public $m.javaType$() {
  }

  public $m.javaType$($m.name$ message) {
    copyFrom(message);
  }

  @Override
  public void copyFrom($m.name$ message) {
    $m.fields: {f|$copyField(f=f, target="message")$}$
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  /** Provided to subclasses to clear all fields, for example when deserializing. */
  protected void reset() {
    $m.fields: {f|$clearField(f=f)$}$
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof $m.javaType$) && isEqualTo(o);
  }

  @Override
  public boolean isEqualTo(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof $m.name$) {
      return $m.name$Util.isEqual(this, ($m.name$) o);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return $m.name$Util.getHashCode(this);
  }  
}
