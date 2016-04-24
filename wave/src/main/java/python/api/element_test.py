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

"""Unit tests for the element module."""


import base64
import unittest

import element
import util


class TestElement(unittest.TestCase):
  """Tests for the element.Element class."""

  def testProperties(self):
    el = element.Element(element.Gadget.class_type,
                         key='value')
    self.assertEquals('value', el.key)

  def testFormElement(self):
    el = element.Input('input')
    self.assertEquals(element.Input.class_type, el.type)
    self.assertEquals(el.value, '')
    self.assertEquals(el.name, 'input')

  def testImage(self):
    image = element.Image('http://test.com/image.png', width=100, height=100)
    self.assertEquals(element.Image.class_type, image.type)
    self.assertEquals(image.url, 'http://test.com/image.png')
    self.assertEquals(image.width, 100)
    self.assertEquals(image.height, 100)

  def testAttachment(self):
    attachment = element.Attachment(caption='My Favorite', data='SomefakeData')
    self.assertEquals(element.Attachment.class_type, attachment.type)
    self.assertEquals(attachment.caption, 'My Favorite')
    self.assertEquals(attachment.data, 'SomefakeData')

  def testGadget(self):
    gadget = element.Gadget('http://test.com/gadget.xml')
    self.assertEquals(element.Gadget.class_type, gadget.type)
    self.assertEquals(gadget.url, 'http://test.com/gadget.xml')

  def testInstaller(self):
    installer = element.Installer('http://test.com/installer.xml')
    self.assertEquals(element.Installer.class_type, installer.type)
    self.assertEquals(installer.manifest, 'http://test.com/installer.xml')

  def testSerialize(self):
    image = element.Image('http://test.com/image.png', width=100, height=100)
    s = util.serialize(image)
    k = s.keys()
    k.sort()
    # we should really only have three things to serialize
    props = s['properties']
    self.assertEquals(len(props), 3)
    self.assertEquals(props['url'], 'http://test.com/image.png')
    self.assertEquals(props['width'], 100)
    self.assertEquals(props['height'], 100)

  def testSerializeAttachment(self):
    attachment = element.Attachment(caption='My Favorite', data='SomefakeData')
    s = util.serialize(attachment)
    k = s.keys()
    k.sort()
    # we should really have two things to serialize
    props = s['properties']
    self.assertEquals(len(props), 2)
    self.assertEquals(props['caption'], 'My Favorite')
    self.assertEquals(props['data'], base64.encodestring('SomefakeData'))
    self.assertEquals(attachment.data, 'SomefakeData')

  def testSerializeLine(self):
    line = element.Line(element.Line.TYPE_H1, alignment=element.Line.ALIGN_LEFT)
    s = util.serialize(line)
    k = s.keys()
    k.sort()
    # we should really only have three things to serialize
    props = s['properties']
    self.assertEquals(len(props), 2)
    self.assertEquals(props['alignment'], 'l')
    self.assertEquals(props['lineType'], 'h1')

  def testSerializeGadget(self):
    gadget = element.Gadget('http://test.com', {'prop1': 'a', 'prop_cap': None}) 
    s = util.serialize(gadget)
    k = s.keys()
    k.sort()
    # we should really only have three things to serialize
    props = s['properties']
    self.assertEquals(len(props), 3)
    self.assertEquals(props['url'], 'http://test.com')
    self.assertEquals(props['prop1'], 'a')
    self.assertEquals(props['prop_cap'], None)

  def testGadgetElementFromJson(self):
    url = 'http://www.foo.com/gadget.xml'
    json = {
      'type': element.Gadget.class_type,
      'properties': {
        'url': url,
      }
    }
    gadget = element.Element.from_json(json)
    self.assertEquals(element.Gadget.class_type, gadget.type)
    self.assertEquals(url, gadget.url)

  def testImageElementFromJson(self):
    url = 'http://www.foo.com/image.png'
    width = '32'
    height = '32'
    attachment_id = '2'
    caption = 'Test Image'
    json = {
      'type': element.Image.class_type,
      'properties': {
        'url': url,
        'width': width,
        'height': height,
        'attachmentId': attachment_id,
        'caption': caption,
      }
    }
    image = element.Element.from_json(json)
    self.assertEquals(element.Image.class_type, image.type)
    self.assertEquals(url, image.url)
    self.assertEquals(width, image.width)
    self.assertEquals(height, image.height)
    self.assertEquals(attachment_id, image.attachmentId)
    self.assertEquals(caption, image.caption)

  def testAttachmentElementFromJson(self):
    caption = 'fake caption'
    data = 'fake data'
    mime_type = 'fake mime'
    attachment_id = 'fake id'
    attachment_url = 'fake URL'
    json = {
      'type': element.Attachment.class_type,
      'properties': {
        'caption': caption,
        'data': data,
        'mimeType': mime_type,
        'attachmentId': attachment_id,
        'attachmentUrl': attachment_url,
      }
    }
    attachment = element.Element.from_json(json)
    self.assertEquals(element.Attachment.class_type, attachment.type)
    self.assertEquals(caption, attachment.caption)
    self.assertEquals(data, attachment.data)
    self.assertEquals(mime_type, attachment.mimeType)
    self.assertEquals(attachment_id, attachment.attachmentId)
    self.assertEquals(attachment_url, attachment.attachmentUrl)

  def testFormElementFromJson(self):
    name = 'button'
    value = 'value'
    default_value = 'foo'
    json = {
      'type': element.Label.class_type,
      'properties': {
        'name': name,
        'value': value,
        'defaultValue': default_value,
      }
    }
    el = element.Element.from_json(json)
    self.assertEquals(element.Label.class_type, el.type)
    self.assertEquals(name, el.name)
    self.assertEquals(value, el.value)

  def testCanInstantiate(self):
    bag = [element.Check(name='check', value='value'),
           element.Button(name='button', value='caption'),
           element.Input(name='input', value='caption'),
           element.Label(label_for='button', caption='caption'),
           element.RadioButton(name='name', group='group'),
           element.RadioButtonGroup(name='name', value='value'),
           element.Password(name='name', value='geheim'),
           element.TextArea(name='name', value='\n\n\n'),
           element.Installer(manifest='test.com/installer.xml'),
           element.Line(line_type='type',
                        indent='3',
                        alignment='r',
                        direction='d'),
           element.Gadget(url='test.com/gadget.xml',
                          props={'key1': 'val1', 'key2': 'val2'}),
           element.Image(url='test.com/image.png', width=100, height=200),
           element.Attachment(caption='fake caption', data='fake data')]
    types_constructed = set([type(x) for x in bag])
    types_required = set(element.ALL.values())
    missing_required = types_constructed.difference(types_required)
    self.assertEquals(missing_required, set())
    missing_constructed = types_required.difference(types_constructed)
    self.assertEquals(missing_constructed, set())


if __name__ == '__main__':
  unittest.main()
