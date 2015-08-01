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

"""Defines the generic robot classes.

This module provides the Robot class and RobotListener interface,
as well as some helper functions for web requests and responses.
"""

import logging
import sys

try:
  __import__('google3') # setup internal test environment
except ImportError:
  pass

import simplejson
import blip
import errors
import ops
import simplejson
import wavelet
import waveservice

DEFAULT_PROFILE_URL = (
    'http://code.google.com/apis/wave/extensions/robots/python-tutorial.html')


class Robot(object):
  """Robot metadata class.

  This class holds on to basic robot information like the name and profile.
  It also maintains the list of event handlers and cron jobs and
  dispatches events to the appropriate handlers.
  """

  def __init__(self, name, image_url='', profile_url=DEFAULT_PROFILE_URL):
    """Initializes self with robot information.

    Args:
      name: The name of the robot
      image_url: (optional) url of an image that should be used as the avatar
          for this robot.
      profile_url: (optional) url of a webpage with more information about
          this robot.
    """
    self._handlers = {}
    self._name = name
    self._verification_token = None
    self._st = None
    self._waveservice = waveservice.WaveService()
    self._profile_handler = None
    self._image_url = image_url
    self._profile_url = profile_url
    self._capability_hash = 0
    self._consumer_key = None
    self._http_post = None

  @property
  def name(self):
    """Returns the name of the robot."""
    return self._name

  @property
  def image_url(self):
    """Returns the URL of the avatar image."""
    return self._image_url

  @property
  def profile_url(self):
    """Returns the URL of an info page for the robot."""
    return self._profile_url

  def get_verification_token_info(self):
    """Returns the verification token and ST parameter."""
    return self._verification_token, self._st

  def get_waveservice(self):
    """Return the currently installed waveservice if available.

    Raises:
      Error: if no service is installed.
    """
    if self._waveservice is None:
      raise errors.Error('Oauth has not been setup')
    return self._waveservice

  def capabilities_hash(self):
    """Return the capabilities hash as a hex string."""
    return hex(self._capability_hash)

  def register_handler(self, event_class, handler, context=None, filter=None):
    """Registers a handler on a specific event type.

    Multiple handlers may be registered on a single event type and are
    guaranteed to be called in order of registration.

    The handler takes two arguments, the event object and the corresponding
    wavelet.

    Args:
      event_class: An event to listen for from the classes defined in the
          events module.

      handler: A function handler which takes two arguments, the wavelet for
          the event and the event object.

      context: The context to provide for this handler.

      filter: Depending on the event, a filter can be specified that restricts
          for which values the event handler will be called from the server.
          Valuable to restrict the amount of traffic send to the robot.
    """
    payload = (handler, event_class, context, filter)
    self._handlers.setdefault(event_class.type, []).append(payload)
    if isinstance(context, list):
      context = ','.join(context)
    self._capability_hash = (self._capability_hash * 13 +
                             hash(ops.PROTOCOL_VERSION) +
                             hash(event_class.type) +
                             hash(context) +
                             hash(filter)) & 0xfffffff

  def set_verification_token_info(self, token, st=None):
    """Set the verification token used in the ownership verification.

    /wave/robot/register starts this process up and will produce this token.

    Args:
      token: the token provided by /wave/robot/register.

      st: optional parameter to verify the request for the token came from
          the wave server.
    """
    self._verification_token = token
    self._st = st

  def set_http_post(self, http_post):
    """Set the http_post handler to use when posting."""
    self._http_post = http_post
    if self._waveservice:
      self._waveservice.set_http_post(http_post)

  def setup_oauth(self, consumer_key, consumer_secret,
      server_rpc_base='https://www-opensocial.googleusercontent.com/api/rpc'):
    """Configure this robot to use the oauth'd json rpc.

    Args:
      consumer_key: consumer key received from the verification process.

      consumer_secret: secret received from the verification process.

      server_rpc_base: url of the rpc gateway to use. Specify None for default.
          For wave preview, https://www-opensocial.googleusercontent.com/api/rpc
          should be used.
          For wave sandbox,
          https://www-opensocial-sandbox.googleusercontent.com/api/rpc should be used.
    """

    consumer_key_prefix = ''
    # NOTE(ljvderijk): Present for backwards capability.
    if server_rpc_base in [waveservice.WaveService.SANDBOX_RPC_URL,
                           waveservice.WaveService.RPC_URL]:
      consumer_key_prefix = 'google.com:'

    self._consumer_key = consumer_key_prefix + consumer_key
    self._waveservice = waveservice.WaveService(
        consumer_key=consumer_key,
        consumer_secret=consumer_secret,
        server_rpc_base=server_rpc_base,
        http_post=self._http_post)

  def register_profile_handler(self, handler):
    """Sets the profile handler for this robot.

    The profile handler will be called when a profile is needed. The handler
    gets passed the name for which a profile is needed or None for the
    robot itself. A dictionary with keys for name, imageUrl and
    profileUrl should be returned.
    """
    self._profile_handler = handler

  def capabilities_xml(self):
    """Return this robot's capabilities as an XML string."""
    lines = []
    for capability, payloads in self._handlers.items():
      for payload in payloads:
        handler, event_class, context, filter = payload
        line = '  <w:capability name="%s"' % capability
        if context:
          if isinstance(context, list):
            context = ','.join(context)
          line += ' context="%s"' % context
        if filter:
          line += ' filter="%s"' % filter
        line += '/>\n'
        lines.append(line)
    if self._consumer_key:
      oauth_tag = '<w:consumer_key>%s</w:consumer_key>\n' % self._consumer_key
    else:
      oauth_tag = ''
    return ('<?xml version="1.0"?>\n'
            '<w:robot xmlns:w="http://wave.google.com/extensions/robots/1.0">\n'
            '<w:version>%s</w:version>\n'
            '%s'
            '<w:protocolversion>%s</w:protocolversion>\n'
            '<w:capabilities>\n'
            '%s'
            '</w:capabilities>\n'
            '</w:robot>\n') % (self.capabilities_hash(),
                               oauth_tag,
                               ops.PROTOCOL_VERSION,
                               '\n'.join(lines))

  def profile_json(self, name=None):
    """Returns a JSON representation of the profile.

    This method is called both for the basic profile of the robot and to
    get a proxying for profile, in which case name is set. By default
    the information supplied at registration is returned.

    Use register_profile_handler to override this default behavior.
    """
    if self._profile_handler:
      data = self._profile_handler(name)
    else:
      data = {'name': self.name,
              'imageUrl': self.image_url,
              'profileUrl': self.profile_url}
    return simplejson.dumps(data)

  def process_events(self, json):
    """Process an incoming set of events encoded as json."""
    parsed = simplejson.loads(json)
    pending_ops = ops.OperationQueue()
    event_wavelet = self.get_waveservice()._wavelet_from_json(parsed, pending_ops)

    for event_data in parsed['events']:
      for payload in self._handlers.get(event_data['type'], []):
        handler, event_class, context, filter = payload
        event = event_class(event_data, event_wavelet)
        handler(event, event_wavelet)

    pending_ops.set_capability_hash(self.capabilities_hash())
    return simplejson.dumps(pending_ops.serialize())

  def new_wave(self, domain, participants=None, message='', proxy_for_id=None,
               submit=False):
    """Create a new wave with the initial participants on it.

    A new wave is returned with its own operation queue. It the
    responsibility of the caller to make sure this wave gets
    submitted to the server, either by calling robot.submit() or
    by calling .submit_with() on the returned wave.

    Args:
      domain: the domain to create the wavelet on. This should
          in general correspond to the domain of the incoming
          wavelet. (wavelet.domain). Exceptions are situations
          where the robot is calling new_wave outside of an
          event or when the server is handling multiple domains.

      participants: initial participants on the wave. The robot
          as the creator of the wave is always added.

      message: a string that will be passed back to the robot
          when the WAVELET_CREATOR event is fired. This is a
          lightweight way to pass around state.

      submit: if true, use the active gateway to make a round
          trip to the server. This will return immediately an
          actual waveid/waveletid and blipId for the root blip.

    """
    return self.get_waveservice().new_wave(
        domain, participants, message, proxy_for_id, submit)

  def fetch_wavelet(self, wave_id, wavelet_id=None, proxy_for_id=None,
                    raw_deltas_from_version=-1, return_raw_snapshot=False):
    """Use the REST interface to fetch a wave and return it.

    The returned wavelet contains a snapshot of the state of the
    wavelet at that point. It can be used to modify the wavelet,
    but the wavelet might change in between, so treat carefully.

    Also note that the wavelet returned has its own operation
    queue. It the responsibility of the caller to make sure this
    wavelet gets submited to the server, either by calling
    robot.submit() or by calling .submit_with() on the returned
    wavelet.
    """
    return self.get_waveservice().fetch_wavelet(
        wave_id, wavelet_id, proxy_for_id, raw_deltas_from_version,
        return_raw_snapshot)

  def blind_wavelet(self, json, proxy_for_id=None):
    """Construct a blind wave from a json string.

    Call this method if you have a snapshot of a wave that you
    want to operate on outside of an event. Since the wave might
    have changed since you last saw it, you should take care to
    submit operations that are as safe as possible.

    Args:
      json: a json object or string containing at least a key
        wavelet defining the wavelet and a key blips defining the
        blips in the view.

      proxy_for_id: the proxying information that will be set on the wavelet's
        operation queue.

    Returns:
      A new wavelet with its own operation queue. It the
      responsibility of the caller to make sure this wavelet gets
      submited to the server, either by calling robot.submit() or
      by calling .submit_with() on the returned wavelet.
    """
    return self.get_waveservice().blind_wavelet(json, proxy_for_id)

  def submit(self, wavelet_to_submit):
    """Submit the pending operations associated with wavelet_to_submit.

    Typically the wavelet will be the result of fetch_wavelet, blind_wavelet
    or new_wave.
    """
    return self.get_waveservice().submit(wavelet_to_submit)
