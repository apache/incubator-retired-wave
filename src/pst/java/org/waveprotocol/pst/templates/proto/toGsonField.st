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
if (has$f.capName$()) {
$endif$

  $if (f.repeated)$
  {
    JsonArray array = new JsonArray();
    for (int i = 0; i < $f.getter$Size(); i++) {
      $if (f.message)$
        JsonElement elem = ((GsonSerializable) $f.getter$(i)).toGson(raw, gson);
        // NOTE(kalman): if multistage parsing worked, split point would go here.
        array.add(elem);
      $elseif (f.enum)$
        array.add(new JsonPrimitive($f.getter$(i).getValue()));
      $elseif (f.blob)$
        array.add(new JsonPrimitive($f.getter$(i).getData()));
      $elseif (f.long)$
        array.add(GsonUtil.toJson($f.getter$(i)));
      $else$
        array.add(new JsonPrimitive($f.getter$(i)));
      $endif$
    }
    $target$.add("$f.number$", array);
  }
  $else$
    $if (f.message)$
    {
      JsonElement elem = ((GsonSerializable) $f.getter$()).toGson(raw, gson);
      $target$.add("$f.number$", elem);
    }
    $elseif (f.enum)$
      $target$.add("$f.number$", new JsonPrimitive($f.getter$().getValue()));
    $elseif (f.blob)$
      $target$.add("$f.number$", new JsonPrimitive($f.getter$().getData()));
    $elseif (f.long)$
      $target$.add("$f.number$", GsonUtil.toJson($f.getter$()));
    $else$
      $target$.add("$f.number$", new JsonPrimitive($f.getter$()));
    $endif$
  $endif$

$if (f.optional)$
}
$endif$
