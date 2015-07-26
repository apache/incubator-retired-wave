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

"""Unit tests for the wavelet module."""


import unittest

import blip
import element
import ops
import wavelet

import simplejson

ROBOT_NAME = 'robot@appspot.com'

TEST_WAVELET_DATA = {
    'creator': ROBOT_NAME,
    'creationTime': 100,
    'lastModifiedTime': 101,
    'participants': [ROBOT_NAME],
    'participantsRoles': {ROBOT_NAME: wavelet.Participants.ROLE_FULL},
    'rootBlipId': 'blip-1',
    'title': 'Title',
    'waveId': 'test.com!w+g3h3im',
    'waveletId': 'test.com!root+conv',
    'tags': ['tag1', 'tag2'],
    'rootThread': {
        'id': '',
        'location': -1,
        'blipIds': ['blip-1']
    }
}

TEST_BLIP_DATA = {
    'blipId': TEST_WAVELET_DATA['rootBlipId'],
    'childBlipIds': [],
    'content': '\ntesting',
    'contributors': [TEST_WAVELET_DATA['creator'], 'robot@google.com'],
    'creator': TEST_WAVELET_DATA['creator'],
    'lastModifiedTime': TEST_WAVELET_DATA['lastModifiedTime'],
    'parentBlipId': None,
    'waveId': TEST_WAVELET_DATA['waveId'],
    'elements': {},
    'waveletId': TEST_WAVELET_DATA['waveletId'],
    'replyThreadIds': [],
    'threadId': ''
}


class TestWavelet(unittest.TestCase):
  """Tests the wavelet class."""

  def setUp(self):
    self.operation_queue = ops.OperationQueue()
    self.all_blips = {}
    self.blip = blip.Blip(TEST_BLIP_DATA,
                          self.all_blips,
                          self.operation_queue)
    self.all_blips[self.blip.blip_id] = self.blip
    root_thread_data = TEST_WAVELET_DATA.get('rootThread')
    root_thread = wavelet.BlipThread('',
                                     root_thread_data.get('location'),
                                     root_thread_data.get('blipIds', []),
                                     self.all_blips,
                                     self.operation_queue)
 
    self.wavelet = wavelet.Wavelet(TEST_WAVELET_DATA,
                                   self.all_blips,
                                   root_thread,
                                   self.operation_queue)
    self.wavelet.robot_address = ROBOT_NAME

  def testWaveletProperties(self):
    w = self.wavelet
    self.assertEquals(TEST_WAVELET_DATA['creator'], w.creator)
    self.assertEquals(TEST_WAVELET_DATA['creationTime'], w.creation_time)
    self.assertEquals(TEST_WAVELET_DATA['lastModifiedTime'],
                      w.last_modified_time)
    self.assertEquals(len(TEST_WAVELET_DATA['participants']),
                      len(w.participants))
    self.assertTrue(TEST_WAVELET_DATA['participants'][0] in w.participants)
    self.assertEquals(TEST_WAVELET_DATA['rootBlipId'], w.root_blip.blip_id)
    self.assertEquals(TEST_WAVELET_DATA['title'], w.title)
    self.assertEquals(TEST_WAVELET_DATA['waveId'], w.wave_id)
    self.assertEquals(TEST_WAVELET_DATA['waveletId'], w.wavelet_id)
    self.assertEquals(TEST_WAVELET_DATA['rootThread']['id'], w.root_thread.id)
    self.assertEquals(TEST_WAVELET_DATA['rootThread']['location'],
                      w.root_thread.location)
    self.assertEquals(len(TEST_WAVELET_DATA['rootThread']['blipIds']),
                      len(w.root_thread.blips))
    self.assertEquals('test.com', w.domain)

  def testWaveletMethods(self):
    w = self.wavelet
    reply = w.reply()
    self.assertEquals(2, len(w.blips))
    w.delete(reply)
    self.assertEquals(1, len(w.blips))
    self.assertEquals(0, len(w.data_documents))
    self.wavelet.data_documents['key'] = 'value'
    self.assert_('key' in w.data_documents)
    self.assertEquals(1, len(w.data_documents))
    for key in w.data_documents:
      self.assertEquals(key, 'key')
    self.assertEquals(1, len(w.data_documents.keys()))
    self.wavelet.data_documents['key'] = None
    self.assertEquals(0, len(w.data_documents))
    num_participants = len(w.participants)
    w.proxy_for('proxy').reply()
    self.assertEquals(2, len(w.blips))
    # check that the new proxy for participant was added
    self.assertEquals(num_participants + 1, len(w.participants))
    w._robot_address = ROBOT_NAME.replace('@', '+proxy@')
    w.proxy_for('proxy').reply()
    self.assertEquals(num_participants + 1, len(w.participants))
    self.assertEquals(3, len(w.blips))

  def testSetTitle(self):
    self.blip._content = '\nOld title\n\nContent'
    self.wavelet.title = 'New title \xd0\xb0\xd0\xb1\xd0\xb2'
    self.assertEquals(1, len(self.operation_queue))
    self.assertEquals('wavelet.setTitle',
                      self.operation_queue.serialize()[1]['method'])
    self.assertEquals(u'\nNew title \u0430\u0431\u0432\n\nContent',
                      self.blip._content)

  def testSetTitleAdjustRootBlipWithOneLineProperly(self):
    self.blip._content = '\nOld title'
    self.wavelet.title = 'New title'
    self.assertEquals(1, len(self.operation_queue))
    self.assertEquals('wavelet.setTitle',
                      self.operation_queue.serialize()[1]['method'])
    self.assertEquals('\nNew title\n', self.blip._content)

  def testSetTitleAdjustEmptyRootBlipProperly(self):
    self.blip._content = '\n'
    self.wavelet.title = 'New title'
    self.assertEquals(1, len(self.operation_queue))
    self.assertEquals('wavelet.setTitle',
                      self.operation_queue.serialize()[1]['method'])
    self.assertEquals('\nNew title\n', self.blip._content)

  def testTags(self):
    w = self.wavelet
    self.assertEquals(2, len(w.tags))
    w.tags.append('tag3')
    self.assertEquals(3, len(w.tags))
    w.tags.append('tag3')
    self.assertEquals(3, len(w.tags))
    w.tags.remove('tag1')
    self.assertEquals(2, len(w.tags))
    self.assertEquals('tag2', w.tags[0])

  def testParticipantRoles(self):
    w = self.wavelet
    self.assertEquals(wavelet.Participants.ROLE_FULL,
                      w.participants.get_role(ROBOT_NAME))
    w.participants.set_role(ROBOT_NAME, wavelet.Participants.ROLE_READ_ONLY)
    self.assertEquals(wavelet.Participants.ROLE_READ_ONLY,
                      w.participants.get_role(ROBOT_NAME))

  def testSerialize(self):
    self.blip.append(element.Gadget('http://test.com', {'a': 3}))
    self.wavelet.title = 'A wavelet title'
    self.blip.append(element.Image(url='http://www.google.com/logos/clickortreat1.gif',
                              width=320, height=118))
    self.blip.append(element.Attachment(caption='fake', data='fake data'))
    self.blip.append(element.Line(line_type='li', indent='2'))
    self.blip.append('bulleted!')
    self.blip.append(element.Installer(
        'http://wave-skynet.appspot.com/public/extensions/areyouin/manifest.xml'))
    self.wavelet.proxy_for('proxy').reply().append('hi from douwe')
    inlineBlip = self.blip.insert_inline_blip(5)
    inlineBlip.append('hello again!')

    serialized = self.wavelet.serialize()
    serialized = simplejson.dumps(serialized)
    self.assertTrue(serialized.find('test.com') > 0)

if __name__ == '__main__':
  unittest.main()
