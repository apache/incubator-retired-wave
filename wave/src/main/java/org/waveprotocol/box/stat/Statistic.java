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
package org.waveprotocol.box.stat;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles tracking of statistic on a per-object or per-class basis.
 *
 * @author David Byttow
 */
public class Statistic {
  // TODO: Track stats over-time as well, for relative usage.
  private static final Logger LOG = Logger.getLogger(Statistic.class.getName());

  /**
   * Represents a single tracked stat.
   */
  public static abstract class Entry {
    final String name;
    final String help;

    Entry(String name, String help) {
      this.name = name;
      this.help = help;
    }

    /**
     * @return name of this stat.
     */
    public String getName() {
      return name;
    }

    /**
     * @return human readable description of the stat.
     */
    public String getHelp() {
      return help;
    }

    @Override
    public String toString() {
      return getValue();
    }

    /**
     * @return the current value of the stat.
     */
    public abstract String getValue();
  }

  @GwtIncompatible("Uses Reflection")
  private static class FieldEntry extends Entry {
    final Field field;
    final Object ref;

    FieldEntry(Stat stat, Field field, Object ref) {
      super(stat.name(), stat.help());
      this.field = field;
      this.ref = ref;
    }

    @Override
    public String getValue() {
      field.setAccessible(true);
      try {
        return field.get(ref).toString();
      } catch (IllegalArgumentException e) {
        LOG.log(Level.WARNING, "Failed to get field.", e);
      } catch (IllegalAccessException e) {
        LOG.log(Level.WARNING, "Failed to access field.", e);
      }
      return "";
    }
  }

  private static final List<Entry> trackedStats = Lists.newLinkedList();

  /**
   * Tracks all static fields of a class marked with a {@link Stat} annotation.
   *
   * @param clazz the class type to track.
   */
  @GwtIncompatible("Uses getDeclaredFields / Reflection")
  public static void trackClass(Class<?> clazz) {
    for (Field field : clazz.getDeclaredFields()) {
      Stat stat = field.getAnnotation(Stat.class);
      if (stat != null) {
        trackedStats.add(new FieldEntry(stat, field, null));
      }
    }
  }

  /**
   * @return the collection of tracked stats.
   */
  public static Collection<Entry> getStats() {
    return ImmutableList.copyOf(trackedStats);
  }
}
