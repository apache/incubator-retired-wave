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

package org.waveprotocol.wave.client.gadget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

/**
 * Overlay and JSON-converter class to hold key-value pairs. Works as
 * String-to-String map. The map may also contain null values in delta maps to
 * indicate keys to be deleted. The implementation makes sure that only
 * string and null values are present at any time. The keys are internally
 * prepended with ":" to avoid overriding JS object properties.
 * TODO(user): Some elements are borrowed from the StringMap class. Consider
 * unifying the JS map classes.
 *
 */
public class StateMap extends JavaScriptObject {
  /**
   * A procedure that accepts a key and the corresponding value from the map,
   * does something with them, and returns a boolean condition.
   *
   * @see StateMap#checkKeyValue(StateMap.CheckKeyValue) Implement this class to
   *      iterate over key-value pairs until either all key-value pairs are
   *      observed or the returned condition is false using the checkKeyValue
   *      method.
   */
  public static interface CheckKeyValue {
    /** The function */
    public boolean check(String key, String value);
  }

  /**
   * A procedure that accepts a key and the corresponding value from the map and
   * does something with them, but does not require to return a boolean
   * condition.
   *
   * Implement this interface to iterate over all key-value pairs using each
   * method.
   */
  public static interface Each  {
    /** The procedure */
    public void apply(String key, String value);
  }

  /**
   * Helper class that implements CheckKeyValue to compare received key-value
   * pairs against key-values in a given StateMap.
   */
  public static class KeyValueComparator implements CheckKeyValue {
    private final StateMap otherMap;

    /**
     * Constructs the comparator for a given map.
     *
     * @param otherMap the map to compare key-values against.
     */
    public KeyValueComparator(StateMap otherMap) {
      this.otherMap = otherMap;
    }

    @Override
    public boolean check(String key, String value) {
      return (value == null) ?
          otherMap.has(key) && (otherMap.get(key) == null) : value.equals(otherMap.get(key));
    }
  }

  /**
   * External construction is banned.
   */
  protected StateMap() {
  }

  /**
   * Creates gadget state object.
   */
  public static StateMap create() {
    return JavaScriptObject.createObject().cast();
  }

  /**
   * Creates gadget state object from a String-to-String map.
   */
  public static StateMap createFromStringMap(ReadableStringMap<String> map) {
    final StateMap stateMap = JavaScriptObject.createObject().cast();
    map.each(new ProcV<String>() {
      @Override
      public void apply(String key, String value) {
        stateMap.put(key, value);
      }
    });
    return stateMap;
  }

  /**
   * Checks whether the key is in the map.
   *
   * @param key the key.
   * @return true if a value indexed by the given key exists in the map.
   */
  public final native boolean has(String key) /*-{
    return this.hasOwnProperty(':' + key);
  }-*/;

  /**
   * Returns the value corresponding to the key.
   *
   * @param key the key.
   * @return the value that corresponds to the key, or null if not present.
   */
  public final native String get(String key) /*-{
    return this[':' + key];
  }-*/;

  /**
   * Puts the value in the map at the given key. The value can be null. Key-
   * value pairs with null value return true in has(key) and null in get(key).
   *
   * @param key the key.
   * @param value the value to set.
   */
  public final native void put(String key, String value) /*-{
    this[':' + key] = value;
  }-*/;


  /**
   * Removes the value with the given key from the map.
   *
   * @param key the key to remove.
   */
  public final void remove(String key) {
    if (has(key)) {
      nativeRemove(key);
    }
  }

  /**
   * Modifies the current state with the delta. The delta contains key-value
   * pairs to modify. If the value in the delta is null the key is removed from
   * the current state map.
   *
   * @param delta the delta map.
   */
  public final void applyDelta(StateMap delta) {
    delta.each(new Each() {
      @Override
      public void apply(String key, String value) {
        if (value != null) {
          put(key, value);
        } else {
          remove(key);
        }
      }
    });
  }

  /**
   * Calculates the delta that can be applied to the current state map to get
   * the new state map. Note that null values are treated as non-existent in
   * both this and newMap.
   *
   * @param newMap the new map to calculate the delta for.
   * @return the delta that can be applied to the current state map to get the
   *         new state map.
   */
  public final StateMap getDelta(final StateMap newMap) {
    final StateMap result = StateMap.create();
    newMap.each(new Each() {
      @Override
      public void apply(String key, String value) {
        if ((value != null) && !value.equals(get(key))) {
          result.put(key, value);
        }
      }
    });
    each(new Each() {
      @Override
      public void apply(String key, String value) {
        if ((value != null) && (newMap.get(key) == null)) {
          result.put(key, null);
        }
      }
    });
    return result;
  }

  /**
   * Compares key-value pairs with the otherState. This comparison is not
   * sensitive to the order in which the pairs are put in JS object. null
   * is not equal to an empty map.
   *
   * TODO(user): Consider implementing as equals; add hashCode.
   *
   * @param otherMap the map to compare to the current one.
   * @return true if the key-value pairs are identical, false otherwise.
   */
  public final boolean compare(final StateMap otherMap) {
    return (otherMap != null) &&
           checkKeyValue(new KeyValueComparator(otherMap)) &&
           otherMap.checkKeyValue(new KeyValueComparator(this));
  }

  /**
   * Iterates over key-value pairs and calls CheckKeyValue.check(key, value),
   * interrupts the loop if the returned value is false.
   *
   * @param proc Interface that implements action for each key-value pair.
   */
  public final void each(final Each proc) {
    checkKeyValue(new CheckKeyValue() {
      @Override
      public boolean check(String key, String value) {
        proc.apply(key, value);
        return true;
      }
    });
  }

  /**
   * Iterates over key-value pairs and calls CheckKeyValue.check(key, value),
   * interrupts the loop if the returned value is false.
   *
   * @param proc Interface that implements action for each key-value pair.
   */
  public final boolean checkKeyValue(CheckKeyValue proc) {
    try {
      return checkKeyValueImpl(proc);
    } catch (Exception e) {
      GWT.getUncaughtExceptionHandler().onUncaughtException(e);
      return false;
    }
  }


  private final native boolean checkKeyValueImpl(CheckKeyValue proc) /*-{
     for (var key in this) {
       if (this.hasOwnProperty(key) && (key.charAt(0) === ':')) {
         if (!proc.
                  @org.waveprotocol.wave.client.gadget.StateMap$CheckKeyValue::check(Ljava/lang/String;Ljava/lang/String;)
                      (key.substring(1), this[key])) {
           return false;
         }
       }
     }
     return true;
   }-*/;

  /**
   * Clears the StateMap object.
   */
  public final void clear() {
    each(new Each() {
      @Override
      public void apply(String key, String value) {
        remove(key);
      }
    });
  }

  /**
   * Copy the contents of an existing StateMap into this one.
   *
   * @param map Map to copy key-value pairs from.
   */
  public final void copyFrom(StateMap map) {
    clear();
    map.each(new Each() {
      @Override
      public void apply(String key, String value) {
        put(key, value);
      }
    });
  }

  /**
   * Copy key-value pairs from jsonObject; accept only string, number, or
   * null values; skip all pairs with other values. This step makes sure that
   * the StateMap only contains sanitized keys prefixed with ':'. Note that
   * hasOwnProperty() cannot be used on jsonObject. However, typeof and null
   * comparison are safe.
   *
   * @param source JavaScriptObject to copy from.
   * @param target JavaScriptObject to copy into.
   */
  public final static native void copyJson(JavaScriptObject source, JavaScriptObject target) /*-{
    for (var key in target) {
      if (target.hasOwnProperty(key)) {
        delete target[key];
      }
    }

    for (var key in source) {
      if (source[key] === null) {
        target[':' + key] = null;
      } else if (typeof source[key] === 'string') {
        target[':' + key] = source[key];
      } else if (typeof source[key] === 'number') {
        target[':' + key] = String(source[key]);
      }
    }
  }-*/;

  /**
   * Populates this StateMap with key-value pairs from the json String.
   *
   * @param json The JSON string to process.
   */
  public final native void fromJson(String json) /*-{
    var jsonObject = {};
    // Use safe eval defined in gadgets.json. This makes sure eval does not
    // execute any dangerous operations.
    if (/^[\],:{}\s]*$/.test(json.replace(/\\["\\\/b-u]/g, '@').
        replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']').
        replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {
      jsonObject = eval('(' + json + ')');
    }

    @org.waveprotocol.wave.client.gadget.StateMap::copyJson(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)
        (jsonObject, this);
  }-*/;

  /**
   * Populates this StateMap with key-value pairs from a JavaScriptObject.
   *
   * @param jsonObject The JSON object to process.
   */
  public final void fromJsonObject(JavaScriptObject jsonObject) {
    copyJson(jsonObject, this);
  }

  /**
   * Converts this StateMap to a JSON String. Eliminates the ":" key prefixes.
   *
   * @return a JSON string representation of this map.
   */
  public final String toJson() {
    final StringBuilder builder = new StringBuilder();
    builder.append("{");
    each(new Each() {
      private boolean firstKey = true;
      @Override
      public void apply(String key, String value) {
        if (firstKey) {
          firstKey = false;
        } else {
          builder.append(",");
        }
        builder.append(escapeValue(key) + ":");
        if (value == null) {
          builder.append("null");
        } else {
          builder.append(escapeValue(value));
        }
      }
    });
    builder.append("}");
    return builder.toString();
  }

  private final native void putKeyValue(JavaScriptObject jso, String key, String value) /*-{
    jso[key] = value;
  }-*/;

  /**
   * Convert this into a serializable JavaScriptObject. Eliminates the ":"
   * key prefixes.
   *
   * @return The JavaScriptObject of the valid keys.
   */
  public final JavaScriptObject asJavaScriptObject() {
    final JavaScriptObject jso = JavaScriptObject.createObject();
    each(new Each() {
      @Override
      public void apply(String key, String value) {
        putKeyValue(jso, key, value);
      }
    });
    return jso;
  }

  /**
   * Helper JS method: removes the value with the given key from the map, the
   * caller makes sure the key exists.
   *
   * @param key the key to remove.
   */
  private final native void nativeRemove(String key) /*-{
    delete this[':' + key];
  }-*/;

  /**
   * JavaScript string escape function.
   *
   * @param string input string.
   * @return escaped string.
   */
  private final native String escapeValue(String string) /*-{
    // TODO(user): Replace this with a call to
    // com.google.gwt.core.client.JsonUtils.escapeValue when it's checked in.
    var escapable = new RegExp('[\\\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4' +
                               '\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]',
                               'g');
    var meta = {
        '\b': '\\b', '\t': '\\t', '\n': '\\n', '\f': '\\f',
        '\r': '\\r', '"' : '\\"', '\\': '\\\\'};

    escapable.lastIndex = 0;
    return escapable.test(string) ?
       '"' + string.replace(escapable, function (a) {
              var c = meta[a];
              return typeof c === 'string' ? c :
                  '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
          }) + '"' :
          '"' + string + '"';
  }-*/;
}
