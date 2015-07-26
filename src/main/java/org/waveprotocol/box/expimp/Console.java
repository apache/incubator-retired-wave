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
package org.waveprotocol.box.expimp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Input/output for Export/Import utilities.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class Console {

  private static final Logger log = Logger.getLogger("ExportImport");

  public static void print(String line) {
    System.out.print(line);
  }

  public static void println(String line) {
    System.out.println(line);
  }

  public static void println() {
    System.out.println();
  }

  public static void error(String error) {
    System.err.println();
    System.err.println(error);
  }

  public static void error(String error, Exception ex) {
    log.log(Level.SEVERE, error, ex);
    System.err.println();
    System.err.println(error);
    System.err.println(ex.toString());
  }

  public static String readLine() throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    return in.readLine();
  }
}
