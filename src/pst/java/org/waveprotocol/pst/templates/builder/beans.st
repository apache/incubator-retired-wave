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

$if (f.optional)$

  public $m.javaType$ clear$f.capName$() {
    $f.name$ = null;
    return this;
  }

$endif$

$if (f.repeated)$

  $if (f.message)$
    public $m.javaType$ addAll$f.capName$(List<? extends $f.messageType$> messages) {
      for ($f.messageType$ message : messages) {
      	add$f.capName$(message);
      }
      return this;
    }
  $else$
    public $m.javaType$ addAll$f.capName$(List<$f.boxedJavaType$> values) {
      this.$f.name$.addAll(values);
      return this;
    }
  $endif$

  $if (f.message)$
    public $m.javaType$ $f.setter$(int n, $f.messageType$ message) {
      this.$f.name$.set(n, message);
      return this;
    }
  $else$
    public $m.javaType$ $f.setter$(int n, $f.javaType$ value) {
      this.$f.name$.set(n, value);
      return this;
    }
  $endif$

  $if (f.message)$
    public $m.javaType$ add$f.capName$($f.messageType$ message) {
      this.$f.name$.add(message);
      return this;
    }
  $else$
    public $m.javaType$ add$f.capName$($f.javaType$ value) {
      this.$f.name$.add(value);
      return this;
    }
  $endif$

  public $m.javaType$ clear$f.capName$() {
    $f.name$.clear();
    return this;
  }

$else$

  $if (f.message)$
    public $m.javaType$ $f.setter$($f.messageType$ message) {
      this.$f.name$ = message;
      return this;
    }
  $else$
    public $m.javaType$ $f.setter$($f.javaType$ value) {
      this.$f.name$ = value;
      return this;
    }
  $endif$

$endif$
