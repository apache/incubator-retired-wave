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

/** We have to use fully-qualified name of the GsonSerializable class here in order to make it
 * visible in case of nested classes.
 */
public $if(m.inner)$static$endif$ final class $m.javaType$ extends org.waveprotocol.wave.communication.gwt.JsonMessage
    implements $m.name$ {

  $m.nestedMessages: {nested|$class(m=nested)$}$

  $m.fields: {f|
    private static final String key$f.capName$ = "$f.number$";
  }$

  protected $m.javaType$() {
  }

  public static $m.javaType$ create() {
    $m.javaType$ instance = ($m.javaType$) JsonMessage.createJsonMessage();
    
    // Force all lists to start with an empty list rather than no property for
    // the list. This is so that the native JS equality works, since (obviously)
    // {} != {"foo": []} while in the context of messages they should be.
    $m.fields: {f|
      $if (f.repeated)$
        instance.clear$f.capName$();
      $endif$
    }$
    
    return instance;
  }

  @Override
  public void copyFrom($m.name$ message) {
    super.copyFrom(($m.javaType$) message);
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  @Override
  public boolean isEqualTo(Object o) {
    if (o instanceof $m.javaType$) {
      return nativeIsEqualTo(o);
    } else if (o instanceof $m.name$) {
      return $m.name$Util.isEqual(this, ($m.name$) o);
    } else {
      return false;
    }
  }
}
