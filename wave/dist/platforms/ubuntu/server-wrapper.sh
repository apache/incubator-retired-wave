#!/bin/sh

#     Licensed to the Apache Software Foundation (ASF) under one
#     or more contributor license agreements.  See the NOTICE file
#     distributed with this work for additional information
#     regarding copyright ownership.  The ASF licenses this file
#     to you under the Apache License, Version 2.0 (the
#     "License"); you may not use this file except in compliance
#     with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#     Unless required by applicable law or agreed to in writing,
#     software distributed under the License is distributed on an
#     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#     KIND, either express or implied.  See the License for the
#     specific language governing permissions and limitations
#     under the License.
#

# This script is a wrappeer around run-seerver.sh that redirects output to a log file.
# It is used by daemon-script.sh
#

# Exit if WAVE_HOME isn't defined or points to a non-existant directory.
if [ -z "$WAVE_HOME" -o ! -d "$WAVE_HOME" ]; then
    echo WAVE_HOME is not defined!
    exit 1
fi

# Exit if there is no log dir under $WAVE_HOME
if [ ! -d $WAVE_HOME/log ]; then
    echo "The log dir ${WAVE_HOME}/log doesn't exist"
    exit 1
fi

cd $WAVE_HOME

DATE=`date +%Y%m%d_%H%M%S`

# Create a symbolic link named wave.log that will point to the msot recent log file.
rm -f ${WAVE_HOME}/log/wave.log
ln -s ${WAVE_HOME}/log/wave_${DATE}.log ${WAVE_HOME}/log/wave.log

# Exec the wave server so that the daemon script can track the pid.
exec ${WAVE_HOME}/run-server.sh > ${WAVE_HOME}/log/wave_${DATE}.log 2>&1
