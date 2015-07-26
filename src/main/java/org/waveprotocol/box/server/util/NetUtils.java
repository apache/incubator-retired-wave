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
package org.waveprotocol.box.server.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Networking utility methods.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class NetUtils {

  /** Default port number for HTTP server addresses. */
  public static final short HTTP_DEFAULT_PORT = 80;

  /**
   * Parses a string of format "host[:port]" into a InetSocketAddress.
   * The port defaults to HTTP_DEFAULT_PORT.
   */
  public static InetSocketAddress parseHttpAddress(String address) throws IOException {
    String[] parts = address.split(":");
    if (parts.length < 1) {
      throw new IOException("Empty address");
    }
    if (parts.length > 2) {
      throw new IOException("Too many colons");
    }
    String host = parts[0];
    short port = HTTP_DEFAULT_PORT;
    if (parts.length == 2) {
      try {
        port = Short.parseShort(parts[1]);
        if (port <= 0) {
          throw new IOException("Invalid port number: " + parts[1]);
        }
      } catch (NumberFormatException e) {
        throw new IOException("Invalid port number: " + parts[1], e);
      }
    }
    InetAddress addr = InetAddress.getByName(host);
    return new InetSocketAddress(addr, port);
  }


  private NetUtils() { } // prevents instantiation
}
