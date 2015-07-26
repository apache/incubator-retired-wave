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


package org.waveprotocol.wave.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.common.util.JsoCollectionFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Encapsulates the entry level module for Undercurrent.
 *
 * This zeroeth stage is only intended to load critical code that has an
 * important reason to be loaded first (e.g., exception handling).
 *
 */
public interface StageZero {

  /**
   * Default implementation of the zeroeth stage.
   */
  public class DefaultProvider extends AsyncHolder.Impl<StageZero> implements StageZero {

    @Override
    protected final void create(Accessor<StageZero> whenReady) {
      onStageInit();
      // TODO: enable webdriver hook.
      GWT.setUncaughtExceptionHandler(createUncaughtExceptionHandler());
      if (GWT.isScript()) {
        CollectionUtils.setDefaultCollectionFactory(new JsoCollectionFactory());
      }
      onStageLoaded();
      whenReady.use(this);
    }

    /** Notifies this provider that the stage is about to be loaded. */
    protected void onStageInit() {
    }

    /** Notifies this provider that the stage has been loaded. */
    protected void onStageLoaded() {
    }

    /** @return the uncaught exception handler to install. */
    protected UncaughtExceptionHandler createUncaughtExceptionHandler() {
      // Use GWT's one by default.
      return GWT.getUncaughtExceptionHandler();
    }
  }
}
