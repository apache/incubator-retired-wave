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

"""Unit tests for the ops module."""


import unittest

import ops


class TestOperation(unittest.TestCase):
  """Test case for Operation class."""

  def testFields(self):
    op = ops.Operation(ops.WAVELET_SET_TITLE, 'opid02',
                       {'waveId': 'wavelet-id',
                        'title': 'a title'})
    self.assertEqual(ops.WAVELET_SET_TITLE, op.method)
    self.assertEqual('opid02', op.id)
    self.assertEqual(2, len(op.params))

  def testConstructModifyTag(self):
    q = ops.OperationQueue()
    op = q.wavelet_modify_tag('waveid', 'waveletid', 'tag')
    self.assertEqual(3, len(op.params))
    op = q.wavelet_modify_tag(
        'waveid', 'waveletid', 'tag', modify_how='remove')
    self.assertEqual(4, len(op.params))

  def testConstructRobotFetchWave(self):
    q = ops.OperationQueue('proxyid')
    op = q.robot_fetch_wave('wave1', 'wavelet1')
    self.assertEqual(3, len(op.params))
    self.assertEqual('proxyid', op.params['proxyingFor'])
    self.assertEqual('wave1', op.params['waveId'])
    self.assertEqual('wavelet1', op.params['waveletId'])
    op = q.robot_fetch_wave('wave1', 'wavelet1',
                            raw_deltas_from_version=5, return_raw_snapshot=True)
    self.assertEqual(5, len(op.params))
    self.assertEqual(5, op.params['rawDeltasFromVersion'])
    self.assertEqual(True, op.params['returnRawSnapshot'])

class TestOperationQueue(unittest.TestCase):
  """Test case for OperationQueue class."""

  def testSerialize(self):
    q = ops.OperationQueue()
    q.set_capability_hash('hash')
    op = q.wavelet_modify_tag('waveid', 'waveletid', 'tag')
    json = q.serialize()
    self.assertEqual(2, len(json))
    self.assertEqual('robot.notify', json[0]['method'])
    self.assertEqual('hash', json[0]['params']['capabilitiesHash'])
    self.assertEqual(ops.PROTOCOL_VERSION, json[0]['params']['protocolVersion'])
    self.assertEqual('wavelet.modifyTag', json[1]['method'])

if __name__ == '__main__':
  unittest.main()
