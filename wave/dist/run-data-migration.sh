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

# This script will start the data migration between two different store types
# Initially this tool is intended to migrate deltas from file to mongodb store
# Run "ant dist-server" before to use this script

# The version of Wave in a Box, extracted from the build.properties file
WAVEINABOX_VERSION=`sed "s/[\\t ]*=[\\t ]*/=/g" config/wave.conf | grep ^version= | cut -f2 -d=`
echo wave-in-a-box-server-$WAVEINABOX_VERSION.jar

exec java -cp dist/wave-$WAVEINABOX_VERSION.jar org.waveprotocol.box.server.DataMigrationTool $*
