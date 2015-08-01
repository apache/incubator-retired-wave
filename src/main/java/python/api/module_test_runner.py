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

"""Module defines the ModuleTestRunnerClass."""


import unittest


class ModuleTestRunner(object):
  """Responsible for executing all test cases in a list of modules."""

  def __init__(self, module_list=None, module_test_settings=None):
    self.modules = module_list or []
    self.settings = module_test_settings or {}

  def RunAllTests(self):
    """Executes all tests present in the list of modules."""
    runner = unittest.TextTestRunner()
    for module in self.modules:
      for setting, value in self.settings.iteritems():
        try:
          setattr(module, setting, value)
        except AttributeError:
          print '\nError running ' + str(setting)
      print '\nRunning all tests in module', module.__name__
      runner.run(unittest.defaultTestLoader.loadTestsFromModule(module))
