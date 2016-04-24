#!/usr/bin/env bash

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

# install the dependencies
dnf install -y ant java-1.8.0-openjdk mongodb
# create install location
cd /opt
sudo mkdir apache
cd apache
sudo mkdir wave
# create the binary
cd /vagrant
./gradlew clean createDist

# Get Apache Wave version
WAVE_VERSION=`sed "s/[\\t ]*=[\\t ]*/=/g" wave/config/wave.conf | grep ^version= | cut -f2 -d=`

cd distributions
sudo tar -C /opt/apache/wave -zxvf apache-wave-bin-$WAVE_VERSION.tar
cd ..
cp scripts/vagrant/application.conf /opt/apache/wave/apache-wave/config/application.conf