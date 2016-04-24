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

package org.waveprotocol.wave.model.experimental.schema;

/**
 * A checker for regular expressions.
 *
 * TODO(user): This needs to be rewritten to properly handle '^' and '$' characters.
 *
 */
final class RegularExpressionChecker {

  /*
   * The following is a list of what each of the subclasses and static objects
   * of <code>State</code> represent.
   *
   * HeadState: The state directly after an unescaped '(', indicating the start
   * of a nested group.
   *
   * TailState: The state while inside a nested group, not at the beginning of a
   * nested group, and outside any escape.
   *
   * EscapeState: The state directly after an escaping '\', indicating that the
   * next character should be interpreted as part of the escape.
   *
   * BASE_HEAD_STATE: The state at position 0.
   *
   * BASE_TAIL_STATE: The state at the top level after position 0 and outside
   * any escape nested group.
   */

  private abstract static class State {

    abstract State nextState(int position, char character) throws InvalidSchemaException;
    abstract void endOfInput() throws InvalidSchemaException;

  }

  private static final class HeadState extends State {

    private final State stack;

    HeadState(State stack) {
      this.stack = stack;
    }

    @Override
    State nextState(int position, char character) throws InvalidSchemaException {
      switch (character) {
        case '\\':
          return new EscapeState(new TailState(stack));
        case '(':
          return new HeadState(new TailState(stack));
        case ')':
          return stack;
        case '*':
        case '?':
          throw new InvalidSchemaException(
              "Unexpected '" + character + "' at position " + position);
        default:
          return new TailState(stack);
      }
    }

    @Override
    void endOfInput() throws InvalidSchemaException {
      throw new InvalidSchemaException("Unmatched '('");
    }

  }

  private static final class TailState extends State {

    private final State stack;

    TailState(State stack) {
      this.stack = stack;
    }

    @Override
    State nextState(int position, char character) {
      switch (character) {
        case '\\':
          return new EscapeState(this);
        case '(':
          return new HeadState(this);
        case ')':
          return stack;
        default:
          return this;
      }
    }

    @Override
    void endOfInput() throws InvalidSchemaException {
      throw new InvalidSchemaException("Unmatched '('");
    }

  }

  private static final class EscapeState extends State {

    private final State stack;

    EscapeState(State stack) {
      this.stack = stack;
    }

    @Override
    State nextState(int position, char character) throws InvalidSchemaException {
      switch (character) {
        case '\\':
        case '|':
        case '*':
        case '?':
        case '.':
        case '(':
        case ')':
          return stack;
        default:
          throw new InvalidSchemaException(
              "Unexpected character after backslash at position " + position);
      }
    }

    @Override
    void endOfInput() throws InvalidSchemaException {
      throw new InvalidSchemaException("Backslash at end of expression");
    }

  }

  private static final State BASE_HEAD_STATE = new State() {

    @Override
    State nextState(int position, char character) throws InvalidSchemaException {
      assert position == 0;
      switch (character) {
        case '\\':
          return new EscapeState(BASE_TAIL_STATE);
        case '(':
          return new HeadState(BASE_TAIL_STATE);
        case ')':
        case '*':
        case '?':
          throw new InvalidSchemaException("Unexpected '" + character + "' at position 0");
        default:
          return BASE_TAIL_STATE;
      }
    }

    @Override
    void endOfInput() {}

  };

  private static final State BASE_TAIL_STATE = new State() {

    @Override
    State nextState(int position, char character) throws InvalidSchemaException {
      switch (character) {
        case '\\':
          return new EscapeState(this);
        case '(':
          return new HeadState(this);
        case ')':
          throw new InvalidSchemaException("Unexpected ')' at position " + position);
        default:
          return this;
      }
    }

    @Override
    void endOfInput() {}

  };

  /**
   * Checks whether the given string is a regular expression, and throws an
   * exception if not.
   *
   * @param re a string
   * @throws InvalidSchemaException if the given string is not a regular
   *         expression
   */
  static void checkRegularExpression(String re) throws InvalidSchemaException {
    State state = BASE_HEAD_STATE;
    for (int i = 0; i < re.length(); ++i) {
      state = state.nextState(i, re.charAt(i));
    }
    state.endOfInput();
  }

}
