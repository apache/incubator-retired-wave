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



import com.google.common.io.ByteStreams;

import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

 /**
 * Serve atmosphere.js file to GWT clients This class allows to serve the
 * Atmosphere Javascript client from the same /atmosphere servlet path which is
 * used to process any Atmosphere request instead of place it on the /static
 * path. In addition, the Javascript file is put together with the rest of
 * Atmosphere related source code, during the build process as any other third
 * party dependency.
 * 
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
  @AtmosphereInterceptorService
  public class AtmosphereClientInterceptor implements AtmosphereInterceptor  {

    private static final Log LOG = Log.get(AtmosphereClientInterceptor.class);

    @Override
    public void configure(AtmosphereConfig config) {
        // Nothing to do
    }

    @Override
    public Action inspect(AtmosphereResource resource) {

      AtmosphereRequest request = resource.getRequest();

      try {
        // Find the first context parameter
        String path = request.getPathInfo();

        if (path == null || path.isEmpty())
         return Action.CONTINUE;

        if (path.startsWith("/")) {
          path = path.substring(1);
        }
        String[] parts = path.split("/");

        // Serve the file
        if (parts.length > 0 && "GET".equals(resource.getRequest().getMethod()) && "atmosphere.js".equals(parts[0])) {
          resource.getResponse().setContentType("text/javascript");
          InputStream is =
              this.getClass().getClassLoader()
                  .getResourceAsStream("org/waveprotocol/box/server/rpc/atmosphere/atmosphere.js");
          OutputStream os = resource.getResponse().getOutputStream();
          ByteStreams.copy(is, os);
          return Action.CANCELLED;

        }


      } catch (IOException e) {
        LOG.severe("Error sending atmosphere.js",e);
      }


      return Action.CONTINUE;
    }


    @Override
    public void postInspect(AtmosphereResource resource) {
      // Nothing to do
    }

  }
