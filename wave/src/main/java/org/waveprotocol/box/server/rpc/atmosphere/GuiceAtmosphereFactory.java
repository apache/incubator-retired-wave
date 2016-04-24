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

package org.waveprotocol.box.server.rpc.atmosphere;

import com.google.inject.Injector;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereObjectFactory;
import org.waveprotocol.wave.util.logging.Log;

/**
 * Custom factory to use wave's guice injector in Atmosphere
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class GuiceAtmosphereFactory implements AtmosphereObjectFactory {

  private static final Log LOG = Log.get(GuiceAtmosphereFactory.class);

  private static Injector injector;

  @SuppressWarnings("unchecked")
  @Override
  public <T, U extends T> U newClassInstance(AtmosphereFramework framework, Class<T> classType, Class<U> classToInstantiate) throws InstantiationException, IllegalAccessException {
      initInjector(framework);


      if (injector == null) {
          return classToInstantiate.newInstance();
      } else {
          return injector.getInstance(classToInstantiate);
      }
  }

  public String toString() {
      return "Guice ObjectFactory";
  }

  private void initInjector(AtmosphereFramework framework) {
      if (injector == null) {
          com.google.inject.Injector servletInjector = (com.google.inject.Injector)
 framework.getServletContext().getAttribute(
              com.google.inject.Injector.class.getName());

          if (servletInjector != null) {
              injector = servletInjector;
              LOG.fine("Existing injector found to create Atmosphere instances");
          } else {
              LOG.fine("Not injector not found to create Atmosphere instances");
          }
      }
  }
}
