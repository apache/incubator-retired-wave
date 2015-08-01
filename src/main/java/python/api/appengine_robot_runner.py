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

"""A module to run wave robots on app engine."""


import logging
import sys

import events

from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app


class CapabilitiesHandler(webapp.RequestHandler):
  """Handler to forward a request ot a handler of a robot."""

  def __init__(self, method, contenttype):
    """Initializes this handler with a specific robot."""
    self._method = method
    self._contenttype = contenttype

  def get(self):
    """Handles HTTP GET request."""
    self.response.headers['Content-Type'] = self._contenttype
    self.response.out.write(self._method())

class ProfileHandler(webapp.RequestHandler):
  """Handler to forward a request ot a handler of a robot."""

  def __init__(self, method, contenttype):
    """Initializes this handler with a specific robot."""
    self._method = method
    self._contenttype = contenttype

  def get(self):
    """Handles HTTP GET request."""
    self.response.headers['Content-Type'] = self._contenttype
    # Respond with proxied profile if name specified
    if self.request.get('name'):
      self.response.out.write(self._method(self.request.get('name')))
    else:
      self.response.out.write(self._method())

class RobotEventHandler(webapp.RequestHandler):
  """Handler for the dispatching of events to various handlers to a robot.

  This handler only responds to post events with a JSON post body. Its primary
  task is to separate out the context data from the events in the post body
  and dispatch all events in order. Once all events have been dispatched
  it serializes the context data and its associated operations as a response.
  """

  def __init__(self, robot):
    """Initializes self with a specific robot."""
    self._robot = robot

  def get(self):
    """Handles the get event for debugging.

    This is useful for debugging but since event bundles tend to be
    rather big it often won't fit for more complex requests.
    """
    ops = self.request.get('events')
    if ops:
      self.request.body = events
      self.post()

  def post(self):
    """Handles HTTP POST requests."""
    json_body = self.request.body
    if not json_body:
      # TODO(davidbyttow): Log error?
      return

    # Redirect stdout to stderr while executing handlers. This way, any stray
    # "print" statements in bot code go to the error logs instead of breaking
    # the JSON response sent to the HTTP channel.
    saved_stdout, sys.stdout = sys.stdout, sys.stderr

    json_body = unicode(json_body, 'utf8')
    logging.info('Incoming: %s', json_body)
    json_response = self._robot.process_events(json_body)
    logging.info('Outgoing: %s', json_response)

    sys.stdout = saved_stdout

    # Build the response.
    self.response.headers['Content-Type'] = 'application/json; charset=utf-8'
    self.response.out.write(json_response.encode('utf-8'))


def operation_error_handler(event, wavelet):
  """Default operation error handler, logging what went wrong."""
  if isinstance(event, events.OperationError):
    logging.error('Previously operation failed: id=%s, message: %s',
                  event.operation_id, event.error_message)


def appengine_post(url, data, headers):
  result = urlfetch.fetch(
      method='POST',
      url=url,
      payload=data,
      headers=headers,
      deadline=10)
  return result.status_code, result.content


class RobotVerifyTokenHandler(webapp.RequestHandler):
  """Handler for the token_verify request."""

  def __init__(self, robot):
    """Initializes self with a specific robot."""
    self._robot = robot

  def get(self):
    """Handles the get event for debugging. Ops usually too long."""
    token, st = self._robot.get_verification_token_info()
    logging.info('token=' + token)
    if token is None:
      self.error(404)
      self.response.out.write('No token set')
      return
    if st is not None:
      if self.request.get('st') != st:
        self.response.out.write('Invalid st value passed')
        return
    self.response.out.write(token)


def create_robot_webapp(robot, debug=False, extra_handlers=None):
  """Returns an instance of webapp.WSGIApplication with robot handlers."""
  if not extra_handlers:
    extra_handlers = []
  return webapp.WSGIApplication([('.*/_wave/capabilities.xml',
                                  lambda: CapabilitiesHandler(
                                                     robot.capabilities_xml,
                                                     'application/xml')),
                                 ('.*/_wave/robot/profile',
                                  lambda: ProfileHandler(
                                                     robot.profile_json,
                                                     'application/json')),
                                 ('.*/_wave/robot/jsonrpc',
                                  lambda: RobotEventHandler(robot)),
                                 ('.*/_wave/verify_token',
                                  lambda: RobotVerifyTokenHandler(robot)),
                                ] + extra_handlers,
                                debug=debug)


def run(robot, debug=False, log_errors=True, extra_handlers=None):
  """Sets up the webapp handlers for this robot and starts listening.

    A robot is typically setup in the following steps:
      1. Instantiate and define robot.
      2. Register various handlers that it is interested in.
      3. Call Run, which will setup the handlers for the app.
    For example:
      robot = Robot('Terminator',
                    image_url='http://www.sky.net/models/t800.png',
                    profile_url='http://www.sky.net/models/t800.html')
      robot.register_handler(WAVELET_PARTICIPANTS_CHANGED, KillParticipant)
      run(robot)

    Args:
      robot: the robot to run. This robot is modified to use app engines
          urlfetch for posting http.
      debug: Optional variable that defaults to False and is passed through
          to the webapp application to determine if it should show debug info.
      log_errors: Optional flag that defaults to True and determines whether
          a default handlers to catch errors should be setup that uses the
          app engine logging to log errors.
      extra_handlers: Optional list of tuples that are passed to the webapp
          to install more handlers. For example, passing
            [('/about', AboutHandler),] would install an extra about handler
            for the robot.
  """
  # App Engine expects to construct a class with no arguments, so we
  # pass a lambda that constructs the appropriate handler with
  # arguments from the enclosing scope.
  if log_errors:
    robot.register_handler(events.OperationError, operation_error_handler)
  robot.set_http_post(appengine_post)
  app = create_robot_webapp(robot, debug, extra_handlers)
  run_wsgi_app(app)
