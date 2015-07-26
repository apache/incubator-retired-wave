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

package org.waveprotocol.wave.client.gadget.renderer;

import static org.waveprotocol.wave.model.gadget.GadgetConstants.CATEGORY_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.PREF_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.STATE_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.TITLE_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.VALUE_ATTRIBUTE;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Class to wrap gadget element children. Includes common methods to deal with
 * data stored in the element nodes.
 *
 */
public class GadgetElementChild {
  /**
   * Enumerates gadget child element types.
   */
  public static enum Type {
    /** Name element type. */
    CATEGORIES(CATEGORY_TAGNAME),
    /** Title element type. */
    TITLE(TITLE_TAGNAME),
    /** State element type. */
    STATE(STATE_TAGNAME),
    /** Preference element type. */
    PREF(PREF_TAGNAME),
    /** Unknown element type. */
    UNKNOWN("?");

    private final String tag;

    private Type(String tag) {
      this.tag = tag;
      typeMap.put(tag, this);
    }

    @Override
    public String toString() {
      return tag;
    }
  }

  /**
   * Type map that has to be outside the Type class.
   */
  // TODO(user): Use CollectionUtils.createStringMap() instead, less bug prone
  // and easier to write unit tests for this class
  private final static StringMap<Type> typeMap = CollectionUtils.createStringMap();

  private final Type type;
  private final ContentElement element;
  private String value = "";

  /**
   * Returns element type based on the tag.
   *
   * @param tag tag to get the type for
   * @return the type that corresponds to the tag
   */
  private static Type getElementType(String tag) {
    return typeMap.containsKey(tag) ? typeMap.get(tag) : Type.UNKNOWN;
  }

  /**
   * Constructs a new wrapper object for the element.
   *
   * @param element element to wrap
   */
  private GadgetElementChild(ContentElement element) {
    this.element = element;
    value = element.getAttribute(VALUE_ATTRIBUTE);
    if (value == null) {
      value = "";
    }
    type = getElementType(getTag());
  }

  /**
   * Creates a new object to wrap the content node of the gadget element.
   *
   * @param node the node to be associated with the new object
   * @return new object to wrap the node
   */
  public static GadgetElementChild create(ContentNode node) {
    if ((node == null) || !node.isElement()) {
      return null;
    } else {
      ContentElement element = node.asElement();
      return new GadgetElementChild(element);
    }
  }

  /**
   * Returns the current value (maybe different from the persistent value).
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the element type.
   *
   * @return element type
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the element tag.
   *
   * @return element tag
   */
  public String getTag() {
    return element.getTagName();
  }

  /**
   * Returns the element.
   *
   * @return element
   */
  public ContentElement getElement() {
    return element;
  }

  /**
   * Returns the name attribute of the element. The name attribute is the key in
   * key-value pair elements such as state or pref.
   *
   * @return the key
   */
  public String getKey() {
    return element.getName();
  }

  /**
   * Updates the current value to the one set in the element's text node and
   * returns boolean that indicates whether the value has changed.
   *
   * @return true if the value changed, false otherwise
   */
  public boolean updateValueFromElement() {
    String persistentValue = element.getAttribute(VALUE_ATTRIBUTE);
    if (persistentValue == null) {
      persistentValue = "";
    }
    if (!persistentValue.equals(value)) {
      value = persistentValue;
      return true;
    }
    return false;
  }

  /**
   * Sets the element text node to a new value.
   *
   * @param value new value
   */
  public void setValue(String value) {
    if (this.value.equals(value)) {
      return;
    }
    element.getMutableDoc().setElementAttribute(element, VALUE_ATTRIBUTE, value);
  }

  /**
   * Returns whether the given child is a duplicate (same type and key) of this
   * object.
   *
   * @param child the child to check
   * @return true if the child is duplicate, false otherwise
   */
  public boolean isDuplicate(GadgetElementChild child) {
    if ((child == null) || (child.getType() != getType())) {
      return false;
    }
    String childKey = child.getKey();
    return  ((childKey == getKey()) || ((childKey != null) && childKey.equals(getKey())));
  }

  @Override
  public String toString() {
    String keyString  = getKey() == null ? "" : ", key '" + getKey() + "'";
    return "Gadget child " + type + keyString + ", value '" + getValue() + "'";
  }
}
