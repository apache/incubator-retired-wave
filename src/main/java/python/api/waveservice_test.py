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
import waveservice
import simplejson
import testdata


class TestWavelet(unittest.TestCase):
  """Tests the wavelet class."""

  def setUp(self):
    self.waveservice = waveservice.WaveService()

  def testWaveletProperties(self):
    operation_queue = ops.OperationQueue()
    TEST_DATA = simplejson.loads(testdata.json_string)
    w = self.waveservice._wavelet_from_json(TEST_DATA,
                                            operation_queue)
    self.assertEquals(TEST_DATA['wavelet']['waveId'], w.wave_id)
    self.assertEquals(TEST_DATA['wavelet']['rootThread']['id'],
                      w.root_thread.id)
    self.assertEquals(TEST_DATA['wavelet']['rootThread']['location'],
                      w.root_thread.location)
    self.assertEquals(len(TEST_DATA['wavelet']['rootThread']['blipIds']),
                      len(w.root_thread.blips))

    b = w.root_blip
    self.assertEquals(len(TEST_DATA['blips']['b+IvD7RCuWB']['replyThreadIds']),
                      len(b.reply_threads))


  def testWaveletBlipMethods(self):
    operation_queue = ops.OperationQueue()
    TEST_DATA = simplejson.loads(testdata.json_string)
    w = self.waveservice._wavelet_from_json(TEST_DATA,
                                            operation_queue)
    root_blip = w.root_blip
    blip = root_blip.continue_thread()
    self.assertEquals(blip.parent_blip_id, root_blip.parent_blip_id)
    self.assertEquals(8, len(w.blips))
    self.assertEquals(4, len(w.root_thread.blips))



if __name__ == '__main__':
  unittest.main()
