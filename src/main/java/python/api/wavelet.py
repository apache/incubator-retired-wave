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

"""Defines classes that are needed to model a wavelet."""

import blip
import errors
import util


class DataDocs(object):
  """Class modeling a bunch of data documents in pythonic way."""

  def __init__(self, init_docs, wave_id, wavelet_id, operation_queue):
    self._docs = init_docs
    self._wave_id = wave_id
    self._wavelet_id = wavelet_id
    self._operation_queue = operation_queue

  def __iter__(self):
    return self._docs.__iter__()

  def __contains__(self, key):
    return key in self._docs

  def __delitem__(self, key):
    if not key in self._docs:
      return
    self._operation_queue.wavelet_datadoc_set(
        self._wave_id, self._wavelet_id, key, None)
    del self._docs[key]

  def __getitem__(self, key):
    return self._docs[key]

  def __setitem__(self, key, value):
    self._operation_queue.wavelet_datadoc_set(
        self._wave_id, self._wavelet_id, key, value)
    if value is None and key in self._docs:
      del self._docs[key]
    else:
      self._docs[key] = value

  def __len__(self):
    return len(self._docs)

  def keys(self):
    return self._docs.keys()

  def serialize(self):
    """Returns a dictionary of the data documents."""
    return self._docs


class Participants(object):
  """Class modelling a set of participants in pythonic way."""

  #: Designates full access (read/write) role.
  ROLE_FULL = "FULL"

  #: Designates read-only role.
  ROLE_READ_ONLY = "READ_ONLY"

  def __init__(self, participants, roles, wave_id, wavelet_id, operation_queue):
    self._participants = set(participants)
    self._roles = roles.copy()
    self._wave_id = wave_id
    self._wavelet_id = wavelet_id
    self._operation_queue = operation_queue

  def __contains__(self, participant):
    return participant in self._participants

  def __len__(self):
    return len(self._participants)

  def __iter__(self):
    return self._participants.__iter__()

  def add(self, participant_id):
    """Adds a participant by their ID (address)."""
    self._operation_queue.wavelet_add_participant(
        self._wave_id, self._wavelet_id, participant_id)
    self._participants.add(participant_id)

  def get_role(self, participant_id):
    """Return the role for the given participant_id."""
    return self._roles.get(participant_id, Participants.ROLE_FULL)

  def set_role(self, participant_id, role):
    """Sets the role for the given participant_id."""
    if role != Participants.ROLE_FULL and role != Participants.ROLE_READ_ONLY:
      raise ValueError(role + ' is not a valid role')
    self._operation_queue.wavelet_modify_participant_role(
        self._wave_id, self._wavelet_id, participant_id, role)
    self._roles[participant_id] = role

  def serialize(self):
    """Returns a list of the participants."""
    return list(self._participants)


class Tags(object):
  """Class modelling a list of tags."""
  def __init__(self, tags, wave_id, wavelet_id, operation_queue):
    self._tags = list(tags)
    self._wave_id = wave_id
    self._wavelet_id = wavelet_id
    self._operation_queue = operation_queue

  def __getitem__(self, index):
    return self._tags[index]

  def __len__(self):
    return len(self._tags)

  def __iter__(self):
    return self._tags.__iter__()

  def append(self, tag):
    """Appends a tag if it doesn't already exist."""
    tag = util.force_unicode(tag)
    if tag in self._tags:
      return
    self._operation_queue.wavelet_modify_tag(
        self._wave_id, self._wavelet_id, tag)
    self._tags.append(tag)

  def remove(self, tag):
    """Removes a tag if it exists."""
    tag = util.force_unicode(tag)
    if not tag in self._tags:
      return
    self._operation_queue.wavelet_modify_tag(
        self._wave_id, self._wavelet_id, tag, modify_how='remove')
    self._tags.remove(tag)

  def serialize(self):
    """Returns a list of tags."""
    return list(self._tags)


class BlipThread(object):
  """ Models a group of blips in a wave."""

  def __init__(self, id, location, blip_ids, all_blips, operation_queue):
    self._id = id
    self._location = location
    self._blip_ids = blip_ids
    self._all_blips = all_blips
    self._operation_queue = operation_queue

  @property
  def id(self):
    """Returns this thread's id."""
    return self._id

  @property
  def location(self):
    """Returns this thread's location."""
    return self._location

  @property
  def blip_ids(self):
    """Returns the blip IDs in this thread."""
    return self._blip_ids

  @property
  def blips(self):
    """Returns the blips in this thread."""
    blips = []
    for blip_id in self._blip_ids:
      blips.append(self._all_blips[blip_id])
    return blips

  def _add_internal(self, blip):
    """Adds a blip to the thread, sends out no operations."""
    self._blip_ids.append(blip.blip_id)
    self._all_blips[blip.blip_id] = blip

  def serialize(self):
    """ Returns serialized properties."""
    return {'id': self._id,
            'location': self._location,
            'blipIds': self._blip_ids}


class Wavelet(object):
  """Models a single wavelet.

  A single wavelet is composed of metadata, participants, and its blips.
  To guarantee that all blips are available, specify Context.ALL for events.
  """

  def __init__(self, json, blips, root_thread, operation_queue, raw_deltas=None):
    """Inits this wavelet with JSON data.

    Args:
      json: JSON data dictionary from Wave server.
      blips: a dictionary object that can be used to resolve blips.
      root_thread: a BlipThread object containing the blips in the root thread.
      operation_queue: an OperationQueue object to be used to
        send any generated operations to.
    """
    self._operation_queue = operation_queue
    self._root_thread = root_thread
    self._wave_id = json.get('waveId')
    self._wavelet_id = json.get('waveletId')
    self._creator = json.get('creator')
    self._raw_deltas = raw_deltas
    self._raw_snapshot = json.get('rawSnapshot')
    self._creation_time = json.get('creationTime', 0)
    self._data_documents = DataDocs(json.get('dataDocuments', {}),
                                    self._wave_id,
                                    self._wavelet_id,
                                    operation_queue)
    self._last_modified_time = json.get('lastModifiedTime')
    self._participants = Participants(json.get('participants', []),
                                      json.get('participantRoles', {}),
                                      self._wave_id,
                                      self._wavelet_id,
                                      operation_queue)
    self._title = json.get('title', '')
    self._tags = Tags(json.get('tags', []),
                      self._wave_id,
                      self._wavelet_id,
                      operation_queue)

    self._raw_data = json
    self._blips = blip.Blips(blips)
    self._root_blip_id = json.get('rootBlipId')
    if self._root_blip_id and self._root_blip_id in self._blips:
      self._root_blip = self._blips[self._root_blip_id]
    else:
      self._root_blip = None
    self._robot_address = None

  @property
  def wavelet_id(self):
    """Returns this wavelet's id."""
    return self._wavelet_id

  @property
  def wave_id(self):
    """Returns this wavelet's parent wave id."""
    return self._wave_id

  @property
  def creator(self):
    """Returns the participant id of the creator of this wavelet."""
    return self._creator

  @property
  def creation_time(self):
    """Returns the time that this wavelet was first created in milliseconds."""
    return self._creation_time

  @property
  def data_documents(self):
    """Returns the data documents for this wavelet based on key name."""
    return self._data_documents

  @property
  def domain(self):
    """Return the domain that wavelet belongs to."""
    p = self._wave_id.find('!')
    if p == -1:
      return None
    else:
      return self._wave_id[:p]

  @property
  def last_modified_time(self):
    """Returns the time that this wavelet was last modified in ms."""
    return self._last_modified_time

  @property
  def participants(self):
    """Returns a set of participants on this wavelet."""
    return self._participants

  @property
  def root_thread(self):
    """Returns the root thread of this wavelet."""
    return self._root_thread

  @property
  def tags(self):
    """Returns a list of tags for this wavelet."""
    return self._tags

  @property
  def raw_deltas(self):
    """If present, return the raw deltas for this wavelet."""
    return self._raw_deltas
  
  @property
  def raw_snapshot(self):
    """If present, return the raw snapshot for this wavelet."""
    return self._raw_snapshot

  def _get_title(self):
    return self._title

  def _set_title(self, title):
    title = util.force_unicode(title)

    if title.find('\n') != -1:
      raise errors.Error('Wavelet title should not contain a newline ' +
                         'character. Specified: ' + title)

    self._operation_queue.wavelet_set_title(self.wave_id, self.wavelet_id,
                                            title)
    self._title = title

    # Adjust the content of the root blip, if it is available in the context.
    if self._root_blip:
      content = '\n'
      splits = self._root_blip._content.split('\n', 2)
      if len(splits) == 3:
        content += splits[2]
      self._root_blip._content = '\n' + title + content

  #: Returns or sets the wavelet's title.
  title = property(_get_title, _set_title,
                   doc='Get or set the title of the wavelet.')

  def _get_robot_address(self):
    return self._robot_address

  def _set_robot_address(self, address):
    if self._robot_address:
      raise errors.Error('robot address already set')
    self._robot_address = address

  robot_address = property(_get_robot_address, _set_robot_address,
                           doc='Get or set the address of the current robot.')

  @property
  def root_blip(self):
    """Returns this wavelet's root blip."""
    return self._root_blip

  @property
  def blips(self):
    """Returns the blips for this wavelet."""
    return self._blips

  def get_operation_queue(self):
    """Returns the OperationQueue for this wavelet."""
    return self._operation_queue

  def serialize(self):
    """Return a dict of the wavelet properties."""
    return {'waveId': self._wave_id,
            'waveletId': self._wavelet_id,
            'creator': self._creator,
            'creationTime': self._creation_time,
            'dataDocuments': self._data_documents.serialize(),
            'lastModifiedTime': self._last_modified_time,
            'participants': self._participants.serialize(),
            'title': self._title,
            'blips': self._blips.serialize(),
            'rootBlipId': self._root_blip_id,
            'rootThread': self._root_thread.serialize()
           }

  def proxy_for(self, proxy_for_id):
    """Return a view on this wavelet that will proxy for the specified id.

    A shallow copy of the current wavelet is returned with the proxy_for_id
    set. Any modifications made to this copy will be done using the
    proxy_for_id, i.e. the robot+<proxy_for_id>@appspot.com address will
    be used.

    If the wavelet was retrieved using the Active Robot API, that is
    by fetch_wavelet, then the address of the robot must be added to the
    wavelet by setting wavelet.robot_address before calling proxy_for().
    """
    util.check_is_valid_proxy_for_id(proxy_for_id)
    self.add_proxying_participant(proxy_for_id)
    operation_queue = self.get_operation_queue().proxy_for(proxy_for_id)
    res = Wavelet(json={},
                  blips={}, root_thread=None,
                  operation_queue=operation_queue)
    res._wave_id = self._wave_id
    res._wavelet_id = self._wavelet_id
    res._creator = self._creator
    res._creation_time = self._creation_time
    res._data_documents = self._data_documents
    res._last_modified_time = self._last_modified_time
    res._participants = self._participants
    res._title = self._title
    res._raw_data = self._raw_data
    res._blips = self._blips
    res._root_blip = self._root_blip
    res._root_thread = self._root_thread
    return res

  def add_proxying_participant(self, id):
    """Ads a proxying participant to the wave.

    Proxying participants are of the form robot+proxy@domain.com. This
    convenience method constructs this id and then calls participants.add.
    """
    if not self.robot_address:
      raise errors.Error(
          'Need a robot address to add a proxying for participant')
    robotid, domain = self.robot_address.split('@', 1)
    if '#' in robotid:
      robotid, version = robotid.split('#')
    else:
      version = None
    if '+' in robotid:
      newid = robotid.split('+', 1)[0] + '+' + id
    else:
      newid = robotid + '+' + id
    if version:
      newid += '#' + version
    newid += '@' + domain
    self.participants.add(newid)

  def submit_with(self, other_wavelet):
    """Submit this wavelet when the passed other wavelet is submited.

    wavelets constructed outside of the event callback need to
    be either explicitly submited using robot.submit(wavelet) or be
    associated with a different wavelet that will be submited or
    is part of the event callback.
    """
    other_wavelet._operation_queue.copy_operations(self._operation_queue)
    self._operation_queue = other_wavelet._operation_queue

  def reply(self, initial_content=None):
    """Replies to the conversation in this wavelet.

    Args:
      initial_content: If set, start with this (string) content.

    Returns:
      A transient version of the blip that contains the reply.
    """
    if not initial_content:
      initial_content = u'\n'
    initial_content = util.force_unicode(initial_content)
    blip_data = self._operation_queue.wavelet_append_blip(
       self.wave_id, self.wavelet_id, initial_content)

    instance = blip.Blip(blip_data, self._blips, self._operation_queue)
    self._blips._add(instance)
    self.root_blip.child_blip_ids.append(instance.blip_id)
    return instance

  def delete(self, todelete):
    """Remove a blip from this wavelet.

    Args:
      todelete: either a blip or a blip id to be removed.
    """
    if isinstance(todelete, blip.Blip):
      blip_id = todelete.blip_id
    else:
      blip_id = todelete
    self._operation_queue.blip_delete(self.wave_id, self.wavelet_id, blip_id)
    self._blips._remove_with_id(blip_id)
