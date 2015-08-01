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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.Date;

/**
 * Date formatting utilities.
 *
 */
@SuppressWarnings("deprecation")  // GWT supports Date.getXXX, but java doesn't anymore
public final class DateUtils {

  private final static long SEC_MS = 1000;
  private final static long MIN_MS = 60 * SEC_MS;
  private final static long HOUR_MS = 60 * MIN_MS;
  private final static long DAY_MS = 24 * HOUR_MS;

  // Singleton class.
  private DateUtils() { }
  private static final DateUtils INSTANCE = new DateUtils();

  /**
   * Please avoid invoking getInstance() inside methods but rather inject the
   * instance into the class through the constructor. This will
   *   - make it easier to move to GIN in the future, and
   *   - make the class easier to test since DateUtils needs to be mocked out
   *     in non-GWT tests.
   *
   * @return the shared DateUtils instance
   */
  public static DateUtils getInstance() {
    return INSTANCE;
  }

  /**
   * Formats a date in the past, taking into account how long ago it was
   *
   * @param time The date to format, in ms since whenever
   * @return The formatted date
   */
  public String formatPastDate(long time) {
    return formatPastDate(new Date(time), new Date());
  }

  private static DateTimeFormat monthDayFormat;

  private static DateTimeFormat getMonthDayFormat() {
    if (monthDayFormat == null) {
      monthDayFormat = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.MONTH_ABBR_DAY);
    }
    return monthDayFormat;
  }

  /**
   * Package-private version, takes a fixed "now" time - used for testing
   */
  String formatPastDate(Date date, Date now) {

    // NOTE(zdwang): For now skip it for junit code; also see formatDateTime()
    if (!GWT.isClient()) {
      return "formatPastDate is not yet implemented in unit test code";
    }

    if (!isValid(date, now)) {
      GWT.log("formatPastDate can only format time in the past, trying anyway", null);
    }

    if (isRecent(date, now, 6 * HOUR_MS) || onSameDay(date, now)) {
      return formatTime(date);
    } else if (isRecent(date, now, 30 * DAY_MS)) {
      return getMonthDayFormat().format(date) + " " + formatTime(date);
    } else if (isSameYear(date, now)) {
      return getMonthDayFormat().format(date);
    } else {
      return DateTimeFormat.getMediumDateFormat().format(date);
    }
  }

  /**
   * Formats the specified time as a String.
   */
  public String formatTime(Date date) {
    // NOTE(zdwang): For now skip it for junit code; also see formatPastDate()
    if (!GWT.isClient()) {
      return "formatDateTime is not yet implemented in unit test code";
    }

    // AM/PM -> am/pm for consistency with formatPastDate()
    return DateTimeFormat.getShortTimeFormat().format(date).toLowerCase();
  }

  /**
   * Formats the specified date and time as a String.
   */
  public String formatDateTime(Date date) {
    // NOTE(zdwang): For now skip it for junit code; also see formatPastDate()
    if (!GWT.isClient()) {
      return "formatDateTime is not yet implemented in unit test code";
    }

    // AM/PM -> am/pm for consistency with formatPastDate()
    return DateTimeFormat.getShortDateTimeFormat().format(date).toLowerCase();
  }

  /**
   * @return true if a date is approximately in the past (i.e., before a
   *         minute in the future).
   */
  private boolean isValid(Date date, Date now) {
    long diff = (now.getTime() + MIN_MS) - date.getTime();
    return diff >= 0;
  }

  /**
   * @return true if a duration is less than x ms.
   */
  private boolean isRecent(Date date, Date now, long ms) {
   return (now.getTime() - date.getTime()) < ms;
  }

 /**
  * @return true if a date occurs on the same day as today.
  */
  private boolean onSameDay(Date date, Date now) {
    return (date.getDate() == now.getDate())
        && (date.getMonth() == now.getMonth())
        && (date.getYear() == now.getYear());
  }

  /**
   * @return true if a date occurs in the same year as this year.
   */
  private boolean isSameYear(Date date, Date now) {
    return date.getYear() == now.getYear();
  }

  /**
   * This is used to get a efficient time for JS.
   * Warning! Use TimerService if you want to actually test and control the time.
   */
  public double currentTimeMillis() {
    // Use an optimised time for JS when running in JS.
    if (!GWT.isClient()) {
      return System.currentTimeMillis();
    } else {
      return Duration.currentTimeMillis();
    }
  }
}
