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

"""Unit tests for the blip module."""


import unittest

import blip
import element
import ops
import simplejson

TEST_BLIP_DATA = {
    'childBlipIds': [],
    'content': '\nhello world!\nanother line',
    'contributors': ['robot@test.com', 'user@test.com'],
    'creator': 'user@test.com',
    'lastModifiedTime': 1000,
    'parentBlipId': None,
    'annotations': [{'range': {'start': 2, 'end': 3},
                     'name': 'key', 'value': 'val'}],
    'waveId': 'test.com!w+g3h3im',
    'waveletId': 'test.com!root+conv',
    'elements':{'14':{'type':'GADGET','properties':{'url':'http://a/b.xml'}}},
}

CHILD_BLIP_ID = 'b+42'
ROOT_BLIP_ID = 'b+43'


class TestBlip(unittest.TestCase):
  """Tests the primary data structures for the wave model."""

  def assertBlipStartswith(self, expected, totest):
    actual = totest.text[:len(expected)]
    self.assertEquals(expected, actual)

  def new_blip(self, **args):
    """Create a blip for testing."""
    data = TEST_BLIP_DATA.copy()
    data.update(args)
    res = blip.Blip(data, self.all_blips, self.operation_queue)
    self.all_blips[res.blip_id] = res
    return res

  def setUp(self):
    self.all_blips = {}
    self.operation_queue = ops.OperationQueue()

  def testBlipProperties(self):
    root = self.new_blip(blipId=ROOT_BLIP_ID,
                         childBlipIds=[CHILD_BLIP_ID])
    child = self.new_blip(blipId=CHILD_BLIP_ID,
                          parentBlipId=ROOT_BLIP_ID)
    self.assertEquals(ROOT_BLIP_ID, root.blip_id)
    self.assertEquals([CHILD_BLIP_ID], root.child_blip_ids)
    self.assertEquals(set(TEST_BLIP_DATA['contributors']), root.contributors)
    self.assertEquals(TEST_BLIP_DATA['creator'], root.creator)
    self.assertEquals(TEST_BLIP_DATA['content'], root.text)
    self.assertEquals(TEST_BLIP_DATA['lastModifiedTime'],
                      root.last_modified_time)
    self.assertEquals(TEST_BLIP_DATA['parentBlipId'], root.parent_blip_id)
    self.assertEquals(TEST_BLIP_DATA['waveId'], root.wave_id)
    self.assertEquals(TEST_BLIP_DATA['waveletId'], root.wavelet_id)
    self.assertEquals(TEST_BLIP_DATA['content'][3], root[3])
    self.assertEquals(element.Gadget.class_type, root[14].type)
    self.assertEquals('http://a/b.xml', root[14].url)
    self.assertEquals('a', root.text[14])
    self.assertEquals(len(TEST_BLIP_DATA['content']), len(root))
    self.assertTrue(root.is_root())
    self.assertFalse(child.is_root())
    self.assertEquals(root, child.parent_blip)

  def testBlipSerialize(self):
    root = self.new_blip(blipId=ROOT_BLIP_ID,
                         childBlipIds=[CHILD_BLIP_ID])
    serialized = root.serialize()
    unserialized = blip.Blip(serialized, self.all_blips, self.operation_queue)
    self.assertEquals(root.blip_id, unserialized.blip_id)
    self.assertEquals(root.child_blip_ids, unserialized.child_blip_ids)
    self.assertEquals(root.contributors, unserialized.contributors)
    self.assertEquals(root.creator, unserialized.creator)
    self.assertEquals(root.text, unserialized.text)
    self.assertEquals(root.last_modified_time, unserialized.last_modified_time)
    self.assertEquals(root.parent_blip_id, unserialized.parent_blip_id)
    self.assertEquals(root.wave_id, unserialized.wave_id)
    self.assertEquals(root.wavelet_id, unserialized.wavelet_id)
    self.assertTrue(unserialized.is_root())

  def testDocumentOperations(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    newlines = [x for x in blip.find('\n')]
    self.assertEquals(2, len(newlines))
    blip.first('world').replace('jupiter')
    bits = blip.text.split('\n')
    self.assertEquals(3, len(bits))
    self.assertEquals('hello jupiter!', bits[1])
    blip.range(2, 5).delete()
    self.assertBlipStartswith('\nho jupiter', blip)

    blip.first('ho').insert_after('la')
    self.assertBlipStartswith('\nhola jupiter', blip)
    blip.at(3).insert(' ')
    self.assertBlipStartswith('\nho la jupiter', blip)

  def testElementHandling(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    url = 'http://www.test.com/image.png'

    org_len = len(blip)
    blip.append(element.Image(url=url))

    elems = [elem for elem in blip.find(element.Image, url=url)]
    self.assertEquals(1, len(elems))
    elem = elems[0]
    self.assertTrue(isinstance(elem, element.Image))
    blip.at(1).insert('twelve chars')
    self.assertTrue(blip.text.startswith('\ntwelve charshello'))

    elem = blip[org_len + 12].value()
    self.assertTrue(isinstance(elem, element.Image))

    blip.first('twelve ').delete()
    self.assertTrue(blip.text.startswith('\nchars'))

    elem = blip[org_len + 12 - len('twelve ')].value()
    self.assertTrue(isinstance(elem, element.Image))

    blip.first('chars').replace(element.Image(url=url))
    elems = [elem for elem in blip.find(element.Image, url=url)]
    self.assertEquals(2, len(elems))
    self.assertTrue(blip.text.startswith('\n hello'))
    elem = blip[1].value()
    self.assertTrue(isinstance(elem, element.Image))

  def testAnnotationHandling(self):
    key = 'style/fontWeight'

    def get_bold():
      for an in blip.annotations[key]:
        if an.value == 'bold':
          return an
      return None

    json = ('[{"range":{"start":3,"end":6},"name":"%s","value":"bold"}]'
            % key)
    blip = self.new_blip(blipId=ROOT_BLIP_ID,
                         annotations=simplejson.loads(json))
    self.assertEquals(1, len(blip.annotations))
    self.assertNotEqual(None, get_bold().value)
    self.assertTrue(key in blip.annotations)

    # extend the bold annotation by adding:
    blip.range(5, 8).annotate(key, 'bold')
    self.assertEquals(1, len(blip.annotations))
    self.assertEquals(8, get_bold().end)

    # clip by adding a same keyed:
    blip[4:12].annotate(key, 'italic')
    self.assertEquals(2, len(blip.annotations[key]))
    self.assertEquals(4, get_bold().end)

    # now split the italic one:
    blip.range(6, 7).clear_annotation(key)
    self.assertEquals(3, len(blip.annotations[key]))

    # test names and iteration
    self.assertEquals(1, len(blip.annotations.names()))
    self.assertEquals(3, len([x for x in blip.annotations]))
    blip[3: 5].annotate('foo', 'bar')
    self.assertEquals(2, len(blip.annotations.names()))
    self.assertEquals(4, len([x for x in blip.annotations]))
    blip[3: 5].clear_annotation('foo')

    # clear the whole thing
    blip.all().clear_annotation(key)
    # getting to the key should now throw an exception
    self.assertRaises(KeyError, blip.annotations.__getitem__, key)

  def testBlipOperations(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    self.assertEquals(1, len(self.all_blips))

    otherblip = blip.reply()
    otherblip.append('hello world')
    self.assertEquals('hello world', otherblip.text)
    self.assertEquals(blip.blip_id, otherblip.parent_blip_id)
    self.assertEquals(2, len(self.all_blips))

    another = blip.continue_thread()
    another.append('hello world')
    self.assertEquals('hello world', another.text)
    self.assertEquals(blip.parent_blip_id, another.parent_blip_id)
    self.assertEquals(3, len(self.all_blips))

    inline = blip.insert_inline_blip(3)
    self.assertEquals(blip.blip_id, inline.parent_blip_id)
    self.assertEquals(4, len(self.all_blips))

  def testInsertInlineBlipCantInsertAtTheBeginning(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    self.assertEquals(1, len(self.all_blips))
    self.assertRaises(IndexError, blip.insert_inline_blip, 0)
    self.assertEquals(1, len(self.all_blips))

  def testDocumentModify(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    blip.all().replace('a text with text and then some text')
    blip[7].insert('text ')
    blip.all('text').replace('thing')
    self.assertEquals('a thing thing with thing and then some thing',
                      blip.text)

  def testIteration(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    blip.all().replace('aaa 012 aaa 345 aaa 322')
    count = 0
    prev = -1
    for start, end in blip.all('aaa'):
      count += 1
      self.assertTrue(prev < start)
      prev = start
    self.assertEquals(3, count)


  def testBlipRefValue(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    content = blip.text
    content = content[:4] + content[5:]
    del blip[4]
    self.assertEquals(content, blip.text)

    content = content[:2] + content[3:]
    del blip[2:3]
    self.assertEquals(content, blip.text)

    blip[2:3] = 'bike'
    content = content[:2] + 'bike' + content[3:]
    self.assertEquals(content, blip.text)

    url = 'http://www.test.com/image.png'
    blip.append(element.Image(url=url))
    self.assertEqual(url, blip.first(element.Image).url)

    url2 = 'http://www.test.com/another.png'
    blip[-1].update_element({'url': url2})
    self.assertEqual(url2, blip.first(element.Image).url)

    self.assertTrue(blip[3:5] == blip.text[3:5])

    blip.append('geheim')
    self.assertTrue(blip.first('geheim'))
    self.assertFalse(blip.first(element.Button))
    blip.append(element.Button(name='test1', value='Click'))
    button = blip.first(element.Button)
    button.update_element({'name': 'test2'})
    self.assertEqual('test2', button.name)

  def testReplace(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    blip.all().replace('\nxxxx')
    blip.all('yyy').replace('zzz')
    self.assertEqual('\nxxxx', blip.text)

  def testDeleteRangeThatSpansAcrossAnnotationEndPoint(self):
    json = ('[{"range":{"start":1,"end":3},"name":"style","value":"bold"}]')
    blip = self.new_blip(blipId=ROOT_BLIP_ID,
                         annotations=simplejson.loads(json),
                         content='\nFoo bar.')
    blip.range(2, 4).delete()
    self.assertEqual('\nF bar.', blip.text)
    self.assertEqual(1, blip.annotations['style'][0].start)
    self.assertEqual(2, blip.annotations['style'][0].end)

  def testInsertBeforeAnnotationStartPoint(self):
    json = ('[{"range":{"start":4,"end":9},"name":"style","value":"bold"}]')
    blip = self.new_blip(blipId=ROOT_BLIP_ID,
                         annotations=simplejson.loads(json),
                         content='\nFoo bar.')
    blip.at(4).insert('d and')
    self.assertEqual('\nFood and bar.', blip.text)
    self.assertEqual(9, blip.annotations['style'][0].start)
    self.assertEqual(14, blip.annotations['style'][0].end)

  def testDeleteRangeInsideAnnotation(self):
    json = ('[{"range":{"start":1,"end":5},"name":"style","value":"bold"}]')
    blip = self.new_blip(blipId=ROOT_BLIP_ID,
                         annotations=simplejson.loads(json),
                         content='\nFoo bar.')
    blip.range(2, 4).delete()

    self.assertEqual('\nF bar.', blip.text)
    self.assertEqual(1, blip.annotations['style'][0].start)
    self.assertEqual(3, blip.annotations['style'][0].end)

  def testReplaceInsideAnnotation(self):
    json = ('[{"range":{"start":1,"end":5},"name":"style","value":"bold"}]')
    blip = self.new_blip(blipId=ROOT_BLIP_ID,
                         annotations=simplejson.loads(json),
                         content='\nFoo bar.')
    blip.range(2, 4).replace('ooo')
    self.assertEqual('\nFooo bar.', blip.text)
    self.assertEqual(1, blip.annotations['style'][0].start)
    self.assertEqual(6, blip.annotations['style'][0].end)

    blip.range(2, 5).replace('o')
    self.assertEqual('\nFo bar.', blip.text)
    self.assertEqual(1, blip.annotations['style'][0].start)
    self.assertEqual(4, blip.annotations['style'][0].end)

  def testReplaceSpanAnnotation(self):
    json = ('[{"range":{"start":1,"end":4},"name":"style","value":"bold"}]')
    blip = self.new_blip(blipId=ROOT_BLIP_ID,
                         annotations=simplejson.loads(json),
                         content='\nFoo bar.')
    blip.range(2, 9).replace('')
    self.assertEqual('\nF', blip.text)
    self.assertEqual(1, blip.annotations['style'][0].start)
    self.assertEqual(2, blip.annotations['style'][0].end)

  def testSearchWithNoMatchShouldNotGenerateOperation(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID)
    self.assertEqual(-1, blip.text.find(':('))
    self.assertEqual(0, len(self.operation_queue))
    blip.all(':(').replace(':)')
    self.assertEqual(0, len(self.operation_queue))

  def testBlipsRemoveWithId(self):
    blip_dict = {
        ROOT_BLIP_ID: self.new_blip(blipId=ROOT_BLIP_ID,
                                    childBlipIds=[CHILD_BLIP_ID]),
        CHILD_BLIP_ID: self.new_blip(blipId=CHILD_BLIP_ID,
                                     parentBlipId=ROOT_BLIP_ID)
    }
    blips = blip.Blips(blip_dict)
    blips._remove_with_id(CHILD_BLIP_ID)
    self.assertEqual(1, len(blips))
    self.assertEqual(0, len(blips[ROOT_BLIP_ID].child_blip_ids))

  def testAppendMarkup(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID, content='\nFoo bar.')
    markup = '<p><span>markup<span> content</p>'
    blip.append_markup(markup)
    self.assertEqual(1, len(self.operation_queue))
    self.assertEqual('\nFoo bar.\nmarkup content', blip.text)

  def testBundledAnnotations(self):
    blip = self.new_blip(blipId=ROOT_BLIP_ID, content='\nFoo bar.')
    blip.append('not bold')
    blip.append('bold', bundled_annotations=[('style/fontWeight', 'bold')])
    self.assertEqual(2, len(blip.annotations))
    self.assertEqual('bold', blip.annotations['style/fontWeight'][0].value)

  def testInlineBlipOffset(self):
    offset = 14
    self.new_blip(blipId=ROOT_BLIP_ID,
                  childBlipIds=[CHILD_BLIP_ID],
                  elements={str(offset):
                      {'type': element.Element.INLINE_BLIP_TYPE,
                       'properties': {'id': CHILD_BLIP_ID}}})
    child = self.new_blip(blipId=CHILD_BLIP_ID,
                          parentBlipId=ROOT_BLIP_ID)
    self.assertEqual(offset, child.inline_blip_offset)

if __name__ == '__main__':
  unittest.main()
