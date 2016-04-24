#!/usr/bin/python2.4
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

"""Utility library containing various helpers used by the API."""

import re

CUSTOM_SERIALIZE_METHOD_NAME = 'serialize'

MARKUP_RE = re.compile(r'<([^>]*?)>')
RESERVED_PROXY_FOR_CHARS_RE = re.compile(ur'[\s\u0000-\u001F@,:<>\u007F]')

def force_unicode(object):
  """ Return the Unicode string version of object, with UTF-8 encoding. """
  if isinstance(object, unicode):
    return object
  return unicode(str(object), 'utf-8')

def parse_markup(markup):
  """Parses a bit of markup into robot compatible text.
  
  For now this is a rough approximation.
  """
  def replace_tag(group):
    if not group.groups:
      return ''
    tag = group.groups()[0].split(' ', 1)[0]
    if (tag == 'p' or tag == 'br'):
      return '\n'
    return ''

  return MARKUP_RE.sub(replace_tag, markup)

def is_iterable(inst):
  """Returns whether or not this is a list, tuple, set or dict .

  Note that this does not return true for strings.
  """
  return hasattr(inst, '__iter__')

def is_dict(inst):
  """Returns whether or not the specified instance is a dict."""
  return hasattr(inst, 'iteritems')


def is_user_defined_new_style_class(obj):
  """Returns whether or not the specified instance is a user-defined type."""
  return type(obj).__module__ != '__builtin__'

def lower_camel_case(s):
  """Converts a string to lower camel case.

  Examples:
    foo => foo
    foo_bar => fooBar
    foo__bar => fooBar
    foo_bar_baz => fooBarBaz

  Args:
    s: The string to convert to lower camel case.

  Returns:
    The lower camel cased string.
  """
  return reduce(lambda a, b: a + (a and b.capitalize() or b), s.split('_'))

def non_none_dict(d):
  """return a copy of the dictionary without none values."""
  return dict([a for a in d.items() if a[1] is not None])

def _serialize_attributes(obj):
  """Serializes attributes of an instance.

  Iterates all attributes of an object and invokes serialize if they are
  public and not callable.

  Args:
    obj: The instance to serialize.

  Returns:
    The serialized object.
  """
  data = {}
  for attr_name in dir(obj):
    if attr_name.startswith('_'):
      continue
    attr = getattr(obj, attr_name)
    if attr is None or callable(attr):
      continue
    # Looks okay, serialize it.
    data[lower_camel_case(attr_name)] = serialize(attr)
  return data


def _serialize_dict(d):
  """Invokes serialize on all of its key/value pairs.

  Args:
    d: The dict instance to serialize.

  Returns:
    The serialized dict.
  """
  data = {}
  for k, v in d.items():
    data[lower_camel_case(k)] = serialize(v)
  return data


def serialize(obj):
  """Serializes any instance.

  If this is a user-defined instance
  type, it will first check for a custom Serialize() function and use that
  if it exists. Otherwise, it will invoke serialize all of its public
  attributes. Lists and dicts are serialized trivially.

  Args:
    obj: The instance to serialize.

  Returns:
    The serialized object.
  """
  if is_user_defined_new_style_class(obj):
    if obj and hasattr(obj, CUSTOM_SERIALIZE_METHOD_NAME):
      method = getattr(obj, CUSTOM_SERIALIZE_METHOD_NAME)
      if callable(method):
        return method()
    return _serialize_attributes(obj)
  elif is_dict(obj):
    return _serialize_dict(obj)
  elif is_iterable(obj):
    return [serialize(v) for v in obj]
  return obj

def is_valid_proxy_for_id(s):
  """ Checks if the given string is a valid proxy id.

  This method asserts whether the string contains reserved characters or not.
  This check is to ensure that when we concatenate the robot id and the proxy
  id, it doesn't result in an invalid participant id.

  The reserved characters are:
  - whitespaces
  - non-printing characters: decimal 0 - 31 (hex 00 - 1F), and decimal 127
    (hex 7F)
  - @, comma, :, <, and >
  If you need to pass in an arbitrary string as the proxy id, please consider
  encoding the string with a URL encoder (for example, urllib.quote_plus) or
  base64 encoder (for example, base64.b64encode) first.

  Args:
    s: the string to be checked.

  Returns:
    True if the string is a valid proxy for id.
  """
  return RESERVED_PROXY_FOR_CHARS_RE.search(s) == None

def check_is_valid_proxy_for_id(s):
  """ Checks if the given string is a valid proxy id.

  Please see isValidProxyForId(String) for more details on the assertion. This
  method raises a ValueError exception if the input string is not a valid proxy
  id.

  Args:
    s: the string to be checked.
  """
  if s != None and not is_valid_proxy_for_id(s):
    raise ValueError(s + ' is not a valid proxy for id.')

class StringEnum(object):
  """Enum like class that is configured with a list of values.

  This class effectively implements an enum for Elements, except for that
  the actual values of the enums will be the string values.
  """

  def __init__(self, *values):
    for name in values:
      setattr(self, name, name)
