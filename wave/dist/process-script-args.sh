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

# Source this script to parse debug-related flags and load arguments into ARGC
# and ARGV.  After this script has been run it will not be possible to access
# the command-line arguments ($1, $2, ...) directly.  Instead the arguments that
# were not understood by the script will be stored as a count in ARGC and the
# argument values in the ARGV.

ARGC=0
declare -a ARGV
SUSPEND="n"
DEBUG_MODE="off"
DEBUG_PORT="8000"
while [ -n "$1" ]; do
  case $1 in
    --debug) DEBUG_MODE="on";;
    --suspend) SUSPEND="y";;
    --debug_port=*) DEBUG_PORT=${1#--debug_port=};;
    *) ARGV[$ARGC]="$1"; ARGC=$(($ARGC + 1));
  esac
  shift
done

if [ $DEBUG_MODE = "on" ] ; then
  DEBUG_FLAGS=-Xrunjdwp:transport=dt_socket,server=y,suspend=$SUSPEND,address=$DEBUG_PORT
fi
