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

import UserDict

import element
import errors

import util

class Annotation(object):
  """Models an annotation on a document.

  Annotations are key/value pairs over a range of content. Annotations
  can be used to store data or to be interpreted by a client when displaying
  the data.
  """

  # Use the following constants to control the display of the client

  #: Reserved annotation for setting background color of text.
  BACKGROUND_COLOR = "style/backgroundColor"
  #: Reserved annotation for setting color of text.
  COLOR = "style/color"
  #: Reserved annotation for setting font family of text.
  FONT_FAMILY = "style/fontFamily"
  #: Reserved annotation for setting font family of text.
  FONT_SIZE = "style/fontSize"
  #: Reserved annotation for setting font style of text.
  FONT_STYLE = "style/fontStyle"
  #: Reserved annotation for setting font weight of text.
  FONT_WEIGHT = "style/fontWeight"
  #: Reserved annotation for setting text decoration.
  TEXT_DECORATION = "style/textDecoration"
  #: Reserved annotation for setting vertical alignment.
  VERTICAL_ALIGN = "style/verticalAlign"
  #: Reserved annotation for setting link.
  LINK = "link/manual"

  def __init__(self, name, value, start, end):
    self._name = name
    self._value = value
    self._start = start
    self._end = end

  @property
  def name(self):
    return self._name

  @property
  def value(self):
    return self._value

  @property
  def start(self):
    return self._start

  @property
  def end(self):
    return self._end

  def _shift(self, where, inc):
    """Shift annotation by 'inc' if it (partly) overlaps with 'where'."""
    if self._start >= where:
      self._start += inc
    if self._end >= where:
      self._end += inc

  def serialize(self):
    """Serializes the annotation.

    Returns:
      A dict containing the name, value, and range values.
    """
    return {'name': self._name,
            'value': self._value,
            'range': {'start': self._start,
                      'end': self._end}}


class Annotations(object, UserDict.DictMixin):
  """A dictionary-like object containing the annotations, keyed by name."""

  def __init__(self, operation_queue, blip):
    self._operation_queue = operation_queue
    self._blip = blip
    self._store = {}

  def __contains__(self, what):
    if isinstance(what, Annotation):
      what = what.name
    return what in self._store

  def _add_internal(self, name, value, start, end):
    """Internal add annotation does not send out operations."""
    if name in self._store:
      # TODO: use bisect to make this more efficient.
      new_list = []
      for existing in self._store[name]:
        if start > existing.end or end < existing.start:
          new_list.append(existing)
        else:
          if existing.value == value:
            # merge the annotations:
            start = min(existing.start, start)
            end = max(existing.end, end)
          else:
            # chop the bits off the existing annotation
            if existing.start < start:
              new_list.append(Annotation(
                  existing.name, existing.value, existing.start, start))
            if existing.end > end:
              new_list.append(Annotation(
                  existing.name, existing.value, existing.end, end))
      new_list.append(Annotation(name, value, start, end))
      self._store[name] = new_list
    else:
      self._store[name] = [Annotation(name, value, start, end)]

  def _delete_internal(self, name, start=0, end=-1):
    """Remove the passed annotaion from the internal representation."""
    if not name in self._store:
      return
    if end < 0:
      end = len(self._blip) + end

    new_list = []
    for a in self._store[name]:
      if start > a.end or end < a.start:
        new_list.append(a)
      elif start < a.start and end > a.end:
        continue
      else:
        if a.start < start:
          new_list.append(Annotation(name, a.value, a.start, start))
        if a.end > end:
          new_list.append(Annotation(name, a.value, end, a.end))
    if new_list:
      self._store[name] = new_list
    else:
      del self._store[name]

  def _shift(self, where, inc):
    """Shift annotation by 'inc' if it (partly) overlaps with 'where'."""
    for annotations in self._store.values():
      for annotation in annotations:
        annotation._shift(where, inc)

    # Merge fragmented annotations that should be contiguous, for example:
    # Annotation('foo', 'bar', 1, 2) and Annotation('foo', 'bar', 2, 3).
    for name, annotations in self._store.items():
      new_list = []
      for i, annotation in enumerate(annotations):
        name = annotation.name
        value = annotation.value
        start = annotation.start
        end = annotation.end

        # Find the last end index.
        for j, next_annotation in enumerate(annotations[i + 1:]):
          # Not contiguous, skip.
          if (end < next_annotation.start):
            break

          # Contiguous, merge.
          if (end == next_annotation.start and value == next_annotation.value):
            end = next_annotation.end
            del annotations[j]
        new_list.append(Annotation(name, value, start, end))
      self._store[name] = new_list

  def __len__(self):
    return len(self._store)

  def __getitem__(self, key):
    return self._store[key]

  def __iter__(self):
    for l in self._store.values():
      for ann in l:
        yield ann

  def names(self):
    """Return the names of the annotations in the store."""
    return self._store.keys()

  def serialize(self):
    """Return a list of the serialized annotations."""
    res = []
    for v in self._store.values():
      res += [a.serialize() for a in v]
    return res


class Blips(object, UserDict.DictMixin):
  """A dictionary-like object containing the blips, keyed on blip ID."""

  def __init__(self, blips):
    self._blips = blips

  def __contains__(self, blip_id):
    return blip_id in self._blips

  def __getitem__(self, blip_id):
    return self._blips[blip_id]

  def __iter__(self):
    return self._blips.__iter__()

  def __len__(self):
    return len(self._blips)

  def _add(self, ablip):
    self._blips[ablip.blip_id] = ablip

  def _remove_with_id(self, blip_id):
    del_blip = self._blips[blip_id]
    if del_blip:
      # Remove the reference to this blip from its parent.
      parent_blip = self._blips[blip_id].parent_blip
      if parent_blip:
        parent_blip._child_blip_ids.remove(blip_id)
    del self._blips[blip_id]

  def get(self, blip_id, default_value=None):
    """Retrieves a blip.

    Returns:
      A Blip object. If none found for the ID, it returns None,
      or if default_value is specified, it returns that.
    """
    return self._blips.get(blip_id, default_value)

  def serialize(self):
    """Serializes the blips.
    Returns:
      A dict of serialized blips.
    """
    res = {}
    for blip_id, item in self._blips.items():
      res[blip_id] = item.serialize()
    return res

  def values(self):
    """Return the blips themselves."""
    return self._blips.values()


class BlipRefs(object):
  """Represents a set of references to contents in a blip.

  For example, a BlipRefs instance can represent the results
  of a search, an explicitly set range, a regular expression,
  or refer to the entire blip. BlipRefs are used to express
  operations on a blip in a consistent way that can easily
  be transfered to the server.

  The typical way of creating a BlipRefs object is to use
  selector methods on the Blip object. Developers will not
  usually instantiate a BlipRefs object directly.
  """

  DELETE = 'DELETE'
  REPLACE = 'REPLACE'
  INSERT = 'INSERT'
  INSERT_AFTER = 'INSERT_AFTER'
  ANNOTATE = 'ANNOTATE'
  CLEAR_ANNOTATION = 'CLEAR_ANNOTATION'
  UPDATE_ELEMENT = 'UPDATE_ELEMENT'

  def __init__(self, blip, maxres=1):
    self._blip = blip
    self._maxres = maxres

  @classmethod
  def all(cls, blip, findwhat, maxres=-1, **restrictions):
    """Construct an instance representing the search for text or elements."""
    obj = cls(blip, maxres)
    obj._findwhat = findwhat
    obj._restrictions = restrictions
    obj._hits = lambda: obj._find(findwhat, maxres, **restrictions)
    if findwhat is None:
      # No findWhat, take the entire blip
      obj._params = {}
    else:
      query = {'maxRes': maxres}
      if isinstance(findwhat, basestring):
        query['textMatch'] = findwhat
      else:
        query['elementMatch'] = findwhat.class_type
        query['restrictions'] = restrictions
      obj._params = {'modifyQuery': query}
    return obj

  @classmethod
  def range(cls, blip, begin, end):
    """Constructs an instance representing an explicitly set range."""
    obj = cls(blip)
    obj._begin = begin
    obj._end = end
    obj._hits = lambda: [(begin, end)]
    obj._params = {'range': {'start': begin, 'end': end}}
    return obj

  def _elem_matches(self, elem, clz, **restrictions):
    if not isinstance(elem, clz):
      return False
    for key, val in restrictions.items():
      if getattr(elem, key) != val:
        return False
    return True

  def _find(self, what, maxres=-1, **restrictions):
    """Iterates where 'what' occurs in the associated blip.

    What can be either a string or a class reference.
    Examples:
        self._find('hello') will return the first occurence of the word hello
        self._find(element.Gadget, url='http://example.com/gadget.xml')
            will return the first gadget that has as url example.com.

    Args:
      what: what to search for. Can be a class or a string. The class
          should be an element from element.py
      maxres: number of results to return at most, or <= 0 for all.
      restrictions: if what specifies a class, further restrictions
         of the found instances.
    Yields:
      Tuples indicating the range of the matches. For a one
      character/element match at position x, (x, x+1) is yielded.
    """
    blip = self._blip
    if what is None:
      yield 0, len(blip)
      raise StopIteration
    if isinstance(what, basestring):
      idx = blip._content.find(what)
      count = 0
      while idx != -1:
        yield idx, idx + len(what)
        count += 1
        if count == maxres:
          raise StopIteration
        idx = blip._content.find(what, idx + len(what))
    else:
      count = 0
      for idx, el in blip._elements.items():
        if self._elem_matches(el, what, **restrictions):
          yield idx, idx + 1
          count += 1
          if count == maxres:
            raise StopIteration

  def _execute(self, modify_how, what, bundled_annotations=None):
    """Executes this BlipRefs object.

    Args:
      modify_how: What to do. Any of the operation declared at the top.
      what: Depending on the operation. For delete, has to be None.
            For the others it is a singleton, a list or a function returning
            what to do; for ANNOTATE tuples of (key, value), for the others
            either string or elements.
            If what is a function, it takes three parameters, the content of
            the blip, the beginning of the matching range and the end.
      bundled_annotations: Annotations to apply immediately.
    Raises:
      IndexError when trying to access content outside of the blip.
      ValueError when called with the wrong values.
    Returns:
      self for chainability.
    """
    blip = self._blip

    if modify_how != BlipRefs.DELETE:
      if not isinstance(what, list):
        what = [what]
      next_index = 0

    matched = []
    # updated_elements is used to store the element type of the
    # element to update
    updated_elements = []
    
    # For now, if we find one markup, we'll use it everywhere.
    next = None
    hit_found = False

    for start, end in self._hits():
      hit_found = True
      if start < 0:
        start += len(blip)
        if end == 0:
          end += len(blip)
      if end < 0:
        end += len(blip)
      if len(blip) == 0:
        if start != 0 or end != 0:
          raise IndexError('Start and end have to be 0 for empty document')
      elif start < 0 or end < 1 or start >= len(blip) or end > len(blip):
        raise IndexError('Position outside the document')
      if modify_how == BlipRefs.DELETE:
        for i in range(start, end):
          if i in blip._elements:
            del blip._elements[i]
        blip._delete_annotations(start, end)
        blip._shift(end, start - end)
        blip._content = blip._content[:start] + blip._content[end:]
      else:
        if callable(what):
          next = what(blip._content, start, end)
          matched.append(next)
        else:
          next = what[next_index]
          next_index = (next_index + 1) % len(what)
        if isinstance(next, str):
          next = util.force_unicode(next)
        if modify_how == BlipRefs.ANNOTATE:
          key, value = next
          blip.annotations._add_internal(key, value, start, end)
        elif modify_how == BlipRefs.CLEAR_ANNOTATION:
          blip.annotations._delete_internal(next, start, end)
        elif modify_how == BlipRefs.UPDATE_ELEMENT:
          el = blip._elements.get(start)
          if not el:
            raise ValueError('No element found at index %s' % start)
          # the passing around of types this way feels a bit dirty:
          updated_elements.append(element.Element.from_json({'type': el.type,
              'properties': next}))
          for k, b in next.items():
            setattr(el, k, b)
        else:
          if modify_how == BlipRefs.INSERT:
            end = start
          elif modify_how == BlipRefs.INSERT_AFTER:
            start = end
          elif modify_how == BlipRefs.REPLACE:
            pass
          else:
            raise ValueError('Unexpected modify_how: ' + modify_how)

          if isinstance(next, element.Element):
            text = ' '
          else:
            text = next

          # in the case of a replace, and the replacement text is shorter,
          # delete the delta.
          if start != end and len(text) < end - start:
            blip._delete_annotations(start + len(text), end)

          blip._shift(end, len(text) + start - end)
          blip._content = blip._content[:start] + text + blip._content[end:]
          if bundled_annotations:
            end_annotation = start + len(text)
            blip._delete_annotations(start, end_annotation)
            for key, value in bundled_annotations:
              blip.annotations._add_internal(key, value, start, end_annotation)

          if isinstance(next, element.Element):
            blip._elements[start] = next

    # No match found, return immediately without generating op.
    if not hit_found:
      return

    operation = blip._operation_queue.document_modify(blip.wave_id,
                                                      blip.wavelet_id,
                                                      blip.blip_id)
    for param, value in self._params.items():
      operation.set_param(param, value)

    modify_action = {'modifyHow': modify_how}
    if modify_how == BlipRefs.DELETE:
      pass
    elif modify_how == BlipRefs.UPDATE_ELEMENT:
      modify_action['elements'] = updated_elements
    elif (modify_how == BlipRefs.REPLACE or
          modify_how == BlipRefs.INSERT or
          modify_how == BlipRefs.INSERT_AFTER):
      if callable(what):
        what = matched
      if what:
        if not isinstance(next, element.Element):
          modify_action['values'] = [util.force_unicode(value) for value in what]
        else:
          modify_action['elements'] = what
    elif modify_how == BlipRefs.ANNOTATE:
      modify_action['values'] = [x[1] for x in what]
      modify_action['annotationKey'] = what[0][0]
    elif modify_how == BlipRefs.CLEAR_ANNOTATION:
      modify_action['annotationKey'] = what[0]
    if bundled_annotations:
      modify_action['bundledAnnotations'] = [
          {'key': key, 'value': value} for key, value in bundled_annotations]
    operation.set_param('modifyAction', modify_action)

    return self

  def insert(self, what, bundled_annotations=None):
    """Inserts what at the matched positions."""
    return self._execute(
        BlipRefs.INSERT, what, bundled_annotations=bundled_annotations)

  def insert_after(self, what, bundled_annotations=None):
    """Inserts what just after the matched positions."""
    return self._execute(
        BlipRefs.INSERT_AFTER, what, bundled_annotations=bundled_annotations)

  def replace(self, what, bundled_annotations=None):
    """Replaces the matched positions with what."""
    return self._execute(
        BlipRefs.REPLACE, what, bundled_annotations=bundled_annotations)

  def delete(self):
    """Deletes the content at the matched positions."""
    return self._execute(BlipRefs.DELETE, None)

  def annotate(self, name, value=None):
    """Annotates the content at the matched positions.

    You can either specify both name and value to set the
    same annotation, or supply as the first parameter something
    that yields name/value pairs. The name and value should both be strings.
    """
    if value is None:
      what = name
    else:
      what = (name, value)
    return self._execute(BlipRefs.ANNOTATE, what)

  def clear_annotation(self, name):
    """Clears the annotation at the matched positions."""
    return self._execute(BlipRefs.CLEAR_ANNOTATION, name)

  def update_element(self, new_values):
    """Update an existing element with a set of new values.

    For example, this code would update a button value:
    button.update_element({'value': 'Yes'})
    This code would update the 'seen' key in a gadget's state:
    gadget.update_element({'seen': 'yes'})

    Args:
      new_values: A dictionary of property names and values.
    """
    return self._execute(BlipRefs.UPDATE_ELEMENT, new_values)

  def __nonzero__(self):
    """Return whether we have a value."""
    for start, end in self._hits():
      return True
    return False

  def value(self):
    """Convenience method to convert a BlipRefs to value of its first match."""
    for start, end in self._hits():
      if end - start == 1 and start in self._blip._elements:
        return self._blip._elements[start]
      else:
        return self._blip.text[start:end]
    raise ValueError('BlipRefs has no values')

  def __getattr__(self, attribute):
    """Mirror the getattr of value().

    This allows for clever things like
    first(IMAGE).url

    or

    blip.annotate_with(key, value).upper()
    """
    return getattr(self.value(), attribute)

  def __radd__(self, other):
    """Make it possible to add this to a string."""
    return other + self.value()

  def __cmp__(self, other):
    """Support comparision with target."""
    return cmp(self.value(), other)

  def __iter__(self):
    for start_end in self._hits():
      yield start_end


class Blip(object):
  """Models a single blip instance.

  Blips are essentially the documents that make up a conversation. Blips can
  live in a hierarchy of blips. A root blip has no parent blip id, but all
  blips have the ids of the wave and wavelet that they are associated with.

  Blips also contain annotations, content and elements, which are accessed via
  the Document object.
  """ 

  def __init__(self, json, other_blips, operation_queue, thread=None,
               reply_threads=None):
    """Inits this blip with JSON data.

    Args:
      json: JSON data dictionary from Wave server.
      other_blips: A dictionary like object that can be used to resolve
        ids of blips to blips.
      thread: The BlipThread object that this blip belongs to.
      reply_threads: A list BlipThread objects that are replies to this blip.
      operation_queue: An OperationQueue object to store generated operations
        in.
    """
    self._blip_id = json.get('blipId')
    self._reply_threads = reply_threads or []
    self._thread = thread
    self._operation_queue = operation_queue
    self._child_blip_ids = list(json.get('childBlipIds', []))
    self._content = json.get('content', '')
    self._contributors = set(json.get('contributors', []))
    self._creator = json.get('creator')
    self._last_modified_time = json.get('lastModifiedTime', 0)
    self._version = json.get('version', 0)
    self._parent_blip_id = json.get('parentBlipId')
    self._wave_id = json.get('waveId')
    self._wavelet_id = json.get('waveletId')
    if isinstance(other_blips, Blips):
      self._other_blips = other_blips
    else:
      self._other_blips = Blips(other_blips)
    self._annotations = Annotations(operation_queue, self)
    for annjson in json.get('annotations', []):
      r = annjson['range']
      self._annotations._add_internal(annjson['name'],
                                      annjson['value'],
                                      r['start'],
                                      r['end'])
    self._elements = {}
    json_elements = json.get('elements', {})
    for elem in json_elements:
      self._elements[int(elem)] = element.Element.from_json(json_elements[elem])
    self.raw_data = json

  @property
  def blip_id(self):
    """The id of this blip."""
    return self._blip_id

  @property
  def wave_id(self):
    """The id of the wave that this blip belongs to."""
    return self._wave_id

  @property
  def wavelet_id(self):
    """The id of the wavelet that this blip belongs to."""
    return self._wavelet_id

  @property
  def child_blip_ids(self):
    """The list of the ids of this blip's children."""
    return self._child_blip_ids

  @property
  def child_blips(self):
    """The list of blips that are children of this blip."""
    return [self._other_blips[blid_id] for blid_id in self._child_blip_ids
                if blid_id in self._other_blips]

  @property
  def thread(self):
    """The thread that this blip belongs to."""
    return self._thread

  @property
  def reply_threads(self):
    """The list of threads that are replies to this blip."""
    return self._reply_threads

  @property
  def inline_reply_threads(self):
    # TODO: Consider moving to constructor
    inline_reply_threads = []
    for reply_thread in self._reply_threads:
      if reply_thread.location > -1:
        inline_reply_threads.append(reply_thread)
    return inline_reply_threads

  @property
  def contributors(self):
    """The set of participant ids that contributed to this blip."""
    return self._contributors

  @property
  def creator(self):
    """The id of the participant that created this blip."""
    return self._creator

  @property
  def last_modified_time(self):
    """The time in seconds since epoch when this blip was last modified."""
    return self._last_modified_time

  @property
  def version(self):
    """The version of this blip."""
    return self._version

  @property
  def parent_blip_id(self):
    """The parent blip_id or None if this is the root blip."""
    return self._parent_blip_id

  @property
  def parent_blip(self):
    """The parent blip or None if it is the root."""
    # if parent_blip_id is None, get will also return None
    return self._other_blips.get(self._parent_blip_id)

  @property
  def inline_blip_offset(self):
    """The offset in the parent if this blip is inline or -1 if not.

    If the parent is not in the context, this function will always
    return -1 since it can't determine the inline blip status.
    """
    parent = self.parent_blip
    if not parent:
      return -1
    for offset, el in parent._elements.items():
      if el.type == element.Element.INLINE_BLIP_TYPE and el.id == self.blip_id:
        return offset
    return -1

  def is_root(self):
    """Returns whether this is the root blip of a wavelet."""
    return self._parent_blip_id is None

  @property
  def annotations(self):
    """The annotations for this document."""
    return self._annotations

  @property
  def elements(self):
    """Returns a list of elements for this document.
    The elements of a blip are things like forms elements and gadgets
    that cannot be expressed as plain text. In the text of the blip, you'll
    typically find a space as a place holder for the element.
    If you want to retrieve the element at a particular index in the blip, use
    blip[index].value().
    """
    return self._elements.values()

  def __len__(self):
    return len(self._content)

  def __getitem__(self, item):
    """returns a BlipRefs for the given slice."""
    if isinstance(item, slice):
      if item.step:
        raise errors.Error('Step not supported for blip slices')
      return self.range(item.start, item.stop)
    else:
      return self.at(item)

  def __setitem__(self, item, value):
    """short cut for self.range/at().replace(value)."""
    self.__getitem__(item).replace(value)

  def __delitem__(self, item):
    """short cut for self.range/at().delete()."""
    self.__getitem__(item).delete()

  def _shift(self, where, inc):
    """Move element and annotations after 'where' up by 'inc'."""
    new_elements = {}
    for idx, el in self._elements.items():
      if idx >= where:
        idx += inc
      new_elements[idx] = el
    self._elements = new_elements
    self._annotations._shift(where, inc)
    
  def _delete_annotations(self, start, end):
    """Delete all annotations between 'start' and 'end'."""
    for annotation_name in self._annotations.names():
      self._annotations._delete_internal(annotation_name, start, end)

  def all(self, findwhat=None, maxres=-1, **restrictions):
    """Returns a BlipRefs object representing all results for the search.
    If searching for an element, the restrictions can be used to specify
    additional element properties to filter on, like the url of a Gadget.
    """
    return BlipRefs.all(self, findwhat, maxres, **restrictions)

  def first(self, findwhat=None, **restrictions):
    """Returns a BlipRefs object representing the first result for the search.
    If searching for an element, the restrictions can be used to specify
    additional element properties to filter on, like the url of a Gadget.
    """
    return BlipRefs.all(self, findwhat, 1, **restrictions)

  def at(self, index):
    """Returns a BlipRefs object representing a 1-character range."""
    return BlipRefs.range(self, index, index + 1)

  def range(self, start, end):
    """Returns a BlipRefs object representing the range."""
    return BlipRefs.range(self, start, end)

  def serialize(self):
    """Return a dictionary representation of this blip ready for json."""
    return {'blipId': self._blip_id,
            'childBlipIds': list(self._child_blip_ids),
            'content': self._content,
            'creator': self._creator,
            'contributors': list(self._contributors),
            'lastModifiedTime': self._last_modified_time,
            'version': self._version,
            'parentBlipId': self._parent_blip_id,
            'waveId': self._wave_id,
            'waveletId': self._wavelet_id,
            'annotations': self._annotations.serialize(),
            'elements': dict([(index, e.serialize())
                              for index, e in self._elements.items()])
           }

  def proxy_for(self, proxy_for_id):
    """Return a view on this blip that will proxy for the specified id.

    A shallow copy of the current blip is returned with the proxy_for_id
    set. Any modifications made to this copy will be done using the
    proxy_for_id, i.e. the robot+<proxy_for_id>@appspot.com address will
    be used.
    """
    util.check_is_valid_proxy_for_id(proxy_for_id)
    operation_queue = self._operation_queue.proxy_for(proxy_for_id)
    res = Blip(json={},
               other_blips={},
               operation_queue=operation_queue)
    res._blip_id = self._blip_id
    res._child_blip_ids = self._child_blip_ids
    res._content = self._content
    res._contributors = self._contributors
    res._creator = self._creator
    res._last_modified_time = self._last_modified_time
    res._version = self._version
    res._parent_blip_id = self._parent_blip_id
    res._wave_id = self._wave_id
    res._wavelet_id = self._wavelet_id
    res._other_blips = self._other_blips
    res._annotations = self._annotations
    res._elements = self._elements
    res.raw_data = self.raw_data
    return res

  @property
  def text(self):
    """Returns the raw text content of this document."""
    return self._content

  def find(self, what, **restrictions):
    """Iterate to matching bits of contents.

    Yield either elements or pieces of text.
    """
    br = BlipRefs.all(self, what, **restrictions)
    for start, end in br._hits():
      if end - start == 1 and start in self._elements:
        yield self._elements[start]
      else:
        yield self._content[start:end]
    raise StopIteration

  def append(self, what, bundled_annotations=None):
    """Convenience method covering a common pattern."""
    return BlipRefs.all(self, findwhat=None).insert_after(
        what, bundled_annotations=bundled_annotations)

  def continue_thread(self):
    """Create and return a blip in the same thread as this blip."""
    blip_data = self._operation_queue.blip_continue_thread(self.wave_id,
                                                           self.wavelet_id,
                                                           self.blip_id)
    new_blip = Blip(blip_data, self._other_blips, self._operation_queue,
                    thread=self._thread)
    if self._thread:
      self._thread._add_internal(new_blip)
    self._other_blips._add(new_blip)
    return new_blip

  def reply(self):
    """Create and return a reply to this blip."""
    blip_data = self._operation_queue.blip_create_child(self.wave_id,
                                                        self.wavelet_id,
                                                        self.blip_id)
    new_blip = Blip(blip_data, self._other_blips, self._operation_queue)
    self._other_blips._add(new_blip)
    return new_blip

  def append_markup(self, markup):
    """Interpret the markup text as xhtml and append the result to the doc.

    Args:
      markup: The markup'ed text to append.
    """
    markup = util.force_unicode(markup)
    self._operation_queue.document_append_markup(self.wave_id,
                                                 self.wavelet_id,
                                                 self.blip_id,
                                                 markup)
    self._content += util.parse_markup(markup)

  def insert_inline_blip(self, position):
    """Inserts an inline blip into this blip at a specific position.

    Args:
      position: Position to insert the blip at. This has to be greater than 0.

    Returns:
      The JSON data of the blip that was created.
    """
    if position <= 0:
      raise IndexError(('Illegal inline blip position: %d. Position has to ' +
                        'be greater than 0.') % position)

    blip_data = self._operation_queue.document_inline_blip_insert(
        self.wave_id,
        self.wavelet_id,
        self.blip_id,
        position)
    new_blip = Blip(blip_data, self._other_blips, self._operation_queue)
    self._other_blips._add(new_blip)
    return new_blip
