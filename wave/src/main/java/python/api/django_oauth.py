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

"""Filter and decorator to wave enable django sites.

If you want to require wave authentication for every handler, just add
WaveOAuthMiddleware to your middleware. If you only want to require
authentication for specific handlers, decorate those with @waveoauth.

In any wave authenticated handler the request object should have a
waveservice field that can be used to talk to wave.

You can specify the following in your settings:
WAVE_CONSUMER_KEY: the consumer key passed to the waveservice. defaults to
    anonymous if not set.
WAVE_CONSUMER_SECRET: the consumer key passed to the waveservice. defaults to
    anonymous if not set.
WAVE_USE_SANDBOX: whether to use the sandbox for this app. Defaults to false.
"""

from django.conf import settings
from django.http import HttpResponse, HttpResponseRedirect
from django.contrib.auth import authenticate

import base64
import logging
from functools import wraps

import waveservice


class WaveOAuthMiddleware(object):
  """Wave middleware to authenticate all requests at this site."""
  def process_request(self, request):
    return _oauth_helper(request)


def waveoauth(func):
  """Decorator used to specify that a handler requires wave authentication."""
  @wraps(func)
  def inner(request, *args, **kwargs):
    result = _oauth_helper(request)
    if result is not None:
      return result
    return func(request, *args, **kwargs)
  return inner


def _oauth_helper(request):
  "Check if we're authenticated and if not, execute the oauth dance."

  consumer_key = getattr(settings, 'WAVE_CONSUMER_KEY', 'anonymous')
  consumer_secret = getattr(settings, 'WAVE_CONSUMER_SECRET', 'anonymous')
  use_sandbox = getattr(settings, 'WAVE_USE_SANDBOX', False)

  service = waveservice.WaveService(
      consumer_key=consumer_key, consumer_secret=consumer_secret, use_sandbox=use_sandbox)

  access_token = request.COOKIES.get('WAVE_ACCESS_TOKEN')
  if access_token:
    service.set_access_token(access_token)
    request.waveservice = service
    return None

  # no access token. dance monkey dance.
  oauth_token = request.GET.get('oauth_token')
  verifier = request.GET.get('oauth_verifier')
  request_token = request.COOKIES.get('WAVE_REQUEST_TOKEN')
  meta = request.META

  # you'd think there would be something better than this madness:
  this_url = meta.get('HTTP_HOST')
  if not this_url:
    this_url = meta.get('SERVER_NAME')
    port = meta.get('SEVER_PORT')
    if port:
      this_url += ':' + port
  this_url += request.path
  schema = meta.get('wsgi.url_scheme', 'http')
  this_url = schema + '://' + this_url

  if not oauth_token or not verifier or not request_token:
    # we're here not returning from a callback. Start.
    request_token = service.fetch_request_token(callback=this_url)
    auth_url = service.generate_authorization_url()
    response = HttpResponseRedirect(auth_url)
    # set a session cookie
    response.set_cookie('WAVE_REQUEST_TOKEN', request_token.to_string())
    return response
  else:
    logging.info('upgrading to access token')
    access_token = service.upgrade_to_access_token(request_token=request_token,
                                                   verifier=verifier)
    # This redirect could be avoided if the caller would set the cookie. This way
    # however we keep the cgi arguments clean.
    response = HttpResponseRedirect(this_url)
    response.set_cookie('WAVE_ACCESS_TOKEN', access_token.to_string(), max_age=24*3600*365)
    return response
