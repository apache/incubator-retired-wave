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

"""Base class to use OAuth to talk to the wave service."""

import httplib
import logging
import urllib
import urlparse

import oauth
import simplejson

import ops
import blip
import errors
import events
import search
import util
import wavelet


class WaveService(object):
  # Google OAuth URLs
  REQUEST_TOKEN_URL = 'https://www.google.com/accounts/OAuthGetRequestToken'
  ACCESS_TOKEN_URL = 'https://www.google.com/accounts/OAuthGetAccessToken'
  AUTHORIZATION_URL = 'https://www.google.com/accounts/OAuthAuthorizeToken'

  # Wave OAuth URLS
  SCOPE = 'http://wave.googleusercontent.com/api/rpc'
  SIGNATURE_METHOD = oauth.OAuthSignatureMethod_HMAC_SHA1()

  # Wave RPC URLs
  RPC_URL = 'https://www-opensocial.googleusercontent.com/api/rpc'
  SANDBOX_RPC_URL = (
      'https://www-opensocial-sandbox.googleusercontent.com/api/rpc')

  def __init__(self, use_sandbox=False, server_rpc_base=None,
               consumer_key='anonymous', consumer_secret='anonymous',
               http_post=None):
    """Initializes a service that can perform the various OAuth steps.

    Args:
      use_sandbox: A boolean indicating whether to use Wave Sandbox URLs
      server_rpc_base: optional explicit url to use for rpc,
          overriding use_sandbox.
      consumer_key: A string for the consumer key, defaults to 'anonymous'
      consumer_secret: A string for the consumer secret, defaults to 'anonymous'
      http_post: handler to call to execute a http post.
    """
    self._consumer = oauth.OAuthConsumer(consumer_key, consumer_secret)
    logging.info('server_rpc_base: %s', server_rpc_base)
    if server_rpc_base:
      self._server_rpc_base = server_rpc_base
    elif use_sandbox:
      self._server_rpc_base = WaveService.SANDBOX_RPC_URL
    else:
      self._server_rpc_base = WaveService.RPC_URL
    logging.info('server:' + self._server_rpc_base)

    self._http_post = self.http_post
    self._connection = httplib.HTTPSConnection('www.google.com')
    self._access_token = None

  def _make_token(self, token):
    """If passed an oauth token, return that. If passed a string, convert."""
    if isinstance(token, basestring):
      return oauth.OAuthToken.from_string(token)
    else:
      return token

  def set_http_post(self, http_post):
    """Set the http_post handler to use when posting."""
    self._http_post = http_post

  def get_token_from_request(self, oauth_request):
    """Convenience function to returning the token from a request.

    Args:
      oauth_request: An OAuthRequest object
    Returns:
      An OAuthToken object
    """
    # Send request to the request token URL
    self._connection.request(oauth_request.http_method, oauth_request.to_url())

    # Extract token from response
    response = self._connection.getresponse().read()
    self._request_token = oauth.OAuthToken.from_string(response)
    return self._request_token

  def fetch_request_token(self, callback=None):
    """Fetches the request token to start the oauth dance.

    Args:
      callback: the URL to where the service will redirect to after
          access is granted.
    Returns:
      An OAuthToken object
    """
    # Create and sign OAuth request
    params = {'scope': WaveService.SCOPE}
    if callback:
      params['oauth_callback'] = callback
    oauth_request = oauth.OAuthRequest.from_consumer_and_token(self._consumer,
        http_url=WaveService.REQUEST_TOKEN_URL, parameters=params)
    oauth_request.sign_request(WaveService.SIGNATURE_METHOD, self._consumer, None)

    return self.get_token_from_request(oauth_request)

  def generate_authorization_url(self, request_token=None):
    """Generates the authorization URL (Step 2).

    Args:
      request_token: An OAuthToken object
    Returns:
      An authorization URL
    """
    # Create Authorization URL request
    if request_token is None:
      request_token = self._request_token
    oauth_request = oauth.OAuthRequest.from_token_and_callback(
        token=request_token, http_url=WaveService.AUTHORIZATION_URL)

    # Send request
    self._connection.request(oauth_request.http_method, oauth_request.to_url())

    # Extract location from the response
    response = self._connection.getresponse()
    return response.getheader('location')

  def upgrade_to_access_token(self, request_token, verifier=None):
    """Upgrades the request_token to an access token (Step 3).

    Args:
      request_token: An OAuthToken object or string
      verifier: A verifier string
    Returns:
      An OAuthToken object
    """
    request_token = self._make_token(request_token)
    params = {}
    if verifier:
      params['oauth_verifier'] = verifier
    oauth_request = oauth.OAuthRequest.from_consumer_and_token(self._consumer,
        token=request_token, http_url=WaveService.ACCESS_TOKEN_URL,
        parameters=params)
    oauth_request.sign_request(WaveService.SIGNATURE_METHOD, self._consumer,
                               request_token)

    self._access_token = self.get_token_from_request(oauth_request)
    return self._access_token

  def set_access_token(self, access_token):
    self._access_token = self._make_token(access_token)

  def http_post(self, url, data, headers):
    """Execute an http post.

    You can provide a different method to use in the constructor. This
    is mostly useful when running on app engine and you want to set
    the time out to something different than the default 5 seconds.

    Args:
        url: to post to
        body: post body
        headers: extra headers to pass along
    Returns:
        response_code, returned_page
    """
    import urllib2
    req = urllib2.Request(url,
                          data=data,
                          headers=headers)
    try:
      f = urllib2.urlopen(req)
      return f.code, f.read()
    except urllib2.HTTPError, e:
      return e.code, e.read()

  def make_rpc(self, operations):
    """Make an rpc call, submitting the specified operations."""

    rpc_host = urlparse.urlparse(self._server_rpc_base).netloc

    # We either expect an operationqueue, a single op or a list
    # of ops:
    if (not isinstance(operations, ops.OperationQueue)):
      if not isinstance(operations, list):
        operations = [operations]
      queue = ops.OperationQueue()
      queue.copy_operations(operations)
    else:
      queue = operations


    data = simplejson.dumps(queue.serialize(method_prefix='wave'))

    oauth_request = oauth.OAuthRequest.from_consumer_and_token(self._consumer,
         token=self._access_token, http_method='POST',
         http_url=self._server_rpc_base)
    oauth_request.sign_request(WaveService.SIGNATURE_METHOD,
         self._consumer, self._access_token)

    logging.info('Active URL: %s'  % self._server_rpc_base)
    logging.info('Active Outgoing: %s' % data)
    headers = {'Content-Type': 'application/json'}
    headers.update(oauth_request.to_header());
    status, content = self._http_post(
         url=self._server_rpc_base,
         data=data,
         headers=headers)

    if status != 200:
      raise errors.RpcError('code: %s\n%s' % (status, content))
    return simplejson.loads(content)

  def _first_rpc_result(self, result):
    """result is returned from make_rpc. Get the first data record
    or throw an exception if it was an error. Ignore responses to
    NOTIFY_OP_ID."""
    result = [record for record in result if record['id'] != ops.NOTIFY_OP_ID]
    if not result:
      raise errors.RpcError('No results found.')
    result = result[0]
    error = result.get('error')
    if error:
      raise errors.RpcError(str(error['code'])
          + ': ' + error['message'])
    data = result.get('data')
    if data is not None:
      return data
    raise errors.Error('RPC Error: No data record.')

  def _wavelet_from_json(self, json, pending_ops):
    """Construct a wavelet from the passed json.

    The json should either contain a wavelet and a blips record that
    define those respective object. The returned wavelet
    will be constructed using the passed pending_ops
    OperationQueue.
    Alternatively the json can be the result of a previous
    wavelet.serialize() call. In that case the blips will
    be contaned in the wavelet record.
    """
    if isinstance(json, basestring):
      json = simplejson.loads(json)

    # Create blips dict so we can pass into BlipThread objects
    blips = {}

    # Setup threads first, as the Blips and Wavelet need to know about them
    threads = {}
    # In case of blind_wavelet or new_wave, we may not have threads indo
    threads_data = json.get('threads', {})
    # Create remaining thread objects
    for thread_id, raw_thread_data in threads_data.items():
      threads[thread_id] = wavelet.BlipThread(thread_id,
          raw_thread_data.get('location'), raw_thread_data.get('blipIds', []),
          blips, pending_ops)

    # If being called from blind_wavelet, wavelet is top level info
    if 'wavelet' in json:
      raw_wavelet_data = json['wavelet']
    elif 'waveletData' in json:
      raw_wavelet_data = json['waveletData']
    else:
      raw_wavelet_data = json
    root_thread_data = raw_wavelet_data.get('rootThread')
    root_thread = wavelet.BlipThread('',
                             root_thread_data.get('location'),
                             root_thread_data.get('blipIds', []),
                             blips,
                             pending_ops)
    threads[''] = root_thread

    # Setup the blips, pass  in reply threads
    for blip_id, raw_blip_data in json['blips'].items():
      reply_threads = [threads[id] for id in raw_blip_data.get('replyThreadIds',
                                                               [])]
      thread = threads.get(raw_blip_data.get('threadId'))
      blips[blip_id] = blip.Blip(raw_blip_data, blips, pending_ops,
                                 thread=thread, reply_threads=reply_threads)

    result = wavelet.Wavelet(raw_wavelet_data, blips, root_thread, pending_ops,
                             raw_deltas=json.get('rawDeltas'))

    robot_address = json.get('robotAddress')
    if robot_address:
      result.robot_address = robot_address

    return result

  def search(self, query, index=None, num_results=None):
    """Execute a search request.

    Args:
      query: what to search for, for example [in:inbox]
      index: index of the first result to return
      num_results: how many results to return
    """
    operation_queue = ops.OperationQueue()
    operation_queue.robot_search(query, index, num_results)
    result = self._first_rpc_result(self.make_rpc(operation_queue))
    return search.Results(result)

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
    util.check_is_valid_proxy_for_id(proxy_for_id)
    operation_queue = ops.OperationQueue(proxy_for_id)
    if not isinstance(message, basestring):
      message = simplejson.dumps(message)

    # Create temporary wavelet data
    blip_data, wavelet_data = operation_queue.robot_create_wavelet(
        domain=domain,
        participants=participants,
        message=message)

    # Create temporary blips dictionary
    blips = {}
    root_blip = blip.Blip(blip_data, blips, operation_queue)
    blips[root_blip.blip_id] = root_blip

    if submit:
      # Submit operation to server and return actual wave/blip IDs
      temp_wavelet = wavelet.Wavelet(wavelet_data,
                                blips=blips,
                                root_thread=None,
                                operation_queue=operation_queue)
      result = self._first_rpc_result(self.submit(temp_wavelet))
      if isinstance(result, list):
        result = result[0]
      if 'blipId' in result:
        blip_data['blipId'] = result['blipId']
        wavelet_data['rootBlipId'] = result['blipId']
      for field in 'waveId', 'waveletId':
        if field in result:
          wavelet_data[field] = result[field]
          blip_data[field] = result[field]
      blips = {}
      root_blip = blip.Blip(blip_data, blips, operation_queue)
      blips[root_blip.blip_id] = root_blip

    root_thread = wavelet.BlipThread('',
                             -1,
                             [root_blip.blip_id],
                             blips,
                             operation_queue)
    new_wavelet = wavelet.Wavelet(wavelet_data,
                              blips=blips,
                              root_thread=root_thread,
                              operation_queue=operation_queue)
    return new_wavelet

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

    Args:
      wave_id: the wave id
      wavelet_id: the wavelet_id
      proxy_for_id: on whose behalf to execute the operation
      raw_deltas_from_version: If specified, return a raw dump of the
        delta history of this wavelet, starting at the given version.
        This may return only part of the history; use additional
        requests with higher raw_deltas_from_version parameters to
        get the rest.
      return_raw_snapshot: if true, return the raw data for this
        wavelet.
    """
    util.check_is_valid_proxy_for_id(proxy_for_id)
    if not wavelet_id:
      domain, id = wave_id.split('!', 1)
      wavelet_id = domain + '!conv+root'
    operation_queue = ops.OperationQueue(proxy_for_id)
    operation_queue.robot_fetch_wave(wave_id, wavelet_id,
        raw_deltas_from_version, return_raw_snapshot)
    result = self._first_rpc_result(self.make_rpc(operation_queue))
    return self._wavelet_from_json(result, ops.OperationQueue(proxy_for_id))

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
    util.check_is_valid_proxy_for_id(proxy_for_id)
    return self._wavelet_from_json(json, ops.OperationQueue(proxy_for_id))

  def submit(self, wavelet_to_submit):
    """Submit the pending operations associated with wavelet_to_submit.

    Typically the wavelet will be the result of fetch_wavelet, blind_wavelet
    or new_wave.
    """
    pending = wavelet_to_submit.get_operation_queue()
    res = self.make_rpc(pending)
    pending.clear()
    return res
