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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;

/**
 * A ModifiableDocument that takes DocumentOperations that are applied to it
 * and prints out Java code to reconstruct these DocumentOperations.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class DocumentOperationCodePrinter {

  // TODO(ohler): bring this functionality back, but put it in DocOpUtil.

  public static String getCode(DocOp op, String targetDocumentSymbolName) {
    return "Java code that corresponds to " + DocOpUtil.toConciseString(op);
  }

  /**
   * Handy utility method to put the java code into a string
   *
   * @param op
   * @param lineDelimiter e.g. "\n"
   * @param targetDocumentSymbolName
   * @return java code
   */
  // Using a lineDelimiter that has no \n in it won't interact well with
  // the way actionWithAttributes tries to indent output.
  public static String getCode(DocOp op,
      String targetDocumentSymbolName, final String lineDelimiter) {
    return "Java code that corresponds to " + DocOpUtil.toConciseString(op);
  }

  /**
   * Minimal stream interface to avoid the need for external dependencies
   */
  public interface SimpleStream {
    /** Send a line to the stream */
    void println(String str);
  }

  private DocumentOperationCodePrinter(SimpleStream out, String targetDocumentSymbolName) {
  }

}
