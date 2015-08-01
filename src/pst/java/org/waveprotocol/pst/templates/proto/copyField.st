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

$if (f.repeated)$

  clear$f.capName$();
  $if (f.message)$
    for ($f.messageType$ field : $target$.$f.getter$()) {
      add$f.capName$(new $f.javaType$(field));
    }
  $else$
    for ($f.javaType$ field : $target$.$f.getter$()) {
      add$f.capName$(field);
    }
  $endif$

$else$

  $if (f.optional)$
  if ($target$.has$f.capName$()) {
  $endif$
    $if (f.message)$
      $f.setter$(new $f.javaType$($target$.$f.getter$()));
    $else$
      $f.setter$($target$.$f.getter$());
    $endif$
  $if (f.optional)$
  } else {
    clear$f.capName$();
  }
  $endif$

$endif$
