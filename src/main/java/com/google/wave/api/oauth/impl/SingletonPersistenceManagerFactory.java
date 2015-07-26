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

package com.google.wave.api.oauth.impl;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

/**
 * PersistenceManagerFactory singleton wrapper class. 
 * Allows a single instance of PersistenceManagerFactory to save from costly
 * initializations.
 * 
 * @author kimwhite@google.com (Kimberly White)
 */
public final class SingletonPersistenceManagerFactory {
  private static final PersistenceManagerFactory pmfInstance =
      JDOHelper.getPersistenceManagerFactory("transactions-optional");

  /** PMF constructor. */
  private SingletonPersistenceManagerFactory() {}

  /**
   * Allows app to reuse a single instance of a PersistenceManagerFactory.
   * 
   * @return instance of persistence manager. 
   */
  public static PersistenceManagerFactory get() {
    return pmfInstance;
  }
}
