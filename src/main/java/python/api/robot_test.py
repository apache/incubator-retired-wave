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

"""Unit tests for the robot module."""

import unittest

import events
import ops
import robot
import simplejson

BLIP_JSON = ('{"wdykLROk*13":'
               '{"lastModifiedTime":1242079608457,'
               '"contributors":["someguy@test.com"],'
               '"waveletId":"test.com!conv+root",'
               '"waveId":"test.com!wdykLROk*11",'
               '"parentBlipId":null,'
               '"version":3,'
               '"creator":"someguy@test.com",'
               '"content":"\\nContent!",'
               '"blipId":"wdykLROk*13","'
               'annotations":[{"range":{"start":0,"end":1},'
                   '"name":"user/e/davidbyttow@google.com","value":"David"}],'
               '"elements":{},'
               '"threadId": "",'
               '"childBlipIds":[]}'
             '}')

WAVELET_JSON = ('{"lastModifiedTime":1242079611003,'
                 '"title":"A title",'
                 '"waveletId":"test.com!conv+root",'
                 '"rootBlipId":"wdykLROk*13",'
                 '"dataDocuments":null,'
                 '"creationTime":1242079608457,'
                 '"waveId":"test.com!wdykLROk*11",'
                 '"participants":["someguy@test.com","monty@appspot.com"],'
                 '"creator":"someguy@test.com",'
                 '"rootThread": '
                 '{"id":"", "location": "-1", "blipIds": ["wdykLROk*13"]},'
                 '"version":5}')

EVENTS_JSON = ('[{"timestamp":1242079611003,'
                '"modifiedBy":"someguy@test.com",'
                '"properties":{"participantsRemoved":[],'
                    '"participantsAdded":["monty@appspot.com"]},'
                '"type":"WAVELET_PARTICIPANTS_CHANGED"}]')

TEST_JSON = '{"blips":%s,"wavelet":%s,"events":%s, "threads": {}}' % (
    BLIP_JSON, WAVELET_JSON, EVENTS_JSON)

NEW_WAVE_JSON = [{"data":
                  {"waveletId": "wavesandbox.com!conv+root",
                   "blipId": "b+LrODcLZkDlu", "waveId":
                   "wavesandbox.com!w+LrODcLZkDlt"},
                  "id": "op2"}]


class TestRobot(unittest.TestCase):
  """Tests for testing the basic parsing of json in robots."""

  def setUp(self):
    self.robot = robot.Robot('Testy')

  def testCreateWave(self):
    self.robot.get_waveservice().submit = lambda x: NEW_WAVE_JSON
    new_wave = self.robot.new_wave('wavesandbox.com', submit=True)
    self.assertEqual('wavesandbox.com!w+LrODcLZkDlt', new_wave.wave_id)

  def testEventParsing(self):
    def check(event, wavelet):
      # Test some basic properties; the rest should be covered by
      # ops.CreateContext.
      root = wavelet.root_blip
      self.assertEqual(1, len(wavelet.blips))
      self.assertEqual('wdykLROk*13', root.blip_id)
      self.assertEqual('test.com!wdykLROk*11', root.wave_id)
      self.assertEqual('test.com!conv+root', root.wavelet_id)
      self.assertEqual('WAVELET_PARTICIPANTS_CHANGED', event.type)
      self.assertEqual({'participantsRemoved': [],
                        'participantsAdded': ['monty@appspot.com']},
                       event.properties)
      self.robot.test_called = True

    self.robot.test_called = False
    self.robot.register_handler(events.WaveletParticipantsChanged,
                                check)
    json = self.robot.process_events(TEST_JSON)
    self.assertTrue(self.robot.test_called)
    operations = simplejson.loads(json)
    # there should be one operation indicating the current version:
    self.assertEqual(1, len(operations))

  def testWrongEventsIgnored(self):
    self.robot.test_called = True

    def check(event, wavelet):
      called = True

    self.robot.test_called = False
    self.robot.register_handler(events.BlipSubmitted,
                                check)
    self.robot.process_events(TEST_JSON)
    self.assertFalse(self.robot.test_called)

  def testOperationParsing(self):
    def check(event, wavelet):
      wavelet.reply()
      wavelet.title = 'new title'
      wavelet.root_blip.append_markup('<b>Hello</b>')

    self.robot.register_handler(events.WaveletParticipantsChanged,
                                check)
    json = self.robot.process_events(TEST_JSON)
    operations = simplejson.loads(json)
    expected = set([ops.ROBOT_NOTIFY,
                    ops.WAVELET_APPEND_BLIP,
                    ops.WAVELET_SET_TITLE,
                    ops.DOCUMENT_APPEND_MARKUP])
    methods = [operation['method'] for operation in operations]
    for method in methods:
      self.assertTrue(method in expected)
      expected.remove(method)
    self.assertEquals(0, len(expected))

  def testSerializeWavelets(self):
    wavelet = self.robot.blind_wavelet(TEST_JSON)
    serialized = wavelet.serialize()
    unserialized = self.robot.blind_wavelet(serialized)
    self.assertEquals(wavelet.creator, unserialized.creator)
    self.assertEquals(wavelet.creation_time, unserialized.creation_time)
    self.assertEquals(wavelet.last_modified_time,
                      unserialized.last_modified_time)
    self.assertEquals(wavelet.root_blip.blip_id, unserialized.root_blip.blip_id)
    self.assertEquals(wavelet.title, unserialized.title)
    self.assertEquals(wavelet.wave_id, unserialized.wave_id)
    self.assertEquals(wavelet.wavelet_id, unserialized.wavelet_id)
    self.assertEquals(wavelet.domain, unserialized.domain)

  def testProxiedBlindWavelet(self):
    def handler(event, wavelet):
      blind_wavelet = self.robot.blind_wavelet(TEST_JSON, 'proxyid')
      blind_wavelet.reply()
      blind_wavelet.submit_with(wavelet)

    self.robot.register_handler(events.WaveletParticipantsChanged, handler)
    json = self.robot.process_events(TEST_JSON)
    operations = simplejson.loads(json)

    self.assertEqual(2, len(operations))
    self.assertEquals(ops.ROBOT_NOTIFY,
                      operations[0]['method'])
    self.assertEquals(ops.WAVELET_APPEND_BLIP, operations[1]['method'])
    self.assertEquals('proxyid', operations[1]['params']['proxyingFor'])

  def testCapabilitiesHashIncludesContextAndFilter(self):
    robot1 = robot.Robot('Robot1')
    robot1.register_handler(events.WaveletSelfAdded, lambda: '')

    robot2 = robot.Robot('Robot2')
    robot2.register_handler(events.WaveletSelfAdded, lambda: '',
                            context=events.Context.ALL)
    self.assertNotEqual(robot1.capabilities_hash(), robot2.capabilities_hash())

    robot3 = robot.Robot('Robot3')
    robot2.register_handler(events.WaveletSelfAdded, lambda: '',
                            context=events.Context.ALL, filter="foo")
    self.assertNotEqual(robot1.capabilities_hash(), robot2.capabilities_hash())
    self.assertNotEqual(robot1.capabilities_hash(), robot3.capabilities_hash())
    self.assertNotEqual(robot2.capabilities_hash(), robot3.capabilities_hash())

class TestGetCapabilitiesXml(unittest.TestCase):

  def setUp(self):
    self.robot = robot.Robot('Testy')
    self.robot.capabilities_hash = lambda: '1'

  def assertStringsEqual(self, s1, s2):
    self.assertEqual(s1, s2, 'Strings differ:\n%s--\n%s' % (s1, s2))

  def testDefault(self):
    expected = (
        '<?xml version="1.0"?>\n'
        '<w:robot xmlns:w="http://wave.google.com/extensions/robots/1.0">\n'
        '<w:version>1</w:version>\n'
        '<w:protocolversion>%s</w:protocolversion>\n'
        '<w:capabilities>\n</w:capabilities>\n'
        '</w:robot>\n') % ops.PROTOCOL_VERSION
    xml = self.robot.capabilities_xml()
    self.assertStringsEqual(expected, xml)

  def testUrls(self):
    profile_robot = robot.Robot(
        'Testy',
        image_url='http://example.com/image.png',
        profile_url='http://example.com/profile.xml')
    profile_robot.capabilities_hash = lambda: '1'
    expected = (
        '<?xml version="1.0"?>\n'
        '<w:robot xmlns:w="http://wave.google.com/extensions/robots/1.0">\n'
        '<w:version>1</w:version>\n'
        '<w:protocolversion>%s</w:protocolversion>\n'
        '<w:capabilities>\n</w:capabilities>\n'
        '</w:robot>\n') % ops.PROTOCOL_VERSION
    xml = profile_robot.capabilities_xml()
    self.assertStringsEqual(expected, xml)

  def testConsumerKey(self):
    # setup_oauth doesn't work during testing, so heavy handed setting of
    # properties it is:
    self.robot._consumer_key = 'consumer'
    expected = (
        '<?xml version="1.0"?>\n'
        '<w:robot xmlns:w="http://wave.google.com/extensions/robots/1.0">\n'
        '<w:version>1</w:version>\n'
        '<w:consumer_key>consumer</w:consumer_key>\n'
        '<w:protocolversion>%s</w:protocolversion>\n'
        '<w:capabilities>\n</w:capabilities>\n'
        '</w:robot>\n') % ops.PROTOCOL_VERSION
    xml = self.robot.capabilities_xml()
    self.assertStringsEqual(expected, xml)

  def testCapsAndEvents(self):
    self.robot.register_handler(events.BlipSubmitted, None,
                                context=[events.Context.SELF,
                                         events.Context.ROOT])
    expected = (
        '<?xml version="1.0"?>\n'
        '<w:robot xmlns:w="http://wave.google.com/extensions/robots/1.0">\n'
        '<w:version>1</w:version>\n'
        '<w:protocolversion>%s</w:protocolversion>\n'
        '<w:capabilities>\n'
        '  <w:capability name="%s" context="SELF,ROOT"/>\n'
        '</w:capabilities>\n'
        '</w:robot>\n') % (ops.PROTOCOL_VERSION, events.BlipSubmitted.type)
    xml = self.robot.capabilities_xml()
    self.assertStringsEqual(expected, xml)


if __name__ == '__main__':
  unittest.main()
