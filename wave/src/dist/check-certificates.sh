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

# This script will test your certificates, verifying that
# the options are set correctly in the config files, that the
# public and private keys match, and that the whole certificate
# chain can be verified up to the root certificate.

srv_cfg=config/server.config
if [ ! -f $srv_cfg ]; then
  echo "You need to generate a valid $srv_cfg file."; exit 1
fi
fed_cfg=config/server.federation.config
if [ ! -f $fed_cfg ]; then
  echo "You need to generate a valid $fed_cfg file."; exit 1
fi

function get()
{
  # retrieve value from federation config file. may fail if a variable is set in both files
  grep "^\s*$1\>" "$fed_cfg" "$srv_cfg"| sed 's/.*=\s*//g' | tail -1
}

if [ "$(get waveserver_disable_verification)" != "false" ]; then
  echo "ERROR: waveserver_disable_verification should be set to false"
  exit 1
fi

if [ "$(get waveserver_disable_signer_verification)" != "false" ]; then
  echo "ERROR: waveserver_disable_signer_verification should be set to false"
  exit 1
fi

if [ ! -e "$(get certificate_private_key)" ]; then
  echo "ERROR: Private key \"$(get certificate_private_key)\" does not exist"
  exit 1
fi

# Break apart the certificate list on the commas.
certlist=(`echo $(get certificate_files) | sed 's/,/ /g'`)

if [ "`openssl x509 -modulus -in ${certlist[0]} -noout`" != "`openssl \
  rsa -in $(get certificate_private_key)  -modulus -noout`" ]; then
  echo "ERROR: Public and private key do not match!"
  exit 1
fi

# Reverse the order of the list for passing into openssl.
len=${#certlist[@]}
for (( i = 0; $i < $len/2; i++ )); do
  swap=$len-$i-1
  tmp=${certlist[i]}
  certlist[i]=${certlist[$swap]}
  certlist[$swap]=$tmp
done

# Verify that each file in the certificate list exists.
for (( i=0; $i < $len; i++ )); do
  if [ ! -e ${certlist[$i]} ]; then
    echo "ERROR: Certificate file does not exist:" ${certlist[$i]}
    exit 1
  fi
done

# Verify the certificate chain.
if (( $len > 1 )); then
  verifycmd="openssl verify -CAfile ${certlist[@]}"
else
  verifycmd="openssl verify ${certlist[@]}"
fi

if $verifycmd | grep -q "OK$" ; then
  echo "SUCCESS: The certificates have been verified and are working correctly"
  exit 0
else
  echo "ERROR: Certificate chain failed to verify"
  $verifycmd
  exit 1
fi
