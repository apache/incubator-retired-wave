echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

if not exist "third_party\solr\solr-4.9.1\example" (
  echo "Please download Solr by running: ant get-third-party-solr-dep "
  echo "Or download it manually from http://apache.spd.co.il/lucene/solr/4.9.1/ into third_party\solr and unzip there."
  pause
  exit 1
)

cd third_party\solr\solr-4.9.1\example
java -jar start.jar
pause
