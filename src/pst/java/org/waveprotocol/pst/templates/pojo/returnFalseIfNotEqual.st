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

  if (has$f.capName$() != $target$.has$f.capName$()) return false;
  $if (f.primitive)$
    if (has$f.capName$() && ($f.getter$() != $target$.$f.getter$())) return false;
  $else$
    if (has$f.capName$() && !$f.getter$().equals($target$.$f.getter$())) return false;
  $endif$

$elseif (f.repeated)$

  if (!$f.getter$().equals($target$.$f.getter$())) return false;

$else$

  $if (f.primitive)$
    if ($f.getter$() != $target$.$f.getter$()) return false;
  $else$
    if (!$f.getter$().equals($target$.$f.getter$())) return false;
  $endif$

$endif$
