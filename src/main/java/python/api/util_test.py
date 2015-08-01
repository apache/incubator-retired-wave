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

"""Unit tests for the util module."""


__author__ = 'davidbyttow@google.com (David Byttow)'


import unittest
import urllib

import ops
import util


class TestUtils(unittest.TestCase):
  """Tests utility functions."""

  def testIsIterable(self):
    self.assertTrue(util.is_iterable([]))
    self.assertTrue(util.is_iterable({}))
    self.assertTrue(util.is_iterable(set()))
    self.assertTrue(util.is_iterable(()))
    self.assertFalse(util.is_iterable(42))
    self.assertFalse(util.is_iterable('list?'))
    self.assertFalse(util.is_iterable(object))

  def testIsDict(self):
    self.assertFalse(util.is_dict([]))
    self.assertTrue(util.is_dict({}))
    self.assertFalse(util.is_dict(set()))
    self.assertFalse(util.is_dict(()))
    self.assertFalse(util.is_dict(42))
    self.assertFalse(util.is_dict('dict?'))
    self.assertFalse(util.is_dict(object))

  def testIsUserDefinedNewStyleClass(self):
    class OldClass:
      pass

    class NewClass(object):
      pass

    self.assertFalse(util.is_user_defined_new_style_class(OldClass()))
    self.assertTrue(util.is_user_defined_new_style_class(NewClass()))
    self.assertFalse(util.is_user_defined_new_style_class({}))
    self.assertFalse(util.is_user_defined_new_style_class(()))
    self.assertFalse(util.is_user_defined_new_style_class(42))
    self.assertFalse(util.is_user_defined_new_style_class('instance?'))

  def testLowerCamelCase(self):
    self.assertEquals('foo', util.lower_camel_case('foo'))
    self.assertEquals('fooBar', util.lower_camel_case('foo_bar'))
    self.assertEquals('fooBar', util.lower_camel_case('fooBar'))
    self.assertEquals('blipId', util.lower_camel_case('blip_id'))
    self.assertEquals('fooBar', util.lower_camel_case('foo__bar'))
    self.assertEquals('fooBarBaz', util.lower_camel_case('foo_bar_baz'))
    self.assertEquals('f', util.lower_camel_case('f'))
    self.assertEquals('f', util.lower_camel_case('f_'))
    self.assertEquals('', util.lower_camel_case(''))
    self.assertEquals('', util.lower_camel_case('_'))
    self.assertEquals('aBCDEF', util.lower_camel_case('_a_b_c_d_e_f_'))

  def assertListsEqual(self, a, b):
    self.assertEquals(len(a), len(b))
    for i in range(len(a)):
      self.assertEquals(a[i], b[i])

  def assertDictsEqual(self, a, b):
    self.assertEquals(len(a.keys()), len(b.keys()))
    for k, v in a.iteritems():
      self.assertEquals(v, b[k])

  def assertNotRaises(self, exception, function, *args):
    try:
      function(*args)
    except ValueError:
      fail()

  def testSerializeList(self):
    data = [1, 2, 3]
    output = util.serialize(data)
    self.assertListsEqual(data, output)

  def testSerializeDict(self):
    data = {'key': 'value', 'under_score': 'value2'}
    expected = {'key': 'value', 'underScore': 'value2'}
    output = util.serialize(data)
    self.assertDictsEqual(expected, output)

  def testNonNoneDict(self):
    a = {'a': 1, 'b': 1}
    self.assertDictsEqual(a, util.non_none_dict(a))
    b = a.copy()
    b['c'] = None
    self.assertDictsEqual(a, util.non_none_dict(b))

  def testForceUnicode(self):
    self.assertEquals(u"aaa", util.force_unicode("aaa"))
    self.assertEquals(u"12", util.force_unicode(12))
    self.assertEquals(u"\u0430\u0431\u0432",
                      util.force_unicode("\xd0\xb0\xd0\xb1\xd0\xb2"))
    self.assertEquals(u'\u30e6\u30cb\u30b3\u30fc\u30c9',
                      util.force_unicode(u'\u30e6\u30cb\u30b3\u30fc\u30c9'))

  def testSerializeAttributes(self):

    class Data(object):
      def __init__(self):
        self.public = 1
        self._protected = 2
        self.__private = 3

      def Func(self):
        pass

    data = Data()
    output = util.serialize(data)
    # Functions and non-public fields should not be serialized.
    self.assertEquals(1, len(output.keys()))
    self.assertEquals(data.public, output['public'])

  def testStringEnum(self):
    util.StringEnum()
    single = util.StringEnum('foo')
    self.assertEquals('foo', single.foo)
    multi = util.StringEnum('foo', 'bar')
    self.assertEquals('foo', multi.foo)
    self.assertEquals('bar', multi.bar)

  def testParseMarkup(self):
    self.assertEquals('foo', util.parse_markup('foo'))
    self.assertEquals('foo bar', util.parse_markup('foo <b>bar</b>'))
    self.assertEquals('foo\nbar', util.parse_markup('foo<br>bar'))
    self.assertEquals('foo\nbar', util.parse_markup('foo<p indent="3">bar'))

  def testIsValidProxyForId(self):
    self.assertTrue(util.is_valid_proxy_for_id(''))
    self.assertTrue(util.is_valid_proxy_for_id('proxyid'))
    self.assertTrue(util.is_valid_proxy_for_id('proxy-id1+gmail.com'))
    self.assertTrue(util.is_valid_proxy_for_id('proxy-id1_at_gmail.com'))
    self.assertTrue(util.is_valid_proxy_for_id('proxy-id%201_at_gmail.com'))
    self.assertTrue(util.is_valid_proxy_for_id(urllib.quote('proxyid@bar.com')))

    self.assertFalse(util.is_valid_proxy_for_id('proxy id1'))
    self.assertFalse(util.is_valid_proxy_for_id(u'proxy\u0000id1'))
    self.assertFalse(util.is_valid_proxy_for_id(u'proxy\u0009id1'))
    self.assertFalse(util.is_valid_proxy_for_id(u'proxy\u001Fid1'))
    self.assertFalse(util.is_valid_proxy_for_id('proxy@id'))
    self.assertFalse(util.is_valid_proxy_for_id('proxy,id'))
    self.assertFalse(util.is_valid_proxy_for_id('proxy:id'))
    self.assertFalse(util.is_valid_proxy_for_id('proxy<id'))
    self.assertFalse(util.is_valid_proxy_for_id('proxy>id'))
    self.assertFalse(util.is_valid_proxy_for_id(u'proxy\u007Fid'))

  def testCheckIsValidProxyForId(self):
    self.assertRaises(ValueError, util.check_is_valid_proxy_for_id,
                      'foo@bar.com')
    self.assertNotRaises(ValueError, util.check_is_valid_proxy_for_id,
                         None)
    self.assertNotRaises(ValueError, util.check_is_valid_proxy_for_id,
                         '')
    self.assertNotRaises(ValueError, util.check_is_valid_proxy_for_id,
                         'foo+bar.com')
    self.assertNotRaises(ValueError, util.check_is_valid_proxy_for_id,
                         urllib.quote('foo@bar.com'))
    self.assertNotRaises(ValueError, util.check_is_valid_proxy_for_id, None)

if __name__ == '__main__':
  unittest.main()
