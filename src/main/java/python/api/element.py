#!/usr/bin/python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""Elements are non-text bits living in blips like images, gadgets etc.

This module defines the Element class and the derived classes.
"""


import base64
import logging
import sys

import util


class Element(object):
  """Elements are non-text content within a document.

  These are generally abstracted from the Robot. Although a Robot can query the
  properties of an element it can only interact with the specific types that
  the element represents.

  Properties of elements are both accessible directly (image.url) and through
  the get method (image.get('url')). In general, Element
  should not be instantiated by robots, but rather rely on the derived classes.
  """

  # INLINE_BLIP_TYPE is not a separate type since it shouldn't be instantiated,
  # only be used for introspection
  INLINE_BLIP_TYPE = "INLINE_BLIP"

  def __init__(self, element_type, **properties):
    """Initializes self with the specified type and any properties.

    Args:
      element_type: string typed member of ELEMENT_TYPE
      properties: either a dictionary of initial properties, or a dictionary
          with just one member properties that is itself a dictionary of
          properties. This allows us to both use
          e = Element(atype, prop1=val1, prop2=prop2...)
          and
          e = Element(atype, properties={prop1:val1, prop2:prop2..})
    """
    if len(properties) == 1 and 'properties' in properties:
      properties = properties['properties']
    self._type = element_type
    # as long as the operation_queue of an element in None, it is
    # unattached. After an element is acquired by a blip, the blip
    # will set the operation_queue to make sure all changes to the
    # element are properly send to the server.
    self._operation_queue = None
    self._properties = properties.copy()
  
  @property
  def type(self):
    """The type of this element."""
    return self._type

  @classmethod
  def from_json(cls, json):
    """Class method to instantiate an Element based on a json string."""
    etype = json['type']
    props = json['properties'].copy()

    element_class = ALL.get(etype)
    if not element_class:
      # Unknown type. Server could be newer than we are
      return Element(element_type=etype, properties=props)

    return element_class.from_props(props)

  def get(self, key, default=None):
    """Standard get interface."""
    return self._properties.get(key, default)

  def __getattr__(self, key):
    return self._properties[key]

  def serialize(self):
    """Custom serializer for Elements."""
    return util.serialize({'properties': util.non_none_dict(self._properties),
                           'type': self._type})


class Input(Element):
  """A single-line input element."""

  class_type = 'INPUT'

  def __init__(self, name, value=''):
    super(Input, self).__init__(Input.class_type,
                                name=name,
                                value=value,
                                default_value=value)

  @classmethod
  def from_props(cls, props):
    return Input(name=props.get('name'), value=props.get('value'))


class Check(Element):
  """A checkbox element."""

  class_type = 'CHECK'

  def __init__(self, name, value=''):
    super(Check, self).__init__(Check.class_type,
                                name=name, value=value, default_value=value)

  @classmethod
  def from_props(cls, props):
    return Check(name=props.get('name'), value=props.get('value'))


class Button(Element):
  """A button element."""

  class_type = 'BUTTON'

  def __init__(self, name, value):
    super(Button, self).__init__(Button.class_type,
                                 name=name, value=value)

  @classmethod
  def from_props(cls, props):
    return Button(name=props.get('name'), value=props.get('value'))


class Label(Element):
  """A label element."""

  class_type = 'LABEL'

  def __init__(self, label_for, caption):
    super(Label, self).__init__(Label.class_type,
                                name=label_for, value=caption)

  @classmethod
  def from_props(cls, props):
    return Label(label_for=props.get('name'), caption=props.get('value'))


class RadioButton(Element):
  """A radio button element."""

  class_type = 'RADIO_BUTTON'

  def __init__(self, name, group):
    super(RadioButton, self).__init__(RadioButton.class_type,
                                      name=name, value=group)

  @classmethod
  def from_props(cls, props):
    return RadioButton(name=props.get('name'), group=props.get('value'))


class RadioButtonGroup(Element):
  """A group of radio buttons."""

  class_type = 'RADIO_BUTTON_GROUP'

  def __init__(self, name, value):
    super(RadioButtonGroup, self).__init__(RadioButtonGroup.class_type,
                                           name=name, value=value)

  @classmethod
  def from_props(cls, props):
    return RadioButtonGroup(name=props.get('name'), value=props.get('value'))


class Password(Element):
  """A password element."""

  class_type = 'PASSWORD'

  def __init__(self, name, value):
    super(Password, self).__init__(Password.class_type,
                                   name=name, value=value)

  @classmethod
  def from_props(cls, props):
    return Password(name=props.get('name'), value=props.get('value'))


class TextArea(Element):
  """A text area element."""

  class_type = 'TEXTAREA'

  def __init__(self, name, value):
    super(TextArea, self).__init__(TextArea.class_type,
                                   name=name, value=value)

  @classmethod
  def from_props(cls, props):
    return TextArea(name=props.get('name'), value=props.get('value'))


class Line(Element):
  """A line element.
  
  Note that Lines are represented in the text as newlines.
  """

  class_type = 'LINE'

  # Possible line types:
  #: Designates line as H1, largest heading.
  TYPE_H1 = 'h1'
  #: Designates line as H2 heading.
  TYPE_H2 = 'h2'
  #: Designates line as H3 heading.
  TYPE_H3 = 'h3'
  #: Designates line as H4 heading.
  TYPE_H4 = 'h4'
  #: Designates line as H5, smallest heading.
  TYPE_H5 = 'h5'
  #: Designates line as a bulleted list item.
  TYPE_LI = 'li'

  # Possible values for align
  #: Sets line alignment to left.
  ALIGN_LEFT = 'l'
  #: Sets line alignment to right.
  ALIGN_RIGHT = 'r'
  #: Sets line alignment to centered.
  ALIGN_CENTER = 'c'
  #: Sets line alignment to justified.
  ALIGN_JUSTIFIED = 'j'

  def __init__(self,
               line_type=None,
               indent=None,
               alignment=None,
               direction=None):
    super(Line, self).__init__(Line.class_type,
                               lineType=line_type,
                               indent=indent,
                               alignment=alignment,
                               direction=direction)

  @classmethod
  def from_props(cls, props):
    return Line(line_type=props.get('lineType'),
                indent=props.get('indent'),
                alignment=props.get('alignment'),
                direction=props.get('direction'))


class Gadget(Element):
  """A gadget element."""

  class_type = 'GADGET'

  def __init__(self, url, props=None):
    if props is None:
      props = {}
    props['url'] = url
    super(Gadget, self).__init__(Gadget.class_type, properties=props)

  @classmethod
  def from_props(cls, props):
    return Gadget(props.get('url'), props)

  def serialize(self):
    """Gadgets allow for None values."""
    return {'properties': self._properties, 'type': self._type}
  
  def keys(self):
    """Get the valid keys for this gadget."""
    return [x for x in self._properties.keys() if x != 'url']


class Installer(Element):
  """An installer element."""

  class_type = 'INSTALLER'

  def __init__(self, manifest):
    super(Installer, self).__init__(Installer.class_type, manifest=manifest)

  @classmethod
  def from_props(cls, props):
    return Installer(props.get('manifest'))



class Image(Element):
  """An image element."""

  class_type = 'IMAGE'

  def __init__(self, url='', width=None, height=None,
               attachmentId=None, caption=None):
    super(Image, self).__init__(Image.class_type, url=url, width=width,
          height=height, attachmentId=attachmentId, caption=caption)

  @classmethod
  def from_props(cls, props):
    props = dict([(key.encode('utf-8'), value)
                  for key, value in props.items()])
    return apply(Image, [], props)

class Attachment(Element):
  """An attachment element.

  To create a new attachment, caption and data are needed.
  mimeType, attachmentId and attachmentUrl are sent via events.
  """

  class_type = 'ATTACHMENT'

  def __init__(self, caption=None, data=None, mimeType=None, attachmentId=None,
                attachmentUrl=None):
    Attachment.originalData = data
    super(Attachment, self).__init__(Attachment.class_type, caption=caption,
          data=data, mimeType=mimeType, attachmentId=attachmentId,
          attachmentUrl=attachmentUrl)

  def __getattr__(self, key):
    if key and key == 'data':
      return Attachment.originalData
    return super(Attachment, self).__getattr__(key)

  @classmethod
  def from_props(cls, props):
    props = dict([(key.encode('utf-8'), value)
                  for key, value in props.items()])
    return apply(Attachment, [], props)

  def serialize(self):
    """Serializes the attachment object into JSON.

    The attachment data is base64 encoded.
    """

    if self.data:
      self._properties['data'] = base64.encodestring(self.data)
    return super(Attachment, self).serialize()


def is_element(cls):
  """Returns whether the passed class is an element."""
  try:
    if not issubclass(cls, Element):
      return False
    h = hasattr(cls, 'class_type')
    return hasattr(cls, 'class_type')
  except TypeError:
    return False

ALL = dict([(item.class_type, item) for item in globals().copy().values()
            if is_element(item)])
