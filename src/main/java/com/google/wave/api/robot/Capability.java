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

package com.google.wave.api.robot;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;
import com.google.wave.api.Context;
import com.google.wave.api.event.EventType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Capability represents a Robot's interest in handling a given event. The
 * Robot can request that additional context and document content be sent with
 * the event.
 *
 */
public class Capability {

  /**
   * The context provided by default if non is declared.
   */
  public static final List<Context> DEFAULT_CONTEXT =
      ImmutableList.of(Context.ROOT, Context.PARENT, Context.CHILDREN);

  /**
   * The list of contexts (parent, children, siblings, all) to send to the Robot
   * for this capability.
   */
  private final List<Context> contexts;

  /**
   * The associated eventType.
   */
  private EventType eventType;

  /**
   * The filter applicable to this event as a regular expression.
   */
  private final Pattern filter;

  /**
   * Configures a Robot capability with the content, contexts and filter
   * specified.
   */
  public Capability(EventType eventType, List<Context> contexts, String filter) {
    Preconditions.checkNotNull(contexts);
    Preconditions.checkNotNull(filter);
    this.contexts = ImmutableList.copyOf(contexts);
    this.eventType = eventType;
    if (filter.isEmpty()) {
      this.filter = null;
    } else {
      this.filter = Pattern.compile(filter);
    }
  }

  /**
   * Convenience constructor with default empty filter.
   */
  public Capability(EventType eventType, List<Context> contexts) {
    this(eventType, contexts, "");
  }

  /**
   * Convenience constructor with default empty filter and default context.
   */
  public Capability(EventType eventType) {
    this(eventType, DEFAULT_CONTEXT, "");
  }

  /**
   * @return the list of contexts requested to be sent for this capability.
   */
  public List<Context> getContexts() {
    return contexts;
  }

  public EventType getEventType() {
    return eventType;
  }

  /**
   * @return whether the set filter matches toMatch
   */
  public boolean matches(String toMatch) {
    if (filter == null) {
      return true;
    } else {
      Matcher matcher = filter.matcher(toMatch);
      return matcher.find();
    }
  }

  /**
   * @return the filter pattern
   */
  public String getFilter() {
    if (filter != null) {
      return filter.pattern();
    } else {
      return "";
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((contexts == null) ? 0 : contexts.hashCode());
    result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
    result = prime * result + ((filter == null) ? 0 : filter.pattern().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Capability)) {
      return false;
    }
    Capability other = (Capability) obj;
    if (contexts == null) {
      if (other.contexts != null) {
        return false;
      }
    } else if (!contexts.equals(other.contexts)) {
      return false;
    }
    if (eventType == null) {
      if (other.eventType != null) {
        return false;
      }
    } else if (!eventType.equals(other.eventType)) {
      return false;
    }
    if (filter == null) {
      if (other.filter != null) {
        return false;
      }
    } else if (!filter.pattern().equals(other.filter.pattern())) {
      return false;
    }
    return true;
  }

}
