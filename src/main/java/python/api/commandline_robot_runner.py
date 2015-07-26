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

"""Run robot from the commandline for testing.

This robot_runner let's you define event handlers using flags and takes the
json input from the std in and writes out the json output to stdout.

for example
  cat events | commandline_robot_runner.py \
      --eventdef-blip_submitted="wavelet.title='title'"
"""

__author__ = 'douwe@google.com (Douwe Osinga)'

import sys
import urllib

from google3.pyglib import app
from google3.pyglib import flags

from google3.walkabout.externalagents import api

from google3.walkabout.externalagents.api import blip
from google3.walkabout.externalagents.api import element
from google3.walkabout.externalagents.api import errors
from google3.walkabout.externalagents.api import events
from google3.walkabout.externalagents.api import ops
from google3.walkabout.externalagents.api import robot
from google3.walkabout.externalagents.api import util

FLAGS = flags.FLAGS

for event in events.ALL:
  flags.DEFINE_string('eventdef_' + event.type.lower(),
                      '',
                      'Event definition for the %s event' % event.type)


def handle_event(src, bot, e, w):
  """Handle an event by executing the source code src."""
  globs = {'e': e, 'w': w, 'api': api, 'bot': bot,
           'blip': blip, 'element': element, 'errors': errors,
           'events': events, 'ops': ops, 'robot': robot,
           'util': util}
  exec src in globs


def run_bot(input_file, output_file):
  """Run a robot defined on the command line."""
  cmdbot = robot.Robot('Commandline bot')
  for event in events.ALL:
    src = getattr(FLAGS, 'eventdef_' + event.type.lower())
    src = urllib.unquote_plus(src)
    if src:
      cmdbot.register_handler(event,
          lambda event, wavelet, src=src, bot=cmdbot:
              handle_event(src, bot, event, wavelet))
  json_body = unicode(input_file.read(), 'utf8')
  json_response = cmdbot.process_events(json_body)
  output_file.write(json_response)


def main(argv):
  run_bot(sys.stdin, sys.stdout)

if __name__ == '__main__':
  app.run()
