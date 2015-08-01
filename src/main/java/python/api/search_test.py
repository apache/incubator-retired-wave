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

"""Unit tests for the search module."""


import unittest

import search
import simplejson

TEST_DIGEST_DATA = {
    'title': 'Title',
    'participants': ['pamela.fox@googlewave.com'],
    'waveId': 'test.com!w+g3h3im',
    'snippet': 'Best test ever',
    'blipCount': '10',
    'unreadCount': '2',
    'lastModified': '1275658457',
}


TEST_RESULTS_DATA = {
    'query': 'in:inbox',
    'numResults': 10,
    'digests': [
      TEST_DIGEST_DATA,
      TEST_DIGEST_DATA
    ]}


class TestResults(unittest.TestCase):
  """Tests the wavelet class."""

  def setUp(self):
    self.results = search.Results(TEST_RESULTS_DATA)

  def testResultsProperties(self):
    r = self.results
    self.assertEquals(TEST_RESULTS_DATA['query'], r.query)
    self.assertEquals(TEST_RESULTS_DATA['numResults'], r.num_results)
    self.assertEquals(len(TEST_RESULTS_DATA['digests']), len(r.digests))


class TestDigest(unittest.TestCase):
  """Tests the wavelet class."""

  def setUp(self):
    self.digest = search.Digest(TEST_DIGEST_DATA)

  def testDigestProperties(self):
    d = self.digest
    self.assertEquals(TEST_DIGEST_DATA['title'], d.title)
    self.assertEquals(TEST_DIGEST_DATA['waveId'], d.wave_id)
    self.assertEquals(TEST_DIGEST_DATA['snippet'], d.snippet)
    self.assertEquals(TEST_DIGEST_DATA['blipCount'], str(d.blip_count))
    self.assertEquals(TEST_DIGEST_DATA['unreadCount'], str(d.unread_count))
    self.assertEquals(TEST_DIGEST_DATA['lastModified'], d.last_modified)
    self.assertTrue(TEST_DIGEST_DATA['participants'][0] in d.participants)
    self.assertEquals('test.com', d.domain)

if __name__ == '__main__':
  unittest.main()
