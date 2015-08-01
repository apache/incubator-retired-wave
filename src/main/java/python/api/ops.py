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

"""Support for operations that can be applied to the server.

Contains classes and utilities for creating operations that are to be
applied on the server.
"""

import errors
import random
import util
import sys


PROTOCOL_VERSION = '0.22'

# Operation Types
WAVELET_APPEND_BLIP = 'wavelet.appendBlip'
WAVELET_SET_TITLE = 'wavelet.setTitle'
WAVELET_ADD_PARTICIPANT = 'wavelet.participant.add'
WAVELET_DATADOC_SET = 'wavelet.datadoc.set'
WAVELET_MODIFY_TAG = 'wavelet.modifyTag'
WAVELET_MODIFY_PARTICIPANT_ROLE = 'wavelet.modifyParticipantRole'
BLIP_CONTINUE_THREAD = 'blip.continueThread'
BLIP_CREATE_CHILD = 'blip.createChild'
BLIP_DELETE = 'blip.delete'
DOCUMENT_APPEND_MARKUP = 'document.appendMarkup'
DOCUMENT_INLINE_BLIP_INSERT = 'document.inlineBlip.insert'
DOCUMENT_MODIFY = 'document.modify'
ROBOT_CREATE_WAVELET = 'robot.createWavelet'
ROBOT_FETCH_WAVE = 'robot.fetchWave'
ROBOT_NOTIFY = 'robot.notify'
ROBOT_SEARCH = 'robot.search'

# Assign always NOTIFY_OP_ID to the notify operation so
# we can easily filter it out later
NOTIFY_OP_ID = '0'

class Operation(object):
  """Represents a generic operation applied on the server.

  This operation class contains data that is filled in depending on the
  operation type.

  It can be used directly, but doing so will not result
  in local, transient reflection of state on the blips. In other words,
  creating a 'delete blip' operation will not remove the blip from the local
  context for the duration of this session. It is better to use the OpBased
  model classes directly instead.
  """

  def __init__(self, method, opid, params):
    """Initializes this operation with contextual data.

    Args:
      method: Method to call or type of operation.
      opid: The id of the operation. Any callbacks will refer to these.
      params: An operation type dependent dictionary
    """
    self.method = method
    self.id = opid
    self.params = params

  def __str__(self):
    return '%s[%s]%s' % (self.method, self.id, str(self.params))

  def set_param(self, param, value):
    self.params[param] = value
    return self

  def serialize(self, method_prefix=''):
    """Serialize the operation.

    Args:
      method_prefix: prefixed for each method name to allow for specifying
      a namespace.

    Returns:
      a dict representation of the operation.
    """
    if method_prefix and not method_prefix.endswith('.'):
      method_prefix += '.'
    return {'method': method_prefix + self.method,
            'id': self.id,
            'params': util.serialize(self.params)}

  def set_optional(self, param, value):
    """Sets an optional parameter.

    If value is None or "", this is a no op. Otherwise it calls
    set_param.
    """
    if value == '' or value is None:
      return self
    else:
      return self.set_param(param, value)


class OperationQueue(object):
  """Wraps the queuing of operations using easily callable functions.

  The operation queue wraps single operations as functions and queues the
  resulting operations in-order. Typically there shouldn't be a need to
  call this directly unless operations are needed on entities outside
  of the scope of the robot. For example, to modify a blip that
  does not exist in the current context, you might specify the wave, wavelet
  and blip id to generate an operation.

  Any calls to this will not be reflected in the robot in any way.
  For example, calling wavelet_append_blip will not result in a new blip
  being added to the robot, only an operation to be applied on the
  server.
  """

  # Some class global counters:
  _next_operation_id = 1

  def __init__(self, proxy_for_id=None):
    self.__pending = []
    self._capability_hash = None
    self._proxy_for_id = proxy_for_id

  def _new_blipdata(self, wave_id, wavelet_id, initial_content='',
                    parent_blip_id=None):
    """Creates JSON of the blip used for this session."""
    temp_blip_id = 'TBD_%s_%s' % (wavelet_id,
                                  hex(random.randint(0, sys.maxint)))
    return {'waveId': wave_id,
            'waveletId': wavelet_id,
            'blipId': temp_blip_id,
            'content': initial_content,
            'parentBlipId': parent_blip_id}

  def _new_waveletdata(self, domain, participants):
    """Creates an ephemeral WaveletData instance used for this session.

    Args:
      domain: the domain to create the data for.
      participants initially on the wavelet
    Returns:
      Blipdata (for the rootblip), WaveletData.
    """
    wave_id = domain + '!TBD_%s' % hex(random.randint(0, sys.maxint))
    wavelet_id = domain + '!conv+root'
    root_blip_data = self._new_blipdata(wave_id, wavelet_id)
    participants = set(participants)
    wavelet_data = {'waveId': wave_id,
                    'waveletId': wavelet_id,
                    'rootBlipId': root_blip_data['blipId'],
                    'participants': participants}
    return root_blip_data, wavelet_data

  def __len__(self):
    return len(self.__pending)

  def __iter__(self):
    return self.__pending.__iter__()

  def clear(self):
    self.__pending = []

  def proxy_for(self, proxy):
    """Return a view of this operation queue with the proxying for set to proxy.

    This method returns a new instance of an operation queue that shares the
    operation list, but has a different proxying_for_id set so the robot using
    this new queue will send out operations with the proxying_for field set.
    """
    res = OperationQueue()
    res.__pending = self.__pending
    res._capability_hash = self._capability_hash
    res._proxy_for_id = proxy
    return res

  def set_capability_hash(self, capability_hash):
    self._capability_hash = capability_hash

  def serialize(self, method_prefix=''):
    first = Operation(ROBOT_NOTIFY,
                      NOTIFY_OP_ID,
                      {'capabilitiesHash': self._capability_hash,
                       'protocolVersion': PROTOCOL_VERSION})
    operations = [first] + self.__pending
    return [op.serialize(method_prefix=method_prefix) for op in operations]
    res = util.serialize(operations)
    return res

  def copy_operations(self, other_queue):
    """Copy the pending operations from other_queue into this one."""
    for op in other_queue:
      self.__pending.append(op)

  def new_operation(self, method, wave_id, wavelet_id, props=None, **kwprops):
    """Creates and adds a new operation to the operation list."""
    if props is None:
      props = {}
    props.update(kwprops)
    if wave_id is not None:
      props['waveId'] = wave_id
    if wavelet_id is not None:
      props['waveletId'] = wavelet_id
    if self._proxy_for_id:
      props['proxyingFor'] = self._proxy_for_id
    operation = Operation(method,
                          'op%s' % OperationQueue._next_operation_id,
                          props)
    self.__pending.append(operation)
    OperationQueue._next_operation_id += 1
    return operation

  def wavelet_append_blip(self, wave_id, wavelet_id, initial_content=''):
    """Appends a blip to a wavelet.

    Args:
      wave_id: The wave id owning the containing wavelet.
      wavelet_id: The wavelet id that this blip should be appended to.
      initial_content: optionally the content to start with

    Returns:
      JSON representing the information of the new blip.
    """
    blip_data = self._new_blipdata(wave_id, wavelet_id, initial_content)
    self.new_operation(WAVELET_APPEND_BLIP, wave_id,
                       wavelet_id, blipData=blip_data)
    return blip_data

  def wavelet_add_participant(self, wave_id, wavelet_id, participant_id):
    """Adds a participant to a wavelet.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      participant_id: Id of the participant to add.

    Returns:
      data for the root_blip, wavelet
    """
    return self.new_operation(WAVELET_ADD_PARTICIPANT, wave_id, wavelet_id,
                              participantId=participant_id)

  def wavelet_datadoc_set(self, wave_id, wavelet_id, name, data):
    """Sets a key/value pair on the data document of a wavelet.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      name: The key name for this data.
      data: The value of the data to set.
    Returns:
      The operation created.
    """
    return self.new_operation(WAVELET_DATADOC_SET, wave_id, wavelet_id,
                              datadocName=name, datadocValue=data)

  def robot_create_wavelet(self, domain, participants=None, message=''):
    """Creates a new wavelet.

    Args:
      domain: the domain to create the wave in
      participants: initial participants on this wavelet or None if none
      message: an optional payload that is returned with the corresponding
          event.

    Returns:
      data for the root_blip, wavelet
    """
    if participants is None:
      participants = []
    blip_data, wavelet_data = self._new_waveletdata(domain, participants)
    op = self.new_operation(ROBOT_CREATE_WAVELET,
                            wave_id=wavelet_data['waveId'],
                            wavelet_id=wavelet_data['waveletId'],
                            waveletData=wavelet_data)
    op.set_optional('message', message)
    return blip_data, wavelet_data

  def robot_search(self, query, index=None, num_results=None):
    """Execute a search request.

    For now this only makes sense in the data API. Wave does not maintain
    an index for robots so no results will be returned in that scenario.

    Args:
      query: what to search for
      index: what index to search from
      num_results: how many results to return
    Returns:
      The operation created.
    """
    op = self.new_operation(
        ROBOT_SEARCH, wave_id=None, wavelet_id=None, query=query)
    if index is not None:
      op.set_param('index', index)
    if num_results is not None:
      op.set_param('numResults', num_results)
    return op

  def robot_fetch_wave(self, wave_id, wavelet_id,
      raw_deltas_from_version=-1, return_raw_snapshot=False):
    """Requests a snapshot of the specified wavelet.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      raw_deltas_from_version: If specified, return a raw dump of the
        delta history of this wavelet, starting at the given version.
        This may return only part of the history; use additional
        requests with higher raw_deltas_from_version parameters to
        get the rest.
      return_raw_snapshot: if true, return the raw data for this
        wavelet.
    Returns:
      The operation created.
    """
    op = self.new_operation(ROBOT_FETCH_WAVE, wave_id, wavelet_id)
    if raw_deltas_from_version != -1:
      op.set_param('rawDeltasFromVersion', raw_deltas_from_version)
    if return_raw_snapshot:
      op.set_param('returnRawSnapshot', return_raw_snapshot)
    return op

  def wavelet_set_title(self, wave_id, wavelet_id, title):
    """Sets the title of a wavelet.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      title: The title to set.
    Returns:
      The operation created.
    """
    return self.new_operation(WAVELET_SET_TITLE, wave_id, wavelet_id,
                              waveletTitle=title)

  def wavelet_modify_participant_role(
      self, wave_id, wavelet_id, participant_id, role):
    """Modify the role of a participant on a wavelet.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      participant_id: Id of the participant to add.
      role: the new roles

    Returns:
      data for the root_blip, wavelet
    """
    return self.new_operation(WAVELET_MODIFY_PARTICIPANT_ROLE, wave_id,
                              wavelet_id, participantId=participant_id,
                              participantRole=role)

  def wavelet_modify_tag(self, wave_id, wavelet_id, tag, modify_how=None):
    """Modifies a tag in a wavelet.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      tag: The tag (a string).
      modify_how: (optional) how to apply the tag. The default is to add
        the tag. Specify 'remove' to remove. Specify None or 'add' to
        add.
    Returns:
      The operation created.
    """
    return self.new_operation(WAVELET_MODIFY_TAG, wave_id, wavelet_id,
                             name=tag).set_optional("modify_how", modify_how)

  def blip_create_child(self, wave_id, wavelet_id, blip_id):
    """Creates a child blip of another blip.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      blip_id: The blip id that this operation is applied to.

    Returns:
      JSON of blip for which further operations can be applied.
    """
    blip_data = self._new_blipdata(wave_id, wavelet_id, parent_blip_id=blip_id)
    self.new_operation(BLIP_CREATE_CHILD, wave_id, wavelet_id,
                       blipId=blip_id,
                       blipData=blip_data)
    return blip_data

  def blip_continue_thread(self, wave_id, wavelet_id, blip_id):
    """Creates a blip in same thread as specified blip.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      blip_id: The blip id that this operation is applied to.

    Returns:
      JSON of blip for which further operations can be applied.
    """
    blip_data = self._new_blipdata(wave_id, wavelet_id)
    self.new_operation(BLIP_CONTINUE_THREAD, wave_id, wavelet_id,
                       blipId=blip_id,
                       blipData=blip_data)
    return blip_data


  def blip_delete(self, wave_id, wavelet_id, blip_id):
    """Deletes the specified blip.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      blip_id: The blip id that this operation is applied to.
    Returns:
      The operation created.
    """
    return self.new_operation(BLIP_DELETE, wave_id, wavelet_id, blipId=blip_id)

  def document_append_markup(self, wave_id, wavelet_id, blip_id, content):
    """Appends content with markup to a document.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      blip_id: The blip id that this operation is applied to.
      content: The markup content to append.
    Returns:
      The operation created.
    """
    return self.new_operation(DOCUMENT_APPEND_MARKUP, wave_id, wavelet_id,
                              blipId=blip_id, content=content)

  def document_modify(self, wave_id, wavelet_id, blip_id):
    """Creates and queues a document modify operation

    The returned operation still needs to be filled with details before
    it makes sense.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      blip_id: The blip id that this operation is applied to.
    Returns:
      The operation created.
    """
    return self.new_operation(DOCUMENT_MODIFY,
                              wave_id,
                              wavelet_id,
                              blipId=blip_id)

  def document_inline_blip_insert(self, wave_id, wavelet_id, blip_id, position):
    """Inserts an inline blip at a specific location.

    Args:
      wave_id: The wave id owning that this operation is applied to.
      wavelet_id: The wavelet id that this operation is applied to.
      blip_id: The blip id that this operation is applied to.
      position: The position in the document to insert the blip.

    Returns:
      JSON data for the blip that was created for further operations.
    """
    inline_blip_data = self._new_blipdata(wave_id, wavelet_id)
    inline_blip_data['parentBlipId'] = blip_id
    self.new_operation(DOCUMENT_INLINE_BLIP_INSERT, wave_id, wavelet_id,
                       blipId=blip_id,
                       index=position,
                       blipData=inline_blip_data)
    return inline_blip_data
