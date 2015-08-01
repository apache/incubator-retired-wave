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

"""Defines classes that are needed to model a search (results)."""

import errors
import logging
import wavelet

class Results(object):
  """Models a set of search results.

  Search results are composed of a list of digests, query, and number of
  results.
  """

  def __init__(self, json):
    """Inits this results object with JSON data.

    Args:
      json: JSON data dictionary from Wave server.
    """
    if 'searchResults' in json:
      json = json['searchResults']
    self._query = json.get('query')
    self._num_results = json.get('numResults')
    self._digests = []
    self._digests = [Digest(digest_data) for digest_data in json['digests']]

  @property
  def query(self):
    """Returns the query for this search."""
    return self._query

  @property
  def num_results(self):
    """Returns the number of results for this search."""
    return self._num_results

  @property
  def digests(self):
    """Returns a list of digests."""
    return self._digests

  def __iter__(self):
    """Iterate over the list of digests."""
    return iter(self._digests)

  def serialize(self):
    """Return a dict of the search results properties."""
    return {'query': self._query,
            'numResults': self._num_results,
            'digests': [digest.serialize() for digest in self._digests]
           }


class Digest(object):
  """Models a single digest.

  A digest is composed of title, wave ID, snippet, and participants.
  """

  def __init__(self, json):
    """Inits this digest with JSON data.

    Args:
      json: JSON data dictionary from Wave server.
    """
    self._wave_id = json.get('waveId')
    self._title = json.get('title')
    self._snippet = json.get('snippet')
    self._blip_count = int(json.get('blipCount'))
    self._unread_count = int(json.get('unreadCount'))
    self._last_modified = json.get('lastModified')
    self._participants = wavelet.Participants(json.get('participants', []),
                                      {},
                                      self._wave_id,
                                      '',
                                      None)
    self._raw_data = json

  @property
  def blip_count(self):
    """Returns the number of blips in this wave."""
    return self._blip_count

  @property
  def unread_count(self):
    """Returns the number of unread blips in this wave."""
    return self._unread_count

  @property
  def last_modified(self):
    """Returns the last modified date of the wave."""
    return self._last_modified

  @property
  def wave_id(self):
    """Returns the digest wave id."""
    return self._wave_id

  @property
  def snippet(self):
    """Returns the snippet for the digest."""
    return self._snippet

  @property
  def domain(self):
    """Return the domain that the wave belongs to."""
    p = self._wave_id.find('!')
    if p == -1:
      return None
    else:
      return self._wave_id[:p]

  @property
  def participants(self):
    """Returns a set of participants on this wave."""
    return self._participants

  @property
  def title(self):
    return self._title

  def serialize(self):
    """Return a dict of the digest properties."""
    return {'waveId': self._wave_id,
            'participants': self._participants.serialize(),
            'title': self._title,
            'snippet': self._snippet,
            'blipCount': self._blip_count,
            'unreadCount': self._unread_count,
            'lastModified': self._last_modified,
           }

  def __str__(self):
    return self._title
