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

  @Override
  public boolean has$f.capName$() {
    return $f.name$ != null;
  }

  @Override
  public void clear$f.capName$() {
    $f.name$ = null;
  }

$endif$

$if (f.repeated)$

  @Override
  public List<$f.boxedJavaType$> $f.getter$() {
    return Collections.unmodifiableList($f.name$);
  }

  $if (f.message)$
    @Override
    public void addAll$f.capName$(List<? extends $f.messageType$> messages) {
      for ($f.messageType$ message : messages) {
      	add$f.capName$(message);
      }
    }
  $else$
    @Override
    public void addAll$f.capName$(List<$f.boxedJavaType$> values) {
      this.$f.name$.addAll(values);
    }
  $endif$

  @Override
  public $f.javaType$ $f.getter$(int n) {
    $if (f.message)$
      return new $f.javaType$($f.name$.get(n));
    $else$
      return $f.name$.get(n);
    $endif$
  }

  $if (f.message)$
    @Override
    public void $f.setter$(int n, $f.messageType$ message) {
      this.$f.name$.set(n, new $f.javaType$(message));
    }
  $else$
    @Override
    public void $f.setter$(int n, $f.javaType$ value) {
      this.$f.name$.set(n, value);
    }
  $endif$

  @Override
  public int $f.getter$Size() {
    return $f.name$.size();
  }

  $if (f.message)$
    @Override
    public void add$f.capName$($f.messageType$ message) {
      this.$f.name$.add(new $f.javaType$(message));
    }
  $else$
    @Override
    public void add$f.capName$($f.javaType$ value) {
      this.$f.name$.add(value);
    }
  $endif$

  @Override
  public void clear$f.capName$() {
    $f.name$.clear();
  }

$else$

  @Override
  public $f.javaType$ $f.getter$() {
    $if (f.message)$
      return new $f.javaType$($f.name$);
    $else$
      return $f.name$;
    $endif$
  }

  $if (f.message)$
    @Override
    public void $f.setter$($f.messageType$ message) {
      this.$f.name$ = new $f.javaType$(message);
    }
  $else$
    @Override
    public void $f.setter$($f.javaType$ value) {
      this.$f.name$ = value;
    }
  $endif$

$endif$
