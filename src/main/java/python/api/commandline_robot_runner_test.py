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

"""Tests for google3.walkabout.externalagents.api.commandline_robot_runner."""

__author__ = 'douwe@google.com (Douwe Osinga)'

import StringIO

from google3.pyglib import app
from google3.pyglib import flags

from google3.testing.pybase import googletest
from google3.walkabout.externalagents.api import commandline_robot_runner
from google3.walkabout.externalagents.api import events

FLAGS = flags.FLAGS


BLIP_JSON = ('{"wdykLROk*13":'
             '{"lastModifiedTime":1242079608457,'
             '"contributors":["someguy@test.com"],'
             '"waveletId":"test.com!conv+root",'
             '"waveId":"test.com!wdykLROk*11",'
             '"parentBlipId":null,'
             '"version":3,'
             '"creator":"someguy@test.com",'
             '"content":"\\nContent!",'
             '"blipId":"wdykLROk*13",'
             '"annotations":[{"range":{"start":0,"end":1},'
             '"name":"user/e/otherguy@test.com","value":"Other"}],'
             '"elements":{},'
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

TEST_JSON = '{"blips":%s,"wavelet":%s,"events":%s}' % (
    BLIP_JSON, WAVELET_JSON, EVENTS_JSON)


class CommandlineRobotRunnerTest(googletest.TestCase):

  def testSimpleFlow(self):
    FLAGS.eventdef_wavelet_participants_changed = 'x'
    flag = 'eventdef_' + events.WaveletParticipantsChanged.type.lower()
    setattr(FLAGS, flag, 'w.title="New title!"')
    input_stream = StringIO.StringIO(TEST_JSON)
    output_stream = StringIO.StringIO()
    commandline_robot_runner.run_bot(input_stream, output_stream)
    res = output_stream.getvalue()
    self.assertTrue('wavelet.setTitle' in res)


def main(unused_argv):
  googletest.main()


if __name__ == '__main__':
  app.run()
