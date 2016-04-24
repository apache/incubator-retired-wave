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

package org.waveprotocol.wave.model.operation;

import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;

/**
 * An exception thrown to indicate an operation was invalid or ill-formed.
 *
 * An operation is well-formed if it satisfies some basic sanity checks, such
 * as only positive arguments for retain, no two successive annotation boundaries,
 * correct nesting of starts and ends, etc.
 *
 * Validity of an operation depends on the document that it is supposed to
 * apply to.  Validity checks include that the items specified in deletion or
 * update components as well as annotations actually match the document, and
 * that the operation preserves the XML schema.
 *
 * Code that deals with operations may assume that they are well-formed;
 * ill-formed operations should be rejected at construction or deserialization
 * time.  It cannot always assume they are valid for a given context.
 */
@SuppressWarnings("serial")
public class OperationException extends Exception {
  private ViolationCollector violations = null;

  /**
   * Constructs a new exception with null as its detail message.
   */
  public OperationException() {
  }

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message The detail message.
   */
  public OperationException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified cause.
   *
   * @param cause The cause.
   */
  public OperationException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message The detail message.
   * @param cause The cause.
   */
  public OperationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param violations Violations recorded during validation checking. The
   *        thrower should not hold onto the violations object.
   */
  public OperationException(ViolationCollector violations) {
    super("Validation failure: " + violations);
    this.violations = violations;
  }

  /**
   * @return true if there is validation violation information associated with
   *         this exception
   */
  public boolean hasViolationsInformation() {
    return violations != null;
  }

  /**
   * @return true if the worst problem was that the schema was violated
   */
  public boolean isSchemaViolation() {
    return violations != null
        && violations.getValidationResult() == ValidationResult.INVALID_SCHEMA;
  }

  /**
   * @return validation violation exception, if any is available. The caller
   *         should not modify or reuse this object.
   */
  public ViolationCollector getViolations() {
    return violations;
  }
}
