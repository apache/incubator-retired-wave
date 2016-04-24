#!/bin/bash

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

# This script will start the Solr server.

# Make sure the third_party/solr/solr-4.9.1/example folder exists.
if [ ! -d third_party/solr/solr-4.9.1/example ]; then
  echo "Please download Solr by running: ant get-third-party-solr-dep "
  echo "Or download it manually from http://apache.spd.co.il/lucene/solr/4.9.1/ into third_party/solr and unzip there."
  exit 1
fi

cd third_party/solr/solr-4.9.1/example
exec java -jar start.jar
