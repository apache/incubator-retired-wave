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
package org.waveprotocol.box.server.rpc;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.waveprotocol.box.server.persistence.file.FileAccountStore;
import org.waveprotocol.box.server.persistence.file.FileUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The servlet for fetching available gadgets from a json file on the server.
 */
@SuppressWarnings("serial")
@Singleton
public class GadgetProviderServlet extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(FileAccountStore.class.getName());

  private final LoadingCache<String, String> jsonCache = CacheBuilder.newBuilder().expireAfterWrite(5,
      TimeUnit.MINUTES).build(new CacheLoader<String, String>() {

    public String load(String key) {
      String jsonString = "";
      try {
        jsonString = FileUtils.getStringFromFile("jsongadgets.json");
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Error while loading gadgets json", e);
      }
      return jsonString;
    }
  });

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      String jsonString = jsonCache.get("");
      if (jsonString.equals("")) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Error loading json data from file");
      } else {
        PrintWriter out = response.getWriter();
        out.print(jsonCache.get(""));
        out.flush();
      }
    } catch (ExecutionException ex) {
      throw new IOException(ex);
    }
  }
}
