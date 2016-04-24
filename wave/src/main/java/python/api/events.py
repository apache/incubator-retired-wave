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

"""Defines event types that are sent from the wave server.

This module defines all of the event types currently supported by the wave
server. Each event type is sub classed from Event and has its own
properties depending on the type.
"""


class Context(object):
  """Specifies constants representing different context requests."""

  #: Requests the root blip.
  ROOT = 'ROOT'
  #: Requests the parent blip of the event blip.
  PARENT = 'PARENT'
  #: Requests the siblings blip of the event blip.
  SIBLINGS = 'SIBLINGS'
  #: Requests the child blips of the event blip.
  CHILDREN = 'CHILDREN'
  #: Requests the event blip itself.
  SELF = 'SELF'
  #: Requests all of the blips of the event wavelet.
  ALL = 'ALL'


class Event(object):
  """Object describing a single event.

  Attributes:
    modified_by: Participant id that caused this event.

    timestamp: Timestamp that this event occurred on the server.

    type: Type string of this event.

    properties: Dictionary of all extra properties. Typically the derrived
        event type should have these explicitly set as attributes, but
        experimental features might appear in properties before that.

    blip_id: The blip_id of the blip for blip related events or the root
        blip for wavelet related events.

    blip: If available, the blip with id equal to the events blip_id.

    proxying_for: If available, the proxyingFor id of the robot that caused the
    event.
  """

  def __init__(self, json, wavelet):
    """Inits this event with JSON data.

    Args:
      json: JSON data from Wave server.
    """
    self.modified_by = json.get('modifiedBy')
    self.timestamp = json.get('timestamp', 0)
    self.type = json.get('type')
    self.raw_data = json
    self.properties = json.get('properties', {})
    self.blip_id = self.properties.get('blipId')
    self.blip = wavelet.blips.get(self.blip_id)
    self.proxying_for = json.get('proxyingFor')

class WaveletBlipCreated(Event):
  """Event triggered when a new blip is created.

  Attributes:
    new_blip_id: The id of the newly created blip.

    new_blip: If in context, the actual new blip.
  """
  type = 'WAVELET_BLIP_CREATED'

  def __init__(self, json, wavelet):
    super(WaveletBlipCreated, self).__init__(json, wavelet)
    self.new_blip_id = self.properties['newBlipId']
    self.new_blip = wavelet.blips.get(self.new_blip_id)


class WaveletBlipRemoved(Event):
  """Event triggered when a new blip is removed.

  Attributes:
    removed_blip_id: the id of the removed blip

    removed_blip: if in context, the removed blip
  """
  type = 'WAVELET_BLIP_REMOVED'

  def __init__(self, json, wavelet):
    super(WaveletBlipRemoved, self).__init__(json, wavelet)
    self.removed_blip_id = self.properties['removedBlipId']
    self.removed_blip = wavelet.blips.get(self.removed_blip_id)


class WaveletParticipantsChanged(Event):
  """Event triggered when the participants on a wave change.

  Attributes:
    participants_added: List of participants added.

    participants_removed: List of participants removed.
  """
  type = 'WAVELET_PARTICIPANTS_CHANGED'

  def __init__(self, json, wavelet):
    super(WaveletParticipantsChanged, self).__init__(json, wavelet)
    self.participants_added = self.properties['participantsAdded']
    self.participants_removed = self.properties['participantsRemoved']


class WaveletSelfAdded(Event):
  """Event triggered when the robot is added to the wavelet."""
  type = 'WAVELET_SELF_ADDED'


class WaveletSelfRemoved(Event):
  """Event triggered when the robot is removed from the wavelet."""
  type = 'WAVELET_SELF_REMOVED'


class WaveletTitleChanged(Event):
  """Event triggered when the title of the wavelet has changed.

  Attributes:
    title: The new title.
  """
  type = 'WAVELET_TITLE_CHANGED'

  def __init__(self, json, wavelet):
    super(WaveletTitleChanged, self).__init__(json, wavelet)
    self.title = self.properties['title']


class BlipContributorsChanged(Event):
  """Event triggered when the contributors to this blip change.

  Attributes:
    contributors_added: List of contributors that were added.

    contributors_removed: List of contributors that were removed.
  """
  type = 'BLIP_CONTRIBUTORS_CHANGED'

  def __init__(self, json, wavelet):
    super(BlipContributorsChanged, self).__init__(json, wavelet)
    self.contibutors_added = self.properties['contributorsAdded']
    self.contibutors_removed = self.properties['contributorsRemoved']


class BlipSubmitted(Event):
  """Event triggered when a blip is submitted."""
  type = 'BLIP_SUBMITTED'


class DocumentChanged(Event):
  """Event triggered when a document is changed.

  This event is fired after any changes in the document and should be used
  carefully to keep the amount of traffic to the robot reasonable. Use
  filters where appropriate.
  """
  type = 'DOCUMENT_CHANGED'


class FormButtonClicked(Event):
  """Event triggered when a form button is clicked.

  Attributes:
    button_name: The name of the button that was clicked.
  """
  type = 'FORM_BUTTON_CLICKED'

  def __init__(self, json, wavelet):
    super(FormButtonClicked, self).__init__(json, wavelet)
    self.button_name = self.properties['buttonName']


class GadgetStateChanged(Event):
  """Event triggered when the state of a gadget changes.

  Attributes:
    index: The index of the gadget that changed in the document.

    old_state: The old state of the gadget.
  """
  type = 'GADGET_STATE_CHANGED'

  def __init__(self, json, wavelet):
    super(GadgetStateChanged, self).__init__(json, wavelet)
    self.index = self.properties['index']
    self.old_state = self.properties['oldState']


class AnnotatedTextChanged(Event):
  """Event triggered when text with an annotation has changed.

  This is mainly useful in combination with a filter on the
  name of the annotation.

  Attributes:
    name: The name of the annotation.

    value: The value of the annotation that changed.
  """
  type = 'ANNOTATED_TEXT_CHANGED'

  def __init__(self, json, wavelet):
    super(AnnotatedTextChanged, self).__init__(json, wavelet)
    self.name = self.properties['name']
    self.value = self.properties.get('value')


class OperationError(Event):
  """Triggered when an event on the server occurred.

  Attributes:
    operation_id: The operation id of the failing operation.

    error_message: More information as to what went wrong.
  """
  type = 'OPERATION_ERROR'

  def __init__(self, json, wavelet):
    super(OperationError, self).__init__(json, wavelet)
    self.operation_id = self.properties['operationId']
    self.error_message = self.properties['message']


class WaveletCreated(Event):
  """Triggered when a new wavelet is created.

  This event is only triggered if the robot creates a new
  wavelet and can be used to initialize the newly created wave.
  wavelets created by other participants remain invisible
  to the robot until the robot is added to the wave in
  which case WaveletSelfAdded is triggered.

  Attributes:
    message: Whatever string was passed into the new_wave
        call as message (if any).
  """
  type = 'WAVELET_CREATED'

  def __init__(self, json, wavelet):
    super(WaveletCreated, self).__init__(json, wavelet)
    self.message = self.properties['message']


class WaveletFetched(Event):
  """Triggered when a new wavelet is fetched.

  This event is triggered after a robot requests to
  see another wavelet. The robot has to be on the other
  wavelet already.

  Attributes:
    message: Whatever string was passed into the new_wave
        call as message (if any).
  """
  type = 'WAVELET_FETCHED'

  def __init__(self, json, wavelet):
    super(WaveletFetched, self).__init__(json, wavelet)
    self.message = self.properties['message']


class WaveletTagsChanged(Event):
  """Event triggered when the tags on a wavelet change."""
  type = 'WAVELET_TAGS_CHANGED'

  def __init__(self, json, wavelet):
    super(WaveletTagsChanged, self).__init__(json, wavelet)


def is_event(cls):
  """Returns whether the passed class is an event."""
  try:
    if not issubclass(cls, Event):
      return False
    return hasattr(cls, 'type')
  except TypeError:
    return False

ALL = [item for item in globals().copy().values() if is_event(item)]
