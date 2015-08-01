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
    return hasProperty(this, key$f.capName$);
  }

  @Override
  public void clear$f.capName$() {
    if (hasProperty(this, key$f.capName$)) {
      deleteProperty(this, key$f.capName$);
    }
  }

$endif$

$if (f.repeated)$

  @Override
  public List<$f.boxedJavaType$> $f.getter$() {
    initArray(this, key$f.capName$);
    List<$f.boxedJavaType$> list = new ArrayList<$f.boxedJavaType$>();
    for (int i = 0; i < $f.getter$Size(); i++) {
      list.add($f.getter$(i));
    }
    return list;
  }

  $if (f.message)$
    @Override
    public void addAll$f.capName$(List<? extends $f.messageType$> models) {
      for ($f.messageType$ model : models) {
      	add$f.capName$(model);
      }
    }
  $else$
    @Override
    public void addAll$f.capName$(List<$f.boxedJavaType$> values) {
      for ($f.javaType$ value : values) {
        add$f.capName$(value);
      }
    }
  $endif$

  @Override
  @SuppressWarnings("unchecked")
  public $f.javaType$ $f.getter$(int n) {
    initArray(this, key$f.capName$);
    $if (f.message)$
      JsArray<$f.javaType$> array = getPropertyAsObject(this, key$f.capName$).cast();
    $elseif (f.enum)$
      JsArrayInteger array = getPropertyAsObject(this, key$f.capName$).cast();
    $elseif (f.blob)$
      JsArrayString array = getPropertyAsObject(this, key$f.capName$).cast();
    $else$
      JsArray$f.boxedJavaType$ array = getPropertyAsObject(this, key$f.capName$).cast();
    $endif$
    if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
    if (array.length() <= n) throw new IllegalArgumentException("index " + n + ">= array length " + array.length());
    $if (f.enum)$
      return ProtoEnums.valOf(array.get(n), $f.javaType$.values());
    $elseif (f.blob)$
      return new Blob(array.get(n));
    $else$
      return array.get(n);
    $endif$
  }

  $if (f.message)$
    @Override
    @SuppressWarnings("unchecked")
    public void $f.setter$(int n, $f.messageType$ model) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArray<$f.javaType$>) getPropertyAsObject(this, key$f.capName$)).set(n, ($f.javaType$) model);
    }
  $elseif (f.enum)$
    @Override
    public void $f.setter$(int n, $f.javaType$ enumValue) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArrayInteger) getPropertyAsObject(this, key$f.capName$)).set(n, enumValue.getValue());
    }
  $elseif (f.blob)$
    @Override
    public void $f.setter$(int n, $f.javaType$ blob) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArrayString) getPropertyAsObject(this, key$f.capName$)).set(n, blob.getData());
    }
  $else$
    @Override
    public void $f.setter$(int n, $f.javaType$ value) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArray$f.boxedJavaType$) getPropertyAsObject(this, key$f.capName$)).set(n, value);
    }
  $endif$

  @Override
  public int $f.getter$Size() {
    return hasProperty(this, key$f.capName$) ? ((JsArray<?>) getPropertyAsObject(this, key$f.capName$)).length() : 0;
  }

  $if (f.message)$
    @Override
    @SuppressWarnings("unchecked")
    public void add$f.capName$($f.messageType$ model) {
      initArray(this, key$f.capName$);
      ((JsArray<$f.javaType$>) getPropertyAsObject(this, key$f.capName$)).push(($f.javaType$) model);
    }
  $elseif (f.enum)$
    @Override
    public void add$f.capName$($f.javaType$ enumValue) {
      initArray(this, key$f.capName$);
      ((JsArrayInteger) getPropertyAsObject(this, key$f.capName$)).push(enumValue.getValue());
    }
  $elseif (f.blob)$
    @Override
    public void add$f.capName$($f.javaType$ blob) {
      initArray(this, key$f.capName$);
      ((JsArrayString) getPropertyAsObject(this, key$f.capName$)).push(blob.getData());
    }
  $else$
    @Override
    public void add$f.capName$($f.javaType$ value) {
      initArray(this, key$f.capName$);
      ((JsArray$f.boxedJavaType$) getPropertyAsObject(this, key$f.capName$)).push(value);
    }
  $endif$

  @Override
  public void clear$f.capName$() {
    clearArray(this, key$f.capName$);
  }

$else$

  @Override
  $if (f.message)$
    public $f.messageType$ $f.getter$() {
      return hasProperty(this, key$f.capName$) ? (($f.javaType$) getPropertyAsObject(this, key$f.capName$)) : null;
    }
  $elseif (f.enum)$
    public $f.javaType$ $f.getter$() {
      return hasProperty(this, key$f.capName$) ? ProtoEnums.valOf(getPropertyAsInteger(this, key$f.capName$), $f.javaType$.values()) : null;
    }
  $elseif (f.blob)$
    public $f.javaType$ $f.getter$() {
      return hasProperty(this, key$f.capName$) ? new Blob(getPropertyAsString(this, key$f.capName$)) : null;
    }
  $else$
    public $f.javaType$ $f.getter$() {
      return hasProperty(this, key$f.capName$) ? getPropertyAs$f.boxedJavaType$(this, key$f.capName$) : $f.defaultValue$;
    }
  $endif$

  $if (f.message)$
    @Override
    public void $f.setter$($f.messageType$ model) {
      setPropertyAsObject(this, key$f.capName$, ($f.javaType$) model);
    }
  $elseif (f.enum)$
    @Override
    public void $f.setter$($f.javaType$ enumValue) {
      setPropertyAsInteger(this, key$f.capName$, enumValue.getValue());
    }
  $elseif (f.blob)$
    @Override
    public void $f.setter$($f.javaType$ blob) {
      setPropertyAsString(this, key$f.capName$, blob.getData());
    }
  $else$
    @Override
    public void $f.setter$($f.javaType$ value) {
      setPropertyAs$f.boxedJavaType$(this, key$f.capName$, value);
    }
  $endif$

$endif$
