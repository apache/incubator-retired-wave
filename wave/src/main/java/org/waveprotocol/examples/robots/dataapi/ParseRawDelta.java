/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.examples.robots.dataapi;

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.util.CharBase64;

/**
 * Parses a base64 encoded raw delta.
 *
 * You can compile and run this program by building the dist jars with "ant dist"
 * and then running the follwing commands in the src directory:
 *
 * javac -classpath '../dist/*' org/waveprotocol/examples/robots/dataapi/ParseRawDelta.java
 * java -classpath '../dist/*:.' org.waveprotocol.examples.robots.dataapi.ParseRawDelta <x>
 *
 * where <x> is your base 64 encoded raw delta.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class ParseRawDelta {

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println(
          "Usage: java [options] " + ParseRawDelta.class.getName() + " <base64 encoded raw delta>");
      System.exit(1);
    } else {
      System.out.println("Parsing base64 string: " + args[0]);
      byte[] bytes = CharBase64.decode(args[0]);
      ProtocolAppliedWaveletDelta parsed = ProtocolAppliedWaveletDelta.parseFrom(bytes);
      System.out.println(parsed.toString());
    }
  }
}
