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

# This script will make a set of certificates for federation.
# To actually federate, the certificiates will need to be signed.
#
# For instructions, see: http://www.waveprotocol.org/federation/certificates

NAME=$1

if [ "$NAME" == '' ]
then
  echo "Usage: $0 <domain name>" 1>&2
  echo "See http://www.waveprotocol.org/federation/certificates\
 for more information" 1>&2
  exit 1
fi

echo "1) Generating key for $NAME in '$NAME.key' ..."
echo
openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -out "$NAME.key"

echo
echo "2) Generating certificate request for $NAME in '$NAME.crt' ..."
echo
openssl req -new -x509 -nodes -sha1 -days 365 -key "$NAME.key" -out "$NAME.crt"
